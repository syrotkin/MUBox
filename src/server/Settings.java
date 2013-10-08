package server;
/**
 * Stores application settings: whether Activity View, shadow files, voting are enabled or disabled. 
 * @author soleksiy
 *
 */
public class Settings {
	public final boolean disableActivityView;
	public final boolean disableShadow;
	public final boolean disableVoting;
	
	public Settings(boolean disableActivityView, boolean disableShadow, boolean disableVoting) {
		this.disableActivityView = disableActivityView;
		this.disableShadow = disableShadow;
		this.disableVoting = disableVoting;
	}
}
