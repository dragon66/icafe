/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * TextualChunk.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    09Jul2015  Rewrote to work with multiple textual chunks
 * WY    05Jul2015  Added write support
 * WY    05Jul2015  Initial creation
 */

package com.icafe4j.image.meta.png;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.png.Chunk;
import com.icafe4j.image.png.ChunkType;
import com.icafe4j.image.png.TextBuilder;
import com.icafe4j.image.png.TextReader;

public class TextualChunks extends Metadata {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TextualChunks.class);
		
	private ChunkType chunkType;
	private Collection<Chunk> chunks;
	private Map<String, String> keyValMap;
	
	public TextualChunks() {
		this(null);
		chunks = new ArrayList<Chunk>();
	}
		
	public TextualChunks(Collection<Chunk> chunks) {
		super(MetadataType.PNG_TEXTUAL, null);
		this.chunks = chunks;
	}
	
	public TextualChunks(ChunkType chunkType, Map<String, String> keyValMap) {
		super(MetadataType.PNG_TEXTUAL, null);
		this.chunkType = chunkType;
		this.keyValMap = keyValMap;
		isDataRead = true;
	}
	
	public Map<String, String> getKeyValMap() {
		ensureDataRead();
		return Collections.unmodifiableMap(keyValMap);
	}
	
	public void addChunk(Chunk chunk) {
		if(chunks != null)
			chunks.add(chunk);
		else
			throw new IllegalStateException("Adding chunks is not allowed");
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			TextReader reader = new TextReader();
			keyValMap = new HashMap<String, String>();
			for(Chunk chunk : chunks) {
				reader.setInput(chunk);
				keyValMap.put(reader.getKeyword(), reader.getText());	
			}
			isDataRead = true;
		}
	}
	
	@Override
	public void showMetadata() {
		ensureDataRead();
		
		LOGGER.info("PNG textual chunk starts =>");
		
		for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
		    LOGGER.info("{}: {}", entry.getKey(), entry.getValue());
		}
		
		LOGGER.info("PNG textual chunk ends <=");
	}

	public void write(OutputStream os) throws IOException {
		if(chunks != null) {
			for(Chunk chunk : chunks) chunk.write(os);
		} else if(keyValMap != null){
			TextBuilder builder = new TextBuilder(chunkType);
			for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
			    Chunk chunk = builder.keyword(entry.getKey()).text(entry.getValue()).build();
			    chunk.write(os);
			}
		}
	}
}