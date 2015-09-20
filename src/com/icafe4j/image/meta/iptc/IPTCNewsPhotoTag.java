/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
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
	 RECORD_VERSION(0, "NewsPhotoVersion"),
	 PICTURE_NUMBER(10, "PictureNumber"),
	 IMAGE_WIDTH(20, "ImageWidth"),
	 IMAGE_HEIGHT(30, "ImageHeight"),
	 PIXEL_WIDTH(40, "PixelWidth"),
	 PIXEL_HEIGHT(50, "PixelHeight"),
	 SUPPLEMENTAL_TYPE(55, "SupplementalType"),
	 COLOR_REPRESENTATION(60, "ColorRepresentation"),
	 INTERCHANGE_COLOR_SPACE(64, "InterchangeColorSpace"),
	 COLOR_SEQUENCE(65, "ColorSequence"),
	 ICC_PROFILE(66, "ICC_Profile"),
	 COLOR_CALIBRATION_MATRIX(70, "ColorCalibrationMatrix"),
	 LOOKUP_TABLE(80, "LookupTable"),
	 NUM_INDEX_ENTRIES(84, "NumIndexEntries"),
	 COLOR_PALETTE(85, "ColorPalette"),
	 BITS_PER_SAMPLE(86, "BitsPerSample"),
	 SAMPLE_STRUCTURE(90, "SampleStructure"),
	 SCANNING_DIRECTION(100, "ScanningDirection"),
	 IMAGE_ROTATION(102, "ImageRotation"),
	 DATA_COMPRESSION_METHOD(110, "DataCompressionMethod"),
	 QUANTIZATION_METHOD(120, "QuantizationMethod"),
	 END_POINTS(125, "EndPoints"),
	 EXCURSION_TOLERANCE(130, "ExcursionTolerance"),
	 BITS_PER_COMPONENT(135, "BitsPerComponent"),
	 MAXIMUM_DENSITY_RANGE(140, "MaximumDensityRange"),
	 GAMMA_COMPENSATED_VALUE(145, "GammaCompensatedValue"),
	 	 
	 UNKNOWN(999, "Unknown");
	 
	 private IPTCNewsPhotoTag(int tag, String name) {
		 this.tag = tag;
		 this.name = name;
	 }
	 
	 @Override
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
