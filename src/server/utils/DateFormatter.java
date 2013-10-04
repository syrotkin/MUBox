package server.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;

public class DateFormatter {
	private static DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy h:mm a"); 
	//private static final long DAY_IN_MILLIS = 1000 * 3600 * 24;
	//private static final long HOUR_IN_MILLIS = 1000 * 3600;
	
	public static String formatDate(Date date) {
		String result;
		DateTime dt = new DateTime(date);
		DateTime now = new DateTime(new Date());
		
		/*
		Date nowDate = new Date();
		long millisDifference = nowDate.getTime() - date.getTime();
		if (millisDifference < DAY_IN_MILLIS) {
			int hoursDifference = (int)(millisDifference/ HOUR_IN_MILLIS);
		}
		*/
		
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
