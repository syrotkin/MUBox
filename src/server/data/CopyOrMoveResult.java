package server.data;

import java.util.Map;

public class CopyOrMoveResult extends NoWarningJSONObject {
	/**
	 * Generated 
	 */
	private static final long serialVersionUID = 6128194436653833157L;
	
	/*
	private String newPath;
	private String rev;
	private String error;
	*/
	public Map<String, String> pathToRevMap;

	public String listPath;  // which path the client has to list after the operation is over.
	
	public String getNewPath() {
		//return newPath;
		return (String)get("newpath");
	}
	public void setNewPath(String newPath) {
		//this.newPath = newPath;
		put("newpath", newPath);
	}
	public String getRev() {
		//return rev;
		return (String)get("rev");
	}
	public void setRev(String rev) {
		//this.rev = rev;
		put("rev", rev);
	}
	public String getError() {
		//return error;
		return (String)get("error");
	}
	public void setError(String error) {
		//this.error = error;
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
