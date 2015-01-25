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
 * ====  =========  =================================================================
 * WY    24Jan2015  initial creation
 */

package cafe.image.meta.adobe;

import cafe.io.IOUtils;

public class JPEGQuality extends _8BIM {

	public JPEGQuality(String name, int size, byte[] data) {
		super(ImageResourceID.JPEG_QUALITY.getValue(), name, size, data);
	}
	
	public void show() {
		super.show();		
		// PhotoShop Save As Quality
		// index 0: Quality level
		byte[] data = getData();
		int value = IOUtils.readShortMM(data, 0);
	
		switch (value) {
			case 0xfffd:
				System.out.print("Quality 1 (Low)");
				break;
			case 0xfffe:
				System.out.print("Quality 2 (Low)");
				break;
			case 0xffff:
				System.out.print("Quality 3 (Low)");
				break;
			case 0x0000:
				System.out.print("Quality 4 (Low)");
				break;
			case 0x0001:
				System.out.print("Quality 5 (Medium)");
				break;
			case 0x0002:
				System.out.print("Quality 6 (Medium)");
				break;
			case 0x0003:
				System.out.print("Quality 7 (Medium)");
				break;
			case 0x0004:
				System.out.print("Quality 8 (High)");
				break;
			case 0x0005:
				System.out.print("Quality 9 (High)");
				break;
			case 0x0006:
				System.out.print("Quality 10 (Maximum)");
				break;
			case 0x0007:
				System.out.print("Quality 11 (Maximum)");
				break;
			case 0x0008:
				System.out.print("Quality 12 (Maximum)");
				break;
			default:
		}
		
		int format = IOUtils.readShortMM(data, 2);
		System.out.print(" : ");
		
		switch (format) {
			case 0x0000:
				System.out.print("Standard Format");
				break;
			case 0x0001:
				System.out.print("Optimised Format");
				break;
			case 0x0101:
				System.out.print("Progressive Format");
				break;
			default:
		}
		
		int progressiveScans = IOUtils.readShortMM(data, 4);
		System.out.print(" : ");
		
		switch (progressiveScans) {
			case 0x0001:
				System.out.print("3 Scans");
				break;
			case 0x0002:
				System.out.print("4 Scans");
				break;
			case 0x0003:
				System.out.print("5 Scans");
				break;
			default:
		}
		
		System.out.println(" - Plus 1 byte unknown trailer value = " + data[6]); // Always seems to be 0x01
	}
}