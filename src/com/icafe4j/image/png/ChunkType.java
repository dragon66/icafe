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

package com.icafe4j.image.png;

import java.util.HashMap;
import java.util.Map;

/**
 * Define PNG chunk types
 * 
 * @author Wen Yu, yuwen_66@yahoo.com 
 * @version 1.0 10/16/2012
 */
public enum ChunkType {	
	// Four critical chunks
	IHDR("IHDR", 0x49484452, Attribute.CRITICAL, 1), // PNG header, must be the first one
	IDAT("IDAT", 0x49444154, Attribute.CRITICAL, 60), // PNG data, could have multiple but must appear consecutively
	IEND("IEND", 0x49454E44, Attribute.CRITICAL, 100), // End of image, must be the last one
	PLTE("PLTE", 0x504C5445, Attribute.CRITICAL, 40), // ColorPalette, must precede the first IDAT
	// Fourteen ancillary chunks	
	TEXT("tEXt", 0x74455874, Attribute.ANCILLARY, 20), // Anywhere between IHDR and IEND
	ZTXT("zTXt", 0x7A545874, Attribute.ANCILLARY, 20), // Anywhere between IHDR and IEND
	ITXT("iTXt", 0x69545874, Attribute.ANCILLARY, 20), // Anywhere between IHDR and IEND
	TRNS("tRNS", 0x74524E53, Attribute.ANCILLARY, 50), // Must precede the first IDAT chunk and must follow the PLTE chunk
	GAMA("gAMA", 0x67414D41, Attribute.ANCILLARY, 30), // Must precede the first IDAT chunk and the PLTE chunk if present
	CHRM("cHRM", 0x6348524D, Attribute.ANCILLARY, 30), // Must precede the first IDAT chunk and the PLTE chunk if present
	SRGB("sRGB", 0x73524742, Attribute.ANCILLARY, 30), // Must precede the first IDAT chunk and the PLTE chunk if present
	ICCP("iCCP", 0x69434350, Attribute.ANCILLARY, 30), // Must precede the first IDAT chunk and the PLTE chunk if present
	BKGD("bKGD", 0x624B4744, Attribute.ANCILLARY, 50), // Must precede the first IDAT chunk and must follow the PLTE chunk
	PHYS("pHYs", 0x70485973, Attribute.ANCILLARY, 30), // Must precede the first IDAT chunk
	SBIT("sBIT", 0x73424954, Attribute.ANCILLARY, 30), // Must precede the first IDAT chunk and the PLTE chunk if present
	SPLT("sPLT", 0x73504C54, Attribute.ANCILLARY, 30), // Must precede the first IDAT chunk
	HIST("hIST", 0x68495354, Attribute.ANCILLARY, 50), // Must precede the first IDAT chunk and must follow the PLTE chunk
	TIME("tIME", 0x74494D45, Attribute.ANCILLARY, 20), // Anywhere between IHDR and IEND
    
	UNKNOWN("UNKNOWN",  0x00000000, Attribute.ANCILLARY, 99); // We don't know this chunk, ranking it right before IEND
	
	/**
	 * We made Attribute public for general usage outside of Attribute class.
	 *
	 * Nested enum types are implicitly static. 
	 */
    public enum Attribute {
    	CRITICAL {
    		   public String[] getNames() { 
    			   // Use clone() to prevent users from changing the values of the internal array elements 
    			   return CRITICAL_NAMES.clone(); 
    		   }
    		   
    		   public int[] getValues() {
    			   return CRITICAL_VALUES.clone();
    		   }
    	},
    	ANCILLARY {
    		   public String[] getNames() { 
 			       return ANCILLARY_NAMES.clone();
 		       }
 		   
 		       public int[] getValues() {
 			       return ANCILLARY_VALUES.clone();
 		       }
    	};
    	
    	public abstract String[] getNames();
    	public abstract int[] getValues();
    	
    	private static final String[] CRITICAL_NAMES = {"IHDR","IDAT","IEND","PLTE"}; 
    	private static final String[] ANCILLARY_NAMES = {
    		                             "tEXt","zTXt","iTXt","tRNS","gAMA","cHRM","sRGB",
    		                             "iCCP","bKGD","pHYs","sBIT","sPLT","hIST","tIME"
    		                            };
    	private static final int[] CRITICAL_VALUES = {0x49484452,0x49444154,0x49454E44,0x504C5445};
    	private static final int[] ANCILLARY_VALUES = {
    		                             0x74455874,0x7A545874,0x69545874,0x74524E53,0x67414D41,0x6348524D,0x73524742,
    		                             0x69434350,0x624B4744,0x70485973,0x73424954,0x73504C54,0x68495354,0x74494D45
    		                            };
    } // End of Attribute definition
    
    private ChunkType(String name, int value, Attribute attribute, int ranking)
    {
    	this.name = name;
    	this.value = value;
        this.attribute = attribute;	
        this.ranking = ranking;
    }    
    
    public Attribute getAttribute()
    {
    	return this.attribute;
    }
    
    public String getName()
    {
    	return this.name;
    }
    
    public int getValue()
    {
    	return this.value;
    }
    /**
     * Ranking is used for sorting chunks to make them conform to PNG specification 
     * before passing them to PNGWriter. 
     *  
     * @return The ranking of the chunk for this chunk type
     */
    public int getRanking() {
    	return this.ranking;
    }   
    
    @Override
    public String toString() {return name;}
    
    /**
     * @param name  A String to test against the names of the chunks
     * @return  True if a match is found, otherwise false. 
     */
    public static boolean containsIgnoreCase(String name) 
    {
    	return stringMap.containsKey(name.toUpperCase());
    }
   
    public static ChunkType fromString(String name)
    {
        return stringMap.get(name.toUpperCase());
    }
    
    public static ChunkType fromInt(int value) {
       	ChunkType chunkType = intMap.get(value);
    	if (chunkType == null)
    	   return UNKNOWN;
    	return chunkType;
    }
    
    private static final Map<String, ChunkType> stringMap = new HashMap<String, ChunkType>();
    private static final Map<Integer, ChunkType> intMap = new HashMap<Integer, ChunkType>();
    
    static
    {
      for(ChunkType chunk : values()) {
          stringMap.put(chunk.toString().toUpperCase(), chunk);
          intMap.put(chunk.getValue(), chunk);
      }
    }   
    
    private final Attribute attribute;
    private final String name;
    private final int value;
    private final int ranking;
}
