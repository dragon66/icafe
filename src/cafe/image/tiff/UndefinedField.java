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

/**
 * TIFF Attribute.UNDEFINED type field.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 02/05/2013
 */
public final class UndefinedField extends TiffField<byte[]> {

	public UndefinedField(short tag, byte[] data) {
		super(tag, FieldType.UNDEFINED, data.length);
		this.data = data;
	}
	
	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
	
		if (data.length <= 4) {
			dataOffset = (int)os.getStreamPointer();
			byte[] tmp = new byte[4];
			System.arraycopy(data, 0, tmp, 0, data.length);
			os.write(tmp);
		} else {
			dataOffset = toOffset;
			os.writeInt(toOffset);
			os.seek(toOffset);
			os.write(data);
			toOffset += data.length;
		}
		return toOffset;
	}
}