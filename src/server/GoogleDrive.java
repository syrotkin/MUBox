package server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.Drive.Children;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Permissions;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

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
import server.model.FileNode;
import server.model.FileTree;
import server.model.User;
import server.settings.GoogleDriveSettings;
import server.utils.DateFormatter;
import server.utils.FilePath;
import spark.Session;
/**
 * CloudStorage implementation for Google Drive.
 * @author soleksiy
 *
 */
public class GoogleDrive implements CloudStorage {
	
	private static final String GOOGLE_FOLDER_TYPE = "application/vnd.google-apps.folder";
		
	/**
	 * Not required for Google Drive.
	 * @param uid User UID.
	 */
	private String uid;
	@Override
	public void setUid(String uid) {		
		this.uid = uid;
	}
	
	private GoogleAuthorizationCodeFlow flow; 
	private UserManager userManager;

	private GoogleDriveSettings googleDriveSettings;
	public GoogleDrive(UserManager userManager, GoogleDriveSettings googleDriveSettings) {
		this.userManager = userManager;
		this.googleDriveSettings = googleDriveSettings;
	}

	private Session session;
	@Override
	public void setServerSession(Session session) {
		this.session = session;
	}

	private JSONObject getAuthenticationResponse() {
		GoogleAuthorizationCodeFlow flow = getFlow();
		String authorizationUrl = getAuthorizationUrl(flow, null);
		if (authorizationUrl == null) {
			// TODO: proper error handling
			System.out.println("google authorization URL error");
			return new JSONObject();
		}
		else {
			System.out.println("redirecting to URL");
			//response.redirect(authorizationUrl); // redirect to oauth2callback
			NoWarningJSONObject jsonObject = new NoWarningJSONObject();
			jsonObject.put("url", authorizationUrl);
			return jsonObject;
		}		
	}
	
	private String getAuthorizationUrl(GoogleAuthorizationCodeFlow flow, String state) {
		try {					
			GoogleAuthorizationCodeRequestUrl urlBuilder =
					flow.newAuthorizationUrl().setRedirectUri(googleDriveSettings.redirectURI).setState(state);
			//urlBuilder.set("user_id", emailAddress); // THIS IS IN THE URL. DON'T CONFUSE WITH THE DB.
			return urlBuilder.build();}
		catch (Exception ex) {
			// TODO:
			ex.printStackTrace();
			return null;
		}
	}
	
	private GoogleAuthorizationCodeFlow getFlow() {
		if (flow == null) {
			List<String> scopes = getScopes();
			HttpTransport httpTransport = new NetHttpTransport();
			JsonFactory jsonFactory = new JacksonFactory();
			flow = new GoogleAuthorizationCodeFlow.Builder(
					httpTransport, jsonFactory, googleDriveSettings.clientID, googleDriveSettings.clientSecret, scopes)
			//.setAccessType("online") // original
			.setAccessType("offline") 			
			//.setApprovalPrompt("auto").build(); // original
			.setApprovalPrompt("force").build();
		}
		return flow;
	}
	
	private List<String> getScopes() {
		List<String> scopes = new ArrayList<String>();
		scopes.add("https://www.googleapis.com/auth/userinfo.email");
		scopes.add("https://www.googleapis.com/auth/userinfo.profile");
		scopes.add(DriveScopes.DRIVE);
		return scopes;
	}
		
