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
 * WY    20Jan2015  Revised to work with Metadata.showMetadata()
 * WY    28Dec2014  Added snoop() to show GIF image metadata 
 * WY    18Nov2014  Fixed bug with splitFramesEx() disposal method "RESTORE_TO_PREVIOUS" 
 * WY    17Nov2014  Added writeAnimatedGIF(GIFFrame) to work with GIFFrame
 * WY    03Oct2014  Added splitFramesEx2() to split animated GIFs into separate images
 * WY    22Apr2014  Added splitFramesEx() to split animated GIFs into separate images
 * WY    20Apr2014  Added splitFrames() to split animated GIFs into frames
 */

package cafe.image.gif;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.image.ImageType;
import cafe.image.options.GIFOptions;
import cafe.image.reader.GIFReader;
import cafe.image.writer.GIFWriter;
import cafe.image.writer.ImageWriter;
import cafe.io.IOUtils;
import cafe.string.StringUtils;

/**
 * GIF image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/16/2014
 */
public class GIFTweaker {	
	// Data transfer object for multiple thread support
	private static class DataTransferObject {
		private byte[] header;	
		private byte[] logicalScreenDescriptor;
		private byte[] globalPalette;
		private byte[] imageDescriptor;
	}
	
	private static boolean copyFrame(InputStream is, OutputStream os, DataTransferObject DTO) throws IOException {
		// Copy global scope data
		os.write(DTO.header);
		os.write(DTO.logicalScreenDescriptor);
		
		if(DTO.globalPalette != null) os.write(DTO.globalPalette);
		
		int image_separator = 0;

		do {
		    image_separator = is.read();
		    if(image_separator == 0x3b) return false;
		    
			if(image_separator == -1) { // End of stream 
				System.out.println("Unexpected end of stream!");
				return false;
			}
		    
			if (image_separator == 0x21) // (!) Extension Block
			{
				int func = is.read();
				os.write(0x21);
				os.write(func);
				
				int len = 0;
				
				while((len = is.read()) > 0) {
					os.write(len);
					byte[] block = new byte[len];
					is.read(block);
					os.write(block);
				}
				
				os.write(0);		
			}
		} while(image_separator != 0x2c); // ","

		readImageDescriptor(is, DTO);
		
		os.write(0x2c);
		os.write(DTO.imageDescriptor);

		if((DTO.imageDescriptor[8]&0x80) == 0x80)
	    {
			int bitsPerPixel = (DTO.imageDescriptor[8]&0x07)+1;
			int colorsUsed = (1<<bitsPerPixel);

			byte[] localPalette = new byte[3*colorsUsed];
		    is.read(localPalette);
		    os.write(localPalette);
		}		
		
		// Copy compressed image data
		os.write(is.read());
		int len = 0;
		
		while((len = is.read()) > 0) {
			os.write(len);
			byte[] block = new byte[len];
			is.read(block);
			os.write(block);
		}
		
		os.write(0);		
		os.write(0x3b);
		
		os.close();
		
		return true;
	}
	
