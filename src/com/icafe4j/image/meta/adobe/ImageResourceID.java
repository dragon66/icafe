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
 * ImageResourceID.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    01Oct2014  Moved from com.icafe4j.image.meta to com.icafe4j.image.meta.adobe
 */

package com.icafe4j.image.meta.adobe;

import java.util.HashMap;
import java.util.Map;

import com.icafe4j.string.StringUtils;

/**
 * Defines Image Resource IDs for Adobe Image Resource Block (IRB)
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/10/2013
 */
public enum ImageResourceID {
	// Adobe Image Resource Block IDs - JPEG APP13
	CHANNELS_ROWS_COLUMNS_DEPTH_MODE("Obsolete. Photoshop 2.0 only. Contains five 2 byte values: number of channels, rows, columns, depth, and mode.", (short)0x03e8),
	PRINT_MANAGER_INFO("Optional. Macintosh print manager print info record.", (short)0x03e9),
	XML("XML data.", (short)0x03ea),
	INDEXED_COLOR_TABLE("Obsolete. Photoshop 2.0 only. Contains the indexed color table.", (short)0x03eb),
	RESOLUTION_INFO("ResolutionInfo structure.", (short)0x03ed),
	ALPHA_CHANNEL_NAMES("Names of the alpha channels as a series of Pascal strings.", (short)0x03ee),
	DISPLAY_INFO("DisplayInfo structure.", (short)0x03ef),
	CAPTION("Optional. The caption as a Pascal string.", (short)0x03f0),
	BORDER_INFO("Border information. Contains a fixed-number for the border width, and 2 bytes for border units (1=inches, 2=cm, 3=points, 4=picas, 5=columns).", (short)0x03f1),
	BACKGROUND_COLOR("Background color.", (short)0x03f2),
	PRINT_FLAGS("Print flags. A series of one byte boolean values: labels, crop marks, color bars, registration marks, negative, flip, interpolate, caption.", (short)0x03f3),
	GRAYSCALE_INFO("Grayscale and multichannel halftoning information.", (short)0x03f4),	
	COLOR_HALFTONING_INFO("Color halftoning information.", (short)0x03f5),
	DUOTONE_HALFTONING_INFO("Duotone halftoning information.", (short)0x03f6),
	GRAYSCALE_FUNCTION("Grayscale and multichannel transfer function.", (short)0x03f7),
	COLOR_FUNCTION("Color transfer functions.", (short)0x03f8),
	DUOTONE_FUNCTION("Duotone transfer functions.", (short)0x03f9),
	DUOTONE_IMAGE_INFO("Duotone image information.", (short)0x03fa),
	EFFECTIVE_BW_VALUES("Two bytes for the effective black and white values for the dot range.", (short)0x03fb),
	OBSOLETE1("Obsolete.", (short)0x03fc),
	EPS_OPTIONS("EPS options.", (short)0x03fd),
	QUICK_MASK_INFO("Quick Mask information. 2 bytes containing Quick Mask channel ID, 1 byte boolean indicating whether the mask was initially empty.", (short)0x03fe),
	OBSOLETE2("Obsolete.", (short)0x03ff),
	LAYER_STATE_INFO("Layer state information. 2 bytes containing the index of target layer. 0=bottom layer.", (short)0x0400),
	WORKING_PATH("Working path (not saved).", (short)0x0401),
	LAYERS_GROUP_INFO("Layers group information. 2 bytes per layer containing a group ID for the dragging groups. Layers in a group have the same group ID.", (short)0x0402),
	OBSOLETE3("Obsolete.", (short)0x0403),
	IPTC_NAA("IPTC-NAA record.", (short)0x0404),
	IMAGE_MODE("Image mode for raw format files.", (short)0x0405),
	JPEG_QUALITY("JPEG quality. Private.", (short)0x0406),
	GRID_INFO("Grid and guides information.", (short)0x0408),
	THUMBNAIL_RESOURCE_PS4("Photoshop 4.0 thumbnail resource.", (short)0x0409), // Photoshop 4.0
	COPYRIGHT_FLAG("Copyright flag. Boolean indicating whether image is copyrighted. Can be set via Property suite or by user in File Info...", (short)0x040a),
	URL("URL. Handle of a text string with uniform resource locator. Can be set via Property suite or by user in File Info...", (short)0x040b),
	THUMBNAIL_RESOURCE_PS5("Photoshop 5.0 thumbnail resource.", (short)0x040c), // Photoshop 5.0
	GLOBLE_ANGLE("Global Angle. 4 bytes that contain an integer between 0..359 which is the global lighting angle for effects layer. If not present assumes 30.", (short)0x040d),
	COLOR_SAMPLERS_RESOURCE("Color samplers resource. See color samplers resource format later in this chapter.", (short)0x040e),
	ICC_PROFILE("ICC Profile. The raw bytes of an ICC format profile.", (short)0x040f),
	WATERMARK("One byte for Watermark.", (short)0x0410),
	ICC_UNTAGGED("ICC Untagged. 1 byte that disables any assumed profile handling when opening the file. 1 = intentionally untagged.", (short)0x0411),
	EFFECTS_VISIBLE("Effects visible. 1 byte global flag to show/hide all the effects layer. Only present when they are hidden.", (short)0x0412),
	SPOT_HALFTONE("Spot Halftone. 4 bytes for version, 4 bytes for length, and the variable length data.", (short)0x0413),
	DOC_SPECIFIC_ID("Document specific IDs, layer IDs will be generated starting at this base value or a greater value if we find existing IDs to already exceed it. 4 bytes.", (short)0x0414),
	UNICODE_ALPHA_NAMES("Unicode Alpha Names. 4 bytes for length and the string as a unicode string.", (short)0x0415),
	INDEXED_COLOR_TABLE_COUNT("Indexed Color Table Count. 2 bytes for the number of colors in table that are actually defined", (short)0x0416),
	TRANSPARENT_INDEX("Tansparent Index. 2 bytes for the index of transparent color, if any.", (short)0x0417),
	GLOBLE_ALTITUDE("Global Altitude. 4 byte entry for altitude.", (short)0x0419),	
	SLICES("Slices.", (short)0x041a),
	WORKFLOW_URL("Workflow URL. Unicode string, 4 bytes of length followed by unicode string.", (short)0x041b),
	JUMP_TO_XPEP("Jump To XPEP. 2 bytes major version, 2 bytes minor version, 4 bytes count.", (short)0x041c),
	ALPHA_IDENTIFIERS("Alpha Identifiers. 4 bytes of length, followed by 4 bytes each for every alpha identifier.", (short)0x041d),
	URL_LIST("URL List. 4 byte count of URLs, followed by 4 byte long, 4 byte ID, and unicode string for each count.", (short)0x041e),
	VERSION_INFO("(Photoshop 6.0) Version Info. 4 byte version, 1 byte HasRealMergedData, unicode string of writer name, unicode string of reader name, 4 bytes of file version.", (short)0x0421),
	EXIF_DATA1("(Photoshop 7.0) EXIF data 1.", (short)0x0422),
	EXIF_DATA3("(Photoshop 7.0) EXIF data 3.", (short)0x0423),
	XMP_METADATA("(Photoshop 7.0) XMP metadata.", (short)0x0424),
	CAPTION_DIGEST("(Photoshop 7.0) Caption digest. 16 bytes: RSA Data Security, MD5 message-digest algorithm.", (short)0x0425),
	PRINT_SCALE("(Photoshop 7.0) Print scale. 2 bytes style (0 = centered, 1 = size to fit, 2 = user defined). 4 bytes x location (floating point). 4 bytes y location (floating point). 4 bytes scale (floating point).", (short)0x0426),
	PIXEL_ASPECT_RATIO("(Photoshop CS) Pixel Aspect Ratio. 4 bytes (version = 1 or 2), 8 bytes double, x / y of a pixel. Version 2, attempting to correct values for NTSC and PAL, previously off by a factor of approx. 5%.", (short)0x0428),
	LAYER_COMPS("(Photoshop CS) Layer Comps. 4 bytes (descriptor version = 16).", (short)0x0429),
	ALTERNATE_DUOTONE_COLORS("(Photoshop CS) Alternate Duotone Colors.  bytes (version = 1), 2 bytes count, following is repeated for each count: [ Color: 2 bytes for space followed by 4 * 2 byte color component ], following this is another 2 byte count, usually 256, followed by Lab colors one byte each for L, a, b.", (short)0x042a),
	ALTERNATE_SPOT_COLORS("(Photoshop CS)Alternate Spot Colors. 2 bytes (version = 1), 2 bytes channel count, following is repeated for each count: 4 bytes channel ID, Color: 2 bytes for space followed by 4 * 2 byte color component", (short)0x042b),
	LAYER_SELECTION_IDS("(Photoshop CS2) Layer Selection ID(s). 2 bytes count, following is repeated for each count: 4 bytes layer ID.", (short)0x042d),
	HDR_TONING_INFO("(Photoshop CS2) HDR Toning information.", (short)0x042e),
	PRINT_INFO("(Photoshop CS2) Print info.", (short)0x042f),
	LAYER_GROUP_ENABLED_ID("(Photoshop CS2) Layer Group(s) Enabled ID. 1 byte for each layer in the document, repeated by length of the resource. NOTE: Layer groups have start and end markers.", (short)0x0430),
	COLOR_SAMPLERS_RESOURCE_CS3("(Photoshop CS3) Color samplers resource. Also see ID 1038 for old format.", (short)0x0431),
	MEASUREMENT_SCALE("(Photoshop CS3) Measurement Scale. 4 bytes (descriptor version = 16), Descriptor.", (short)0x0432),
	TIMELINE_INFO("(Photoshop CS3) Timeline Information. 4 bytes (descriptor version = 16).", (short)0x0433),
	SHEET_DISCLOSURE("(Photoshop CS3) Sheet Disclosure. 4 bytes (descriptor version = 16).", (short)0x0434),
	DISPLAY_INFO_STRUCTURE("(Photoshop CS3) DisplayInfo structure to support floating point clors. Also see ID 1007.", (short)0x0435),
	ONION_SKINS("(Photoshop CS3) Onion Skins. 4 bytes (descriptor version = 16).", (short)0x0436),
	COUNT_INFO("(Photoshop CS4) Count Information. 4 bytes (descriptor version = 16).", (short)0x0438),
	PRINT_INFO_CS5("(Photoshop CS5) Print Information. 4 bytes (descriptor version = 16).", (short)0x043a),
	PRINT_STYLE("(Photoshop CS5) Print Style. 4 bytes (descriptor version = 16).", (short)0x043b),
	MACINTOSH_NS_PRINT_INFO("(Photoshop CS5) Macintosh NSPrintInfo. Variable OS specific info for Macintosh. NSPrintInfo. It is recommened that you do not interpret or use this data.", (short)0x043c),
	WINDOWS_DEVMODE("(Photoshop CS5) Windows DEVMODE. Variable OS specific info for Windows. DEVMODE. It is recommened that you do not interpret or use this data.", (short)0x043d),
	AUTO_SAVE_FILE_PATH("(Photoshop CS6) Auto Save File Path. Unicode string. It is recommened that you do not interpret or use this data.", (short)0x043e),
	AUTO_SAVE_FORMAT("(Photoshop CS6) Auto Save Format. Unicode string. It is recommened that you do not interpret or use this data.", (short)0x043f),
	// 0x07d0-0x0bb8 (2000-3000)
	PATH_INFO0("Path Information (saved paths).", (short)0x07d0),
	PATH_INFO998("Path Information (saved paths).", (short)0x0bb6),
	CLIPPING_PATH_NAME("Name of clipping path.", (short)0x0bb7),
	ORIGIN_PATH_INFO("Origin path info.", (short)0x0bb8),
	// 0x0fa0-0x1387 (4000-4999)
	PLUGIN_RESOURCE0("Plug-In resource.", (short)0x0fa0),
	PLUGIN_RESOURCE999("Plug-In resource.", (short)0x1387),
	
