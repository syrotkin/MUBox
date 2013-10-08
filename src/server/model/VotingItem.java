package server.model;
/**
 * Common interface for {@link VotingRequest} and {@link VotingClosedNotification}
 * @author soleksiy
 *
 */
public interface VotingItem {
	public String getMessage();
	public long getVotingID();
	public Boolean isAccepted();
}

