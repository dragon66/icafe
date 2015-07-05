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
 * TextualChunkReader.java
 *
 * Who   Date       Description
 * ====  =========  ==================================================
 * WY    05Jul2015  Initial creation
 */

package cafe.image.meta.png;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cafe.image.meta.MetadataReader;
import cafe.image.png.Chunk;
import cafe.image.png.TextReader;

public class TextualChunkReader extends TextReader implements MetadataReader {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TextualChunkReader.class);

	public TextualChunkReader(Chunk chunk) throws IOException {
		super(chunk);
	}

	@Override
	public void showMetadata() {
		LOGGER.info("PNG textual chunk starts =>");
		LOGGER.info("Key word: {}", getKeyword());
		LOGGER.info("Text: {}", getKeyword().equals("XML:com.adobe.xmp")?"":getText());
		LOGGER.info("PNG textual chunk ends <=");
	}

	@Override
	public boolean isDataLoaded() {
		return true;
	}
}