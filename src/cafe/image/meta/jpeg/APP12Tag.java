/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * APP12Tag.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package cafe.image.meta.jpeg;

import java.util.HashMap;
import java.util.Map;

public enum APP12Tag {
	//
	QUALITY(1, "Quality"),
	COMMENT(2, "Comment"),
	COPYRIGHT(3, "Copyright"),
	 
	UNKNOWN(999, "Unknown");
	 
	private static final Map<Integer, APP12Tag> recordMap = new HashMap<Integer, APP12Tag>();
	 
	static {
		for(APP12Tag record : values()) {
			recordMap.put(record.getTag(), record);
		}
	}
	 
	public static APP12Tag fromTag(int value) {
		 APP12Tag record = recordMap.get(value);
		 if (record == null)
			 return UNKNOWN;
		 return record;
 	}
	 
	private final int tag;
   
	private final String name;
   
	private APP12Tag(int tag, String name) {
		this.tag = tag;
		this.name = name;
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
}