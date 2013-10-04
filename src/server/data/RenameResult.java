package server.data;

import java.util.Map;

public class RenameResult {
	
	private String error;
	public Map<String, String> pathToRevMap;
	//public String rev;
	
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	
}
