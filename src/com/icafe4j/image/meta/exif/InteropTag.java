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

package com.icafe4j.image.meta.exif;

import java.util.HashMap;
import java.util.Map;

import com.icafe4j.image.tiff.FieldType;
import com.icafe4j.image.tiff.Tag;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.string.StringUtils;

/**
 * Defines Interoperability tags
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/26/2014
 */
public enum InteropTag implements Tag {
	// EXIF InteropSubIFD tags
	INTEROPERABILITY_INDEX("Interoperability Index", (short)0x0001) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	INTEROPERABILITY_VERSION("Interoperability Version", (short)0x0002) {
		public FieldType getFieldType() {
			return FieldType.UNDEFINED;
		}
	},
	RELATED_IMAGE_FILE_FORMAT("Related Image File Format", (short)0x1000) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	RELATED_IMAGE_WIDTH("Related Image Width", (short)0x1001) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	RELATED_IMAGE_LENGTH("Related Image Length", (short)0x1002) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	// unknown tag
	UNKNOWN("Unknown",  (short)0xffff); 
	// End of IneropSubIFD tags
		
	private InteropTag(String name, short value)
	{
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	
	public short getValue() {
		return value;
	}
	
	@Override
    public String toString() {
		if (this == UNKNOWN)
			return name;
		return name + " [Value: " + StringUtils.shortToHexStringMM(value) +"]";
	}
	
    public static Tag fromShort(short value) {
       	InteropTag tag = tagMap.get(value);
    	if (tag == null)
    	   return TiffTag.UNKNOWN;
   		return tag;
    }
    
    private static final Map<Short, InteropTag> tagMap = new HashMap<Short, InteropTag>();
       
    static
    {
      for(InteropTag tag : values()) {
           tagMap.put(tag.getValue(), tag);
      }
    }
    
    /**
     * Intended to be overridden by certain tags to provide meaningful string
     * representation of the field value such as compression, photo metric interpretation etc.
     * 
	 * @param value field value to be mapped to a string
	 * @return a string representation of the field value or empty string if no meaningful string
	 * 	representation exists.
	 */
    public String getFieldAsString(Object value) {
    	return "";
	}
    
    public boolean isCritical() {
    	return true;
    }
	
	public FieldType getFieldType() {
		return FieldType.UNKNOWN;
	}
	
	private final String name;
	private final short value;
}