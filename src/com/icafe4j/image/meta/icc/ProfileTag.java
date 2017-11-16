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

package com.icafe4j.image.meta.icc;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ICC Profile Tag
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/16/2014
 */
public enum ProfileTag {
	// Public tags
	A2B0(TagType.PUBLIC, 0x41324230, "AToB0Tag"), // Multi-dimensional transformation structure
	A2B1(TagType.PUBLIC, 0x41324231, "AToB1Tag"), // Multi-dimensional transformation structure
	A2B2(TagType.PUBLIC, 0x41324232, "AToB2Tag"), // Multi-dimensional transformation structure
	bXYZ(TagType.PUBLIC, 0x6258595A, "blueMatrixColumnTag"), // The third column in the matrix used in matrix/TRC transforms. (This column is combined with the linear blue channel during the matrix multiplication).
	bTRC(TagType.PUBLIC, 0x62545243, "blueTRCTag"), // Blue channel tone reproduction curve
	B2A0(TagType.PUBLIC, 0x42324130, "BToA0Tag"), // Multi-dimensional transformation structure
	B2A1(TagType.PUBLIC, 0x42324131, "BToA1Tag"), // Multi-dimensional transformation structure
	B2A2(TagType.PUBLIC, 0x42324132, "BToA2Tag"), // Multi-dimensional transformation structure
	B2D0(TagType.PUBLIC, 0x42324430, "BToD0Tag"), // Multi-dimensional transformation structure
	B2D1(TagType.PUBLIC, 0x42324431, "BToD1Tag"), // Multi-dimensional transformation structure
	B2D2(TagType.PUBLIC, 0x42324432, "BToD2Tag"), // Multi-dimensional transformation structure
	B2D3(TagType.PUBLIC, 0x42324433, "BToD3Tag"), // Multi-dimensional transformation structure
	BKPT(TagType.PUBLIC, 0x626b7074, "mediaBlackPointTag"), // nCIEXYZ of media black point
	calt(TagType.PUBLIC, 0x63616C74, "calibrationDateTimeTag"), // Profile calibration date and time
	chad(TagType.PUBLIC, 0x63686164, "chromaticAdaptationTag"), // Converts an nCIEXYZ colour relative to the actual adopted white to the nCIEXYZ colour relative to the PCS adopted white. Required only if the chromaticity of the actual adopted white is different from that of the PCS adopted white.
	chrm(TagType.PUBLIC, 0x6368726D, "chromaticityTag"), // Set of phosphor/colorant chromaticity
	clro(TagType.PUBLIC, 0x636C726F, "colorantOrderTag"), // Identifies the laydown order of colorants
	clrt(TagType.PUBLIC, 0x636C7274, "colorantTableTag"), // Identifies the colorants used in the profile. Required for N-component based Output profiles and DeviceLink profiles only if the data colour space field is xCLR (e.g. 3CLR)
	clot(TagType.PUBLIC, 0x636C6F74, "colorantTableOutTag"), // Identifies the output colorants used in the profile, required only if the PCS Field is xCLR (e.g. 3CLR)
	ciis(TagType.PUBLIC, 0x63696973, "colorimetricIntentImageStateTag"), // Indicates the image state of PCS colorimetry produced using the colorimetric intent transforms
	cprt(TagType.PUBLIC, 0x63707274, "copyrightTag"), // Profile copyright information
	desc(TagType.PUBLIC, 0x64657363, "profileDescriptionTag"), // Structure containing invariant and localizable versions of the profile name for displays
	desm(TagType.PRIVATE, 0x6473636d, "appleMultilanguageDescriptionTag"),
	dmnd(TagType.PUBLIC, 0x646D6E64, "deviceMfgDescTag"), // Displayable description of device manufacturer
	dmdd(TagType.PUBLIC, 0x646D6464, "deviceModelDescTag"), // Displayable description of device model
	D2B0(TagType.PUBLIC, 0x44324230, "DToB0Tag"), // Multi-dimensional transformation structure
	D2B1(TagType.PUBLIC, 0x44324231, "DToB1Tag"), // Multi-dimensional transformation structure
	D2B2(TagType.PUBLIC, 0x44324232, "DToB2Tag"), // Multi-dimensional transformation structure
	D2B3(TagType.PUBLIC, 0x44324233, "DToB3Tag"), // Multi-dimensional transformation structure
	gamt(TagType.PUBLIC, 0x67616D74, "gamutTag"), // Out of gamut: 8-bit or 16-bit data
	gTRC(TagType.PUBLIC, 0x67545243, "greenTRCTag"), // Green channel tone reproduction curve
	gXYZ(TagType.PUBLIC, 0x6758595A, "greenMatrixColumnTag"), // The second column in the matrix used in matrix/TRC transforms (This column is combined with the linear green channel during the matrix multiplication).
	kTRC(TagType.PUBLIC, 0x6B545243, "grayTRCTag"), // Grey tone reproduction curve
	lumi(TagType.PUBLIC, 0x6C756D69, "luminanceTag"), // Absolute luminance for emissive device
	meas(TagType.PUBLIC, 0x6D656173, "measurementTag"), // Alternative measurement specification information
	mmod(TagType.PRIVATE, 0x6d6d6f64, "MakeAndModel"),
	ncl2(TagType.PUBLIC, 0x6E636C32, "namedColor2Tag"), // PCS and optional device representation for named colours
	pre0(TagType.PUBLIC, 0x70726530, "preview0Tag"), // Preview transformation: 8-bit or 16-bit data
	pre1(TagType.PUBLIC, 0x70726531, "preview1Tag"), // Preview transformation: 8-bit or 16-bit data
	pre2(TagType.PUBLIC, 0x70726532, "preview2Tag"), // Preview transformation: 8-bit or 16-bit data
	pseq(TagType.PUBLIC, 0x70736571, "profileSequenceDescTag"), // An array of descriptions of the profile sequence
	psid(TagType.PUBLIC, 0x70736964, "profileSequenceIdentifierTag"),
	resp(TagType.PUBLIC, 0x72657370, "outputResponseTag"), //	Description of the desired device response
	rigo(TagType.PUBLIC, 0x72696730, "perceptualRenderingIntentGamutTag"), // When present, the specified gamut is defined to be the reference medium gamut for the PCS side of both the A2B0 and B2A0 tags
	rig2(TagType.PUBLIC, 0x72696732, "saturationRenderingIntentGamutTag"), //	When present, the specified gamut is defined to be the reference medium gamut for the PCS side of both the A2B2 and B2A2 tags
	rXYZ(TagType.PUBLIC, 0x7258595A, "redMatrixColumnTag"), // The first column in the matrix used in matrix/TRC transforms. (This column is combined with the linear red channel during the matrix multiplication).
	rTRC(TagType.PUBLIC, 0x72545243, "redTRCTag"), //	Red channel tone reproduction curve
	targ(TagType.PUBLIC, 0x74617267, "charTargetTag"), //	Characterization target such as IT8/7.2
	tech(TagType.PUBLIC, 0x74656368, "technologyTag"), //	Device technology information such as LCD, CRT, Dye Sublimation, etc.
	vcgt(TagType.PRIVATE, 0x76636774, "VideoCardGammaType"),
	vued(TagType.PUBLIC, 0x76756564, "viewingCondDescTag"), // Viewing condition description
	view(TagType.PUBLIC, 0x76696577, "viewingConditionsTag"), // Viewing condition parameters
	wtpt(TagType.PUBLIC, 0x77747074, "mediaWhitePointTag"), // nCIEXYZ of media white point

	UNKNOWN(TagType.UNKNOWN, 0xFFFFFFFF, "UnknownTag");
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileTag.class);

	public enum TagType { //
		PUBLIC,
		PRIVATE,
		UNKNOWN;		
	}
	
	public TagType getTagType() {
		return tagType;
	}
	
	private ProfileTag(TagType tagType, int value, String description) {
		this.description = description;
		this.value = value;
		this.tagType = tagType;
	}	
	
	public String getDescription() {
		return description;
	}
	
	public int getValue() {
		return value;
	}
	
	@Override
    public String toString() {
		return name() + " (" + description + ")";
	}
	
    public static ProfileTag fromInt(int value) {
       	ProfileTag tag = typeMap.get(value);
    	if (tag == null) {
    	 LOGGER.warn("tag value 0x{} unknown", Integer.toHexString(value));
    		return UNKNOWN;
    	}
   		return tag;
    }
    
    private static final Map<Integer, ProfileTag> typeMap = new HashMap<Integer, ProfileTag>();
       
    static
    {
      for(ProfileTag tagSignature : values())
           typeMap.put(tagSignature.getValue(), tagSignature);
    } 
	
    private final TagType tagType;
	private final String description;
	private final int value;
}