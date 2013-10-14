package server.settings;
/**
 * Stores application settings: whether Activity View, shadow files, voting are enabled or disabled. 
 * @author soleksiy
 *
 */
public class Settings {
	//public final boolean disableActivityView;
	//public final boolean disableShadow;
	//public final boolean disableVoting;
	
	public final ClientSideSettings clientSideSettings;
	public final DropboxSettings dropboxSettings;
	public final GoogleDriveSettings googleDriveSettings;
	
	public Settings(ClientSideSettings clientSideSettings, DropboxSettings dropboxSettings, GoogleDriveSettings googleDriveSettings) {
		this.clientSideSettings = clientSideSettings;
		this.dropboxSettings = dropboxSettings;
		this.googleDriveSettings = googleDriveSettings;
	}

}
