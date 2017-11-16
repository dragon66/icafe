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
 * ColorSpaceID.java - Adobe Photoshop color space IDs
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    28Jul2015  Initial creation
 */

package com.icafe4j.image.meta.adobe;

import java.util.HashMap;
import java.util.Map;

public enum ColorSpaceID {
	//They are full unsigned 16-bit values as in Apple's RGBColor data structure. Pure red = 65535, 0, 0.
	RGB("RGB: The first three values in the color data are red, green, and blue", 0),
	//They are full unsigned 16-bit values as in Apple's HSVColor data structure. Pure red = 0,65535, 65535.
	HSB("HSB:  The first three values in the color data are hue, saturation, and brightness", 1),
	//They are full unsigned 16-bit values. 0 = 100% ink. For example, pure cyan = 0,65535,65535,65535.
	CMYK("CMYK: The four values in the color data are cyan, magenta, yellow, and black", 2),	 
	PANTONE("Pantone matching system", 3), // Custom color space
	FOCOLTONE("Focoltone colour system", 4), // Custom color space
	TRUMATCH("Trumatch color", 5), // Custom color space
	TOYO88("Toyo 88 colorfinder 1050", 6),
	//Lightness is a 16-bit value from 0...10000. Chrominance components are each 16-bit values from -12800...12700.
	//Gray values are represented by chrominance components of 0. Pure white = 10000,0,0.
	Lab("Lab: The first three values in the color data are lightness, a chrominance, and b chrominance", 7), 
	Grayscale("Grayscale: The first value in the color data is the gray value, from 0...10000", 8),
	HKS("HKS colors", 10),

	UNKNOWN("Unknown", 99);
	
	private ColorSpaceID(String description, int value) {
		this.description = description;
		this.value = value;
	}
	
	public String getDescription() {
		return description;
	}
	
	public int getValue() {
		return value;
	}
	
	public static ColorSpaceID fromInt(int value) {
       	ColorSpaceID id = idMap.get(value);
    	if (id == null)
    		return UNKNOWN;
   		return id;
    }
	
	private static final Map<Integer, ColorSpaceID> idMap = new HashMap<Integer, ColorSpaceID>();
       
    static
    {
      for(ColorSpaceID id : values()) {
           idMap.put(id.getValue(), id);
      }
    }
	
	private final String description;
	private final int value;
}
