package server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import server.data.DatabaseManager;
import server.data.FileManager;
import server.data.SharedFolderManager;
import server.data.UniqueIDGenerator;
import server.enums.MoveAction;
import server.model.FileData;
import server.model.FileEntry;
import server.model.SharedFolder;
import server.model.Voting;
import server.model.VotingUser;
import server.utils.FilePath;


public class ChangeManager {
	private SharedFolderManager sharedFolderManager;
	private FileManager fileManager;
	private Map<String, String> dummyMap = new HashMap<String, String>();
	private DatabaseUpdater dbUpdater;
	private DatabaseInserter dbInserter;
	private DatabaseRemover dbRemover;
	
	public ChangeManager(SharedFolderManager shfm, FileManager fm) {
		this.sharedFolderManager = shfm;
		this.fileManager = fm;
		dbUpdater = new DatabaseUpdater(fm);
		dbInserter = new DatabaseInserter(fm);
		dbRemover = new DatabaseRemover(fm);
	}
		
	public void recordCopy(String fromPath, String toPath, String userUid, Map<String, String> pathToRevMap, Map<String, String> pathToFileIdMap) {
		//long sharedFolderIDFrom = sharedFolderManager.getSharedFolderIDForUser(userUid, fromPath); //does not matter. The from folder is not geting changed
		//long folderSeqTo = sharedFolderManager.getSharedFolderIDForUser(userUid, toPath);
		SharedFolder sharedFolderTo = sharedFolderManager.getSharedFolderForUser(userUid, toPath);
		long sharedFolderIDTo = getSharedFolderID(sharedFolderTo); 
		Date now = new Date();
		FileData entryAndDescendants = fileManager.listEntryAndDescendants(userUid, fromPath, false);
		// 1. Copy -- only the Insertion part
		System.out.println("\tFrom path: " + fromPath);
		System.out.println("\tTo path: " + toPath);
		for (FileEntry entry: entryAndDescendants.getEntries()) {
			String oldPath = entry.getPath(); // e.g. /SeparateTesting/F, 
			String newPath = replaceFirstOccurrence(oldPath, fromPath, toPath); // should replace the whole path for parent, but only part of the path for the descendants
			setCreationInfo(entry, newPath, now, userUid, "copy");
			setSharingInfo(entry, sharedFolderTo);
			entry.setOldParent(FilePath.getParentPath(oldPath));
			entry.setOldFileName(entry.getFilename());
			System.out.println("\tEntry.getPath(): "+ entry.getPath());
			// at this point the path has been replaced
			//if (entry.getPath().equals(toPath)) {
			//	System.out.println(entry.getPath() + " equals from path: " + fromPath + ", id: " + fileId);				
			//	entry.setFileId(fileId);
			//}
			recordInDB(dbInserter, entry, sharedFolderIDTo, userUid, pathToRevMap, pathToFileIdMap);
		}
	}

	private String replaceFirstOccurrence(String oldPath, String fromPath, String toPath) {
		return oldPath.replaceFirst("^" + Pattern.quote(fromPath), Matcher.quoteReplacement(toPath));
	}
	
	// HACK: because if an ancestor is renamed, all its children are considered moved.
	private String getActionForDescendants(String originalAction, String descendantOldPath, String ancestorOldPath) {
		if ("move".equals(originalAction)) {
			return "move";
		}
		else if ("rename".equals(originalAction)) {
			if (descendantOldPath.equals(ancestorOldPath)) { // the descendant IS the ancestor!
				return "rename";
			}
			else {
				return "move";
			}
		}
		else if ("renameshared".equals(originalAction)) {
			if (descendantOldPath.equals(ancestorOldPath)) {
				return "renameshared";
			}
			else {
				return "moveshared";
			}
		}
		else {
			System.out.println("originalAction can only be 'move' or 'rename'! Will crap out.");
			throw new IllegalArgumentException("originalAction can only be 'move' or 'rename'.");
		}
 	}
		
