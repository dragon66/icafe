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
import java.io.IOException;

import cafe.image.ImageIO;
import cafe.image.ImageMeta;
import cafe.image.ImageType;
import cafe.image.options.JPEGOptions;
import cafe.image.tiff.IFD;
import cafe.image.tiff.LongField;
import cafe.image.tiff.RationalField;
import cafe.image.tiff.ShortField;
import cafe.image.tiff.TiffFieldEnum;
import cafe.image.tiff.TiffTag;
import cafe.image.writer.ImageWriter;
import cafe.io.RandomAccessOutputStream;

/**
 * Encapsulates image EXIF thumbnail metadata
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/08/2014
 */
public class ExifThumbnail {
	// Comprised of an IFD and an associated image
	//Create thumbnail IFD (IFD1 in the case of JPEG EXIF segment)
	private IFD thumbnailIFD; 
	private BufferedImage thumbnail;
	// JPEG and TIFF, perhaps other formats have subtle difference
	private int flavor;
	private int quality = 90;
	
	public ExifThumbnail() {
		this(Exif.EXIF_FLAVOR_JPG); // Default to JPEG flavor
	}
	
	public ExifThumbnail(int flavor) {
		this(flavor, null);
	}
	
	public ExifThumbnail(int flavor, BufferedImage thumbnail) {
		this.flavor = flavor;
		this.thumbnail = thumbnail;
		thumbnailIFD = new IFD();
	}
	
	public boolean containsImage() {
		return thumbnail != null;
	}
	
	public void setImage(BufferedImage thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	public void setImageQuality(int quality) {
		this.quality = quality;
	}
	
	public void write(RandomAccessOutputStream randOS, int offset) throws IOException {
		// We are going to write the IFD and associated thumbnail
		int thumbnailWidth = thumbnail.getWidth();
		int thumbnailHeight = thumbnail.getHeight();
		thumbnailIFD.addField(new ShortField(TiffTag.IMAGE_WIDTH.getValue(), new short[]{(short)thumbnailWidth}));
		thumbnailIFD.addField(new ShortField(TiffTag.IMAGE_LENGTH.getValue(), new short[]{(short)thumbnailHeight}));
		if(flavor == Exif.EXIF_FLAVOR_JPG) {
			thumbnailIFD.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue(), new int[]{0})); // Place holder
			thumbnailIFD.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue(), new int[]{0})); // Place holder
		} else {
			thumbnailIFD.addField(new LongField(TiffTag.STRIP_OFFSETS.getValue(), new int[]{0})); // Place holder
			thumbnailIFD.addField(new LongField(TiffTag.STRIP_BYTE_COUNTS.getValue(), new int[]{0})); // Place holder
		}
		// Other related tags
		thumbnailIFD.addField(new RationalField(TiffTag.X_RESOLUTION.getValue(), new int[] {thumbnailWidth, 1}));
		thumbnailIFD.addField(new RationalField(TiffTag.Y_RESOLUTION.getValue(), new int[] {thumbnailHeight, 1}));
		thumbnailIFD.addField(new ShortField(TiffTag.RESOLUTION_UNIT.getValue(), new short[]{1})); //No absolute unit of measurement
		thumbnailIFD.addField(new ShortField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue(), new short[]{(short)TiffFieldEnum.PhotoMetric.YCbCr.getValue()}));
		thumbnailIFD.addField(new ShortField(TiffTag.SAMPLES_PER_PIXEL.getValue(), new short[]{3}));		
		thumbnailIFD.addField(new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), new short[]{8, 8, 8}));
		thumbnailIFD.addField(new ShortField(TiffTag.YCbCr_SUB_SAMPLING.getValue(), new short[]{1, 1}));
		thumbnailIFD.addField(new ShortField(TiffTag.PLANAR_CONFIGURATTION.getValue(), new short[]{(short)TiffFieldEnum.PlanarConfiguration.CONTIGUOUS.getValue()}));
		if(flavor == Exif.EXIF_FLAVOR_JPG)
			thumbnailIFD.addField(new ShortField(TiffTag.COMPRESSION.getValue(), new short[]{(short)TiffFieldEnum.Compression.OLD_JPG.getValue()}));
		else
			thumbnailIFD.addField(new ShortField(TiffTag.COMPRESSION.getValue(), new short[]{(short)TiffFieldEnum.Compression.JPG.getValue()}));			
		thumbnailIFD.addField(new ShortField(TiffTag.ROWS_PER_STRIP.getValue(), new short[]{(short)thumbnailHeight}));
		// Write the thumbnail IFD
		// This line is very important!!!
		randOS.seek(thumbnailIFD.write(randOS, offset));
		// Create a JPEGWriter to write the image
		ImageWriter jpgWriter = ImageIO.getWriter(ImageType.JPG);
		// Create a ImageMeta builder
		ImageMeta.ImageMetaBuilder builder = new ImageMeta.ImageMetaBuilder();
		// Create JPEGOptions		
		JPEGOptions jpegOptions = new JPEGOptions();			
		jpegOptions.setQuality(quality);
		builder.imageOptions(jpegOptions);
		// Set ImageMeta to the writer
		jpgWriter.setImageMeta(builder.build());
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
		if(flavor == Exif.EXIF_FLAVOR_JPG) {// Update fields
			randOS.seek(thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue()).getDataOffset());
			randOS.writeInt((int)startOffset);
			randOS.seek(thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue()).getDataOffset());
			randOS.writeInt(totalOut);
		} else if(flavor == Exif.EXIF_FLAVOR_TIFF) {
			randOS.seek(thumbnailIFD.getField(TiffTag.STRIP_OFFSETS.getValue()).getDataOffset());
			randOS.writeInt((int)startOffset);
			randOS.seek(thumbnailIFD.getField(TiffTag.STRIP_BYTE_COUNTS.getValue()).getDataOffset());
			randOS.writeInt(totalOut);
		}
	}
}
