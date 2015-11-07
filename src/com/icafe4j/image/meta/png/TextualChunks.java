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
 * WY    04Nov2015  Added chunk type check
 * WY    09Jul2015  Rewrote to work with multiple textual chunks
 * WY    05Jul2015  Added write support
 * WY    05Jul2015  Initial creation
 */

package com.icafe4j.image.meta.png;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.png.Chunk;
import com.icafe4j.image.png.ChunkType;
import com.icafe4j.image.png.TextReader;

public class TextualChunks extends Metadata {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TextualChunks.class);
	
	/* This queue is used to keep track of the unread chunks
	 * After it's being read, all of it's elements will be moved
	 * to chunks list
	 */
	private Queue<Chunk> queue;
	// We keep chunks and keyValMap in sync
	private List<Chunk> chunks;
	private Map<String, String> keyValMap;
	
	public TextualChunks() {
		super(MetadataType.PNG_TEXTUAL, null);
		this.queue = new LinkedList<Chunk>();
		this.chunks = new ArrayList<Chunk>();
		this.keyValMap = new HashMap<String, String>();		
	}
		
	public TextualChunks(Collection<Chunk> chunks) {
		super(MetadataType.PNG_TEXTUAL, null);
		validateChunks(chunks);
		this.queue = new LinkedList<Chunk>(chunks);
		this.chunks = new ArrayList<Chunk>();
		this.keyValMap = new HashMap<String, String>();
	}
	
	public List<Chunk> getChunks() {
		ArrayList<Chunk> chunkList = new ArrayList<Chunk>(chunks);
		chunkList.addAll(queue);		
		return chunkList;
	}
	
	public Map<String, String> getKeyValMap() {
		ensureDataRead();
		return Collections.unmodifiableMap(keyValMap);
	}
	
	public void addChunk(Chunk chunk) {
		validateChunkType(chunk.getChunkType());
		queue.offer(chunk);
	}
	
	public void read() throws IOException {
		if(queue.size() > 0) {
			TextReader reader = new TextReader();
			for(Chunk chunk : queue) {
				reader.setInput(chunk);
				keyValMap.put(reader.getKeyword(), reader.getText());
				chunks.add(chunk);
			}
			queue.clear();
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
	
	private static void validateChunks(Collection<Chunk> chunks) {
		for(Chunk chunk : chunks)
			validateChunkType(chunk.getChunkType());
	}
	
	private static void validateChunkType(ChunkType chunkType) {
		if((chunkType != ChunkType.TEXT) && (chunkType != ChunkType.ITXT) 
				&& (chunkType != ChunkType.ZTXT))
			throw new IllegalArgumentException("Expect Textual chunk!");
	}
}