	// See comments in the _old method. They may not all make sense.
	public void restoreRenameShared(String initiatorSuggestedPath, String initiatorPath, String initiatorUid) {
		restoreMoveShared(initiatorSuggestedPath, initiatorPath, initiatorUid);
	}

	public void restoreMoveShared(String initiatorSuggestedPath, String initiatorPath, String initiatorUid) {
		// Removal part
		FileData entryAndDescendants = fileManager.listEntryAndDescendants(initiatorUid, initiatorSuggestedPath, false);
		for (FileEntry entry: entryAndDescendants.getEntries()) {
			// 1. Restore -- Deletion/Removal! part
			recordInDB(dbRemover, entry, -1, initiatorUid, dummyMap, dummyMap); // removing the old entry from DB
		}
		// Undeletion part
		FileData restorableEntryAndDescendants = fileManager.listEntryAndDescendants(initiatorUid, initiatorPath, true);		
		for (FileEntry entry: restorableEntryAndDescendants.getEntries())
		{
			entry.setDeleted(false);
			if (entry.containsField("votingID")) { // Voting is finished at this point
				entry.removeField("votingID");
			}
			recordInDB(dbInserter, entry, -1, initiatorUid, dummyMap, dummyMap);
		}
		fileManager.restoreAncestors(initiatorUid, initiatorPath, -1L);
	}
	
	public void restoreDeleteShared(String path, String initiatorUid) {
		// Undeletion part
		FileData restorableEntryAndDescendants = fileManager.listEntryAndDescendants(initiatorUid, path, true);		
		for (FileEntry entry: restorableEntryAndDescendants.getEntries())
		{
			entry.setDeleted(false);
			if (entry.containsField("votingID")) { // Voting is finished at this point
				entry.removeField("votingID");
			}
			recordInDB(dbInserter, entry, -1, initiatorUid, dummyMap, dummyMap);
		}
		fileManager.restoreAncestors(initiatorUid, path, -1L);
	}
	
	public void suggestDelete(String path, String initiatorUid) {
		VotingInfo info = getVotingInfo(initiatorUid, path, path, "suggestdelete");
		recordVotingUsers(info, initiatorUid, path, path, true);
	}
		
	public void deleteShared(String path, String initiatorUid) {
		VotingInfo votingInfo = getVotingInfo(initiatorUid, path, path, "deleteshared");
		FileData fileData = fileManager.listEntryAndDescendants(initiatorUid, path, false);
		Date date = new Date();
		for (FileEntry entry : fileData.getEntries()) {
			setDeletionInfo(entry, date, initiatorUid, "deleteshared");
			entry.setVotingID(votingInfo.getVotingID()); // set votingID for itself and descendants.
			recordInDB(dbUpdater, entry, -1L, initiatorUid, dummyMap, dummyMap); // mark as deleted just for myself
		}	
		recordVotingUsers(votingInfo, initiatorUid, path, path, false);
	}
	
	public void moveShared(String fromPath, String toPath, String initiatorUid) {
		VotingInfo votingInfo = getVotingInfo(initiatorUid, fromPath, toPath, "moveshared");
		Date now = new Date();
		String action = "moveshared";
		FileData entryAndDescendants = fileManager.listEntryAndDescendants(initiatorUid, fromPath, false);
		for (FileEntry entry: entryAndDescendants.getEntries()) {
			String oldPath = entry.getPath(); // oldPath is the path per entry, cf. fromPath -- the path of the topmost entry (for initiator) -- may not match the other users' paths
			String newPath = replaceFirstOccurrence(oldPath, fromPath, toPath); // newPath is per entry (for initiator) -- may not match the other users' paths
			System.out.println("moveShared oldPath: " + oldPath);
			System.out.println("moveShared newPath: " + newPath);
			// 1. deletion
			setDeletionInfo(entry, now, initiatorUid, action);
			entry.setNewParent(FilePath.getParentPath(newPath));
			entry.setNewFileName(FilePath.getFileName(newPath));
			recordInDB(dbUpdater, entry, -1L, initiatorUid, dummyMap, dummyMap); // record just for initiator, do not change the rev (not a full-fledged move)
			// 2. creation
			removeOldFields(entry);
			Date creationDate = new Date(now.getTime() + 1);
			setCreationInfo(entry, newPath, creationDate, initiatorUid, action);
			entry.setOldParent(FilePath.getParentPath(oldPath));
			entry.setOldFileName(FilePath.getFileName(oldPath));
			entry.setVotingID(votingInfo.getVotingID()); // setting votingID for itself and descendants
			recordInDB(dbInserter, entry, -1L, initiatorUid, dummyMap, dummyMap); // record just for initiator, do not change the rev
		}
		recordVotingUsers(votingInfo, initiatorUid, fromPath, toPath, false);
	}

