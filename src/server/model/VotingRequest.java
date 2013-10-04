package server.model;

import com.mongodb.BasicDBObject;

/**
 * An item (row in a table) displayed in the voting/notification dialog.
 */
public class VotingRequest extends BasicDBObject implements VotingItem {
	
	private static final long serialVersionUID = 7994272530170067208L;
	
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
	
	public long getVotingID() {
		return getLong("votingID");
	}
	public void setVotingID(long votingID) {
		put("votingID", votingID);
	}
	
	public String getInitiator() {
		return (String)get("initiator");
	}
	public void setInitiator(String initiatorUid) {
		put("initiator", initiatorUid);
	}
	
	public String getInitiatorName() {
		return (String)get("initiatorName");
	}
	public void setInitiatorName(String initiatorName) {
		put("initiatorName", initiatorName);
	}
	
	public String getAction( ) {
		return (String)get("action");
	}
	public void setAction(String action) {
		put("action", action);
	}
	
	public String getMessage() {
		return (String)get("message");
	}
	public void setMessage(String message) {
		put("message", message);
	}
	
	public Boolean isAccepted() {
		return (Boolean)get("isAccepted");
	}
	public void setAccepted(Boolean isAccepted) {
		put("isAccepted", isAccepted);
	}
}