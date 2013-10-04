package server;

public class ServerInfo {
	
	private static Object lock = new Object();
	
	private static String protocol = null;
	private static String serverName = null;
	private static int port = 0;
	
	
	public static void setServerInfo(String protocol_, String serverName_, int port_) {
		synchronized (lock) {
			if (ServerInfo.protocol == null){
				protocol = protocol_;
			} 
			if (ServerInfo.serverName == null) {
				ServerInfo.serverName = serverName_;
			}
			if (ServerInfo.port == 0) {
				port = port_;
			}
		}
	}
	
	public static String getProtocol() {
		return protocol;
	}
	
	public static String getServerName() {
		return serverName;
	}
	
	public static int getPort() {
		return port;
	}
	
}
