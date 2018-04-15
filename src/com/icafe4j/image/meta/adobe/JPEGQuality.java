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
 * JPEGQuality.java
 *
 * Who   Date       Description
 * ====  =========  ==================================================
 * WY    16Apr2015  Changed int constants to enums
 */

package com.icafe4j.image.meta.adobe;

import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.io.IOUtils;

public class JPEGQuality extends _8BIM {
	public enum Format {
		FORMAT_STANDARD(0x0000),
		FORMAT_OPTIMISED(0x0001),
		FORMAT_PROGRESSIVE(0x0101);
		
		private final int value;
		
		private Format(int value) {
			this.value= value;
		}
		
		public int getValue() {
			return value;
		}
	}
	public enum ProgressiveScans {
		PROGRESSIVE_3_SCANS(0x0001),
		PROGRESSIVE_4_SCANS(0x0002),
		PROGRESSIVE_5_SCANS(0x0003);
		
		private final int value;
		
		private ProgressiveScans(int value) {
			this.value= value;
		}
			
		public int getValue() {
			return value;
		}		
	}
	public enum Quality {
		QUALITY_1_LOW(0xfffd),
		QUALITY_2_LOW(0xfffe),
		QUALITY_3_LOW(0xffff),
		QUALITY_4_LOW(0x0000),
		QUALITY_5_MEDIUM(0x0001),
		QUALITY_6_MEDIUM(0x0002),
		QUALITY_7_MEDIUM(0x0003),
		QUALITY_8_HIGH(0x0004),
		QUALITY_9_HIGH(0x0005),
		QUALITY_10_MAXIMUM(0x0006),
		QUALITY_11_MAXIMUM(0x0007),
		QUALITY_12_MAXIMUM(0x0008);		
		
		private final int value;
		
		private Quality(int value) {
			this.value= value;
		}
		
		public int getValue() {
			return value;
		}	
	}
	// Default values
	private int quality = Quality.QUALITY_5_MEDIUM.getValue();	
	private int format = Format.FORMAT_STANDARD.getValue();;	
	private int progressiveScans = ProgressiveScans.PROGRESSIVE_3_SCANS.getValue();
	private byte trailer = 0x01;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(JPEGQuality.class);
	
	public JPEGQuality() {
		this("JPEGQuality");
	}
	
	public JPEGQuality(String name) {
		super(ImageResourceID.JPEG_QUALITY, name, null);
	}
	
	public JPEGQuality(String name, byte[] data) {
		super(ImageResourceID.JPEG_QUALITY, name, data);
		read();
	}
	
	public JPEGQuality(Quality quality, Format format, ProgressiveScans progressiveScans) {
		this("JPEGQuality", quality, format, progressiveScans);
	}
	
	public JPEGQuality(String name, Quality quality, Format format, ProgressiveScans progressiveScans) {
		super(ImageResourceID.JPEG_QUALITY, name, null);
		// Null check
		if(quality == null || format == null || progressiveScans == null)
			throw new IllegalArgumentException("Input parameter(s) is null");
		this.quality = quality.getValue();
		this.format = format.getValue();
		this.progressiveScans = progressiveScans.getValue();
	}
	
	public int getFormat() {
		return format;
	}
	
	public String getFormatAsString() {
		String retVal = "";
		
		switch (format) {
			case 0x0000:
				retVal = "Standard Format";
				break;
			case 0x0001:
				retVal= "Optimised Format";
				break;
			case 0x0101:
				retVal = "Progressive Format";
				break;
			default:
		}
		
		return retVal;
	}
	
	protected MetadataEntry getMetadataEntry() {
		//
		ImageResourceID eId  = ImageResourceID.fromShort(getID());
		MetadataEntry entry = new MetadataEntry(eId.name(), eId.getDescription(), true);
		entry.addEntry(new MetadataEntry("Quality", getQualityAsString()));
		entry.addEntry(new MetadataEntry("Format", getFormatAsString()));
		entry.addEntry(new MetadataEntry("Progressive Scans", getProgressiveScansAsString()));
				
		return entry;
	}
	
	public int getProgressiveScans() {
		return progressiveScans;
	}
	
	public String getProgressiveScansAsString() {
		String retVal = "";
		
		switch (progressiveScans) {
			case 0x0001:
				retVal = "3 Scans";
				break;
			case 0x0002:
				retVal = "4 Scans";
				break;
			case 0x0003:
				retVal = "5 Scans";
				break;
			default:
		}
		
		return retVal;
	}
	
	public int getQuality() {
		return quality;
	}

	public String getQualityAsString() {
		String retVal = "";
		
		switch (quality) {
			case 0xfffd:
				retVal = "Quality 1 (Low)";
				break;
			case 0xfffe:
				retVal ="Quality 2 (Low)";
				break;
			case 0xffff:
				retVal = "Quality 3 (Low)";
				break;
			case 0x0000:
				retVal = "Quality 4 (Low)";
				break;
			case 0x0001:
				retVal = "Quality 5 (Medium)";
				break;
			case 0x0002:
				retVal = "Quality 6 (Medium)";
				break;
			case 0x0003:
				retVal = "Quality 7 (Medium)";
				break;
			case 0x0004:
				retVal= "Quality 8 (High)";
				break;
			case 0x0005:
				retVal = "Quality 9 (High)";
				break;
			case 0x0006:
				retVal = "Quality 10 (Maximum)";
				break;
			case 0x0007:
				retVal = "Quality 11 (Maximum)";
				break;
			case 0x0008:
				retVal = "Quality 12 (Maximum)";
				break;
			default:
		}
		
		return retVal;
	}
	
	public void print() {
		super.print();
		LOGGER.info("{} : {} : {} - Plus 1 byte unknown trailer value = {}", getQualityAsString(), getFormatAsString(), getProgressiveScansAsString(), trailer);
	}
	
	private void read() {
		// PhotoShop Save As Quality
		// index 0: Quality level
		quality = IOUtils.readUnsignedShortMM(data, 0);
		format = IOUtils.readUnsignedShortMM(data, 2);
		progressiveScans = IOUtils.readUnsignedShortMM(data, 4);	
		trailer = data[6];// Always seems to be 0x01
	}
	
	public void setFormat(Format format) {
		if(format == null) throw new IllegalArgumentException("Input format is null");
		this.format = format.getValue();
	}
	
	public void setProgressiveScans(ProgressiveScans progressiveScans) {
		if(progressiveScans == null) throw new IllegalArgumentException("Input progressive scans is null");
		this.progressiveScans = progressiveScans.getValue();
	}
	
	public void setQuality(Quality quality) {
		if(quality == null) throw new IllegalArgumentException("Input quality is null");
		this.quality = quality.getValue();
	}
	
	public void write(OutputStream os) throws IOException {
		if(data == null) {
			data = new byte[7];
			data[0] = (byte)((quality>>8)&0xff);
			data[1] = (byte)(quality&0xff);
			data[2] = (byte)((format>>8)&0xff);
			data[3] = (byte)(format&0xff);
			data[4] = (byte)((progressiveScans>>8)&0xff);
			data[5] = (byte)(progressiveScans&0xff);
			data[6] = trailer;
			size = data.length;
		}
		super.write(os);
	}
}