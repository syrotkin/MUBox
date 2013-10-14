package server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.mongodb.BasicDBList;

import server.data.CopyOrMoveResult;
import server.data.DatabaseManager;
import server.data.DeleteResult;
import server.data.FileManager;
import server.data.NoWarningJSONArray;
import server.data.NoWarningJSONObject;
import server.data.RenameResult;
import server.data.SharedFolderManager;
import server.data.UserManager;
import server.data.VotingManager;
import server.enums.CopyMoveAction;
import server.enums.MoveAction;
import server.model.FileActivity;
import server.model.FileEntry;
import server.model.SharedFolder;
import server.model.User;
import server.model.VotingInput;
import server.settings.Settings;
import server.settings.SettingsManager;
import server.utils.FilePath;
import spark.*;

/**
 * Main class that starts the server. 
 * @author soleksiy
 *
 */
public class MUBoxStart {
	
	private static int getPort(String[] args) {
		int result = -1;
		for (int i = 0; i < args.length; i++) {
			if ("--port".equals(args[i]) && i < args.length - 1) {
				try {
					result = Integer.parseInt(args[i+1]);
				} catch (NumberFormatException ex) {
					result = -1;
				}
			}
		}
		if (result == -1) {
			return 9090; // hardcoded
		}
		return result;
	}

