package server.settings;

public class ClientSideSettings {
	public final boolean disableActivityView;
	public final boolean disableShadow;
	public final boolean disableVoting;
	
	public ClientSideSettings(boolean disableActivityView, boolean disableShadow, boolean disableVoting) { 
		this.disableActivityView = disableActivityView;
		this.disableShadow = disableShadow;
		this.disableVoting = disableVoting;
	}
}