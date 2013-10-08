package server.data;
/**
 * Encapsulates result of a deletion
 * @author soleksiy
 *
 */
public class DeleteResult {
	private String error;
	private String rev;
	
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public String getRev() {
		return rev;
	}
	public void setRev(String rev) {
		this.rev = rev;
	}
	
}
