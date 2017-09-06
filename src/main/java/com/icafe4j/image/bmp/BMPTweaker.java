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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static com.icafe4j.string.XMLUtils.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.meta.Metadata;
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
		Document doc = createDocumentNode(); // Create a document for ImageMetadata
		// Create a new data transfer object to hold data
		DataTransferObject DTO = new DataTransferObject();
		readHeader(is, DTO);
		
		LOGGER.info("... BMP snoop starts...");
		LOGGER.info("Image signature: {}", new String(DTO.fileHeader, 0, 2));
		LOGGER.info("File size: {} bytes", IOUtils.readInt(DTO.fileHeader, 2));
		LOGGER.info("Reserved1 (2 bytes): {}", IOUtils.readShort(DTO.fileHeader, 6));
		LOGGER.info("Reserved2 (2 bytes): {}", IOUtils.readShort(DTO.fileHeader, 8));
		LOGGER.info("Data offset: {}", IOUtils.readInt(DTO.fileHeader, 10));
		
		Node root = createElement(doc, "bitmap");
		Node header = createElement(doc, "header");
		Node fileHeader = createElement(doc, "file-header");
		Node imageSignature = createElement(doc, "image-signature");
		Node fileSize = createElement(doc, "file-size");
		Node reserved1 = createElement(doc, "reserved1");		
		Node reserved2 = createElement(doc, "reserved2");
		Node dataOffset = createElement(doc, "data-offset");
		addText(doc, imageSignature, new String(DTO.fileHeader, 0, 2));
		addText(doc, fileSize, IOUtils.readInt(DTO.fileHeader, 2) + " bytes");
		addText(doc, reserved1, "" + IOUtils.readShort(DTO.fileHeader, 6));
		addText(doc, reserved2, "" + IOUtils.readShort(DTO.fileHeader, 8));
		addText(doc, dataOffset, "byte " + IOUtils.readInt(DTO.fileHeader, 10));
		addChild(header, fileHeader);
		addChild(fileHeader, imageSignature);
		addChild(fileHeader, fileSize);
		addChild(fileHeader, reserved1);
		addChild(fileHeader, reserved2);
		addChild(fileHeader, dataOffset);
		
		// TODO add more ImageMetadata elements to doc
		LOGGER.info("Info header length: {}", IOUtils.readInt(DTO.infoHeader, 0));
		LOGGER.info("Image width: {}", IOUtils.readInt(DTO.infoHeader, 4));
		LOGGER.info("Image heigth: {}", IOUtils.readInt(DTO.infoHeader, 8));	
		
		String alignment = "";
		if(IOUtils.readInt(DTO.infoHeader, 8) > 0)
			alignment = "BOTTOM_UP" ;
		else
			alignment = "TOP_DOWN";
		
		LOGGER.info("Image alignment: {}", alignment);
		LOGGER.info("Number of planes: {}", IOUtils.readShort(DTO.infoHeader, 12));
		LOGGER.info("BitCount (bits per pixel): {}", IOUtils.readShort(DTO.infoHeader, 14));
		LOGGER.info("Compression: {}", BmpCompression.fromInt(IOUtils.readInt(DTO.infoHeader, 16)));
		LOGGER.info("Image size (compressed size of image): {} bytes", IOUtils.readInt(DTO.infoHeader, 20));
		LOGGER.info("Horizontal resolution (Pixels/meter): {}", IOUtils.readInt(DTO.infoHeader, 24));
		LOGGER.info("Vertical resolution (Pixels/meter): {}", IOUtils.readInt(DTO.infoHeader, 28));
		LOGGER.info("Colors used (number of actually used colors): {}", IOUtils.readInt(DTO.infoHeader, 32));
		LOGGER.info("Important colors (number of important colors): {}", IOUtils.readInt(DTO.infoHeader, 36));
		
		Node infoHeader = createElement(doc, "info-header");
		Node infoHeaderLen = createElement(doc, "info-header-length");
		Node imageAlignment = createElement(doc, "image-alignment");
		Node numOfPlanes = createElement(doc, "number-of-planes");
		Node bitCount = createElement(doc, "bits-per-pixel");
		Node compression = createElement(doc, "compression");
		Node imageSize = createElement(doc, "compessed-image-size");
		Node horizontalResolution = createElement(doc, "horizontal-resolution");
		Node verticalResolution = createElement(doc, "vertical-resolution");
		Node colorsUsed = createElement(doc, "colors-used");
		Node importantColors = createElement(doc, "important-colors");
		
		addText(doc, infoHeaderLen, IOUtils.readInt(DTO.infoHeader, 0) + " bytes");
		addText(doc, imageAlignment, alignment);
		addText(doc, numOfPlanes, IOUtils.readShort(DTO.infoHeader, 12) + " planes");
		addText(doc, bitCount, IOUtils.readShort(DTO.infoHeader, 14) + " bits per pixel");
		addText(doc, compression, BmpCompression.fromInt(IOUtils.readInt(DTO.infoHeader, 16)).toString());
		addText(doc, imageSize, IOUtils.readInt(DTO.infoHeader, 20) + " bytes");
		addText(doc, horizontalResolution, IOUtils.readInt(DTO.infoHeader, 24) + " pixels/meter");
		addText(doc, verticalResolution, IOUtils.readInt(DTO.infoHeader, 28) + " pixels/meter");
		addText(doc, colorsUsed, IOUtils.readInt(DTO.infoHeader, 32) + " colors used");
		addText(doc, importantColors, IOUtils.readInt(DTO.infoHeader, 36) + " important colors");		
		
		addChild(infoHeader, infoHeaderLen);
		addChild(infoHeader, imageAlignment);
		addChild(infoHeader, numOfPlanes);
		addChild(infoHeader, bitCount);
		addChild(infoHeader, compression);
		addChild(infoHeader, imageSize);
		addChild(infoHeader, horizontalResolution);
		addChild(infoHeader, verticalResolution);
		addChild(infoHeader, colorsUsed);
		addChild(infoHeader, importantColors);
		
		addChild(header, infoHeader);
		
		addChild(root, header);
		addChild(doc, root);
				
		int bitsPerPixel = IOUtils.readShort(DTO.infoHeader, 14);
		
		if(bitsPerPixel <= 8) {
			readPalette(is, DTO);
			LOGGER.info("Color map follows");
		}
		
		metadataMap.put(MetadataType.IMAGE, new ImageMetadata(doc));
		
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