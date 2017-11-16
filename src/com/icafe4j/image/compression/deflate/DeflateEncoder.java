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

package com.icafe4j.image.compression.deflate;

import java.io.OutputStream;
import java.util.zip.Deflater;

import com.icafe4j.image.compression.ImageEncoder;
import com.icafe4j.util.Updatable;

/** A wrapper class for Java deflate encoding
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/10/2014
 */
public class DeflateEncoder implements ImageEncoder {
	// Declare variables
	private byte[] buffer;
	private Deflater deflater;
	private OutputStream os;
	private Updatable<Integer> writer;
	private boolean isTIFF;
	
	public DeflateEncoder(OutputStream os, int bufferSize, int compressLevel) {
		this.os = os;
		this.buffer = new byte[bufferSize];
		this.deflater = new Deflater(compressLevel);
	}
	
	public DeflateEncoder(OutputStream os, int bufferSize, int compressLevel, Updatable<Integer> writer) {
		this(os, bufferSize, compressLevel);
		this.writer = writer;
		this.isTIFF = true;
	}
	
	public void encode(byte[] pixels, int start, int len) throws Exception {
		// Set input
		deflater.setInput(pixels, start, len);
		// This is the magic here
		deflater.finish();
		while(!deflater.finished()) {
			int temp = deflater.deflate(buffer);
			if(temp <= 0) break;
			os.write(buffer, 0, temp);			
		}
	}

	public void finish() throws Exception {
		if(isTIFF && writer != null)
			writer.update(deflater.getTotalOut());
	}
	
	public int getCompressedDataLen() {
		return deflater.getTotalOut();
	}

	public void initialize() throws Exception {
		deflater.reset();
	}
}