package server.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import server.Breadcrumb;
import server.CloudStorage;
import server.Constants;
import server.Dropbox;
import server.GoogleDrive;
import server.ServerInfo;
import server.model.FileData;
import server.model.FileEntry;
import server.model.SharedFolder;
import server.model.User;
import server.utils.FilePath;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;


public class FileManager {
	private static final String FILEDATA = "filedata";
	private static final String DELTA_CURSORS = "deltacursors";

	private DatabaseManager dbManager;
	private UserManager userManager;
	private SharedFolderManager sharedFolderManager;

	public FileManager(DatabaseManager dbm, UserManager um, SharedFolderManager shfm) {
		this.dbManager = dbm;
		this.userManager = um;
		this.sharedFolderManager = shfm;
	}

	public String getDeltaCursor(String uid) {
		DBCollection deltaCursors =  dbManager.getCollection(DELTA_CURSORS);
		BasicDBObject query = new BasicDBObject("uid", uid);
		String deltaCursor = null;
		try (DBCursor dbCursor = deltaCursors.find(query)) {// should return 1 row
			while (dbCursor.hasNext()) {
				deltaCursor = (String) dbCursor.next().get("cursor");
			}
		}
		catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return deltaCursor;
	}
	
	public Long getChangeId(String uid) {
		DBCollection deltaCursors = dbManager.getCollection(DELTA_CURSORS);
		BasicDBObject query = new BasicDBObject("uid", uid);
		DBObject result = deltaCursors.findOne(query);
		if (result == null) {
			return null;
		}
		return (Long)result.get("changeId");
	} 
	
	public void saveChangeId(String uid, long newChangeID) {
		System.out.println("saving change ID");
		DBCollection deltaCursors = dbManager.getCollection(DELTA_CURSORS);
		BasicDBObject query = new BasicDBObject("uid", uid);
		DBObject matchingDoc = deltaCursors.findOne(query);
		if (matchingDoc == null) {
			query.append("changeId", newChangeID);
			deltaCursors.save(query);
		}
		else {
			matchingDoc.put("changeId", newChangeID);
			deltaCursors.save(matchingDoc);
		}
	}

	public void saveDeltaCursor(String uid, String deltaCursor) {
		DBCollection deltaCursors = dbManager.getCollection(DELTA_CURSORS);
		BasicDBObject query = new BasicDBObject("uid", uid);
		try (DBCursor dbCursor = deltaCursors.find(query)) {
			if (dbCursor.count() == 0) {
				query.append("cursor", deltaCursor);
				deltaCursors.save(query);
			}
			else {
				while (dbCursor.hasNext()) {
					DBObject current = dbCursor.next();
					current.put("cursor", deltaCursor);
					deltaCursors.save(current);
					break;
				}
			}
		}
		catch (Exception e) {
			// TODO: proper handling
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private boolean needDeltaRefresh(String userUid) {
		// TODO: later may need other criteria, such as ADDING (a field) and checking deltacursors.timestamp
		DBCollection deltacursors = dbManager.getCollection("deltacursors");
		BasicDBObject query = new BasicDBObject("uid", userUid);
		return deltacursors.findOne(query) == null;
	}

	// Using the file data we got from delta, update the filedata DB collection
	private void saveDeltaData(FileData fileData, String userUid, boolean overwrite) {
		Date now = new Date();
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		if (overwrite) {
			BasicDBObject query = new BasicDBObject("uid", userUid); // = new BasicDBObject("uid", fileData.getUid());
			fileDataCollection.remove(query);
			for (FileEntry fileEntry : fileData.getEntries()) {
				fileDataCollection.insert(fileEntry);
			}
		}
		else {
			// match each path, then update the information
			for (FileEntry fileEntry: fileData.getEntries()) {
				BasicDBObject query = new BasicDBObject("lowercasePath", fileEntry.getPath().toLowerCase()).append("uid", userUid);
				try (DBCursor dbCursor = fileDataCollection.find(query)) {
					if (dbCursor.count() == 0) { // we did not have such a file
						// the entry is not in DB. Insert it regardless whether it isDeleted or !isDeleted.
						// deletionDate is set to what we got from dropbox delta
						fileDataCollection.insert(fileEntry);
					}
					else { // we already had this file in the DB
						while (dbCursor.hasNext()) {
							FileEntry current = (FileEntry)dbCursor.next();
							boolean isCurrentDeleted = current.isDeleted();
							boolean isCurrentUpdated = false;
							if (fileEntry.isDeleted()) { // file deleted in dropbox, mark as deleted in DB
								if (!isCurrentDeleted) {
									current.setDeleted(true);
									Date deletionDate = fileEntry.getDeletionDate() != null ? fileEntry.getDeletionDate() : now;
									current.setDeletionDate(deletionDate);
									isCurrentUpdated = true;
									// NOTE: not setting rev if deleted
								}
							}
							else { // file not deleted in dropbox, undelete in DB.
								if (isCurrentDeleted) {
									current.setDeleted(false); 
									current.setDeletionDate(null); 
									current.setDeletionAction(null); 
									// for cases where file has been marked deleted in delta, but then recreated
									current.setPath(fileEntry.getPath()); 
									current.setFilename(fileEntry.getFilename());
									current.setDir(fileEntry.isDir());									
									current.setRev(fileEntry.getRev());
									// TODO: could be restored. creationAction == "restore"?
									isCurrentUpdated = true;
								}
							}
							if (isCurrentUpdated) {
								fileDataCollection.save(current);
							}
							//break; // TODO: Should we assume that this path is unique and break out of the loop?
							// There may be multiple hits for the same lowercasePath if the files have been deleted and recreated
						}
					}
				}
			}
		}
	}

	public FileEntry getFileEntry(String id) {
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));
		FileEntry result = (FileEntry)fileDataCollection.findOne(query);
		return result;
	}