	public void suggestMove(String fromPath, String toPath, String initiatorUid) {
		VotingInfo votingInfo = getVotingInfo(initiatorUid, fromPath, toPath, "suggestmove");
		recordVotingUsers(votingInfo, initiatorUid, fromPath, toPath, true);
	}
		
	public void suggestRename(String fromPath, String toPath, String initiatorUid) {
		VotingInfo votingInfo = getVotingInfo(initiatorUid, fromPath, toPath, "suggestrename");
		recordVotingUsers(votingInfo, initiatorUid, fromPath, toPath, true);
	}
	
	// Similar to recordMove, but
	// 1. only does rename (+ subsequent move of the descendants)
	// 2. only for the given user -- be careful, no shared
	// 3. inserts data into voting and votingusers
	// 4. we still need folderseqs for that (because the path in votingusers may be different)
	public void renameShared(String fromPath, String toPath, String initiatorUid) {
		VotingInfo votingInfo = getVotingInfo(initiatorUid, fromPath, toPath, "renameshared");
		Date now = new Date();
		FileData entryAndDescendants = fileManager.listEntryAndDescendants(initiatorUid, fromPath, false);
		String moveAction = "renameshared";
		for (FileEntry entry: entryAndDescendants.getEntries()) {
			String oldPath = entry.getPath();
			String newPath = replaceFirstOccurrence(oldPath, fromPath, toPath);
			String actionAsString = getActionForDescendants(moveAction.toString().toLowerCase(), oldPath, fromPath);
			// 1. deletion
			setDeletionInfo(entry, now, initiatorUid, actionAsString);
			entry.setNewParent(FilePath.getParentPath(newPath));
			entry.setNewFileName(FilePath.getFileName(newPath));
			recordInDB(dbUpdater, entry, -1L, initiatorUid, dummyMap, dummyMap); // means: record just for this user, do not change the rev
			// 2. creation
			removeOldFields(entry);
			Date creationDate = new Date(now.getTime() + 1);
			setCreationInfo(entry, newPath, creationDate, initiatorUid, actionAsString);
			entry.setOldParent(FilePath.getParentPath(oldPath));
			entry.setOldFileName(FilePath.getFileName(oldPath));
			entry.setVotingID(votingInfo.getVotingID()); // setting votingID for itself and descendants
			recordInDB(dbInserter, entry, -1L, initiatorUid, dummyMap, dummyMap); // record just for this user, do not change the rev
		}
		// NOTE: recording users later, so that it does not trigger notifications prematurely
		// TODO: may factor this into getRenameSharedVotingInfo because we don't care
		recordVotingUsers(votingInfo, initiatorUid, fromPath, toPath, false);
	}
	
	private VotingInfo getVotingInfo(String initiatorUid, String fromPath, String toPath, String action) {
		long sharedFolderIDFrom = sharedFolderManager.getSharedFolderIDForUser(initiatorUid, fromPath); // needed for getting correct paths to insert in votingusers
		long sharedFolderIDTo = sharedFolderManager.getSharedFolderIDForUser(initiatorUid, toPath);
		List<SharedFolder> sharedFolders = sharedFolderManager.getSharedFolders(sharedFolderIDFrom);
		VotingScheme scheme = getVotingScheme(sharedFolders);
		int userCount = sharedFolders.size();
		long votingID = recordVoting(initiatorUid, userCount, action, scheme);
		VotingInfo votingInfo = new VotingInfo();
		votingInfo.setVotingID(votingID);
		votingInfo.setSharedFolders(sharedFolders);
		votingInfo.setSharedFolderIDFrom(sharedFolderIDFrom);
		votingInfo.setSharedFolderIDTo(sharedFolderIDTo);
		return votingInfo;
	}

