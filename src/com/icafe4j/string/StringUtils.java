/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * StringUtils.java
 *
 * Who   Date       Description
 * ====  =========  ==============================================================
 * WY    03May2015  Added rationalToString()
 * WY    04Mar2015  Added toHexString()
 * WY    04Mar2015  Added generateMD5()
 * WY    07Feb201   Added decimalToDMS() and DMSToDecimal()
 * WY    23Jan2015  Moved XML related methods to XMLUtils
 * WY    10Jan2015  Added showXML() and printNode() to show XML document
 * WY    28Dec2014  Added isInCharset() to test if a String can be encoded with
 * 					certain character set.
 */

package com.icafe4j.string;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.*;

/**
 * String utility class  
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 09/18/2012
 */
public class StringUtils {
	
	private static final char[] HEXES = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	/**
	 * Formats byte array.
	 * 
	 * @param bytes an array of byte.
	 * @return a hex string representation of the byte array.
	 */
	public static String byteArrayToHexString(byte[] bytes) {	    
	    return byteArrayToHexString(bytes, 0, bytes.length);
	}
	
	public static String byteArrayToHexString(byte[] bytes, int offset, int length) {		
		if ( bytes == null )
			return null;
		
		if(bytes.length == 0) return "[]";
	    
	    if(offset < 0 || offset >= bytes.length)
	    	throw new IllegalArgumentException("Offset out of array bound!");
	    
	    int endOffset = offset + Math.min(length, bytes.length);
		 
	    if(endOffset > bytes.length)
	    	length = bytes.length - offset;
	    
	    StringBuilder hex = new StringBuilder(5*length + 2);	    
	    hex.append("[");
	    
	    for (int i = offset; i < endOffset; i++) {
	    	hex.append("0x").append(HEXES[(bytes[i] & 0xf0) >> 4])
	         .append(HEXES[bytes[i] & 0x0f]).append(",");
	    }
	    
	    // Remove the last ","
	    if(hex.length() > 1)
	    	hex.deleteCharAt(hex.length()-1);
	    
	    if(endOffset < bytes.length)
	    	hex.append(" ..."); // Partial output
	    
	    hex.append("]");
	    
	    return hex.toString();
	}
	
	public static String byteToHexString(byte b) {
	    return String.format("0x%02X ", b);
	}
	
	/**
	 * Capitalizes the first character of the words in a string.
	 * 
	 * @param s the input string
	 * @return a string with the first character of all words capitalized
	 */
	public static String capitalize(String s) {   
		StringBuffer myStringBuffer = new StringBuffer();
		Pattern p = Pattern.compile("\\b(\\w)(\\w*)");
		Matcher m = p.matcher(s);
		
        while (m.find()) {
			if(!Character.isUpperCase(m.group().charAt(0)))
               m.appendReplacement(myStringBuffer, m.group(1).toUpperCase()+"$2");
        }
        
        return m.appendTail(myStringBuffer).toString();
	}
	
	public static String capitalizeFully(String s) {   
		return capitalize(s.toLowerCase());
	}
	
	public static String concat(Iterable<? extends CharSequence> strings, String delimiter) {
        int capacity = 0;
        int delimLength = delimiter.length();

        Iterator<? extends CharSequence> iter = strings.iterator();
        
        while (iter.hasNext()) {
        	CharSequence next = iter.next();
        	
        	if(!isNullOrEmpty(next))
        		capacity += next.length() + delimLength;
        }

        StringBuilder buffer = new StringBuilder(capacity);
        iter = strings.iterator();
        
        while (iter.hasNext()) {
            CharSequence next = iter.next();
            
            if(!isNullOrEmpty(next)) {
            	buffer.append(next);
            	buffer.append(delimiter);
            }
        }
        
        int lastIndexOfDelimiter = buffer.lastIndexOf(delimiter);
        buffer.delete(lastIndexOfDelimiter, buffer.length());
        
        return buffer.toString();
    }
	
	public static String concat(String first, String second) {
		if(first == null) return second;
		if(second == null) return first;
		
		StringBuilder sb = new StringBuilder(first.length() + second.length());
		sb.append(first);
		sb.append(second);
		
		return sb.toString();
	}
	
