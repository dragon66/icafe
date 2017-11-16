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

package com.icafe4j.image.meta.iptc;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.icafe4j.string.StringUtils;

/**
 * Defines DataSet tags for IPTC ObjectData Record - Record number 8.
 * * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 07/02/2013
 */
public enum IPTCObjectDataTag implements IPTCTag {
	 SUB_FILE(10, "SubFile"),
		 	 	 
	 UNKNOWN(999, "Unknown");
	 
	 private IPTCObjectDataTag(int tag, String name) {
		 this.tag = tag;
		 this.name = name;
	 }
	 
	 public static IPTCObjectDataTag fromTag(int value) {
		 IPTCObjectDataTag record = recordMap.get(value);
	   	if (record == null)
	   		return UNKNOWN;
      	return record;
	 }
	 
	 public boolean allowMultiple() {
		 return false;
	 }
	 
	 // Default implementation. Could be replaced by individual ENUM
	 public String getDataAsString(byte[] data) {
		 try {
			 String strVal = new String(data, "UTF-8").trim();
			 if(strVal.length() > 0) return strVal;
		 } catch (UnsupportedEncodingException e) {
			 e.printStackTrace();
		 }
		 // Hex representation of the data
		 return StringUtils.byteArrayToHexString(data, 0, 10);
	 }
	 
	 public String getName() {
		 return name;
	 }
	 
	 public int getTag() {
		 return tag;
	 }
  
	 @Override public String toString() {
	   return name;
	 }
  
	 private static final Map<Integer, IPTCObjectDataTag> recordMap = new HashMap<Integer, IPTCObjectDataTag>();
   
	 static
	 {
		 for(IPTCObjectDataTag record : values()) {
			 recordMap.put(record.getTag(), record);
		 }
	 }	    
 
	 private final int tag;
	 private final String name;
}
