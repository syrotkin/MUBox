package server.settings;

public class GoogleDriveSettings {
	public final String appName;
	public final String redirectURI;
	public final String clientID;
	public final String clientSecret;
	
	public GoogleDriveSettings(String appName, String redirectURI, String clientID, String clientSecret) {
		this.appName = appName;
		this.redirectURI = redirectURI;
		this.clientID = clientID;
		this.clientSecret = clientSecret;
	}
}