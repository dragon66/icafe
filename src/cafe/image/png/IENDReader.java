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

import cafe.util.Reader;

/**
 * PNG IEND chunk reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/29/2013
 */
public class IENDReader implements Reader {

	private Chunk chunk;
	
	public IENDReader(Chunk chunk) {
		//
		if (chunk.getChunkType() != ChunkType.IEND) {
			throw new IllegalArgumentException("Not a valid IEND chunk.");
		}
		
		this.chunk = chunk;
	}

	@Override
	public void read() throws IOException {
		if(chunk.getData().length != 0) {
			System.out.println("Warning: IEND data field is not empty!");
		}
	}
}
