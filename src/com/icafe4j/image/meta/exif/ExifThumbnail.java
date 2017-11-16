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
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    27Apr2015  Added copy constructor
 * WY    10Apr2015  Added new constructor, changed write()
 * WY    09Apr2015  Moved setWriteQuality() to super class
 * WY    14Jan2015  initial creation
 */

package com.icafe4j.image.meta.exif;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.icafe4j.image.ImageIO;
import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.meta.Thumbnail;
import com.icafe4j.image.options.JPEGOptions;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.LongField;
import com.icafe4j.image.tiff.RationalField;
import com.icafe4j.image.tiff.ShortField;
import com.icafe4j.image.tiff.TIFFTweaker;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffFieldEnum;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.image.writer.ImageWriter;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.MemoryCacheRandomAccessOutputStream;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.io.RandomAccessOutputStream;

/**
 * Encapsulates image EXIF thumbnail metadata
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/08/2014
 */
public class ExifThumbnail extends Thumbnail {
	// Comprised of an IFD and an associated image
	// Create thumbnail IFD (IFD1 in the case of JPEG EXIF segment)
	private IFD thumbnailIFD = new IFD(); 
	
	public ExifThumbnail() { }
	
	public ExifThumbnail(BufferedImage thumbnail) {
		super(thumbnail);
	}
	
	public ExifThumbnail(ExifThumbnail other) { // Copy constructor
		this.dataType = other.dataType;
		this.height = other.height;
		this.width = other.width;
		this.thumbnail = other.thumbnail;
		this.compressedThumbnail = other.compressedThumbnail;
		this.thumbnailIFD = other.thumbnailIFD;
	}
	
	public ExifThumbnail(int width, int height, int dataType, byte[] compressedThumbnail) {
		super(width, height, dataType, compressedThumbnail);
	}
	
	public ExifThumbnail(int width, int height, int dataType, byte[] compressedThumbnail, IFD thumbnailIFD) {
		super(width, height, dataType, compressedThumbnail);
		this.thumbnailIFD = thumbnailIFD;
	}
	
