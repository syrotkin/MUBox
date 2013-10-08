package server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import server.data.CopyOrMoveResult;
import server.data.FileManager;
import server.data.NoWarningJSONObject;
import server.data.RenameResult;
import server.data.DeleteResult;
import server.enums.CopyMoveAction;
import server.model.FileData;
import server.model.FileEntry;
import server.model.User;
import spark.Session;

/**
 * Cloud abstraction. Common ancestor of cloud storage implementations, such as Dropbox, GoogleDrive.
 * @author soleksiy
 *
 */
public interface CloudStorage {
	/**
	 * Sets the user's UID. This is useful when CloudStorage is created without access to server Session.
	 * Then Credential/AccessTokens could be obtained from the UID
	 * @param uid user UID
	 */
	public void setUid(String uid);
	/**
	 * Sets the server (Spark) session, as opposed to the Dropbox session.
	 * @param session
	 */
	public abstract void setServerSession(Session session);
	/**
	 * Gets file info. This is used for downloading a file.
	 * @param path Path to the file.
	 * @param rev File revision identifier. Rev in Dropbox. RevisionID in Google Drive.
	 * @param fileManager Reference to {@link FileManager}
	 * @return File information {@link FileInfo} of the downloaded file.
	 */
	public abstract FileInfo getFileInfo(String path, String rev, FileManager fileManager);
	/**
	 * Delete a file in cloud storage
	 * @param path Path to file
	 * @param fileManager Reference to {@link FileManager}
	 * @return Object that contains information about the deletion operation
	 */
	public abstract DeleteResult delete(String path, FileManager fileManager);
	/**
	 * The voting version of delete.
	 * @param path Path to file
	 * @param initiatorUid UID of the vote initiator/deleter.
	 * @param fileManager Reference to {@link FileManager}
	 * @return Object that contains information about the deletion operation
	 */
	public abstract DeleteResult deleteImpersonated(String path, String initiatorUid, FileManager fileManager);
	/**
	 * Renames a file in cloud storage
	 * @param oldPath Old path to file
	 * @param newPath New path to file
	 * @param fileManager Reference to {@link FileManager}
	 * @return Result of the rename operation
	 */
	public abstract RenameResult rename(String oldPath, String newPath, FileManager fileManager);
	/**
	 * The voting version of rename.
	 * @param path Old file path
	 * @param newPath New file path
	 * @param initiatorUid UID of the user who initiated the vote (the renamer).
	 * @param fileManager Reference to {@link FileManager}
	 * @return Result of the rename operation.
	 */
	public abstract RenameResult renameImpersonated(String path, String newPath, String initiatorUid, FileManager fileManager);
	/**
	 * Gets a link to file for downloading
	 * @param path Path to file
	 * @param fileManager Reference to {@link FileManager}
	 * @return JSON Object with the URL or error.
	 */
	public abstract JSONObject getLink(String path, FileManager fileManager);
	/**
	 * Deletes the user's information from the session.
	 */
	public abstract void logout();
	/**
	 * Gets the delta/file changes for a given user
	 * @param fileManager  Reference to {@link FileManager}
	 * @param userUid User UID.
	 * @return FileData -- the list of file changes
	 */
	public abstract FileData getDeltaData(FileManager fileManager,
			String userUid);
	/**
	 * Gets a list of revisions for a file. NOTE: Getting revisions of directories is not supported.
	 * @param path Path to file
	 * @param fileManager Reference to {@link FileManager}
	 * @return JSONArray of revisions 
	 */
	public abstract JSONArray revisions(String path, FileManager fileManager);
	/**
	 * Creates a folder in cloud storage
	 * @param filePath Path to the new folder
	 * @param user User
	 * @param fileManager Reference to {@link FileManager}
	 * @return JSONObject that describes the new folder, contains its filename, path and fileId if necessary.
	 */
	public abstract JSONObject createFolder(String filePath, User user, FileManager fileManager);
	/**
	 * Copies or moves a file depending on the arguments
	 * @param action Could be either CopyMoveAction.COPY or CopyMoveAction.MOVE
	 * @param fromPath Original file path
	 * @param toPath New file path
	 * @param fileManager Reference to {@link FileManager}
	 * @return Result of the copy/move operation. Contains name, revision information or error.
	 */
	public abstract CopyOrMoveResult copyOrMoveFile(CopyMoveAction action,
			String fromPath, String toPath, FileManager fileManager);
	/**
	 * The voting version of move. Uses another user's credentials to move a file on his behalf.
	 * @param path Original file path
	 * @param newPath New file path
	 * @param initiatorUid UID of the vote initiator.
	 * @param fileManager  Reference to {@link FileManager}
	 * @return Result of the copy/move operation. Contains name, revision information or error.
	 */
	public abstract CopyOrMoveResult moveImpersonated(String path, String newPath,
			String initiatorUid, FileManager fileManager);
	/**
	 * Restores a deleted file in the cloud storage.
	 * @param fileEntry
	 * @param userUid
	 * @param fileManager
	 * @return JSON object that is the result of restoring. Could contain an error.
	 */
	public abstract JSONObject undelete(FileEntry fileEntry, String userUid,
			FileManager fileManager);
	/**
	 * Gets the file at <code>fullFilePath</code>. Uploads it to <code>parentPath/fileName</code>.
	 * @param fullFilePath 
	 * @param parentPath
	 * @param fileName
	 * @return JSON object that is the result of the upload.
	 */
	public abstract NoWarningJSONObject upload(String fullFilePath, String parentPath, String fileName, FileManager fileManager);
	/**
	 * Tries to list files. Returns an "authentication" response if the user is not authorized. The authentication response contains a URL to redirect to either Google or Dropbox to authenticate.
	 *  uid is in the query string, Query string takes precendence. Treat it as a new login. 
	 * @param uid
	 * @param serverName
	 * @param port
	 * @param path
	 * @param fileManager
	 * @return List of file metadata
	 */
	public JSONObject tryListFiles(String uid, String serverName, int port, String path, FileManager fileManager);
	/**
	 * Shares a folder. Relevant for Google Drive because Dropbox does not have sharing API.
	 * @param owner
	 * @param uid
	 * @param path
	 * @param fileManager
	 */
	public void shareFolder(User owner, String uid, String path, FileManager fileManager);
}