	IMAGEREADY_VARIABLES("Image Ready variables. XML representation of variables definition.", (short)0x1b58),
	IMAGEREADY_DATASETS("Image Ready data sets.", (short)0x1b59),
	
	LIGHTROOM_WORKFLOW("(Photoshop CS3) Lightroom workflow, if present the document is in the middle of a Lightroom workflow.", (short)0x1f40),
	
	PRINT_FLAGS_INFO("Print flags information. 2 bytes version (=1), 1 byte center crop marks, 1 byte (=0), 4 bytes bleed width value, 2 bytes bleed width scale.", (short)0x2710),
	
	// unknown tag
	UNKNOWN("Unknown",  (short)0xffff); 
  	
	private ImageResourceID(String description, short value) {
		this.description = description;
		this.value = value;
	}
	
	public String getDescription() {
		return description;
	}
	
	public short getValue() {
		return value;
	}
	
	@Override
    public String toString() {
		if (this == UNKNOWN)
			return name();
		return name() + " [Value: " + StringUtils.shortToHexStringMM(value) +"]";
	}
	
    public static ImageResourceID fromShort(short value) {
       	ImageResourceID id = idMap.get(value);
    	if (id == null)
    		return UNKNOWN;
   		return id;
    }
    
    private static final Map<Short, ImageResourceID> idMap = new HashMap<Short, ImageResourceID>();
       
    static
    {
      for(ImageResourceID id : values()) {
           idMap.put(id.getValue(), id);
      }
    }
    
 	private final String description;
	private final short value;
}
