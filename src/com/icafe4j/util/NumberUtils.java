/**
 * NumberUtility.java
 *
 * Source:  http://www.freejavaguide.com  
 */
package com.icafe4j.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Class provides common functions on number formats.
 */
public class NumberUtils {
	
	/**
     * Method takes Object as parameter and returns decimal number.
     * if argument is float or double and contains tailing zeros
     * it removes them. If argument is float or double then no change in return type.
     * Change the Format of the Number by changing the String Pattern
     */
    public static String changeToDecimalFormat(Object number) {

        BigDecimal bdNumber = new BigDecimal(number.toString());
        bdNumber = bdNumber.stripTrailingZeros();           //Returns a BigDecimal with any trailing zero's removed
        String pattern = "###,##0.0###########";		//To apply formatting when the number of digits in input equals the pattern
        DecimalFormat newFormat = new DecimalFormat(pattern, new DecimalFormatSymbols(Locale.US));
        return newFormat.format(bdNumber);

    }

    /* Some times its required to have a fixed set of decimal places for a 
     * number. We can set that by changing the precision number for a particular
     * input BigDecimal Input String
     */
    public static String changeToRequiredDecimals(String bigDecimalString,
            int precision) {
        String newFormattedString = null;
        String afterDecimal = null;
        if (bigDecimalString == null || bigDecimalString.length() == 0) {
            return "0.0";
        }
        if (bigDecimalString.contains(".")) {
            afterDecimal = bigDecimalString.substring(bigDecimalString
                    .indexOf(".") + 1);
            int length = Math.abs((afterDecimal.length() - precision));
            if (afterDecimal.length() < precision) {
                newFormattedString = bigDecimalString;
                for (int i = 0; i < length; i++) {
                    newFormattedString = newFormattedString + "0";
                }
            } else if (afterDecimal.length() > precision) {
                newFormattedString = bigDecimalString.substring(0,
                        bigDecimalString.length() - length);
                if (precision == 0) {
                    newFormattedString = newFormattedString.substring(0,
                            newFormattedString.indexOf("."));
	            } else {
	                newFormattedString = bigDecimalString;
	            }

	        } else {
		            if (precision > 0)
		                newFormattedString = bigDecimalString + ".";
		            else
		                newFormattedString = bigDecimalString;
		            for (int i = 0; i < precision; i++) {
		                newFormattedString = newFormattedString + "0";
		            }
	        }
        }
        return newFormattedString;
    }
    
    /* Method takes Object as parameter and removes commas from the parameter */
    public static double removeCommasFromNumber(Object number) {
        try {
            StringBuffer inputNo = new StringBuffer(number.toString());
            if (inputNo.length() > 0) {
                while (inputNo.indexOf(",") != -1) {
                    inputNo.deleteCharAt(inputNo.indexOf(","));
                }
            } else {
                return 0.0;
            }
            return Double.parseDouble(inputNo.toString());

        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    public static byte reverseBits(byte x) {
		//return (byte)Integer.reverse(input[i] << 24);
    	int b = 0;
		
    	while (x!=0){
			  b<<=1;
			  b|=( x &1);
			  x>>=1;
		}
		
		return (byte)b;
	}

    public static int reverseBits(int x) {
		//return Integer.reverse(x);
    	int i = 0;
		
		while (x!=0){
			  i <<= 1;
			  i |= (x&1);
			  x >>= 1;
		}
		
		return i;
	}	
}