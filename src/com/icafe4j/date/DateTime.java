/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.date;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * A wrapper for the Date class with information for TimeZone, Locale as well
 * as a calendar associated with it 
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 02/15/2013
 */
public class DateTime 
{
	private Date date;
	private TimeZone timeZone;
	private Locale locale;
	private Calendar calendar;

	public DateTime(Date date) {
		this(date, TimeZone.getDefault(), Locale.getDefault());
	}
	
	public DateTime(Date date, TimeZone timeZone, Locale locale) {
		this.date = date;
		this.timeZone = timeZone;
		this.locale = locale;
		this.calendar = Calendar.getInstance(timeZone, locale);
		
		calendar.setTime(date);
	}
	
	public DateTime(Date date, TimeZone timeZone) {
		this(date, timeZone, Locale.getDefault());
	}
	
	public boolean after(DateTime that) {	
		return this.getUTCTimeInMillis() > that.getUTCTimeInMillis();
	}
	
	public boolean before(DateTime that) {
		return this.getUTCTimeInMillis() < that.getUTCTimeInMillis();
	}
	
	/**
	 * Simple representation of the current date for the default TimeZone
	 * and Locale.
	 * 
	 * @return a DateTime object for the default TimeZone and Locale.
	 */
	public static DateTime currentDate() {
		TimeZone tz = TimeZone.getDefault();
		Locale locale = Locale.getDefault();
	
		return currentDate(tz, locale);
	}
	
	public static DateTime currentDate(TimeZone timeZone) {
		Locale locale = Locale.getDefault();
			
		return currentDate(timeZone, locale);
	}
	
	public static DateTime currentDate(TimeZone timeZone, Locale locale) {
		Date date = new Date();
	
		return new DateTime(date, timeZone, locale);
	}
	
	public static DateTime currentDateUTC() {
		return currentDate(TimeZone.getTimeZone("UTC"));
	}
	
	public static DateTime currentDateUTC(Locale locale) {
		return currentDate(TimeZone.getTimeZone("UTC"), locale);
	}
	
	public DateTime dateAfter(int years, int months) {
		Calendar clone = (Calendar)calendar.clone();
		clone.add(Calendar.YEAR, years);
		clone.add(Calendar.MONTH, months);
		
		return new DateTime(clone.getTime(), timeZone, locale);
	}
	
	public DateTime dateAfter(int years, int months, int days) {
		Calendar clone = (Calendar)calendar.clone();
		clone.add(Calendar.YEAR, years);
		clone.add(Calendar.MONTH, months);
		clone.add(Calendar.DAY_OF_MONTH, days);
		
		return new DateTime(clone.getTime(), timeZone, locale);
	}
	
	public DateTime dateAfter(int years, int months, int days, int hours, int minutes, int seconds, int millis) {
		Calendar clone = (Calendar)calendar.clone();
		clone.add(Calendar.YEAR, years);
		clone.add(Calendar.MONTH, months);
		clone.add(Calendar.DAY_OF_MONTH, days);
		clone.add(Calendar.HOUR, hours);
		clone.add(Calendar.MINUTE, minutes);
		clone.add(Calendar.SECOND, seconds);
		clone.add(Calendar.MILLISECOND, millis);
		
		return new DateTime(clone.getTime(), timeZone, locale);
	}	
		
	public DateTime daysAfter(int days) {
		
		return new DateTime(new Date(date.getTime() + 1000L*3600*24*days), timeZone, locale);
	}
	
	public boolean equals(Object that) {
		if (!(that instanceof DateTime))
			return false;
		return this.getUTCTimeInMillis() == ((DateTime)that).getUTCTimeInMillis();
	}
	
	public String format(DateFormat df) {
		return df.format(date);
	}
	
	public String format(String format) {
		return format(format, Locale.getDefault());
	}

	public String format(String format, Locale locale) {	
		
        DateFormat df = new SimpleDateFormat(format, (locale == null)?Locale.getDefault():locale);
		df.setTimeZone(timeZone);
		String dateString = df.format(date);
	
		return dateString;
	}
	
	 /**
     * Formats a DateTime to a ISO8601 string with a second fraction part of up to 3 digits.
     */
    public String formatISO8601() {        
        return DateUtils.formatISO8601(date, timeZone);    	            
    }
	
	public Date getDate() {
		return (Date)date.clone();
	}
		
	public int getDayOfMonth() {
		return calendar.get(Calendar.DAY_OF_MONTH);
	}
		
	public int getDayOfWeek() {
		return calendar.get(Calendar.DAY_OF_WEEK);
	}
	
	public int getMonth() {
		return calendar.get(Calendar.MONTH);
	}
	
	public long getUTCTimeInMillis() {
		long time = date.getTime();
		int zoneOffset = timeZone.getOffset(time);
		
		return time + zoneOffset;
	}
		
	public int getYear() {
		return calendar.get(Calendar.YEAR);
	}
	
	public int hashCode() {
		return Long.valueOf(getUTCTimeInMillis()).hashCode();
	}
	
	public DateTime hoursAfter(int hours) {
		return new DateTime(new Date(date.getTime() + 1000L*3600*hours), timeZone, locale);
	}	
	
	public DateTime monthsAfter(int months) {
		Calendar clone = (Calendar)calendar.clone();
		clone.add(Calendar.MONTH, months);
		
		return new DateTime(clone.getTime(), timeZone, locale);
	}
			
	public String toString() {
	
		DateFormat df = DateFormat.getInstance();	
		df.setTimeZone(timeZone);
	
		return df.format(this.date) + " " + timeZone.getDisplayName();
	}
	
	public DateTime yearsAfter(int years) {
		Calendar clone = (Calendar)calendar.clone();
		clone.add(Calendar.YEAR, years);
		
		return new DateTime(clone.getTime(), timeZone, locale);
	}	
}
