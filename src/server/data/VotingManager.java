package server.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

import server.ChangeManager;
import server.CloudFactory;
import server.CloudStorage;
import server.enums.MoveAction;
import server.model.OpenVote;
import server.model.User;
import server.model.Voting;
import server.model.VotingInput;
import server.model.VotingUser;
import server.model.VotingRequest;
import server.model.VotingClosedNotification;
/**
 * Class for managing voting processes ("voting" database collection) and user votes ("votingusers" database collection).
 * @author soleksiy
 *
 */
public class VotingManager {

	private DatabaseManager dbManager;
	private UserManager userManager;
	private static final String VOTING = "voting";
	private static final String VOTING_USERS = "votingusers";
	
	public VotingManager(DatabaseManager dbm, UserManager um) {
		this.dbManager = dbm;
		this.userManager = um;
	}
		
	private OpenVote createOpenVote(Voting voting, VotingUser votingUser) {
		OpenVote vote = new OpenVote();
		vote.setVotingID(voting.getID()); 
		vote.setInitiator(voting.getInitiatorUid());
		String action = null;
		if (voting.getAction().contains("delete")) {
			action = "Delete";
		}
		else if (voting.getAction().contains("rename")) {
			action = "Rename";
		}
		else if (voting.getAction().contains("move")) {
			action = "Move";
		}
		vote.setAction(action);
		vote.setVotesFor(voting.getVotesFor());
		vote.setVotesAgainst(voting.getVotesAgainst());
		vote.setUserCount(voting.getUserCount());
		vote.setScheme(voting.getScheme());
		if (voting.isAccepted() != null) {
			vote.setAccepted(voting.isAccepted());
		}
		
		vote.setVoted(votingUser.getVoted());
		vote.setPath(votingUser.getPath());
		if ("Delete".equals(action)) {
			vote.setNewPath("N/A");
		}
		else {
			vote.setNewPath(votingUser.getNewPath());
		}
		return vote;
	}
	
	// list open votes where user is the participant
	/**
	 * 
	 * Lists the voting processes where the given user is a participant
	 * @param userUid User UID
	 * @return BSON list of open voting process IDs.
	 */
	public BasicDBList listOpenVotes(String userUid) {
		BasicDBList openVotes = new BasicDBList();
		DBCollection votingCollection = dbManager.getCollection(VOTING, Voting.class);
		DBCollection votingUsersCollection = dbManager.getCollection(VOTING_USERS, VotingUser.class);
		// Get all votingUser documents with the given uid
		BasicDBObject query = new BasicDBObject("uid", userUid);
		BasicDBList votingIDList = new BasicDBList();
		Map<Long, VotingUser> votingIDToVotingUserMap = new HashMap<Long, VotingUser>();
		try (DBCursor dbCursor = votingUsersCollection.find(query)) {
			while (dbCursor.hasNext()) {
				VotingUser votingUser = (VotingUser)dbCursor.next();
				votingIDList.add(votingUser.getVotingID());
				votingIDToVotingUserMap.put(votingUser.getVotingID(), votingUser);
			}
		}
		// Get all the voting documents with the given votingID, and that were not closed yet (isAccepted is not set)
		//query = new BasicDBObject("_id", new BasicDBObject("$in", votingIDList)).append("isAccepted", new BasicDBObject("$exists", false));
		// getting all, regardless whether closed or not
		query = new BasicDBObject("_id", new BasicDBObject("$in", votingIDList)); //.append("isAccepted", new BasicDBObject("$exists", false));
		List<String> initiatorUidList = new ArrayList<String>();
		try (DBCursor dbCursor = votingCollection.find(query)) {
			while (dbCursor.hasNext()) {
				Voting voting = (Voting)dbCursor.next();
				initiatorUidList.add(voting.getInitiatorUid());
				VotingUser votingUser = votingIDToVotingUserMap.get(voting.getID());
				OpenVote vote =createOpenVote(voting, votingUser);
				openVotes.add(vote);
			}
		}
		// Adding initiator UID and username to each openvote
		List<User> users = userManager.listUsers(initiatorUidList, null, false);
		for (Object voteObject : openVotes) {
			OpenVote openVote = (OpenVote)voteObject;
			String userName = findUserName(users, openVote.getInitiator());
			if (userName != null) {
				openVote.setInitiatorName(userName);
			}
		}
		return openVotes;
	}

