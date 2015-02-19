/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.iptc;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import cafe.string.StringUtils;

/**
 * Defines DataSet tags for IPTC FotoStation Record - Record number 240.
 * * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 07/02/2013
 */
public enum IPTCFotoStationTag implements IPTCTag  {
	// No record available
	UNKNOWN(999, "Unknown");
	 
	private IPTCFotoStationTag(int tag, String name) {
		this.tag = tag;
		this.name = name;
	}
	 
	public static IPTCFotoStationTag fromTag(int value) {
		IPTCFotoStationTag record = recordMap.get(value);
	   	if (record == null)
	   		return UNKNOWN;
		return record;
	}
	
	 @Override
	 public boolean allowMultiple() {
		 return false;
	 }
	 
	 public String getDataAsString(byte[] data) {
		 try {
			 return new String(data, "UTF-8").trim();
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
	 
	private static final Map<Integer, IPTCFotoStationTag> recordMap = new HashMap<Integer, IPTCFotoStationTag>();
	  
	static
	{
	    for(IPTCFotoStationTag record : values()) {
	        recordMap.put(record.getTag(), record);
	    }
	}	    
	
	private final int tag;
	private final String name;
}