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

package com.icafe4j.image.bmp;

import java.util.HashMap;
import java.util.Map;

/**
 * BMP compression type.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/09/2014
 */
public enum BmpCompression {
	//
	BI_RGB("No Compression", 0),
	BI_RLE8("8 bit RLE Compression (8 bit only)", 1),
	BI_RLE4("4 bit RLE Compression (4 bit only)", 2),
	BI_BITFIELDS("No compression (16 & 32 bit only)", 3),
	
	UNKNOWN("Unknown", 9999);
	
	private BmpCompression(String description, int value) {
		this.description = description;
		this.value = value;
	}
	
	public String getDescription() {
		return description;
	}
	
	public int getValue() {
		return value;
	}
	
	@Override
    public String toString() {
		return description;
	}
	
	public static BmpCompression fromInt(int value) {
       	BmpCompression compression = typeMap.get(value);
    	if (compression == null)
    	   return UNKNOWN;
      	return compression;
    }
    
    private static final Map<Integer, BmpCompression> typeMap = new HashMap<Integer, BmpCompression>();
       
    static
    {
      for(BmpCompression compression : values())
    	  typeMap.put(compression.getValue(), compression);
    } 

	private String description;
	private int value;
}