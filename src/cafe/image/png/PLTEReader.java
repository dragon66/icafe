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
 * PNG PLTE chunk reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/26/2013
 */
public class PLTEReader implements Reader {

	private byte[] redMap;
	private byte[] greenMap;
	private byte[] blueMap;
	private Chunk chunk;
	
	public PLTEReader(Chunk chunk) throws IOException {
		//
		if (chunk.getChunkType() != ChunkType.PLTE) {
			throw new IllegalArgumentException("Not a valid PLTE chunk.");
		}
		
		this.chunk = chunk;
		read();
	}
	
	public byte[] getRedMap() { return redMap; }
	public byte[] getGreenMap() { return greenMap; }
	public byte[] getBlueMap() { return blueMap; }
	
	@Override
	public void read() throws IOException {	
		
		byte[] colorMap = chunk.getData();
		int mapLen = colorMap.length;
		
		if ((mapLen % 3) != 0) {
			throw new IllegalArgumentException("Invalid colorMap length: " + mapLen);
		}
		
		redMap = new byte[mapLen/3];
		greenMap = new byte[mapLen/3];
		blueMap = new byte[mapLen/3];
		
		for (int i = mapLen - 1, j = redMap.length - 1; j >= 0; j--) {
			blueMap[j]  = colorMap[i--];
			greenMap[j] = colorMap[i--];
			redMap[j] 	= colorMap[i--];			
		}		
	}
}