	private VotingScheme getVotingScheme(List<SharedFolder> sharedFolders) {
		SharedFolder sharedFolder1 = sharedFolders.size() == 0 ? null : sharedFolders.get(0);
		String votingSchemeName = null;
		int percentage = -1;
		int periodInMinutes = -1;
		if (sharedFolder1 != null) {
			votingSchemeName = sharedFolder1.getVotingScheme();
			// NOTE: not all schemes need extra parameters. The extra parameters will be -1 by default.
			percentage = sharedFolder1.getPercentageUsers();
			periodInMinutes = sharedFolder1.getPeriodInMinutes();
		}
		else {
			votingSchemeName = "majority";
		}
		VotingScheme scheme = new VotingScheme();
		scheme.setName(votingSchemeName);
		scheme.setPercentage(percentage);
		scheme.setPeriodInMinutes(periodInMinutes);
		return scheme;
	}

	/**
	 * withCheck means that we will check if a vote with a given user uid and given path already exists. 
	 * If so, it will delete voting document corresponding to this new vote AND will not insert new votingusers 
	 */
	private void recordVotingUsers(VotingInfo votingInfo, String initiatorUid, String fromPath, String toPath, boolean withCheck) {	
		long votingID = votingInfo.getVotingID();
		List<SharedFolder> sharedFolders = votingInfo.getSharedFolders();
		long sharedFolderIDTo = votingInfo.getSharedFolderIDTo();
		String relativeFromPath = fileManager.getPathToFileRelativeToSharedRoot(sharedFolders, initiatorUid, fromPath);
		String relativeToPath =  null;
		if (sharedFolderIDTo != -1L) {
			relativeToPath = fileManager.getPathToFileRelativeToSharedRoot(sharedFolders, initiatorUid, toPath);	
		}
		List<VotingUser> usersToInsert = new ArrayList<VotingUser>();
		DBCollection votingUsersCollection = DatabaseManager.getInstance().getCollection("votingusers", VotingUser.class);
		for (SharedFolder sharedFolder : sharedFolders) { 
			String uid = sharedFolder.getUid();
			String sharedRoot = sharedFolder.getPath();
			VotingUser votingUser = new VotingUser();
			votingUser.setUid(uid);
			votingUser.setVotingID(votingID);
			String oldPath = FilePath.combine(sharedRoot, relativeFromPath);
			votingUser.setPath(oldPath);
			System.out.println("relativeFromPath: " + relativeFromPath);
			System.out.println("relativeToPath: " + relativeToPath);
			if (relativeToPath == null) {
				if (uid.equals(initiatorUid)) {
					votingUser.setNewPath(toPath);
				}
				else {
					votingUser.setNewPath(null);
				}
			}
			else {
				String newPath = FilePath.combine(sharedRoot, relativeToPath);
				votingUser.setNewPath(newPath);
			}
			votingUser.setVoted(uid.equals(initiatorUid));
			usersToInsert.add(votingUser);
		}
		
		boolean canInsert = true;
		if (withCheck) {
			// Check if there is already a votinguser with this uid and path
			for (VotingUser votingUser: usersToInsert) {
				BasicDBObject query = new BasicDBObject("uid", votingUser.getUid()).append("path", votingUser.getPath());
				if (votingUsersCollection.findOne(query) != null) {
					canInsert = false;
					break;
				}
			}
		}
		if (canInsert) {
			for (VotingUser votingUser: usersToInsert) {
				votingUsersCollection.save(votingUser); // inserts because we have created a new VotingUser
			}
		}
		else {
			System.out.println("Canceling vote: vote on same paths by same users already exists. Will delete voting, id: " + votingID);
			DBCollection votingCollection  = DatabaseManager.getInstance().getCollection("voting", Voting.class);
			BasicDBObject objectToRemove = new BasicDBObject("_id", votingID);
			votingCollection.remove(objectToRemove);
		}
	}

