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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.util.Reader;

/**
 * PNG IEND chunk reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/29/2013
 */
public class IENDReader implements Reader {

	private Chunk chunk;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(IENDReader.class);
	
	public IENDReader(Chunk chunk) {
		if(chunk == null) throw new IllegalArgumentException("Input chunk is null");
		
		if (chunk.getChunkType() != ChunkType.IEND) {
			throw new IllegalArgumentException("Not a valid IEND chunk.");
		}
		
		this.chunk = chunk;
		
		try {
			read();
		} catch (IOException e) {
			throw new RuntimeException("IENDReader: error reading chunk");
		}
	}

	public void read() throws IOException {
		if(chunk.getData().length != 0) {
			LOGGER.warn("Warning: IEND data field is not empty!");
		}
	}
}
