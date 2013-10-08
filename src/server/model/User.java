package server.model;

import com.mongodb.BasicDBObject;

/**
 * Represents a cloud storage user. Could contain fields specific to Dropbox or Google Drive.
 * @author soleksiy
 *
 */
public class User extends BasicDBObject {
	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -5831817917342332776L;
			
	private static final String UID = "uid";
	private static final String DISPLAY_NAME = "display_name";
	private static final String ACCESS_KEY = "access_key";
	private static final String ACCESS_SECRET = "access_secret";
	private static final String REFRESH_TOKEN = "refreshToken";
	private static final String EMAIL = "email";
	private static final String PROVIDER = "provider";
	private static final String IMG = "img";
		

	public String getUid() {
		return (String)get(UID);
	}
	public void setUid(String uid) {
		put(UID, uid);
	}
	
	public String getDisplayName() {
		return (String)get(DISPLAY_NAME);
	}
	public void setDisplayName(String displayName) {
		put(DISPLAY_NAME, displayName);
	}
	/**
	 * Access key in Dropbox
	 * Access token in Google
	 */
	public String getAccessKey() {
		return (String)get(ACCESS_KEY);
	}
	/**
	 * Access key in Dropbox
	 * Access token in Google
	 */
	public void setAccessKey(String accessKey) {
		if (accessKey != null) {
			put(ACCESS_KEY, accessKey);
		}
	}
	
	/** 
	 * Only in Dropbox
	 */
	public String getAccessSecret() {
		return (String)get(ACCESS_SECRET);
	}
	/** 
	 * Only in Dropbox
	 */
	public void setAccessSecret(String accessSecret) {
		if (accessSecret != null) {
			put(ACCESS_SECRET, accessSecret);
		}
	}
	
	/** 
	 * Only in Google Drive
	 */
	public String getRefreshToken() {
		return (String)get(REFRESH_TOKEN);
	}
	/** 
	 * Only in Google Drive
	 */
	public void setRefreshToken(String refreshToken) {
		if (refreshToken != null) {
			put(REFRESH_TOKEN, refreshToken);
		}
	}
	
	/** 
	 * Only in Google Drive
	 */
	public String getEmail() {
		return (String)get(EMAIL);
	}
	/** 
	 * Only in Google Drive
	 */
	public void setEmail(String email) {
		if (email != null) {
			put(EMAIL, email);
		}
	}

	public String getProvider() {
		return (String)get(PROVIDER);
	}
	public void setProvider(String provider) {
		if (provider != null) {
			put(PROVIDER, provider);
		}
	}

	public String getImg() {
		return (String)get(IMG);
	}
	public void setImg(String img) {
		if (img != null) {
			put(IMG, img);
		}
	}

}
