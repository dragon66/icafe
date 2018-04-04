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
 * ExifThumbnail.java
 *
 * Who   Date         Description
 * ====  ==========   ==================================================
 * WY    04Apr2018    Add getAsBufferedImage()
 * WY    27Apr2015    Make fields protected for subclass copy constructor
 * WY    10Apr2015    Changed to abstract class, added write()
 * WY    09Apr2015    Added setWriteQuality()
 */ 

package com.icafe4j.image.meta;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class Thumbnail {
	// Internal data type for thumbnail represented by a BufferedImage
	public static final int DATA_TYPE_KRawRGB = 0; // For ExifThumbnail and IRBThumbnail
	// Represented by a byte array of JPEG
	public static final int DATA_TYPE_KJpegRGB = 1; // For ExifThumbnail and IRBThumbnail
	// Represented by a byte array of uncompressed TIFF
	public static final int DATA_TYPE_TIFF = 2; // For ExifThumbnail only
	
	protected BufferedImage thumbnail;
	protected byte[] compressedThumbnail;
	
	protected int writeQuality = 100; // Default JPEG write quality
	
	protected int width;
	protected int height;
	
	// Default data type
	protected int dataType = Thumbnail.DATA_TYPE_KRawRGB;
	
	public Thumbnail() {}
	
	public Thumbnail(BufferedImage thumbnail) {
		setImage(thumbnail);
	}
	
	public Thumbnail(int width, int height, int dataType, byte[] compressedThumbnail) {
		setImage(width, height, dataType, compressedThumbnail);
	}
	
	public boolean containsImage() {
		return thumbnail != null || compressedThumbnail != null;
	}
	
	public BufferedImage getAsBufferedImage() {
		if(dataType == Thumbnail.DATA_TYPE_KJpegRGB || dataType == Thumbnail.DATA_TYPE_TIFF)
			try {
				return javax.imageio.ImageIO.read(new ByteArrayInputStream(getCompressedImage()));
			} catch (IOException e) {				
				throw new RuntimeException("Error decoding compressed thumbnail data to BufferedImage");
			}
		else if(dataType == Thumbnail.DATA_TYPE_KRawRGB) return getRawImage();
		else return null;
	}
	
	public byte[] getCompressedImage() {
		return compressedThumbnail;
	}
	
	public int getDataType() {
		return dataType;
	}
	
	public String getDataTypeAsString() {
		switch(dataType) {
			case 0:
				return "DATA_TYPE_KRawRGB";
			case 1:
				return "DATA_TYPE_KJpegRGB";
			case 2:
				return "DATA_TYPE_TIFF";
			default:
				return "DATA_TYPE_Unknown";
		}
	}
	
	public int getHeight() {
		return height;
	}
	
	public BufferedImage getRawImage() {
		return thumbnail;
	}
	
	public int getWidth() {
		return width;
	}
	
	public void setImage(BufferedImage thumbnail) {
		this.width = thumbnail.getWidth();
		this.height = thumbnail.getHeight();
		this.thumbnail = thumbnail;
		this.dataType = DATA_TYPE_KRawRGB;
	}
	
	public void setImage(int width, int height, int dataType, byte[] compressedThumbnail) {
		this.width = width;
		this.height = height;
		
		if(dataType == DATA_TYPE_KJpegRGB || dataType == DATA_TYPE_TIFF) {
			this.compressedThumbnail = compressedThumbnail;
			this.dataType = dataType;
		}
	}
	
	public void setWriteQuality(int quality) {
		this.writeQuality = quality;
	}
	
	public abstract void write(OutputStream os) throws IOException;
}