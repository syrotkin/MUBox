package server;

import server.data.UserManager;
import server.model.User;
import spark.Request;
import spark.Session;

/**
 * Cloud factory. Creates instances of {@link CloudStorage}.
 * @author soleksiy
 *
 */
public class CloudFactory {
	
	private UserManager userManager;
	private Session session;
	
	public CloudFactory(UserManager userManager) {
		this.userManager = userManager;
	}
	
	private CloudStorage getCloudStorage(String provider) {
		// Defaulting to Dropbox
		CloudStorage cloudStorage;
		if (provider == null) {
			System.out.println("CloudFactory: Provider in Session null. Defaulting to Dropbox!");
			provider = Constants.Provider.DROPBOX;
		}
		if (Constants.Provider.DROPBOX.equalsIgnoreCase(provider)) {
			cloudStorage = new Dropbox(userManager);
		}
		else if (Constants.Provider.GOOGLE.equalsIgnoreCase(provider)) {
			cloudStorage = new GoogleDrive(userManager);
		}
		else {
			System.out.println(provider + " is not a valid provider");
			throw new IllegalArgumentException(provider + " is not a valid provider");
		}
		storeInSession(Constants.PROVIDER_SESSION_KEY, provider);
		cloudStorage.setServerSession(this.session);
		return cloudStorage;
	}

	/**
	 * Gets the cloud storage implementation stored in the server session.
	 * @param session Server session
	 * @return Specific cloud storage provider
	 */
	public CloudStorage getCloudStorage(Session session) {
		setServerSession(session);
		String provider = getFromSession(Constants.PROVIDER_SESSION_KEY);
		return getCloudStorage(provider);
	}

	/**
	 * Sets server session
	 * @param session Server session
	 */
	public void setServerSession(Session session) {
		this.session = session;
	}

	/**
	 * Gets cloud storage implementation by user UID.
	 * @param uid User UID
	 * @return Cloud storage implementation
	 */
	public CloudStorage getCloudStorageByUserUid(String uid) {
		User user = userManager.getUser(uid);
		String provider = user.getProvider();
		CloudStorage cloudStorage = getCloudStorage(provider);
		cloudStorage.setUid(uid);
		return cloudStorage;
	}
	
	/**
	 * Gets cloud storage from the server request.
	 * @param request Server request
	 * @return Cloud storage implementation
	 */
	public CloudStorage getCloudStorage(Request request) {
		setServerSession(request.session());
		String provider = getFromSession(Constants.PROVIDER_SESSION_KEY);
		if (provider == null) {
			provider = request.queryParams("provider");
		}
		if (provider == null) {
			System.out.println("Provider is null.");
			// will default to dropbox in a subsequent method call
		}
		return getCloudStorage(provider);
	}

	private void storeInSession(String key, Object item) {
		if (this.session != null) {
			this.session.attribute(key, item);
		}
	}
	private String getFromSession(String key) {
		if (this.session == null) {
			return null;
		}
		return this.session.attribute(key);
	}
	
}