	private String findUserName(List<User> users, String initiator) {
		for (User user : users) {
			if (user.getUid().equals(initiator)) {
				return user.getDisplayName();
			}
		}
		return null;
	}
	/**
	 * Lists the votes where a given user has not voted yet
	 * @param userUid User UID
	 * @return BSON list of votes where the user has not voted yet.
	 */
	public BasicDBList listVotingRequests(String userUid) {
		BasicDBList result = new BasicDBList();
		DBCollection votingCollection = dbManager.getCollection(VOTING, Voting.class);
		DBCollection votingUsersCollection = dbManager.getCollection(VOTING_USERS, VotingUser.class);
		BasicDBObject query = new BasicDBObject("uid", userUid).append("voted", false);
		BasicDBList votingIDs = new BasicDBList();
		try (DBCursor dbCursor = votingUsersCollection.find(query)) {
			while (dbCursor.hasNext()) {
				VotingUser votingUser = (VotingUser)dbCursor.next();
				VotingRequest item = new VotingRequest();
				item.setPath(votingUser.getPath());
				item.setNewPath(votingUser.getNewPath());
				item.setVotingID(votingUser.getVotingID());
				result.add(item);
				votingIDs.add(votingUser.getVotingID());
			}
		}
		query = new BasicDBObject("_id", new BasicDBObject("$in", votingIDs));
		List<String> initiatorUids = new ArrayList<String>();
		Map<Long, Voting> votingIDToVotingMap = new HashMap<Long, Voting>();
		try (DBCursor dbCursor = votingCollection.find(query)) {
			while (dbCursor.hasNext()) {
				Voting voting = (Voting)dbCursor.next();
				String initiatorUid = voting.getInitiatorUid();
				if (!initiatorUid.equals(userUid)) {
					initiatorUids.add(initiatorUid);
					votingIDToVotingMap.put(voting.getID(), voting);
				}
			}
		}
		Map<String, User> uidToUserMap = getUidToUserMap(initiatorUids, userUid);
		addVotingInfoToVotingRequestList(result, votingIDToVotingMap, uidToUserMap);
		return result;
	}
	/**
	 * List notifications of closed voting processes.
	 * @param initiatorUid Vote initiator UID
	 * @param changeManager 
	 * @param dropbox
	 * @return BSON list of closed voting processes.
	 */
	public BasicDBList listVotingClosedNotifications(String initiatorUid, ChangeManager changeManager, CloudStorage dropbox) {
		// input: initiatorUid
		// output: votingID, action,h, isAccepted;   path, newPat;   message
		BasicDBList result = new BasicDBList();
		DBCollection votingCollection = dbManager.getCollection(VOTING, Voting.class);
		DBCollection votingUsersCollection = dbManager.getCollection(VOTING_USERS, VotingUser.class);
		BasicDBObject query = new BasicDBObject("initiator", initiatorUid).append("isAccepted", new BasicDBObject("$ne", null));
		// There could be multiple such votings, with different votingIDs.
		Map<Long, VotingClosedNotification> votingIDToNotificationMap = new HashMap<Long, VotingClosedNotification>();
		BasicDBList votingIDs = new BasicDBList();
		try (DBCursor dbCursor = votingCollection.find(query)) {
			while (dbCursor.hasNext()) {
				Voting voting = (Voting)dbCursor.next();
				VotingClosedNotification notification = new VotingClosedNotification();
				notification.setVotingID(voting.getID());
				notification.setAccepted(voting.isAccepted());
				notification.setAction(voting.getAction());
				notification.setError(voting.getError());
				result.add(notification);
				votingIDToNotificationMap.put(voting.getID(), notification);
				votingIDs.add(voting.getID());
			}
		}
		query = new BasicDBObject("votingID", new BasicDBObject("$in", votingIDs)).append("uid", initiatorUid);
		try (DBCursor dbCursor = votingUsersCollection.find(query)) {
			while (dbCursor.hasNext()) {
				VotingUser votingUser = (VotingUser)dbCursor.next();
				VotingClosedNotification notification = votingIDToNotificationMap.get(votingUser.getVotingID());
				if (notification!=null) {
					notification.setPath(votingUser.getPath());
					notification.setNewPath(votingUser.getNewPath());
					String message = getClosedNotificationMessage(notification);
					notification.setMessage(message);
					// it should be in the result list already
				}
			}
		}
		//System.out.println("listVotingClosedNotifications size: " + result.size());
		return result;
	}

