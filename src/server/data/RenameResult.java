package server.data;

import java.util.Map;
/**
 * Encapsulation of a result of a rename operation.
 * @author soleksiy
 *
 */
public class RenameResult {
	
	private String error;
	public Map<String, String> pathToRevMap;
	
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	
}
