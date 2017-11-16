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

package com.icafe4j.image.tiff;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * TiffFieldEnum.java
 * <p>
 * This class provides a central place for all the TIFF fields related enumerations
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/26/2014
 */
public class TiffFieldEnum {
	
	public enum PhotoMetric {
		// Baseline
		WHITE_IS_ZERO("WhiteIsZero (for bilevel and grayscale images)", 0),
		BLACK_IS_ZERO("BlackIsZero (for bilevel and grayscale images)", 1),
		RGB("RGB, value of (0,0,0) represents black, and (255,255,255) represents white, assuming 8-bit components", 2),
		PALETTE_COLOR("Palette color, a color is described with a single component", 3),
		TRANSPARENCY_MASK("Transparency mask, the image is used to define an irregularly shaped region of another image in the same TIFF file. SamplesPerPixel and BitsPerSample must be 1. PackBits compression is recommended", 4),
		// Extension
		SEPARATED("Separated, usually CMYK", 5),
		YCbCr("YCbCr", 6),
		CIE_LAB("CIE L*a*b*", 8),
		ICC_LAB("ICC L*a*b*", 9),
		ITU_LAB("ITU L*a*b*", 10),
		CFA("CFA (Color Filter Array)", 32803),
		LINEAR__RAW("LinearRaw", 34892),
		
		UNKNOWN("Unknown", 9999);
		 
		private PhotoMetric(String description, int value) {
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
		
		public static PhotoMetric fromValue(int value) {
	       	PhotoMetric photoMetric = typeMap.get(value);
	    	if (photoMetric == null)
	    	   return UNKNOWN;
	      	return photoMetric;
	    }
	    
	    private static final Map<Integer, PhotoMetric> typeMap = new HashMap<Integer, PhotoMetric>();
	       
	    static
	    {
	      for(PhotoMetric photoMetric : values())
	    	  typeMap.put(photoMetric.getValue(), photoMetric);
	    } 

		private String description;
		private int value;
	}
	
	public enum Compression {
		//
		NONE("No Compression", 1),
		CCITTRLE("CCITT modified Huffman RLE", 2),
		CCITTFAX3("CCITT Group 3 fax encoding", 3),
		CCITTFAX4("CCITT Group 4 fax encoding", 4),
		LZW("LZW", 5),
		OLD_JPG("JPEG ('old-style' JPEG)", 6),
	    JPG("JPEG ('new-style' JPEG technote #2)", 7),
	    DEFLATE_ADOBE("Deflate ('Adobe-style')", 8),
	    JBIG_ON_BW("JBIG on black and white", 9),
		JBIG_ON_COLOR("JBIG on color", 10),
		PACKBITS("PackBits compression, aka Macintosh RLE", 32773),
		DEFLATE("Deflate", 32946),	
		
		UNKNOWN("Unknown", 9999);
		
		private Compression(String description, int value) {
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
		
		public static Compression fromValue(int value) {
	       	Compression compression = typeMap.get(value);
	    	if (compression == null)
	    	   return UNKNOWN;
	      	return compression;
	    }
	    
	    private static final Map<Integer, Compression> typeMap = new HashMap<Integer, Compression>();
	       
	    static {
	      for(Compression compression : values())
	    	  typeMap.put(compression.getValue(), compression);
	    } 
	    
	    public static EnumSet<Compression> forBilevel() {
	    	return EnumSet.of(CCITTRLE, CCITTFAX3, CCITTFAX4, LZW, DEFLATE, DEFLATE_ADOBE, PACKBITS);
	    }
	    
	    public static EnumSet<Compression> forIndexed() {
	    	return EnumSet.of(LZW, DEFLATE, DEFLATE_ADOBE, PACKBITS);
	    }
	    
	    public static EnumSet<Compression> forGrayScale() {
	    	return EnumSet.of(LZW, DEFLATE, DEFLATE_ADOBE, PACKBITS, JPG);
	    }

	    public static EnumSet<Compression> forTrueColor() {
	    	return EnumSet.of(LZW, DEFLATE, DEFLATE_ADOBE, PACKBITS, JPG);
	    }
	    
		private String description;
		private int value;
	}
	
	public enum PlanarConfiguration {
		CONTIGUOUS("Chunky format (The component values for each pixel are stored contiguously)", 1),
		SEPARATE("Planar format (The components are stored in separate component planes)", 2),
		UNKNOWN("Unknown", 9999);
		
		private PlanarConfiguration(String description, int value) {
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
		
		public static PlanarConfiguration fromValue(int value) {
	       	PlanarConfiguration planarConfiguration = typeMap.get(value);
	    	if (planarConfiguration == null)
	    	   return UNKNOWN;
	      	return planarConfiguration;
	    }
	    
	    private static final Map<Integer, PlanarConfiguration> typeMap = new HashMap<Integer, PlanarConfiguration>();
	       
	    static
	    {
	      for(PlanarConfiguration planarConfiguration : values())
	    	  typeMap.put(planarConfiguration.getValue(), planarConfiguration);
	    } 
		
		private final String description;
		private final int value;
	}
	
	public enum ResolutionUnit {
		RESUNIT_NONE("No absolute unit of measurement. Used for images that may have a non-square aspect ratio, but no meaningful absolute dimensions.", 1),
		RESUNIT_INCH("Inch.", 2),
		RESUNIT_CENTIMETER("Centimeter.", 3),
		UNKNOWN("Unknown", 9999);
		
		private ResolutionUnit(String description, int value) {
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
		
		public static ResolutionUnit fromValue(int value) {
	       	ResolutionUnit resolutionUnit = typeMap.get(value);
	    	if (resolutionUnit == null)
	    	   return UNKNOWN;
	      	return resolutionUnit;
	    }
	    
	    private static final Map<Integer, ResolutionUnit> typeMap = new HashMap<Integer, ResolutionUnit>();
	       
	    static
	    {
	      for(ResolutionUnit resolutionUnit : values())
	    	  typeMap.put(resolutionUnit.getValue(), resolutionUnit);
	    } 
		
		private final String description;
		private final int value;
	}
	
	private TiffFieldEnum() {}	
}
