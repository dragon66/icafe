/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * ICCProfile.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    30Sep2014  Rewrite to use byte array as input
 * WY    29Sep2014  Added getData()
 * WY    29Sep2014  Added new constructor ICCProfile(byte[])
 */

package com.icafe4j.image.meta.icc;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.io.IOUtils;
import com.icafe4j.string.StringUtils;

/**
 * International Color Consortium Profile (ICC Profile)
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 07/02/2013
 */
public class ICCProfile extends Metadata {
	// Profile header - 128 bytes in length and contains 18 fields
	private static class ICCProfileHeader {
		private long profileSize;
		private byte[] preferredCMMType = new byte[4];
		private byte[] profileVersionNumber = new byte[4];
		private int profileClass;
		private byte[] colorSpace = new byte[4];
		private byte[] PCS = new byte[4];
		private byte[] dateTimeCreated = new byte[12];
		private byte[] profileFileSignature = new byte[4]; // "acsp" 61637370h
		private byte[] primaryPlatformSignature = new byte[4];
		private byte[] profileFlags = new byte[4];
		private byte[] deviceManufacturer = new byte[4];
		private byte[] deviceModel = new byte[4];
		private byte[] deviceAttributes = new byte[8];
		private int renderingIntent;
		private byte[] PCSXYZ = new byte[12];
		private byte[] profileCreator = new byte[4];
		private byte[] profileID = new byte[16];
		private byte[] bytesReserved = new byte[28];
	}
	public static final int TAG_TABLE_OFFSET = 128;

	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(ICCProfile.class);
	
