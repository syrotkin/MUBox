package server.data;

import java.util.Map;
/**
 * A JSON object used to pass the result of a copy/move operation.
 * @author soleksiy
 *
 */
public class CopyOrMoveResult extends NoWarningJSONObject {
	/**
	 * Generated 
	 */
	private static final long serialVersionUID = 6128194436653833157L;
	
	public Map<String, String> pathToRevMap;

	/**
	 * Which path the client has to list after the operation is over.
	 */
	public String listPath;  
	
	public String getNewPath() {
		return (String)get("newpath");
	}
	public void setNewPath(String newPath) {
		put("newpath", newPath);
	}
	public String getRev() {
		return (String)get("rev");
	}
	public void setRev(String rev) {
		put("rev", rev);
	}
	public String getError() {
		return (String)get("error");
	}
	public void setError(String error) {
		put("error", error);
	}
	
	public String getFileId() {
		return (String)get("fileId");
	}
	public void setFileId(String fileId) {
		put("fileId", fileId);
	}
	
	public Map<String, String> getPathToFileIdMap() {
		return (Map<String, String>)get("pathToFileIdMap");
	}
	public void setPathToFileIdMap(Map<String, String> pathToFileIdMap) {
		put("pathToFileIdMap", pathToFileIdMap);
	}
}
