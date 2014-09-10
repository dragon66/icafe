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

import java.io.IOException;
import cafe.util.Reader;

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
		
	public UnknownChunkReader(Chunk chunk) throws IOException {
		this.chunk = chunk;
		read();
	}
	
	public int getChunkValue() {
		return this.chunkValue;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void read() throws IOException
    {       
   		if (chunk instanceof UnknownChunk) {
   			UnknownChunk unknownChunk = (UnknownChunk)chunk;
   			this.chunkValue = unknownChunk.getChunkValue();
   			this.data = unknownChunk.getData();
   		} else
   		    throw new IllegalArgumentException("Expect UnknownChunk.");
     }
}
