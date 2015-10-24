/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.meta.image;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;

public class Comment extends Metadata {
	private String comment;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Comment.class);
	
	public Comment(byte[] data) {
		super(MetadataType.COMMENT, data);
		ensureDataRead();
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			try {
				this.comment = new String(data, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			isDataRead = true;
		}
	}
	
	public String getComment() {
		return comment;
	}
	
	public void showMetadata() {
		ensureDataRead();
		LOGGER.info("Comment: {}", comment);
	}
}