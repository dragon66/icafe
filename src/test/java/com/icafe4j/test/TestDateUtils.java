package com.icafe4j.test;

import java.util.Locale;
import java.util.TimeZone;

import com.icafe4j.date.DateTime;
import com.icafe4j.date.DateUtils;

public class TestDateUtils extends TestBase {

	public TestDateUtils() { }
	
	public static void main(String[] args) {
		new TestDateUtils().test();
	}
	
	public void test(String ... args) {
		logger.info(DateTime.currentDate().format("EEE, d MMM yyyy HH:mm:ss zzz"));
		logger.info(DateTime.currentDateUTC().format("EEE, d MMM yyyy HH:mm:ss z"));
		logger.info("{}", DateTime.currentDate(TimeZone.getTimeZone("GMT-1:00"), Locale.getDefault()));
		logger.info("{} (Beijing/China)", DateTime.currentDate(TimeZone.getTimeZone("GMT+8:00"), Locale.getDefault()));
		logger.info("{}", DateTime.currentDate().daysAfter(-5));
		logger.info("{}", DateUtils.isValidDateStr("2001, Jul 04","yyyy, MMM dd", Locale.US));
	}
}