package server.model;

import com.mongodb.BasicDBObject;

/**
 * Represents a shared folder.
 * @author soleksiy
 *
 */
public class SharedFolder extends BasicDBObject {
	/**
	 * Generated
	 */
	private static final long serialVersionUID = -6493272083734650293L;

	private static final String UID = "uid";
	private static final String PATH = "path";
	private static final String SEQ = "seq";
	
	public String getUid() {
		return (String)get(UID);
	}
	public void setUid(String uid) {
		put(UID, uid);
	}
	
	public String getPath() {
		return (String)get(PATH);
	}
	public void setPath(String path) {
		put(PATH, path);
	}
	
	public long getSharedFolderID() {
		return getLong(SEQ);
	}
	public void setSharedFolderID(long folderSeq) {
		put(SEQ, folderSeq);
	}
		
	public String getOwner() {
		return (String)get("owner");
	}
	public void setOwner(String ownerUid) {
		put("owner", ownerUid);
	}
	
	/** 
	 * Related to voting. Gets the voting scheme for this shared folder. 
	 */
	public String getVotingScheme() {
		return (String)get("votingScheme");
	}
	/** 
	 * Related to voting. Sets the voting scheme for this shared folder. 
	 */
	public void setVotingScheme(String votingScheme) {
		put("votingScheme", votingScheme);
	}	
	/** 
	 * Related to voting. Gets "percentage". Used with "percentage" and related voting schemes  
	 */
	public int getPercentageUsers() {
		Integer i = (Integer)get("percentage");
		if (i == null) {
			return -1;
		}
		return i.intValue();
	}
	/** 
	 * Related to voting. Sets "percentage". Used with "percentage" and related voting schemes  
	 */
	public void setPercentageUsers(int percentage) {
		put("percentage", percentage);
	}
	/** 
	 * Related to voting. Gets "periodInMinutes". Used with time-constrained voting  
	 */
	public int getPeriodInMinutes() {
		Integer i = (Integer)get("periodInMinutes");
		if (i == null) {
			return -1;
		}
		return i.intValue();
	}
	/**
	 * Related to voting. Sets "periodInMinutes". Used with time-constrained voting.
	 * @param minutes
	 */
	public void setPeriodInMinutes(int minutes) {
		put("periodInMinutes", minutes);
	}

} // end class
