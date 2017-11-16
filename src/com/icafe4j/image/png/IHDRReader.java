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

package com.icafe4j.image.png;

import java.io.IOException;

import com.icafe4j.io.IOUtils;
import com.icafe4j.util.Reader;

/**
 * PNG IHDR chunk reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/25/2013
 */
public class IHDRReader implements Reader {

	private int width = 0;
	private int height = 0;
	private byte bitDepth = 0;
	private byte colorType = 0;
	private byte compressionMethod = 0;
	private byte filterMethod = 0;
	private byte interlaceMethod = 0;
	private Chunk chunk;
	
	public IHDRReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		if (chunk.getChunkType() != ChunkType.IHDR) {
			throw new IllegalArgumentException("Not a valid IHDR chunk.");
		}
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("IHDRReader: error reading chunk");
		}
	}
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public byte getBitDepth() { return bitDepth; }
	public byte getColorType() { return colorType; }
	public byte getCompressionMethod() { return compressionMethod; }
	public byte getFilterMethod() { return filterMethod; }
	public byte getInterlaceMethod() { return interlaceMethod; }

	public void read() throws IOException {	
		//
		byte[] data = chunk.getData();
		
		this.width = IOUtils.readIntMM(data, 0);
		this.height = IOUtils.readIntMM(data, 4);
		this.bitDepth = data[8];
		this.colorType = data[9];
		this.compressionMethod = data[10];
		this.filterMethod = data[11];
		this.interlaceMethod = data[12];
	}
}
