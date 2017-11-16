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
 * PNG tRNS chunk reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/03/2013
 */
public class TRNSReader implements Reader {

	private byte[] alpha = new byte[0];
	private Chunk chunk;
	
	public TRNSReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		if (chunk.getChunkType() != ChunkType.TRNS) {
			throw new IllegalArgumentException("Not a valid TRNS chunk.");
		}
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("TRNSReader: error reading chunk");
		}
	}
	
	public byte[] getAlpha() {
		return alpha.clone(); // Defensive copy
	}

	public void read() throws IOException {
		this.alpha = chunk.getData();
	}
}
