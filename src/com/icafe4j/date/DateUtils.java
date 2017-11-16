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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
	
	public static String format(Date date, String format) {
    	SimpleDateFormat df = new SimpleDateFormat(format);
	    	
    	return df.format(date);
    }
	
	public static String formatISO8601(Date date) {        
        return format(date, "yyyy-MM-dd'T'HH:mm:ss.SSS");
	}
	
	/**
     * Formats a DateTime to a ISO8601 string with a second fraction part of up to 3 digits.
     */
    public static String formatISO8601(Date date, TimeZone timeZone) {
        
        SimpleDateFormat df = null;
      
        if (timeZone != null) {        	
        	df =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSSZ");
        	df.setTimeZone(timeZone);
        	
            String result = df.format(date);
            
        	if (result.endsWith("0000"))
        		return result.replaceAll("[+-]0000$", "Z");
        	
            return result.replaceAll("(\\d{2})(\\d{2})$", "$1:$2");        	
        } 
     
        df =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        	
        return df.format(date);
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
	
	/**
	 * Parses a ISO8601 string into a DateTime to retain time zone information.
     * 
     * The pattern is dynamically created in accordance with the input date string.
     * Time zone is optional.
     * 
	 * The fraction part of the second if exists is cut down to 3 digits, i.e to the 
	 * precision of millisecond.
	 * <p>
	 * http://www.w3.org/TR/NOTE-datetime
	 * <p>
	 * The formats are as follows. Exactly the components shown here must be
	 * present, with exactly this punctuation. Note that the "T" appears literally
	 * in the string, to indicate the beginning of the time element, as specified in
	 * ISO 8601.
	 * <pre>
	 *    Year:
	 *       YYYY (eg 1997)
	 *    Year and month:
	 *       YYYY-MM (eg 1997-07)
	 *    Complete date:
	 *       YYYY-MM-DD (eg 1997-07-16)
	 *    Complete date plus hours and minutes:
	 *       YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
	 *    Complete date plus hours, minutes and seconds:
	 *       YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
	 *    Complete date plus hours, minutes, seconds and a decimal fraction of a second
	 *       YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
	 *
	 * where:
	 *
	 *      YYYY = four-digit year
	 *      MM   = two-digit month (01=January, etc.)
	 *      DD   = two-digit day of month (01 through 31)
	 *      hh   = two digits of hour (00 through 23) (am/pm NOT allowed)
	 *      mm   = two digits of minute (00 through 59)
	 *      ss   = two digits of second (00 through 59)
	 *      s    = one or more digits representing a decimal fraction of a second
	 *      TZD  = time zone designator (Z or +hh:mm or -hh:mm)
	 * </pre>
	 */
    public static DateTime parseISO8601(String input) throws ParseException {
    	
    	TimeZone timeZone = null;
    	StringBuilder sb = new StringBuilder(30);
    	String[] dateParts = {"yyyy", "yyyy-MM", "yyyy-MM-dd"};
    	String[] timeParts = {"HH", "HH:mm", "HH:mm:ss"};
    	String[] inputs = input.split("-");
       
       	sb.append(dateParts[Math.min(inputs.length - 1, 2)]);
    
      	int timeIndex = input.indexOf("T");
      	
      	// If we have time parts
      	if (timeIndex > 0)
    	{      		
      		input = input.replace("Z", "+00:00");
      		boolean hasTimeZone = false;
      		int timeZoneLen = 0;
      		
      		String timeStr = input.substring(timeIndex);
      		// If we have time zone
      		//if (StringUtils.contains(timeStr, "[+-]\\d{2}:\\d{2}$"))
      		if (timeStr.indexOf("+") > 0 || timeStr.indexOf("-") > 0)
      		{
      			hasTimeZone = true;
      			timeZoneLen = 5;
      		    timeZone = TimeZone.getTimeZone("GMT" + input.substring(input.length() - 6));
      		    input = StringUtils.replaceLast(input, ":", "");   
      		}
      		
     		String[] timeInputs = input.split(":");
    		sb.append("'T'");    
    		sb.append(timeParts[timeInputs.length - 1]);
    		
    		int indexOfDot = input.indexOf(".");  
    	
    		if (indexOfDot > 0) {
    			
    			sb.append("'.'");
    			
    			int secondFractionLen = input.length() - indexOfDot - 1 - timeZoneLen;
    			
    			// cut to 3 digits, we avoid round up which may cause problem in case of carrying
    			if (secondFractionLen > 3) 
    			{
    				String secondFractionStr = input.substring(indexOfDot, indexOfDot + secondFractionLen + 1);
    				input = input.replace(secondFractionStr, input.substring(indexOfDot, indexOfDot + 4));    	   
    			}
    			
    			for (int j = secondFractionLen; j > 0; j--)
    			{
    				sb.append("S");
    			}    			
    		}
    		if (hasTimeZone)
    			sb.append("Z");    		
    	}
      	
      	SimpleDateFormat df = new SimpleDateFormat(sb.toString());
    	
    	return new DateTime(df.parse(input), timeZone);        
    }   
    
    public static Date yearsAfter(Date date, int years) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.YEAR, years);
		
		return calendar.getTime();		
	}
}