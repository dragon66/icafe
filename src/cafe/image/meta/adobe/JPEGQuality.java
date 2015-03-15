/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
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
 */

package cafe.image.meta.adobe;

import java.io.IOException;
import java.io.OutputStream;

import cafe.io.IOUtils;

public class JPEGQuality extends _8BIM {
	// Constants
	public static final int QUALITY_1_LOW = 0xfffd;
	public static final int QUALITY_2_LOW = 0xfffe;
	public static final int QUALITY_3_LOW = 0xffff;
	public static final int QUALITY_4_LOW = 0x0000;
	public static final int QUALITY_5_MEDIUM = 0x0001;
	public static final int QUALITY_6_MEDIUM = 0x0002;
	public static final int QUALITY_7_MEDIUM = 0x0003;
	public static final int QUALITY_8_HIGH = 0x0004;
	public static final int QUALITY_9_HIGH = 0x0005;
	public static final int QUALITY_10_MAXIMUM = 0x0006;
	public static final int QUALITY_11_MAXIMUM = 0x0007;
	public static final int QUALITY_12_MAXIMUM = 0x0008;
	
	public static final int FORMAT_STANDARD = 0x0000;
	public static final int FORMAT_OPTIMISED = 0x0001;
	public static final int FORMAT_PROGRESSIVE = 0x0101;
	
	public static final int PROGRESSIVE_3_SCANS = 0x0001;
	public static final int PROGRESSIVE_4_SCANS = 0x0002;
	public static final int PROGRESSIVE_5_SCANS = 0x0003;
	
	private int quality;
	private int format;
	private int progressiveScans;
	private byte trailer = 0x01;
	
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
	
	private void read() {
		// PhotoShop Save As Quality
		// index 0: Quality level
		quality = IOUtils.readUnsignedShortMM(data, 0);
		format = IOUtils.readUnsignedShortMM(data, 2);
		progressiveScans = IOUtils.readUnsignedShortMM(data, 4);	
		trailer = data[6];// Always seems to be 0x01
	}
	
	public void print() {
		super.print();
		System.out.print(getQualityAsString());
		System.out.print(" : ");
		System.out.print(getFormatAsString());
		System.out.print(" : ");
		System.out.print(getProgressiveScansAsString());
		System.out.println(" - Plus 1 byte unknown trailer value = " + trailer); // Always seems to be 0x01
	}

	public void setFormat(int format) {
		this.format = format;
	}
	
	public void setProgressiveScans(int progressiveScans) {
		this.progressiveScans = progressiveScans;
	}
	
	public void setQuality(int quality) {
		this.quality = quality;
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