	private long recordVoting(String initiatorUid, int userCount, String action, VotingScheme scheme) {
		DBCollection votingCollection = DatabaseManager.getInstance().getCollection("voting", Voting.class);
		UniqueIDGenerator generator = new UniqueIDGenerator(DatabaseManager.getInstance());
		long votingID = generator.getUniqueID("voting");
		Voting voting = new Voting();
		voting.setID(votingID);
		voting.setAction(action);
		voting.setScheme(scheme.getName());
		if(scheme.getPercentage() != -1) {
			voting.setPercentage(scheme.getPercentage());
		}
		if (scheme.getPeriodInMinutes() != -1) {
			voting.setPeriodInMinutes(scheme.getPeriodInMinutes());
		}
		voting.setInitiatorUid(initiatorUid);
		voting.setVotesFor(1);
		voting.setVotesAgainst(0);
		voting.setUserCount(userCount);
		voting.setStartTime(new Date());
		votingCollection.save(voting);
		return votingID;
	}
	
	// fromPath is the topmost ancestor's "from path"
	// toPath is the topmost ancestor's "to path"
	// oldPath is a the "old path" per file entry (different for each file entry)
	// newPath is the "new path" per file entry (different for each file entry)
	// MoveAction.MOVE --> regular move, MoveAction.RENAME --> rename, MoveAction.RESTORE --> unmove, unrename (also see separate recordUndelete method)
	// initiatorUid is used when we want to record the user different from the current user, e.g. in case of "rename shared" + accept --> we record the initiator as the "deleter" and "creator"
	public void recordMove(String fromPath, String toPath, String userUid, MoveAction moveAction, Map<String, String> pathToRevMap, String initiatorUid, boolean includeDeleted) {
			long sharedFolderIDFrom = sharedFolderManager.getSharedFolderIDForUser(userUid, fromPath);
			//long folderSeqTo = sharedFolderManager.getSharedFolderIDForUser(userUid, toPath);
			SharedFolder sharedFolderTo = sharedFolderManager.getSharedFolderForUser(userUid, toPath);
			long sharedFolderIDTo = getSharedFolderID(sharedFolderTo);
			Date now = new Date();
			// NOTE: if RESTORE, it is correct to set includeDeleted = false because this is a move. The files we are moving _back_
			// are not actually _deleted_. Watch out for includeDeleted in other scenarios.
			// NOTE: if it is RENAME for the topmost, it is MOVE for descendants
			FileData entryAndDescendants = fileManager.listEntryAndDescendants(userUid, fromPath, includeDeleted);
			for (FileEntry entry : entryAndDescendants.getEntries()) {
				String oldPath = entry.getPath();
				String newPath = replaceFirstOccurrence(oldPath, fromPath, toPath);
				String actionAsString = null;
				if (MoveAction.RESTORE == moveAction) { // this is a regular restore. restore***Shared would be separate functions
					actionAsString = "restore";
					// 1. Restore -- Deletion/Removal! part
					recordInDB(dbRemover, entry, sharedFolderIDFrom, userUid, pathToRevMap, dummyMap); // removing the old entry from DB; Move should not modify fileId
					// 2. Restore -- Insertion part
					setCreationInfo(entry, newPath, now, userUid, actionAsString);
					setSharingInfo(entry, sharedFolderTo);
					entry.setOldParent(null);
					entry.setOldFileName(null);
				}
				else {
					actionAsString = getActionForDescendants(moveAction.toString().toLowerCase(), oldPath, fromPath);
					// 1. Move -- Deletion part
					String deletionUid = initiatorUid != null ? initiatorUid : userUid;
					entry.removeField("lastSeen"); // because it has not been seen as deleted yet.
					setDeletionInfo(entry, now, deletionUid, actionAsString);
					entry.setNewParent(FilePath.getParentPath(newPath));
					entry.setNewFileName(FilePath.getFileName(newPath));
					recordInDB(dbUpdater, entry, sharedFolderIDFrom, userUid, pathToRevMap, dummyMap); // its rev does not change (well, I don't change it).
					// 2. Move -- Insertion part
					removeOldFields(entry);
					String creationUid = initiatorUid !=null ? initiatorUid : userUid; // We record the voting initiator as the "changer" of the file
					if (entry.containsField("votingID")) { // Voting is finished at this point
						entry.removeField("votingID");
					}
					Date creationDate = new Date(now.getTime() + 1);
					if (!oldPath.equals(fromPath)) { // supposedly, this is a descendant HACK: add 1 more millisecond to descendant
						creationDate = new Date(creationDate.getTime() + 1);
					}
					setCreationInfo(entry, newPath, creationDate, creationUid, actionAsString);
					setSharingInfo(entry, sharedFolderTo);
					entry.setOldParent(FilePath.getParentPath(oldPath));
					entry.setOldFileName(FilePath.getFileName(oldPath));
				}
				// Is folderSeqTo correct? or should we change it back to folderSeqFrom??
				recordInDB(dbInserter, entry, sharedFolderIDTo, userUid, pathToRevMap, dummyMap);
			}
			// HACK: if restoring a file somewhere in the hierarchy, make sure all its ancestors are restored (i.e. isDeleted: false).
			if (MoveAction.RESTORE == moveAction) {
				fileManager.restoreAncestors(userUid, toPath, sharedFolderIDTo);
			}
			// End HACK
	}
	
