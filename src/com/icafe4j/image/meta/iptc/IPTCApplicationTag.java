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
 * Defines DataSet tags for IPTC Application Record - Record number 2.
 * * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/10/2013
 */
public enum IPTCApplicationTag implements IPTCTag {
	 RECORD_VERSION(0, "ApplicationRecordVersion") {
		 public String getDataAsString(byte[] data) {
			 // Hex representation of the data
			 return StringUtils.byteArrayToHexString(data, 0, 10);
		 }
	 },
	 OBJECT_TYPE_REF(3, "ObjectTypeRef"),
	 OBJECT_ATTR_REF(4, "ObjectAttribRef") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 OBJECT_NAME(5, "ObjectName"),
	 EDIT_STATUS(7, "EditStatus"),
	 EDITORIAL_UPDATE(8, "EditorialUpdate"),
	 URGENCY(10, "Urgency"),
	 SUBJECT_REF(12, "SubjectReference") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 CATEGORY(15, "Category"){
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 SUPP_CATEGORY(20, "SupplementalCategories") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 FIXTURE_ID(22, "FixtureID"),
	 KEY_WORDS(25, "Keywords") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 CONTENT_LOCATION_CODE(26, "ContentLocationCode") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 CONTENT_LOCATION_NAME(27, "ContentLocationName") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 RELEASE_DATE(30, "ReleaseDate"),
	 RELEASE_TIME(35, "ReleaseTime"),
	 EXPIRATION_DATE(37, "ExpirationDate"),
	 EXPIRATION_TIME(38, "ExpirationTime"),
	 SPECIAL_INSTRUCTIONS(40, "SpecialInstructions"),
	 ACTION_ADVISED(42, "ActionAdvised"),
	 REFERENCE_SERVICE(45, "ReferenceService") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 REFERENCE_DATE(47, "ReferenceDate") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 REFERENCE_NUMBER(50, "ReferenceNumber") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 DATE_CREATED(55, "DateCreated"),
	 TIME_CREATED(60, "TimeCreated"),
	 DIGITAL_CREATION_DATE(62, "DigitalCreationDate"),
	 DIGITAL_CREATION_TIME(63, "DigitalCreationTime"),
	 ORIGINATING_PROGRAM(65, "OriginatingProgram"),
	 PROGRAM_VERSION(70, "ProgramVersion"),
	 OBJECT_CYCLE(75, "ObjectCycle"),
	 BY_LINE(80, "ByLine") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 BY_LINE_TITLE(85, "ByLineTitle") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 CITY(90, "City"),
	 SUB_LOCATION(92, "SubLocation"),
	 PROVINCE_STATE(95, "ProvinceState"),
	 COUNTRY_CODE(100, "CountryCode"),
	 COUNTRY_NAME(101, "CountryName"),
	 ORIGINAL_TRANSMISSION_REF(103, "OriginalTransmissionRef"),
	 HEADLINE(105, "Headline"),
	 CREDIT(110, "Credit") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 SOURCE(115, "Source"),
	 COPYRIGHT_NOTICE(116, "CopyrightNotice") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 CONTACT(118, "Contact") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 CAPTION_ABSTRACT(120, "CaptionAbstract"),
	 WRITER_EDITOR(122, "WriterEditor") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 RASTERIZED_CAPTION(125, "RasterizedCaption"),
	 IMAGE_TYPE(130, "ImageType"),
	 IMAGE_ORIENTATION(131, "ImageOrientation"),
	 LANGUAGE_ID(135, "LanguageID"),
	 AUDIO_TYPE(150, "AudioType"),
	 AUDIO_SAMPLING_RATE(151, "AudioSamplingRate"),
	 AUDIO_SAMPLING_RESOLUTION(152, "AudioSamplingResolution"),
	 AUDIO_DURATION(153, "AudioDuration"),
	 AUDIO_OUTCUE(154, "AudioOutcue"),
	 OBJECT_DATA_PREVIEW_FILE_FORMAT(200, "ObjectDataPreviewFileFormat"),
	 OBJECT_DATA_PREVIEW_FILE_FORMAT_VERSION(201, "ObjectDataPreviewFileFormatVersion"),
	 OBJECT_DATA_PREVIEW_DATA(202, "ObjectDataPreviewData"),
	 PHOTO_MECHANIC_PREFERENCES(221, "PhotoMechanicPreferences"),
	 CLASSIFY_STATE(225, "ClassifyState"),
	 SIMILARITY_INDEX(228, "SimilarityIndex"),
	 DOCUMENT_NOTES(230, "DocumentNotes"),
	 DOCUMENT_HISTORY(231, "DocumentHistory"),
	 EXIF_CAMERA_INFO(232, "ExifCameraInfo"),
	 CATALOG_SETS(255, "CatalogSets") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	  
	 UNKNOWN(999, "Unknown");
	 
	 private IPTCApplicationTag(int tag, String name) {
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
	 
	 public static IPTCApplicationTag fromTag(int value) {
       	IPTCApplicationTag record = recordMap.get(value);
    	if (record == null)
    		return UNKNOWN;
    	return record;
    }
   
    @Override public String toString() {
	   return name;
    }
   
    private static final Map<Integer, IPTCApplicationTag> recordMap = new HashMap<Integer, IPTCApplicationTag>();
    
    static
    {
      for(IPTCApplicationTag record : values()) {
          recordMap.put(record.getTag(), record);
      }
    }	    
  
    private final int tag;
    private final String name;				
}