	private static boolean readFrame(InputStream is, DataTransferObject DTO) throws IOException {
		// Need to reset some of the fields
		int disposalMethod = -1;
		boolean transparent = false;
		int transparent_color = -1;
		// End of fields reset
	   
		int image_separator = 0;
	
		do {		   
			image_separator = is.read();
			    
			if(image_separator == -1 || image_separator == 0x3b) { // End of stream 
				System.out.println("<<END OF STREAM>>");
				return false;
			}
			    
			if (image_separator == 0x21) // (!) Extension Block
			{
				int func = is.read();
				int len = is.read();
				
				System.out.print("Extension block (length: " + len + ", function 0x" + Integer.toHexString(func) +")");
	
				if (func == 0xf9) // Graphic Control Label - identifies the current block as a Graphic Control Extension
				{
					System.out.println(" - Graphic control block");
					System.out.println("<<Start of graphic control block>>");
					int packedFields = is.read();
					// Determine the disposal method
					disposalMethod = ((packedFields&0x1c)>>2);
					switch(disposalMethod) {
						case GIFOptions.DISPOSAL_UNSPECIFIED:
							System.out.println("Frame disposal method: UNSPECIFIED");
							break;
						case GIFOptions.DISPOSAL_LEAVE_AS_IS:
							System.out.println("Frame disposal method: LEAVE_AS_IS");
							break;
						case GIFOptions.DISPOSAL_RESTORE_TO_BACKGROUND:
							System.out.println("Frame disposal method: RESTORE_TO_BACKGROUND");
							break;
						case GIFOptions.DISPOSAL_RESTORE_TO_PREVIOUS:
							System.out.println("Frame disposal method: RESTORE_TO_PREVIOUS");
							break;
						default:
							System.out.println("Invalid GIF frame disposal method: " + disposalMethod);
					}
					// Check for transparent color flag
					if((packedFields&0x01) == 0x01)
					{
						IOUtils.skipFully(is, 2);
						transparent = true;
						System.out.println("Transparent GIF");					 
						transparent_color = is.read();
						System.out.println("Transparent color index: " + transparent_color);
						len = is.read();// len=0, block terminator!
					} else {
						IOUtils.skipFully(is, 3);
						len = is.read();// len=0, block terminator!
					}
					System.out.println("<<End of graphic control block>>");
				} else if(func == 0xff) { // Application label
					System.out.println(" - Application block");
					IOUtils.skipFully(is, len);
					len = is.read(); // Block terminator
				} else if(func == 0xfe) { // Comment block
					System.out.println(" - Comment block");
					byte[] comment = new byte[len];
					IOUtils.readFully(is, comment);
					System.out.println("Comment: " + new String(comment));
					len = is.read();
				} else {
					System.out.println();
				}
				// GIF87a specification mentions the repetition of multiple length
				// blocks while GIF89a gives no specific description. For safety, here
				// a while loop is used to check for block terminator!
				while(len != 0) 
				{
					IOUtils.skipFully(is, len);
					len = is.read();// len=0, block terminator!
				} 
			}
		} while(image_separator != 0x2c); // ","
		
		System.out.println("<<Start of new frame>>");
		
		readImageDescriptor(is, DTO);
		
		System.out.println("Image left position: " + ((DTO.imageDescriptor[0]&0xff)|((DTO.imageDescriptor[1]&0xff)<<8)));
		System.out.println("Image top position: " + ((DTO.imageDescriptor[2]&0xff)|((DTO.imageDescriptor[3]&0xff)<<8)));
		System.out.println("Image width: " + ((DTO.imageDescriptor[4]&0xff)|((DTO.imageDescriptor[5]&0xff)<<8)));
		System.out.println("Image height: " + ((DTO.imageDescriptor[6]&0xff)|((DTO.imageDescriptor[7]&0xff)<<8)));
		
		int colorsUsed = 1 << ((DTO.logicalScreenDescriptor[4]&0x07)+1);
		
		byte[] localPalette = null;
		
		if((DTO.imageDescriptor[8]&0x80) == 0x80) {
			// A local color map is present
			System.out.println("A local color map is present");
			int bitsPerPixel = (DTO.imageDescriptor[8]&0x07)+1;
			System.out.println("BitsPerPixel: " + bitsPerPixel);
			colorsUsed = (1<<bitsPerPixel);
			System.out.println("Colors used in local palette: " + colorsUsed);

			localPalette = new byte[3*colorsUsed];
		    is.read(localPalette);
		}		
	
		if(localPalette == null) localPalette = DTO.globalPalette;
		
		if (transparent && transparent_color < colorsUsed) {
			int redIndex = transparent_color*3;
			System.out.println("Transparent color: " + StringUtils.intToHexStringMM(localPalette[redIndex]&0xff<<16
					                                   |localPalette[redIndex + 1]&0xff<<8
					                                   |localPalette[redIndex + 2]&0xff));
		}
			
		if((DTO.imageDescriptor[8]&0x40) == 0x40) 
			System.out.println("Interlaced image"); 
		
		System.out.println("LZW Minimum Code Size: " + is.read());
		
		int len = 0;
		
		while((len = is.read()) > 0) {
			byte[] block = new byte[len];
			is.read(block);
		}
		
		return true;
	}
	
