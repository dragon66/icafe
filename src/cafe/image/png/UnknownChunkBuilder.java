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

import cafe.util.Builder;

/**
 * Special chunk builder for UnknownChunk.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/31/2012
 */
public class UnknownChunkBuilder implements Builder<Chunk> {

	private int chunkType;
	private byte[] data;
	
	public UnknownChunkBuilder type(int type) {
		this.chunkType = type;
		return this;
	}
	
	public UnknownChunkBuilder data(byte[] data) {
		this.data = data;
		return this;
	}
	
	public UnknownChunkBuilder() {}

	public Chunk build() {
		long crc = Chunk.calculateCRC(chunkType, data);
	    
	    return new UnknownChunk(data.length, chunkType, data, crc);	
	}
}