	public void write(OutputStream os) throws IOException {
		RandomAccessOutputStream randOS = null;
		if(os instanceof RandomAccessOutputStream) randOS = (RandomAccessOutputStream)os;
		else randOS = new MemoryCacheRandomAccessOutputStream(os);
		int offset = (int)randOS.getStreamPointer(); // Get current write position
		if(getDataType() == Thumbnail.DATA_TYPE_KJpegRGB) { // Compressed old-style JPEG format
			byte[] compressedImage = getCompressedImage();
			if(compressedImage == null) throw new IllegalArgumentException("Expected compressed thumbnail data does not exist!");
			thumbnailIFD.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue(), new int[] {0})); // Placeholder
			thumbnailIFD.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue(), new int[] {compressedImage.length}));
			offset = thumbnailIFD.write(randOS, offset);
			// This line is very important!!!
			randOS.seek(offset);
			randOS.write(getCompressedImage());
			// Update fields
			randOS.seek(thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT).getDataOffset());
			randOS.writeInt(offset);
		} else if(getDataType() == Thumbnail.DATA_TYPE_TIFF) { // Uncompressed TIFF format
			// Read the IFDs into a list first
			List<IFD> list = new ArrayList<IFD>();			   
			RandomAccessInputStream tiffIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(getCompressedImage()));
			TIFFTweaker.readIFDs(list, tiffIn);
			TiffField<?> stripOffset = list.get(0).getField(TiffTag.STRIP_OFFSETS);
    		if(stripOffset == null) 
    			stripOffset = list.get(0).getField(TiffTag.TILE_OFFSETS);
    		TiffField<?> stripByteCounts = list.get(0).getField(TiffTag.STRIP_BYTE_COUNTS);
    		if(stripByteCounts == null) 
    			stripByteCounts = list.get(0).getField(TiffTag.TILE_BYTE_COUNTS);
    		offset = list.get(0).write(randOS, offset); // Write out the thumbnail IFD
    		int[] off = new int[0];;
    		if(stripOffset != null) { // Write out image data and update offset array
    			off = stripOffset.getDataAsLong();
    			int[] counts = stripByteCounts.getDataAsLong();
    			for(int i = 0; i < off.length; i++) {
    				tiffIn.seek(off[i]);
    				byte[] temp = new byte[counts[i]];
    				tiffIn.readFully(temp);
    				randOS.seek(offset);
    				randOS.write(temp);
    				off[i] = offset;
    				offset += counts[i];    				
    			}
    		}
    		tiffIn.shallowClose();
    		// Update offset field
			randOS.seek(stripOffset.getDataOffset());
			for(int i : off)
				randOS.writeInt(i);		
		} else {
			BufferedImage thumbnail = getRawImage();
			if(thumbnail == null) throw new IllegalArgumentException("Expected raw data thumbnail does not exist!");
			// We are going to write the IFD and associated thumbnail
			int thumbnailWidth = thumbnail.getWidth();
			int thumbnailHeight = thumbnail.getHeight();
			thumbnailIFD.addField(new ShortField(TiffTag.IMAGE_WIDTH.getValue(), new short[]{(short)thumbnailWidth}));
			thumbnailIFD.addField(new ShortField(TiffTag.IMAGE_LENGTH.getValue(), new short[]{(short)thumbnailHeight}));
			thumbnailIFD.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue(), new int[]{0})); // Place holder
			thumbnailIFD.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue(), new int[]{0})); // Place holder
			// Other related tags
			thumbnailIFD.addField(new RationalField(TiffTag.X_RESOLUTION.getValue(), new int[] {thumbnailWidth, 1}));
			thumbnailIFD.addField(new RationalField(TiffTag.Y_RESOLUTION.getValue(), new int[] {thumbnailHeight, 1}));
			thumbnailIFD.addField(new ShortField(TiffTag.RESOLUTION_UNIT.getValue(), new short[]{1})); //No absolute unit of measurement
			thumbnailIFD.addField(new ShortField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue(), new short[]{(short)TiffFieldEnum.PhotoMetric.YCbCr.getValue()}));
			thumbnailIFD.addField(new ShortField(TiffTag.SAMPLES_PER_PIXEL.getValue(), new short[]{3}));		
			thumbnailIFD.addField(new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), new short[]{8, 8, 8}));
			thumbnailIFD.addField(new ShortField(TiffTag.YCbCr_SUB_SAMPLING.getValue(), new short[]{1, 1}));
			thumbnailIFD.addField(new ShortField(TiffTag.PLANAR_CONFIGURATTION.getValue(), new short[]{(short)TiffFieldEnum.PlanarConfiguration.CONTIGUOUS.getValue()}));
			thumbnailIFD.addField(new ShortField(TiffTag.COMPRESSION.getValue(), new short[]{(short)TiffFieldEnum.Compression.OLD_JPG.getValue()}));
			thumbnailIFD.addField(new ShortField(TiffTag.ROWS_PER_STRIP.getValue(), new short[]{(short)thumbnailHeight}));
			// Write the thumbnail IFD
			// This line is very important!!!
			randOS.seek(thumbnailIFD.write(randOS, offset));
			// Create a JPEGWriter to write the image
			ImageWriter jpgWriter = ImageIO.getWriter(ImageType.JPG);
			// Create a ImageParam builder
			ImageParam.ImageParamBuilder builder = ImageParam.getBuilder();
			// Create JPEGOptions		
			JPEGOptions jpegOptions = new JPEGOptions();			
			jpegOptions.setQuality(writeQuality);
			builder.imageOptions(jpegOptions);
			// Set ImageParam to the writer
			jpgWriter.setImageParam(builder.build());
			// This is amazing. We can actually keep track of how many bytes have been written to
			// the underlying stream by JPEGWriter
			long startOffset = randOS.getStreamPointer();
			try {
				jpgWriter.write(thumbnail, randOS);
			} catch (Exception e) {
				e.printStackTrace();
			}
			long finishOffset = randOS.getStreamPointer();			
			int totalOut = (int)(finishOffset - startOffset);
			// Update fields
			randOS.seek(thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT).getDataOffset());
			randOS.writeInt((int)startOffset);
			randOS.seek(thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH).getDataOffset());
			randOS.writeInt(totalOut);
		}
		// Close the RandomAccessOutputStream instance if we created it locally
		if(!(os instanceof RandomAccessOutputStream)) randOS.shallowClose();
	}
}