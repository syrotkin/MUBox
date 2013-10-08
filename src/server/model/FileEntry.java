package server.model;

import java.util.Date;
import java.util.List;

import com.mongodb.BasicDBObject;

/**
 * Represents a file metadata document, used in the "filedata" collection; for dealing with file metadata locally
 * @author soleksiy
 *
 */
public class FileEntry extends BasicDBObject {
		
	/**
	 * Generated
	 */
	private static final long serialVersionUID = 8122012666190052532L;

	
	private static final String _ID = "_id";
	private static final String UID = "uid";
	
	private static final String FILE_ID = "fileId"; // This is for Google Drive file ID;
	
	private static final String FILENAME = "filename";
	private static final String PATH = "path";
	
	private static final String LOWERCASE_PATH = "lowercasePath";
	
	private static final String IS_DIR = "isDir";
	private static final String IS_SHARED = "isShared";
	private static final String IS_DELETED = "isDeleted";
	
	private static final String DELETION_DATE = "deletionDate";
	private static final String DELETION_UID = "deletionUid";
	private static final String DELETION_USER_NAME = "deletionUserName";
	private static final String DELETION_ACTION = "deletionAction";
	private static final String NEW_PARENT = "newParent";
	private static final String NEW_FILE_NAME = "newFileName";
	
	private static final String CREATION_DATE = "creationDate"; 
	private static final String CREATION_UID = "creationUid";
	private static final String CREATION_USER_NAME = "creationUserName";
	private static final String CREATION_ACTION = "creationAction";
	private static final String OLD_PARENT = "oldParent";
	private static final String OLD_FILE_NAME = "oldFileName";
	
	private static final String REV = "rev";
	
	private static final String SHARED_FOLDER_ID = "sharedFolderID";


	private static final String DELETION_USER_IMG = "deletionUserImg";


	private static final String CREATION_USER_IMG = "creationUserImg";
	
	public String getID() {
		if (this.containsField(_ID)) {
			return get(_ID).toString();
		}
		return "-1";
	}
	
	// Necessary if we are reusing the same entry after inserting it in a DB once. 
	// After it is inserted, it will have an _id, and subsequent inserts will not work.
	public void removeID() {
		if (this.containsField(_ID)) {
			this.removeField(_ID);
		}
	}
	
	// uid
	public String getUid() {
		return (String)get(UID);
	}
	public void setUid(String uid) {
		put(UID, uid);	
	}
	// fileId
	public String getFileId() {
		return (String)get(FILE_ID);
	}
	public void setFileId(String fileId) {
		if (fileId != null) {
			put(FILE_ID, fileId);
		}
	}
	// filename
	public String getFilename() {
		return (String)get(FILENAME);
	}
	public void setFilename(String filename) {
		put(FILENAME, filename);
	}
	// path
	public String getPath() {
		return (String)get(PATH);
	}
	public void setPath(String path) {
		put(PATH, path);
		if (path == null) {
			put(LOWERCASE_PATH, null);
		}
		else {
			put(LOWERCASE_PATH, path.toLowerCase());
		}
	}
	// isDir
	public boolean isDir() {
		return getBoolean(IS_DIR);
	}
	public void setDir(boolean isDir) {
		put (IS_DIR, isDir);
	}
	// isShared
	public boolean isShared() {
		return getBoolean(IS_SHARED);
	}
	public void setShared(boolean isShared) {
		put(IS_SHARED, isShared);
	}
	
	// isDeleted
	public boolean isDeleted() {
		return getBoolean(IS_DELETED);
	}
	public void setDeleted(boolean isDeleted) {
		put(IS_DELETED, isDeleted);
	}
	// deletionDate
	public Date getDeletionDate() {
		return (Date)get(DELETION_DATE);
	}
	public void setDeletionDate(Date deletionDate) {
		put(DELETION_DATE, deletionDate);
	}
	// deletionUid
	public String getDeletionUid() {
		return (String)get(DELETION_UID);
	}
	public void setDeletionUid(String deletionUid) {
		put(DELETION_UID, deletionUid);
	}
	// deletionUserName
	public String getDeletionUserName() {
		return (String)get(DELETION_USER_NAME);
	}
	public void setDeletionUserName(String deletionUserName) {
		put(DELETION_USER_NAME, deletionUserName);
	}
	// deletionAction
	public String getDeletionAction() {
		return (String)get(DELETION_ACTION);
	}
	public void setDeletionAction(String deletionAction) {
		put(DELETION_ACTION, deletionAction);
	}

