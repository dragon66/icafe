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
 * WY    05Jul2015  Added write support
 * WY    05Jul2015  Initial creation
 */

package cafe.image.meta.png;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.image.png.Chunk;
import cafe.image.png.ChunkType;
import cafe.image.png.TextBuilder;
import cafe.image.png.TextReader;

public class TextualChunk extends Metadata {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TextualChunk.class);
		
	private static MetadataType validate(ChunkType chunkType) {
		if(chunkType == null) throw new IllegalArgumentException("ChunkType is null");
		if(chunkType == ChunkType.TEXT || chunkType == ChunkType.ITXT || chunkType == ChunkType.ZTXT)
			return MetadataType.PNG_TEXTUAL;
		throw new IllegalArgumentException(
				"Input ChunkType is not textual chunk! Should be one the following: \n"
				+ "'ChunkType.TEXT, ChunkType.ITXT, or ChunkType.ZTXT'");
	}

	private ChunkType chunkType;
	private Chunk chunk;
	private String keyword;	
	private String text;
	
	public TextualChunk(Chunk chunk) {
		super(validate(chunk.getChunkType()), chunk.getData());
		this.chunkType = chunk.getChunkType();
		this.chunk = chunk;
	}
	
	public TextualChunk(ChunkType chunkType, String keyword, String text) {
		super(validate(chunkType), null);
		if(keyword == null || text == null)
			throw new IllegalArgumentException("keyword or text is null");
		this.chunkType = chunkType;
		this.keyword = keyword.trim().replaceAll("\\s+", " ");
		this.text = text;
		isDataRead = true;
	}
	
	public Chunk getChunk() {
		if(chunk == null)
			chunk = new TextBuilder(chunkType).keyword(keyword).text(text).build();
	
		return chunk;
	}

	public ChunkType getChunkType() {
		return chunkType;
	}
	
	public byte[] getData() {
		return getChunk().getData();
	}
	
	public String getKeyword() {
		ensureDataRead();
		return keyword;
	}
	
	public String getText() {
		ensureDataRead();
		return text;		
	}
	
	@Override
	public void read() throws IOException {
		if(!isDataRead) {
			TextReader reader = new TextReader(chunk);
			this.keyword = reader.getKeyword();
			this.text = reader.getText();
			isDataRead = true;
		}
	}
	
	@Override
	public void showMetadata() {
		LOGGER.info("PNG textual chunk starts =>");
		LOGGER.info("Key word: {}", getKeyword());
		LOGGER.info("Text: {}", getKeyword().equals("XML:com.adobe.xmp")?"<XMP data not shown>":getText());
		LOGGER.info("PNG textual chunk ends <=");
	}

	public void write(OutputStream os) throws IOException {
		getChunk().write(os);
	}
}