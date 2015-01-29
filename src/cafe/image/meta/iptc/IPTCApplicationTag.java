/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.iptc;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines DataSet tags for IPTC Application Record - Record number 2.
 * * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/10/2013
 */
public enum IPTCApplicationTag {
	 RECORD_VERSION(0, "ApplicationRecordVersion"),
	 OBJECT_TYPE_REF(3, "ObjectTypeRef"),
	 OBJECT_ATTR_REF(4, "ObjectAttribRef"),
	 OBJECT_NAME(5, "ObjectName"),
	 EDIT_STATUS(7, "EditStatus"),
	 EDITORIAL_UPDATE(8, "EditorialUpdate"),
	 URGENCY(10, "Urgency"),
	 SUBJECT_REF(12, "SubjectReference"),
	 CATEGORY(15, "Category"),
	 SUPP_CATEGORY(20, "SupplementalCategories"),
	 FIXTURE_ID(22, "FixtureID"),
	 KEY_WORDS(25, "Keywords"),
	 CONTENT_LOCATION_CODE(26, "ContentLocationCode"),
	 CONTENT_LOCATION_NAME(27, "ContentLocationName"),
	 RELEASE_DATE(30, "ReleaseDate"),
	 RELEASE_TIME(35, "ReleaseTime"),
	 EXPIRATION_DATE(37, "ExpirationDate"),
	 EXPIRATION_TIME(38, "ExpirationTime"),
	 SPECIAL_INSTRUCTIONS(40, "SpecialInstructions"),
	 ACTION_ADVISED(42, "ActionAdvised"),
	 REFERENCE_SERVICE(45, "ReferenceService"),
	 REFERENCE_DATE(47, "ReferenceDate"),
	 REFERENCE_NUMBER(50, "ReferenceNumber"),
	 DATE_CREATED(55, "DateCreated"),
	 TIME_CREATED(60, "TimeCreated"),
	 DIGITAL_CREATION_DATE(62, "DigitalCreationDate"),
	 DIGITAL_CREATION_TIME(63, "DigitalCreationTime"),
	 ORIGINATING_PROGRAM(65, "OriginatingProgram"),
	 PROGRAM_VERSION(70, "ProgramVersion"),
	 OBJECT_CYCLE(75, "ObjectCycle"),
	 BY_LINE(80, "ByLine"),
	 BY_LINE_TITLE(85, "ByLineTitle"),
	 CITY(90, "City"),
	 SUB_LOCATION(92, "SubLocation"),
	 PROVINCE_STATE(95, "ProvinceState"),
	 COUNTRY_CODE(100, "CountryCode"),
	 COUNTRY_NAME(101, "CountryName"),
	 ORIGINAL_TRANSMISSION_REF(103, "OriginalTransmissionRef"),
	 HEADLINE(105, "Headline"),
	 CREDIT(110, "Credit"),
	 SOURCE(115, "Source"),
	 COPYRIGHT_NOTICE(116, "CopyrightNotice"),
	 CONTACT(118, "Contact"),
	 CAPTION_ABSTRACT(120, "CaptionAbstract"),
	 WRITER_EDITOR(122, "WriterEditor"),
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
	 
	 UNKNOWN(999, "Unknown");
	 
	 private IPTCApplicationTag(int tag, String name) {
		 this.tag = tag;
		 this.name = name;
	 }
	 
	 public String getName() {
		 return name;
	 }
	 
	 public int getTag() { return tag; }
	 
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
