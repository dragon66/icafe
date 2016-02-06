/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.icafe4j.string.StringUtils;

/**
 * Date utility class  
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 09/19/2012
 */
public class DateUtils
{
	private DateUtils(){} // Prevents instantiation
	
	public static Date dateAfter(Date date, int years, int months) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.YEAR, years);
		calendar.add(Calendar.MONTH, months);
		
		return calendar.getTime();
	}
	
	public static Date dateAfter(Date date, int years, int months, int days) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.YEAR, years);
		calendar.add(Calendar.MONTH, months);
		calendar.add(Calendar.DAY_OF_MONTH, days);
		
		return calendar.getTime();		
	}
	
	public static Date dateAfter(Date date, int years, int months, int days, int hours, int minutes, int seconds, int millisecs) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.YEAR, years);
		calendar.add(Calendar.MONTH, months);
		calendar.add(Calendar.DAY_OF_MONTH, days);
		calendar.add(Calendar.HOUR, hours);
		calendar.add(Calendar.MINUTE, minutes);
		calendar.add(Calendar.SECOND, seconds);
		calendar.add(Calendar.MILLISECOND, millisecs);
		
		return calendar.getTime();		
	}	
	
	public static Date daysAfter(Date date, int days) {
		return new Date(date.getTime() + 1000L*3600*24*days);
	}	
	
	public static Date hoursAfter(Date date, int hours) {
		return new Date(date.getTime() + 1000L*3600*hours);
	}
	
	/**
	 * See {@link #isValidDateStr(String, String, Locale)} for more details
	 */
	public static boolean isValidDateStr(String date, String format) {
	    return isValidDateStr(date, format, Locale.getDefault());
	}
	
	/**
	 * Checks if a date string conforms to a specific format for the given locale
	 * 
	 * @param date a date string
	 * @param format a date format string
	 * @param locale the locale to use when interpreting the date string
	 * @return true if the date string conforms to the date format for the given
	 * locale, otherwise false
	 */
	public static boolean isValidDateStr(String date, String format, Locale locale) {
	    // Sanity check on parameters
		if (StringUtils.isNullOrEmpty(date) || StringUtils.isNullOrEmpty(format))
	    	throw new IllegalArgumentException("Null or empty date/format.");
	    
		try {
	      SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
	      sdf.setLenient(false);
	      sdf.parse(date);
	    }
	    catch (ParseException e) {
	      return false;
	    }
		
	    return true;
	}
	
	public static Date monthsAfter(Date date, int months) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MONTH, months);
		
		return calendar.getTime();		
	}	
	
	public static Date yearsAfter(Date date, int years) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.YEAR, years);
		
		return calendar.getTime();		
	}
}