	private static void readGlobalPalette(InputStream is, int num_of_color, DataTransferObject DTO) throws IOException {
		 DTO.globalPalette = new byte[num_of_color*3];
		 is.read(DTO.globalPalette);
	}
	
	private static void readHeader(InputStream is, DataTransferObject DTO) throws IOException {
		DTO.header = new byte[6];
		is.read(DTO.header);
	}
	
	private static void readImageDescriptor(InputStream is, DataTransferObject DTO) throws IOException {
		DTO.imageDescriptor = new byte[9];
	    is.read(DTO.imageDescriptor);
	}
	
	private static void readLSD(InputStream is, DataTransferObject DTO) throws IOException {
		DTO.logicalScreenDescriptor = new byte[7];
		is.read(DTO.logicalScreenDescriptor);
	}
	
	public static Map<MetadataType, Metadata> readMetadata(InputStream is) throws IOException {
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		// Create a new data transfer object to hold data
		DataTransferObject DTO = new DataTransferObject();
		
		readHeader(is, DTO);
		readLSD(is, DTO);
		
		System.out.println("... GIF snoop starts ...");
		System.out.println("<<Start of lobal parameters>>");
		System.out.print(new String(DTO.header, 0, 3)); // GIF signature
		System.out.println(new String(DTO.header, 3, 3)); // GIF version
		System.out.println("Logical screen width: " + (short)((DTO.logicalScreenDescriptor[0]&0xff)|((DTO.logicalScreenDescriptor[1]&0xff)<<8)));
		System.out.println("Logical screen_height: " + (short)((DTO.logicalScreenDescriptor[2]&0xff)|((DTO.logicalScreenDescriptor[3]&0xff)<<8)));
		
		// Packed byte
		if((DTO.logicalScreenDescriptor[4]&0x80) == 0x80) 
		{
			// A global color map is present 
			System.out.println("A global color map is present");
			int bitsPerPixel = (DTO.logicalScreenDescriptor[4]&0x07)+1;
			int colorsUsed = (1 << bitsPerPixel);
			System.out.println("Colors used in global palette: " + colorsUsed);
			
			readGlobalPalette(is, colorsUsed, DTO);			
		}
		
		System.out.println("Color resolution: " + (DTO.logicalScreenDescriptor[4]>>4&0x07 + 1));
		System.out.println("Global color map sorted: " + ((DTO.logicalScreenDescriptor[4]>>3&0x01) == 1));
		System.out.println("Background color index: " + DTO.logicalScreenDescriptor[5]);
		System.out.println("Aspect ration: " + DTO.logicalScreenDescriptor[6]);
		System.out.println("<<End of global parameters>>");
		System.out.println("*********************************************************");
		
		// Time to read frames one by one
		int frameCount = 0;
		
		while(readFrame(is, DTO)) {
			System.out.println("<<End of frame #" + frameCount + ">>");
			System.out.println("*********************************************************");
			frameCount++;	
		}
		
		System.out.println("Total number of frames: " + frameCount);
		System.out.println("... GIF snoop ends ...");
		
		return metadataMap;
	}
	