	/**
	 * This will close time-constrained voting processes, so that the method <code>listVotingClosedNotifications</code> could pick up the closed votings.
	 * @param changeManager
	 * @param cloudFactory
	 * @param fileManager
	 */
	public void closeTimeConstraintVotings(ChangeManager changeManager, CloudFactory cloudFactory, FileManager fileManager) {
		DBCollection votingCollection = dbManager.getCollection(VOTING, Voting.class);
		DBCollection votingUsersCollection = dbManager.getCollection(VOTING_USERS, VotingUser.class);
		BasicDBObject query = new BasicDBObject("periodInMinutes", new BasicDBObject("$exists", true)
								.append("$ne", -1))
								.append("isAccepted", new BasicDBObject("$exists", false));
		try (DBCursor dbCursor = votingCollection.find(query)) {
			while (dbCursor.hasNext()) {
				Voting voting = (Voting)dbCursor.next();
				if (isVotingClosed(voting)) {
					System.out.println("Found a closed vote. Evaluating whether to accept or reject.");
					if (doAccept(voting)) {
						VotingUser initiator = getInitiatorVotingUser(voting, votingUsersCollection);
						CloudStorage cloudStorage = cloudFactory.getCloudStorageByUserUid(initiator.getUid());
						accept(voting, initiator.getPath(), initiator.getNewPath(), initiator.getUid(), changeManager, cloudStorage, fileManager);
					}
					else {
						reject(voting, changeManager);
					}
				}
			}
		}
	}

	private String getClosedNotificationMessage(VotingClosedNotification notification) {
		// at this point, votingID, action, isAccepted, path, newPath are set
		StringBuffer buffer = new StringBuffer();
		String action = notification.getAction();
		if ("renameshared".equals(action) || "suggestrename".equals(action)) {
			buffer.append("Renaming ").append(notification.getPath())
			.append(" to ").append(notification.getNewPath());
		}
		else if ("moveshared".equals(action) || "suggestmove".equals(action)) {
			buffer.append("Moving ").append(notification.getPath())
			.append(" to ").append(notification.getNewPath());
		}
		else if ("deleteshared".equals(action) || "suggestdelete".equals(action)) {
			buffer.append("Deleting ").append(notification.getPath());
		}
		else {
			buffer.append(action).append(" ")
			.append(notification.getPath()).append(". new path: ")
			.append(notification.getNewPath());
		}
		if (notification.getError() != null) {
			buffer.append(" resulted in an error and was canceled: " + notification.getError());
		}
		else {
			if (notification.isAccepted()) {
				buffer.append(" was accepted.");
			}
			else {
				buffer.append(" was rejected.");
			}
		}
		return buffer.toString();
	}

	/**
	 * Adds initiator, initiatorName, action, message.
	 */
	private void addVotingInfoToVotingRequestList(BasicDBList result, 
			Map<Long, Voting> votingIDToVotingMap,
			Map<String, User> uidToUserMap) {
		for (Object o : result) {
			VotingRequest votingRequest = (VotingRequest)o;
			Voting voting = votingIDToVotingMap.get(votingRequest.getVotingID());
			if (voting != null) {
				String initiator = voting.getInitiatorUid();
				votingRequest.setInitiator(initiator);
				votingRequest.setAction(voting.getAction());
				votingRequest.setAccepted(voting.isAccepted());
				User user = uidToUserMap.get(initiator);
				String message = getVotingRequestMessage(votingRequest, user);
				votingRequest.setMessage(message);
			}
		}
	}

