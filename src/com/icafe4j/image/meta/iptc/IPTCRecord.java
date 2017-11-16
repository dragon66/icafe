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

import java.util.HashMap;
import java.util.Map;

/**
 * Defines IPTC data set record number
 * * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/26/2015
 */
public enum IPTCRecord {
	ENVELOP(1, "Envelop"), APPLICATION(2, "Application"), NEWSPHOTO(3, "NewsPhoto"),
	PRE_OBJECTDATA(7, "PreObjectData"), OBJECTDATA(8, "ObjectData"), POST_OBJECTDATA(9, "PostObjectData"),
	FOTOSTATION(240, "FotoStation"), UNKNOWN(999, "Unknown");	
	
	private IPTCRecord(int recordNumber, String name) {
		this.recordNumber = recordNumber;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public int getRecordNumber() {
		return recordNumber;
	}
	
	@Override public String toString() {
		return name;
	}
	
	public static IPTCRecord fromRecordNumber(int value) {
      	IPTCRecord record = recordMap.get(value);
	   	if (record == null)
	   		return UNKNOWN;
   		return record;
	}
	
	private static final Map<Integer, IPTCRecord> recordMap = new HashMap<Integer, IPTCRecord>();
	   
	static
	{
		for(IPTCRecord record : values()) {
			recordMap.put(record.getRecordNumber(), record);
		}
	}	    
 	
	private final int recordNumber;
	private final String name;
}