	public static String concat(String first, String... strings) {
		StringBuilder sb;
		
		if(first != null) sb = new StringBuilder(first);
		else sb = new StringBuilder();
		
		for (String s: strings) {		
			if(!isNullOrEmpty(s))
				sb.append(s);
	 	}
		
		return sb.toString();
	}
		
	public static <T extends CharSequence> String concat(T[] strings, String delimiter) {
        int capacity = 0;
        int delimLength = delimiter.length();
        
        for (T value : strings) {
        	if(!isNullOrEmpty(value))
        		capacity += value.length() + delimLength;
        }
        
        StringBuilder buffer = new StringBuilder(capacity);
              
        for (T value : strings) {
        	if(!isNullOrEmpty(value)) {
        		buffer.append(value);
            	buffer.append(delimiter);
        	}
        }
        
        int lastIndexOfDelimiter = buffer.lastIndexOf(delimiter);
        buffer.delete(lastIndexOfDelimiter, buffer.length());
        
        return buffer.toString();
    }
	
	/**
	 * Regular expression version of the String contains method.
	 * If used with a match from start or match from end regular expression,
	 * it becomes the regular expression version of the {@link String#
	 * startsWith(String)} or {@link String#endsWith(String)}
	 * methods.
	 * 
	 * @param input the input string
	 * @param regex the regular expression to which this string is to be matched
	 * @return true if a match is found, otherwise false
	 */
	public static boolean contains(String input, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(input);
        
		if (m.find()) {
			return true;
        }
		
		return false;
	}
	
	// From http://stackoverflow.com/questions/15547329/how-to-prettily-format-gps-data-in-java-android
	// Input a double latitude or longitude in the decimal format
	public static String decimalToDMS(double coord) {
		String output;
		int degrees, minutes, seconds;
	    // gets the modulus the coordinate divided by one (MOD1).
	    // in other words gets all the numbers after the decimal point.
	    // e.g. mod = 87.728056 % 1 == 0.728056
	    //
	    // next get the integer part of the coord. In other words the, whole number part.
	    // e.g. intPart = 87
	    double mod = coord % 1;
	    degrees = (int)coord;
	    // next times the MOD1 of degrees by 60 so we can find the integer part for minutes.
	    // get the MOD1 of the new coord to find the numbers after the decimal point.
	    // e.g. coord = 0.728056 * 60 == 43.68336
	    //      mod = 43.68336 % 1 == 0.68336
	    //
	    // next get the value of the integer part of the coord.
	    // e.g. intPart = 43
	    coord = mod * 60;
	    mod = coord % 1;
	    minutes = (int)coord;
	    //do the same again for minutes
	    //e.g. coord = 0.68336 * 60 == 41.0016
	    //e.g. intPart = 41
	    coord = mod * 60;
	    seconds = (int)coord;
	    //Standard output of DÂ°M'S"
	    output = degrees + '\u00B0' + minutes + "'" + seconds + "\"";

	    return output;
	}

	/**
	 * Converts DMS to decimal 
	 *
	 * Input: latitude or longitude in the DMS format ( example: N 43Â° 36' 15.894")
	 * @param hemisphereOUmeridien => {W,E,S,N}
	 * @return latitude or longitude in decimal format
	 */
	public double DMSToDecimal(String hemisphereOUmeridien, double degrees, double minutes, double seconds) {
		double LatOrLon = 0;
		double sign = 1.0;

		if((hemisphereOUmeridien == "W")||(hemisphereOUmeridien == "S")) {
			sign = -1.0;
		}              
		
		LatOrLon = sign*(Math.floor(degrees) + Math.floor(minutes)/60.0 + seconds/3600.0);

		return LatOrLon;               
    }
	
	/**
	 * From www.javapractices.com EscapeChars.java
	 * 
	 * @param url URL string to be encoded
	 * @return a encoded URL string
	 */	
	public static String encodeURL(String url) {
		String result = null;
	    
		try {
	       result = URLEncoder.encode(url, "UTF-8");
	    }
	    catch (UnsupportedEncodingException ex){
	       throw new RuntimeException("UTF-8 not supported", ex);
	    }
	    
		return result;
	}
		
