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

import java.util.zip.Inflater;

import com.icafe4j.image.compression.ImageDecoder;

/** A wrapper class for Java deflate decoding
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/09/2014
 */
public class DeflateDecoder implements ImageDecoder {
	// Declare variables
	private Inflater inflater;
		
	public DeflateDecoder(byte[] input) {
		inflater = new Inflater();
		inflater.setInput(input);
	}
	
	public DeflateDecoder() {
		inflater = new Inflater();
	}
	
	public void setInput(byte[] input) {
		setInput(input, 0, input.length);
	}
	
	public void setInput(byte[] input, int start, int len) {
		inflater.reset(); // Must reset to work with new input
		inflater.setInput(input, start, len);
	}
	
	public int decode(byte[] pixels, int start, int len) throws Exception {
		int totalBytes = 0;
		while(totalBytes < len && !inflater.needsInput()) {
			int bytesInflated = inflater.inflate(pixels, start, len);
			start += bytesInflated;
			totalBytes += bytesInflated;
		}
		return totalBytes;
	}
}