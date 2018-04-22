/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.png.Chunk;
import com.icafe4j.image.png.ChunkType;
import com.icafe4j.image.png.TextReader;

public class TextualChunks extends Metadata {
	/* This queue is used to keep track of the unread chunks
	 * After it's being read, all of it's elements will be moved
	 * to chunks list
	 */
	private Queue<Chunk> queue;
	// We keep chunks and keyValMap in sync
	private List<Chunk> chunks;
	private Map<String, String> keyValMap;
	
	public TextualChunks() {
		super(MetadataType.PNG_TEXTUAL);
		this.queue = new LinkedList<Chunk>();
		this.chunks = new ArrayList<Chunk>();
		this.keyValMap = new HashMap<String, String>();		
	}
		
	public TextualChunks(Collection<Chunk> chunks) {
		super(MetadataType.PNG_TEXTUAL);
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
	
	public Iterator<MetadataEntry> iterator() {
		ensureDataRead();
		List<MetadataEntry> entries = new ArrayList<MetadataEntry>();
		MetadataEntry root = new MetadataEntry("PNG", "Textual Chunks", true);
		
		for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
		    root.addEntry(new MetadataEntry(entry.getKey(), entry.getValue()));
		}
		
		entries.add(root);
		
		return Collections.unmodifiableCollection(entries).iterator();
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
				String key = reader.getKeyword();
				String text = reader.getText();
				String oldText = keyValMap.get(key);
				keyValMap.put(key, (oldText == null)? text: oldText + "; " + text);
				chunks.add(chunk);
			}
			queue.clear();
		}
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