	/**
	 * Escapes HTML reserved characters and other characters which might cause Cross Site Scripting
	 * (XSS) hacks
	 *
	 * The following table comes from www.javapractice.com EscapeChars.java
	 * 
	 * <P>The following characters are replaced with corresponding HTML character entities:
	 * 
     * <table border='1' cellpadding='3' cellspacing='0'>
     *  <tr><th> Character </th><th>Replacement</th></tr>
     *  <tr><td> < </td><td> &lt; </td></tr>
     *  <tr><td> > </td><td> &gt; </td></tr>
     *  <tr><td> & </td><td> &amp; </td></tr>
     *  <tr><td> " </td><td> &quot;</td></tr>
     *  <tr><td> \t </td><td> &#009;</td></tr>
     *  <tr><td> ! </td><td> &#033;</td></tr>
     *  <tr><td> # </td><td> &#035;</td></tr>
     *  <tr><td> $ </td><td> &#036;</td></tr>
     *  <tr><td> % </td><td> &#037;</td></tr>
     *  <tr><td> ' </td><td> &#039;</td></tr>
     *  <tr><td> ( </td><td> &#040;</td></tr> 
     *  <tr><td> ) </td><td> &#041;</td></tr>
     *  <tr><td> * </td><td> &#042;</td></tr>
     *  <tr><td> + </td><td> &#043; </td></tr>
     *  <tr><td> , </td><td> &#044; </td></tr>
     *  <tr><td> - </td><td> &#045; </td></tr>
     *  <tr><td> . </td><td> &#046; </td></tr>
     *  <tr><td> / </td><td> &#047; </td></tr>
     *  <tr><td> : </td><td> &#058;</td></tr>
     *  <tr><td> ; </td><td> &#059;</td></tr>
     *  <tr><td> = </td><td> &#061;</td></tr>
     *  <tr><td> ? </td><td> &#063;</td></tr>
     *  <tr><td> @ </td><td> &#064;</td></tr>
     *  <tr><td> [ </td><td> &#091;</td></tr>
     *  <tr><td> \ </td><td> &#092;</td></tr>
     *  <tr><td> ] </td><td> &#093;</td></tr>
     *  <tr><td> ^ </td><td> &#094;</td></tr>
     *  <tr><td> _ </td><td> &#095;</td></tr>
     *  <tr><td> ` </td><td> &#096;</td></tr>
     *  <tr><td> { </td><td> &#123;</td></tr>
     *  <tr><td> | </td><td> &#124;</td></tr>
     *  <tr><td> } </td><td> &#125;</td></tr>
     *  <tr><td> ~ </td><td> &#126;</td></tr>
     * </table>
     *
	 * @return a string with the specified characters replaced by HTML entities
	 */
	public static String escapeHTML(String input) {
		Iterator<Character> itr = stringIterator(input);
		StringBuilder result = new StringBuilder();		
		
		while (itr.hasNext())
		{
			Character c = itr.next();
			
			switch (c)
			{				
				case '<':
					result.append("&lt;");
					break;
				case '>':
					result.append("&gt;");
					break;
				case '&':
					result.append("&amp;");
					break;
				case '"':
					result.append("&quot;");
					break;
				case '\t':
					result.append("&#009;");
					break;
				case '!':
					result.append("&#033;");
					break;
				case '#':
					result.append("&#035;");
					break;
				case '$':
					result.append("&#036;");
					break;
				case '%':
					result.append("&#037;");
					break;
				case '\'':
					result.append("&#039;");
					break;
				case '(':
					result.append("&#040;");
					break;
				case ')':
					result.append("&#041;");
					break;
				case '*':
					result.append("&#042;");
					break;
				case '+':
					result.append("&#043;");
					break;
				case ',':
					result.append("&#044;");
					break;
				case '-':
					result.append("&#045;");
					break;
				case '.':
					result.append("&#046;");
					break;
				case '/':
					result.append("&#047;");
					break;
				case ':':
					result.append("&#058;");
					break;
				case ';':
					result.append("&#059;");
					break;
				case '=':
					result.append("&#061;");
					break;
				case '?':
					result.append("&#063;");
					break;
				case '@':
					result.append("&#064;");
					break;
				case '[':
					result.append("&#091;");
					break;
				case '\\':
					result.append("&#092;");
					break;
				case ']':
					result.append("&#093;");
					break;
				case '^':
					result.append("&#094;");
					break;
				case '_':
					result.append("&#095;");
					break;
				case '`':
					result.append("&#096;");
					break;
				case '{':
					result.append("&#123;");
					break;
				case '|':
					result.append("&#124;");
					break;
				case '}':
					result.append("&#125;");
					break;
				case '~':
					result.append("&#126;");
					break;
				default:
					result.append(c);
			}
		}
		
		return result.toString();
	}
	
