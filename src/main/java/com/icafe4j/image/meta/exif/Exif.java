/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * Exif.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    10Apr2015  Moved data loaded checking to ExifReader
 * WY    31Mar2015  Fixed bug with getImageIFD() etc
 * WY    17Feb2015  Added addImageField() to add TIFF image tag
 * WY    11Feb2015  Added showMetadata()
 * WY    03Feb2015  Factored out TiffExif and JpegExif
 * WY    03Feb2015  Made class abstract
 * WY    14Jan2015  Moved thumbnail related code to ExifThumbnail
 * WY    06May2014  Complete rewrite to support adding thumbnail IFD
 */

package com.icafe4j.image.meta.exif;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.Thumbnail;
import com.icafe4j.image.tiff.FieldType;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.TIFFTweaker;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.FileCacheRandomAccessOutputStream;
import com.icafe4j.io.IOUtils;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.io.RandomAccessOutputStream;

/**
 * EXIF wrapper
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/08/2014
 */
public abstract class Exif extends Metadata {
	protected IFD imageIFD;
	protected IFD exifSubIFD;
	protected IFD gpsSubIFD;
	protected ExifThumbnail thumbnail;
	
	private boolean containsThumbnail;
	private boolean isThumbnailRequired;
	
	public static final int FIRST_IFD_OFFSET = 0x08;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Exif.class);
	
	public Exif() {
		super(MetadataType.EXIF);
		isDataRead = true;
	}
	
	public Exif(byte[] data) {
		super(MetadataType.EXIF, data);
		ensureDataRead();
	}
	
	public Exif(IFD imageIFD) {
		this();
		setImageIFD(imageIFD);
	}
	
	public Exif(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public void addExifField(ExifTag tag, FieldType type, Object data) {
		if(exifSubIFD == null)
			exifSubIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			exifSubIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required EXIF TIFF field");
	}
	
	public void addGPSField(GPSTag tag, FieldType type, Object data) {
		if(gpsSubIFD == null)
			gpsSubIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			gpsSubIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required GPS TIFF field");
	}
	
	public void addImageField(TiffTag tag, FieldType type, Object data) {
		if(imageIFD == null)
			imageIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			imageIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required Image TIFF field");
	}
	
	public boolean containsThumbnail() {
		if(containsThumbnail)
			return true;
		if(thumbnail != null)
			return true;
		return false;
	}
	
	public IFD getExifIFD() {
		if(exifSubIFD != null) {
			return new IFD(exifSubIFD);
		}
		
		return null;
	}
	
	public IFD getGPSIFD() {
		if(gpsSubIFD != null) {
			return new IFD(gpsSubIFD);
		} 

		return null;
	}
	
	public IFD getImageIFD() {
		if(imageIFD != null) {
			return new IFD(imageIFD);
		}
		
		return null;		
	}

	public ExifThumbnail getThumbnail() {
		if(thumbnail != null)
			return new ExifThumbnail(thumbnail);
	
		return null;
	}
	
	public boolean isThumbnailRequired() {
		return isThumbnailRequired;
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			RandomAccessInputStream exifIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(data));
			List<IFD> ifds = new ArrayList<IFD>(3);
			TIFFTweaker.readIFDs(ifds, exifIn);
			if(ifds.size() > 0) {
				imageIFD = ifds.get(0);
				exifSubIFD = imageIFD.getChild(TiffTag.EXIF_SUB_IFD);
				gpsSubIFD = imageIFD.getChild(TiffTag.GPS_SUB_IFD);
			}
		    // We have thumbnail IFD
		    if(ifds.size() >= 2) {
		    	IFD thumbnailIFD = ifds.get(1);
		    	int width = -1;
		    	int height = -1;
		    	TiffField<?> field = thumbnailIFD.getField(TiffTag.IMAGE_WIDTH);
		    	if(field != null) 
		    		width = field.getDataAsLong()[0];
		    	field = thumbnailIFD.getField(TiffTag.IMAGE_LENGTH);
		    	if(field != null)
		    		height = field.getDataAsLong()[0];
		    	field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT);
		    	if(field != null) { // JPEG format, save as JPEG
		    		int thumbnailOffset = field.getDataAsLong()[0];
		    		field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH);
		    		int thumbnailLen = field.getDataAsLong()[0];
		    		exifIn.seek(thumbnailOffset);
		    		byte[] thumbnailData = new byte[thumbnailLen];
		    		exifIn.readFully(thumbnailData);
		    		thumbnail = new ExifThumbnail(width, height, Thumbnail.DATA_TYPE_KJpegRGB, thumbnailData, thumbnailIFD);
		    		containsThumbnail = true;				    
		    	} else { // Uncompressed TIFF
		    		field = thumbnailIFD.getField(TiffTag.STRIP_OFFSETS);
		    		if(field == null) 
		    			field = thumbnailIFD.getField(TiffTag.TILE_OFFSETS);
		    		if(field != null) {
		    			 exifIn.seek(0);
		    			 ByteArrayOutputStream bout = new ByteArrayOutputStream();
		    			 RandomAccessOutputStream tiffout = new FileCacheRandomAccessOutputStream(bout);
		    			 TIFFTweaker.retainPages(exifIn, tiffout, 1);
		    			 tiffout.close(); // Auto flush when closed
		    			 thumbnail = new ExifThumbnail(width, height, Thumbnail.DATA_TYPE_TIFF, bout.toByteArray(), thumbnailIFD);
		    			 containsThumbnail = true;		    			    
		    		}
		    	}
		    }
		    exifIn.shallowClose();		
		    isDataRead = true;
		}
	}
	
	public void setExifIFD(IFD exifSubIFD) {
		this.exifSubIFD = exifSubIFD;
	}
	
	public void setGPSIFD(IFD gpsSubIFD) {
		this.gpsSubIFD = gpsSubIFD;
	}
	
	public void setImageIFD(IFD imageIFD) {
		if(imageIFD == null)
			throw new IllegalArgumentException("Input image IFD is null");
		this.imageIFD = imageIFD;
		this.exifSubIFD = imageIFD.getChild(TiffTag.EXIF_SUB_IFD);
		this.gpsSubIFD = imageIFD.getChild(TiffTag.GPS_SUB_IFD);
	}
	
	/**
	 * @param thumbnail a Thumbnail instance. If null, a thumbnail
	 *        will be generated from the input image.
	 */	
	public void setThumbnail(ExifThumbnail thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	public void setThumbnailImage(BufferedImage thumbnail) {
		if(this.thumbnail == null)
			this.thumbnail = new ExifThumbnail(); 
		this.thumbnail.setImage(thumbnail);
	}
	
	public void setThumbnailRequired(boolean isThumbnailRequired) {
		this.isThumbnailRequired = isThumbnailRequired;
	}
		
	@Override
	public void showMetadata() {
		ensureDataRead();
		LOGGER.info("Exif output starts =>");
		if(imageIFD != null) {
			LOGGER.info("<<Image IFD starts>>");
			TIFFTweaker.printIFD(imageIFD, TiffTag.class, "");
			LOGGER.info("<<Image IFD ends>>");
		}
		if(containsThumbnail) {
			LOGGER.info("Exif thumbnail format: {}", (thumbnail.getDataType() == 1? "DATA_TYPE_JPG":"DATA_TYPE_TIFF"));
			LOGGER.info("Exif thumbnail data length: {}", thumbnail.getCompressedImage().length);
		}
		LOGGER.info("<= Exif output ends");
	}
	
	public abstract void write(OutputStream os) throws IOException;
}