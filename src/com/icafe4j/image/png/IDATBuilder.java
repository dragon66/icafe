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
 * IDATBuilder.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    31Mar2016  Reversed changes to constructor on 27Mar2016
 * WY    27Mar2016  Changed constructor to set new compression level
 */

package com.icafe4j.image.png;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

import com.icafe4j.util.Builder;

/**
 * PNG IDAT chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/26/2013
 */
public class IDATBuilder extends ChunkBuilder implements Builder<Chunk> {

	private ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
	private Deflater deflater = new Deflater(5);
		
	public IDATBuilder() {
		super(ChunkType.IDAT);		
	}
	
	public IDATBuilder(int compressionLevel) {
		super(ChunkType.IDAT);
		deflater = new Deflater(compressionLevel);
	}
	
	public IDATBuilder data(byte[] data, int offset, int length) {
		// Caches the bytes
		bout.write(data, offset, length);
		
		return this;
	}
	
	public IDATBuilder data(byte[] data) {
		return data(data, 0, data.length);
	}

	@Override
	protected byte[] buildData() {
		// Compresses raw data
		deflater.setInput(bout.toByteArray());
		
		bout.reset();
		byte buffer[] = new byte[4096];
		
		if(finish)
			// This is to make sure we get all the input data compressed
			deflater.finish();
		
		while(!deflater.finished()) {
			int bytesCompressed = deflater.deflate(buffer);
			if(bytesCompressed <= 0) break;
			bout.write(buffer, 0, bytesCompressed);
		}		 
		
		byte temp[] = bout.toByteArray();
			
		bout.reset();
		
		return temp;
	}
	
	public void setFinish(boolean finish) {
		this.finish = finish;
	}
	
	private boolean finish;
}
