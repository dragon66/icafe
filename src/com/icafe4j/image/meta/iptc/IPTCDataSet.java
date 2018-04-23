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
 * IPTCDataSet.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    16Aug2016  Added support for Unicode string data
 * WY    16Jul2015  Added two new constructors for IPTCApplicationTag
 * WY    19Dec2015  Added getDataAsString()
 * WY    01Feb2015  Added equals() and hashCode()
 * WY    29Jan2015  Fixed bug with write() write wrong data and size
 * WY    29Jan2015  Added name field as a key to HashMap usage
 */

package com.icafe4j.image.meta.iptc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.icafe4j.io.IOUtils;
import com.icafe4j.util.ArrayUtils;

/**
 * International Press Telecommunications Council (IPTC) data set
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/10/2013
 */
public class IPTCDataSet {
	// Fields
	private int recordNumber; // Corresponds to IPTCRecord enumeration recordNumber 
	private int tag; // Corresponds to IPTC tag enumeration tag field
	private int size;
	private byte[] data;
	private int offset;
	private IPTCTag tagEnum;
	
	// A unique name used as HashMap key
	private String name;
	
	private static final byte[] getBytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported encoding UTF-8");
		}
	}
	
	public IPTCDataSet(int tag, byte[] data) {
		this(IPTCRecord.APPLICATION, tag, data);
	}
	
	public IPTCDataSet(int recordNumber, int tag, int size, byte[] data, int offset) {
		this.recordNumber = recordNumber;
		this.tag = tag;
		this.size = size;
		this.data = data;
		this.offset = offset;		
		this.name = getTagName();
	}
	
	public IPTCDataSet(int tag, String value) {
		this(tag, IPTCDataSet.getBytes(value));
	}
	
	public IPTCDataSet(IPTCApplicationTag appTag, byte[] data) {
		this(appTag.getTag(), data);
	}
	
	public IPTCDataSet(IPTCApplicationTag appTag, String value) {
		this(appTag.getTag(), value);
	}
	
	public IPTCDataSet(IPTCRecord record, int tag, byte[] data) {
		this(record.getRecordNumber(), tag, data.length, data, 0);
	}
	
	public IPTCDataSet(IPTCRecord record, int tag, String value) {
		this(record, tag, IPTCDataSet.getBytes(value));
	}
	
	public boolean allowMultiple() {
		return tagEnum.allowMultiple();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IPTCDataSet other = (IPTCDataSet) obj;
		byte[] thisData = ArrayUtils.subArray(data, offset, size);
		byte[] thatData = ArrayUtils.subArray(other.data, other.offset, other.size);
		if (!Arrays.equals(thisData, thatData))
			return false;
		if (recordNumber != other.recordNumber)
			return false;
		if (tag != other.tag)
			return false;
		return true;
	}
	
	public byte[] getData() {
		return ArrayUtils.subArray(data, offset, size);
	}
	
	public String getDataAsString() {
		return tagEnum.getDataAsString(getData());
	}
	
	public String getName() {
		return name;
	}
	
	public String getRecordType() {
		//
		switch (recordNumber) {
			case 1: //Envelope Record
				return "Envelope Record";
			case 2: //Application Record
				return "Application Record";
			case 3: //NewsPhoto Record
				return "NewsPhoto Record";
			case 7: //PreObjectData Record
				return "PreObjectData Record";
			case 8: //ObjectData Record
				return "ObjectData Record";
			case 9: //PostObjectData Record
				return "PostObjectData Record";
			case 240: //FotoStation Record
				return "FotoStation Record";
			default:
				return "Unknown Record";
		}
	}
	
	public int getRecordNumber() {
		return recordNumber;
	}
	
	public int getSize() {
		return size;
	}
	
	public int getTag() {
		return tag;
	}
	
	public IPTCTag getTagEnum() {
		return tagEnum;
	}
	
	private String getTagName() {
		switch(IPTCRecord.fromRecordNumber(recordNumber)) {
			case APPLICATION:
				tagEnum = IPTCApplicationTag.fromTag(tag);
				break;
			case ENVELOP:
				tagEnum = IPTCEnvelopeTag.fromTag(tag);
				break;
			case FOTOSTATION:
				tagEnum = IPTCFotoStationTag.fromTag(tag);
				break;
			case NEWSPHOTO:
				tagEnum = IPTCNewsPhotoTag.fromTag(tag);
				break;
			case OBJECTDATA:
				tagEnum = IPTCObjectDataTag.fromTag(tag);
				break;
			case POST_OBJECTDATA:
				tagEnum = IPTCPostObjectDataTag.fromTag(tag);
				break;
			case PRE_OBJECTDATA:
				tagEnum = IPTCPreObjectDataTag.fromTag(tag);
				break;
			default:
				tagEnum = IPTCApplicationTag.UNKNOWN;
		}
		
		return tagEnum.getName();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(ArrayUtils.subArray(data, offset, size));
		result = prime * result + recordNumber;
		result = prime * result + tag;
		return result;
	}
	
	/**
	 * Write the current IPTCDataSet to the OutputStream
	 * 
	 * @param out OutputStream to write the IPTCDataSet
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		out.write(0x1c); // tag marker
		out.write(recordNumber);
		out.write(getTag());
		IOUtils.writeShortMM(out, size);
		out.write(data, offset, size);
	}
}