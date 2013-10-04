package server;

import server.data.UserManager;
import server.model.User;
import spark.Request;
import spark.Session;

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

	public CloudStorage getCloudStorage(Session session) {
		setServerSession(session);
		String provider = getFromSession(Constants.PROVIDER_SESSION_KEY);
		return getCloudStorage(provider);
	}

	public void setServerSession(Session session) {
		this.session = session;
	}

	public CloudStorage getCloudStorageByUserUid(String uid) {
		User user = userManager.getUser(uid);
		String provider = user.getProvider();
		CloudStorage cloudStorage = getCloudStorage(provider);
		cloudStorage.setUid(uid);
		return cloudStorage;
	}
	
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

