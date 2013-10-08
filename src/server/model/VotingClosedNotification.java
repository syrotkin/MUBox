package server.model;

import com.mongodb.BasicDBObject;
/**
 * Represents a notification that a voting process has been closed.
 * @author soleksiy
 *
 */
public class VotingClosedNotification extends BasicDBObject implements VotingItem {

	private static final long serialVersionUID = -4921903633176756916L;

	@Override
	public String getMessage() {
		return (String)get("message");
	}
	public void setMessage(String message) {
		put("message", message);
	}

	@Override
	public long getVotingID() {
		return getLong("votingID");
	}
	public void setVotingID(long votingID) {
		put("votingID", votingID);
	}

	@Override
	public Boolean isAccepted() {
		return (Boolean)get("isAccepted");
	}
	public void setAccepted(boolean isAccepted) {
		put("isAccepted", isAccepted);
	}
	
	public String getAction() {
		return (String)get("action");
	}
	public void setAction(String action) {
		put("action", action);
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
	
	public String getError() {
		return (String)get("error");
	}
	public void setError(String error) {
		put("error", error);
	}
	
}