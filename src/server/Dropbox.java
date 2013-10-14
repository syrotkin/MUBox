package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import server.data.CopyOrMoveResult;
import server.data.DeleteResult;
import server.data.FileManager;
import server.data.NoWarningJSONArray;
import server.data.NoWarningJSONObject;
import server.data.RenameResult;
import server.data.UserManager;
import server.enums.CopyMoveAction;
import server.model.FileData;
import server.model.FileEntry;
import server.model.User;
import server.settings.DropboxSettings;
import server.utils.DateFormatter;
import server.utils.FilePath;
import spark.Session;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DeltaEntry;
import com.dropbox.client2.DropboxAPI.DeltaPage;
import com.dropbox.client2.DropboxAPI.DropboxLink;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.WebAuthSession;

/**
 * Implementation of CloudStorage for Dropbox. Provides Dropbox-specific file management operations.
 * @author soleksiy
 *
 */
public class Dropbox implements CloudStorage {	
	
	private DropboxAuthorizationManager authorizationManager;
	private UserManager userManager;
	
	public Dropbox(UserManager userManager, DropboxSettings dropboxSettings) {
		this.userManager = userManager;
		authorizationManager = new DropboxAuthorizationManager(dropboxSettings);
	}
	
	private Session session;
	private String uid;
	@Override
	public void setUid(String uid) {
		this.uid = uid;
	}

	
	/* (non-Javadoc)
	 * @see server.CloudStorage#setServerSession(spark.Session)
	 */
	@Override
	public void setServerSession(Session session) {
		this.session = session;
		authorizationManager.setServerSession(session);
	}
	
	
	private JSONObject getAuthenticationResponse(String serverName, int port) {
		return authorizationManager.getAuthenticationResponse(serverName, port);
	} 

