/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.exif;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import cafe.image.tiff.FieldType;
import cafe.image.tiff.Tag;
import cafe.image.tiff.TiffTag;
import cafe.string.StringUtils;

/**
 * Defines GPS tags
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/10/2013
 */
public enum GPSTag implements Tag {
	// EXIF GPSSubIFD tags
	GPS_VERSION_ID("GPSVersionID", (short)0x0000),
	GPS_LATITUDE_REF("GPSLatitudeRef", (short)0x0001),
	GPS_LATITUDE("GPSLatitude", (short)0x0002) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 6)
				throw new IllegalArgumentException("Wrong number of GPSLatitute data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return df.format(1.0*intValues[0]/intValues[1]) + '\u00B0' + df.format(1.0*intValues[2]/intValues[3])
	        		+ "'" + df.format(1.0*intValues[4]/intValues[5]) + "\"";	
		}
	},
	GPS_LONGITUDE_REF("GPSLongitudeRef", (short)0x0003),
	GPS_LONGITUDE("GPSLongitude", (short)0x0004) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 6)
				throw new IllegalArgumentException("Wrong number of GPSLongitude data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return df.format(1.0*intValues[0]/intValues[1]) + '\u00B0' + df.format(1.0*intValues[2]/intValues[3])
	        		+ "'" + df.format(1.0*intValues[4]/intValues[5]) + "\"";	
		}
	},
	GPS_ALTITUDE_REF("GPSAltitudeRef", (short)0x0005),
	GPS_ALTITUDE("GPSAltitude", (short)0x0006) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of GPSAltitute data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return df.format(1.0*intValues[0]/intValues[1]) + "m";	
		}
	},
	GPS_TIME_STAMP("GPSTimeStamp", (short)0x0007) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 6)
				throw new IllegalArgumentException("Wrong number of GPSTimeStamp data number: " + intValues.length);
			//formatting numbers up to 2 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.##");
	        return df.format(1.0*intValues[0]/intValues[1]) + ":" + df.format(1.0*intValues[2]/intValues[3])
	        		+ ":" + df.format(1.0*intValues[4]/intValues[5]);	
		}
	},
	GPS_SATELLITES("GPSSatellites", (short)0x0008),
	GPS_STATUS("GPSStatus", (short)0x0009),
	GPS_MEASURE_MODE("GPSMeasureMode", (short)0x000a),	
	GPS_DOP("GPSDOP/ProcessingSoftware", (short)0x000b),
	GPS_SPEED_REF("GPSSpeedRef", (short)0x000c),
	GPSSpeed("GPSSpeed", (short)0x000d),
	GPS_TRACK_REF("GPSTrackRef", (short)0x000e),
	GPS_TRACK("GPSTrack", (short)0x000f),
	GPS_IMG_DIRECTION_REF("GPSImgDirectionRef", (short)0x0010),
	GPS_IMG_DIRECTION("GPSImgDirection", (short)0x0011) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of GPSImgDirection data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return df.format(1.0*intValues[0]/intValues[1]) + "°";	
		}
	},
	GPS_MAP_DATUM("GPSMapDatum", (short)0x0012),
	GPS_DEST_LATITUDE_REF("GPSDestLatitudeRef", (short)0x0013),
	GPS_DEST_LATITUDE("GPSDestLatitude", (short)0x0014) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 6)
				throw new IllegalArgumentException("Wrong number of GPSDestLatitute data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return df.format(1.0*intValues[0]/intValues[1]) + '\u00B0' + df.format(1.0*intValues[2]/intValues[3])
	        		+ "'" + df.format(1.0*intValues[4]/intValues[5]) + "\"";	
		}
	},
	GPS_DEST_LONGITUDE_REF("GPSDestLongitudeRef", (short)0x0015),
	GPS_DEST_LONGITUDE("GPSDestLongitude", (short)0x0016) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 6)
				throw new IllegalArgumentException("Wrong number of GPSDestLongitude data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return df.format(1.0*intValues[0]/intValues[1]) + '\u00B0' + df.format(1.0*intValues[2]/intValues[3])
	        		+ "'" + df.format(1.0*intValues[4]/intValues[5]) + "\"";	
		}
	},
	GPS_DEST_BEARING_REF("GPSDestBearingRef", (short)0x0017),
	GPS_DEST_BEARING("GPSDestBearing", (short)0x0018) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of GPSDestBearing data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return df.format(1.0*intValues[0]/intValues[1]) + "m";	
		}
	},
	GPS_DEST_DISTANCE_REF("GPSDestDistanceRef", (short)0x0019),
	GPS_DEST_DISTANCE("GPSDestDistance", (short)0x001a) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of GPSDestDistance data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.###");
	        return df.format(1.0*intValues[0]/intValues[1]) + "m";	
		}
	},
	GPS_PROCESSING_METHOD("GPSProcessingMethod", (short)0x001b),
	GPS_AREA_INFORMATION("GPSAreaInformation", (short)0x001c),
	GPS_DATE_STAMP("GPSDateStamp", (short)0x001d),
	GPS_DIFFERENTIAL("GPSDifferential", (short)0x001e),
	GPS_HPOSITIONING_ERROR("GPSHPositioningError", (short)0x001f),
	// unknown tag
	UNKNOWN("Unknown",  (short)0xffff); 
    // End of EXIF GPSSubIFD tags
	
	private GPSTag(String name, short value)
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
       	GPSTag tag = tagMap.get(value);
    	if (tag == null)
    	   return TiffTag.UNKNOWN;
   		return tag;
    }
    
    private static final Map<Short, GPSTag> tagMap = new HashMap<Short, GPSTag>();
       
    static
    {
      for(GPSTag tag : values()) {
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
	
	public FieldType getFieldType() {
		return FieldType.UNKNOWN;
	}
	
	private final String name;
	private final short value;
}
