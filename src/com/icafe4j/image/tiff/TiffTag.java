/**
 * COPYRIGHT (C) 2014-2019 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.tiff;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import com.icafe4j.string.StringUtils;

/**
 * TiffField tag enumeration.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/04/2013
 */
public enum TiffTag implements Tag {	
	// Definition includes all baseline and extended tags.	
	NEW_SUBFILE_TYPE("New Subfile Type", (short)0x00FE, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Warning: unknown new subfile type value: " + value;
			
			switch(intValue) {
				case 0: description = "Default value 0"; break; 
				case 1:	description = "Reduced-resolution image data"; break;
				case 2: description = "A single page of a multi-page image";	break;
				case 4: description = "A transparency mask for another image";	break;
			}
			
			return description;
		}
		
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	SUBFILE_TYPE("Subfile Type", (short)0x00FF, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Unknown subfile type value: " + value;
			
			switch(intValue) {
				case 0: description = "Default value 0"; break;
				case 1:	description = "Full-resolution image data"; break;
				case 2: description = "Reduced-resolution image data"; break;
				case 3: description = "A single page of a multi-page image";	break;
			}
			
			return description;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	IMAGE_WIDTH("Image Width", (short)0x0100, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	IMAGE_LENGTH("Image Length", (short)0x0101, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	BITS_PER_SAMPLE("Bits Per Sample", (short)0x0102, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	COMPRESSION("Compression", (short)0x0103, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			return TiffFieldEnum.Compression.fromValue(((int[])value)[0]).getDescription();
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	PHOTOMETRIC_INTERPRETATION("Photometric Interpretation", (short)0x0106, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			return TiffFieldEnum.PhotoMetric.fromValue(((int[])value)[0]).getDescription();
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	THRESHOLDING("Thresholding", (short)0x0107, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Unknown thresholding value: " + value;
			
			switch(intValue) {
				case 1:	description = "No dithering or halftoning has been applied to the image data"; break;
				case 2: description = "An ordered dither or halftone technique has been applied to the image data";	break;
				case 3: description = "A randomized process such as error diffusion has been applied to the image data";	break;
			}
			
			return description;
		}		
	},
	
	CELL_WIDTH("Cell Width", (short)0x0108, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	CELL_LENGTH("Cell Length", (short)0x0109, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	FILL_ORDER("Fill Order", (short)0x010A, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			// We only has two values for this tag, so we use an array instead of a map
			String[] values = {"Msb2Lsb", "Lsb2Msb"};
			int intValue = ((int[])value)[0];
			if(intValue != 1 && intValue != 2) 
				return "Warning: unknown fill order value: " + intValue;
			
			return values[intValue-1]; 
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	DOCUMENT_NAME("Document Name", (short)0x010D, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	IMAGE_DESCRIPTION("Image Description", (short)0x010E, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
		public boolean isCritical() {
			return false;
		}
	},
	
	MAKE("Make", (short)0x010F, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
		public boolean isCritical() {
			return false;
		}
	},
	
	MODEL("Model", (short)0x0110, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	STRIP_OFFSETS("Strip Offsets", (short)0x0111, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	ORIENTATION("Orientation", (short)0x0112, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			String[] values = {"TopLeft", "TopRight", "BottomRight", "BottomLeft", "LeftTop",
								"RightTop",	"RightBottom", "LeftBottom"};
			int intValue = ((int[])value)[0];
			if(intValue >= 1 && intValue <= 8)
				return values[intValue - 1];
			
			return "Warning: orientation value: " + intValue;
		}
	},
	
	SAMPLES_PER_PIXEL("Samples Per Pixel", (short)0x0115, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	ROWS_PER_STRIP("Rows Per Strip", (short)0x0116, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	STRIP_BYTE_COUNTS("Strip Byte Counts", (short)0x0117, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	MIN_SAMPLE_VALUE("Min Sample Value", (short)0x0118, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	MAX_SAMPLE_VALUE("Max Sample Value", (short)0x0119, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	X_RESOLUTION("XResolution", (short)0x011A, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of XResolution data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.##");
	        return StringUtils.rationalToString(df, true, intValues);	
		}
		
		public FieldType getFieldType() {
			return FieldType.RATIONAL;
		}
	},
	
	Y_RESOLUTION("YResolution", (short)0x011B, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			int[] intValues = (int[])value;
			if(intValues.length != 2)
				throw new IllegalArgumentException("Wrong number of YResolution data number: " + intValues.length);
			//formatting numbers up to 3 decimal places in Java
	        DecimalFormat df = new DecimalFormat("#,###,###.##");
	        return StringUtils.rationalToString(df, true, intValues);	
		}
		public FieldType getFieldType() {
			return FieldType.RATIONAL;
		}
	},
	
	PLANAR_CONFIGURATTION("Planar Configuration", (short)0x011C, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			return TiffFieldEnum.PlanarConfiguration.fromValue(((int[])value)[0]).getDescription();
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	PAGE_NAME("Page Name", (short)0x011D, Attribute.EXTENDED),
	X_POSITION("XPosition", (short)0x011E, Attribute.EXTENDED),
	Y_POSITION("YPosition", (short)0x011F, Attribute.EXTENDED),
	
	FREE_OFFSETS("Free Offsets", (short)0x0120, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	FREE_BYTE_COUNTS("Free Byte Counts", (short)0x0121, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	/**
	 *  The precision of the information contained in the GrayResponseCurve.
	 *  Because optical density is specified in terms of fractional numbers,
	 *  this field is necessary to interpret the stored integer information.
	 *  For example, if GrayScaleResponseUnits is set to 4 (ten-thousandths 
	 *  of a unit), and a GrayScaleResponseCurve number for gray level 4 is
	 *  3455, then the resulting actual value is 0.3455. 
	 */
	GRAY_RESPONSE_UNIT("Gray Response Unit", (short)0x0122, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			//
			String[] values = {"Number represents tenths of a unit", "Number represents hundredths of a unit",
					"Number represents thousandths of a unit", "Number represents ten-thousandths of a unit",
					"Number represents hundred-thousandths of a unit"};
			// Valid values are from 1 to 5
			int intValue = ((int[])value)[0];
			if(intValue >= 1 && intValue <= 5)
				return values[intValue - 1];
			
			return "Warning: unknown resolution unit value: " + intValue;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	GRAY_RESPONSE_CURVE("Gray Response Curve", (short)0x0123, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	T4_OPTIONS("T4 Options", (short)0x0124, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Warning: unknown T4 options value: " + intValue;
			
			switch(intValue) {
				case 0: description = "Basic 1-dimensional coding"; break; 
				case 1:	description = "2-dimensional coding"; break;
				case 2: description = "Uncompressed mode";	break;
				case 4: description = "Fill bits have been added as necessary before EOL codes such that EOL always ends on a byte boundary";	break;
			}
			
			return description;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	}, 
	
	T6_OPTIONS("T6 Options", (short)0x0125, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Warning: unknown T6 options value: " + intValue;
			
			switch(intValue) {
				case 2: description = "Uncompressed mode"; break;
				case 0: description = "Default value 0";
			}			
			
			return description;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	RESOLUTION_UNIT("Resolution Unit", (short)0x0128, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			// We only has three values for this tag, so we use an array instead of a map
			String[] values = {"No absolute unit of measurement (Used for images that may have a non-square aspect ratio, but no meaningful absolute dimensions)", 
					"Inch", "Centimeter"};
			int intValue = ((int[])value)[0];
			if(intValue != 1 && intValue != 2 && intValue != 3) 
				return "Warning: unknown resolution unit value: " + intValue;
			
			return values[intValue-1]; 
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	PAGE_NUMBER("Page Number", (short)0x0129, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	TRANSFER_FUNCTION("Transfer Function", (short)0x012D, Attribute.EXTENDED),
	
	SOFTWARE("Software", (short)0x0131, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
		public boolean isCritical() {
			return false;
		}
	},
	
	DATETIME("DateTime", (short)0x0132, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	ARTIST("Artist", (short)0x013B, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	HOST_COMPUTER("Host Computer", (short)0x013C, Attribute.BASELINE),
	
	PREDICTOR("Predictor", (short)0x013D, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Unknown predictor value: " + intValue;
			
			switch(intValue) {
				case 1:	description = "No prediction scheme used before coding"; break;
				case 2: description = "Horizontal differencing"; break;
				case 3: description = "Floating point horizontal differencing";	break;
			}
			
			return description;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	WHITE_POINT("White Point", (short)0x013E, Attribute.EXTENDED),
	PRIMARY_CHROMATICITIES("PrimaryChromaticities", (short)0x013F, Attribute.EXTENDED),
	
	COLORMAP("ColorMap", (short)0x0140, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	HALTONE_HINTS("Halftone Hints", (short)0x0141, Attribute.EXTENDED),
	
	TILE_WIDTH("Tile Width", (short)0x0142, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	TILE_LENGTH("Tile Length", (short)0x0143, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	TILE_OFFSETS("Tile Offsets", (short)0x0144, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	TILE_BYTE_COUNTS("Tile Byte Counts", (short)0x0145, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	BAD_FAX_LINES("Bad Fax Lines", (short)0x0146, Attribute.EXTENDED),
	CLEAN_FAX_DATA("Clean Fax Data", (short)0x0147, Attribute.EXTENDED),
	CONSECUTIVE_BAD_FAX_LINES("ConsecutiveBadFaxLines", (short)0x0148, Attribute.EXTENDED),
	
	SUB_IFDS("Sub IFDs", (short)0x014A, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or IFD
		}
	},
	
	INK_SET("Ink Set", (short)0x014C, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Warning: unknown InkSet value: " + intValue;
			
			switch(intValue) {
				case 1:	description = "CMYK"; break;
				case 2: description = "Not CMYK"; break;
			}
			
			return description;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	INK_NAMES("Ink Names", (short)0x014D, Attribute.EXTENDED),
	NUMBER_OF_INKS("Number Of Inks", (short)0x014E, Attribute.EXTENDED),
	DOT_RANGE("Dot Range", (short)0x0150, Attribute.EXTENDED),
	TARGET_PRINTER("Target Printer", (short)0x0151, Attribute.EXTENDED),
	
	EXTRA_SAMPLES("Extra Samples", (short)0x0152, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			// We only has three values for this tag, so we use an array instead of a map
			String[] values = {"Unspecified data", "Associated alpha data (with pre-multiplied color)",
					"Unassociated alpha data"};
			int intValue = ((int[])value)[0];
			if(intValue >= 0 && intValue <= 2)
				return values[intValue]; 
			
			return "Warning: unknown extra samples value: " + intValue;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	SAMPLE_FORMAT("Sample Format", (short)0x0153, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			String[] values = {"Unsigned integer data", "Two's complement signed integer data",
					"IEEE floating point data",	"Undefined data format", "Complex integer data",
					"Complex IEED floating point data"};
			int intValue = ((int[])value)[0];
			if(intValue >= 1 && intValue <= 6)
				return values[intValue - 1]; 
			
			return "Warning: unknown sample format value: " + intValue;
		}
	},
	
	S_MIN_SAMPLE_VALUE("S Min Sample Value", (short)0x0154, Attribute.EXTENDED),
	S_MAX_SAMPLE_VALUE("S Max Sample Value", (short)0x0155, Attribute.EXTENDED),
	TRANSFER_RANGE("Transfer Range", (short)0x0156, Attribute.EXTENDED),
	CLIP_PATH("Clip Path", (short)0x0157, Attribute.EXTENDED),
	X_CLIP_PATH_UNITS("X Clip Path Units", (short)0x0158, Attribute.EXTENDED),
	Y_CLIP_PATH_UNITS("Y Clip Path Units", (short)0x0159, Attribute.EXTENDED),
	
	INDEXED("Indexed", (short)0x015A, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Warning: unknown Indexed value: " + intValue;
			
			switch(intValue) {
				case 0:	description = "Not indexde"; break;
				case 1: description = "Indexed"; break;
			}
			
			return description;
		} 
	},
	
	JPEG_TABLES("JPEGTables - optional, for new-style JPEG compression", (short)0x015B, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.UNDEFINED;
		}
	},
	
	OPI_PROXY("OPI Proxy", (short)0x015F, Attribute.EXTENDED),
	
	GLOBAL_PARAMETERS_IFD("Global Parameters IFD", (short)0x0190, Attribute.EXTENDED),
	
	PROFILE_TYPE("Profile Type", (short)0x0191, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Warning: unknown profile type value: " + intValue;
			
			switch(intValue) {
				case 0:	description = "Unspecified"; break;
				case 1: description = "Group 3 fax"; break;
			}
			
			return description;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	FAX_PROFILE("Fax Profile", (short)0x0192, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			String[] values = {"Does not conform to a profile defined for TIFF for facsimile", "Minimal black & white lossless, Profile S",
					"Extended black & white lossless, Profile F",	"Lossless JBIG black & white, Profile J", "Lossy color and grayscale, Profile C",
					"Lossless color and grayscale, Profile L", "Mixed Raster Content, Profile M"};
			int intValue = ((int[])value)[0];
			if(intValue >= 0 && intValue <= 6)
				return values[intValue]; 
			
			return "Warning: unknown fax profile value: " + intValue;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	CODING_METHODS("Coding Methods", (short)0x0193, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Unknown coding method value: " + intValue;
			
			switch(intValue) {
				case 1:	description  = "Unspecified compression"; break;
				case 2: description  = "1-dimensional coding, ITU-T Rec. T.4 (MH - Modified Huffman)"; break;
				case 4: description  = "2-dimensional coding, ITU-T Rec. T.4 (MR - Modified Read)"; break;
				case 8: description  = "2-dimensional coding, ITU-T Rec. T.6 (MMR - Modified MR)"; break;
				case 16: description = "ITU-T Rec. T.82 coding, using ITU-T Rec. T.85 (JBIG)"; break;
				case 32: description = "ITU-T Rec. T.81 (Baseline JPEG)"; break;
				case 64: description = "ITU-T Rec. T.82 coding, using ITU-T Rec. T.43 (JBIG color)"; break;
			}
			
			return description;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},				
	
	VERSION_YEAR("Version Year", (short)0x0194, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.BYTE;
		}
	},
	
	MODE_NUMBER("Mode Number", (short)0x0195, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.BYTE;
		}
	},
	
	DECODE("Decode", (short)0x01B1, Attribute.EXTENDED),
	DEFAULT_IMAGE_COLOR("Default Image Color", (short)0x01B2, Attribute.EXTENDED),
	
	JPEG_PROC("JPEG Proc", (short)0x0200, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	JPEG_INTERCHANGE_FORMAT("JPEG Interchange Format/Jpeg IF Offset", (short)0x0201, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	JPEG_INTERCHANGE_FORMAT_LENGTH("JPEG Interchange Format Length/Jpeg IF Byte Count", (short)0x0202, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	JPEG_RESTART_INTERVAL("JPEG Restart Interval", (short)0x0203, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	JPEG_LOSSLESS_PREDICTORS("JPEG Lossless Predictors", (short)0x0205, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	JPEG_POINT_TRANSFORMS("JPEG Point Transforms", (short)0x0206, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	JPEG_Q_TABLES("JPEG Q Tables", (short)0x0207, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	JPEG_DC_TABLES("JPEG DC Tables", (short)0x0208, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	JPEG_AC_TABLES("JPEG AC Tables", (short)0x0209, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	YCbCr_COEFFICIENTS("YCbCr Coefficients", (short)0x0211, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.RATIONAL;
		}
	},
	
	YCbCr_SUB_SAMPLING("YCbCr SubSampling", (short)0x0212, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	YCbCr_POSITIONING("YCbCr Positioning", (short)0x0213, Attribute.EXTENDED) {
		public String getFieldAsString(Object value) {
			//
			int intValue = ((int[])value)[0];
			String description = "Warning: unknown YCbCr positioning value: " + intValue;
			
			switch(intValue) {
				case 1:	description = "Centered"; break;
				case 2: description = "Cosited"; break;
			}
			
			return description;
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	REFERENCE_BLACK_WHITE("Reference Black White", (short)0x0214, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.RATIONAL;
		}
	},
	
	STRIP_ROW_COUNTS("Strip Row Counts", (short)0x022F, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	XMP("XMP", (short)0x02BC, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.BYTE;
		}
	},
	
	RATING("Rating", (short)0x4746, Attribute.PRIVATE),
	RATING_PERCENT("Rating Percent", (short)0x4749, Attribute.PRIVATE),
	
	IMAGE_ID("Image ID", (short)0x800D, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	MATTEING("Matteing", (short)0x80e3, Attribute.PRIVATE),
	
	COPYRIGHT("Copyright", (short)0x8298, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	// (International Press Telecommunications Council) metadata.
	IPTC("Rich Tiff IPTC", (short)0x83BB, Attribute.PRIVATE) {
		public FieldType getFieldType() {
			return FieldType.UNDEFINED; // Or BYTE
		}
	},
	
	IT8_SITE("IT8 Site", (short)0x84e0, Attribute.PRIVATE),
    IT8_COLOR_SEQUENCE("IT8 Color Sequence", (short)0x84e1, Attribute.PRIVATE),
    IT8_HEADER("IT8 Header", (short)0x84e2, Attribute.PRIVATE),
    IT8_RASTER_PADDING("IT8 Raster Padding", (short)0x84e3, Attribute.PRIVATE),
    IT8_BITS_PER_RUN_LENGTH("IT8 Bits Per Run Length", (short)0x84e4, Attribute.PRIVATE),
    IT8_BITS_PER_EXTENDED_RUN_LENGTH("IT8 Bits Per Extended Run Length", (short)0x84e5, Attribute.PRIVATE),
    IT8_COLOR_TABLE("IT8 Color Table", (short)0x84e6, Attribute.PRIVATE),
    IT8_IMAGE_COLOR_INDICATOR("IT8 Image Color Indicator", (short)0x84e7, Attribute.PRIVATE),
    IT8_BKG_COLOR_INDICATOR("IT8 Bkg Color Indicator", (short)0x84e8, Attribute.PRIVATE),
    IT8_IMAGE_COLOR_VALUE("IT8 Image Color Value", (short)0x84e9, Attribute.PRIVATE),
    IT8_BKG_COLOR_VALUE("IT8 Bkg Color Value", (short)0x84ea, Attribute.PRIVATE),
    IT8_PIXEL_INTENSITY_RANGE("IT8 Pixel Intensity Range", (short)0x84eb, Attribute.PRIVATE),
    IT8_TRANSPARENCY_INDICATOR("IT8 Transparency Indicator", (short)0x84ec, Attribute.PRIVATE),
    IT8_COLOR_CHARACTERIZATION("IT8 Color Characterization", (short)0x84ed, Attribute.PRIVATE),
    IT8_HC_USAGE("IT8 HC Usage", (short)0x84ee, Attribute.PRIVATE),
    
    IPTC2("Rich Tiff IPTC", (short)0x8568, Attribute.PRIVATE),
    
    FRAME_COUNT("Frame Count", (short)0x85b8, Attribute.PRIVATE),
   
    // Photoshop image resources
    PHOTOSHOP("Photoshop", (short)0x8649, Attribute.PRIVATE) {
		public FieldType getFieldType() {
			return FieldType.BYTE;
		}
	},
    
	// The following tag is for ExifSubIFD
	EXIF_SUB_IFD("Exif Sub IFD", (short)0x8769, Attribute.PRIVATE) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	IMAGE_LAYER("Image Layer", (short)0x87ac, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	ICC_PROFILE("ICC Profile", (short)0x8773, Attribute.PRIVATE) {
		public FieldType getFieldType() {
			return FieldType.UNDEFINED;
		}
	},
	
	// The following tag is for GPSSubIFD
	GPS_SUB_IFD("GPS Sub IFD", (short)0x8825, Attribute.PRIVATE) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	DATE_TIME_ORIGINAL("DateTime Original", (short)0x9003, Attribute.UNKNOWN),
	
	/* Photoshop-specific TIFF tag. Starts with a null-terminated character
	 * string of "Adobe Photoshop Document Data Block"
	 */
	IMAGE_SOURCE_DATA("Image Source Data", (short)0x935C, Attribute.PRIVATE),
	
	WINDOWS_XP_TITLE("WindowsXP Title", (short) 0x9c9b, Attribute.PRIVATE) {
		public String getFieldAsString(Object value) {
			//
			byte[] byteValue = (byte[]) value;
			String description = "";
			try {
				description = new String(byteValue, "UTF-16LE").trim();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			return description;
		}
		public boolean isCritical() {
			return false;
		}
	},
	
	WINDOWS_XP_COMMENT("WindowsXP Comment", (short)0x9c9c, Attribute.PRIVATE) {
		public String getFieldAsString(Object value) {
			//
			byte[] byteValue = (byte[])value;
			String description = "";
			try {
				description = new String(byteValue, "UTF-16LE").trim();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			return description;
		}
		public boolean isCritical() {
			return false;
		}
	},
	
	WINDOWS_XP_AUTHOR("WindowsXP Author", (short)0x9c9d, Attribute.PRIVATE) {
		public String getFieldAsString(Object value) {
			//
			byte[] byteValue = (byte[])value;
			String description = "";
			try {
				description = new String(byteValue, "UTF-16LE").trim();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			return description;
		}
		public boolean isCritical() {
			return false;
		}
	},
	
	WINDOWS_XP_KEYWORDS("WindowsXP Keywords", (short)0x9c9e, Attribute.PRIVATE){
		public String getFieldAsString(Object value) {
			//
			byte[] byteValue = (byte[])value;
			String description = "";
			try {
				description = new String(byteValue, "UTF-16LE").trim();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			return description;
		}
		public boolean isCritical() {
			return false;
		}
	},
	
	WINDOWS_XP_SUBJECT("WindowsXP Subject", (short) 0x9c9f, Attribute.PRIVATE) {
		public String getFieldAsString(Object value) {
			//
			byte[] byteValue = (byte[]) value;
			String description = "";
			try {
				description = new String(byteValue, "UTF-16LE").trim();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			return description;
		}
		public boolean isCritical() {
			return false;
		}
	},
	
	// Ranking unknown tag the least significant.
	UNKNOWN("Unknown", (short)0xffff, Attribute.UNKNOWN); 
	
	public enum Attribute {
		BASELINE, EXTENDED, PRIVATE, UNKNOWN;
		
		@Override public String toString() {
			return StringUtils.capitalizeFully(name());
		}
	} 
	
	private static final Map<Short, TiffTag> tagMap = new HashMap<Short, TiffTag>();
	
	static
    {
      for(TiffTag tiffTag : values()) {
           tagMap.put(tiffTag.getValue(), tiffTag);
      }
    }	
	
	public static Tag fromShort(short value) {
       	TiffTag tiffTag = tagMap.get(value);
    	if (tiffTag == null)
    	   return UNKNOWN;
       	return tiffTag;
    }
	
	private final String name;
	
	private final short value;
	
    private final Attribute attribute;
    
    private TiffTag(String name, short value, Attribute attribute)
	{
		this.name = name;
		this.value = value;
		this.attribute = attribute;
	}
       
    public Attribute getAttribute() {
		return attribute;
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
    
    public String getName()	{
		return this.name;
	}
    
    public short getValue()	{
		return this.value;
	}
    
    public boolean isCritical() {
    	return true;
    }
    
    @Override
    public String toString() {
		if (this == UNKNOWN)
			return name;
		return name + " [Value: " + StringUtils.shortToHexStringMM(value) +"] (" + getAttribute() + ")";
	}
}