	/**
	 * Removes the fields that are irrelevant for a newly created FileEntry. 
	*/
	private void removeOldFields(FileEntry entry) {
		entry.removeField("_id");
		entry.removeField("lastSeen");
		entry.removeField("deletionDate");
		entry.removeField("deletionUid");
		entry.removeField("deletionAction");
		entry.removeField("newParent");
		entry.removeField("newFileName");
	}

	public void recordDelete(String path, String userUid, String initiatorUid, boolean includeDeleted) {
		System.out.println("in recordDelete, path is: "  + path + ", user is: " + userUid +", initiator: " + initiatorUid);
		long sharedFolderID = sharedFolderManager.getSharedFolderIDForUser(userUid, path);
		FileData fileData = fileManager.listEntryAndDescendants(userUid, path, includeDeleted);
		Date date = new Date();
		String deletionUid = initiatorUid != null ? initiatorUid : userUid;
		for (FileEntry entry : fileData.getEntries()) {
			setDeletionInfo(entry, date, deletionUid, "delete");
			if (entry.containsField("votingID")) { // Voting is finished at this point
				entry.removeField("votingID");
			}
			recordInDB(dbUpdater, entry, sharedFolderID, userUid, dummyMap, dummyMap);
		}
	}
	
	public void recordUndelete(String path, String userUid) {
		//long folderSeq = sharedFolderManager.getSharedFolderIDForUser(userUid, path);
		SharedFolder sharedFolder = sharedFolderManager.getSharedFolderForUser(userUid, path);
		long sharedFolderID = getSharedFolderID(sharedFolder);
		FileData entryAndDescendants = fileManager.listEntryAndDescendants(userUid, path, true);
		//System.out.println("record FileData Undelete2");
		//entryAndDescendants.printEntries();
		for (FileEntry fileEntry : entryAndDescendants.getEntries()) {
			fileEntry.setDeleted(false);
			fileEntry.setDeletionDate(null);
			fileEntry.setDeletionUid(null);
			fileEntry.setDeletionUserName(null);
			fileEntry.setDeletionAction(null);
			// NOTE: Now record the creationAction as "restore". This is to prevent
			// one restore following another restore,e.g., in case of move + delete or rename + delete.
			Date now = new Date();
			fileEntry.setCreationDate(now);
			fileEntry.setCreationAction("restore");
			fileEntry.setCreationUid(userUid);
			recordInDB(dbInserter, fileEntry, sharedFolderID, userUid, dummyMap, dummyMap);
		}
	}
	
