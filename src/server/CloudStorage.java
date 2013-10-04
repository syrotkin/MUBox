package server;

import java.io.IOException;

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

public interface CloudStorage {

	/**
	 * Sets the user's UID. This is useful when CloudStorage is created without access to server Session.
	 * Then Credential/AccessTokens could be obtained from the UID
	 */
	public void setUid(String uid);
	/**
	 * Sets the server (Spark) session, as opposed to the Dropbox session.
	 */
	public abstract void setServerSession(Session session);
		
	public abstract FileInfo getFileInfo(String path, String rev, FileManager fileManager);

	public abstract DeleteResult delete(String path, FileManager fileManager);

	/**
	 * The voting version of delete. 
	 */
	public abstract DeleteResult deleteImpersonated(String path, String initiatorUid, FileManager fileManager);

	public abstract RenameResult rename(String oldPath, String newPath, FileManager fileManager);

	/**
	 * The voting version of rename.
	 */
	public abstract RenameResult renameImpersonated(String path, String newPath, String initiatorUid, FileManager fileManager);

	public abstract JSONObject getLink(String path, FileManager fileManager);

	public abstract void logout();

	public abstract FileData getDeltaData(FileManager fileManager,
			String userUid);

	// Getting revisions of directories is not supported.
	public abstract JSONArray revisions(String path, FileManager fileManager);

	public abstract JSONObject createFolder(String filePath, User user, FileManager fileManager);

	public abstract CopyOrMoveResult copyOrMoveFile(CopyMoveAction action,
			String fromPath, String toPath, FileManager fileManager);

	/**
	 * The voting version of move. 
	 */
	public abstract CopyOrMoveResult moveImpersonated(String path, String newPath,
			String initiatorUid, FileManager fileManager);

	public abstract JSONObject undelete(FileEntry fileEntry, String userUid,
			FileManager fileManager);

	/**
	 * Gets the file at <code>fullFilePath</code>. Uploads it to <code>parentPath/fileName</code>.
	 * @param fullFilePath
	 * @param parentPath
	 * @param fileName
	 */
	public abstract NoWarningJSONObject upload(String fullFilePath, String parentPath, String fileName, FileManager fileManager);

	/**
	 * Tries to list files. Returns an "authentication" response if the user is not authorized. The authentication response contains a URL to redirect to either Google or Dropbox to authenticate.
	 * // uid is in the query string, Query string takes precendence. Treat it as a new login. 
	 */
	public JSONObject tryListFiles(String uid, String serverName, int port, String path, FileManager fileManager);
	
	/**
	 * Relevant for Google Drive because Dropbox does not have sharing API. 
	 */
	public void shareFolder(User owner, String uid, String path, FileManager fileManager);
}