	private String getVotingRequestMessage(VotingRequest votingItem, User user) {
		StringBuffer message = new StringBuffer();
		if (user!= null) {
			votingItem.setInitiatorName(user.getDisplayName());
			message.append(user.getDisplayName()).append(" ").
			append(getActiveVoiceAction(votingItem));
		}
		else {
			message.append(votingItem.getPath()).append(" was ").
			append(getPassiveVoiceAction(votingItem));
		}
		return message.toString();
	}
	
	/**
	 * Advanced natural language processing 
	 */
	private String getActiveVoiceAction(VotingRequest votingItem) {
		String action = votingItem.getAction();
		if ("renameshared".equals(action)) {
			return "renamed " + votingItem.getPath() + " to " + votingItem.getNewPath();
		}
		else if ("suggestrename".equals(action)) {
			return "suggested renaming " + votingItem.getPath() + " to " + votingItem.getNewPath();
		}
		else if ("moveshared".equals(action) || "suggestmove".equals(action)) {
			String actionLabel = "moveshared".equals(action) ? "moved " : "suggested moving ";
			String result = actionLabel + votingItem.getPath();
			if (votingItem.getNewPath() != null) {
				return result + " to " + votingItem.getNewPath();
			}
			else {
				return result + " outside the shared folder";
			}
		}
		else if ("deleteshared".equals(action)) {
			return "deleted " + votingItem.getPath();
		} 
		else if("suggestdelete".equals(action)) {
			return "suggested deleting " + votingItem.getPath();
		}
		else {
			return action + "d " + votingItem.getPath();
		}
 	}	
	
	// NOTE: If everything goes well, this should not be used. We should always have the username of the initiator. Always use active voice
	private String getPassiveVoiceAction(VotingRequest votingItem) {
		String action = votingItem.getAction();
		if ("renameshared".equals(action)) {
			return "renamed to " + votingItem.getNewPath();
		}
		else if ("suggestrename".equals(action)) {
			return "suggested to be renamed to " + votingItem.getNewPath();
		}
		else if ("moveshared".equals(action) || "suggestmove".equals(action)) {
			String actionLabel = "moveshared".equals(action) ? "moved " : "suggested to be moved ";
			if (votingItem.getNewPath() != null) {
				return actionLabel + " to " + votingItem.getNewPath();
			}
			else {
				return actionLabel + " outside the shared folder";
			}
		}
		else if ("deleteshared".equals(action)) {
			return "deleted";
		}
		else if ("suggestdelete".equals(action)) {
			return "suggested to be deleted";
		}
		else {
			return "was " + action + "d";
		}
	}

	private Map<String, User> getUidToUserMap(List<String> initiatorUids, String currentUid) {
		List<User> users = userManager.listUsers(initiatorUids, currentUid, false);
		Map<String, User> uidToUserMap = new HashMap<String, User>();
		for (User user : users) {
			uidToUserMap.put(user.getUid(), user);
		}
		return uidToUserMap;
	}

	private boolean updateVotingUsers(VotingInput votingInput) {
		String uid = votingInput.getUid(); // user who voted
		long votingID = votingInput.getVotingID(); 
		// VotingUser: update => set voted;; get path, newPath (in case we need to accept and perform action (e.g. rename))
		BasicDBObject query = new BasicDBObject("uid", uid).append("votingID", votingID);
		DBCollection votingUsersCollection = dbManager.getCollection(VOTING_USERS, VotingUser.class);
		VotingUser votingUser = (VotingUser)votingUsersCollection.findOne(query);
		if (votingUser == null) {
			return false; // either a bug or the vote has been cancelled.
		}
		votingUser.setVoted(true);
		votingUsersCollection.save(votingUser);
		return true;
	}
	
