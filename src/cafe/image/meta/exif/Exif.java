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
 * WY    14Jan2015  Moved thumbnail related code to ExifThumbnail
 * WY    06May2014  Complete rewrite to support adding thumbnail IFD
 */

package cafe.image.meta.exif;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cafe.image.jpeg.Marker;
import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataReader;
import cafe.image.meta.MetadataType;
import cafe.image.tiff.ASCIIField;
import cafe.image.tiff.IFD;
import cafe.image.tiff.LongField;
import cafe.image.tiff.RationalField;
import cafe.image.tiff.ShortField;
import cafe.image.tiff.Tag;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffTag;
import cafe.io.IOUtils;
import cafe.io.MemoryCacheRandomAccessOutputStream;
import cafe.io.RandomAccessOutputStream;
import cafe.io.WriteStrategyMM;

/**
 * EXIF wrapper
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/08/2014
 */
public class Exif extends Metadata {
	// EXIF thumbnail flavors
	public static final int EXIF_FLAVOR_JPG = 0xFFD8;//JPEG SOI
	public static final int EXIF_FLAVOR_TIFF = 0x4D4D;//TIFF MM
	
	private int flavor;	
	private IFD imageIFD;
	private IFD exifSubIFD;
	private IFD gpsSubIFD;
	private ExifThumbnail thumbnail;
	private int firstIFDOffset = 0x08;
	
	private MetadataReader reader;
	
	private boolean isThumbnailRequired;
	
	public Exif(int flavor) {
		super(MetadataType.EXIF, null); 
		if(flavor != Exif.EXIF_FLAVOR_JPG && flavor != Exif.EXIF_FLAVOR_TIFF)
			throw new IllegalArgumentException("Unknown EXIF flavor: " + flavor);
		this.flavor = flavor;
		if(flavor == Exif.EXIF_FLAVOR_JPG)
			createImageIFD();		
	}
	
	public Exif(byte[] data) {
		super(MetadataType.EXIF, data);
		this.reader = new ExifReader(data);
	}
	
	public Exif(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	private void createImageIFD() {
		// Create Image IFD (IFD0)
		imageIFD = new IFD();
		TiffField<?> tiffField = new ASCIIField(TiffTag.IMAGE_DESCRIPTION.getValue(), "Exif created by JPEGTweaker\0");
		imageIFD.addField(tiffField);
		tiffField = new ASCIIField(TiffTag.MAKE.getValue(), "Yu's\0");
		imageIFD.addField(tiffField);
		tiffField = new ASCIIField(TiffTag.MODEL.getValue(), "JPEGTweaker 1.0\0");
		imageIFD.addField(tiffField);
		tiffField = new ShortField(TiffTag.ORIENTATION.getValue(), new short[]{1});
		imageIFD.addField(tiffField);
		tiffField = new RationalField(TiffTag.X_RESOLUTION.getValue(), new int[] {300, 1});
		imageIFD.addField(tiffField);
		tiffField = new RationalField(TiffTag.Y_RESOLUTION.getValue(), new int[] {300, 1});
		imageIFD.addField(tiffField);
		tiffField = new ShortField(TiffTag.RESOLUTION_UNIT.getValue(), new short[]{2});
		imageIFD.addField(tiffField);
		String softWare = "JPEGTweaker 1.0\0";
		tiffField = new ASCIIField(TiffTag.SOFTWARE.getValue(), softWare);
		imageIFD.addField(tiffField);
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		tiffField = new ASCIIField(TiffTag.DATETIME.getValue(), formatter.format(new Date()) + '\0');
		imageIFD.addField(tiffField);
		tiffField = new LongField(TiffTag.EXIF_SUB_IFD.getValue(), new int[]{0}); // Place holder
		imageIFD.addField(tiffField);
	}
	
	public boolean hasThumbnail() {
		return thumbnail != null && thumbnail.containsImage();
	}
	
	public void addExif(IFD exifSubIFD) {
		if(imageIFD != null)
			imageIFD.addChild(TiffTag.EXIF_SUB_IFD, exifSubIFD);
		else
			this.exifSubIFD = exifSubIFD;
	}
	
	public void addGPS(IFD gpsSubIFD) {
		if(imageIFD != null)
			imageIFD.addChild(TiffTag.GPS_SUB_IFD, gpsSubIFD);
		else
			this.gpsSubIFD = gpsSubIFD;
	}
	
	/**
	 * @param thumbnail optional thumbnail image. If null, will be generated from the input image
	 */
	public void addThumbnail(BufferedImage thumbnail) {
		this.thumbnail = new ExifThumbnail(flavor, thumbnail);
		this.isThumbnailRequired = true;
	}
	
	public boolean isThumbnailRequired() {
		return isThumbnailRequired;
	}	
	
	public void setThumbnail(BufferedImage thumbnail) {
		this.thumbnail.setImage(thumbnail);
	}
	
	public IFD getIFD(Tag tag) {
		if(tag.getValue() == TiffTag.EXIF_SUB_IFD.getValue()) {
			return exifSubIFD;
		} else if(tag.getValue() == TiffTag.GPS_SUB_IFD.getValue()) {
			return gpsSubIFD;
		}
		return null;
	}
	
	public MetadataReader getReader() {
		return reader;
	}
	
	/** 
	 * Write the EXIF data to the OutputStream
	 * 
	 * @param os OutputStream
	 * @throws Exception 
	 */
	@Override
	public void write(OutputStream os) throws IOException {
		RandomAccessOutputStream randOS = null;
		// Write the EXIF data according to the flavor
		if(flavor == Exif.EXIF_FLAVOR_JPG) {
			// Writes APP1 marker
			IOUtils.writeShortMM(os, Marker.APP1.getValue());
			// Wraps output stream with a RandomAccessOutputStream
			randOS = new MemoryCacheRandomAccessOutputStream(os);
			// TIFF structure starts here
			short endian = IOUtils.BIG_ENDIAN;
			short tiffID = 0x2a; //'*'
			randOS.setWriteStrategy(WriteStrategyMM.getInstance());
			randOS.writeShort(endian);
			randOS.writeShort(tiffID);
			// First IFD offset relative to TIFF structure
			randOS.seek(0x04);
			randOS.writeInt(firstIFDOffset);
			// Writes IFDs
			randOS.seek(firstIFDOffset);
			int offset = imageIFD.write(randOS, firstIFDOffset);
			if(isThumbnailRequired && thumbnail != null) {
				imageIFD.setNextIFDOffset(randOS, offset);
				thumbnail.write(randOS, offset);
			}
			// Now it's time to update the segment length
			int length = (int)randOS.getLength();
			// Update segment length
			IOUtils.writeShortMM(os, length + 8);
			// Add EXIF identifier with trailing bytes [0x00,0x00].
			byte[] exif = {0x45, 0x78, 0x69, 0x66, 0x00, 0x00};
			IOUtils.write(os, exif);
			// Dump randOS to normal output stream and we are done!
			randOS.seek(0);
			randOS.writeToStream(length);
			randOS.close();
		} else if(flavor == Exif.EXIF_FLAVOR_TIFF) {
			if(isThumbnailRequired && thumbnail != null) {
				randOS = (RandomAccessOutputStream)os;
				thumbnail.write(randOS, (int)randOS.getStreamPointer());
			}
		}
	}	
}