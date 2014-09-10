/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.png;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import cafe.io.IOUtils;
import cafe.util.Reader;

/**
 * PNG IDAT chunk reader
 * <p>
 * All the IDAT chunks must be merged together before using this reader, as
 * per PNG specification, the compressed data stream is the concatenation of
 * the contents of all the IDAT chunks.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/26/2013
 */
public class IDATReader implements Reader {

	private byte[] rawData;
	private ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(1024);
	
	public IDATReader() {}
	
	public IDATReader addChunk(Chunk chunk) throws IOException {
		//
		if (chunk.getChunkType() != ChunkType.IDAT) {
			throw new IllegalArgumentException("Not a valid IDAT chunk.");
		}		
		
		byteOutput.write(chunk.getData());
		return this;
	}
	
	public byte[] getData() throws IOException { 
		read();
		return rawData;
	}

	@Override
	public void read() throws IOException {		
		// Inflate compressed data
		BufferedInputStream bin = new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(byteOutput.toByteArray())));
		this.rawData = IOUtils.inputStreamToByteArray(bin); 
	}
}
