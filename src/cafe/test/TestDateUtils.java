package cafe.test;

import java.util.Locale;
import java.util.TimeZone;

import cafe.date.DateTime;
import cafe.date.DateUtils;

public class TestDateUtils extends TestBase {

	public TestDateUtils() { }
	
	public static void main(String[] args) {
		new TestDateUtils().test();
	}
	
	public void test(String ... args) {
		log.info(DateTime.currentDate().format("EEE, d MMM yyyy HH:mm:ss zzz"));
		log.info(DateTime.currentDateUTC().format("EEE, d MMM yyyy HH:mm:ss z"));
		log.info("{}", DateTime.currentDate(TimeZone.getTimeZone("GMT-1:00"), Locale.getDefault()));
		log.info("{} (Beijing/China)", DateTime.currentDate(TimeZone.getTimeZone("GMT+8:00"), Locale.getDefault()));
		log.info("{}", DateTime.currentDate().daysAfter(-5));
		log.info("{}", DateUtils.isValidDateStr("2001, Jul 04","yyyy, MMM dd", Locale.US));
	}
}