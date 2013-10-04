package server.model;

import java.util.Date;

import com.mongodb.BasicDBObject;

public class Voting extends BasicDBObject {
	
	private static final String _ID = "_id";
	private static final String ACTION = "action";
	private static final String NEW_PATH = "newPath";
	private static final String VOTES_FOR = "votesFor";
	private static final String VOTES_AGAINST = "votesAgainst";
	private static final String USER_COUNT = "userCount";
	private static final String SCHEME = "scheme";
	private static final String INITIATOR = "initiator";
	private static final String INITIATOR_NAME = "initiatorName";
	private static final String IS_ACCEPTED = "isAccepted";
	
	private static final long serialVersionUID = 1883808702439502114L;
	
	
	public long getID() {
		return getLong(_ID);
	}
	public void setID(long id) {
		put(_ID, id);
	}
	
	public String getAction() {
		return (String)get(ACTION);
	}
	public void setAction(String action) {
		put(ACTION, action);
	}
	
	public String getNewPath() {
		return (String)get(NEW_PATH);
	}
	public void setNewPath(String newPath) {
		put(NEW_PATH, newPath);
	}
	
	public int getVotesFor() {
		return getInt(VOTES_FOR);
	}
	public void setVotesFor(int votesFor) {
		put(VOTES_FOR, votesFor);
	}
	// TODO: this is not atomic
	public void incVotesFor() {
		int prev =  getInt(VOTES_FOR);
		put(VOTES_FOR, ++prev);
	}
	
	public int getVotesAgainst() {
		return getInt(VOTES_AGAINST);
	}
	public void setVotesAgainst(int votesAgainst) {
		put(VOTES_AGAINST, votesAgainst);
	}
	// TODO: not atomic
	public void incVotesAgainst() {
		int prev = getInt(VOTES_AGAINST);
		put(VOTES_AGAINST, ++prev);
	}
	
	public int getUserCount() {
		return getInt(USER_COUNT);
	}
	public void setUserCount(int userCount) {
		put(USER_COUNT, userCount);
	}
	
	// returns the initiator UID
	public String getInitiatorUid() {
		return (String)get(INITIATOR);
	}
	public void setInitiatorUid(String initiatorUid) {
		put(INITIATOR, initiatorUid);
	}
	
	public void setInitiatorName(String userName) {
		put(INITIATOR_NAME, userName);
	}
	public String getInitiatorName() {
		return (String)get(INITIATOR_NAME);
	}
	
	public Boolean isAccepted() {
		return (Boolean)get(IS_ACCEPTED);
	}
	public void setAccepted(boolean isAccepted) {
		put(IS_ACCEPTED, isAccepted);
	}
	
	
	public String getScheme() {
		return (String)get(SCHEME);
	}
	public void setScheme(String scheme) {
		put(SCHEME, scheme);
	}
	public int getPercentage() {
		Integer i = (Integer)get("percentage");
		if (i == null) {
			return -1;
		}
		return i.intValue();
	}
	public void setPercentage(int percentage) {
		put("percentage", percentage);
	}
	public int getPeriodInMinutes() {
		Integer i = (Integer)get("periodInMinutes");
		if (i == null) {
			return -1;
		}
		return i.intValue();
	}
	public void setPeriodInMinutes(int periodInMinutes) {
		put("periodInMinutes", periodInMinutes);
	}
	public Date getStartTime() {
		return (Date)get("startTime");
	}
	public void setStartTime(Date date) {
		put("startTime", date);
	}
	
	public String getError() {
		return (String)get("error");
	}
	
	public void setError(String error) {
		put("error", error);
	}

}
