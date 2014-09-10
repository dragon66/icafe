package cafe.test;

import java.util.Locale;
import java.util.TimeZone;

import cafe.date.DateTime;
import cafe.date.DateUtils;

public class TestDateUtils {

	public TestDateUtils() {	}
	
	public static void main(String[] args) {
		System.out.println(DateTime.currentDate().format("EEE, d MMM yyyy HH:mm:ss zzz"));
		System.out.println(DateTime.currentDateUTC().format("EEE, d MMM yyyy HH:mm:ss z"));
		System.out.println(DateTime.currentDate(TimeZone.getTimeZone("GMT-1:00"), Locale.getDefault()));
		System.out.println(DateTime.currentDate(TimeZone.getTimeZone("GMT+8:00"), Locale.getDefault()) + " (Beijing/China)");
		System.out.println(DateTime.currentDate().daysAfter(-5));
		System.out.println(DateUtils.isValidDateStr("2001, Jul 04","yyyy, MMM dd", Locale.US));
	}
}