	private Voting updateVoting(VotingInput votingInput) {
		long votingID = votingInput.getVotingID();
		boolean accepted = votingInput.getAccepted();
		// Voting: get action and update, depending on accepted, either increment votesFor or votesAgainst
		DBCollection votingCollection = dbManager.getCollection(VOTING, Voting.class);
		BasicDBObject query = new BasicDBObject("_id", votingID);
		Voting voting = (Voting)votingCollection.findOne(query);
		if (accepted) {
			voting.incVotesFor();
		}
		else {
			voting.incVotesAgainst();
		}
		votingCollection.save(voting);
		return voting;
	}
	/**
	 * Gets VotingInput. Evaluates whether the voting process is cancelled or can be closed. If it is cancelled, reverts the change. If it can be closed, accepts or rejects it based on the voting scheme.
	 * @param votingInput Information about the voting process
	 * @param dropbox
	 * @param changeManager
	 * @param fileManager
	 * @return True if success, false if error.
	 */
	public boolean vote(VotingInput votingInput, CloudStorage dropbox, ChangeManager changeManager, FileManager fileManager) {
		boolean success = true;
		String uid = votingInput.getUid(); // user who voted
		long votingID = votingInput.getVotingID(); 
		//boolean accepted = votingInput.getAccepted(); ???????
		boolean cancel = votingInput.getCancel();
		DBCollection votingUsersCollection = dbManager.getCollection(VOTING_USERS, VotingUser.class);
		
		if (!updateVotingUsers(votingInput)) {
			return false;
		}
		Voting voting = updateVoting(votingInput);
		
		VotingUser initiator = getInitiatorVotingUser(voting, votingUsersCollection);
		String path = initiator.getPath();
		String newPath = initiator.getNewPath();
				
		if (cancel) {
			success = reject(voting, changeManager);
			removeVotingUsers(votingID); // so that the initiator does not get the rejection notification
			removeVoting(votingID);
		}
		else if (isVotingClosed(voting)) {
			if (doAccept(voting)) {
				System.out.println("accepting: uid: " + uid);
				System.out.println("\tpath: " + path);
				System.out.println("\tnewPath: " + newPath);
				
				success = accept(voting, path, newPath, uid, changeManager, dropbox, fileManager);
			}
			else { // reject!
				success = reject(voting, changeManager);
			}
		}
		else { // voting is still open
			// do nothing
		}
		return success;
	}

	private VotingUser getInitiatorVotingUser(Voting voting, DBCollection votingUsersCollection) {
		String uid= voting.getInitiatorUid();
		long votingID = voting.getID();
		// Find such votingUser that has uid = uid, votingId = votingID
		BasicDBObject query = new BasicDBObject("uid", uid).append("votingID", votingID);
		return (VotingUser)votingUsersCollection.findOne(query);
	}

	private boolean accept(Voting voting, String path, String newPath, String uid, ChangeManager changeManager, CloudStorage cloudStorage, FileManager fileManager) {
		boolean isSuccess = true;
		boolean isError = false;
		String error = null;
		if (voting.isAccepted() != null) {
			return false;
		}
		DBCollection votingCollection = dbManager.getCollection(VOTING, Voting.class);
		long votingID = voting.getID();
		String action = voting.getAction();
		String initiatorUid = voting.getInitiatorUid();
		if ("renameshared".equals(action) || "suggestrename".equals(action)) {
			RenameResult renameResult = cloudStorage.renameImpersonated(path, newPath, initiatorUid, fileManager);
			if (renameResult.getError() == null) {
				changeManager.recordMove(path, newPath, initiatorUid, MoveAction.RENAME, renameResult.pathToRevMap, initiatorUid, true); // NOTE: changed from uid --> initiatorUid, includeDeleted = true
			}
			else {
				System.out.println("Error renaming in cloud: " + renameResult.getError());
				isSuccess = false;
				isError = true;
				error = renameResult.getError();
			}
		}
		else if ("moveshared".equals(action) || "suggestmove".equals(action)) {
			System.out.println("accepting "+ action);
			CopyOrMoveResult copyOrMoveResult = cloudStorage.moveImpersonated(path, newPath, initiatorUid, fileManager);
			if (copyOrMoveResult.getError() == null) {
				System.out.println(cloudStorage.getClass().toString() + " move succeeded");
				changeManager.recordMove(path, newPath, initiatorUid, MoveAction.MOVE, copyOrMoveResult.pathToRevMap, initiatorUid, true); // including deleted because we run on behalf of initiator
			}
			else {
				System.out.println("Error moving in cloud: " + copyOrMoveResult.getError());
				isSuccess = false;
				isError = true;
				error = copyOrMoveResult.getError();
			}
		}
		else if ("deleteshared".equals(action) || "suggestdelete".equals(action)) {
			// TODO: 1. dropbox; 2. recordDelete
			 DeleteResult deleteResult = cloudStorage.deleteImpersonated(path, initiatorUid, fileManager); // the current voting user can delete this file, it is still in the shared folder
			 if (deleteResult.getError() == null) {
				 changeManager.recordDelete(path, initiatorUid, initiatorUid, true); // the current voting user can delete this file, it is still not deleted, do not need to fool around with includeDeleted
			 }
			 else {
				 System.out.println("error deleting in cloud.");
				 isSuccess = false;
				 isError = true;
				 error = deleteResult.getError();
			 }
		}
		else {
			System.out.println("Accepting other actions is NOT implemented!");
			isSuccess = false;
		}
		if (isSuccess || isError) {
			closeVoting(votingCollection, votingID, isSuccess, error);
		}
		return isSuccess;
	}
	

