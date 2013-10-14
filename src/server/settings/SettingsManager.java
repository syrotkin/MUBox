package server.settings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class SettingsManager {
	
	// Client-side settings keys
	
	private static final String DISABLE_ACTIVITY_VIEW_SETTINGS_KEY = "disableActivityView";
	private static final String DISABLE_SHADOW_SETTINGS_KEY = "disableShadow";
	private static final String DISABLE_VOTING_SETTINGS_KEY = "disableVoting";
	
	// Dropbox settings keys
	
	private static final String DROPBOX_APP_KEY_SETTINGS_KEY = "dropboxAppKey";
	private static final String DROPBOX_APP_SECRET_SETTINGS_KEY = "dropboxAppSecret";
	private static final String DROPBOX_CALLBACK_SUFFIX_SETTINGS_KEY = "dropboxCallbackSuffix";
				
	// Google Drive settings keys
	
	private static final String GOOGLE_APP_NAME_SETTINGS_KEY = "googleAppName";
	private static final String GOOGLE_REDIRECT_URI_SETTINGS_KEY = "googleRedirectURI";
	private static final String GOOGLE_CLIENT_ID_SETTINGS_KEY = "googleClientID";
	private static final String GOOGLE_CLIENT_SECRET_SETTINGS_KEY = "googleClientSecret";
	
	public Settings readProperties() {
		Properties prop = new Properties();
		boolean disableActivityView;
		boolean disableShadow;
		boolean disableVoting;
		
		String dropboxCallbackSuffix;
		String dropboxAppKey;
		String dropboxAppSecret;
				
		String googleAppName;
		String googleRedirectURI;
		String googleClientID;
		String googleClientSecret;
		
		ClientSideSettings clientSideSettings = null;
		DropboxSettings dropboxSettings = null;
		GoogleDriveSettings googleDriveSettings = null;
		
		try {
			prop.load(new FileInputStream("config.properties"));
			disableActivityView = Boolean.parseBoolean(prop.getProperty(DISABLE_ACTIVITY_VIEW_SETTINGS_KEY, "false"));
			disableShadow = Boolean.parseBoolean(prop.getProperty(DISABLE_SHADOW_SETTINGS_KEY, "false"));
			disableVoting = Boolean.parseBoolean(prop.getProperty(DISABLE_VOTING_SETTINGS_KEY, "false"));
			clientSideSettings = new ClientSideSettings(disableActivityView, disableShadow, disableVoting);
						
			dropboxCallbackSuffix = prop.getProperty(DROPBOX_CALLBACK_SUFFIX_SETTINGS_KEY);
			dropboxAppKey = prop.getProperty(DROPBOX_APP_KEY_SETTINGS_KEY);
			dropboxAppSecret = prop.getProperty(DROPBOX_APP_SECRET_SETTINGS_KEY);
			dropboxSettings = new DropboxSettings(dropboxCallbackSuffix, dropboxAppKey, dropboxAppSecret);
			
			googleAppName = prop.getProperty(GOOGLE_APP_NAME_SETTINGS_KEY);
			googleRedirectURI = prop.getProperty(GOOGLE_REDIRECT_URI_SETTINGS_KEY);
			googleClientID = prop.getProperty(GOOGLE_CLIENT_ID_SETTINGS_KEY);
			googleClientSecret = prop.getProperty(GOOGLE_CLIENT_SECRET_SETTINGS_KEY);
			googleDriveSettings = new GoogleDriveSettings(googleAppName, googleRedirectURI, googleClientID, googleClientSecret);
					
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			disableActivityView = false;
			disableShadow = false;
			disableVoting = false;
		} catch (IOException e) {
			e.printStackTrace();
			disableActivityView = false;
			disableShadow = false;
			disableVoting = false;
		}
		return new Settings(clientSideSettings, dropboxSettings, googleDriveSettings);
	}
	
	
}
