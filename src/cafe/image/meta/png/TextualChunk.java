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
 * WY    05Jul2015  Intial creation
 */

package cafe.image.meta.png;

import java.io.IOException;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.image.png.Chunk;
import cafe.image.png.ChunkType;

public class TextualChunk extends Metadata {

	private ChunkType chunkType;
	private TextualChunkReader reader;
	
	public TextualChunk(Chunk chunk) {
		super(validate(chunk.getChunkType()), chunk.getData());
		this.chunkType = chunk.getChunkType();
		try {
			this.reader = new TextualChunkReader(chunk);
		} catch (IOException e) {
			throw new RuntimeException("Error: TextualChunkReader failed to read textual chunk");
		}
	}
	
	public String getKeyword() {
		if(reader != null)
			return reader.getKeyword();
		
		return "";
	}
	
	public String getText() {
		if(reader != null)
			return reader.getText();
		
		return "";		
	}
	
	private static MetadataType validate(ChunkType chunkType) {
		if(chunkType == ChunkType.TEXT || chunkType == ChunkType.ITXT || chunkType == ChunkType.ZTXT)
			return MetadataType.PNG_TEXTUAL;
		throw new IllegalArgumentException(
				"Input ChunkType is not textual! Should be one the following: \n"
				+ "'ChunkType.TEXT, ChunkType.ITXT, or ChunkType.ZTXT'");
	}
	
	public ChunkType getChunkType() {
		return chunkType;
	}

	@Override
	public TextualChunkReader getReader() {
		return reader;
	}
}