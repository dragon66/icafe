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

package com.icafe4j.util;

import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A common language utility class
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 09/19/2012
 */
public class LangUtils {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(LangUtils.class);
		
	private LangUtils(){} // Prevents instantiation
	
	// TODO: need to rewrite this method (may have to create and return a new class Rational)
	public static long[] doubleToRational(double number) {
		// Code below doesn't work for 0 and NaN - just check before
		if((number == 0.0d) || Double.isNaN(number)) {
			throw new IllegalArgumentException(number + " cannot be represented as a rational number");
		}
		
		long bits = Double.doubleToLongBits(number);

		long sign = bits >>> 63;
		long exponent = ((bits >>> 52) ^ (sign << 11)) - 1023;
		long fraction = bits << 12; // bits are "reversed" but that's not a problem

		long a = 1L;
		long b = 1L;

		for (int i = 63; i >= 12; i--) {
		    a = a * 2 + ((fraction >>> i) & 1);
		    b *= 2;
		}

		if (exponent > 0)
		    a *= 1 << exponent;
		else
		    b *= 1 << -exponent;

		if (sign == 1)
		    a *= -1;

		return new long[]{a, b};
	}
	
	// From Effective Java 2nd Edition. 
	// Use of asSubclass to safely cast to a bounded type token
	public static Annotation getAnnotation(AnnotatedElement element, String annotationTypeName) {
	     Class<?> annotationType = null; // Unbounded type token
	
	     try {
	            annotationType = Class.forName(annotationTypeName);
	     } catch (Exception ex) {
	            throw new IllegalArgumentException(ex);
	     }
	
	     return element.getAnnotation(annotationType.asSubclass(Annotation.class));
	}
	
	// Creates a friendly string representation of the class
	public static String getClassName(Class<?> c) {
	    String name = c.getName().replace('$','.');
	    
	    if (c.isArray()) {
	    	switch (name.charAt(1)) {
			    case 'B':
					name = "byte";
					break;
				case 'C':
					name = "char";
					break;
				case 'D':
					name = "double";
					break;
			    case 'F':
					name = "float";
					break;
			    case 'I':
					name = "int";
					break;
			    case 'J':
					name = "long";
					break;
			    case 'L':
					name = name.substring(2, name.length() - 1);
					break;
			    case 'S':
					name = "short";
					break;
			    case 'Z':
					name = "boolean";
					break;
	    	}
			name = name + "[]";
	    }
	    
	    return name;
	}
	
	/**
	 * @param m Method we want to probe generic type arguments.
	 * @param i the i'th parameter of the method.
	 * @return an array of parameterized Types for the i'th argument or an empty array. 
	 */
	public static Type[] getGenericTypeArguments(Method m, int i) {		 
		 try {
			 Type t = m.getGenericParameterTypes()[i];
		 
			 if(t instanceof ParameterizedType) {
				 ParameterizedType pt = (ParameterizedType) t;
				 return pt.getActualTypeArguments();
			 }
		 } catch(Exception e) {
			 LOGGER.error("Error probing generic type arguments!", e);
	     }
		 
		 return new Type[]{};
	}
	
	public static void log(String message, PrintStream out) {
		StackTraceElement se = Thread.currentThread().getStackTrace()[2];
		out.println("; " + message + " - [" + se.getClassName() + "." + se.getMethodName() +"(): line " + se.getLineNumber() + "]");		
	}
	
	/** Java language specific classes return null cSource */
	public static URL getLoadedClassLocation(Class<?> cls) {
		ProtectionDomain pDomain = cls.getProtectionDomain();
		CodeSource cSource = pDomain.getCodeSource();
		URL loc = (cSource==null)?null:cSource.getLocation(); 
		
		return loc; 
	}
	
	/**
	 * @param className A fully qualified class name with package information
	 * @return The location where the class has been loaded by the Java Virtual
	 * Machine or null.
	 */
	public static URL getLoadedClassLocation(String className) {
		Class<?> cls = null;
		
		try	{
			cls = Class.forName(className);
		} catch (ClassNotFoundException ex)	{
			return null;			
		}
		
		return getLoadedClassLocation(cls);
	}
	
	public static URL getLoadedClassURL(String className) {
		Class<?> cls = null;
		
		try	{
			cls = Class.forName(className);
		} catch (ClassNotFoundException ex) { 
			return null;			
		}
		
		ClassLoader classLoader = cls.getClassLoader();

		URL url = classLoader.getResource(className.replaceAll(Pattern.quote("."), "/") + ".class");
		
		return url;
	}
	
	// A convenience way to call main of other classes.
	// Based on something I am not sure where I got it.
	public static void invokeMain(String... args) {
		try {
		    Class<?> c = Class.forName(args[0]);
			Class<String[]> argTypes = String[].class;
			Method main = c.getDeclaredMethod("main", argTypes);
	  	    Object mainArgs = Arrays.copyOfRange(args, 1, args.length);
		    LOGGER.info("invoking {}.main()\n", c.getName());
		    main.invoke(null, mainArgs);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Converts long value to int hash code.
	 * 
	 * @param value long value
	 * @return int hash code for the long
	 */
	public static int longToIntHashCode(long value) {
		return Long.valueOf(value).hashCode();
	}
		
	// From stackoverflow.com
	public static URI relativize(URI base, URI child) {
		 // Normalize paths to remove . and .. segments
		 base = base.normalize();
		 child = child.normalize();

		 // Split paths into segments
		 String[] bParts = base.getPath().split("/");
		 String[] cParts = child.getPath().split("/");

		 // Discard trailing segment of base path
		 if (bParts.length > 0 && !base.getPath().endsWith("/")) {
		     System.arraycopy(bParts, 0, bParts, 0, bParts.length - 1);
			 // JDK1.6+ 
			 //bParts = java.util.Arrays.copyOf(bParts, bParts.length - 1);
		 }

		 // Remove common prefix segments
		 int i = 0;
		  
		 while (i < bParts.length && i < cParts.length && bParts[i].equals(cParts[i])) {
			i++;
		 }

		 // Construct the relative path
		 StringBuilder sb = new StringBuilder();
		  
		 for (int j = 0; j < (bParts.length - i); j++) {
			sb.append("../");
		 }
		  
		 for (int j = i; j < cParts.length; j++) {
			if (j != i) {
			  sb.append("/");
			}
			sb.append(cParts[j]);
		 }
		  
		 return URI.create(sb.toString());
	}	
}