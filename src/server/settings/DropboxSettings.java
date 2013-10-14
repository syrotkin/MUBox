package server.settings;

public class DropboxSettings {
	public final String callbackSuffix;
	public final String appKey;
	public final String appSecret;
	
	public DropboxSettings(String callbackSuffix, String appKey, String appSecret) {
		this.callbackSuffix = callbackSuffix;
		this.appKey = appKey;
		this.appSecret = appSecret;
	}
}