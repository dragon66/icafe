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
 * Defines DataSet tags for IPTC NewsPhoto Record - Record number 3.
 * * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 07/02/2013
 */
public enum IPTCNewsPhotoTag implements IPTCTag {
	 RECORD_VERSION(0, "News Photo Version"),
	 PICTURE_NUMBER(10, "Picture Number"),
	 IMAGE_WIDTH(20, "Image Width"),
	 IMAGE_HEIGHT(30, "Image Height"),
	 PIXEL_WIDTH(40, "Pixel Width"),
	 PIXEL_HEIGHT(50, "Pixel Height"),
	 SUPPLEMENTAL_TYPE(55, "Supplemental Type"),
	 COLOR_REPRESENTATION(60, "Color Representation"),
	 INTERCHANGE_COLOR_SPACE(64, "Interchange Color Space"),
	 COLOR_SEQUENCE(65, "Color Sequence"),
	 ICC_PROFILE(66, "ICC_Profile"),
	 COLOR_CALIBRATION_MATRIX(70, "Color Calibration Matrix"),
	 LOOKUP_TABLE(80, "Lookup Table"),
	 NUM_INDEX_ENTRIES(84, "Num Index Entries"),
	 COLOR_PALETTE(85, "Color Palette"),
	 BITS_PER_SAMPLE(86, "Bits Per Sample"),
	 SAMPLE_STRUCTURE(90, "Sample Structure"),
	 SCANNING_DIRECTION(100, "Scanning Direction"),
	 IMAGE_ROTATION(102, "Image Rotation"),
	 DATA_COMPRESSION_METHOD(110, "Data Compression Method"),
	 QUANTIZATION_METHOD(120, "Quantization Method"),
	 END_POINTS(125, "End Points"),
	 EXCURSION_TOLERANCE(130, "Excursion Tolerance"),
	 BITS_PER_COMPONENT(135, "Bits Per Component"),
	 MAXIMUM_DENSITY_RANGE(140, "Maximum Density Range"),
	 GAMMA_COMPENSATED_VALUE(145, "Gamma Compensated Value"),
	 	 
	 UNKNOWN(999, "Unknown");
	 
	 private IPTCNewsPhotoTag(int tag, String name) {
		 this.tag = tag;
		 this.name = name;
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
	 
	 public static IPTCNewsPhotoTag fromTag(int value) {
      	IPTCNewsPhotoTag record = recordMap.get(value);
	   	if (record == null)
	   		return UNKNOWN;
	 	return record;
	 }
  
	 @Override public String toString() {
		   return name;
	 }
  
	 private static final Map<Integer, IPTCNewsPhotoTag> recordMap = new HashMap<Integer, IPTCNewsPhotoTag>();
   
	 static
	 {
		 for(IPTCNewsPhotoTag record : values()) {
			 recordMap.put(record.getTag(), record);
		 }
	 }	    
   
	 private final int tag;
	 private final String name;
}