	private boolean reject(Voting voting, ChangeManager changeManager) {
		if (voting.isAccepted() != null) {
			return false;
		}
		DBCollection votingCollection = dbManager.getCollection(VOTING, Voting.class);
		DBCollection votingUsersCollection = dbManager.getCollection(VOTING_USERS, VotingUser.class);
		boolean success = true;
		long votingID = voting.getID();
		String action = voting.getAction();
		if ("renameshared".equals(action)) {
			VotingUser initiator = getInitiatorVotingUser(voting, votingUsersCollection);
			String initiatorPath = initiator.getPath();
			String initiatorNewPath = initiator.getNewPath();
			changeManager.restoreRenameShared(initiatorNewPath, initiatorPath, initiator.getUid());
		}
		else if ("suggestrename".equals(action)) {
			// nothing?
		}
		else if ("moveshared".equals(action)) {
			VotingUser initiator = getInitiatorVotingUser(voting, votingUsersCollection);
			String initiatorPath = initiator.getPath();
			String initiatorNewPath = initiator.getNewPath();
			changeManager.restoreMoveShared(initiatorNewPath, initiatorPath, initiator.getUid());
		}
		else if ("suggestmove".equals(action)) {
			// nothing?
		}
		else if ("deleteshared".equals(action)) {
			VotingUser initiator = getInitiatorVotingUser(voting, votingUsersCollection);
			String path = initiator.getPath();
			changeManager.restoreDeleteShared(path, initiator.getUid());
		}
		else if ("suggestdelete".equals(action)) {
			// nothing?
		}
		else {
			System.out.println("Rejecting other actions is not implemented!");
			success = false;
		}
		if (success) {
			closeVoting(votingCollection, votingID, false, null); // isAccepted == false anyway, because this is a reject.
		}
		return success;
	}
	
	/**
	 * A "Voting process" is considered closed if the field isAccepted is not null. This method sets the isAccepted to the value of the passed parameter
	 */
	private void closeVoting(DBCollection votingCollection, long votingID, boolean isAccepted, String errorMessage) {
		BasicDBObject query = new BasicDBObject("_id", votingID);
		Voting voting = (Voting)votingCollection.findOne(query);
		System.out.println("_id: " + votingID);
		if (voting != null) {
			voting.setAccepted(isAccepted);
		}
		if (errorMessage != null) {
			voting.setError(errorMessage);
		}
		else {
			System.out.println("_id: " + votingID + ", voting is null");
		}
		votingCollection.save(voting);
		setVotedVotingUsers(votingID);
	}
	
