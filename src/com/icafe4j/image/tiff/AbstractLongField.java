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

public abstract class AbstractLongField extends TiffField<int[]> {

	public AbstractLongField(short tag, FieldType fieldType, int[] data) {
		super(tag, fieldType, data.length);	
		this.data = data;
	}
	
	public int[] getData() {
		return data.clone();
	}
	
	public int[] getDataAsLong() {
		return getData();
	}
	
	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
		
		if (data.length == 1) {
			dataOffset = (int)os.getStreamPointer();
			os.writeInt(data[0]);
		} else {
			dataOffset = toOffset;
			os.writeInt(toOffset);
			os.seek(toOffset);
			
			for (int value : data)
				os.writeInt(value);
			
			toOffset += (data.length << 2);
		}
		return toOffset;
	}
}