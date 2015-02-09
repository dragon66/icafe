/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * ExifReader.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    06Feb2015  Moved showIFDs() and showIFD() to TIFFTweaker and renamed them to
 *                  printIFDs() and printIFD()
 */

package cafe.image.meta.exif;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import cafe.image.meta.MetadataReader;
import cafe.image.meta.Thumbnail;
import cafe.image.tiff.IFD;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffTag;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;

public class ExifReader implements MetadataReader {
	private boolean loaded;
	private byte[] data;
	private ExifThumbnail thumbnail;
	private boolean containsThumbnail;
	private List<IFD> ifds = new ArrayList<IFD>(3);
	
	public ExifReader(byte[] exif) {
		this.data = exif;
	}
	
	public ExifReader(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public ExifReader(IFD imageIFD) {
		ifds.add(0, imageIFD);
	}
	
	public IFD getExifIFD() {
		return ifds.get(0).getChild(TiffTag.EXIF_SUB_IFD);
	}
	
	public IFD getGPSIFD() {
		return ifds.get(0).getChild(TiffTag.GPS_SUB_IFD);
	}
	
	public IFD getImageIFD() {
		return ifds.get(0);
	}
	
	public List<IFD> getIFDs() {
		return ifds;
	}
	
	public boolean containsThumbnail() {
		return containsThumbnail;
	}
	
	public ExifThumbnail getThumbnail() {
		return thumbnail;
	}
	
	public boolean isDataLoaded() {
		return loaded;
	}
	
	@Override
	public void read() throws IOException {
		if(data != null) {
			RandomAccessInputStream exifIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(data));
	    	TIFFTweaker.readIFDs(ifds, exifIn);		
		    // We have thumbnail IFD
		    if(ifds.size() >= 2) {
		    	containsThumbnail = true;
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
		    	} else { // Uncompressed, save as TIFF
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
		    		}
		    	}
		    }
		    exifIn.close();
		}
	    loaded = true;
	}
	
	@Override
	public void showMetadata() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Exif reader output starts =>");
		TIFFTweaker.printIFDs(ifds, "");
		if(containsThumbnail) {
			System.out.println("Exif thumbnail format: " + (thumbnail.getDataType() == 1? "DATA_TYPE_JPG":"DATA_TYPE_TIFF"));
			System.out.println("Exif thumbnail data length: " + thumbnail.getCompressedImage().length);
		}
		System.out.println("<= Exif reader output ends");
	}
}