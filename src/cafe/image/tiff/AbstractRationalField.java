/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.tiff;

import java.io.IOException;

import cafe.io.RandomAccessOutputStream;

public abstract class AbstractRationalField extends TiffField<int[]> {

	public AbstractRationalField(short tag, FieldType fieldType, int[] data) {
		super(tag, fieldType, data.length>>1);
		this.data = data;
	}
	
	public int[] getDataAsLong() {
		return data;
	}

	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
		//
		dataOffset = toOffset;
		os.writeInt(toOffset);
		os.seek(toOffset);
		
		for (int value : data)
			os.writeInt(value);
		
		toOffset += (data.length << 2);
		
		return toOffset;
	}
}