/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.image;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataReader;
import cafe.image.meta.MetadataType;

public class Comment extends Metadata {
	private String comment;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Comment.class);
	
	public Comment(byte[] data) {
		super(MetadataType.COMMENT, data);
		try {
			this.comment = new String(data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public String getComment() {
		return comment;
	}
	
	public void showMetadata() {
		LOGGER.info("Comment: {}", comment);
	}

	@Override
	public MetadataReader getReader() {
		return null;
	}
}