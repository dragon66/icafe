/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.png;

import java.io.IOException;
import java.io.OutputStream;

import cafe.io.IOUtils;

/**
 * Special chunk to handle PNG ChunkType.UNKNOWN.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/30/2012
 */
public class UnknownChunk extends Chunk {

	private final int chunkValue;
	
	public UnknownChunk(long length, int chunkValue, byte[] data, long crc) {
		super(ChunkType.UNKNOWN, length, data, crc);
		this.chunkValue = chunkValue;
	}
	
	public int getChunkValue(){
		return chunkValue;
	}
	
	@Override public boolean isValidCRC() {				 
		return (calculateCRC(chunkValue, getData()) == getCRC());
	}
	
	@Override public void write(OutputStream os) throws IOException{
		IOUtils.writeIntMM(os, (int)getLength());
		IOUtils.writeIntMM(os, this.chunkValue);
		IOUtils.write(os, getData());
		IOUtils.writeIntMM(os, (int)getCRC());
	}
	
	@Override public String toString() {
		return super.toString() + "[Chunk type: " + getChunkType() + ", value: 0x"+ Integer.toHexString(chunkValue)+"]";
	}	
}