	@Override
	public FileInfo getFileInfo(String path, String rev, FileManager fileManager) {
		System.out.println("GoogleDrive getFileInfo -- not implemented");
		Drive googleDriveService = getServiceInstance();
		User user = getFromSession(Constants.USER_SESSION_KEY);
		FileEntry fileEntry = fileManager.getFileEntry(user.getUid(), path);
		String fileId = fileEntry.getFileId();
		InputStream inputStream = null;
		String mimeType = null;
		String fileName = null;
		try {
			File file = googleDriveService.files().get(fileId).execute();
			fileName = file.getTitle();
			mimeType = file.getMimeType();
			String downloadUrl = null;
			if (rev != null && rev.length() != 0) {
				downloadUrl = googleDriveService.revisions().get(fileId, rev).execute().getDownloadUrl();
			}
			else {
				downloadUrl = file.getDownloadUrl();
			}
			if (downloadUrl != null && downloadUrl.length() != 0) {
				HttpResponse httpResponse = googleDriveService.getRequestFactory()
						.buildGetRequest(new GenericUrl(downloadUrl)).execute();
				inputStream = httpResponse.getContent();
			}
			else {
				System.out.println("Cannot get download URL!");
				throw new RuntimeException("Could not get download URL for "+ path);
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();	
		}
		if (inputStream == null) {
			String message = "Could not get byte stream. Will fail.";
			System.out.println(message);
			throw new RuntimeException(message);
		}
		else {
			String localPath = Constants.DOWNLOADED_FILES + "\\" + fileName;
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(localPath);
				int nextByte;
				while ((nextByte = inputStream.read())!= -1) {
					out.write(nextByte);
				}
			}
			catch (IOException ex) {
				ex.printStackTrace();
				throw new RuntimeException("Could not read file.");
			}
			finally {
				try {
					inputStream.close();
					if (out != null) {
						out.close();
					}
				}
				catch (IOException ex2) {
					ex2.printStackTrace();
					// can still continue
				}
			}
			java.io.File localFile = new java.io.File(localPath);
			System.out.println("abs path: " + localFile.getAbsolutePath() +", mime: " + mimeType);
			return new FileInfo(localFile.getAbsolutePath(), mimeType);
		}
	}

	@Override
	public DeleteResult delete(String path, FileManager fileManager) {
		User user = getFromSession(Constants.USER_SESSION_KEY);
		Drive googleDriveService = getServiceInstance();
		String userUid = user.getUid();
		return delete(userUid, path, fileManager, googleDriveService);
	}

