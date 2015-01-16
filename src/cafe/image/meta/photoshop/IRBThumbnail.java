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

/** 
 * Photoshop Image Resource Block thumbnail wrapper.
 *
 * @author Wen Yu, yuwen_66@yahoo.com 
 * @version 1.0 01/10/2015   
 */
public class IRBThumbnail {
	public static final int DATA_TYPE_KRawRGB = 0;
	public static final int DATA_TYPE_KJpegRGB = 1;
	
	private int dataType;
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
	
	private BufferedImage thumbnail;
	private byte[] compressedThumbnail;
	
	private byte[] data;	

	public IRBThumbnail(ImageResourceID id, int dataType, int width, int height, int paddedRowBytes, int totalSize, int compressedSize, int bitsPerPixel, int numOfPlanes, byte[] data) {
		this.id = id;
		this.dataType = dataType;
		this.width = width;
		this.height = height;
		this.paddedRowBytes = paddedRowBytes;
		this.totalSize = totalSize;
		this.compressedSize = compressedSize;
		this.bitsPerPixel = bitsPerPixel;
		this.numOfPlanes = numOfPlanes;
		this.data = data;
		setImage();
	}
	
	public int getBitsPerPixel() {
		return bitsPerPixel;
	}
	
	public byte[] getCompressedImage() {
		return compressedThumbnail;
	}
	
	public int getCompressedSize() {
		return compressedSize;
	}
	
	public int getDataType() {
		return dataType;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getNumOfPlanes() {
		return numOfPlanes;
	}
	
	public int getPaddedRowBytes() {
		return paddedRowBytes;
	}
	
	public BufferedImage getRawImage() {
		return thumbnail;
	}
	
	public ImageResourceID getResouceID() {
		return id;
	}
	
	public int getTotalSize() {
		return totalSize;		
	}
	
	public int getWidth() {
		return width;
	}
	
	private void setImage() {
		// JFIF data in RGB format. For resource ID 1033 (0x0409) the data is in BGR format.
		if(dataType == DATA_TYPE_KJpegRGB) {
			// Note: Not sure whether or not this will create wrong color JPEG
			// if it's written by Photoshop 4.0!
			compressedThumbnail = data;
		} else if(dataType == DATA_TYPE_KRawRGB) {
			// kRawRGB - NOT tested yet!
			//Create a BufferedImage
			DataBuffer db = new DataBufferByte(data, totalSize);
			int[] off = {0, 1, 2};//RGB band offset, we have 3 bands
			if(id == ImageResourceID.THUMBNAIL_RESOURCE_PS4)
				off = new int[]{2, 1, 0}; // RGB band offset for BGR for photoshop4.0 BGR format
			int numOfBands = 3;
			int trans = Transparency.OPAQUE;
				
			WritableRaster raster = Raster.createInterleavedRaster(db, width, height, paddedRowBytes, numOfBands, off, null);
			ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, trans, DataBuffer.TYPE_BYTE);
	   		thumbnail = new BufferedImage(cm, raster, false, null);		
		}
	}
 }