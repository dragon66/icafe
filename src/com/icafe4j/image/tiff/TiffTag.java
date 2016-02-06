/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
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
	NEW_SUBFILE_TYPE("NewSubfileType", (short)0x00FE, Attribute.BASELINE) {
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
	
	SUBFILE_TYPE("SubfileType", (short)0x00FF, Attribute.BASELINE) {
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
	
	IMAGE_WIDTH("ImageWidth", (short)0x0100, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	IMAGE_LENGTH("ImageLength", (short)0x0101, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	BITS_PER_SAMPLE("BitsPerSample", (short)0x0102, Attribute.BASELINE) {
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
	
	PHOTOMETRIC_INTERPRETATION("PhotometricInterpretation", (short)0x0106, Attribute.BASELINE) {
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
	
	CELL_WIDTH("CellWidth", (short)0x0108, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	CELL_LENGTH("CellLength", (short)0x0109, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	FILL_ORDER("FillOrder", (short)0x010A, Attribute.BASELINE) {
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
	
	DOCUMENT_NAME("DocumentName", (short)0x010D, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	IMAGE_DESCRIPTION("ImageDescription", (short)0x010E, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	MAKE("Make", (short)0x010F, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	MODEL("Model", (short)0x0110, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
		}
	},
	
	STRIP_OFFSETS("StripOffsets", (short)0x0111, Attribute.BASELINE) {
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
			
			return "Warning: unknown planar configuration value: " + intValue;
		}
	},
	
	SAMPLES_PER_PIXEL("SamplesPerPixel", (short)0x0115, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	ROWS_PER_STRIP("RowsPerStrip", (short)0x0116, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	STRIP_BYTE_COUNTS("StripByteCounts", (short)0x0117, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	MIN_SAMPLE_VALUE("MinSampleValue", (short)0x0118, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	MAX_SAMPLE_VALUE("MaxSampleValue", (short)0x0119, Attribute.BASELINE) {
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
	
	PLANAR_CONFIGURATTION("PlanarConfiguration", (short)0x011C, Attribute.BASELINE) {
		public String getFieldAsString(Object value) {
			return TiffFieldEnum.PlanarConfiguration.fromValue(((int[])value)[0]).getDescription();
		}
		
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	PAGE_NAME("	PageName", (short)0x011D, Attribute.EXTENDED),
	X_POSITION("XPosition", (short)0x011E, Attribute.EXTENDED),
	Y_POSITION("YPosition", (short)0x011F, Attribute.EXTENDED),
	
	FREE_OFFSETS("FreeOffsets", (short)0x0120, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	FREE_BYTE_COUNTS("FreeByteCounts", (short)0x0121, Attribute.BASELINE) {
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
	GRAY_RESPONSE_UNIT("GrayResponseUnit", (short)0x0122, Attribute.BASELINE) {
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
	
	GRAY_RESPONSE_CURVE("GrayResponseCurve", (short)0x0123, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	T4_OPTIONS("T4Options", (short)0x0124, Attribute.EXTENDED) {
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
	
	T6_OPTIONS("T6Options", (short)0x0125, Attribute.EXTENDED) {
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
	
	RESOLUTION_UNIT("ResolutionUnit", (short)0x0128, Attribute.BASELINE) {
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
	
	PAGE_NUMBER("PageNumber", (short)0x0129, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	TRANSFER_FUNCTION("TransferFunction", (short)0x012D, Attribute.EXTENDED),
	
	SOFTWARE("Software", (short)0x0131, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.ASCII;
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
	
	HOST_COMPUTER("HostComputer", (short)0x013C, Attribute.BASELINE),
	
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
	
	WHITE_POINT("WhitePoint", (short)0x013E, Attribute.EXTENDED),
	PRIMARY_CHROMATICITIES("PrimaryChromaticities", (short)0x013F, Attribute.EXTENDED),
	
	COLORMAP("ColorMap", (short)0x0140, Attribute.BASELINE) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	HALTONE_HINTS("HalftoneHints", (short)0x0141, Attribute.EXTENDED),
	
	TILE_WIDTH("TileWidth", (short)0x0142, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	TILE_LENGTH("TileLength", (short)0x0143, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	TILE_OFFSETS("TileOffsets", (short)0x0144, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	TILE_BYTE_COUNTS("TileByteCounts", (short)0x0145, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or SHORT
		}
	},
	
	BAD_FAX_LINES("BadFaxLines", (short)0x0146, Attribute.EXTENDED),
	CLEAN_FAX_DATA("CleanFaxData", (short)0x0147, Attribute.EXTENDED),
	CONSECUTIVE_BAD_FAX_LINES("ConsecutiveBadFaxLines", (short)0x0148, Attribute.EXTENDED),
	
	SUB_IFDS("SubIFDs", (short)0x014A, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG; // Or IFD
		}
	},
	
	INK_SET("InkSet", (short)0x014C, Attribute.EXTENDED) {
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
	
	INK_NAMES("InkNames", (short)0x014D, Attribute.EXTENDED),
	NUMBER_OF_INKS("NumberOfInks", (short)0x014E, Attribute.EXTENDED),
	DOT_RANGE("DotRange", (short)0x0150, Attribute.EXTENDED),
	TARGET_PRINTER("TargetPrinter", (short)0x0151, Attribute.EXTENDED),
	
	EXTRA_SAMPLES("ExtraSamples", (short)0x0152, Attribute.BASELINE) {
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
	
	SAMPLE_FORMAT("SampleFormat", (short)0x0153, Attribute.EXTENDED) {
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
	
	S_MIN_SAMPLE_VALUE("SMinSampleValue", (short)0x0154, Attribute.EXTENDED),
	S_MAX_SAMPLE_VALUE("SMaxSampleValue", (short)0x0155, Attribute.EXTENDED),
	TRANSFER_RANGE("TransferRange", (short)0x0156, Attribute.EXTENDED),
	CLIP_PATH("ClipPath", (short)0x0157, Attribute.EXTENDED),
	X_CLIP_PATH_UNITS("XClipPathUnits", (short)0x0158, Attribute.EXTENDED),
	Y_CLIP_PATH_UNITS("YClipPathUnits", (short)0x0159, Attribute.EXTENDED),
	
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
	
	OPI_PROXY("OPIProxy", (short)0x015F, Attribute.EXTENDED),
	
	GLOBAL_PARAMETERS_IFD("GlobalParametersIFD", (short)0x0190, Attribute.EXTENDED),
	
	PROFILE_TYPE("ProfileType", (short)0x0191, Attribute.EXTENDED) {
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
	
	FAX_PROFILE("FaxProfile", (short)0x0192, Attribute.EXTENDED) {
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
	
	CODING_METHODS("CodingMethods", (short)0x0193, Attribute.EXTENDED) {
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
	
	VERSION_YEAR("VersionYear", (short)0x0194, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.BYTE;
		}
	},
	
	MODE_NUMBER("ModeNumber", (short)0x0195, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.BYTE;
		}
	},
	
	DECODE("Decode", (short)0x01B1, Attribute.EXTENDED),
	DEFAULT_IMAGE_COLOR("DefaultImageColor", (short)0x01B2, Attribute.EXTENDED),
	
	JPEG_PROC("JPEGProc", (short)0x0200, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	JPEG_INTERCHANGE_FORMAT("JPEGInterchangeFormat/JpegIFOffset", (short)0x0201, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	JPEG_INTERCHANGE_FORMAT_LENGTH("JPEGInterchangeFormatLength/JpegIFByteCount", (short)0x0202, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	JPEG_RESTART_INTERVAL("JPEGRestartInterval", (short)0x0203, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	JPEG_LOSSLESS_PREDICTORS("JPEGLosslessPredictors", (short)0x0205, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	JPEG_POINT_TRANSFORMS("JPEGPointTransforms", (short)0x0206, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	JPEG_Q_TABLES("JPEGQTables", (short)0x0207, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	JPEG_DC_TABLES("JPEGDCTables", (short)0x0208, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	JPEG_AC_TABLES("JPEGACTables", (short)0x0209, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	YCbCr_COEFFICIENTS("YCbCrCoefficients", (short)0x0211, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.RATIONAL;
		}
	},
	
	YCbCr_SUB_SAMPLING("YCbCrSubSampling", (short)0x0212, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.SHORT;
		}
	},
	
	YCbCr_POSITIONING("YCbCrPositioning", (short)0x0213, Attribute.EXTENDED) {
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
	
	REFERENCE_BLACK_WHITE("ReferenceBlackWhite", (short)0x0214, Attribute.EXTENDED) {
		public FieldType getFieldType() {
			return FieldType.RATIONAL;
		}
	},
	
	STRIP_ROW_COUNTS("StripRowCounts", (short)0x022F, Attribute.EXTENDED) {
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
	RATING_PERCENT("RatingPercent", (short)0x4749, Attribute.PRIVATE),
	
	IMAGE_ID("ImageID", (short)0x800D, Attribute.EXTENDED) {
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
	IPTC("RichTiffIPTC", (short)0x83BB, Attribute.PRIVATE) {
		public FieldType getFieldType() {
			return FieldType.UNDEFINED; // Or BYTE
		}
	},
	
	IT8_SITE("IT8Site", (short)0x84e0, Attribute.PRIVATE),
    IT8_COLOR_SEQUENCE("IT8ColorSequence", (short)0x84e1, Attribute.PRIVATE),
    IT8_HEADER("IT8Header", (short)0x84e2, Attribute.PRIVATE),
    IT8_RASTER_PADDING("IT8RasterPadding", (short)0x84e3, Attribute.PRIVATE),
    IT8_BITS_PER_RUN_LENGTH("IT8BitsPerRunLength", (short)0x84e4, Attribute.PRIVATE),
    IT8_BITS_PER_EXTENDED_RUN_LENGTH("IT8BitsPerExtendedRunLength", (short)0x84e5, Attribute.PRIVATE),
    IT8_COLOR_TABLE("IT8ColorTable", (short)0x84e6, Attribute.PRIVATE),
    IT8_IMAGE_COLOR_INDICATOR("IT8ImageColorIndicator", (short)0x84e7, Attribute.PRIVATE),
    IT8_BKG_COLOR_INDICATOR("IT8BkgColorIndicator", (short)0x84e8, Attribute.PRIVATE),
    IT8_IMAGE_COLOR_VALUE("IT8ImageColorValue", (short)0x84e9, Attribute.PRIVATE),
    IT8_BKG_COLOR_VALUE("IT8BkgColorValue", (short)0x84ea, Attribute.PRIVATE),
    IT8_PIXEL_INTENSITY_RANGE("IT8PixelIntensityRange", (short)0x84eb, Attribute.PRIVATE),
    IT8_TRANSPARENCY_INDICATOR("IT8TransparencyIndicator", (short)0x84ec, Attribute.PRIVATE),
    IT8_COLOR_CHARACTERIZATION("IT8ColorCharacterization", (short)0x84ed, Attribute.PRIVATE),
    IT8_HC_USAGE("IT8HCUsage", (short)0x84ee, Attribute.PRIVATE),
    
    IPTC2("RichTiffIPTC", (short)0x8568, Attribute.PRIVATE),
    
    FRAME_COUNT("FrameCount", (short)0x85b8, Attribute.PRIVATE),
   
    // Photoshop image resources
    PHOTOSHOP("Photoshop", (short)0x8649, Attribute.PRIVATE) {
		public FieldType getFieldType() {
			return FieldType.BYTE;
		}
	},
    
	// The following tag is for ExifSubIFD
	EXIF_SUB_IFD("ExifSubIFD", (short)0x8769, Attribute.PRIVATE) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	IMAGE_LAYER("ImageLayer", (short)0x87ac, Attribute.EXTENDED) {
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
	GPS_SUB_IFD("GPSSubIFD", (short)0x8825, Attribute.PRIVATE) {
		public FieldType getFieldType() {
			return FieldType.LONG;
		}
	},
	
	/* Photoshop-specific TIFF tag. Starts with a null-terminated character
	 * string of "Adobe Photoshop Document Data Block"
	 */
	IMAGE_SOURCE_DATA("ImageSourceData", (short)0x935C, Attribute.PRIVATE),
	
	WINDOWS_XP_TITLE("WindowsXPTitle", (short) 0x9c9b, Attribute.PRIVATE) {
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
	},
	
	WINDOWS_XP_COMMENT("WindowsXPComment", (short)0x9c9c, Attribute.PRIVATE) {
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
	},
	
	WINDOWS_XP_AUTHOR("WindowsXPAuthor", (short)0x9c9d, Attribute.PRIVATE) {
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
	},
	
	WINDOWS_XP_KEYWORDS("WindowsXPKeywords", (short)0x9c9e, Attribute.PRIVATE){
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
	},
	
	WINDOWS_XP_SUBJECT("WindowsXPSubject", (short) 0x9c9f, Attribute.PRIVATE) {
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
    
    @Override
    public String toString() {
		if (this == UNKNOWN)
			return name;
		return name + " [Value: " + StringUtils.shortToHexStringMM(value) +"] (" + getAttribute() + ")";
	}
}