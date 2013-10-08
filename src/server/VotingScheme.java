package server;
/**
 * Encapsulates voting scheme information.
 * @author soleksiy
 *
 */
public class VotingScheme {
	private String name;
	private int periodInMinutes;
	private int percentage;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public int getPeriodInMinutes() {
		return periodInMinutes;
	}
	public void setPeriodInMinutes(int periodInMinutes) {
		this.periodInMinutes = periodInMinutes;
	}
	
	public int getPercentage() {
		return percentage;
	}
	public void setPercentage(int percentage) {
		this.percentage = percentage;
	}
}