	/**
	 * Delete with impersonation 
	 */
	@Override
	public DeleteResult deleteImpersonated(String path, String initiatorUid, FileManager fileManager) {
		Credential originalCredential = getFromSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY);
		User impersonator = userManager.getUser(initiatorUid);
		Credential impersonatorCredential = getCredential(impersonator);
		Drive googleDriveService = createServiceInstance(impersonatorCredential);
		DeleteResult result = delete(initiatorUid, path, fileManager, googleDriveService);
		if (originalCredential != null) {
			storeInSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY, originalCredential);
		}
		return result;
	}
	
	private DeleteResult delete(String userUid, String path, FileManager fileManager, Drive service) {
		DeleteResult result = new DeleteResult();
		FileEntry fileEntry = fileManager.getFileEntry(userUid, path);
		try {
			service.files().trash(fileEntry.getFileId()).execute();
		}
		catch (IOException ex) {
			result.setError(ex.toString());
		}
		return result;
	}
	
	@Override
	public RenameResult rename(String oldPath, String newPath, FileManager fileManager) {
		Drive googleDriveService = getServiceInstance();
		User user = getFromSession(Constants.USER_SESSION_KEY);
		return rename(user.getUid(), oldPath, newPath, fileManager, googleDriveService);
	}

	@Override
	public RenameResult renameImpersonated(String oldPath, String newPath, String initiatorUid, FileManager fileManager) {
		Credential originalCredential = getFromSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY);
		User impersonator = userManager.getUser(initiatorUid);
		System.out.println("impersonator is: " + impersonator.getDisplayName());
		Credential impersonatorCredential = getCredential(impersonator);
		Drive googleDriveService = createServiceInstance(impersonatorCredential);
		RenameResult result = rename(initiatorUid, oldPath, newPath, fileManager, googleDriveService);
		if (originalCredential != null) {
			storeInSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY, originalCredential);
		}
		return result;
	}

	private RenameResult rename(String userUid, String oldPath, String newPath, FileManager fileManager, 
			Drive googleDriveService) {
		RenameResult result = new RenameResult();
		FileEntry entry = fileManager.getFileEntry(userUid, oldPath);
		String fileId = entry.getFileId();
		try {
			File file = googleDriveService.files().get(fileId).execute(); // can fail.
			String newName = FilePath.getFileName(newPath);
			file.setTitle(newName);
			file = googleDriveService.files().update(fileId, file).execute(); // can fail
		}
		catch (IOException ex) {
			ex.printStackTrace();
			result.setError(ex.toString());
		}
		result.pathToRevMap = new HashMap<String, String>();
		return result;
	}

	@Override
	public JSONObject getLink(String path, FileManager fileManager) {
		NoWarningJSONObject result = new NoWarningJSONObject();
		Drive googleDriveService = getServiceInstance();
		User user = getFromSession(Constants.USER_SESSION_KEY);
		FileEntry fileEntry = fileManager.getFileEntry(user.getUid(), path);
		String fileId = fileEntry.getFileId();
		Permission newPermission = new Permission();
		newPermission.setType("anyone");
		newPermission.setValue("anyone"); // the value would be discarded anyway
		newPermission.setRole("reader");
		try {
			Permissions.Insert insertRequest = googleDriveService.permissions().insert(fileId, newPermission);
			newPermission = insertRequest.execute();
			System.out.println("new permission. id: " + newPermission.getId() + "; type: " + newPermission.getType() + 
					", value: " + newPermission.getValue() + ", role: " + newPermission.getRole());
			File file = googleDriveService.files().get(fileId).execute();
			result.put("url", file.getWebContentLink()); 
		}
		catch (IOException ex) {
			ex.printStackTrace();
			result.put("error", ex.toString());
		}
		return result;
	}

	@Override
	public void logout() {
		removeFromSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY);
		removeFromSession(Constants.USER_SESSION_KEY);
		removeFromSession(Constants.PROVIDER_SESSION_KEY);
	}

	@Override
	public FileData getDeltaData(FileManager fileManager, String userUid) {
		System.out.println("Getting Google Delta data!");
		Drive googleDriveService = getServiceInstance();
		List<FileEntry> fileEntries = new ArrayList<FileEntry>();
		try {
			fileEntries = listChanges(fileManager, userUid, googleDriveService);
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return new FileData(userUid);
		}
		FileData result = new FileData(userUid);
		for (FileEntry fileEntry : fileEntries) {
			fileEntry.setUid(userUid);
			System.out.println(fileEntry);
			result.addEntry(fileEntry);
		}
		return result;
	}
	
	private List<FileEntry> listChanges(FileManager fileManager, String userUid, Drive service) throws IOException {
		// TODO: store the previous changeID and pass it to this call.
		// Change IDs: null, 6241, 6245, ... some missing ... 6262
		//Long changeId = 6258L; //null;
		Long changeId = fileManager.getChangeId(userUid);
		List<Change> result = new ArrayList<Change>();
		Changes.List listRequest = service.changes().list();
		if (changeId != null) {
			listRequest.setStartChangeId(changeId);
		}
		int i = 0;
		do {
			ChangeList changes = listRequest.execute();
			result.addAll(changes.getItems());
			listRequest.setPageToken(changes.getNextPageToken());
			changeId = changes.getLargestChangeId();

			System.out.println("Iteration " + ++i);
		} while (listRequest.getPageToken() != null && listRequest.getPageToken().length() != 0);

		System.out.println("Next change ID: " + changeId);
		if (changeId != null) {
			fileManager.saveChangeId(userUid, changeId);
		}
		System.out.println("NUmber of changes: " + result.size());
		FileTree tree = buildTree(result);
		//print2(tree);
		List<FileEntry> fileEntries = getFileEntryList(tree);
		return fileEntries;
	}
	
	private List<FileEntry> getFileEntryList(FileTree tree) {
		List<FileEntry> result = new ArrayList<FileEntry>();
		FileNode root = tree.getRoot();
		List<FileNode> childNodes = root.getChildNodes();
		if (childNodes != null) {
			for (FileNode childNode: childNodes) {
				getFileEntryListRec(childNode, new StringBuffer("/"), result);
			}
		}
		return result;
	}
	
	private void getFileEntryListRec(FileNode node, StringBuffer pathBuffer, List<FileEntry> fileList) {
		FileEntry entry = new FileEntry();
		fileList.add(entry);
		String currentTitle = node.getFile().getTitle();
		String currentPath = pathBuffer.toString()  + currentTitle; 
		entry.setFileId(node.getFileId());
		entry.setFilename(currentTitle);
		entry.setPath(currentPath);
		// we don't need lowercase paths in Google Drive
		Boolean trashed = node.getFile().getLabels().getTrashed();
		if (trashed != null && trashed == true) {
			entry.setDeleted(true);
			long deletedTime = node.getFile().getModifiedDate().getValue();
			entry.setDeletionDate(new Date(deletedTime));
		}
		else {
			entry.setDeleted(false);
		}
		entry.setDir(isFolder(node.getFile()));
		boolean shared = node.getFile().getShared() != null && node.getFile().getShared() == true ? true : false;
		entry.setShared(shared);
		long createdTime = node.getFile().getCreatedDate().getValue();
		entry.setCreationDate(new Date(createdTime));

		// NOTE: This is wrong, but this is a workaround. We assume that the last modifying user created or deleted this file.
		String userId = node.getFile().getLastModifyingUser().getPermissionId();
		if (entry.isDeleted()) {
			entry.setDeletionUid(userId);
			entry.setDeletionUserName(node.getFile().getLastModifyingUserName());
		}
		else {
			entry.setCreationUid(userId);
			entry.setCreationUserName(node.getFile().getLastModifyingUserName());
		}
		if (node.getChildNodes() != null) {
			for (FileNode childNode : node.getChildNodes()) {
				getFileEntryListRec(childNode, new StringBuffer(currentPath).append("/"), fileList);
			}
		}
	}
	
	private boolean isFolder(File file) {
		return file != null && GOOGLE_FOLDER_TYPE.equals(file.getMimeType()); 
	}
	
	/**
	 * Builds a FileTree out of a list of changes; using the "built-in" parent links, not calling new children again. 
	 */
	private FileTree buildTree(List<Change> changeList) {
		Map<String, List<File>> childrenMap = new HashMap<String, List<File>>();
		// Init, create root node in tree
		FileTree tree = new FileTree();
		FileNode rootNode = new FileNode();
		rootNode.setFileId("root");
		tree.setRoot(rootNode);
		List<FileNode> rootChildNodes = new ArrayList<FileNode>();
		rootNode.setChildNodes(rootChildNodes);
		Queue<FileNode> queue = new LinkedList<FileNode>();
		// Constructing the first level of tree and making a children hahstable
		for (Change change : changeList) {
			if (change.getFile() == null) {
				// TODO: this file has been "deleted forever"!
				// TODO: if this file is in the database, probably just remove it, never show it!
				continue;
			}
			if (change.getFile().getParents() == null || change.getFile().getParents().size() == 0) {
				continue;
			}
			ParentReference parent = change.getFile().getParents().get(0); 
			Boolean isRoot = parent.getIsRoot(); 
			if (isRoot != null && isRoot == true) {
				FileNode rootChild = new FileNode();
				rootChild.setFile(change.getFile());
				rootChild.setFileId(change.getFile().getId());
				rootChildNodes.add(rootChild);
				queue.add(rootChild);
			}
			// Building children hash table:
			// NOTE: Assume only one parent for each file
			if (!childrenMap.containsKey(parent.getId())) {
				childrenMap.put(parent.getId(), new ArrayList<File>());
			}
			childrenMap.get(parent.getId()).add(change.getFile());			
		}
		// BFS, constructing tree
		Map<String, Boolean> visited = new HashMap<String, Boolean>();
		while (queue.peek() != null) {
			FileNode node = queue.remove();
			if (visited.containsKey(node.getFileId())) {
				continue;
			}
			visited.put(node.getFileId(), true);
			List<File> children = childrenMap.get(node.getFileId()); // NOTE: Actually, this makes sense only for folders
			if (children != null) {
				List<FileNode> childNodes = new ArrayList<FileNode>();
				for (File file: children) {
					FileNode childNode = new FileNode();
					childNode.setFileId(file.getId());
					childNode.setFile(file);
					childNodes.add(childNode);
					queue.add(childNode);
				}
				node.setChildNodes(childNodes);
			}
		}
		return tree;	
	}
		
	private Credential buildGoogleCredential() {
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();
		GoogleCredential credential  = new GoogleCredential.Builder()
		.setJsonFactory(jsonFactory)
		.setTransport(httpTransport)
		.setClientSecrets(googleDriveSettings.clientID, googleDriveSettings.clientSecret)
		.build();
		return credential;
	}

	private Credential getAndStoreCredential(User user) {
		Credential credential = getFromSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY);
		if (credential == null) {
			if (user == null) {
				return null;
			}
			credential = getCredential(user);
			storeInSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY, credential);
		}
		return credential;
	}
	
	private Credential getCredential(User user) {
		Credential credential =  buildGoogleCredential();
		credential.setAccessToken(user.getAccessKey());
		credential.setRefreshToken(user.getRefreshToken());
		return credential;
	}
	
	private Drive getServiceInstance() {
		Drive googleDriveService = getFromSession(Constants.DRIVE_SESSION_KEY);
		if (googleDriveService == null) {
			User user = getFromSession(Constants.USER_SESSION_KEY);
			if (user == null) {
				String message = "User not in session. Cannot create Drive service. Will fail.";
				System.out.println(message);
				throw new RuntimeException(message);
			}
			googleDriveService = createServiceInstance(user);
			storeInSession(Constants.DRIVE_SESSION_KEY, googleDriveService);
		}
		return googleDriveService;
	}
	
	private Drive createServiceInstance(User user) {	
		Credential credential = getAndStoreCredential(user);
		if (credential == null) {
			System.out.println("Cannot get user's credential. Will fail.");
			throw new RuntimeException("Cannot get user's credential. Will fail.");
		}
		return createServiceInstance(credential);
	}
	
	private Drive createServiceInstance(Credential credential) {
		HttpTransport httpTransport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();
		Drive service = new Drive.Builder(httpTransport, jsonFactory, credential)
		.setApplicationName(googleDriveSettings.appName).build();
		return service;
	}
	
	@Override
	public JSONArray revisions(String path, FileManager fileManager) {
		NoWarningJSONArray result = new NoWarningJSONArray();
		User user = getFromSession(Constants.USER_SESSION_KEY);
		Drive googleDriveService = getServiceInstance();
		FileEntry fileEntry = fileManager.getFileEntry(user.getUid(), path);
		String fileId = fileEntry.getFileId();
		try {
			List<Revision> revisionList =  googleDriveService.revisions().list(fileId).execute().getItems();
			for (Revision revision: revisionList) {
				NoWarningJSONObject jsonEntry = new NoWarningJSONObject();
				jsonEntry.put("filename", fileEntry.getFilename());
				jsonEntry.put("rev", revision.getId());
				jsonEntry.put("path", path);
				long timeValue = revision.getModifiedDate().getValue();
				Date date = new Date(timeValue);
				jsonEntry.put("modified", DateFormatter.formatDate(date));
				// this is semantically wrong. We are interested whether the revision is deleted, not the file.
				jsonEntry.put("isDeleted", fileEntry.isDeleted()); 
				jsonEntry.put("link", "/download" + path + "?rev" + revision.getId());
				result.add(jsonEntry);
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("Will return an empty revision list.");
		}
		return result;
	}

	@Override
	public JSONObject createFolder(String filePath, User user, FileManager fileManager) {
		Drive googleDriveService = getServiceInstance();
		String parentPath = FilePath.getParentPath(filePath);
		String fileName = FilePath.getFileName(filePath);
		File file = new File();
		file.setTitle(fileName);
		file.setMimeType(GOOGLE_FOLDER_TYPE);
		String parentId = getParentId(fileManager, user, parentPath);
		setParent(file, parentId);
		try {
			System.out.println("going to insert: " + file.getTitle() + " to folder: " + parentId);
			Insert insertRequest =	googleDriveService.files().insert(file); 
			file = insertRequest.execute();
			NoWarningJSONObject result = new NoWarningJSONObject();
			result.put("filename", fileName);
			result.put("path", filePath);
			result.put("isDir", true);
			result.put("fileId", file.getId());
			return result;
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	private String getParentId(FileManager fileManager, User user, String parentPath) {
		FileEntry parent = fileManager.getFileEntry(user.getUid(), parentPath);
		String parentId = null;
		if (parent == null) {
			parentId = "root"; // Default Google Drive folder ID
		}
		else {
			parentId = parent.getFileId();
			if (parentId == null) {
				parentId = "root";
			}
		}
		return parentId;
	}

	private void setParent(File file, String parentId) {
		List<ParentReference> parents = new ArrayList<ParentReference>();
		ParentReference parent = new ParentReference();
		parent.setId(parentId); 
		parents.add(parent);
		file.setParents(parents);
	}

	@Override
	public CopyOrMoveResult copyOrMoveFile(CopyMoveAction action, String fromPath, String toPath, FileManager fileManager) {
		Drive googleDriveService = getServiceInstance();
		User user = getFromSession(Constants.USER_SESSION_KEY);
		return copyOrMoveFile(action, user.getUid(), fromPath, toPath, fileManager, googleDriveService);
	}

	@Override
	public CopyOrMoveResult moveImpersonated(String oldPath, String newPath, String initiatorUid, FileManager fileManager) {
		Credential originalCredential = getFromSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY);
		User impersonator = userManager.getUser(initiatorUid);
		Credential impersonatorCredential = getCredential(impersonator);
		Drive googleDriveService = createServiceInstance(impersonatorCredential);
		CopyOrMoveResult result = copyOrMoveFile(CopyMoveAction.MOVE, initiatorUid, oldPath, newPath, fileManager, googleDriveService);
		if (originalCredential != null) {
			storeInSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY, originalCredential);
		}
		return result;
	}
	
	private CopyOrMoveResult copyOrMoveFile(CopyMoveAction action, String userUid, String fromPath, String toPath, FileManager fileManager, Drive googleDriveService) {
		CopyOrMoveResult result = new CopyOrMoveResult();
		Map<String, String> pathToRev = new HashMap<String, String>();
		result.pathToRevMap = pathToRev;
		FileEntry fileEntry = fileManager.getFileEntry(userUid, fromPath);
		String fileId = fileEntry.getFileId();
		String parentToPath = FilePath.getParentPath(toPath);
		FileEntry parent = fileManager.getFileEntry(userUid, parentToPath);
		String parentId = parent.getFileId();
		
		if (action == CopyMoveAction.COPY) {
			System.out.println("copying " + fromPath + " to " + parentToPath);
			try {
				Map<String, String> pathToFileIdMap = new HashMap<String, String>();
				createFolderRec(userUid, fileEntry, parentToPath, parentId, fileManager, pathToFileIdMap, googleDriveService);
				//result.setFileId(copiedFile.getId());
				result.setPathToFileIdMap(pathToFileIdMap);
				result.setNewPath(toPath);
			}
			catch (IOException ex) {
				ex.printStackTrace();
				result.setError(ex.toString());
			}
		}
		else if (action == CopyMoveAction.MOVE) {
			System.out.println("moving " + fromPath + " to " + parentToPath);
			try {
				File file = googleDriveService.files().get(fileId).execute(); // can fail.
				setParent(file, parentId);
				file = googleDriveService.files().update(fileId, file).execute(); // can fail
				result.setNewPath(toPath);
			}
			catch (IOException ex) {
				ex.printStackTrace();
				result.setError(ex.toString());
			}
		}
		return result;
	}
		
	private void createFolderRec(String userUid, FileEntry fileEntry, String parentToPath, String parentId, FileManager fileManager, 
			Map<String, String> pathToFileIdMap, Drive googleDriveService) throws IOException {
		if (fileEntry.isDir()) {
			File newFile = new File();
			newFile.setTitle(fileEntry.getFilename());
			newFile.setMimeType(GOOGLE_FOLDER_TYPE);
			setParent(newFile, parentId);
			newFile = googleDriveService.files().insert(newFile).execute();
			parentId = newFile.getId();
			String newPath = FilePath.combine(parentToPath, fileEntry.getFilename());
			System.out.println("folder, newPath: " + newPath + "id: " + parentId);
			pathToFileIdMap.put(newPath, parentId);
			FileData children = fileManager.listChildrenForPath(userUid, fileEntry.getPath(), false, false);
			for (FileEntry childEntry : children.getEntries()) {
				createFolderRec(userUid, childEntry, newPath, parentId, fileManager, pathToFileIdMap, googleDriveService);
			}
		}
		else { // regular file
			File copiedFile = new File();
			setParent(copiedFile, parentId);
			copiedFile = googleDriveService.files().copy(fileEntry.getFileId(), copiedFile).execute();
			String newPath = FilePath.combine(parentToPath, fileEntry.getFilename());
			System.out.println("file, newPath: " + newPath + "id: " + copiedFile.getId());
			pathToFileIdMap.put(newPath, copiedFile.getId());
			System.out.println("Old id: " + fileEntry.getFileId() + ", new id: " + copiedFile.getId());
		}
	}

	@Override
	public JSONObject undelete(FileEntry fileEntry, String userUid,	FileManager fileManager) {
		NoWarningJSONObject result = new NoWarningJSONObject();
		Drive googleDriveService = getServiceInstance();
		String fileId = fileEntry.getFileId();
		try {
			googleDriveService.files().untrash(fileId).execute();
		}
		catch (IOException ex) {
			ex.printStackTrace();
			result.put("error", ex.toString());
		}
		return result;
	}

	@Override
	public NoWarningJSONObject upload(String fullFilePath, String parentPath, String fileName, FileManager fileManager) {
		System.out.println("Entered GoogleDrive.upload");
		User user = getFromSession(Constants.USER_SESSION_KEY);
		Drive googleDriveService = getServiceInstance();
		File body = new File();
		body.setTitle(fileName);
		String contentType = getContentType(fullFilePath);
		body.setMimeType(contentType);
		String parentId = getParentId(fileManager, user, parentPath);
		setParent(body, parentId);
		
		java.io.File fileContent = new java.io.File(fullFilePath);
		FileContent mediaContent = new FileContent(contentType, fileContent);
		File file = null;
		try {
			file = googleDriveService.files().insert(body, mediaContent).execute();
		} catch (IOException e) {
			e.printStackTrace();
			return buildUploadResult(fileName, parentPath, null, false, e.toString());
		}
		System.out.println("Will return from GoogleDrive.upload");
		return buildUploadResult(fileName, parentPath, file.getId(), true, "");
	}

	// TODO: this method is repeated in Dropbox and GoogleDrive. Bad!
	private NoWarningJSONObject buildUploadResult(String fileName, String parentPath, String fileId, boolean success, String errorMessage) {
		NoWarningJSONObject result = new NoWarningJSONObject();
		result.put("filename", fileName);
		result.put("parentPath", parentPath);
		if (fileId != null) {
			result.put("fileId", fileId);
		}
		result.put("success", success);
		if (!success) {
			result.put("errorMessage", errorMessage);
		}
		return result;
	}
	
	private String getContentType(String fullFilePath) {
		String contentType = null;
		Path path = Paths.get(fullFilePath);
		try {
			contentType = java.nio.file.Files.probeContentType(path);
		} catch (IOException e) {
			e.printStackTrace();
			// contentType will stay null
		}
		return contentType;
	}
	
	/**
	 * Exchanges the code received from Google OAuth for a credential. Stores Credential in session.
	 * Gets user information from credential. Returns the user. 
	 * This method is only relevant for GoogleDrive.
	 * @param code Code received from Google OAuth
	 * @return The authenticated user
	 */
	public User finishAuthentication(String code) throws IOException {
		GoogleAuthorizationCodeFlow flow = getFlow();
		GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
				.setRedirectUri(googleDriveSettings.redirectURI).execute();
		Credential credential = buildGoogleCredential();	
		credential.setFromTokenResponse(tokenResponse);
		storeInSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY, credential);
		Drive googleDriveService = createServiceInstance(credential);
		storeInSession(Constants.DRIVE_SESSION_KEY, googleDriveService);
		return getUser(credential);
	}
	
	private User getUser(Credential credential) {
		Userinfo userinfo = getUserInfo(credential);
		String userId = userinfo.getId();
		String userEmail = userinfo.getEmail();
		String userName = userinfo.getName();
		User user = new User();
		user.setUid(userId);
		user.setDisplayName(userName);
		user.setEmail(userEmail);
		user.setAccessKey(credential.getAccessToken());
		user.setRefreshToken(credential.getRefreshToken());
		user.setProvider(Constants.Provider.GOOGLE);
		return user;
	}
	
	private Userinfo getUserInfo(Credential credential) {
		Oauth2 userInfoService  = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credential).build();
		Userinfo userInfo = null;
		try {
			userInfo = userInfoService.userinfo().get().execute();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		if (userInfo != null && userInfo.getId() != null) {
			return userInfo;
		}
		else {
			throw new RuntimeException("No user ID");
		}
	}
	
	private void storeInSession(String key, Object item) {
		if (this.session != null) {
			System.out.println("Storing " + key + " in session.");
			this.session.attribute(key, item);
		}
		else {
			System.out.println("Session is null, can't store " + key);
		}
	}
	
	private <T> T getFromSession(String key) {
		if (this.session != null) {
			System.out.println("Getting " + key + " from session.");
			return session.attribute(key);
		}
		else {
			System.out.println("Session is null, cannot get " + key);
			return null;
		}
	}
	
	private void removeFromSession(String key) {
		if (this.session != null) {
			this.session.removeAttribute(key);
		}
	}
	
	/**
	 * NOTE: Assume that by the time we call this method, the session has been set and is not null;
	 */
	public JSONObject tryListFiles(String uid, String serverName, int port, String path, FileManager fileManager) {
		if (uid != null) {
			System.out.println("UID is not null, provider is google");
			User user = userManager.getUser(uid);
			if (user != null) {
				getAndStoreCredential(user); // gets credential from session, if not, then from user, saves in session
				storeInSession(Constants.USER_SESSION_KEY, user);
				return fileManager.listFiles(path, false, user, this);
			}
			else {
				return getAuthenticationResponse();
			}
		}
		else {
			System.out.println("UID is null, provider is google");
			Credential credential = getFromSession(Constants.GOOGLE_CREDENTIAL_SESSION_KEY);
			if (credential == null) {
				System.out.println("credential is null");
				return getAuthenticationResponse();
			}
			else {
				User user = getFromSession(Constants.USER_SESSION_KEY);
				if (user != null) {
					return fileManager.listFiles(path, false, user, this);
				}
				else {
					System.out.println("Unsupported state.");
					throw new UnsupportedOperationException("not implemented");
				}
			}
		}
	}

	@Override
	public void shareFolder(User owner, String uid, String path, FileManager fileManager) {
		FileEntry folderToShare = fileManager.getFileEntry(owner.getUid(), path);
		if (folderToShare!= null) {
			String fileId = folderToShare.getFileId();
			User partner = userManager.getUser(uid);
			Drive googleDriveService = getServiceInstance();
			Permission newPermission = new Permission();
			newPermission.setType("user");
			newPermission.setValue(partner.getEmail());
			newPermission.setRole("writer");
			try {
				Permissions.Insert insertRequest = googleDriveService.permissions().insert(fileId, newPermission);
				newPermission = insertRequest.execute();
				System.out.println("new permission. id: " + newPermission.getId() + "; type: " + newPermission.getType() + 
						", value: " + newPermission.getValue() + ", role: " + newPermission.getRole());
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			System.out.println("Cannot find folder at " + path + " for user " + owner.getDisplayName());
		}
	}
	
	/**************************************************Debugging helpers*****************************/
	
	/**
	 * Prints all files with complete paths
	 */
	private void print2(FileTree tree) {
		FileNode root = tree.getRoot();
		List<FileNode> childNodes = root.getChildNodes();
		if (childNodes != null) {
			for (FileNode childNode : root.getChildNodes()) {
				printRec2(childNode, new StringBuffer("/"));
			}
		}
	}
	private void printRec2(FileNode node, StringBuffer pathBuffer) {
		System.out.print(pathBuffer.toString() + node.getFile().getTitle());
		Boolean trashed = node.getFile().getLabels().getTrashed();
		if (trashed != null && trashed == true) { 
			System.out.println(" -- Trashed");
		}
		else {
			System.out.println();
		}
		if (node.getChildNodes() != null) {
			for (FileNode childNode: node.getChildNodes()) {
				printRec2(childNode, new StringBuffer(pathBuffer.toString()).append(node.getFile().getTitle()).append("/"));
			}
		}
	}
	private void getFileMetadataByID(Drive service) throws IOException {
		System.out.println("getting file by metadata");
		String id = "1jg__xY2KArcLyW5kamsupXpKutRZt__ysmcBPP1EV64";
		
		Files.Get get = service.files().get(id);
		File file = get.execute();
		System.out.println(file.getTitle());
	}
	private void listChildren(Drive service) throws IOException {		
		//String folderId = "1bROFAtF1keI8J-NuqcowUSNyeHBkXr6aESKlsK0RGt0";
		String folderId = "root";
		Children.List listRequest = service.children().list(folderId);
		int i = 0;
		do {
			ChildList children = listRequest.execute();
			for (ChildReference child : children.getItems()) {
				System.out.println(child.getId() + ": " + child.toPrettyString());
			}
			listRequest.setPageToken(children.getNextPageToken());
			System.out.println("Iteration: " + ++i);
		}
		while (listRequest.getPageToken() != null && listRequest.getPageToken().length() > 0);
	}

}