	public void recordUpload(String parentPath, String fileName, String rev, String fileId, String userUid) {
		System.out.println("in recordUpload");
		System.out.println("\tparentPath: " + parentPath);
		System.out.println("\tfileName: " + fileName);
		SharedFolder sharedFolder = sharedFolderManager.getSharedFolderForUser(userUid, parentPath);
		long sharedFolderID = getSharedFolderID(sharedFolder);
		Date now = new Date();
		FileEntry entry = new FileEntry();
		System.out.println("\tcombined: " + FilePath.combine(parentPath, fileName));
		setCreationInfo(entry, FilePath.combine(parentPath, fileName), now, userUid, "upload");
		setSharingInfo(entry, sharedFolder);
		// this cannot be the topmost shared folder. This is a file.
		entry.setDir(false); // because this is an upload
		entry.setRev(rev);
		entry.setFileId(fileId);
		recordInDB(dbInserter, entry, sharedFolderID, userUid, dummyMap, dummyMap);
	}
	
	public void recordNewFolder(String path, String rev, String fileId, String userUid) {
		System.out.println("in recordFileDataNewFolder");
		//long folderSeq = sharedFolderManager.getSharedFolderIDForUser(userUid, path);
		SharedFolder sharedFolder = sharedFolderManager.getSharedFolderForUser(userUid, path);
		long sharedFolderID = getSharedFolderID(sharedFolder);
		Date now = new Date();
		FileEntry entry = new FileEntry();
		setCreationInfo(entry, path, now, userUid, "newfolder");
		setSharingInfo(entry, sharedFolder);
		// this cannot be the topmost shared folder. This is a newly created folder. It has to be shared first.
		entry.setDir(true);
		entry.setRev(rev);
		entry.setFileId(fileId);
		recordInDB(dbInserter, entry, sharedFolderID, userUid, dummyMap, dummyMap);
	}
	
	private long getSharedFolderID(SharedFolder sharedFolder) {
		return sharedFolder != null ? sharedFolder.getSharedFolderID() : -1L;
	}
	
	private void setSharingInfo(FileEntry entry, SharedFolder sharedFolder) {
		if (sharedFolder != null) {
			entry.setShared(true); 
			String owner = sharedFolder.getOwner();
			if (owner != null) {
				entry.setOwner(owner);
			}
		}
	}
	
	// initiatorUid is passed when the currently logged in user is not the one who initiated the change (in the context of shared folders and voting) 
	void setCreationInfo(FileEntry entry, String newPath, Date date, String userUid, String creationAction) {
		entry.setPath(newPath);
		entry.setFilename(FilePath.getFileName(newPath));
		entry.setDeleted(false);
		 
		entry.setCreationDate(date);
		entry.setCreationUid(userUid);
		entry.setCreationAction(creationAction); //"rename"; or "edit" or "move"
		// TODO: decide whether to set creationUserName or not
	}
	
	private void setDeletionInfo(FileEntry entry, Date date, String userUid, String deletionAction) {
		entry.setDeleted(true);
		entry.setDeletionDate(date);
		entry.setDeletionUid(userUid);
		entry.setDeletionAction(deletionAction);//"rename" or "move"
		// TODO: decide whether to set deletionUserName or not
	}
	
	void recordInDB(DatabaseExecutor dbExecutor, FileEntry entry, long sharedFolderID, String userUid, Map<String, String> pathToRevMap, Map<String, String> pathToFileIdMap) {
		entry.removeID(); // in case the entry was created using another entry as a template, we don't want to update the original entry, but want to create a new one
		String rev = pathToRevMap.get(entry.getPath());
		entry.setRev(rev); // setRev takes care of nulls
		String fileId = pathToFileIdMap.get(entry.getPath());
		entry.setFileId(fileId); // setFileId takes care of nulls
		if (sharedFolderID != -1L) { // record update/insert for all users who share the folder
			recordInDBShared(dbExecutor, entry, sharedFolderID, userUid, rev);
		}
		else { // record update/insert just for this user
			recordInDBSingleUser(dbExecutor, entry, userUid);
		}
	}
	