	/**
	 * Replaces "&" with its entity "&amp;" to make it a valid HTML link
	 * 
	 * @param queryString a URL string with a query string attached
	 * @return a valid URL string to be used as a link
	 */
	public static String escapeQueryStringAmp(String queryString) {
		return queryString.replace("&", "&amp;");
	}
	
	public static String escapeRegex(String input) {
		Iterator<Character> itr = stringIterator(input);
		StringBuilder result = new StringBuilder();		
		
		while (itr.hasNext())
		{
			Character c = itr.next();
			
			switch (c)
			{
				case '.':
				case '^':
				case '$':
				case '*':
				case '+':
				case '?':
				case '(':
				case ')':
				case '[':
				case '{':
					result.append("\\").append(c);
					break;
				case '\\':
					result.append("\\\\");
				    break;
				default:
					result.append(c);
			}
		}
		
		return result.toString();
	}
	
	/**
	 * Generate MD5 digest from a byte array
	 * 
	 * @param message byte array to generate MD5
	 * @return MD5 string
	 */
	public static String generateMD5(byte[] message) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("No such algorithm: MD5");
		}
		
		return toHexString(md.digest(message));
    }
   
	public static String intToHexString(int value) {
		StringBuilder buffer = new StringBuilder(10);
		
		buffer.append("0x");		
		
		buffer.append(HEXES[(value & 0x0000000F)]);
		buffer.append(HEXES[(value & 0x000000F0) >>> 4]);
		buffer.append(HEXES[(value & 0x00000F00) >>> 8]);
		buffer.append(HEXES[(value & 0x0000F000) >>> 12]);
		buffer.append(HEXES[(value & 0x000F0000) >>> 16]);
		buffer.append(HEXES[(value & 0x00F00000) >>> 20]);
		buffer.append(HEXES[(value & 0x0F000000) >>> 24]);
		buffer.append(HEXES[(value & 0xF0000000) >>> 28]);
		
		return buffer.toString();
	}

	public static String intToHexStringMM(int value) {
		
		StringBuilder buffer = new StringBuilder(10);
		
		buffer.append("0x");
		
		buffer.append(HEXES[(value & 0xF0000000) >>> 28]);
		buffer.append(HEXES[(value & 0x0F000000) >>> 24]);
		buffer.append(HEXES[(value & 0x00F00000) >>> 20]);
		buffer.append(HEXES[(value & 0x000F0000) >>> 16]);
		buffer.append(HEXES[(value & 0x0000F000) >>> 12]);
		buffer.append(HEXES[(value & 0x00000F00) >>> 8]);
		buffer.append(HEXES[(value & 0x000000F0) >>> 4]);
		buffer.append(HEXES[(value & 0x0000000F)]);
		
		return buffer.toString();
	}
	
	public static boolean isInCharset(String input, String encoding) {
		Charset charset = null;
		try {
			// May throw different unchecked exceptions
			charset = Charset.forName(encoding);
		} catch(Exception ex) {
			ex.printStackTrace();
			return false;
		}
	    // Convert input into byte array and encode it again using the
	    // same character set and see if we get the same string
	    String output = new String(input.getBytes(charset), charset);
	    
	    return output.equals(input);
	}
	
	/**
	 * Checks if a string is null, empty, or consists only of white spaces
	 * 
	 * @param str the input CharSequence to check
	 * @return true if the input string is null, empty, or contains only white
	 * spaces, otherwise false
	 */
	public static boolean isNullOrEmpty(CharSequence str) {
		return ((str == null) || (str.length() == 0));
	}
	
	/**
	 * Formats TIFF long data field.
	 * 
	 * @param data an array of int.
	 * @param unsigned true if the int value should be treated as unsigned,
	 * 		  otherwise false 
	 * @return a string representation of the int array.
	 */
	public static String longArrayToString(int[] data, boolean unsigned) {
		
		return longArrayToString(data, 0, data.length, unsigned);
	}
	
	public static String longArrayToString(int[] data, int offset, int length, boolean unsigned) {
		if ( data == null ) {
		      return null;
		}
			
		if(data.length == 0) return "[]";
	    
	    if(offset < 0 || offset >= data.length)
	    	throw new IllegalArgumentException("Offset out of array bound!");
	    
	    int endOffset = offset + Math.min(length, data.length);
		 
	    if(endOffset > data.length)
	    	length = data.length - offset;
	    
	    StringBuilder longs = new StringBuilder();	    
	    longs.append("[");
		    
	    for (int i = offset; i < endOffset; i++)
		{
	    	if(unsigned) {
				// Convert it to unsigned integer
				longs.append(data[i]&0xffffffffL);
			} else {
				longs.append(data[i]);
			}
			longs.append(",");
		}
	    
	    // Remove the last ","
	    if(longs.length() > 1)
	    	longs.deleteCharAt(longs.length()-1);
	    
	    if(endOffset < data.length)
	    	longs.append(" ..."); // Partial output
	    
	    longs.append("]");
	    
	    return longs.toString();	    
	}
	
	public static boolean parseBoolean(String s) {
		return Boolean.parseBoolean(s);
	}
	
	public static byte parseByte(String s) {
		return Byte.parseByte(s);
	}
	
	public static byte parseByte(String s, int radix) {
		return Byte.parseByte(s, radix);
	}
	
	public static double parseDouble(String s) {
		return Double.parseDouble(s);
	}
	
	public static float parseFloat(String s) {
		return Float.parseFloat(s);
	}
	
	public static int parseInt(String s) {
		return Integer.parseInt(s);
	}
	
	public static int parseInt(String s, int radix) {
		return Integer.parseInt(s, radix);
	}		
	
	public static long parseLong(String s) {
		return Long.parseLong(s);
	}
	
	public static long parseLong(String s, int radix) {
		return Long.parseLong(s, radix);
	}
	
	public static short parseShort(String s) {
		return Short.parseShort(s);
	}
	
	public static short parseShort(String s, int radix) {
		return Short.parseShort(s, radix);
	}	
	
	public static String quoteRegexReplacement(String replacement)
	{
		return Matcher.quoteReplacement(replacement);
	}
	
	/**
	 * Formats TIFF rational data field.
	 * 
	 * @param data an array of int.
	 * @param unsigned true if the int value should be treated as unsigned,
	 * 		  otherwise false 
	 * @return a string representation of the int array.
	 */
	public static String rationalArrayToString(int[] data, boolean unsigned) {
		if(data.length%2 != 0)
			throw new IllegalArgumentException("Data length is odd number, expect even!");

		StringBuilder rational = new StringBuilder();
		rational.append("[");
		
		for (int i=0; i<data.length; i+=2)
		{
			long  numerator = data[i], denominator = data[i+1];
			
			//if(denominator == 0) throw new ArithmeticException("Divided by zero");
			
			if (unsigned) {
				// Converts it to unsigned integer
				numerator = (data[i]&0xffffffffL);
				denominator = (data[i+1]&0xffffffffL);
			}
			
			rational.append(numerator);			
			rational.append("/");
			rational.append(denominator);
			
			rational.append(",");
		}
		
		rational.deleteCharAt(rational.length()-1);
		rational.append("]");
		
		return rational.toString();
	}
	
	public static String rationalToString(DecimalFormat df, boolean unsigned, int ... rational) {
		if(rational.length < 2) throw new IllegalArgumentException("Input data length is too short");
		if(rational[1] == 0) throw new ArithmeticException("Divided by zero");
		
		long numerator = rational[0];
		long denominator= rational[1];
		
		if (unsigned) {
			// Converts it to unsigned integer
			numerator = (numerator&0xffffffffL);
			denominator = (denominator&0xffffffffL);
		}
		
		return df.format(1.0*numerator/denominator);
	}
	
	/**
	 * Replaces the last occurrence of the string represented by the regular expression
	 *  
	 * @param input input string
 	 * @param regex the regular expression to which this string is to be matched
	 * @param replacement the string to be substituted for the match
	 * @return the resulting String
	 */
	public static String replaceLast(String input, String regex, String replacement) {
		return input.replaceAll(regex+"(?!.*"+regex+")", replacement); // Using negative look ahead
	}
	
	public static String reverse(String s) {
		if(s == null) return null;
		
		return new StringBuilder(s).reverse().toString();
	}
	
	// This method will not work for surrogate pairs
	public static String reverse2(String s) {
		if(s == null) return null;
		
		int i, len = s.length();
	    StringBuilder dest = new StringBuilder(len);

	    for (i = (len - 1); i >= 0; i--)
	       dest.append(s.charAt(i));
	    
	    return dest.toString();
	}
	
	public static String reverse(String str, String delimiter) {	
		if(isNullOrEmpty(delimiter)) {
			return str;
		}
		
		StringBuilder sb = new StringBuilder(str.length());
		reverseIt(str, delimiter, sb);
		
		return sb.toString();
	}
	
	public static String reverse2(String str, String delimiter) {
		if(isNullOrEmpty(delimiter) || isNullOrEmpty(str) || (str.trim().length() == 0) || (str.indexOf(delimiter) < 0)) {
			return str;
		} 			
	
		String escaptedDelimiter = escapeRegex(delimiter);
		// Keep the trailing white spaces by setting limit to -1	
		String[] stringArray = str.split(escaptedDelimiter, -1);
		StringBuilder sb = new StringBuilder(str.length() + delimiter.length());
			
		for (int i = stringArray.length-1; i >= 0; i--)
		{
			sb.append(stringArray[i]).append(delimiter);
		}

		return sb.substring(0, sb.lastIndexOf(delimiter));
	}
	
	private static void reverseIt(String str, String delimiter, StringBuilder sb) {
		if(isNullOrEmpty(str) || (str.trim().length() == 0) || str.indexOf(delimiter) < 0) {
			sb.append(str);
			return;
		}	
		// Recursion
		reverseIt(str.substring(str.indexOf(delimiter)+delimiter.length()), delimiter, sb);
		sb.append(delimiter);
		sb.append(str.substring(0, str.indexOf(delimiter)));
	}
		
	public static String reverseWords(String s) {
		String[] stringArray = s.split("\\b");
		StringBuilder sb = new StringBuilder(s.length());
		
		for (int i = stringArray.length-1; i >= 0; i--)
		{
			sb.append(stringArray[i]);
		}
		
		return sb.toString();
	}
	
	/**
	 * Formats TIFF short data field.
	 * 
	 * @param data an array of short.
	 * @param unsigned true if the short value should be treated as unsigned,
	 * 		  otherwise false 
	 * @return a string representation of the short array.
	 */
	public static String shortArrayToString(short[] data, boolean unsigned) {
		return shortArrayToString(data, 0, data.length, unsigned);
	}
	
	public static String shortArrayToString(short[] data, int offset, int length, boolean unsigned) {
		if ( data == null ) {
		      return null;
		}
			
		if(data.length == 0) return "[]";
	    
	    if(offset < 0 || offset >= data.length)
	    	throw new IllegalArgumentException("Offset out of array bound!");
	    
	    int endOffset = offset + Math.min(length, data.length);
		 
	    if(endOffset > data.length)
	    	length = data.length - offset;
	    
	    StringBuilder shorts = new StringBuilder();	    
	    shorts.append("[");
		    
	    for (int i = offset; i < endOffset; i++)
		{
			if(unsigned) {
				// Convert it to unsigned short
				shorts.append(data[i]&0xffff);
			} else {
				shorts.append(data[i]);
			}
			shorts.append(",");
		}
	    
	    // Remove the last ","
	    if(shorts.length() > 1)
	    	shorts.deleteCharAt(shorts.length()-1);
	    
	    if(endOffset < data.length)
	    	shorts.append(" ..."); // Partial output
	    
	    shorts.append("]");
		
		return shorts.toString();
	}
	
	public static String shortToHexString(short value) {
		StringBuilder buffer = new StringBuilder(6);
		
		buffer.append("0x");		
		
		buffer.append(HEXES[(value & 0x000F)]);
		buffer.append(HEXES[(value & 0x00F0) >>> 4]);
		buffer.append(HEXES[(value & 0x0F00) >>> 8]);
		buffer.append(HEXES[(value & 0xF000) >>> 12]);
		
		return buffer.toString();
	}
	
	public static String shortToHexStringMM(short value) {
		
		StringBuilder buffer = new StringBuilder(6);
		
		buffer.append("0x");
		
		buffer.append(HEXES[(value & 0xF000) >>> 12]);
		buffer.append(HEXES[(value & 0x0F00) >>> 8]);
		buffer.append(HEXES[(value & 0x00F0) >>> 4]);
		buffer.append(HEXES[(value & 0x000F)]);
		
		return buffer.toString();
	}
	
	/**
	 *  Converts stack trace to string
	 */
    public static String stackTraceToString(Throwable e) {  
        StringWriter sw = new StringWriter();  
        e.printStackTrace(new PrintWriter(sw));
        
        return sw.toString();   
    }
	
	/**
	 * A read-only String iterator from stackoverflow.com
	 * 
	 * @param string input string to be iterated
	 * @return an iterator for the input string
	 */
	public static Iterator<Character> stringIterator(final String string) {
		// Ensure the error is found as soon as possible.
		if (string == null)
			throw new NullPointerException();

		return new Iterator<Character>() {
			private int index = 0;

			public boolean hasNext() {
				return index < string.length();
			}

			public Character next() {
				/*
				 * Throw NoSuchElementException as defined by the Iterator contract,
				 * not IndexOutOfBoundsException.
				 */
				if (!hasNext())
					throw new NoSuchElementException();
				return string.charAt(index++);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	/**
	 * Convert plain text to HTML - from Stackoverflow.com
	 * 
	 * @param s the original text string
	 * @return a string with the specified characters replaced by HTML entities and HTML tags
	 */
	public static String text2HTML(String s) {
	    StringBuilder builder = new StringBuilder();
	    boolean previousWasASpace = false;
	    for( char c : s.toCharArray() ) {
	        if( c == ' ' ) {
	            if( previousWasASpace ) {
	                builder.append("&nbsp;");
	                previousWasASpace = false;
	                continue;
	            }
	            previousWasASpace = true;
	        } else {
	            previousWasASpace = false;
	        }
	        switch(c) {
	            case '<': builder.append("&lt;"); break;
	            case '>': builder.append("&gt;"); break;
	            case '&': builder.append("&amp;"); break;
	            case '"': builder.append("&quot;"); break;
	            case '\n': builder.append("<br>"); break;
	            // We need Tab support here, because we print StackTraces as HTML
	            case '\t': builder.append("&nbsp; &nbsp; &nbsp;"); break;  
	            default:
	                if( c < 128 ) {
	                    builder.append(c);
	                } else {
	                    builder.append("&#").append((int)c).append(";");
	                }    
	        }
	    }
	    return builder.toString();
	}
	
	public static String toHexString(byte[] bytes) {
		return toHexString(bytes, 0, bytes.length);
	}
	
	/**
	 * Convert byte array to hex string
	 * 
	 * @param bytes input byte array
	 * @param offset start offset
	 * @param length number of items to include
	 *  
	 * @return a hex string representation for the byte array without 0x prefix
	 */
	public static String toHexString(byte[] bytes, int offset, int length) {		
		if ( bytes == null )
			return null;
	    
		if(bytes.length == 0) return "";
	    
	    if(offset < 0 || offset >= bytes.length)
	    	throw new IllegalArgumentException("Offset out of array bound!");
	    
	    int endOffset = offset + Math.min(length, bytes.length);
		 
	    if(endOffset > bytes.length)
	    	length = bytes.length - offset;
	    
	    StringBuilder hex = new StringBuilder(5*length + 2);	    
		    
	    for (int i = offset; i < endOffset; i++) {
	    	hex.append(HEXES[(bytes[i] & 0xf0) >> 4])
	         .append(HEXES[bytes[i] & 0x0f]);
	    }
	    
	    return hex.toString();
	}
	
	public static String toUTF16BE(byte[] data, int start, int length) {
		String retVal = "";
		
		 try {
			 retVal = new String(data, start, length, "UTF-16BE");
		 } catch (UnsupportedEncodingException e) {
			 e.printStackTrace();
		 }
		
		 return retVal;		 
	}
	
	private StringUtils(){} // Prevents instantiation	
}
