/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * IRBThumbnail.java
 *
 * Who   Date       Description
 * ====  =========  ===============================================================
 * WY    10Jan2015  Initial creation for IRBReader to encapsulate IRB thumbnail
 */

package cafe.image.meta.photoshop;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import cafe.image.ImageIO;
import cafe.image.ImageType;
import cafe.image.writer.ImageWriter;

/** 
 * Photoshop Image Resource Block thumbnail wrapper.
 *
 * @author Wen Yu, yuwen_66@yahoo.com 
 * @version 1.0 01/10/2015   
 */
public class IRBThumbnail {
	public static final int FORMAT_KRawRGB = 0;
	public static final int FORMAT_KJpegRGB = 1;
	
	private int format;
	private int width;
	private int height;
	//Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
	private int paddedRowBytes;
	// Total size = widthbytes * height * planes
	private int totalSize;
	// Size after compression. Used for consistency check.
	private int compressedSize;
	// Bits per pixel. = 24
	private int bitsPerPixel;
	// Number of planes. = 1
	private int numOfPlanes;
	private ImageResourceID id;
	
	byte[] data;	

	public IRBThumbnail(ImageResourceID id, int format, int width, int height, int paddedRowBytes, int totalSize, int compressedSize, int bitsPerPixel, int numOfPlanes, byte[] data) {
		this.id = id;
		this.format = format;
		this.width = width;
		this.height = height;
		this.paddedRowBytes = paddedRowBytes;
		this.totalSize = totalSize;
		this.compressedSize = compressedSize;
		this.bitsPerPixel = bitsPerPixel;
		this.numOfPlanes = numOfPlanes;
		this.data = data;
	}
	
	public int getFormat() {
		return format;
	}
	
	public ImageResourceID getResouceID() {
		return id;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getPaddedRowBytes() {
		return paddedRowBytes;
	}
	
	public int getTotalSize() {
		return totalSize;		
	}
	
	public int getCompressedSize() {
		return compressedSize;
	}
	
	public int getBitsPerPixel() {
		return bitsPerPixel;
	}
	
	public int getNumOfPlanes() {
		return numOfPlanes;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void write(OutputStream out) throws IOException {
		// Write the thumbnail to outputStream
		int thumbnailFormat = getFormat();
		int totalSize = getTotalSize();
		int sizeAfterCompression = getCompressedSize();
		int width = getWidth();
		int height = getHeight();
		int widthBytes = getPaddedRowBytes();
		ImageResourceID eId = getResouceID();
		byte[] thumbnailData = getData();
		
		// JFIF data in RGB format. For resource ID 1033 (0x0409) the data is in BGR format.
		if(thumbnailFormat == FORMAT_KJpegRGB) {
			// Note: Not sure whether or not this will create wrong color JPEG
			// if it's written by Photoshop 4.0!
			out.write(thumbnailData, 0, sizeAfterCompression);
		} else if(thumbnailFormat == FORMAT_KRawRGB) {
			// kRawRGB - NOT tested yet!
			//Create a BufferedImage
			DataBuffer db = new DataBufferByte(thumbnailData, totalSize);
			int[] off = {0, 1, 2};//RGB band offset, we have 3 bands
			if(eId == ImageResourceID.THUMBNAIL_RESOURCE_PS4)
				off = new int[]{2, 1, 0}; // RGB band offset for BGR for photoshop4.0 BGR format
			int numOfBands = 3;
			int trans = Transparency.OPAQUE;
				
			WritableRaster raster = Raster.createInterleavedRaster(db, width, height, widthBytes, numOfBands, off, null);
			ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, trans, DataBuffer.TYPE_BYTE);
	   		BufferedImage bi = new BufferedImage(cm, raster, false, null);
			// Create a new writer to write the image
			ImageWriter writer = ImageIO.getWriter(ImageType.JPG);
			try {
				writer.write(bi, out);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void write(String pathToThumbnail) throws IOException {
		OutputStream fout = new FileOutputStream(pathToThumbnail + ".jpg");
    	write(fout);
    	fout.close();
	}
 }