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
 * GIFTweaker.java
 *
 * Who   Date       Description
 * ====  =========  ====================================================================
 * WY    28Dec2014  Added snoop() to show GIF image metadata 
 * WY    18Nov2014  Fixed bug with splitFramesEx() disposal method "RESTORE_TO_PREVIOUS" 
 * WY    17Nov2014  Added writeAnimatedGIF(GIFFrame) to work with GIFFrame
 * WY    03Oct2014  Added splitFramesEx2() to split animated GIFs into separate images
 * WY    22Apr2014  Added splitFramesEx() to split animated GIFs into separate images
 * WY    20Apr2014  Added splitFrames() to split animated GIFs into frames
 */

package cafe.image.bmp;

import java.io.IOException;
import java.io.InputStream;

import cafe.io.IOUtils;

/**
 * BMP image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/29/2014
 */
public class BMPTweaker {	
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
	
	public static void snoop(InputStream is) throws IOException {
		// Create a new data transfer object to hold data
		DataTransferObject DTO = new DataTransferObject();
		readHeader(is, DTO);
		
		System.out.println("... BMP snoop starts...");
		System.out.println("Image signature: " + new String(DTO.fileHeader, 0, 2));
		System.out.println("File size: " + IOUtils.readInt(DTO.fileHeader, 2) + " bytes");
		System.out.println("Reserved1 (2 bytes): " + IOUtils.readShort(DTO.fileHeader, 6));
		System.out.println("Reserved2 (2 bytes): " + IOUtils.readShort(DTO.fileHeader, 8));
		System.out.println("Data offset: " + IOUtils.readInt(DTO.fileHeader, 10));
		
		System.out.println("Info header length: " + IOUtils.readInt(DTO.infoHeader, 0));
		System.out.println("Image width: " + IOUtils.readInt(DTO.infoHeader, 4));
		System.out.println("Image heigth: " + IOUtils.readInt(DTO.infoHeader, 8));		
		if(IOUtils.readInt(DTO.infoHeader, 8) > 0)
			System.out.println("Image alignment: ALIGN_BOTTOM_UP");
		else
			System.out.println("Image alignment: ALIGN_TOP_DOWN");
		System.out.println("Number of planes: " + IOUtils.readShort(DTO.infoHeader, 12));
		System.out.println("BitCount (bits per pixel): " + IOUtils.readShort(DTO.infoHeader, 14));
		System.out.println("Compression: " + BmpCompression.fromInt(IOUtils.readInt(DTO.infoHeader, 16)));
		System.out.println("Image size (compressed size of image): " + IOUtils.readInt(DTO.infoHeader, 20) + " bytes");
		System.out.println("Horizontal resolution (Pixels/meter): " + IOUtils.readInt(DTO.infoHeader, 24));
		System.out.println("Vertical resolution (Pixels/meter): " + IOUtils.readInt(DTO.infoHeader, 28));
		System.out.println("Colors used (number of actually used colors): " + IOUtils.readInt(DTO.infoHeader, 32));
		System.out.println("Important colors (number of important colors): " + IOUtils.readInt(DTO.infoHeader, 36));
		
		int bitsPerPixel = IOUtils.readShort(DTO.infoHeader, 14);
		
		if(bitsPerPixel <= 8) {
			readPalette(is, DTO);
			System.out.println("Color map follows");
		}		
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