	/**
	 * As we are closing the voting, it does not make sense to have outstanding votes.
	 */
	private void setVotedVotingUsers(long votingID) {
		DBCollection votingUsersCollection = dbManager.getCollection(VOTING_USERS, VotingUser.class);
		BasicDBObject query = new BasicDBObject("votingID", votingID);
		try (DBCursor dbCursor = votingUsersCollection.find(query)) {
			while (dbCursor.hasNext()) {
				VotingUser votingUser = (VotingUser)dbCursor.next();
				if (!votingUser.getVoted()) {
					votingUser.setVoted(true);
					votingUsersCollection.save(votingUser);
				}
			}
		}
	}

	/**
	 * Removes information from the votingusers collection based on the votingID -- the voting process ID
	 * Could be useful in removevoting?votingID=id
	 * @param votingID Voting process ID
	 */
	public void removeVotingUsers(long votingID) {		
		DBCollection votingUsersCollection = dbManager.getCollection(VOTING_USERS, VotingUser.class);
		BasicDBObject query = new BasicDBObject("votingID", votingID);
		System.out.println("removing from votinguser documents with votingID = " + votingID);
		votingUsersCollection.remove(query);
	}

	/**
	 * Removes information about the voting process from the voting collection, based on the votingID -- the voting process ID
	 * Could be useful -- when initiator removes it from the notification list.
	 * @param votingID
	 */
	public void removeVoting(long votingID) {
		DBCollection votingCollection = dbManager.getCollection(VOTING, Voting.class);
		BasicDBObject query = new BasicDBObject("_id", votingID);
		System.out.println("removing from voting documents with _id = " + votingID);
		votingCollection.remove(query);
	}
	
	private boolean isVotingClosed(Voting voting) {
		String scheme = voting.getScheme();
		int votesFor = voting.getVotesFor();
		int votesAgainst = voting.getVotesAgainst();
		int userCount = voting.getUserCount();
		int minuteInMSec = 1000 * 60;		
		long currentTime = new Date().getTime(); 
		long startTime = voting.getStartTime().getTime();
		System.out.println("currentTime - startTime = " + (currentTime - startTime) + "ms");
		System.out.println("in minutes: " + (currentTime - startTime)/minuteInMSec + "min");
		
		if ("majority".equals(scheme)) {
			return votesFor + votesAgainst > userCount/2.0;
		}
		else if ("majoritytimeconstraint".equals(scheme)) {
			int periodInMinutes = voting.getPeriodInMinutes();
			return (currentTime - startTime)/ minuteInMSec >= periodInMinutes || votesFor + votesAgainst > userCount/2.0;
		}
		else if ("percentage".equals(scheme)) {
			int percentage = voting.getPercentage();
			return votesFor + votesAgainst > ((double)percentage * (double)userCount)/100.0;
		}
		else if ("percentagetimeconstraint".equals(scheme)) {
			int periodInMinutes = voting.getPeriodInMinutes();
			int percentage = voting.getPercentage();
			return (currentTime - startTime)/ minuteInMSec >= periodInMinutes || votesFor + votesAgainst > ((double)percentage * (double)userCount)/100.0;
		}
		else if ("vetotimeconstraint".equals(scheme)) {
			int periodInMinutes = voting.getPeriodInMinutes();
			return (currentTime - startTime)/ minuteInMSec >= periodInMinutes || votesAgainst >= 1;
		}
		else {
			return false;
		}
	}
	
	private boolean doAccept(Voting voting) {
		String scheme = voting.getScheme();
		int votesFor = voting.getVotesFor();
		int votesAgainst = voting.getVotesAgainst();
		int userCount = voting.getUserCount();
		if ("majority".equals(scheme) || "majoritytimeconstraint".equals(scheme)) {
			return votesFor - votesAgainst >= userCount/2.0;
		}
		else if ("percentage".equals(scheme) || "percentagetimeconstraint".equals(scheme)) {
			return votesFor >= ((double)voting.getPercentage() * (double)userCount)/100.0;
		}
		else if ("vetotimeconstraint".equals(scheme)) {
			return votesAgainst == 0;
		}
		else {
			return false;
		}
	}
	
} // end class