	/**
	 * For a given user, gets paths relative to the shared folder root. 
	 * Then iterates through all shared folders, creates user-specific paths by appending the relative paths to the current shared folder root
	 * saves the change (this is in the loop, so it will be saved for every user/every shared folder) 
	 */
	private void recordInDBShared(DatabaseExecutor dbExecutor, FileEntry entry,  long folderSeq, String userUid, String rev) {
		List<SharedFolder> sharedFolders = sharedFolderManager.getSharedFolders(folderSeq);	
		// Debugging
		System.out.println("recordInDBShared");
		for (SharedFolder sharedFolder : sharedFolders) {
			System.out.println("\tsharedFolder: " + sharedFolder.getSharedFolderID() +", " + sharedFolder.getUid() +", " + sharedFolder.getPath());
		}
		// End debugging
		String relativeFilePath = fileManager.getPathToFileRelativeToSharedRoot(sharedFolders, userUid, entry.getPath());
		String relativeOldParentPath = null;
		String originalOldParent = entry.getOldParent(); // parent paths of the original (initiator) entry
		String originalNewParent = entry.getNewParent();
		if (entry.getOldParent() != null) {
			relativeOldParentPath = fileManager.getPathToFileRelativeToSharedRoot(sharedFolders, userUid, entry.getOldParent());
		}
		String relativeNewParentPath = null;
		if (entry.getNewParent() != null) {
			relativeNewParentPath = fileManager.getPathToFileRelativeToSharedRoot(sharedFolders, userUid, entry.getNewParent());
		}
		for (SharedFolder sharedFolder : sharedFolders) {
			entry.removeID();
			entry.setRev(rev); // setRev takes care of nulls
			String sharedRoot = sharedFolder.getPath();
			entry.setPath(FilePath.combine(sharedRoot, relativeFilePath));				
			if (relativeOldParentPath != null) {
				entry.setOldParent(FilePath.combine(sharedRoot, relativeOldParentPath));
			}
			else {
				if (userUid.equals(sharedFolder.getUid()) && originalOldParent != null) {
					entry.setOldParent(originalOldParent);
				}
				else {
					entry.setOldParent(null); // added to hide non-shared paths from users who are not supposed to see them.
				}
			}
			if (relativeNewParentPath != null) {
				entry.setNewParent(FilePath.combine(sharedRoot, relativeNewParentPath));
			}
			else {
				if (userUid.equals(sharedFolder.getUid()) && originalNewParent != null) {
					entry.setNewParent(originalNewParent);
				}
				else {
					entry.setNewParent(null); // added to hide non-shared paths from the users who are not supposed to see them.
				}
			}
			dbExecutor.execute(entry, sharedFolder.getUid());
		}
	}
	
	private void recordInDBSingleUser(DatabaseExecutor dbExecutor, FileEntry entry, String userUid) {
		dbExecutor.execute(entry, userUid);
	}
	
} // end class


abstract class DatabaseExecutor {
	protected FileManager fileManager;
	
	public DatabaseExecutor(FileManager fm) {
		this.fileManager = fm;
	}
	
	public abstract void execute(FileEntry entry, String userUid);
}

class DatabaseUpdater extends DatabaseExecutor {
	public DatabaseUpdater(FileManager fm) {
		super(fm);
	}
	public void execute(FileEntry entry, String userUid) {
		fileManager.updateFileSetDeleted(entry, userUid);
	}
}

class DatabaseInserter extends DatabaseExecutor {
	public DatabaseInserter(FileManager fm) {
		super(fm);
	}
	public void execute(FileEntry entry, String userUid) {
		fileManager.insertNewFileEntry(entry, userUid);
	}
}

class DatabaseRemover extends DatabaseExecutor {
	public DatabaseRemover(FileManager fm) {
		super(fm);
	}
	public void execute(FileEntry entry, String userUid) {
		fileManager.removeFileEntry(entry, userUid);
	}
}