	public static void showProfile(byte[] data) {
		if(data != null && data.length > 0) {
			ICCProfile icc_profile = new ICCProfile(data);
			try {
				icc_profile.read();
				icc_profile.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void showProfile(InputStream is) {
		try {
			showProfile(IOUtils.inputStreamToByteArray(is));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ICCProfileHeader header;	
	private ProfileTagTable tagTable;
	
	public ICCProfile(byte[] profile) {
		super(MetadataType.ICC_PROFILE, profile);
		ensureDataRead();
	}
	
	public ICCProfile(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public boolean canBeUsedIndependently() {
		return (((header.profileFlags[0]>>6)&0x01) == 0);
	}
	
	public String getBytesReserved() {
		return StringUtils.byteArrayToHexString(header.bytesReserved);
	}
	
	public String getColorSpace() {
		return new String(header.colorSpace).trim();
	}
	
	public String getDateTimeCreated() {
		int year = IOUtils.readUnsignedShortMM(header.dateTimeCreated, 0);
		int month = IOUtils.readUnsignedShortMM(header.dateTimeCreated, 2);
		int day = IOUtils.readUnsignedShortMM(header.dateTimeCreated, 4);
		int hour = IOUtils.readUnsignedShortMM(header.dateTimeCreated, 6);
		int minutes = IOUtils.readUnsignedShortMM(header.dateTimeCreated, 8);
		int seconds = IOUtils.readUnsignedShortMM(header.dateTimeCreated, 10);
		
		return year + "/" + month + "/" + day + ", " + hour + ":" + minutes + ":" + seconds;
	}
	
	public String getDeviceAttributes() {
		return (isReflective()?"reflective":"transparency") + ", " + (isGlossy()?"glossy":"matte") + ", " + (isPositive()?"positive":"negative") + ", " + (isColor()?"color":"black & white");
	}
	
	public String getDeviceManufacturer() {
		return new String(header.deviceManufacturer).trim();
	}
	
	public String getDeviceModel() {
		return new String(header.deviceModel).trim();
	}
	
	public String getPCS() {
		return new String(header.PCS).trim();
	}
	
	public float[] getPCSXYZ() {
		float PCSX = IOUtils.readS15Fixed16MMNumber(header.PCSXYZ, 0);
		float PCSY = IOUtils.readS15Fixed16MMNumber(header.PCSXYZ, 4);
		float PCSZ = IOUtils.readS15Fixed16MMNumber(header.PCSXYZ, 8);
		
		return new float[] {PCSX, PCSY, PCSZ};
	}
	
	public String getPreferredCMMType() {
		return new String(header.preferredCMMType).trim();
	}
	
	public String getPrimaryPlatformSignature() {
		return new String(header.primaryPlatformSignature).trim();
	}
	
	public String getProfileClass() {
		switch(header.profileClass) {
			case 0x73636E72:
				return "scnr";
			case 0x6D6E7472:
				return "mntr";
			case 0x70727472:
				return "prtr";
			case 0x6C696E6B:
				return "link";
			case 0x73706163:
				return "spac";
			case 0x61627374:
				return "abst";
			case 0x6E6D636C:
				return "nmcl";
			default:
				return "unknown";
		}
	}
	
	public String getProfileClassDescription() {
		switch(header.profileClass) {
			case 0x73636E72:
				return "'scnr': input devices - scanners and digital cameras";
			case 0x6D6E7472:
				return "'mntr': display devices - CRTs and LCDs";
			case 0x70727472:
				return "'prtr': output devices - printers";
			case 0x6C696E6B:
				return "'link': device link profiles";
			case 0x73706163:
				return "'spac': color space conversion profiles";
			case 0x61627374:
				return "'abst': abstract profiles";
			case 0x6E6D636C:
				return "'nmcl': named color profiles";
			default:
				throw new IllegalArgumentException("Unknown profile/device class: " + header.profileClass);
		}
	}
	
	public String getProfileCreator() {
		return new String(header.profileCreator).trim();
	}
	
	public String getProfileFileSignature() {
		return new String(header.profileFileSignature).trim();
	}
	
	public String getProfileFlags() {
		return (isEmbeddedInFile()?"embedded in file":"not embedded") + ", " + (canBeUsedIndependently()?"used independently":"cannot be used independently");
	}
	
	public String getProfileID() {
		return StringUtils.byteArrayToHexString(header.profileID);
	}
	
	public long getProfileSize() {
		return header.profileSize;
	}
	
	public String getProfileVersionNumber() {
		int majorVersion = (header.profileVersionNumber[0]&0xff);
		int minorRevision = ((header.profileVersionNumber[1]>>4)&0x0f);
		int bugFix = (header.profileVersionNumber[1]&0x0f);
		
		return "" + majorVersion + "." + minorRevision + bugFix;			
	}
	
	public int getRenderingIntent() {
		return header.renderingIntent&0x0000ffff;
	}
	
	public String getRenderingIntentDescription() {
		switch(header.renderingIntent&0x0000ffff) {
			case 0:
				return "perceptual";
			case 1:
				return "media-relative colorimetric";
			case 2:
				return "saturation";
			case 3:
				return "ICC-absolute colorimetric";
			default:
				throw new IllegalArgumentException("Unknown rendering intent: " + (header.renderingIntent&0x0000ffff));
		}
	}
	
	public ProfileTagTable getTagTable() {
		return tagTable;
	}
	
	public boolean isColor() {
		return (((header.deviceAttributes[0]>>4)&0x01) == 0);
	}
	
	public boolean isEmbeddedInFile() {
		return (((header.profileFlags[0]>>7)&0x01) == 1);
	}
	
	public boolean isGlossy() {
		return (((header.deviceAttributes[0]>>6)&0x01) == 0);
	}
	
	public boolean isPositive() {
		return (((header.deviceAttributes[0]>>5)&0x01) == 0);
	}
		
	public boolean isReflective() {
		return (((header.deviceAttributes[0]>>7)&0x01) == 0);
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			this.header = new ICCProfileHeader();
			this.tagTable = new ProfileTagTable();
			readHeader(data);
			readTagTable(data);
			isDataRead = true;
		}
	}
	
	private void readHeader(byte[] data) {
		header.profileSize = IOUtils.readUnsignedIntMM(data, 0);
		System.arraycopy(data, 4, header.preferredCMMType, 0, 4);
		System.arraycopy(data, 8, header.profileVersionNumber, 0, 4);
		header.profileClass = IOUtils.readIntMM(data, 12);
		System.arraycopy(data, 16, header.colorSpace, 0, 4);
		System.arraycopy(data, 20, header.PCS, 0, 4);
		System.arraycopy(data, 24, header.dateTimeCreated, 0, 12);
		System.arraycopy(data, 36, header.profileFileSignature, 0, 4);
		System.arraycopy(data, 40, header.primaryPlatformSignature, 0, 4);
		System.arraycopy(data, 44, header.profileFlags, 0, 4);
		System.arraycopy(data, 48, header.deviceManufacturer, 0, 4);
		System.arraycopy(data, 52, header.deviceModel, 0, 4);
		System.arraycopy(data, 56, header.deviceAttributes, 0, 8);
		header.renderingIntent = IOUtils.readIntMM(data, 64);
		System.arraycopy(data, 68, header.PCSXYZ, 0, 12);
		System.arraycopy(data, 80, header.profileCreator, 0, 4);
		System.arraycopy(data, 84, header.profileID, 0, 16);
		System.arraycopy(data, 100, header.bytesReserved, 0, 28);
	}
	
	private void readTagTable(byte[] data) {
		tagTable.read(data);
	}
	
	private void showHeader() {
		LOGGER.info("*** Start of ICC_Profile Header ***");
		LOGGER.info("Profile Size: {}", getProfileSize());
		LOGGER.info("CMM Type: {}", getPreferredCMMType());
		LOGGER.info("Version: {}", getProfileVersionNumber());
		LOGGER.info("Profile/Device Class: {}", getProfileClassDescription());
		LOGGER.info("Color Space: {}", getColorSpace());
		LOGGER.info("PCS: {}", getPCS());
		LOGGER.info("Date Created: {}", getDateTimeCreated());
		LOGGER.info("Profile File Signature: {}", getProfileFileSignature());
		LOGGER.info("Primary Platform Signature: {}", getPrimaryPlatformSignature());
		LOGGER.info("Flags: {}", getProfileFlags());
		LOGGER.info("Device Manufacturer: {}", getDeviceManufacturer());
		LOGGER.info("Device Model: {}", getDeviceModel());
		LOGGER.info("Device Attributes: {}", getDeviceAttributes());
		LOGGER.info("Rendering Intent: {}", getRenderingIntentDescription());		
		LOGGER.info("PCS Illuminant: X = {}, Y = {}, Z = {}", getPCSXYZ()[0], getPCSXYZ()[1], getPCSXYZ()[2]);
		LOGGER.info("Profile Creator: {}", getProfileCreator());
		LOGGER.info("Profile ID: {}", getProfileID());
		LOGGER.info("*** End of ICC_Profile Header ***");
	}
	
	@Override
	public void showMetadata() {
		ensureDataRead();
		showHeader();
		showTagTable();
	}

	private void showTagTable() {
		tagTable.showTable();
	}
}