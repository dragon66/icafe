/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.icc;

import java.io.IOException;
import java.io.InputStream;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.io.ReadStrategyMM;
import cafe.string.StringUtils;

/**
 * International Color Consortium Profile (ICC Profile)
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 07/02/2013
 */
public class ICCProfile {
	private ICCProfileHeader header;
	private ProfileTagTable tagTable;
	
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
	
	public ICCProfile(InputStream is) throws IOException {
		// Wrap the input stream with a RandomAccessInputStream
		RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(is);
		randIS.setReadStrategy(ReadStrategyMM.getInstance());
		this.header = new ICCProfileHeader();
		this.tagTable = new ProfileTagTable();
		readHeader(randIS);
		readTagTable(randIS);
		randIS.close();
	}
	
	private void readHeader(RandomAccessInputStream is) throws IOException {
		header.profileSize = is.readUnsignedInt();
		is.read(header.preferredCMMType);
		is.read(header.profileVersionNumber);
		header.profileClass = is.readInt();
		is.read(header.colorSpace);
		is.read(header.PCS);
		is.read(header.dateTimeCreated);
		is.read(header.profileFileSignature);
		is.read(header.primaryPlatformSignature);
		is.read(header.profileFlags);
		is.read(header.deviceManufacturer);
		is.read(header.deviceModel);
		is.read(header.deviceAttributes);
		header.renderingIntent = is.readInt();
		is.read(header.PCSXYZ);
		is.read(header.profileCreator);
		is.read(header.profileID);
		is.read(header.bytesReserved);
	}
	
	public String getColorSpace() {
		return new String(header.colorSpace);
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
	
	public String getPCS() {
		return new String(header.PCS);
	}
	
	public String getPreferredCMMType() {
		return new String(header.preferredCMMType);
	}
	
	public String getPrimaryPlatformSignature() {
		return new String(header.primaryPlatformSignature);
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
	
	public String getProfileFileSignature() {
		return new String(header.profileFileSignature);
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
	
	public boolean isEmbeddedInFile() {
		return (((header.profileFlags[0]>>7)&0x01) == 1);
	}
	
	public boolean canBeUsedIndependently() {
		return (((header.profileFlags[0]>>6)&0x01) == 0);
	}
	
	public String getProfileFlags() {
		return (isEmbeddedInFile()?"embedded in file":"not embedded") + ", " + (canBeUsedIndependently()?"used independently":"cannot be used independently");
	}
	
	public String getDeviceManufacturer() {
		return new String(header.deviceManufacturer);
	}
	
	public String getDeviceModel() {
		return new String(header.deviceModel);
	}
	
	public boolean isReflective() {
		return (((header.deviceAttributes[0]>>7)&0x01) == 0);
	}
	
	public boolean isGlossy() {
		return (((header.deviceAttributes[0]>>6)&0x01) == 0);
	}
	
	public boolean isPositive() {
		return (((header.deviceAttributes[0]>>5)&0x01) == 0);
	}
	
	public boolean isColor() {
		return (((header.deviceAttributes[0]>>4)&0x01) == 0);
	}
	
	public String getDeviceAttributes() {
		return (isReflective()?"reflective":"transparency") + ", " + (isGlossy()?"glossy":"matte") + ", " + (isPositive()?"positive":"negative") + ", " + (isColor()?"color":"black & white");
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
	
	public float[] getPCSXYZ() {
		float PCSX = IOUtils.readS15Fixed16MMNumber(header.PCSXYZ, 0);
		float PCSY = IOUtils.readS15Fixed16MMNumber(header.PCSXYZ, 4);
		float PCSZ = IOUtils.readS15Fixed16MMNumber(header.PCSXYZ, 8);
		
		return new float[] {PCSX, PCSY, PCSZ};
	}
	
	public String getProfileCreator() {
		return new String(header.profileCreator);
	}
	
	public String getProfileID() {
		return StringUtils.byteArrayToHexString(header.profileID);
	}
	
	public String getBytesReserved() {
		return StringUtils.byteArrayToHexString(header.bytesReserved);
	}
	
	private void readTagTable(RandomAccessInputStream is) throws IOException {
		tagTable.read(is);
	}
	
	public void showHeader() {
		System.out.println("*** Start of ICC_Profile Header ***");
		System.out.println("Profile Size: " + getProfileSize());
		System.out.println("CMM Type: " + getPreferredCMMType());
		System.out.println("Version: " + getProfileVersionNumber());
		System.out.println("Profile/Device Class: " + getProfileClassDescription());
		System.out.println("Color Space: " + getColorSpace());
		System.out.println("PCS: " + getPCS());
		System.out.println("Date Created: " + getDateTimeCreated());
		System.out.println("Profile File Signature: " + getProfileFileSignature());
		System.out.println("Primary Platform Signature: " + getPrimaryPlatformSignature());
		System.out.println("Flags: " + getProfileFlags());
		System.out.println("Device Manufacturer: " + getDeviceManufacturer());
		System.out.println("Device Model: " + getDeviceModel());
		System.out.println("Device Attributes: " + getDeviceAttributes());
		System.out.println("Rendering Intent: " + getRenderingIntentDescription());		
		System.out.println("PCS Illuminant: X = " + getPCSXYZ()[0] + ", Y = " + getPCSXYZ()[1] + ", Z = " + getPCSXYZ()[2]);
		System.out.println("Profile Creator: " + getProfileCreator());
		System.out.println("Profile ID: " + getProfileID());
		System.out.println("*** End of ICC_Profile Header ***");
	}
	
	public void showTagTable() {
		tagTable.showTable();
	}
}