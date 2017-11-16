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

package com.icafe4j.image.jpeg;

import java.util.HashMap;
import java.util.Map;

/**
 * Class represents JPEG marker.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/08/2013
 */
public enum Marker {

	    /**
	     * Define JPEG markers. 
	     * A marker is prefixed by a one byte segment identifier 0xff. 
	     * Most markers will have additional information following them. 
	     * When this is the case, the marker and its associated information
	     * is referred to as a "header." In a header the marker is immediately 
	     * followed by two bytes that indicate the length of the information, 
	     * in bytes, that the header contains. The two bytes that indicate the 
	     * length are always included in that count.
	     */
	    TEM ("Temporary private use by arithmetic encoders", (short)0xff01),
	    SOF0("Baseline DCT", (short)0xffc0),
	    SOF1("Extended sequential DCT, Huffman coding", (short)0xffc1),
	    SOF2("Progressive DCT, Huffman coding", (short)0xffc2),
	    SOF3("Lossless, Huffman coding", (short)0xffc3),
	    DHT("Define Huffman table", (short)0xffc4),
	    SOF5("Differential sequential DCT, Huffman coding", (short)0xffc5),
	    SOF6("Differential progressive DCT, Huffman coding", (short)0xffc6),
	    SOF7("Differential lossless, Huffman coding", (short)0xffc7),
	    JPG("Reserved", (short)0xffc8), 
	    SOF9("Sequential DCT, arithmetic coding", (short)0xffc9), 
	    SOF10("Progressive DCT, arithmetic coding", (short)0xffca), 
	    SOF11("Lossless, arithmetic coding", (short)0xffcb),
	    DAC("Define Arithmetic Table ", (short)0xffcc), 
	    SOF13("Differential sequential DCT, arithmetic coding", (short)0xffcd), 
	    SOF14("Differential progressive DCT, arithmetic coding", (short)0xffce), 
	    SOF15("Differential lossless, arithmetic coding", (short)0xffcf),
	    /**
	     * RSTn are used for resync, may be ignored, no length and other contents are
	     * associated with these markers. 
	     */
	    RST0("Restart 0", (short)0xffd0),  
	    RST1("Restart 1", (short)0xffd1),
	    RST2("Restart 2", (short)0xffd2),
	    RST3("Restart 3", (short)0xffd3),
	    RST4("Restart 4", (short)0xffd4),
	    RST5("Restart 5", (short)0xffd5),
	    RST6("Restart 6", (short)0xffd6),
	    RST7("Restart 7", (short)0xffd7),
	    //End of RSTn definitions
	    SOI("Start of image", (short)0xffd8),
	    EOI("End of image", (short)0xffd9),
	    SOS("Start of scan", (short)0xffda),
	    DQT("Define quantization table", (short)0xffdb),
	    DNL("Define number of lines", (short)0xffdc),
	    DRI("Define restart interval", (short)0xffdd),
	    DHP("Define hierarchical progression", (short)0xffde),
	    EXP("Expand reference components", (short)0xffdf),
	    APP0("JFIF/JFXX/CIFF/AVI1", (short)0xffe0),
	    APP1("EXIF/XMP/ExtendedXMP", (short)0xffe1),
	    APP2("FPXR/ICC profile/MPF/PreviewImage", (short)0xffe2), 
	    APP3("Meta/Stim", (short)0xffe3), 
	    APP4("Scalado", (short)0xffe4), 
	    APP5("RMETA", (short)0xffe5), 
	    APP6("EPPIM/NITF", (short)0xffe6), 
	    APP7("", (short)0xffe7), 
	    APP8("SPIFF", (short)0xffe8), 
	    APP9("", (short)0xffe9), 
	    APP10("Comment", (short)0xffea), 
	    APP11("", (short)0xffeb), 
	    APP12("Photoshop Ducky/PictureInfo", (short)0xffec), 
	    APP13("Photoshop IRB/Adobe_CM", (short)0xffed),
	    APP14("Adobe DCT encoding information", (short)0xffee), 
	    APP15("GraphicConverter", (short)0xffef), 
	    JPG0("Reserved", (short)0xfff0),
	    // A lot more here ...
	    JPG13("Reserved", (short)0xfffd),
	    COM("Comment", (short)0xfffe),
	    // End of JPEG marker definitions
	    // Special case of arbitrary padding 0xff after segment identifier.
	    PADDING("Padding", (short)0xffff),
	    // Special case of unknown segment identifier.
	    UNKNOWN("Unknown", (short)0x0000);
	 	    
	    private Marker(String description, short value) {
			this.description = description;
			this.value = value;
	    }
		
	    public enum Attribute {
			STAND_ALONE,
			MARKER_SEGMENT;		
	    }
	    
	    public String getDescription() {
		   return description;
	    }
	   
	    public short getValue() {
		   return value;
	    }
	    
	    public static Marker fromShort(short value) {
	       	Marker marker = markerMap.get(value);
	    	if (marker == null)
	    	   return UNKNOWN;
	      	return marker;
	    }
	   
	    @Override public String toString() {
		   return name() + ": " + description;
	    }
	   
	    private static final Map<Short, Marker> markerMap = new HashMap<Short, Marker>();
	    
	    static
	    {
	      for(Marker marker : values()) {
	          markerMap.put(marker.getValue(), marker);
	      }
	    }	    
   	  
	    private final String description;
	    private final short value;
}