	public FileEntry getFileEntry(String userUid, String path) {
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		BasicDBObject query = new BasicDBObject("uid", userUid).append("path", path);
		FileEntry dbResult = (FileEntry)fileDataCollection.findOne(query); // TODO: This is findOne() Are you sure??
		return dbResult;
	}
	/*
	public String getFileId(String path, String uid) {
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		BasicDBObject query = new BasicDBObject("uid", uid).append("path", path);
	}
	*/

	// Used in case of restore. 
	// Removes from the database the version that was restored to the previous version.
	// A design choice
	// Can also hide it and have some way of showing it if needed.
	public void removeFileEntry(FileEntry entry, String uid) {
		BasicDBObject query = new BasicDBObject("uid", uid).append("lowercasePath", entry.getPath().toLowerCase());
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA);
		fileDataCollection.remove(query);
	}

	private void updateFile(FileEntry entry, String userUid, FileEntry setter) {
		BasicDBObject query = new BasicDBObject("uid", userUid).append("path", entry.getPath());
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		BasicDBObject updater = new BasicDBObject("$set", setter);
		fileDataCollection.update(query, updater);
	}

	private FileEntry getLastSeenSetter(Date date) {
		FileEntry setter = new FileEntry();
		setter.setLastSeen(date);
		return setter;
	}
	
	private FileEntry getDeletionSetter(FileEntry deletionEntry) {
		System.out.println("setting deleted: " + deletionEntry.getPath());
		FileEntry setter = new FileEntry();
		setter.setDeleted(true);
		//setter.removeField("lastSeen");
		setter.setDeletionDate(deletionEntry.getDeletionDate()); 
		setter.setDeletionUid(deletionEntry.getDeletionUid()); 
		setter.setDeletionAction(deletionEntry.getDeletionAction()); 
		setter.setNewParent(deletionEntry.getNewParent()); 
		setter.setNewFileName(deletionEntry.getNewFileName());	
		if (deletionEntry.getVotingID()!= -1L) {
			setter.setVotingID(deletionEntry.getVotingID());
		}
		return setter;
	}

	private FileEntry getSharedSetter(FileEntry entry) {
		FileEntry setter = new FileEntry();
		setter.setShared(entry.isShared());
		String owner = entry.getOwner();
		if (owner != null) {
			setter.setOwner(owner);
		}
		return setter;
	}

	public void updateFileSetShared(FileEntry entry, String uid) {
		FileEntry setter = getSharedSetter(entry);
		updateFile(entry, uid, setter);
	}

	public void updateFileSetDeleted(FileEntry deletionEntry, String uid) {
		// deletionEntry may not have the correct _id in the context
		// if we are updating it for ALL users (shared folder)
		FileEntry setter = getDeletionSetter(deletionEntry);		
		updateFile(deletionEntry, uid, setter);
	}

	public void insertNewFileEntry(FileEntry entry, String userUid) {
		// TODO Leave creationDate as is, it has already been set.
		// INSERT into filedata at path entry.path, with all the necessary data
		System.out.println("DatabaseManager.insertNewFileEntry");
		System.out.println("\t" + entry.getFilename()  + " " + userUid);
		entry.setUid(userUid);
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		BasicDBObject query = new BasicDBObject("uid", userUid).append("lowercasePath", entry.getPath().toLowerCase()); // this will also find deleted entries!
		try(DBCursor dbCursor = fileDataCollection.find(query)) {
			if (dbCursor.count() == 0) { // no such file in DB
				System.out.println("\tNo such file in DB");
				System.out.println("\tTo insert: " + entry.toString());
				fileDataCollection.insert(entry);
				//System.out.println("\tPossible write error: " + writeResult.getError());
				System.out.println("\tinserted: " + entry.getFilename() + " " + entry.getPath());
			}
			else { // there is already such a file in DB
				while (dbCursor.hasNext()) {
					System.out.println("\tThere is such a file in DB.");
					FileEntry next = (FileEntry)dbCursor.next();
					next.setUid(userUid);
					next.setFilename(entry.getFilename());
					next.setPath(entry.getPath());
					next.setDeleted(entry.isDeleted());
					next.setDir(entry.isDir());
					next.setShared(entry.isShared());
					if (entry.getSharedFolderID() != -1) {
						next.setSharedFolderID(entry.getSharedFolderID());
					}
					next.setDeletionDate(entry.getDeletionDate());
					next.setDeletionUid(entry.getDeletionUid());
					next.setDeletionAction(entry.getDeletionAction());
					next.setCreationDate(entry.getCreationDate());
					next.setCreationUid(entry.getCreationUid());
					next.setOldParent(entry.getOldParent());
					next.setOldFileName(entry.getOldFileName());
					if (!entry.isDeleted() && !entry.isDir()) {
						next.setCreationAction("edit");
					}
					else {
						next.setCreationAction(entry.getCreationAction());
					}
					fileDataCollection.save(next);
					System.out.println("\tmodified existing: "  + entry.getFilename() + " " + entry.getPath());
				}
			}
		}
	}

	/********************************** Ancestors *************************************************/

	/**
	 *  In case a file is inserted/restored in a folder hierarchy where some ancestors may be marked as deleted, we 
	 *  need to undelete those ancestors.
	 */
	public void restoreAncestors(String userUid, String toPath, long folderSeqTo) {
		boolean isUndeleteRequired = false;
		DBObject query = buildAncestorQuery(userUid, toPath, folderSeqTo);
		FileData ancestors = listAncestors(query);
		for (FileEntry ancestorEntry : ancestors.getEntries()) {
			if (ancestorEntry.isDeleted()) {
				isUndeleteRequired = true;
			}
		}
		if (isUndeleteRequired) {
			updateFileSetUndeleted(query);
		}	
	}

	/**
	 * Helper method used for restoring ancestors.
	 */
	private FileData listAncestors(DBObject query) {
		FileData result = new FileData(null);
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		try (DBCursor dbCursor = fileDataCollection.find(query)) {
			while (dbCursor.hasNext()) {
				FileEntry next = (FileEntry)dbCursor.next();
				result.addEntry(next);
			}
		}
		return result;
	}

	/**
	 * Get the path to the file in all the shared folders
	 * The resulting query will contain all the user UIDs and all the ancestor paths of the 
	 * input path (and its equivalent in the shared folders) 
	 */
	private DBObject buildAncestorQuery(String userUid, String path, long folderSeq) {
		List<String> inputPaths = new ArrayList<String>();
		BasicDBList uids = new BasicDBList();
		if (folderSeq != -1L) {
			List<SharedFolder> sharedFolders = sharedFolderManager.getSharedFolders(folderSeq);
			String pathToFile = getPathToFileRelativeToSharedRoot(sharedFolders, userUid, path);
			for (SharedFolder folderInfo : sharedFolders) {
				String fullPath = FilePath.combine(folderInfo.getPath(), pathToFile);
				inputPaths.add(fullPath);
				uids.add(folderInfo.getUid());
			}
		}
		else {
			inputPaths.add(path);
			uids.add(userUid);
		}
		BasicDBList pathList = new BasicDBList();
		for (String inputPath : inputPaths) {
			String oldPath = inputPath;
			while (oldPath.lastIndexOf('/') > 0) {
				oldPath = oldPath.substring(0, oldPath.lastIndexOf('/'));
				pathList.add(oldPath);
			}
		}	
		BasicDBObject query = new BasicDBObject("uid", new BasicDBObject("$in", uids)).
				append("path", new BasicDBObject("$in", pathList));
		System.out.println("buildQuery: " + query.toString());
		return query;
	}

	/**
	 * To be used for undeleting ancestors if a file from deep inside the hierarchy was restored (undelete/unmoved), but its ancestors are still deleted 
	 */
	private void updateFileSetUndeleted(DBObject query) {
		FileEntry setter = new FileEntry();
		setter.setDeleted(false);
		setter.setDeletionDate(null);
		setter.setDeletionUid(null);
		setter.setDeletionAction(null);
		setter.setNewParent(null);
		setter.setNewFileName(null);
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		BasicDBObject updater = new BasicDBObject("$set", setter);
		System.out.println("updateFileSetUndeleted query:" + query.toString());
		System.out.println("\tupdater: " + updater.toString());
		fileDataCollection.updateMulti(query, updater);
	}

	// Gets the part of the path that is appended to the "shared root path" to form the full path
	// e.g. given "/Sharing/File.txt", and "/Sharing", gets /File.txt 
	// The part /File.txt will be the same for all users
	public String getPathToFileRelativeToSharedRoot(List<SharedFolder> sharedFolders, String userUid, String userFullPath) {
		String userRootPath = null;
		for (SharedFolder folderInfo: sharedFolders) {
			if (userUid.equals(folderInfo.getUid())) {
				userRootPath = folderInfo.getPath();
				break;
			}
		}
		if (userRootPath == null) {
			return null;
		}
		if (userFullPath.contains(userRootPath)) {
			return userFullPath.substring(userRootPath.length());
		}
		else {
			return null;
		}
	}
	/************************************************ End Ancestors ****************************************/

	public JSONObject listFiles(String path, boolean forceRefreshDelta, User user, CloudStorage cloudStorage) {
		// check if data is in the database.
		//System.out.println("Inside listFiles. cloudStorage: " + cloudStorage.getClass());
		NoWarningJSONObject result = new NoWarningJSONObject();
		String userUid = user.getUid();
		String userName = user.getDisplayName();
		if (isFirstFileAccess(cloudStorage, user)) {
			// TODO: This would be relevant for Google, e.g. get all data from Google
			System.out.println("First time access for this user access");
		}
		else if (forceRefreshDelta || needDeltaRefresh(userUid)) {
			System.out.println("Getting new delta for uid: " + userUid +", username: " + userName);
			
			FileData deltaData = cloudStorage.getDeltaData(this, userUid);
			System.out.println("delta returned: " + deltaData.getEntries().size() + " results");
			saveDeltaData(deltaData, userUid, false);
		}
		FileData fileData = listChildrenForPath(userUid, path, true, true); // reading data from the FileData table, populated by DeltaData
		//System.out.println("filemanager, fileData for user: " + userUid + "; path: " + path + ", SIZE: " + fileData.getEntries().size());
		NoWarningJSONArray fileList = new NoWarningJSONArray();
		addFileDataToResult(fileList, fileData, user);
		result.put("fileList", fileList);
		result.put("url", "");
		JSONArray breadcrumbs = Breadcrumb.getBreadcrumbsAsJSON(path);
		result.put("breadcrumbs", breadcrumbs);
		addCurrentUserToResult(result, user);
		addProvider(result, cloudStorage);
		return result;
	}

	private void addProvider(NoWarningJSONObject result, CloudStorage cloudStorage) {
		if (cloudStorage instanceof Dropbox) {
			result.put("provider", Constants.Provider.DROPBOX);
		}
		else if (cloudStorage instanceof GoogleDrive) {
			result.put("provider", Constants.Provider.GOOGLE);
		}
		else {
			System.out.println("invalid provider: " + cloudStorage.getClass());
			throw new RuntimeException("invalid provider: " + cloudStorage.getClass());
		}
	}

	// TODO: need to implement different logic for Google Drive if first file access 
	private boolean isFirstFileAccess(CloudStorage cloudStorage, User user) {
		if (cloudStorage instanceof Dropbox) {
			return false; // not relevant for dropbox
		}
		else if (cloudStorage instanceof GoogleDrive) {
			return false;
		}
		else {
			String message = "Only Dropbox and Google Drive are supported providers";
			System.out.println(message);
			throw new IllegalStateException(message);
		}
	}

	// Adds each FileEntry in FileData to the fileList JSONArray
	// listingUser is the user for whom we are listing the files.
	private void addFileDataToResult(NoWarningJSONArray fileList, FileData fileData, User listingUser) {
		for (FileEntry fileEntry : fileData.getEntries()) {
			NoWarningJSONObject jsonEntry = new NoWarningJSONObject();
			jsonEntry.put("_id", fileEntry.getID());
			if (fileEntry.getFileId() != null) {
				jsonEntry.put("fileId", fileEntry.getFileId());
			}
			jsonEntry.put("filename", fileEntry.getFilename());
			jsonEntry.put("isDir", fileEntry.isDir());
			jsonEntry.put("path", fileEntry.getPath());
			jsonEntry.put("isDeleted", fileEntry.isDeleted());
			
			if (fileEntry.getLastSeen() != null) {
				jsonEntry.put("lastSeen", fileEntry.getLastSeen().getTime());
			}
			
			boolean isShared = fileEntry.isShared();
			jsonEntry.put("isShared", isShared); // could be used to allow "rename shared", "suggest rename"
			if (isShared) {
				String owner = fileEntry.getOwner();
				if (owner != null) {
					jsonEntry.put("owner", owner);
				}
				long votingID = fileEntry.getVotingID();
				if (votingID != -1L) {
					jsonEntry.put("votingID", votingID);
				}
			}
			long sharedFolderID = fileEntry.getSharedFolderID();
			if (sharedFolderID != -1) {
				jsonEntry.put("sharedFolderID", sharedFolderID);
				List<User> sharedUsers = fileEntry.getSharedUsers();
				if (sharedUsers!= null) {
					NoWarningJSONArray sharedUserArray = new NoWarningJSONArray();
					for (User user : sharedUsers) {
						NoWarningJSONObject jsonUser = new NoWarningJSONObject();
						jsonUser.put("uid", user.getUid());
						jsonUser.put("display_name", user.getDisplayName());
						String userImg = user.getImg();
						if (userImg != null) {
							jsonUser.put("img", userImg);
						}
						sharedUserArray.add(jsonUser);
					}
					jsonEntry.put("sharedUsers", sharedUserArray);
				}
			}
			if (fileEntry.isDeleted()) {
				// TODO: add message, if move, add separate property, newLink -- put html there. Then ng-bind-html-unsafe to it in AngularJS
				// may also be: if rename, just add a newLink="newFileName" // then if it is a folder, just open the folder, if it is a file, download the file
				Date deletionDate = fileEntry.getDeletionDate();
				if (deletionDate != null) {
					String deletionUserName = fileEntry.getDeletionUserName();
					String deletionUserInfo = deletionUserName != null && !deletionUserName.equals(listingUser.getDisplayName()) ? deletionUserName + ", " : "";
					jsonEntry.put("userName", deletionUserInfo);
					setDeletionActionDetails(jsonEntry, fileEntry);
					jsonEntry.put("date", deletionDate.getTime());
				}
			}
			else {
				if (fileEntry.getCreationAction() != null && fileEntry.getCreationDate() != null) {
					Date creationDate = fileEntry.getCreationDate();
					if (creationDate != null) {
						String creationUserName = fileEntry.getCreationUserName();
						String creationUserInfo = creationUserName != null && !creationUserName.equals(listingUser.getDisplayName()) ? creationUserName + ", " : "";
						jsonEntry.put("userName", creationUserInfo);
						setCreationActionDetails(jsonEntry, fileEntry);
						jsonEntry.put("date", creationDate.getTime());
					}
				}
			}
			fileList.add(jsonEntry);
		}
	}

	private void addCurrentUserToResult(NoWarningJSONObject result, User user) {
		String userUid = user.getUid();
		String displayName = user.getDisplayName();
		NoWarningJSONObject userData = new NoWarningJSONObject();
		userData.put("uid", userUid);
		userData.put("display_name", displayName);
		result.put("user", userData);
	}

	private void setDeletionActionDetails(NoWarningJSONObject jsonEntry, FileEntry fileEntry) {
		String details = null;
		String deletionAction = fileEntry.getDeletionAction();
		if ("delete".equals(deletionAction)) {
			details = "Deleted: ";
		}
		else if ("deleteshared".equals(deletionAction)) {
			details = "Deleted, acceptance pending: ";
		}
		else if ("rename".equals(deletionAction) || "renameshared".equals(deletionAction)) {
			String newLink = null;
			if (fileEntry.isDir()) {
				newLink = "<a href=\"#"+ FilePath.combine(fileEntry.getNewParent(), fileEntry.getNewFileName()) +"\">" + fileEntry.getNewFileName() + "</a>";
			}
			else {
				if ("rename".equals(deletionAction)) {
					String serverName = ServerInfo.getServerName();
					int port = ServerInfo.getPort();
					String url = "http://" + serverName + ":" + port;
					newLink = "<a href=\""+ url +"/directdownload"+ FilePath.combine(fileEntry.getNewParent(), fileEntry.getNewFileName()) +"\" target=\"_blank\">" + fileEntry.getNewFileName() + "</a>";
				}
				else {
					newLink = "<a href=\"javascript:void(0)\">" + fileEntry.getNewFileName() + "</a>";
				}
			}
			jsonEntry.put("newLink", newLink);
			details = "Renamed: ";
		}
		else if ("move".equals(deletionAction) || "moveshared".equals(deletionAction)) {
			String newLink = null;
			if (fileEntry.getNewParent() != null) {
				newLink = "<a href=\"#"+ fileEntry.getNewParent() +"\">" + fileEntry.getNewParent() + "</a>";
			}
			jsonEntry.put("newLink", newLink);
			details = "Moved: ";
		}
		jsonEntry.put("details", details);
		jsonEntry.put("deletionAction", deletionAction);
	}

	private void setCreationActionDetails(NoWarningJSONObject jsonEntry, FileEntry fileEntry) {
		String details = null;
		String creationAction = fileEntry.getCreationAction();
		if ("upload".equals(creationAction)) {
			details = "Added: ";
		}
		else if ("edit".equals(creationAction)) {
			details = "Modified: ";
		}
		else if ("newfolder".equals(creationAction)) {
			details = "Created: ";
		}
		else if ("copy".equals(creationAction)) {
			details = "Copied: ";
			String newLink = null;
			if (fileEntry.getOldParent() != null) {
				newLink = "<a href=\"#" + fileEntry.getOldParent() + "\">" + fileEntry.getOldParent() + "</a>";
			}
			jsonEntry.put("newLink", newLink);
		}
		else if ("move".equals(creationAction)) {
			details = "Moved: ";
			String newLink = null;
			if (fileEntry.getOldParent() != null) { 
				newLink = "<a href=\"#" + fileEntry.getOldParent() + "\">" + fileEntry.getOldParent() + "</a>";
			}
			jsonEntry.put("newLink", newLink);
		}
		else if ("moveshared".equals(creationAction)) {
			details = "Moved, acceptance pending: ";
			String newLink = null;
			if (fileEntry.getOldParent() != null) {
				newLink = "<a href=\"#" + fileEntry.getOldParent() + "\">" + fileEntry.getOldParent() + "</a>";
			}
			jsonEntry.put("newLink", newLink);
		}
		else if ("rename".equals(creationAction)) {
			details = "Renamed: ";
			// NOTE: this is not a real link (the file has been deleted), but for consistency put this string in the newLink.
			jsonEntry.put("newLink", fileEntry.getOldFileName());
		}
		else if ("renameshared".equals(creationAction)) {
			details = "Renamed, acceptance pending: ";
			// NOTE: likewise, this is not a real link.
			jsonEntry.put("newLink", fileEntry.getOldFileName());
		}

		else if ("restore".equals(creationAction)) {
			details = "Restored: ";
		}
		else {
			details = creationAction + ": ";
		}
		jsonEntry.put("details", details);
		jsonEntry.put("creationAction", creationAction);
	}

	private FileData listFiles(String userUid, String regex, boolean includeDeleted, boolean listSharedUsers, 
			boolean excludeOwnDeletions, boolean markLastSeen) {
		BasicDBObject query = new BasicDBObject("uid", userUid).append("path", Pattern.compile(regex));
		if (!includeDeleted) { 
			query = query.append("isDeleted", false);
		}
		// Need filename, isDir, path
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		FileData result = new FileData(userUid);
		Map<String, String> users = new HashMap<String, String>();
		Date now = new Date();
		try (DBCursor dbCursor = fileDataCollection.find(query).sort(new BasicDBObject("isDir", -1).append("lowercasePath", 1))) {
			while (dbCursor.hasNext()) {
				FileEntry current = (FileEntry)dbCursor.next();
				boolean isDeleted = current.isDeleted();
				String deletionUid = current.getDeletionUid();
				String creationUid = current.getCreationUid();
				if (!isDeleted || includeDeleted) {
					if (excludeOwnDeletions) {
						if ((isDeleted && deletionUid != null && deletionUid.equals(userUid))) {
							continue;
						}
					}
					String userName = null;
					if (isDeleted) {
						//if (!userUid.equals(deletionUid)) { 
							userName = userManager.getDisplayName(deletionUid, users);
							current.setDeletionUserName(userName);
						//}
					}
					else {
						//if (!userUid.equals(creationUid)) {
							userName = userManager.getDisplayName(creationUid, users);
							current.setCreationUserName(userName);
						//}
					}
					if (listSharedUsers) {
						if (current.isDir()) {
							long sharedFolderID = current.getSharedFolderID(); 
							if (current.getSharedFolderID() != -1) {
								List<User> sharedUsers = userManager.listUsers(sharedFolderManager.listUserUids(sharedFolderID), userUid, true); 
								current.setSharedUsers(sharedUsers);
							}
						}
					}
					result.addEntry(current);
					if (markLastSeen) {
						setLastSeen(current, now, fileDataCollection);
					}
					
				}
			}
		}
		return result;	
	}

	private void setLastSeen(FileEntry current, Date date, DBCollection fileDataCollection) {
		//BasicDBObject query = new BasicDBObject("uid", current.getUid()).append("path", current.getPath());
		FileEntry setter = getLastSeenSetter(date);
		updateFile(current, current.getUid(), setter);
	}

	// gets data for given path, its immediate children, but not subdirectories. It is like /home2/path --> listing path
	// omits isDeleted entries
	public FileData listChildrenForPath(String userUid, String path, boolean includeDeleted, boolean markLastSeen) {
		String pathPart = "/".equals(path) ? "/" : path + "/";
		String escapedPath = Pattern.quote(pathPart);
		String regex = "^" +escapedPath + "[^/]+$";
		return listFiles(userUid, regex, includeDeleted, true, false, markLastSeen);
	}

	public FileData listDescendants(String userUid, String path, boolean includeDeleted) {
		String escapedPath = Pattern.quote(path);
		String regex = "^" + escapedPath + "/.+$";
		return listFiles(userUid, regex, includeDeleted, false, false, false);
	}

	public FileData listEntryAndDescendants(String userUid, String path, boolean includeDeleted) {
		//String pathPart = "/".equals(path) ? "/" : path;
		String escapedPath = Pattern.quote(path);
		String regex = "^" + escapedPath + "$|^" + escapedPath + "/.+$";
		return listFiles(userUid, regex, includeDeleted, false, false, false);
	}

	// folder: {uid, path}, adding seq to it
	public void insertSharedFolders(List<SharedFolder> folders) {
		System.out.println("enter FileManager.insertSharedFolders");
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		long newID = sharedFolderManager.insertSharedFolders(folders);

		SharedFolder ownersFolder = getOwnersFolder(folders);
		// if no data matches, ownersFileData is a FileData object with an empty Entries list
		FileData ownersFileData = listEntryAndDescendants(ownersFolder.getUid(), ownersFolder.getPath(), false);

		for (SharedFolder folder : folders) {
			if (folder.getUid().equals(ownersFolder.getUid())) { 
				// this is the owner, he already has the hierarchy
				for (FileEntry fileEntry : ownersFileData.getEntries()) {
					if (fileEntry.getPath().equals(folder.getPath())) {
						fileEntry.setSharedFolderID(newID);
					}
					fileEntry.setShared(true);
					String owner = folder.getOwner();
					if (owner != null) {
						fileEntry.setOwner(owner);
					}
					fileDataCollection.save(fileEntry);
				}
			}
			else {
				for (FileEntry fileEntry: ownersFileData.getEntries()) {
					String path = fileEntry.getPath().replaceFirst(Pattern.quote(ownersFolder.getPath()), Matcher.quoteReplacement(folder.getPath()));
					// TODO: what if Dean syncs with Dropbox before we do this sharing?
					// then Dean will have existing files.
					// Need to query if each file exists, then do what you did before.
					// if it exists, update it,
					// if it does not exist, create a new one.
					BasicDBObject query = new BasicDBObject("uid", folder.getUid()).append("path", path);
					FileEntry entry = (FileEntry)fileDataCollection.findOne(query); 
					if (entry == null) { 
						// this should be the usual case. The other users don't have these shared files in MUBox, 
						// only in Dropbox
						FileEntry newEntry = new FileEntry();
						newEntry.setUid(folder.getUid());
						newEntry.setFileId(fileEntry.getFileId());
						newEntry.setPath(path);
						newEntry.setFilename(FilePath.getFileName(path));
						newEntry.setDir(fileEntry.isDir());
						newEntry.setDeleted(false);
						newEntry.setShared(true);
						if (path.equals(folder.getPath())) {
							newEntry.setSharedFolderID(newID);
						}
						String owner = folder.getOwner();
						if (owner != null) {
							newEntry.setOwner(owner);
						}
						fileDataCollection.insert(newEntry);
					}
					else { 
						// in case users resync with Dropbox, the files shared in Dropbox become available in MUBox, 
						// and at this point we just need to mark them shared
						entry.setShared(true);
						if (path.equals(folder.getPath())) {
							entry.setSharedFolderID(newID);
						}
						String owner = folder.getOwner();
						if (owner != null) {
							entry.setOwner(owner);
						}
						fileDataCollection.save(entry);
					}
				}
			}
		}
	}

	// folder: {uid, path}, adding seq to it
	public void insertSharedFolders_OLD(List<SharedFolder> folders) {
		System.out.println("enter FileManager.insertSharedFolders");
		DBCollection fileDataCollection = dbManager.getCollection(FILEDATA, FileEntry.class);
		long newID = sharedFolderManager.insertSharedFolders(folders);
		
	
		for (SharedFolder folder : folders) {
			// insert into filedata that this folder is shared
			BasicDBObject query = new BasicDBObject("uid", folder.getUid()).append("path", folder.getPath());
			
			try (DBCursor dbCursor = fileDataCollection.find(query)) {
				if (dbCursor.count() == 0) {
					System.out.println("Inserting new row in filedata: " + query.toString());
					String path = (String)query.get("path");
					String filename = FilePath.getFileName(path);
					query.put("filename", filename);
					query.put("lowercasePath", path.toLowerCase());
					query.put("isDeleted", false);
					query.put("isDir", true);
					query.put("isShared", true);
					query.put("sharedFolderID", newID);
					String owner = folder.getOwner();
					if (owner!=null) {
						query.put("owner", folder.getOwner());
					}
					fileDataCollection.insert(query);
				}
				else {
					while (dbCursor.hasNext()) {
						FileEntry next = (FileEntry)dbCursor.next();
						System.out.println("marking as shared: uid:" + (String)next.get("uid")  + ", path: " + (String)next.get("path"));
						next.setShared(true);
						next.setSharedFolderID(newID);
						String owner = folder.getOwner();
						if (owner != null) {
							next.setOwner(owner);
						}
						fileDataCollection.save(next);
						// Making all descendants shared, too.
						FileData descendants = listDescendants(folder.getUid(), next.getPath(), true);
						for (FileEntry descendant : descendants.getEntries()) {
							descendant.setShared(true);
							descendant.setOwner(owner);
							updateFileSetShared(descendant, descendant.getUid());
						}
					}
				}
			}
		}
	}

	private SharedFolder getOwnersFolder(List<SharedFolder> folders) {
		for (int i = 0; i < folders.size(); i++) {
			if (folders.get(i).getUid().equals(folders.get(i).getOwner())) {
				return folders.get(i); 
			}
		}
		return null;
	}

} // end class