	/**
	 * Entry point of the application. Starts the Spark server, sets the port, establishes the server routes that react to the user input.
	 * @param args Arguments. Accepts port number --port <number>. By default, port is 9090.
	 */
	public static void main(String[] args) {
		SettingsManager settingsManager = new SettingsManager();
		final Settings settings = settingsManager.readProperties();
		final DatabaseManager dbManager = DatabaseManager.getInstance();
		final UserManager userManager = new UserManager(dbManager);
		final SharedFolderManager sharedFolderManager = new SharedFolderManager(dbManager);
		final FileManager fileManager = new FileManager(dbManager, userManager, sharedFolderManager);
		final ChangeManager changeManager = new ChangeManager(sharedFolderManager, fileManager);
		final VotingManager votingManager = new VotingManager(dbManager, userManager);
		final CloudFactory cloudFactory = new CloudFactory(userManager, settings);

		final VotingEvaluator votingEvaluator = new VotingEvaluator(votingManager, changeManager, cloudFactory, fileManager);
		votingEvaluator.start();
				
		Spark.setPort(getPort(args));
		
		Spark.staticFileRoute("/client/app"); // app has to be under SparkTest/bin
		// e.g. http://localhost:<port>/version.txt -- corresponds to ...SparkTest/bin/angular/app/version.txt	
		//String currentDirectory = System.getProperty("user.dir");
		//System.out.println("current dir: " + currentDirectory);
		// by default, current directory is: C:\dev\test\SparkTest
			
		setUpActivityRoutes(dbManager, userManager);
		
		setUpRevisiontRoutes(cloudFactory, fileManager);
		
		setUpUpload(changeManager, fileManager, cloudFactory);	
		
		setUpHome2(userManager, fileManager, cloudFactory);
		
		setUpDownload(cloudFactory, fileManager);
		
		Spark.get(new Route("/settings") {
			@Override
			public Object handle(Request request, Response response) {
				NoWarningJSONObject result = new NoWarningJSONObject();
				result.put("disableActivityView", settings.clientSideSettings.disableActivityView);
				result.put("disableShadow", settings.clientSideSettings.disableShadow);
				result.put("disableVoting", settings.clientSideSettings.disableVoting);
				return result;
			}
		});
		
		// This is only relevant for Google Drive
		final String oauth2CallbackRoute = "/oauth2callback";
		Spark.get(new Route(oauth2CallbackRoute) {
			@Override
			public Object handle(Request request, Response response) {
				System.out.println("INSIDE OAuth2Callback");
				String code = request.queryParams("code");
				if (code != null) {
					try {
						GoogleDrive cloud = new GoogleDrive(userManager, settings.googleDriveSettings);
						cloud.setServerSession(request.session());
						User user = cloud.finishAuthentication(code);
						System.out.println("got user: " + user.getUid() + ", name: " + user.getDisplayName());
						storeInSession(request.session(), user);
						storeInSession(request.session(), Constants.Provider.GOOGLE);
						userManager.saveUser(user);
						String requestUrl = request.raw().getRequestURL().toString();
						String requestUri = request.raw().getRequestURI();
						printDebugInfo(request);
						String newUrl = requestUrl.replaceFirst(requestUri, "/muboxindex.html#/?provider=google&uid=" + user.getUid());	

						// 1. in case we redirect to /muboxindex.html from Google
						//JSONObject result = new JSONObject();
						//result.put("uid", user.getUid());
						//return result;
												
						// 2. in case we redirect to /oauth2callback from Google
						System.out.println("redirecting to " + newUrl);
						response.raw().sendRedirect(newUrl);						
						return null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}
				else {
					System.out.println("/oauth2callback accessed without code parameter");
					return "";
				}	
			}

			private void printDebugInfo(Request request) {
				String serverName = request.raw().getServerName();
				int serverPort = request.raw().getServerPort();
				String requestUri = request.raw().getRequestURI();
				String requestUrl = request.raw().getRequestURL().toString();
				String servletPath = request.raw().getServletPath();
				System.out.println("name: " + serverName + "; port: " + serverPort + "; requestURI: " + requestUri + "; requestURL: " + requestUrl +"; servletPath: " + servletPath);
			}
			
		});
		
		Spark.post(new Route("/vote") {
			@Override
			public Object handle(Request request, Response response) {	
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				Set<String> allParams = request.queryParams();
				VotingInput votingInput = new VotingInput();
				String parentPath = null;
				for (String param: allParams) {
					System.out.println("inside vote -- param: " + param);
					JSONObject jsonParam = (JSONObject)JSONValue.parse(param);
					votingInput.setUid((String)jsonParam.get("uid"));
					votingInput.setAccepted((boolean)jsonParam.get("accepted"));
					votingInput.setVotingID((long)jsonParam.get("votingID"));
					parentPath = (String)jsonParam.get("parentPath");
					Boolean cancel = (Boolean)jsonParam.get("cancel");
					votingInput.setCancel(cancel == null ? false : cancel.booleanValue());
					break;
				}
				boolean success = votingManager.vote(votingInput, cloudStorage, changeManager, fileManager);
				System.out.println("vote success: " + success);
				User user = getUserFromSession(request.session());
				JSONObject responseData = fileManager.listFiles(parentPath == null ? "/" : parentPath, false, user, cloudStorage);
				if (responseData == null) {
					System.out.println("vote responseData is null");
				}
				else {
					System.out.println(responseData.get("fileList"));
				}
				return responseData;
			}
		});
		
		Spark.get(new Route("/getsharedfolderinfo/:id") {
			@Override
			public Object handle(Request request, Response response) {
				long id = Long.parseLong(request.params(":id"));
				List<SharedFolder> list = sharedFolderManager.getSharedFolders(id);
				NoWarningJSONObject result = new NoWarningJSONObject();
				if (list.size() != 0) {
					SharedFolder folder = list.get(0);
					result.put("votingScheme", folder.getVotingScheme());
					if (folder.getPercentageUsers() != -1) {
						result.put("percentage", folder.getPercentageUsers());
					}
					if (folder.getPeriodInMinutes() != -1) {
						result.put("timeUnit", "1");
						result.put("numTimeUnits", folder.getPeriodInMinutes());
					}
				}
				return result;
			}
		});
		
		Spark.post(new Route("/sharefolder") { // used to be: owner, folder, uid_list. Now: { hash:hash, users (incl.owner): [{uid, path},...] }
			@Override
			public Object handle(Request request, Response response) {
				List<SharedFolder> sharedFolders = new ArrayList<SharedFolder>();
				Set<String> allParams= request.queryParams();
				User owner = getUserFromSession(request.session());
				String schemeName = null;
				int percentage = -1;
				int periodInMinutes = -1;
				String parentPath = null;
				for (String param : allParams) {
					System.out.println("Param: " + param);
					JSONObject jsonParam = (JSONObject)JSONValue.parse(param);
					JSONArray foldersAsJSON = (JSONArray)jsonParam.get("users");
					schemeName = (String)jsonParam.get("scheme");
					System.out.println("schemeName: " + schemeName);
					percentage = (int)(long)jsonParam.get("percentage");
					System.out.println("percentage: " + percentage);
					periodInMinutes = (int)(long)jsonParam.get("periodInMinutes");
					System.out.println("periodInMinutes: " + periodInMinutes);

					CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
					for (Object element : foldersAsJSON) {
						
						JSONObject current = (JSONObject)element;
						String uid = (String)current.get("uid");
						String path = (String)current.get("path");	
						parentPath = FilePath.getParentPath(path);
						if (!owner.getUid().equals(uid)) {
							cloudStorage.shareFolder(owner, uid, path, fileManager);
						}
						SharedFolder folder = new SharedFolder();
						folder.setUid(uid);
						folder.setPath(path);
						folder.setOwner(owner.getUid());
						folder.setVotingScheme(schemeName);
						folder.setPercentageUsers(percentage);
						folder.setPeriodInMinutes(periodInMinutes);
						sharedFolders.add(folder);
						System.out.println("sharedFolder: " + folder);
					}
					fileManager.insertSharedFolders(sharedFolders);
					break; // because we assume there is only one JSON POST parameter
				}
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				return fileManager.listFiles(parentPath != null ? parentPath : "/", false, owner, cloudStorage);
			}
		});

		Spark.post(new Route("/copyfile") {
			@Override
			public Object handle(Request request, Response response) {
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				CopyOrMoveResult result = handleCopyOrMove(CopyMoveAction.COPY, request, cloudStorage, changeManager, fileManager);
				if (result.getError() != null) {
					return result;
				}
				User user = getUserFromSession(request.session());
				return fileManager.listFiles(result.listPath, false, user, cloudStorage);
			}
		});
				
		Spark.post(new Route("/movefile") {
			@Override
			public Object handle(Request request, Response response) {
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				CopyOrMoveResult result = handleCopyOrMove(CopyMoveAction.MOVE, request, cloudStorage, changeManager, fileManager);
				if (result.getError() != null) {
					return result;
				}
				User user = getUserFromSession(request.session());
				return fileManager.listFiles(result.listPath, false, user, cloudStorage);
			}
		});
				
		Spark.get(new Route("/listusers") {
			@Override
			public Object handle(Request request, Response response) {
				String provider = request.queryParams("provider");
				return userManager.listUsers(provider);
			}
		});
		
		Spark.get(new Route("/listproviders") {
			@Override
			public Object handle(Request request, Response response) {
				return userManager.listProviders();
			}
		});
		
		Spark.get(new Route("/logout") {
			@Override
			public Object handle(Request request, Response response) {
				
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				cloudStorage.logout();
				response.redirect("/index.html");
				return null;
			}
		});
				
		// For debugging -- Dropbox is hardcoded
		Spark.get(new Route("/delta") {
			@Override
			public Object handle(Request request, Response response) {
				Dropbox dropbox = new Dropbox(userManager, settings.dropboxSettings);
				dropbox.setServerSession(request.session());
				return dropbox.delta(fileManager);
			}
		});
		
		final String syncPrefix = "/sync";
		Spark.get(new Route(syncPrefix + "/*") { // /sync/*
			@Override
			public Object handle(Request request, Response response) {
				String fullPath = request.pathInfo();
				String path = fullPath.substring(syncPrefix.length());
				User user = getUserFromSession(request.session());
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				return fileManager.listFiles(path, true, user, cloudStorage);
			}
		});
	
		final String directDownloadPrefix = "/directdownload";
		Spark.get(new Route(directDownloadPrefix + "/*") { // /directdownload/*
			@Override
			public Object handle(Request request, Response response) {
				try {
					CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
					String fullPath = request.pathInfo();
					String path = fullPath.substring(directDownloadPrefix.length());
					JSONObject result = cloudStorage.getLink(path, fileManager);
					String url = (String)result.get("url");
					if (url != null) {
						System.out.println("download url: " + url);
						response.redirect(url);
					}
					else {
						System.out.println("No URL returned from getLink.");
					}
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
		});

		final String newfolderPrefix = "/newfolder";
		Spark.get(new Route(newfolderPrefix + "/*") {
			@Override
			public Object handle(Request request, Response response) {
				CloudStorage cloud = cloudFactory.getCloudStorage(request.session());
				String fullPath = request.pathInfo();
				String path = fullPath.substring(newfolderPrefix.length());
				User user = getUserFromSession(request.session());
				JSONObject newFolder = cloud.createFolder(path, user, fileManager);
				String newFolderPath = (String)newFolder.get("path");
				String rev = null;
				String fileId = null;
				rev = (String)newFolder.get("rev");
				if (rev == null) {
					fileId = (String)newFolder.get("fileId");
				}
				changeManager.recordNewFolder(newFolderPath, rev, fileId, user.getUid());
				return fileManager.listFiles(FilePath.getParentPath(newFolderPath), false, user, cloud);
			}
		});
		
		Spark.post(new Route("/delete") {
			@Override
			public Object handle(Request request, Response response) {
				Set<String> allParams = request.queryParams();
				String path = null;
				String action = null;
				for (String param: allParams) {
					JSONObject jsonParam = (JSONObject)JSONValue.parse(param);
					path = (String)jsonParam.get("path");
					action = (String)jsonParam.get("action");
					break;
				}
				if (path == null || action == null) {
					System.out.println("error: wrong 'delete' parameters");
					return "error";
				}
				User user = getUserFromSession(request.session());				
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				if ("delete".equals(action)) {
					System.out.println("deleting: " + path);
					DeleteResult deleteResult = cloudStorage.delete(path, fileManager);
					if (deleteResult.getError() == null) {
						changeManager.recordDelete(path, user.getUid(), null, false);
					}
				}
				else if ("deleteshared".equals(action)) {
					System.out.println("delete shared: " + path);
					changeManager.deleteShared(path, user.getUid());
				}	
				else if ("suggestdelete".equals(action)) {
					System.out.println("suggest delete: " + path);
					changeManager.suggestDelete(path, user.getUid());
				}
				String parentPath = FilePath.getParentPath(path);
				return fileManager.listFiles(parentPath, false, user, cloudStorage);
			}
		});
		
		Spark.post(new Route("/notifications") {
			@Override
			public Object handle(Request request, Response response) {
				User user = getUserFromSession(request.session());
				String uid = user.getUid();
				BasicDBList votingRequests = votingManager.listVotingRequests(uid);
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				BasicDBList votingClosedNotifications = votingManager.listVotingClosedNotifications(uid, changeManager, cloudStorage);
				votingRequests.addAll(votingClosedNotifications);
				Set<String> allParams = request.queryParams();
				String parentPath = null;
				for (String param: allParams) {
					//System.out.println("notifications; param: " + param);
					JSONObject jsonParam = (JSONObject)JSONValue.parse(param);
					parentPath = (String)jsonParam.get("parentPath");
					//System.out.println("parentPath: " + parentPath);
					break;
				}
				JSONObject listFilesResult = fileManager.listFiles(parentPath == null ? "/" : parentPath, false, user, cloudStorage);
				NoWarningJSONObject result = new NoWarningJSONObject();
				result.put("fileList", listFilesResult.get("fileList"));
				result.put("notificationList", votingRequests);
				return result;
			}
		});
		
		Spark.get(new Route("/removevoting") {
			@Override
			public Object handle(Request request, Response response) {
				String votingIDParam = request.queryParams("votingid");
				long votingID = Long.parseLong(votingIDParam);
				System.out.println("removevoting, votingid: " + votingID);
				votingManager.removeVotingUsers(votingID);
				votingManager.removeVoting(votingID);
				return "great success";
			}
		}); 
		
		Spark.get(new Route("/openvotes/:uid") {
			@Override
			public Object handle(Request request, Response response) {
				String uid = request.params(":uid");
				return votingManager.listOpenVotes(uid); // it is a BSON list
			}
		});
		
		// URL similar to this: /rename/path/to/foo.txt?newname=bar.txt
		final String renamePrefix = "/rename";
		Spark.get(new Route(renamePrefix + "/*") { // rename/*
			@Override
			public Object handle(Request request, Response response) {
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				String fullPath = request.pathInfo(); // path including the prefix
				String path = fullPath.substring(renamePrefix.length());
				System.out.println("SparkStart.rename. Path is: " + path);
				String newName = request.queryParams("newname");
				String parentPath = FilePath.getParentPath(path);
				System.out.println("parentPath :" + parentPath);
				String newPath = FilePath.combine(parentPath, newName);
				RenameResult result = cloudStorage.rename(path, newPath, fileManager);
				User user;
				if (result.getError() == null) {
					user = getUserFromSession(request.session());
					changeManager.recordMove(path, newPath, user.getUid(), MoveAction.RENAME, result.pathToRevMap, null, false);
				}
				user = getUserFromSession(request.session());
				return fileManager.listFiles(parentPath, false, user, cloudStorage);
			}
		});
				
		final String renameSharedPrefix = "/renameshared";
		Spark.get(new Route(renameSharedPrefix + "/*") { // renameshared/*
			@Override
			public Object handle(Request request, Response response) {
				System.out.println("enter renameshared");
				String fullPath = request.pathInfo();
				String path = fullPath.substring(renameSharedPrefix.length());
				String newName = request.queryParams("newname");
				String parentPath = FilePath.getParentPath(path);
				String newPath = FilePath.combine(parentPath, newName);
				User user = getUserFromSession(request.session());
				changeManager.renameShared(path, newPath, user.getUid());
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				return fileManager.listFiles(parentPath, false, user, cloudStorage);
			}
		});
		
		final String suggestRenamePrefix = "/suggestrename";
		Spark.get(new Route(suggestRenamePrefix + "/*") {
			@Override
			public Object handle(Request request, Response response) {
				System.out.println("enter suggestrename");
				String fullPath = request.pathInfo();
				String path = fullPath.substring(suggestRenamePrefix.length());
				String newName = request.queryParams("newname");
				String parentPath = FilePath.getParentPath(path);
				String newPath = FilePath.combine(parentPath, newName);
				User user = getUserFromSession(request.session());
				changeManager.suggestRename(path, newPath, user.getUid());
				System.out.println("parentPath: " + parentPath);
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				return fileManager.listFiles(parentPath, false, user, cloudStorage);
			}
		});
		
		final String restorePrefix = "/restore";
		Spark.get(new Route(restorePrefix + "/*") {
			@Override
			public Object handle(Request request, Response response) {
				String fullPath = request.pathInfo(); // this is the path including the delete prefix
				String path = fullPath.substring(restorePrefix.length());
				User userFromSession = request.session().attribute(Constants.USER_SESSION_KEY);
				String userUid = userFromSession.getUid();
				//String userUid = request.session().attribute(Constants.USER_UID_SESSION_KEY);
				FileEntry fileEntry = fileManager.getFileEntry(userUid, path);
				String creationAction = fileEntry.getCreationAction();
				String deletionAction = fileEntry.getDeletionAction();
				// Valid choices:
				// creationAction == "move" || creationAction == "rename" || deletionAction == "delete"
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				if ("delete".equals(deletionAction)) { // NOTE: undelete takes priority over unrename and unmove
					System.out.println("restore -- undelete");
					JSONObject result = cloudStorage.undelete(fileEntry, userUid, fileManager);
					if (result.get("error") == null) {
						changeManager.recordUndelete(path, userUid);
					}
					User user = getUserFromSession(request.session());
					return fileManager.listFiles(FilePath.getParentPath(path), false, user, cloudStorage);
				}
				else if ("move".equals(creationAction)) {
					System.out.println("restore -- unmove");
					String oldPath = FilePath.combine(fileEntry.getOldParent(), fileEntry.getOldFileName());
					CopyOrMoveResult result = cloudStorage.copyOrMoveFile(CopyMoveAction.MOVE, path, oldPath, fileManager);
					if (result.getNewPath() != null) { // TODO: this is a hack, heuristic, careful with what the result looks like
						changeManager.recordMove(path, oldPath, userUid, MoveAction.RESTORE, result.pathToRevMap, null, false);
					}
				}
				else if ("rename".equals(creationAction)) {
					System.out.println("restore -- unrename");
					String oldPath = FilePath.combine(fileEntry.getOldParent(), fileEntry.getOldFileName());
					RenameResult result = cloudStorage.rename(path, oldPath, fileManager);
					String error = result.getError(); 
					if (error == null) {
						changeManager.recordMove(path, oldPath, userUid, MoveAction.RESTORE, result.pathToRevMap, null, false);
					}
					User user = getUserFromSession(request.session());
					return fileManager.listFiles(FilePath.getParentPath(path), false, user, cloudStorage);					
				}
				// else: should not be allowed to restore (for now)
				return "restored";
			}
		});

	} // end main()
	
	

	private static void setUpRevisiontRoutes(final CloudFactory cloudFactory, final FileManager fileManager) {
		final String revisionsPrefix = "/revisions";
		Spark.get(new Route(revisionsPrefix + "/*") { // /revisions/*
			@Override
			public Object handle(Request request, Response response) {
				String fullPath = request.pathInfo();
				String path = fullPath.substring(revisionsPrefix.length());
				response.type("application/json");
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				return cloudStorage.revisions(path, fileManager);
			}
		});
	}

	private static void setUpActivityRoutes(final DatabaseManager dbManager, final UserManager userManager) {
		final String activityPrefix = "/activity";
		Spark.get(new Route(activityPrefix + "/*") {
			@Override
			public Object handle(Request request, Response response) {
				String fullPath = request.pathInfo();
				String path = fullPath.substring(activityPrefix.length());
				System.out.println("Activity called: path =  " +  path);
				String userUid = request.queryParams("uid");
				if (userUid == null || "undefined".equals(userUid)) {
					User user = request.session().attribute(Constants.USER_SESSION_KEY);
					userUid = user.getUid(); //request.session().attribute(Constants.USER_UID_SESSION_KEY);
				}
				List<FileActivity> activities = new ArrayList<FileActivity>();
				NoWarningJSONArray resultList = new NoWarningJSONArray();
				if (userUid!= null) {
					ActivityManager activityManager = new ActivityManager(dbManager, userManager);
					activities = activityManager.listActivities(path, userUid);
					for (FileActivity activity : activities) {
						resultList.add(activity);
					}
				}
				System.out.println("SparkStart, activities size: " + activities.size());
				if (activities != null && activities.size() != 0) {
					System.out.println(activities.get(0).toJSONString());
				}
				System.out.println("Will return " + resultList.toJSONString());
				NoWarningJSONObject result = new NoWarningJSONObject();
				result.put("activityList", resultList);
				String userName = (String)request.session().attribute(Constants.DISPLAY_NAME_SESSION_KEY);
				result.put("userName", userName);
				return result;
			}
		});
	}

	private static CopyOrMoveResult handleCopyOrMove(CopyMoveAction action, Request request, CloudStorage cloudStorage, ChangeManager changeManager, FileManager fileManager) {
		String fromPath = null;
		String toPath = null;
		String parentPath = null;
		String actionFromClient = null;
		Set<String> params = request.queryParams();
		for (String param : params) {
			JSONObject o = null;
			try {
				o = (JSONObject)JSONValue.parseWithException(param);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (o == null) {
				CopyOrMoveResult errorResult = new CopyOrMoveResult();
				errorResult.setError("Could not parse the input");
				return errorResult;
			}
			fromPath = (String)o.get("from");
			toPath = (String)o.get("to");
			parentPath = (String)o.get("parentPath");
			actionFromClient = (String)o.get("action");
			System.out.println("from: " + fromPath);
			System.out.println("to: " + toPath);
			System.out.println("action from client: " + actionFromClient);
			break; // assume there is only one parameter
		}
		if (fromPath == null || toPath == null) {
			CopyOrMoveResult result = new CopyOrMoveResult();
			result.setError("Could not parse client data.");
			return result;
		}
		else {
			User user = request.session().attribute(Constants.USER_SESSION_KEY);
			String userUid = user.getUid(); //(String)request.session().attribute(Constants.USER_UID_SESSION_KEY);
			if ("pasteshared".equals(actionFromClient)) {
				changeManager.moveShared(fromPath, toPath, userUid); // don't need pathToRevMap because nothing has changed in Dropbox
				CopyOrMoveResult result = new CopyOrMoveResult();
				result.listPath = parentPath;
				return result;
			}
			else if ("suggestpaste".equals(actionFromClient)) {
				changeManager.suggestMove(fromPath, toPath, userUid);
				CopyOrMoveResult result = new CopyOrMoveResult();
				result.listPath = parentPath;
				return result;
			}
			else {
				CopyOrMoveResult result = cloudStorage.copyOrMoveFile(action, fromPath, toPath, fileManager);
				result.listPath = parentPath;
				if (result.getNewPath() != null) { // TODO: this is a hack, heuristic, careful with what the result looks like
					if (action == CopyMoveAction.COPY) {
						changeManager.recordCopy(fromPath, toPath, userUid, result.pathToRevMap, result.getPathToFileIdMap());
					}
					else if (action == CopyMoveAction.MOVE) {
						if ("paste".equals(actionFromClient)) {
							changeManager.recordMove(fromPath, toPath, userUid, MoveAction.MOVE, result.pathToRevMap, null, false);
						}
					}
				}
				return result;
			}
		}
	}

	// This was not used for Dropbox because directdownload was used
	private static void setUpDownload(final CloudFactory cloudFactory, final FileManager fileManager) {
		final String downloadPrefix = "/download";
		Spark.get(new Route(downloadPrefix + "/*") { // /download/*
			@Override
			public Object handle(Request request, Response response) {
				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request.session());
				try {
					// https://github.com/perwendel/spark/issues/3
					String fullPath = request.pathInfo();
					String path = fullPath.substring(downloadPrefix.length());
					String rev = request.queryParams("rev");
					FileInfo fileInfo = cloudStorage.getFileInfo(path, rev, fileManager);
					String filePath = fileInfo.getName();
					String contentType = fileInfo.getMimeType();
					byte[] out = null;
					System.out.println(filePath);
					out =  IOUtils.toByteArray(new FileInputStream(filePath));
					response.raw().setContentType(contentType + ";charset=utf-8"); 
					final OutputStream os = response.raw().getOutputStream();
					os.write(out, 0, out.length);
					return ""; // so that we only return the file we wrote to outputstream. Not twice
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					return "FileNotFoundException: " + e.getMessage();
				} catch (IOException e) {
					e.printStackTrace();
					return "IOException: " + e.getMessage();
				}
			}
		});
	}
	
	private static void setUpHome2(final UserManager userManager, final FileManager fileManager, final CloudFactory cloudFactory) {
		final String homePrefix = "/home2";
		Spark.get(new Route(homePrefix + "/*") { // /home2/*
			@Override
			public Object handle(Request request, Response response) {
				String protocol = request.raw().getProtocol();
				String serverName = request.raw().getServerName();
				int port = request.raw().getServerPort();
				ServerInfo.setServerInfo(protocol, serverName, port);
				
				String fullPath = request.pathInfo();
				String path = fullPath.substring(homePrefix.length());
				String uid = request.queryParams("uid");

				String requestUrl = request.raw().getRequestURL().toString();
				String queryString = request.raw().getQueryString();
				System.out.println("/home2, request URL: " + requestUrl + "; queryString: " + queryString);

				CloudStorage cloudStorage = cloudFactory.getCloudStorage(request);
				// if uid is in the query string, Query string takes precendence. Treat it as a new login.
				return cloudStorage.tryListFiles(uid, serverName, port, path, fileManager);
			}
		});
	}

	private static void setUpUpload(ChangeManager changeManager, FileManager fileManager, CloudFactory cloudFactory) {
		Spark.post(new server.routes.UploadRoute("/upload", changeManager, fileManager, cloudFactory));
	}


	protected static User getUserFromSession(Session session) {
		return (User)session.attribute(Constants.USER_SESSION_KEY);
	}
	
	private static void storeInSession(Session session, String provider) {
		System.out.println("storing " + provider + " in session");
		session.attribute(Constants.PROVIDER_SESSION_KEY, provider);
	}
	
	protected static void storeInSession(Session session, User user) {
		session.attribute(Constants.USER_SESSION_KEY, user);		
	}

}







				