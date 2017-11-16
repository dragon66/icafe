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

package com.icafe4j.image.tiff;

import java.io.IOException;

import com.icafe4j.io.RandomAccessOutputStream;

public abstract class AbstractShortField extends TiffField<short[]> {

	public AbstractShortField(short tag, FieldType fieldType, short[] data) {
		super(tag, fieldType, data.length);
		this.data = data;	
	}
	
	public short[] getData() {
		return data.clone();
	}

	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
		if (data.length <= 2) {
			dataOffset = (int)os.getStreamPointer();
			short[] tmp = new short[2];
			System.arraycopy(data, 0, tmp, 0, data.length);
			for (short value : tmp)
				os.writeShort(value);
		} else {
			dataOffset = toOffset;
			os.writeInt(toOffset);
			os.seek(toOffset);
			
			for (short value : data)
				os.writeShort(value);
			
			toOffset += (data.length << 1);
		}
		return toOffset;
	}
}