	/**
	 * Split a multiple frame GIF into individual frames and save them as GIF images.
	 * The split is "literally" since no frame decoding and other operations involved.
	 * This sometimes leads to funny looking GIFs.
	 * 
	 * @param is input GIF image stream
	 * @param outputFilePrefix optional output file name prefix  
	 */	
	public static void splitFrames(InputStream is, String outputFilePrefix) throws Exception {
		// Create a new data transfer object to hold data
		DataTransferObject DTO = new DataTransferObject();
		
		readHeader(is, DTO);
		readLSD(is, DTO);
		
		if((DTO.logicalScreenDescriptor[4]&0x80) == 0x80) // A global color map is present 
		{
			int bitsPerPixel = (DTO.logicalScreenDescriptor[4]&0x07)+1;
			int colorsUsed = (1 << bitsPerPixel);
			readGlobalPalette(is, colorsUsed, DTO);			
		}
		
		int frameCount = 0;
		String outFileName;
		FileOutputStream os;
		
		do {
			outFileName = StringUtils.isNullOrEmpty(outputFilePrefix)?"frame_" + frameCount++:outputFilePrefix + "_frame_" + frameCount++;
			os = new FileOutputStream(outFileName + ".gif");
		} while(copyFrame(is, os, DTO));
		
		os.close(); // Close the last file stream in order to delete it
		// Delete the last file which is invalid
		new File(outFileName + ".gif").delete();
	}
	
