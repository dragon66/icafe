/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.meta.exif;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import com.icafe4j.image.tiff.FieldType;
import com.icafe4j.image.tiff.Tag;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.string.StringUtils;

/**
 * Defines EXIF tags
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/10/2013
 */
public enum ExifTag implements Tag {
	EXPOSURE_TIME("ExposureTime", (short)0x829a),	
	FNUMBER("FNumber", (short)0x829d) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of EXIF FNumber data number: " + intValues.length);
			//formatting numbers up to 2 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.#");
	        return "F" + StringUtils.rationalToString(df, true, intValues);	
		}
	},
  	//EXIF_SUB_IFD("ExifSubIFD", (short)0x8769),	
	EXPOSURE_PROGRAM("ExposureProgram", (short)0x8822),
	SPECTRAL_SENSITIVITY("SpectralSensitivity", (short)0x8824),
	//GPS_SUB_IFD("GPSSubIFD", (short)0x8825),
	ISO_SPEED_RATINGS("ISOSpeedRatings", (short)0x8827),
	OECF("OECF", (short)0x8828),
	
	EXIF_VERSION("ExifVersion", (short)0x9000) {
		public String getFieldAsString(Object value) {
			return new String((byte[])value).trim();
		}
	},
	DATE_TIME_ORIGINAL("DateTimeOriginal", (short)0x9003),
	DATE_TIME_DIGITIZED("DateTimeDigitized", (short)0x9004),
	
	COMPONENT_CONFIGURATION("ComponentConfiguration", (short)0x9101),
	COMPRESSED_BITS_PER_PIXEL("CompressedBitsPerPixel", (short)0x9102),
	
	SHUTTER_SPEED_VALUE("ShutterSpeedValue", (short)0x9201) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of EXIF ShutterSpeedValue data number: " + intValues.length);
			//formatting numbers up to 2 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.##");
	        return StringUtils.rationalToString(df, false, intValues);	
		}
	},
	APERTURE_VALUE("ApertureValue", (short)0x9202),
	BRIGHTNESS_VALUE("BrightValue", (short)0x9203),
	EXPOSURE_BIAS_VALUE("ExposureBiasValue", (short)0x9204),
	MAX_APERTURE_VALUE("MaxApertureValue", (short)0x9205),
	SUBJECT_DISTANCE("SubjectDistance", (short)0x9206),
	METERING_MODE("MeteringMode", (short)0x9207),
	LIGHT_SOURCE("LightSource", (short)0x9208),
	FLASH("Flash", (short)0x9209),	
	FOCAL_LENGTH("FocalLength", (short)0x920a) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of EXIF FocalLength data number: " + intValues.length);
			//formatting numbers up to 2 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.##");
	        return StringUtils.rationalToString(df, true, intValues) + "mm";	
		}
	},	
	SUBJECT_AREA("SubjectArea", (short)0x9214),	
	MAKER_NODE("MakerNote", (short)0x927c),
	USER_COMMENT("UserComment", (short)0x9286),
	
	SUB_SEC_TIME("SubSecTime", (short)0x9290),
	SUB_SEC_TIME_ORIGINAL("SubSecTimeOriginal", (short)0x9291),
	SUB_SEC_TIME_DIGITIZED("SubSecTimeDigitized", (short)0x9292),
	
	FLASH_PIX_VERSION("FlashPixVersion", (short)0xa000) {
		public String getFieldAsString(Object value) {
			return new String((byte[])value).trim();
		}
	},
	COLOR_SPACE("ColorSpace", (short)0xa001) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Warning: unknown color space value: " + intValue;
			
			switch(intValue) {
				case 1:	description = "sRGB"; break;
				case 65535: description = "Uncalibrated";	break;
			}
			
			return description;
		}
	},
	EXIF_IMAGE_WIDTH("ExifImageWidth", (short)0xa002),
	EXIF_IMAGE_HEIGHT("ExifImageHeight", (short)0xa003),
	RELATED_SOUND_FILE("RelatedSoundFile", (short)0xa004),
	
	EXIF_INTEROPERABILITY_OFFSET("ExifInteroperabilityOffset", (short)0xa005) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	FLASH_ENERGY("FlashEnergy", (short)0xa20b),
	SPATIAL_FREQUENCY_RESPONSE("SpatialFrequencyResponse", (short)0xa20c),
	FOCAL_PLANE_X_RESOLUTION("FocalPlaneXResolution", (short)0xa20e),
	FOCAL_PLANE_Y_RESOLUTION("FocalPlaneYResolution", (short)0xa20f),
	FOCAL_PLANE_RESOLUTION_UNIT("FocalPlaneResolutionUnit", (short)0xa210),
	
	SUBJECT_LOCATION("SubjectLocation", (short)0xa214),
	EXPOSURE_INDEX("ExposureIndex", (short)0xa215),
	SENSING_METHOD("SensingMethod", (short)0xa217),
	
	FILE_SOURCE("FileSource", (short)0xa300),
	SCENE_TYPE("SceneType", (short)0xa301),
	CFA_PATTERN("CFAPattern", (short)0xa302),
	
	CUSTOM_RENDERED("CustomRendered", (short)0xa401),
	EXPOSURE_MODE("ExposureMode", (short)0xa402),
	WHITE_BALENCE("WhileBalence", (short)0xa403),
	DIGITAL_ZOOM_RATIO("DigitalZoomRatio", (short)0xa404),
	FOCAL_LENGTH_IN_35MM_FORMAT("FocalLengthIn35mmFormat", (short)0xa405),
	SCENE_CAPTURE_TYPE("SceneCaptureType", (short)0xa406),
	GAIN_CONTROL("GainControl", (short)0xa407),
	CONTRAST("Contrast", (short)0xa408),
	SATURATION("Saturation", (short)0xa409),
	SHARPNESS("Sharpness", (short)0xa40a),
	DEVICE_SETTING_DESCRIPTION("DeviceSettingDescription", (short)0xa40b),
	SUBJECT_DISTANCE_RANGE("SubjectDistanceRange", (short)0xa40c),
	
	IMAGE_UNIQUE_ID("ImageUniqueID", (short)0xa420),
	
	OWNER_NAME("OwnerName", (short)0xa430),
	BODY_SERIAL_NUMBER("BodySerialNumber", (short)0xa431),
	LENS_SPECIFICATION("LensSpecification", (short)0xa432),
	LENS_Make("LensMake", (short)0xa433),
	LENS_MODEL("LensModel", (short)0xa434),
	LENS_SERIAL_NUMBER("LensSerialNumber", (short)0xa435),
	
	EXPAND_SOFTWARE("ExpandSoftware", (short)0xafc0),
	EXPAND_LENS("ExpandLens", (short)0xafc1),
	EXPAND_FILM("ExpandFilm", (short)0xafc2),
	EXPAND_FILTER_LENS("ExpandFilterLens", (short)0xafc3),
	EXPAND_SCANNER("ExpandScanner", (short)0xafc4),
	EXPAND_FLASH_LAMP("ExpandFlashLamp", (short)0xafc5),
		
	PADDING("Padding", (short)0xea1c),
	
	UNKNOWN("Unknown",  (short)0xffff); 
	
	private ExifTag(String name, short value)
	{
		this.name = name;
		this.value = value;
	} 
	
	public String getName()
	{
		return this.name;
	}	
	
	public short getValue()
	{
		return this.value;
	}
	
	@Override
    public String toString() {
		if (this == UNKNOWN)
			return name;
		return name + " [Value: " + StringUtils.shortToHexStringMM(value) +"]";
	}
	
    public static Tag fromShort(short value) {
       	ExifTag exifTag = tagMap.get(value);
    	if (exifTag == null)
    	   return TiffTag.UNKNOWN;
   		return exifTag;
    }
    
    private static final Map<Short, ExifTag> tagMap = new HashMap<Short, ExifTag>();
       
    static
    {
      for(ExifTag exifTag : values()) {
           tagMap.put(exifTag.getValue(), exifTag);
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