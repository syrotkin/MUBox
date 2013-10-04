package server.data;

import java.util.ArrayList;
import java.util.List;

import server.model.SharedFolder;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

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
	
	public long getSharedFolderIDForUser(String userUid, String path) {
		SharedFolder folder = getSharedFolderForUser(userUid, path);
		if (folder == null) {
			return -1L;
		}
		else {
			return folder.getSharedFolderID();
		}
	}
	
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
	
	//http://shiflett.org/blog/2010/jul/auto-increment-with-mongodb
	public long getNextSharedFolderID() {
		UniqueIDGenerator creator = new UniqueIDGenerator(this.dbManager);
		return creator.getUniqueID(SHARED_FOLDERS);
	}
	
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
