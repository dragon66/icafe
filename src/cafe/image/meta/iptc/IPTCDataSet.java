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

import cafe.string.StringUtils;

/**
 * International Press Telecommunications Council (IPTC) data set
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/10/2013
 */
public class IPTCDataSet {
	private int recordNumber;
	private int tag;
	private int size;
	private byte[] data;
	private int offset;
	
	public IPTCDataSet(int recordNumber, int tag, int size, byte[] data, int offset) {
		this.recordNumber = recordNumber;
		this.tag = tag;
		this.size = size;
		this.data = data;
		this.offset = offset;
	}
	
	public int getRecordNumber() {
		return recordNumber;
	}
	
	public int getTag() {
		return tag;
	}		
	
	public int getSize() {
		return size;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public void print() {
		
		switch (recordNumber) {
			case 1: //Envelope Record
				System.out.println("Record number " + recordNumber + ": Envelope Record");
				System.out.println("Dataset name: " + IPTCEnvelopeTag.fromTag(tag));
				break;
			case 2: //Application Record
				System.out.println("Record number " + recordNumber + ": Application Record");
				System.out.println("Dataset name: " + IPTCApplicationTag.fromTag(tag));
				break;
			case 3: //NewsPhoto Record
				System.out.println("Record number " + recordNumber + ": NewsPhoto Record");
				System.out.println("Dataset name: " + IPTCNewsPhotoTag.fromTag(tag));
				break;
			case 7: //PreObjectData Record
				System.out.println("Record number " + recordNumber + ": PreObjectData Record");
				System.out.println("Dataset name: " + IPTCPreObjectDataTag.fromTag(tag));
				break;
			case 8: //ObjectData Record
				System.out.println("Record number " + recordNumber + ": ObjectData Record");
				System.out.println("Dataset name: " + IPTCObjectDataTag.fromTag(tag));
				break;				
			case 9: //PostObjectData Record
				System.out.println("Record number " + recordNumber + ": PostObjectData Record");
				System.out.println("Dataset name: " + IPTCPostObjectDataTag.fromTag(tag));
				break;	
			case 240: //FotoStation Record
				System.out.println("Record number " + recordNumber + ": FotoStation Record");
				System.out.println("Dataset name: " + IPTCFotoStationTag.fromTag(tag));
				break;	
			default:
				System.out.println("Record number " + recordNumber + ": Unknown Record");
				break;
		}
		
		System.out.println("Dataset tag: " + tag + "[" + StringUtils.shortToHexStringMM((short)tag) + "]");
		System.out.println("Dataset size: " + size);
		System.out.println("Dataset value: " + new String(data, offset, size).trim());
	}	
}
