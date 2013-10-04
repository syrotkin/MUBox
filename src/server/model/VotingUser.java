package server.model;

import com.mongodb.BasicDBObject;

public class VotingUser extends BasicDBObject {

	private static final String UID = "uid";
	private static final String PATH = "path";
	private static final String VOTING_ID = "votingID";
	private static final String NEW_PATH = "newPath";
	private static final String VOTED = "voted";
		
	private static final long serialVersionUID = 447201954146045054L;
		

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

	public long getVotingID() {
		return getLong(VOTING_ID);
	}
	public void setVotingID(long votingID) {
		put(VOTING_ID, votingID);
	}
	
	public String getNewPath() {
		return (String)get(NEW_PATH);
	}
	public void setNewPath(String newPath) {
		put(NEW_PATH, newPath);
	}

	public boolean getVoted() {
		return getBoolean(VOTED);
	}
	public void setVoted(boolean voted) {
		put(VOTED, voted);
	}

}
