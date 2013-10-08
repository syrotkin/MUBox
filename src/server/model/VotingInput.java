package server.model;

import com.mongodb.BasicDBObject;

/**
 * Describes voting information sent from the client side. 
 */
public class VotingInput extends BasicDBObject {
	private static final long serialVersionUID = 3095063024248237456L;
	
	// uid, votingID, accepted
		
	public String getUid() {
		return (String)get("uid");
	}
	public void setUid(String uid) {
		put("uid", uid);
	}
	
	public long getVotingID() {
		return getLong("votingID");
	}
	public void setVotingID(long votingID) {
		put("votingID", votingID);
	}
	
	public boolean getAccepted() {
		return getBoolean("accepted");
	}
	public void setAccepted(boolean accepted) {
		put("accepted", accepted);
	}
	
	public boolean getCancel() {
		Boolean cancel = (Boolean)get("cancel");
		if (cancel == null) {
			return false;
		}
		return cancel.booleanValue();
	}
	public void setCancel(boolean cancel) {
		put("cancel", cancel);
	}
}
