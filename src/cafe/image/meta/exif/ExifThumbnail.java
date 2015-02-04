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
 * ExifThumbnail.java
 *
 * Who   Date          Description
 * ====  ==========    =================================================
 * WY    14Jan2015     initial creation
 */

package cafe.image.meta.exif;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cafe.image.ImageIO;
import cafe.image.ImageParam;
import cafe.image.ImageType;
import cafe.image.meta.Thumbnail;
import cafe.image.options.JPEGOptions;
import cafe.image.tiff.IFD;
import cafe.image.tiff.LongField;
import cafe.image.tiff.RationalField;
import cafe.image.tiff.ShortField;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffFieldEnum;
import cafe.image.tiff.TiffTag;
import cafe.image.writer.ImageWriter;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;

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
	private int writeQuality = 90; // Default JPEG write quality
	
	public ExifThumbnail() { }
	
	public ExifThumbnail(BufferedImage thumbnail) {
		super(thumbnail);
	}
	
	public ExifThumbnail(int width, int height, int dataType, byte[] compressedThumbnail, IFD thumbnailIFD) {
		super(width, height, dataType, compressedThumbnail);
		this.thumbnailIFD = thumbnailIFD;
	}
	
	public void setWriteQuality(int quality) {
		this.writeQuality = quality;
	}
	
	public void write(RandomAccessOutputStream randOS, int offset) throws IOException {
		if(getDataType() == Thumbnail.DATA_TYPE_KJpegRGB) { // Compressed old-style JPEG format
			thumbnailIFD.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue(), new int[] {0})); // Placeholder
			offset = thumbnailIFD.write(randOS, offset);
			// This line is very important!!!
			randOS.seek(offset);
			randOS.write(getCompressedImage());
			// Update fields
			randOS.seek(thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue()).getDataOffset());
			randOS.writeInt(offset);
		} else if(getDataType() == Thumbnail.DATA_TYPE_TIFF) { // Uncompressed TIFF format
			// Read the IFDs into a list first
			List<IFD> list = new ArrayList<IFD>();			   
			RandomAccessInputStream tiffIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(getCompressedImage()));
			TIFFTweaker.readIFDs(list, tiffIn);
			TiffField<?> stripOffset = list.get(0).getField(TiffTag.STRIP_OFFSETS.getValue());
    		if(stripOffset == null) 
    			stripOffset = list.get(0).getField(TiffTag.TILE_OFFSETS.getValue());
    		TiffField<?> stripByteCounts = list.get(0).getField(TiffTag.STRIP_BYTE_COUNTS.getValue());
    		if(stripByteCounts == null) 
    			stripByteCounts = list.get(0).getField(TiffTag.TILE_BYTE_COUNTS.getValue());
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
    		tiffIn.close();
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
			ImageParam.ImageParamBuilder builder = new ImageParam.ImageParamBuilder();
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
			randOS.seek(thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue()).getDataOffset());
			randOS.writeInt((int)startOffset);
			randOS.seek(thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue()).getDataOffset());
			randOS.writeInt(totalOut);
		}
	}
}