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

import com.icafe4j.util.Builder;

/**
 * Base builder for PNG chunks.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/30/2012
 */
public abstract class ChunkBuilder implements Builder<Chunk> {

	private final ChunkType chunkType;
	
	public ChunkBuilder(ChunkType chunkType) {
		this.chunkType = chunkType;
	}
	
	protected ChunkType getChunkType() {
		return chunkType;
	}
	
	public final Chunk build() {
		byte[] data = buildData();
		
		long crc = Chunk.calculateCRC(chunkType.getValue(), data);
	    
		return new Chunk(chunkType, data.length, data, crc);
	}
	
	protected abstract byte[] buildData();
}