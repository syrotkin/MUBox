package server;

import java.util.List;

import server.model.SharedFolder;
/**
 * Encapsulates the voting information to be inserted in the voting collection.
 * @author soleksiy
 *
 */
public class VotingInfo {
	private List<SharedFolder> sharedFolders;
	private long votingID;
	private long sharedFolderIDFrom;
	private long sharedFolderIDTo;
	
	public List<SharedFolder> getSharedFolders() {
		return sharedFolders;
	}
	public void setSharedFolders(List<SharedFolder> sharedFolders) {
		this.sharedFolders = sharedFolders;
	}
	
	public long getVotingID() {
		return votingID;
	}
	public void setVotingID(long votingID) {
		this.votingID = votingID;
	}
	
	public long getSharedFolderIDFrom() {
		return sharedFolderIDFrom;
	}
	public void setSharedFolderIDFrom(long sharedFolderIDFrom) {
		this.sharedFolderIDFrom = sharedFolderIDFrom;
	}
	
	public long getSharedFolderIDTo() {
		return sharedFolderIDTo;
	}
	public void setSharedFolderIDTo(long sharedFolderIDTo) {
		this.sharedFolderIDTo = sharedFolderIDTo;
	}
	
}