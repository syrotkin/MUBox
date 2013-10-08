package server.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
/**
 * Formats a timestamp as a number of "minutes ago" or "hours ago" if it is within 1 day. Otherwise, formats it as dd.MM.yyyy h:mm a. 
 * @author soleksiy
 *
 */
public class DateFormatter {
	private static DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy h:mm a"); 
	
	public static String formatDate(Date date) {
		String result;
		DateTime dt = new DateTime(date);
		DateTime now = new DateTime(new Date());
			
		int days = Days.daysBetween(dt, now).getDays();
		if (days < 1) {
			int hours = Hours.hoursBetween(dt, now).getHours();
			if (hours < 1) {
				int minutes = Minutes.minutesBetween(dt, now).getMinutes();
				result = minutes + (minutes == 1 ? " minute" : " minutes") + " ago";
			}
			else {
				result = hours + (hours == 1 ? " hour" : " hours") + " ago";
			}
		}
		else {
			result = dateFormat.format(date);
		}
		return result;
		
	}
}
