package server.model;

import java.util.Date;

import server.data.NoWarningJSONObject;


/**
 * For displaying activities in the Activity View 
 */
public class FileActivity extends NoWarningJSONObject {
	
	/**
	 * Generated 
	 */
	private static final long serialVersionUID = 8984251425467785092L;
	
	private static final String FILENAME = "filename";
	private static final String PATH = "path";
	private static final String DATE = "date";
	private static final String ACTION = "action";
	private static final String USERNAME = "userName";
	private static final String DETAILS = "details";

	
	/*
	private String filename;
	private String path;
	
	private Date date; 		// could be deletion or creation
	private String action; 	// could be deletion or creation
	private String userName; // could be deletion or creation
	
	private String details;
	*/
	
	public Date creationDate;
	public String creationUid;
	public String creationUserName;
	public String creationAction;
	
	public Date deletionDate;
	public String deletionUid;
	public String deletionUserName;
	public String deletionAction;
			
	
	public String getFilename() {
		return (String)get(FILENAME);
	}
	public void setFilename(String filename) {
		put(FILENAME, filename);
	}
	
	public String getPath() {
		return (String)get(PATH);
	}
	public void setPath(String path) {
		put(PATH, path);
	}
	
	public Date getDate() {
		return new Date(Long.parseLong((String)get(DATE)));
	}
	public void setDate(Date date) {
		put(DATE, date.getTime());
	}
	
	public String getAction() {
		return (String)get(ACTION);
	}
	public void setAction(String action) {
		put(ACTION, action);
	}
	
	public String getUserName() {
		return (String)get(USERNAME);
	}
	public void setUserName(String userName) {
		put(USERNAME, userName);
	}
	
	public String getDetails() {
		return (String)get(DETAILS);
	}
	public void setDetails(String details) {
		put(DETAILS, details);
	}
	
	public String getImg() {
		return (String)get("img");
	}
	public void setImg(String img) {
		put("img", img);
	}
	
}
