package server.data;

import java.util.ArrayList;
import java.util.List;

import server.model.SharedFolder;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
/**
 * Class for operations on shared folders. Mostly interacts with the sharedfolders collection.
 * @author soleksiy
 *
 */
public class SharedFolderManager {
	// Collections (tables)
	private static final String SHARED_FOLDERS = "sharedfolders";
	
	// Fields (columns)
	private static final String SEQ = "seq";
	private static final String UID = "uid";
	
	private DatabaseManager dbManager;
	public SharedFolderManager(DatabaseManager dbManager) {
		this.dbManager = dbManager;
	}
	/**
	 * Gets shared folder records for a given ID. (There is one shared folder record per shared folder user, all of them have the same ID).
	 * @param sharedFolderID
	 * @return List of shared folders
	 */
	public List<SharedFolder> getSharedFolders(long sharedFolderID) {
		List<SharedFolder> result = new ArrayList<SharedFolder>();
		DBCollection sharedFolders = dbManager.getCollection(SHARED_FOLDERS, SharedFolder.class);
		BasicDBObject query = new BasicDBObject(SEQ, sharedFolderID);
		try (DBCursor dbCursor = sharedFolders.find(query)) {
			while (dbCursor.hasNext()) {
				SharedFolder currentFolder = (SharedFolder)dbCursor.next();
				result.add(currentFolder);
			}
		}
		return result;
	}
	/**
	 * For a given user UID and path, get the shared folder ID.
	 * @param userUid
	 * @param path
	 * @return Shared folder ID
	 */
	public long getSharedFolderIDForUser(String userUid, String path) {
		SharedFolder folder = getSharedFolderForUser(userUid, path);
		if (folder == null) {
			return -1L;
		}
		else {
			return folder.getSharedFolderID();
		}
	}
	/**
	 * For a given user UID and path, get the shared folder.
	 * @param userUid
	 * @param path
	 * @return Shared folder
	 */
	public SharedFolder getSharedFolderForUser(String userUid, String path) {
		DBCollection sharedFolders = dbManager.getCollection(SHARED_FOLDERS, SharedFolder.class);
		BasicDBObject query = new BasicDBObject(UID, userUid);
		try (DBCursor dbCursor = sharedFolders.find(query)) {
			while (dbCursor.hasNext()) {
				SharedFolder current = (SharedFolder)dbCursor.next();
				String pathInDB = (String)current.get("path");
				if (path.startsWith(pathInDB)) { // because the same user may share multiple folders
					if (path.equals(pathInDB)) {
						return current;
					}
					else if (path.length() > pathInDB.length() && path.charAt(pathInDB.length()) == '/') {
						return current;
					}
					else {
						//System.out.println("Rogue path: " + pathInDB);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the next sequential ID for a shared folder.
	 * Idea: http://shiflett.org/blog/2010/jul/auto-increment-with-mongodb 
	 * @return Next shared folder ID
	 */
	public long getNextSharedFolderID() {
		UniqueIDGenerator creator = new UniqueIDGenerator(this.dbManager);
		return creator.getUniqueID(SHARED_FOLDERS);
	}
	/**
	 * Lists user UIDs for a given shared folder ID
	 * @param sharedFolderID Shared folder ID
	 * @return List of user UIDs.
	 */
	public List<String> listUserUids(long sharedFolderID) {
		DBCollection sharedFolders = dbManager.getCollection(SHARED_FOLDERS);
		BasicDBObject query = new BasicDBObject(SEQ, sharedFolderID);
		List<String> userUids = new ArrayList<String>();
		try (DBCursor dbCursor = sharedFolders.find(query)) {
			while (dbCursor.hasNext()) {
				DBObject current = dbCursor.next();
				String uid =  (String) current.get(UID);
				userUids.add(uid);
			}
		}
		return userUids;
	}
	/**
	 * Inserts or updates the shared folders that are passed as arguments.
	 * @param folders List of shared folders
	 * @return The shared folder ID used by the inserted/updated shared folders.
	 */
	public long insertSharedFolders(List<SharedFolder> folders) {
		System.out.println("Enter SharedFolderManager.insertSharedFolders");
		long newID = getNextSharedFolderID();
		DBCollection sharedFoldersCollection = dbManager.getCollection(SHARED_FOLDERS, SharedFolder.class);
		for (SharedFolder folder : folders) {
			BasicDBObject query = new BasicDBObject("uid", folder.getUid()).append("path", folder.getPath()); 
			SharedFolder existingFolder = (SharedFolder)sharedFoldersCollection.findOne(query);
			if (existingFolder == null) {
				System.out.println("folder with path: " + folder.getPath() + " did not exist.");
				folder.setSharedFolderID(newID);
				sharedFoldersCollection.insert(folder); // this should set all the sharedFolder fields correctly.
			}
			else {
				System.out.println("folder with path: " + folder.getPath() + " exists. Will update it.");
				existingFolder.setSharedFolderID(newID);
				existingFolder.setOwner(folder.getOwner());
				existingFolder.setVotingScheme(folder.getVotingScheme());
				existingFolder.setPercentageUsers(folder.getPercentageUsers());
				existingFolder.setPeriodInMinutes(folder.getPeriodInMinutes());
				sharedFoldersCollection.save(existingFolder);
			}
		}
		return newID;
	}

} // end class
