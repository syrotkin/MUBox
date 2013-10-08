package server.model;

import com.mongodb.BasicDBObject;
/**
 * Represents a voting process, normally stored in the "voting" collection.
 * @author soleksiy
 *
 */
public class OpenVote extends BasicDBObject {
	private static final long serialVersionUID = 7467628976553226943L;
	//public Voting voting;
	//public VotingUser votingUser;
	/*
	 * This is what we store:
		long votingID;
		// String initiator -- but then we would need the username...
		String action;
		int votesFor
		int votesAgainst
		int userCount
		String scheme
		boolean voted
		String path
		String newPath (store it, but then decide if relevant)
	*/
	
	public long getVotingID() {
		return (long)get("votingID");
	}
	public void setVotingID(long votingID) {
		put("votingID", votingID);
	}
	public String getAction() {
		return (String)get("action");
	}
	public void setAction(String action) {
		put("action", action);
	}
	public int getVotesFor() {
		return (int)get("votesFor");
	}
	public void setVotesFor(int votesFor) {
		put("votesFor", votesFor);
	}
	public int getVotesAgainst() {
		return (int)get("votesAgainst");
	}
	public void setVotesAgainst(int votesAgainst) {
		put("votesAgainst", votesAgainst);
	}
	public int getUserCount() {
		return (int)get("userCount");
	}
	public void setUserCount(int userCount) {
		put("userCount", userCount);
	}
	public String getScheme() {
		return (String)get("scheme");
	}
	public void setScheme(String scheme) {
		put("scheme", scheme);
	}
	public boolean getVoted() {
		return (boolean)get("voted");
	}
	public void setVoted(boolean voted) {
		put("voted", voted);
	}
	public String getPath() {
		return (String)get("path");
	}
	public void setPath(String path) {
		put("path", path);
	}
	public String getNewPath() {
		return (String)get("newPath");
	}
	public void setNewPath(String newPath) {
		put("newPath", newPath);
	}
	public Boolean isAccepted() {
		return (Boolean)get("isAccepted");
	}
	public void setAccepted(boolean isAccepted) {
		put("isAccepted", isAccepted);
	}
	public String getInitiator() {
		return (String)get("initiator");
	}
	public void setInitiator(String initiator) {
		put("initiator", initiator);
	}
	public String getInitiatorName() {
		return (String)get("initiatorName");
	}
	public void setInitiatorName(String initiatorName) {
		put("initiatorName", initiatorName);
	}
}