/**
 * COPYRIGHT (C) 2014-2019 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.tiff;

import java.io.IOException;

import com.icafe4j.io.RandomAccessOutputStream;
import com.icafe4j.string.StringUtils;

/**
 * IFD (Image File Directory) field.
 * <p>
 * We could have used a TiffTag enum as the first parameter of the constructor, but this
 * will not work with unknown tags of tag type TiffTag.UNKNOWN. In that case, we cannot
 * use the tag values to sort the fields or as keys for a hash map as used by {@link IFD}.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/04/2013
 */
public abstract class TiffField<T> implements Comparable<TiffField<?>> {

	private final short tag;
	private final FieldType fieldType;
	private final int length;
	protected T data;
	protected static final int MAX_STRING_REPR_LEN = 10; // Default length for string representation
	
	protected int dataOffset;
	
	public TiffField(short tag, FieldType fieldType, int length) {
		this.tag = tag;
		this.fieldType = fieldType;
		this.length = length;
	}
	
	public int compareTo(TiffField<?> that) {
		return (this.tag&0xffff) - (that.tag&0xffff);
    }
	
	public T getData() {
		return data;
	}
	
	/** Return an integer array representing TIFF long field */
	public int[] getDataAsLong() { 
		throw new UnsupportedOperationException("getDataAsLong() method is only supported by"
				+ " short, long, and rational data types");
	}
	
	/**
	 * @return a String representation of the field data
	 */
	public abstract String getDataAsString();
	
	public int getLength() {
		return length;
	}
	
	/**
	 * Used to update field data when necessary.
	 * <p>
	 * This method should be called only after the field has been written to the underlying RandomOutputStream.
	 * 
	 * @return the stream position where actual data starts to write
	 */
	public int getDataOffset() {
		return dataOffset;
	}
	
	public short getTag() {
		return tag;
	}
	
	public FieldType getType() {
		return this.fieldType;
	}

	@Override public String toString() {
		short tag = this.getTag();
		Tag tagEnum = TiffTag.fromShort(tag);
		
		if (tagEnum != TiffTag.UNKNOWN)
			return tagEnum.toString();
		return tagEnum.toString() + " [TiffTag value: "+ StringUtils.shortToHexStringMM(tag) + "]";
	}
	
	public final int write(RandomAccessOutputStream os, int toOffset) throws IOException {
		// Write the header first
		os.writeShort(this.tag);
		os.writeShort(getType().getValue());
		os.writeInt(getLength());
		// Then the actual data
		return writeData(os, toOffset);
	}
	
	protected abstract int writeData(RandomAccessOutputStream os, int toOffset) throws IOException;
}
