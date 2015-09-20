package com.icafe4j.io;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class PropertyUtil
{
  private static ResourceBundle b;
  
  public static String getString( String key )
  {
	  if (b == null) {
		  b = getBundle();
	  }
	  return b.getString(key);
  }
  
  /** Get bundle from .properties files in the current dir. */
  private static ResourceBundle getBundle()
  {   
     ResourceBundle bundle = null;
     
     InputStream in = null;
     
     try {
    	 try {
    		 in = PropertyUtil.class.getResourceAsStream("properties");
    	 } catch(Exception e1) {}
     
    	 if(in == null) {
    		 in = new FileInputStream("properties");
    	 }
    	 if (in != null) {
    		 bundle = new PropertyResourceBundle(in);
    		 return bundle;
    	 }
     } catch (Exception e) {
    	 e.printStackTrace();
     }
     
     return null;
  }
}