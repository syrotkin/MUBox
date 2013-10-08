package server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;

import server.data.NoWarningJSONObject;
import server.data.UserManager;
import server.model.User;
import spark.Session;

/**
 * Class that manages authorization for Dropbox
 * @author soleksiy
 *
 */
public class DropboxAuthorizationManager {
	
	// OleksiyApp2
	private final String APP_KEY = "aat3ba7bvoenafk";
	private final String APP_SECRET = "wsonxwq5hp66eue"; 
	private static final AccessType ACCESS_TYPE = AccessType.DROPBOX;
	private static final String CALLBACK_SUFFIX = "/muboxindex.html";
	private Session session;
	/**
	 * Sets web server session	
	 * @param session Web server (Spark) session.
	 */
	public void setServerSession(Session session) {
		this.session = session;
	}
	/**
	 * Gets the user from server session. If the user is not there: if the user UID is known, get the user from the database; if the user UID is not known, get the user UID from the Dropbox {@link WebAuthInfo}.
	 * From the user's access key and access secret form an access token pair and set it in the current Dropbox WebSession.
	 * @param userManager
	 * @param uid
	 * @return True if success, false otherwise.
	 */
	public boolean configureAccessTokens(UserManager userManager, String uid) {
		User user = getFromSession(Constants.USER_SESSION_KEY);
		if (user == null) {	
			if (uid != null) {
				user = userManager.getUser(uid);
				//accessTokenPair = new AccessTokenPair(user.getAccessKey(), user.getAccessSecret());
				this.storeInSession(user);
			}
			else {
				WebAuthInfo webAuthInfo = getFromSession(Constants.WEB_AUTH_INFO_SESSION_KEY);
				if (webAuthInfo == null) {
					return false;
				}
				try {
					uid = getWebAuthSession().retrieveWebAccessToken(webAuthInfo.requestTokenPair);
				} catch (DropboxException e) {
					// TODO: Depending on the exception, need to reauthenticate
					e.printStackTrace();
				}
				AccessTokenPair accessTokenPair = getWebAuthSession().getAccessTokenPair();
				user = createAuthenticatedUser(uid, accessTokenPair);
				this.storeInSession(user);
				userManager.saveUser(user);
			}
		}
		getWebAuthSession().setAccessTokenPair(new AccessTokenPair(user.getAccessKey(), user.getAccessSecret()));
		return true;
	}
	
	private User createAuthenticatedUser(String uid, AccessTokenPair accessTokenPair) {
		String displayName = getDisplayName();
		User user = new User();
		user.setUid(uid);
		user.setDisplayName(displayName);
		user.setAccessKey(accessTokenPair.key);
		user.setAccessSecret(accessTokenPair.secret);
		user.setProvider(Constants.Provider.DROPBOX);
		return user;
	}
	
	/**
	 * Queries Dropbox to get display_name for the currently authenticated user.
	 */
	private String getDisplayName() {
		// This should be called right after setting tokens on a session, so don't configure tokens here
		DropboxAPI<WebAuthSession> api = new DropboxAPI<WebAuthSession>(getWebAuthSession());
		String name = "User";
		try {
			Account account =  api.accountInfo();
			name  = account.displayName;
		} catch (DropboxException e) {
			e.printStackTrace();
		}
		return name;
	}
	
	private void storeInSession(User user) {
		storeInSession(Constants.USER_SESSION_KEY, user);
	}
	
	/**
	 * Gets the response that contains a Dropbox redirect URL and an empty file list (If the file list is not empty, the client simply displays the list. If the file list is empty, the client navigates to the 
	 * authorization URL) 
	 * @param serverName
	 * @param port
	 * @return JSON object with the fields url and fileList.
	 */
	public JSONObject getAuthenticationResponse(String serverName, int port) {
		String oAuthURL = getOAuthURL(serverName, port);
		NoWarningJSONObject result = new NoWarningJSONObject();
		JSONArray list = new JSONArray();
		result.put("url", oAuthURL);
		result.put("fileList", list);
		return result;
	}
	
	private String getOAuthURL(String serverName, int port) {
		WebAuthSession webAuthSession = null;
		WebAuthInfo webAuthInfo = null;
		try {
			webAuthSession = initWebAuthSession(); // init as opposed to get -- ?? to clear the possibly set wrong tokens?
			String url = "http://" + serverName + ":" + port + CALLBACK_SUFFIX;
			webAuthInfo = webAuthSession.getAuthInfo(url);
		} catch (DropboxException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		finally {
			if (webAuthSession == null || webAuthInfo == null) {
				String message= "Could not authenticate. WebAuthInfo is null.";
				System.err.println(message);
				throw new RuntimeException(message);
			}
		}
		storeInSession(Constants.WEB_AUTH_SESSION_SESSION_KEY, webAuthSession);
		storeInSession(Constants.WEB_AUTH_INFO_SESSION_KEY, webAuthInfo);
		//removeFromSession(Constants.ACCESS_KEY_SESSION_KEY);
		//removeFromSession(Constants.ACCESS_SECRET_SESSION_KEY);
		// clear existing tokens because otherwise they will never be reset
		removeFromSession(Constants.USER_SESSION_KEY);
		System.out.println(webAuthInfo.url);
		return webAuthInfo.url;
	}
	/**
	 * Forms an access token pair from the user's access key and access secret, sets the token pair as a property of the current Dropbox WebSession.
	 * @param user
	 */
	public void setCurrentUserInAuthSession(User user) {
		getWebAuthSession().setAccessTokenPair(new AccessTokenPair(user.getAccessKey(), user.getAccessSecret()));
	}
	/**
	 * Creates a new <code>WebAuthSession</code>
	 * @return New <code>WebAuthSession</code>
	 */
	public  WebAuthSession initWebAuthSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
		WebAuthSession authSession = new WebAuthSession(appKeyPair, ACCESS_TYPE);
		return authSession;
	}
	/**
	 * Tries to fetch a Dropbox WebAuthSession from the server session. If it is not there, initiates a WebAuthSession and stores it in the server session.
	 * @return The retrieved WebAuthSession
	 */
	public WebAuthSession getWebAuthSession() {
		WebAuthSession webAuthSession = getFromSession(Constants.WEB_AUTH_SESSION_SESSION_KEY);
		if (webAuthSession == null) {
			webAuthSession = initWebAuthSession();
			storeInSession(Constants.WEB_AUTH_SESSION_SESSION_KEY, webAuthSession);
		}
		return webAuthSession;
	}
	
	private void storeInSession(String key, Object item) {
		if (this.session != null) {
			this.session.attribute(key, item);
		}
	}
	private <T> T getFromSession(String key) {
		if (this.session == null) {
			return null;
		}
		return this.session.attribute(key);
	}
	private void removeFromSession(String key) {
		if (this.session != null) {
			this.session.removeAttribute(key);
		}
	}


}
