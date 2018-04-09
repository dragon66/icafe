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
	 RECORD_VERSION(0, "Application Record Version") {
		 public String getDataAsString(byte[] data) {
			 // Hex representation of the data
			 return StringUtils.byteArrayToHexString(data, 0, 10);
		 }
	 },
	 OBJECT_TYPE_REF(3, "Object Type Ref"),
	 OBJECT_ATTR_REF(4, "Object Attrib Ref") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 OBJECT_NAME(5, "Object Name"),
	 EDIT_STATUS(7, "Edit Status"),
	 EDITORIAL_UPDATE(8, "Editorial Update"),
	 URGENCY(10, "Urgency"),
	 SUBJECT_REF(12, "Subject Reference") {
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
	 SUPP_CATEGORY(20, "Supplemental Categories") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 FIXTURE_ID(22, "Fixture ID"),
	 KEY_WORDS(25, "Keywords") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 CONTENT_LOCATION_CODE(26, "Content Location Code") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 CONTENT_LOCATION_NAME(27, "Content Location Name") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 RELEASE_DATE(30, "Release Date"),
	 RELEASE_TIME(35, "Release Time"),
	 EXPIRATION_DATE(37, "Expiration Date"),
	 EXPIRATION_TIME(38, "Expiration Time"),
	 SPECIAL_INSTRUCTIONS(40, "Special Instructions"),
	 ACTION_ADVISED(42, "Action Advised"),
	 REFERENCE_SERVICE(45, "Reference Service") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 REFERENCE_DATE(47, "Reference Date") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 REFERENCE_NUMBER(50, "Reference Number") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 DATE_CREATED(55, "Date Created"),
	 TIME_CREATED(60, "Time Created"),
	 DIGITAL_CREATION_DATE(62, "Digital Creation Date"),
	 DIGITAL_CREATION_TIME(63, "Digital Creation Time"),
	 ORIGINATING_PROGRAM(65, "Originating Program"),
	 PROGRAM_VERSION(70, "Program Version"),
	 OBJECT_CYCLE(75, "Object Cycle"),
	 BY_LINE(80, "ByLine") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 BY_LINE_TITLE(85, "ByLine Title") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 CITY(90, "City"),
	 SUB_LOCATION(92, "SubLocation"),
	 PROVINCE_STATE(95, "Province State"),
	 COUNTRY_CODE(100, "Country Code"),
	 COUNTRY_NAME(101, "Country Name"),
	 ORIGINAL_TRANSMISSION_REF(103, "Original Transmission Ref"),
	 HEADLINE(105, "Headline"),
	 CREDIT(110, "Credit") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 SOURCE(115, "Source"),
	 COPYRIGHT_NOTICE(116, "Copyright Notice") {
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
	 CAPTION_ABSTRACT(120, "Caption Abstract"),
	 WRITER_EDITOR(122, "Writer Editor") {
		 @Override
		 public boolean allowMultiple() {
			 return true;
		 }
	 },
	 RASTERIZED_CAPTION(125, "Rasterized Caption"),
	 IMAGE_TYPE(130, "Image Type"),
	 IMAGE_ORIENTATION(131, "Image Orientation"),
	 LANGUAGE_ID(135, "Language ID"),
	 AUDIO_TYPE(150, "Audio Type"),
	 AUDIO_SAMPLING_RATE(151, "Audio Sampling Rate"),
	 AUDIO_SAMPLING_RESOLUTION(152, "Audio Sampling Resolution"),
	 AUDIO_DURATION(153, "Audio Duration"),
	 AUDIO_OUTCUE(154, "Audio Out cue"),
	 OBJECT_DATA_PREVIEW_FILE_FORMAT(200, "Object Data Preview File Format"),
	 OBJECT_DATA_PREVIEW_FILE_FORMAT_VERSION(201, "Object Data Preview File Format Version"),
	 OBJECT_DATA_PREVIEW_DATA(202, "Object Data Preview Data"),
	 PHOTO_MECHANIC_PREFERENCES(221, "Photo Mechanic Preferences"),
	 CLASSIFY_STATE(225, "Classify State"),
	 SIMILARITY_INDEX(228, "Similarity Index"),
	 DOCUMENT_NOTES(230, "Document Notes"),
	 DOCUMENT_HISTORY(231, "Document History"),
	 EXIF_CAMERA_INFO(232, "Exif Camera Info"),
	 CATALOG_SETS(255, "Catalog Sets") {
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