	public String getDeletionUserImg() {
		return (String)get(DELETION_USER_IMG);
	}
	public void setDeletionUserImg(String deletionUserImg) {
		put(DELETION_USER_IMG, deletionUserImg);
	}
	// newParent
	public String getNewParent() {
		return (String)get(NEW_PARENT);
	}
	public void setNewParent(String newParent) {
		put(NEW_PARENT, newParent);
	}
	// newFileName
	public String getNewFileName() {
		return (String)get(NEW_FILE_NAME);
	}
	public void setNewFileName(String newFileName) {
		put(NEW_FILE_NAME, newFileName);
	}
	// creationDate
	public Date getCreationDate() {
		return (Date)get(CREATION_DATE);
	}
	public void setCreationDate(Date creationDate) {
		put(CREATION_DATE, creationDate);
	}
	// creationUid
	public String getCreationUid() {
		return (String)get(CREATION_UID);
	}
	public void setCreationUid(String creationUid) {
		put(CREATION_UID, creationUid);
	}
	// creationUserName
	public String getCreationUserName() {
		return (String)get(CREATION_USER_NAME);
	}
	public void setCreationUserName(String creationUserName) {
		put(CREATION_USER_NAME, creationUserName);
	}
	// creationAction
	public String getCreationAction() {
		return (String)get(CREATION_ACTION);
	}
	public void setCreationAction(String creationAction) {
		put(CREATION_ACTION, creationAction);
	}
	
	public String getCreationUserImg() {
		return (String)get(CREATION_USER_IMG);
	}
	public void setCreationUserImg(String creationUserImg) {
		put(CREATION_USER_IMG, creationUserImg);
	}
	
	// oldParent
	public String getOldParent() {
		return (String)get(OLD_PARENT);
	}
	public void setOldParent(String oldParent) {
		put(OLD_PARENT, oldParent);
	}
	// oldFileName
	public String getOldFileName() {
		return (String)get(OLD_FILE_NAME);
	}
	public void setOldFileName(String oldFileName) {
		put(OLD_FILE_NAME, oldFileName);
	}
	// rev
	public String getRev() {
		return (String)get(REV);
	}
	public void setRev(String rev) {
		//String currentRev = (String)get(REV);
		if (rev != null) {
			put(REV, rev);
		}
	}
	// sharedFolderID
	public long getSharedFolderID() {
		Long id = (Long)get(SHARED_FOLDER_ID);
		if (id == null) {
			return -1;
		}
		return id.longValue();
	}
	public void setSharedFolderID(long sharedFolderID) {
		put(SHARED_FOLDER_ID, sharedFolderID);
	}

	private List<User> sharedUsers;
	public List<User> getSharedUsers() {
		return sharedUsers;
	}
	public void setSharedUsers(List<User> sharedUsers) {		
		this.sharedUsers = sharedUsers;
	}
		
	// get/set the owner UID (matters for shared folders and everything inside the shared folders)
	public String getOwner() {
		return (String)get("owner");
	}
	public void setOwner(String owner) {
		put("owner", owner);
	}

	// This will be set for files whose creationAction == "renameshared" or "moveshared", 
	// so we could track the votingID and cancel the voting if necessary
	public long getVotingID() {
		Long votingID = (Long)get("votingID");
		if (votingID == null) {
			return -1L;
		}
		return votingID;
	}
	public void setVotingID(long votingID) {
		put("votingID", votingID);
	}

	public Date getLastSeen() {
		return (Date)get("lastSeen");
	}
	public void setLastSeen(Date date) {
		put("lastSeen", date);
	}
	
} // end class
