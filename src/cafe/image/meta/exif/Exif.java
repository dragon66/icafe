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
 * Exif.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    11Feb2015  Added showMetadata()
 * WY    03Feb2015  Factored out TiffExif and JpegExif
 * WY    03Feb2015  Made class abstract
 * WY    14Jan2015  Moved thumbnail related code to ExifThumbnail
 * WY    06May2014  Complete rewrite to support adding thumbnail IFD
 */

package cafe.image.meta.exif;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.image.tiff.FieldType;
import cafe.image.tiff.IFD;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffTag;
import cafe.io.IOUtils;

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
	
	public static final int firstIFDOffset = 0x08;
	
	private ExifReader reader;
	
	private boolean isThumbnailRequired;
	
	public Exif() {
		super(MetadataType.EXIF, null);
	}
	
	public Exif(byte[] data) {
		super(MetadataType.EXIF, data);
		this.reader = new ExifReader(data);
	}
	
	public Exif(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public Exif(IFD imageIFD) {
		this();
		setImageIFD(imageIFD);
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
	
	
	public void addThumbnail(ExifThumbnail thumbnail) {
		if(thumbnail == null)
			this.thumbnail = new ExifThumbnail(); 
		else
			this.thumbnail = thumbnail;
		this.isThumbnailRequired = true;
	}
	
	public boolean containsImage() {
		return thumbnail != null && thumbnail.containsImage();
	}
	
	public IFD getImageIFD() {
		if(imageIFD != null) {
			return imageIFD;
		} else {
			if (reader != null && !reader.isDataLoaded()) {
				try {
					reader.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
				imageIFD = reader.getImageIFD();
			}
			
			return imageIFD;
		}
	}
	
	public IFD getExifIFD() {
		if(exifSubIFD != null) {
			return exifSubIFD;
		} else {
			if (reader != null && !reader.isDataLoaded()) {
				try {
					reader.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
				exifSubIFD = reader.getImageIFD();
			}
			
			return exifSubIFD;
		}
	}
	
	public IFD getGPSIFD() {
		if(gpsSubIFD != null) {
			return gpsSubIFD;
		} else {
			if (reader != null && !reader.isDataLoaded()) {
				try {
					reader.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
				gpsSubIFD = reader.getImageIFD();
			}
			
			return gpsSubIFD;
		}
	}
	
	public ExifReader getReader() {
		return reader;
	}
	
	public ExifThumbnail getThumbnail() {
		if(thumbnail != null)
			return thumbnail;
		if(reader != null && !reader.isDataLoaded()) {
			try {
				reader.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return reader.getThumbnail();
		}
		return null;
	}
	
	public boolean isThumbnailRequired() {
		return isThumbnailRequired;
	}
	
	public void setExifIFD(IFD exifSubIFD) {
		this.exifSubIFD = exifSubIFD;
	}
	
	public void setGPSSubIFD(IFD gpsSubIFD) {
		this.gpsSubIFD = gpsSubIFD;
	}
	
	public void setImageIFD(IFD imageIFD) {
		this.imageIFD = imageIFD;
		IFD exifSubIFD = imageIFD.getChild(TiffTag.EXIF_SUB_IFD);
		if(exifSubIFD != null)
			this.exifSubIFD = exifSubIFD;
		IFD gpsSubIFD = imageIFD.getChild(TiffTag.GPS_SUB_IFD);
		if(gpsSubIFD != null)
			this.gpsSubIFD = gpsSubIFD;
	}
	
	/**
	 * @param thumbnail an ExifThumbnail instance. If null, a thumbnail will be
	 *        generated from the input image.
	 */	
	public void setThumbnail(ExifThumbnail thumbnail) {
		if(thumbnail == null)
			this.thumbnail = new ExifThumbnail(); 
		else
			this.thumbnail = thumbnail;
		this.isThumbnailRequired = true;
	}
	
	public void setThumbnailImage(BufferedImage thumbnail) {
		if(this.thumbnail == null)
			this.thumbnail = new ExifThumbnail(); 
		this.thumbnail.setImage(thumbnail);
	}
	
	@Override
	public void showMetadata() {
		if(imageIFD != null) {
			System.out.println("<<Image IFD starts>>");
			TIFFTweaker.printIFD(imageIFD, TiffTag.class, "");
			System.out.println("<<Image IFD ends>>");
		} else
			super.showMetadata();
	}
	
	public abstract void write(OutputStream os) throws IOException;
}