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
 * JFIFThumbnail.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    14Jul2015  Added copy constructor
 * WY    12Jul2015  Initial creation
 */

package com.icafe4j.image.meta.jpeg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import com.icafe4j.image.meta.Thumbnail;

public class JFIFThumbnail extends Thumbnail {

	public JFIFThumbnail(BufferedImage thumbnail) {
		super(thumbnail);
	}
	
	public JFIFThumbnail(JFIFThumbnail other) { // Copy constructor
		this.dataType = other.dataType;
		this.height = other.height;
		this.width = other.width;
		this.thumbnail = other.thumbnail;
		this.compressedThumbnail = other.compressedThumbnail;
	}

	@Override
	public void write(OutputStream os) throws IOException {
		BufferedImage thumbnail = getRawImage();
		if(thumbnail == null) throw new IllegalArgumentException("Expected raw data thumbnail does not exist!");
		int[] rgbs = thumbnail.getRGB(0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null, 0, thumbnail.getWidth());
		for(int rgb : rgbs) {
			os.write(rgb >> 16); // Red
			os.write(rgb >> 8); // Green
			os.write(rgb); // Blue
		}
	}
}