	/* (non-Javadoc)
	 * @see server.CloudStorage#getFileInfo(java.lang.String, java.lang.String)
	 */
	@Override
	public FileInfo getFileInfo(String path, String rev, FileManager fileManager) {
		DropboxAPI<WebAuthSession> api = getDropboxAPI();
		String fileName = FilePath.getFileName(path); 
		System.out.println("Dropbox.getFileInfo. fileName: " + fileName);
		Entry entry = null;
		try {
			entry = api.metadata(path, -1, null, false, rev);
		}
		catch (DropboxException e) { // TODO: depending on exception (e.g. tokens revoked), may need to reauthenticate
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		String localPath = Constants.DOWNLOADED_FILES + "\\" + fileName; // TODO: proper file path handling??
		File file = new File(localPath);
		FileOutputStream outputStream =  null;
		try  {
			outputStream = new FileOutputStream(file);
			api.getFile(path, rev, outputStream, null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (DropboxException ex) { // TODO: depending on exception (e.g. tokens revoked), may need to reauthenticate
			ex.printStackTrace();
		}
		finally {
			if (outputStream!=null) {
				try {
					outputStream.close();
				}
				catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		if (outputStream == null) {
			String message = "Could not download file " + path + " from Dropbox.";
			System.err.println(message);
			throw new RuntimeException(message);
		}
		return new FileInfo(file.getAbsolutePath(), entry.mimeType);
	}

	/* (non-Javadoc)
	 * @see server.CloudStorage#delete(java.lang.String)
	 */
	@Override
	public DeleteResult delete(String path, FileManager fileManager) {
		System.out.println("In Dropbox.delete. Path is " + path);
		DropboxAPI<WebAuthSession> api = getDropboxAPI();
		return delete(api, path, fileManager);
	}
	
	/* (non-Javadoc)
	 * @see server.CloudStorage#delete(java.lang.String, java.lang.String)
	 */
	@Override
	public DeleteResult deleteImpersonated(String path, String initiatorUid, FileManager fileManager) {
		DropboxAPI<WebAuthSession> api = getDropboxAPIForUser(initiatorUid);
		return delete(api, path, fileManager);
	}

	private DeleteResult delete(DropboxAPI<WebAuthSession> api, String path, FileManager fileManager) {
		DeleteResult result = new DeleteResult();
		try {
			api.delete(path);
			String rev = queryRev(api, path);
			result.setRev(rev);
		} catch (DropboxException e) {		
			// TODO: proper handling
			e.printStackTrace();
			result.setError(e.toString());
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see server.CloudStorage#rename(java.lang.String, java.lang.String)
	 */
	@Override
	public RenameResult rename(String oldPath, String newPath, FileManager fileManager) {
		DropboxAPI<WebAuthSession> api = getDropboxAPI();
		return rename(api, oldPath, newPath, fileManager);	
	}
	
	/* (non-Javadoc)
	 * @see server.CloudStorage#rename(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public RenameResult renameImpersonated(String path, String newPath, String initiatorUid, FileManager fileManager) {
		DropboxAPI<WebAuthSession> api = getDropboxAPIForUser(initiatorUid);
		return rename(api, path, newPath, fileManager);
	}
	
	private RenameResult rename(DropboxAPI<WebAuthSession> api, String oldPath, String newPath, FileManager fileManager) {
		RenameResult result = new RenameResult();
		Entry entry = null;
		Map<String, String> pathToRevMap = new HashMap<String, String>();
		try {
			entry = api.move(oldPath, newPath);
			pathToRevMap.put(entry.path, entry.rev);
			getDescendantRevs(api, entry, pathToRevMap);
			result.pathToRevMap = pathToRevMap;
			System.out.println(newPath +", rev:" + entry.rev);
		} catch (DropboxException e) {
			// TODO: proper exception handling
			e.printStackTrace();
			result.setError(e.toString());
		}
		if (entry == null) {
			result.setError("Could not rename " + oldPath);
			return result;
		}
		else {
			//result.rev = entry.rev;
			return result;
		}
	}

	/* (non-Javadoc)
	 * @see server.CloudStorage#copyOrMoveFile(server.enums.CopyMoveAction, java.lang.String, java.lang.String)
	 */
	@Override
	public CopyOrMoveResult copyOrMoveFile(CopyMoveAction action, String fromPath, String toPath, FileManager fileManager) {
		CopyOrMoveResult result = new CopyOrMoveResult();
		try {
			DropboxAPI<WebAuthSession> api = getDropboxAPI();
			return copyOrMoveFile(api, action, fromPath, toPath, fileManager);
		}
		catch (Exception ex1) {
			ex1.printStackTrace();
			result.setError(ex1.getMessage());
			return result;
		}
	}
		
	/* (non-Javadoc)
	 * @see server.CloudStorage#moveFile(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public CopyOrMoveResult moveImpersonated(String fromPath, String toPath, String initiatorUid, FileManager fileManager) {
		DropboxAPI<WebAuthSession> api = getDropboxAPIForUser(initiatorUid);
		return copyOrMoveFile(api, CopyMoveAction.MOVE, fromPath, toPath, fileManager);
	}

	/**
	 * Version of copyOrMoveFile that takes a reference to the DropboxAPI. 
	 */
	private CopyOrMoveResult copyOrMoveFile(DropboxAPI<WebAuthSession> api,	CopyMoveAction action, String fromPath, String toPath, FileManager fileManager) {
		CopyOrMoveResult result = new CopyOrMoveResult();
		Map<String, String> pathToRev = new HashMap<String, String>();
		Map<String, String> pathToFileIdMap = new HashMap<String, String>(); // this is only relevant for google, so created a dummy map here
		Entry entry = null;
		try {
			if (action == CopyMoveAction.COPY) {
				entry = api.copy(fromPath, toPath);
			}
			else {
				entry = api.move(fromPath, toPath);
			}
			pathToRev.put(entry.path, entry.rev);
			getDescendantRevs(api, entry, pathToRev);
			result.pathToRevMap = pathToRev;
			result.setPathToFileIdMap(pathToFileIdMap);
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result.setError(e.toString());
		}
		if (entry == null) {
			return result;
		}
		else {
			result.setNewPath(entry.path);
			result.setRev(entry.rev);
			return result;
		}
	}
	
	private void getDescendantRevs(DropboxAPI<WebAuthSession> api, Entry entry, Map<String, String> pathToRev) throws DropboxException {
		if (entry.isDir) {
			entry = api.metadata(entry.path, 0, null, true, null);
			List<Entry> children = entry.contents;
			for (Entry childEntry : children) {
				pathToRev.put(childEntry.path, childEntry.rev);
				getDescendantRevs(api, childEntry, pathToRev);
			}
		}
	}

	/* (non-Javadoc)
	 * @see server.CloudStorage#getLink(java.lang.String)
	 */
	@Override
	public JSONObject getLink(String path, FileManager fileManager) {
		DropboxAPI<WebAuthSession> api = getDropboxAPI(); 
		NoWarningJSONObject result = new NoWarningJSONObject();
		try {
			DropboxLink link = api.media(path, false);
			result.put("url", link.url);
		} catch (DropboxException e) {
			e.printStackTrace();
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see server.CloudStorage#logout()
	 */
	@Override
	public void logout() {
		removeFromSession(Constants.WEB_AUTH_SESSION_SESSION_KEY);
		removeFromSession(Constants.USER_SESSION_KEY);
		removeFromSession(Constants.PROVIDER_SESSION_KEY);
		//removeFromSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY);
	}
	
	private void removeFromSession(String key) {
		if (this.session != null) {
			this.session.removeAttribute(key);
		}
	}

	/* (non-Javadoc)
	 * @see server.CloudStorage#getDeltaData(server.data.FileManager, java.lang.String)
	 */
	@Override
	public FileData getDeltaData(FileManager fileManager, String userUid) {
		// Though we intend to get this delta only once (the very first login), we can reuse the
		// deltacursors.cursor here
		DateFormat dateFormat = new SimpleDateFormat();
		DropboxAPI<WebAuthSession> api = getDropboxAPI();
		String deltaCursor = fileManager.getDeltaCursor(userUid);
		FileData result = new FileData(userUid);
		Date dateNow = new Date(); // record this date as creation date
		DeltaPage<Entry> deltaPage  = null;
		do {
			try {
				deltaPage = api.delta(deltaCursor);
				List<DeltaEntry<Entry>> deltaEntries = deltaPage.entries;
				deltaCursor = deltaPage.cursor;
				for (DeltaEntry<Entry> deltaEntry : deltaEntries) {
					String path = deltaEntry.lcPath;
					Entry entry =  deltaEntry.metadata;
					FileEntry fileEntry = new FileEntry();
					fileEntry.setUid(userUid);
					if (entry == null) {
						System.out.println("Dropbox.delta. Metadata is null. File " + path + " has been deleted.");
						fileEntry.setPath(path); // path is all lowercase. There is no way to get a case sensitive version?
						fileEntry.setFilename(FilePath.getFileName(path));
						fileEntry.setDeleted(true);
						fileEntry.setShared(false);
						fileEntry.setDeletionDate(dateNow);
						// TODO: We don't know if this is a file or a directory 
					}
					else {
						System.out.println("Dropbox.delta. File " + path + " exists.");
						fileEntry.setPath(entry.path);
						fileEntry.setFilename(entry.fileName());
						fileEntry.setDir(entry.isDir);
						fileEntry.setDeleted(entry.isDeleted);
						fileEntry.setShared(false);
						if (entry.modified != null) {
							try {
								fileEntry.setCreationDate(dateFormat.parse(entry.modified));
							} catch (ParseException e) {
								fileEntry.setCreationDate(dateNow);
							} // This matters if the file is not in DB or we are overwriting. Otherwise, this value should not be updated
						}
						else {
							fileEntry.setCreationDate(dateNow);
						}
						fileEntry.setDeletionDate(null);
						fileEntry.setRev(entry.rev);
					}
					result.addEntry(fileEntry);
				}
			} catch (DropboxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} while (deltaPage!=null && deltaPage.hasMore);
		if (deltaCursor != null) {
			fileManager.saveDeltaCursor(userUid, deltaCursor);
		}
		return result;
	}
	
	// Used for debugging -- with the /delta route --> returns delta as text
	// getDeltaData() is used in code
	Object delta(FileManager fileManager) {
		StringBuffer buffer = null;
		DropboxAPI<WebAuthSession> api = getDropboxAPI();
		//String uid = session.attribute(Constants.USER_UID);
		User user = session.attribute(Constants.USER_SESSION_KEY);
		String deltaCursor = fileManager.getDeltaCursor(user.getUid());
		buffer = new StringBuffer();
		DeltaPage<Entry> deltaPage  = null;
		do {
			try {
				deltaPage = api.delta(deltaCursor);
				List<DeltaEntry<Entry>> deltaEntries = deltaPage.entries;
				deltaCursor = deltaPage.cursor;
				for (DeltaEntry<Entry> deltaEntry : deltaEntries) {
					String path = deltaEntry.lcPath;
					Entry entry =  deltaEntry.metadata;
					if (entry == null) {
						System.out.println("Dropbox.delta. Metadata is null. File " + path + " has been deleted.");
						buffer.append(path).append(": deleted\n");
					}
					else {
						buffer.append(path).append(": [").append(entry.path).append(", hash: ").append(entry.hash)
						.append(", rev: ").append(entry.rev).append(", mod. time: ").append(entry.modified)
						.append(",dir: ").append(entry.isDir).append(", icon: ").append(entry.icon).append("]\n");
					}
				}
			} catch (DropboxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (buffer.length() != 0) {
				buffer.append("\n\n");
			}
		} while (deltaPage!=null && deltaPage.hasMore);
		if (deltaCursor != null) {
			fileManager.saveDeltaCursor(user.getUid(), deltaCursor);
		}
		return buffer.toString();
	}
	
	// Directories are not supported
	/* (non-Javadoc) Directories are not supported
	 * @see server.CloudStorage#revisions(java.lang.String)
	 */
	@Override
	public JSONArray revisions(String path, FileManager fileManager) {
		DropboxAPI<WebAuthSession> api = getDropboxAPI();
		NoWarningJSONArray result = new NoWarningJSONArray();
		try {
			List<Entry> entries = api.revisions(path, 0);
			if (entries != null) {
				int length = entries.size();
				for (int i = 0; i < entries.size(); ++i) {  
					Entry entry = entries.get(i);
					NoWarningJSONObject jsonEntry = new NoWarningJSONObject();
					jsonEntry.put("filename", entry.fileName());
					jsonEntry.put("rev", entry.rev);
					jsonEntry.put("path", entry.path);
					Date modifiedDate = RESTUtility.parseDate(entry.modified);
					String modifiedString = DateFormatter.formatDate(modifiedDate);
					jsonEntry.put("modified", modifiedString);
					jsonEntry.put("isDeleted", entry.isDeleted);
					if (entry.isDeleted) {
						if (i < length - 1) {
							//  /download{{revision.path}}?rev={{revision.rev}}
							jsonEntry.put("link", "/download" + entry.path + "?rev=" + entries.get(i + 1).rev); // NOTE: since some revisions correspond to deleted paths (and dropbox does not let us download deleted files), 
							  																				    //we provide links to the revision right before, which is not deleted.
						}
						else {
							jsonEntry.put("link", "/download" + entry.path + "?rev=" + entries.get(i + 1).rev);
						}
					}
					else {
						jsonEntry.put("link", "/download" + entry.path + "?rev=" + entry.rev);
					}
					result.add(jsonEntry);
				}
			}
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see server.CloudStorage#createFolder(java.lang.String)
	 */
	@Override
	public JSONObject createFolder(String filePath, User user, FileManager fileManager) {
		DropboxAPI<WebAuthSession> api = getDropboxAPI();
		NoWarningJSONObject result = new NoWarningJSONObject();
		try {
			System.out.println("In Dropbox.createFolder. filePath: " + filePath);
			Entry newFolder = api.createFolder(filePath);
			result.put("filename", newFolder.fileName());
			result.put("path", newFolder.path);
			result.put("isDir", true);
			result.put("rev", newFolder.rev);
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
		
	/* (non-Javadoc)
	 * @see server.CloudStorage#undelete(server.model.FileEntry, java.lang.String, server.data.FileManager)
	 */
	@Override
	public JSONObject undelete(FileEntry fileEntry, String userUid, FileManager fileManager) {
		System.out.println("Dropbox.undelete is called.");
		NoWarningJSONObject result = new NoWarningJSONObject();
		DropboxAPI<WebAuthSession> api = getDropboxAPI();
		try {
			String rev = fileEntry.getRev();
			if (rev == null) {
				System.out.println("ERROR: cannot get revision for "  + fileEntry.getPath());
				result.put("error", "Could not get revision for " + fileEntry.getPath());
				return result;
			}
			System.out.println("rev is: " + rev);
			undeleteRecursive(api, fileEntry, userUid, fileManager);
			return result; // empty
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			System.out.println("e.message: " + e.getMessage());
			e.printStackTrace();
			System.out.println("will record as error");
			result.put("error", e.getMessage());
			return result;
		}
	}
	
	/**
	 * We need this hack in Dropbox. Dropbox does not allow to undelete folders.
	 * To undelete a folder, we have to undelete all the files in the folder.
	 */
	private void undeleteRecursive(DropboxAPI<WebAuthSession> api, FileEntry fileEntry, String userUid, FileManager fileManager) throws DropboxException {
		if (fileEntry.isDir()) { 
			FileData fileData = fileManager.listChildrenForPath(userUid, fileEntry.getPath(), true, false);
			for (FileEntry childEntry : fileData.getEntries()) {
				undeleteRecursive(api, childEntry, userUid, fileManager);
			}
		}
		else {
			System.out.println("Dropbox: restoring " + fileEntry.getPath() + ", rev: " + fileEntry.getRev());
			api.restore(fileEntry.getPath(), fileEntry.getRev());
		}
	}
		
	/* (non-Javadoc)
	 * @see server.CloudStorage#upload(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public NoWarningJSONObject upload(String fullFilePath, String parentPath, String fileName, FileManager fileManager) {
		// NOTE: in this case assume parentPath already has the trailing slash.
		// NOTE: right now, does not overwrite nor specifies whether the user wants to overwrite the file. Just creates a new file
		System.out.println("in Dropbox.upload");
		DropboxAPI<WebAuthSession> api = getDropboxAPI();
		File file = new File(fullFilePath);
		long fileLengthInBytes = file.length();
		boolean success = false;
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			if (fileLengthInBytes < 150 * 1024 * 1024) {
				try {
					System.out.println("uploading small file");
					//Entry entry = api.putFile(FilePath.combine(parentPath, fileName), fileInputStream, fileLength, null, null);  // NOTE: this is the old version, without overwriting
					Entry entry = api.putFileOverwrite(FilePath.combine(parentPath, fileName), fileInputStream, fileLengthInBytes, null);
					System.out.println("\tparentPath: " + parentPath);
					System.out.println("\tfileName: " + fileName);
					success = true;
					return buildUploadResult(entry.fileName(), parentPath, entry.rev, true, "");
				} catch (DropboxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return buildUploadResult(fileName, parentPath, null, false, e.toString());
				}
			}
			else { // > 150MB --> ChunkedUploader
				DropboxAPI<WebAuthSession>.ChunkedUploader uploader = api.getChunkedUploader(fileInputStream, fileLengthInBytes);
				int retryCounter = 0;
				while (!uploader.isComplete()) {
					try {
						System.out.println("uploading big file");
						uploader.upload();
					}
					catch (DropboxException e) {
						++retryCounter;
						if (retryCounter > 2) {
							return buildUploadResult(fileName, parentPath, null, false, e.toString());
						}
						else {
							continue;
						}
					} catch (IOException e) {
						// TODO Network Error?
						// Could not read from the inputStream
						e.printStackTrace();
						return buildUploadResult(fileName, parentPath, null, false, e.toString());
					}
				}
				success = true;
				String rev = queryRev(api, FilePath.combine(parentPath, fileName)); // Another dropbox query, but called only for 150MB files
				return buildUploadResult(fileName, parentPath, rev, true, "");
			}
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return buildUploadResult(fileName, parentPath, null, false, e2.toString());
		}
		catch (IOException e) { // if closing the fileInputStream failed
			// we don't care if closing failed?
			e.printStackTrace();
			if (success) {
				return buildUploadResult(fileName, parentPath, null, true, "");
			}
			else {
				return buildUploadResult(fileName, parentPath, null, false, e.toString());
			}
		}
	}
	
	private String queryRev(DropboxAPI<WebAuthSession> api, String path) {
		if (api == null) {
			api = getDropboxAPI();
		}
		String rev = null;
		try {
			Entry entry = api.metadata(path, 1, null, false, null);
			rev = entry.rev;
		}
		catch (DropboxException ex) {
			ex.printStackTrace();
		}
		return rev;
	}
	
	private NoWarningJSONObject buildUploadResult(String fileName, String parentPath, String rev, boolean success, String errorMessage) {
		NoWarningJSONObject result = new NoWarningJSONObject();
		result.put("filename", fileName);
		result.put("parentPath", parentPath);
		if (rev != null) {
			result.put("rev", rev);
		}
		result.put("success", success);
		if (!success) {
			result.put("errorMessage", errorMessage);
		}
		return result;
	}
	
	private DropboxAPI<WebAuthSession> getDropboxAPI() {
		configureAccessTokens();
		DropboxAPI<WebAuthSession> api = new DropboxAPI<WebAuthSession>(authorizationManager.getWebAuthSession());
		return api;
	}

	private DropboxAPI<WebAuthSession> getDropboxAPIForUser(String initiatorUid) {
		WebAuthSession webAuthSession = this.authorizationManager.initWebAuthSession();
		User user = userManager.getUser(initiatorUid);
		webAuthSession.setAccessTokenPair(new AccessTokenPair(user.getAccessKey(), user.getAccessSecret()));
		return new DropboxAPI<WebAuthSession>(webAuthSession);	
	}
	
	private boolean configureAccessTokens() {
		return authorizationManager.configureAccessTokens(this.userManager, this.uid);	
	}
		
	private User getUserFromSession() {
		if (this.session != null) {
			return this.session.attribute(Constants.USER_SESSION_KEY);
		}
		return null;
	}

	private void storeInSession(User user) {
		storeInSession(Constants.USER_SESSION_KEY, user);
	}
	
	private void storeInSession(String key, Object item) {
		if (this.session != null) {
			this.session.attribute(key, item);
		}
	}
		
	/**
	 * NOTE: Assumes that by the time it is called, the session is not null
	 */
	public JSONObject tryListFiles(String uid, String serverName, int port, String path, FileManager fileManager) {
		if (uid != null) {
			System.out.println("UID is not null, provider is dropbox");
			// uid passed in query string, we have seen this user before, and have his data stored in DB.
			// we have to set the tokens for current Dropbox session -- if we treat this uid as a new login.
			User user = userManager.getUser(uid);
			if (user != null) {
				storeInSession(user);
				return fileManager.listFiles(path, false, user, this);
			}
			else {
				return getAuthenticationResponse(serverName, port);
			}
		}
		else {
			System.out.println("UID is null, provider is dropbox");
			User user = getUserFromSession(); 
			if (user != null) {
				return fileManager.listFiles(path, false, user, this);
			}
			else {
				boolean ready = configureAccessTokens(); // finishing authentication
				if (ready) {
					user = getUserFromSession();
					return fileManager.listFiles(path, false, user, this);
				}
				else {
					System.out.println("redirecting to Dropbox authorization.");
					return getAuthenticationResponse(serverName, port);
				}
			}
		}
	}

		
	/**
	 * Not relevant for Dropbox! Dropbox does not expose Sharing API
	 */
	@Override
	public void shareFolder(User owner, String uid, String path, FileManager fileManager) {
		// TODO Auto-generated method stub
	}

} // end class