	/**
	 * Split animated GIF to individual images
	 * 
	 * @param is input animated GIF stream
	 * @param writer ImageWriter for the output frame
	 * @param outputFilePrefix optional prefix for the output image
	 * @throws Exception
	 * 
	 * @deprecated Use {@link #splitFramesEx2(InputStream, ImageWriter, String) splitFramesEx2} instead.
	 */
	@Deprecated
	public static void splitFramesEx(InputStream is, ImageWriter writer, String outputFilePrefix) throws Exception {
		// Create a GIFReader to read GIF frames	
		GIFReader reader = new GIFReader();
		// Create a GIFWriter or other writers to write the frames
		ImageType imageType = writer.getImageType();
		// This single call will trigger the reading of the global scope data		
		BufferedImage bi = reader.getFrameAsBufferedImage(is);
		// After reading the global scope data, we can retrieve values such logical screen width and height etc.
		int logicalScreenWidth = reader.getLogicalScreenWidth();
		int logicalScreenHeight = reader.getLogicalScreenHeight();
		// Then create a BufferedImage with the width and height of the logical screen and draw frames upon it
		BufferedImage baseImage = new BufferedImage(logicalScreenWidth, logicalScreenHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = baseImage.createGraphics();
		//g.setColor(reader.getBackgroundColor());
		//g.fillRect(0, 0, logicalScreenWidth, logicalScreenHeight);
		
		//ImageParam.ImageParamBuilder builder = new ImageParam.ImageParamBuilder();
		//builder.transparent(reader.isTransparent()).transparentColor(reader.getBackgroundColor().getRGB());
  
		int frameCount = 0;		
		String baseFileName = StringUtils.isNullOrEmpty(outputFilePrefix)?"frame_":outputFilePrefix + "_frame_";
		
		while(bi != null) {
			int image_x = reader.getImageX();
			int image_y = reader.getImageY();
			/* Backup the area to be override by this frame */
			int imageWidth = bi.getWidth();
			int imageHeight = bi.getHeight();
			Rectangle area = new Rectangle(image_x, image_y, imageWidth, imageHeight);
			// Create a backup bufferedImage
			BufferedImage backup = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			backup.setData(baseImage.getData(area));
			/* End of backup */
			// Draw this frame to the base
			g.drawImage(bi, image_x, image_y, null);
			// Write the base to file
			String outFileName = baseFileName + frameCount++;
			FileOutputStream os = new FileOutputStream(outFileName + "." + imageType.getExtension());			
			//writer.setImageMeta(builder.build());
			writer.write(baseImage, os);
			// Assume no transparency
			//builder.transparent(false);
			// Check about disposal method to take action accordingly
			if(reader.getDisposalMethod() == 1 || reader.getDisposalMethod() == 0) // Leave in place or unspecified
				; // No action needed
			else if(reader.getDisposalMethod() == 2) { // Restore to background
				Composite oldComposite = g.getComposite();
				g.setComposite(AlphaComposite.Clear);
				g.fillRect(image_x, image_y, imageWidth, imageHeight);
				g.setComposite(oldComposite);
				//g.setColor(reader.getBackgroundColor());
				//g.fillRect(0, 0, logicalScreenWidth, logicalScreenHeight);
				//builder.transparent(true);
			} else if(reader.getDisposalMethod() == 3) { // Restore to previous
				Composite oldComposite = g.getComposite();
				g.setComposite(AlphaComposite.Src);
				g.drawImage(backup, image_x, image_y, null);
				g.setComposite(oldComposite);			
			} else { // To be defined - should never come here
				baseImage = new BufferedImage(logicalScreenWidth, logicalScreenHeight, BufferedImage.TYPE_INT_ARGB);
				g = baseImage.createGraphics();
				//g.setColor(reader.getBackgroundColor());
				//g.fillRect(0, 0, logicalScreenWidth, logicalScreenHeight);
				//builder.transparent(true);
			}
			// Read another frame if we have more
			bi = reader.getFrameAsBufferedImage(is);
		}
	}

	/** 
	 * Split animated GIF to individual images
	 * 
	 * @param is input animated GIF stream
	 * @param writer ImageWriter for the output frame
	 * @param outputFilePrefix optional prefix for the output image
	 * @throws Exception
	 */
	public static void splitFramesEx2(InputStream is, ImageWriter writer, String outputFilePrefix) throws Exception {
		// Create a GIFReader to read GIF frames	
		GIFReader reader = new GIFReader();
		// Create a GIFWriter or other writers to write the frames
		ImageType imageType = writer.getImageType();
		BufferedImage bi = reader.getFrameAsBufferedImageEx(is);
	
		int frameCount = 0;		
		String baseFileName = StringUtils.isNullOrEmpty(outputFilePrefix)?"frame_":outputFilePrefix + "_frame_";
		
		while(bi != null) {
			// Write the frame to file
			String outFileName = baseFileName + frameCount++;
			FileOutputStream os = new FileOutputStream(outFileName + "." + imageType.getExtension());			
			writer.write(bi, os);
			bi = reader.getFrameAsBufferedImageEx(is);
		}
	}
	
	/**
	 * Create animated GIFs from a series of BufferedImage
	 * 
	 * @param images an array of BufferedImage 
	 * @param delays delay times in millisecond between the frames
	 * @param loopCount loop count for the animated GIF
	 * @param os OutputStream to write the image
	 */
	public static void writeAnimatedGIF(BufferedImage[] images, int[] delays, int loopCount,  OutputStream os) throws Exception {
		GIFWriter writer = new GIFWriter();
		writer.setLoopCount(loopCount);
		writer.writeAnimatedGIF(images, delays, os);
	}
	
	public static void writeAnimatedGIF(BufferedImage[] images, int[] delays, OutputStream os) throws Exception {
		writeAnimatedGIF(images, delays, 0, os);
	}
	
	/**
	 * Write an array of GIFFrame as an animated GIF. This method gives a user more control over the frame
	 * parameters such as delay, frame position, disposal method etc.
	 * 
	 * @param frames array of GIFFrame
	 * @param loopCount loopCount for the animated GIF
	 * @param os OutputStream to write the image
	 * @throws Exception
	 */
	public static void writeAnimatedGIF(GIFFrame[] frames, int loopCount, OutputStream os) throws Exception {
		GIFWriter writer = new GIFWriter();
		writer.setLoopCount(loopCount);
		writer.writeAnimatedGIF(frames, os);
	}
	
	public static void writeAnimatedGIF(GIFFrame[] frames, OutputStream os) throws Exception {
		writeAnimatedGIF(frames, 0, os);
	}
	
	public static void writeAnimatedGIF(List<GIFFrame> frames, int loopCount, OutputStream os) throws Exception {
		writeAnimatedGIF(frames.toArray(new GIFFrame[0]), loopCount, os);
	}
	
	public static void writeAnimatedGIF(List<GIFFrame> frames, OutputStream os) throws Exception {
		writeAnimatedGIF(frames, 0, os);
	}
	
	private GIFTweaker() {}
}