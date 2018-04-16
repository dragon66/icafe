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
 * BMPTweaker.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    29Dec2014  Initial creation
 */

package com.icafe4j.image.bmp;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.image.ImageMetadata;
import com.icafe4j.io.IOUtils;

/**
 * BMP image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/29/2014
 */
public class BMPTweaker {	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(BMPTweaker.class);

	// Data transfer object for multiple thread support
	private static class DataTransferObject {
		private byte[] fileHeader; // 14
		private byte[] infoHeader; // 40	
		private int[] colorPalette;
	}
	
	private static void readHeader(InputStream is, DataTransferObject DTO) throws IOException {
		DTO.fileHeader = new byte[14];
		DTO.infoHeader = new byte[40];
		
		is.read(DTO.fileHeader);
		is.read(DTO.infoHeader);
	}
	
	public static Map<MetadataType, Metadata> readMetadata(InputStream is) throws IOException {
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		ImageMetadata imageMeta = new ImageMetadata();
		// Create a new data transfer object to hold data
		DataTransferObject DTO = new DataTransferObject();
		readHeader(is, DTO);
				
		LOGGER.info("... BMP Image Inforamtion starts...");
		LOGGER.info("Image signature: {}", new String(DTO.fileHeader, 0, 2));
		LOGGER.info("File size: {} bytes", IOUtils.readInt(DTO.fileHeader, 2));
		LOGGER.info("Reserved1 (2 bytes): {}", IOUtils.readShort(DTO.fileHeader, 6));
		LOGGER.info("Reserved2 (2 bytes): {}", IOUtils.readShort(DTO.fileHeader, 8));
		LOGGER.info("Data offset: {}", IOUtils.readInt(DTO.fileHeader, 10));
		
		MetadataEntry header = new MetadataEntry("BMP File Header", "Bitmap File Header", true);
		header.addEntry(new MetadataEntry("Image signature", new String(DTO.fileHeader, 0, 2)));
		header.addEntry(new MetadataEntry("File size", IOUtils.readInt(DTO.fileHeader, 2) + " bytes"));
		header.addEntry(new MetadataEntry("Reserved1", IOUtils.readShort(DTO.fileHeader, 6)+""));
		header.addEntry(new MetadataEntry("Reserved2", IOUtils.readShort(DTO.fileHeader, 8)+""));
		header.addEntry(new MetadataEntry("Data-offset", "byte " + IOUtils.readInt(DTO.fileHeader, 10)));
		
		imageMeta.addMetadataEntry(header);
	
		// TODO add more ImageMetadata elements
		LOGGER.info("Info header length: {}", IOUtils.readInt(DTO.infoHeader, 0));
		LOGGER.info("Image width: {}", IOUtils.readInt(DTO.infoHeader, 4));
		LOGGER.info("Image heigth: {}", IOUtils.readInt(DTO.infoHeader, 8));
		
		String alignment = "";
		if(IOUtils.readInt(DTO.infoHeader, 8) > 0)
			alignment = "BOTTOM_UP" ;
		else
			alignment = "TOP_DOWN";
		
		MetadataEntry infoHeader = new MetadataEntry("BMP Info Header", "Bitmap Information Header", true);
		infoHeader.addEntry(new MetadataEntry("Info-header-lengthen", IOUtils.readInt(DTO.infoHeader, 0) + " bytes"));
		infoHeader.addEntry(new MetadataEntry("Image-alignment", alignment));
		infoHeader.addEntry(new MetadataEntry("Number-of-planes", IOUtils.readShort(DTO.infoHeader, 12) + " planes"));
		infoHeader.addEntry(new MetadataEntry("Bits-per-pixel", IOUtils.readShort(DTO.infoHeader, 14) + " bits per pixel"));
		infoHeader.addEntry(new MetadataEntry("Compression", BmpCompression.fromInt(IOUtils.readInt(DTO.infoHeader, 16)).toString()));
		infoHeader.addEntry(new MetadataEntry("Compessed-image-size", IOUtils.readInt(DTO.infoHeader, 20) + " bytes"));
		infoHeader.addEntry(new MetadataEntry("Horizontal-resolution", IOUtils.readInt(DTO.infoHeader, 24) + " pixels/meter"));
		infoHeader.addEntry(new MetadataEntry("Vertical-resolution", IOUtils.readInt(DTO.infoHeader, 28) + " pixels/meter"));
		infoHeader.addEntry(new MetadataEntry("Colors-used", IOUtils.readInt(DTO.infoHeader, 32) + " colors used"));
		infoHeader.addEntry(new MetadataEntry("Important-colors", IOUtils.readInt(DTO.infoHeader, 36) + " important colors"));
	
		imageMeta.addMetadataEntry(infoHeader);
		
		LOGGER.info("Image alignment: {}", alignment);
		LOGGER.info("Number of planes: {}", IOUtils.readShort(DTO.infoHeader, 12));
		LOGGER.info("BitCount (bits per pixel): {}", IOUtils.readShort(DTO.infoHeader, 14));
		LOGGER.info("Compression: {}", BmpCompression.fromInt(IOUtils.readInt(DTO.infoHeader, 16)));
		LOGGER.info("Image size (compressed size of image): {} bytes", IOUtils.readInt(DTO.infoHeader, 20));
		LOGGER.info("Horizontal resolution (Pixels/meter): {}", IOUtils.readInt(DTO.infoHeader, 24));
		LOGGER.info("Vertical resolution (Pixels/meter): {}", IOUtils.readInt(DTO.infoHeader, 28));
		LOGGER.info("Colors used (number of actually used colors): {}", IOUtils.readInt(DTO.infoHeader, 32));
		LOGGER.info("Important colors (number of important colors): {}", IOUtils.readInt(DTO.infoHeader, 36));		
				
		int bitsPerPixel = IOUtils.readShort(DTO.infoHeader, 14);
		
		if(bitsPerPixel <= 8) {
			readPalette(is, DTO);
			LOGGER.info("Color map follows");
		}
		
		metadataMap.put(MetadataType.IMAGE, imageMeta);
		
		return metadataMap;		
	}
	
	private static void readPalette(InputStream is, DataTransferObject DTO) throws IOException {
		int index = 0, bindex = 0;
		int colorsUsed = IOUtils.readInt(DTO.infoHeader, 32);
		int bitsPerPixel = IOUtils.readShort(DTO.infoHeader, 14);
		int dataOffset = IOUtils.readInt(DTO.fileHeader, 10);
		int numOfColors = (colorsUsed == 0)?(1<<bitsPerPixel):colorsUsed;
		byte palette[] = new byte[numOfColors*4];
		DTO.colorPalette = new int[numOfColors];	
     
		IOUtils.readFully(is, palette);

        for(int i = 0; i < numOfColors; i++)
		{
			DTO.colorPalette[index++] = ((0xff<<24)|(palette[bindex]&0xff)|((palette[bindex+1]&0xff)<<8)|((palette[bindex+2]&0xff)<<16));
			bindex += 4;
		}
		// There may be some extra bytes between colorPalette and actual image data
		IOUtils.skipFully(is, dataOffset - numOfColors*4 - 54);
	}
	
	private BMPTweaker() {}
}