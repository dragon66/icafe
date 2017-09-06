/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * DuckyDataSet.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package com.icafe4j.image.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.io.IOUtils;
import com.icafe4j.util.ArrayUtils;

public class DuckyDataSet {
	private int tag;
	private int size;
	private byte[] data;
	private int offset;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(DuckyDataSet.class);
	
	public DuckyDataSet(int tag, byte[] data) {
		this(tag, data.length, data, 0);
	}
	
	public DuckyDataSet(int tag, int size, byte[] data, int offset) {
		this.tag = tag;
		this.size = size;
		this.data = data;
		this.offset = offset;
	}
	
	public byte[] getData() {
		return ArrayUtils.subArray(data, offset, size);
	}
	
	public int getSize() {
		return size;
	}
	
	public int getTag() {
		return tag;
	}
	
	public void print() {
		if(size < 4) {
			LOGGER.warn("Data set size {} is too small, should >= 4", size);
			return;
		}
			
		DuckyTag etag = DuckyTag.fromTag(tag);
		
		if(etag == DuckyTag.UNKNOWN) {
			LOGGER.info("Unknown tag: {}", tag);
		} else if(etag == DuckyTag.QUALITY) {
			LOGGER.info(etag + ": {}", IOUtils.readUnsignedIntMM(data, offset));
		} else {
			String value = "";
			try {
				// We need to skip 4 unknown bytes for each string entry!!!
				value = new String(data, offset + 4, size - 4, "UTF-16BE");
			} catch (UnsupportedEncodingException e) {
				LOGGER.error("UnsupportedEncoding \"UTF-16BE\"");
			}
			LOGGER.info(etag + ": {}", value);
		}
	}
	
	public void write(OutputStream out) throws IOException {
		IOUtils.writeShortMM(out, tag);
		IOUtils.writeShortMM(out, size);
		out.write(data, offset, size);
	}
}