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

import com.icafe4j.util.Reader;

/**
 * Special chunk reader for UnknownChunk.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/01/2013
 */
public class UnknownChunkReader implements Reader {

	private int chunkValue;
	private byte[] data;
	private Chunk chunk;
		
	public UnknownChunkReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("UnknownChunkReader: error reading chunk");
		}
	}
	
	public int getChunkValue() {
		return this.chunkValue;
	}
	
	public byte[] getData() {
		return data.clone();
	}
	
	public void read() throws IOException {       
   		if (chunk instanceof UnknownChunk) {
   			UnknownChunk unknownChunk = (UnknownChunk)chunk;
   			this.chunkValue = unknownChunk.getChunkValue();
   			this.data = unknownChunk.getData();
   		} else
   		    throw new IllegalArgumentException("Expect UnknownChunk.");
     }
}
