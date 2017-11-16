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
 * GIFTweaker.java
 *
 * Who   Date       Description
 * ====  =========  ====================================================
 * WY    20Feb2017  Fix splitFrames() throws stream closed exception
 * WY    04Apr2016  Rewrite insertXMPApplicationBlock() to leverage GifXMP
 * WY    09Oct2015  Fixed regression bug with splitAnimatedGIF()
 * WY    16Sep2015  Added insertComment() to insert comment extension
 * WY    17Aug2015  Revised to write animated GIF frame by frame
 * WY    06Jul2015  Added insertXMP(InputSream, OutputStream, XMP) 
 * WY    24Jun2015  Renamed splitFramesEx2() to splitAnimatedGIF()
 * WY    30Mar2015  Fixed bug with insertXMP() replacing '\0' with ' '
 * WY    12Mar2015  Cleaned up debugging console output
 * WY    03Mar2015  Added overloaded insertXMPApplicationBlock() with
 *                  XMP string as input
 * WY    13Feb2015  Added insertXMPApplicationBlock() to insert XMP meta data
 * WY    13Feb2015  Added code to readMetadata() Comment and XMP meta data
 * WY    20Jan2015  Renamed snoop() to readMetadata() to work with Metadata.readMetadata()
 * WY    28Dec2014  Added snoop() to show GIF image meta data 
 * WY    18Nov2014  Fixed bug with splitFramesEx() disposal method "RESTORE_TO_PREVIOUS" 
 * WY    17Nov2014  Added writeAnimatedGIF(GIFFrame) to work with GIFFrame
 * WY    03Oct2014  Added splitFramesEx2() to split animated GIFs into separate images
 * WY    22Apr2014  Added splitFramesEx() to split animated GIFs into separate images
 * WY    20Apr2014  Added splitFrames() to split animated GIFs into frames
 */

package com.icafe4j.image.gif;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.ImageIO;
import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.gif.GifXMP;
import com.icafe4j.image.meta.image.Comments;
import com.icafe4j.image.meta.xmp.XMP;
import com.icafe4j.image.writer.GIFWriter;
import com.icafe4j.image.writer.ImageWriter;
import com.icafe4j.io.IOUtils;
import com.icafe4j.string.StringUtils;
import com.icafe4j.util.ArrayUtils;

/**
 * GIF image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/16/2014
 */
public class GIFTweaker {
	// Define constants
	public static final byte IMAGE_SEPARATOR = 0x2c; // ","
	public static final byte IMAGE_TRAILER = 0x3b; // ";"
	public static final byte EXTENSION_INTRODUCER = 0x21; // "!"
	public static final byte GRAPHIC_CONTROL_LABEL = (byte)0xf9;
	public static final byte APPLICATION_EXTENSION_LABEL = (byte)0xff;
	public static final byte COMMENT_EXTENSION_LABEL = (byte)0xfe;
	public static final byte TEXT_EXTENSION_LABEL = 0x01;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(GIFTweaker.class);		
	
	// Data transfer object for multiple thread support
	private static class DataTransferObject {
		private byte[] header;	
		private byte[] logicalScreenDescriptor;
		private byte[] globalPalette;
		private byte[] imageDescriptor;
		private Map<MetadataType, Metadata> metadataMap;
		private Comments comments;
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
				LOGGER.error("Unexpected end of stream!");
				return false;
			}
		    
			if (image_separator == 0x21) { // (!) Extension Block
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

		if((DTO.imageDescriptor[8]&0x80) == 0x80) {
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
	
	/**
	 * This is intended to be called after writing all the frames if we write
	 * an animated GIF frame by frame.
	 * 
	 * @param os OutputStream for the animated GIF
	 * @throws Exception
	 */
	public static void finishWrite(OutputStream os) throws Exception {	   	
    	os.write(IMAGE_TRAILER);
		os.close();    	
	}
	
	public static void insertComments(InputStream is, OutputStream os, List<String> comments) throws IOException {
		// Read and copy header and LSD
 		// Create a new data transfer object to hold data
 		DataTransferObject DTO = new DataTransferObject();
 		readHeader(is, DTO);
 		readLSD(is, DTO);
 		os.write(DTO.header);
 		os.write(DTO.logicalScreenDescriptor);
		if((DTO.logicalScreenDescriptor[4]&0x80) == 0x80) {
			int bitsPerPixel = (DTO.logicalScreenDescriptor[4]&0x07)+1;
			int colorsUsed = (1 << bitsPerPixel);
			
			readGlobalPalette(is, colorsUsed, DTO);
			os.write(DTO.globalPalette);
		}
		int numOfComments = comments.size();
		for(int i = 0; i < numOfComments; i++) {
			os.write(EXTENSION_INTRODUCER);
			os.write(COMMENT_EXTENSION_LABEL);
			byte[] commentBytes = comments.get(i).getBytes();
			int numBlocks = commentBytes.length/0xff;
			int leftOver = commentBytes.length % 0xff;
			int offset = 0;
			if(numBlocks > 0) {
				for(int block = 0; block < numBlocks; block++) {
					os.write(0xff);
					os.write(commentBytes, offset, 0xff);
					offset += 0xff;
				}
			}
			if(leftOver > 0) {
				os.write(leftOver);
				os.write(commentBytes, offset, leftOver);
			}
			os.write(0);			
		}
		// Copy the rest of the input stream
 		byte buf[] = new byte[10240]; // 10K
 		int bytesRead = is.read(buf);
 		
 		while(bytesRead != -1) {
 			os.write(buf, 0, bytesRead);
 			bytesRead = is.read(buf);
 		}
	}
	
	public static void insertXMPApplicationBlock(InputStream is, OutputStream os, XMP xmp) throws IOException {
		// Read and copy header and LSD
 		// Create a new data transfer object to hold data
 		DataTransferObject DTO = new DataTransferObject();
 		readHeader(is, DTO);
 		readLSD(is, DTO);
 		os.write(DTO.header);
 		os.write(DTO.logicalScreenDescriptor);

		if((DTO.logicalScreenDescriptor[4]&0x80) == 0x80) {
			int bitsPerPixel = (DTO.logicalScreenDescriptor[4]&0x07)+1;
			int colorsUsed = (1 << bitsPerPixel);
			
			readGlobalPalette(is, colorsUsed, DTO);
			os.write(DTO.globalPalette);
		}
	
		// Insert XMP
 		xmp.write(os);
 		
 		// Copy the rest of the input stream
 		byte[] buf = new byte[10240]; // 10K
 		int bytesRead = is.read(buf);
 		
 		while(bytesRead != -1) {
 			os.write(buf, 0, bytesRead);
 			bytesRead = is.read(buf);
 		}
	}
	
	public static void insertXMPApplicationBlock(InputStream is, OutputStream os, byte[] xmp) throws IOException {
		insertXMPApplicationBlock(is, os, new GifXMP(xmp));
    }
	
	/**
     * This is intended to be called first when writing an animated GIF
     * frame by frame.
     * 
     * @param writer GIFWriter to write the animated GIF
     * @param os OutputStream for the animated GIF
     * @param logicalScreenWidth width of the logical screen. If it is less than
     *        or equal zero, it will be determined from the first frame
     * @param logicalScreenHeight height of the logical screen. If it is less than
     *        or equal zero, it will be determined from the first frame
     * @throws Exception
     */
	public static void prepareForWrite(GIFWriter writer, OutputStream os, int logicalScreenWidth, int logicalScreenHeight) throws Exception {
		writer.prepareForWrite(os, logicalScreenWidth, logicalScreenHeight);
	}
	
	public static void insertXMPApplicationBlock(InputStream is, OutputStream os, String xmp) throws IOException {
		insertXMPApplicationBlock(is, os, new GifXMP(xmp));
	}
	
	private static boolean readFrame(InputStream is, DataTransferObject DTO) throws IOException {
		// Need to reset some of the fields
		int disposalMethod = -1;
		// End of fields reset
	   
		int image_separator = 0;
	
		do {		   
			image_separator = is.read();
			    
			if(image_separator == -1 || image_separator == 0x3b) { // End of stream 
				return false;
			}
			    
			if (image_separator == 0x21) { // (!) Extension Block
				int func = is.read();
				int len = is.read();
				
				if (func == 0xf9) {
					// Graphic Control Label - identifies the current block as a Graphic Control Extension
					//<<Start of graphic control block>>
					int packedFields = is.read();
					// Determine the disposal method
					disposalMethod = ((packedFields&0x1c)>>2);
					switch(disposalMethod) {
						case GIFFrame.DISPOSAL_UNSPECIFIED:
							// Frame disposal method: UNSPECIFIED
						case GIFFrame.DISPOSAL_LEAVE_AS_IS:
							// Frame disposal method: LEAVE_AS_IS
						case GIFFrame.DISPOSAL_RESTORE_TO_BACKGROUND:
							// Frame disposal method: RESTORE_TO_BACKGROUND
						case GIFFrame.DISPOSAL_RESTORE_TO_PREVIOUS:
							// Frame disposal method: RESTORE_TO_PREVIOUS
							break;
						default:
							//throw new RuntimeException("Invalid GIF frame disposal method: " + disposalMethod);
					}
					// Check for transparent color flag
					if((packedFields&0x01) == 0x01) {
						IOUtils.skipFully(is, 2);
						// Transparent GIF
						is.read(); // Transparent color index
						len = is.read();// len=0, block terminator!
					} else {
						IOUtils.skipFully(is, 3);
						len = is.read();// len=0, block terminator!
					}
					// <<End of graphic control block>>
				} else if(func == 0xff) { // Application block
					// Application block
					byte[] xmp_id = {'X', 'M', 'P', ' ', 'D', 'a', 't', 'a', 'X', 'M', 'P' };
					byte[] temp = new byte[0x0B];
					IOUtils.readFully(is, temp);
					// If we have XMP data
					if(Arrays.equals(xmp_id, temp)) {
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						len = is.read();
						while(len != 0) {
							bout.write(len);
							temp = new byte[len];
							IOUtils.readFully(is, temp);
							bout.write(temp);
							len = is.read();
						}
						byte[] xmp = bout.toByteArray();
						// Remove the magic trailer - 258 bytes minus the block terminator
						len = xmp.length - 257;
						if(len > 0) // Put it into the Meta data map
							DTO.metadataMap.put(MetadataType.XMP, new GifXMP(ArrayUtils.subArray(xmp, 0, len)));
						len = 0; // We're already at block terminator
					} else 
						len = is.read(); // Block terminator					
				} else if(func == 0xfe) { // Comment block
					// Comment block
					byte[] comment = new byte[len];
					IOUtils.readFully(is, comment);
					if(DTO.comments == null) DTO.comments = new Comments();
					DTO.comments.addComment(comment);
					len = is.read();
				}
				// GIF87a specification mentions the repetition of multiple length
				// blocks while GIF89a gives no specific description. For safety, here
				// a while loop is used to check for block terminator!
				while(len != 0) {
					IOUtils.skipFully(is, len);
					len = is.read();// len=0, block terminator!
				}
			}
		} while(image_separator != 0x2c); // ","
		
		// <<Start of new frame>>		
		readImageDescriptor(is, DTO);
		
		int colorsUsed = 1 << ((DTO.logicalScreenDescriptor[4]&0x07)+1);
		
		byte[] localPalette = null;
		
		if((DTO.imageDescriptor[8]&0x80) == 0x80) {
			// A local color map is present
			int bitsPerPixel = (DTO.imageDescriptor[8]&0x07)+1;
			// Colors used in local palette
			colorsUsed = (1<<bitsPerPixel);
			localPalette = new byte[3*colorsUsed];
		    is.read(localPalette);
		}		
	
		if(localPalette == null) localPalette = DTO.globalPalette;	
		is.read(); // LZW Minimum Code Size		
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
		DTO.header = new byte[6]; // GIFXXa
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
		// Create a new data transfer object to hold data
		DataTransferObject DTO = new DataTransferObject();
		// Created a Map for the Meta data
		DTO.metadataMap = new HashMap<MetadataType, Metadata>();
		
		readHeader(is, DTO);
		readLSD(is, DTO);
		
		// Packed byte
		if((DTO.logicalScreenDescriptor[4]&0x80) == 0x80) {
			// A global color map is present 
			int bitsPerPixel = (DTO.logicalScreenDescriptor[4]&0x07)+1;
			int colorsUsed = (1 << bitsPerPixel);
			
			readGlobalPalette(is, colorsUsed, DTO);			
		}
		
		while(readFrame(is, DTO)) {
			;	
		}
		
		if(DTO.comments != null)
			DTO.metadataMap.put(MetadataType.COMMENT, DTO.comments);		
		
		return DTO.metadataMap;		
	}
	
	/**
	 * Split an animated GIF into individual frames and save them as GIF images.
	 * The split is "as is" as no frame decoding and other operations involved.
	 * As individual frames are often related to each other with regard to the
	 * animated GIF image, this sometimes leads to funny looking GIFs.
	 * 
	 * @param is input GIF image stream
	 * @param outputFilePrefix optional output file name prefix  
	 */	
	public static void splitFrames(InputStream is, String outputFilePrefix) throws Exception {
		// Create a new data transfer object to hold data
		DataTransferObject DTO = new DataTransferObject();
		
		readHeader(is, DTO);
		readLSD(is, DTO);
		
		if((DTO.logicalScreenDescriptor[4]&0x80) == 0x80) {
			// A global color map is present 
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
	 * Split animated GIF to individual GIF images.
	 * 
	 * @param animatedGIF input animated GIF stream
	 * @param writer ImageWriter for the output frame
	 * @param outputFilePrefix optional prefix for the output image
	 * @throws Exception
	 * 
	 * @deprecated Use {@link #splitAnimatedGIF(InputStream, ImageWriter, String) splitAnimagedGIF} instead.
	 */
	@Deprecated
	public static void splitFrames2(InputStream animatedGIF, ImageWriter writer, String outputFilePrefix) throws Exception {
		// Create a GIFReader to read GIF frames	
		FrameReader reader = new FrameReader();
		// Create a GIFWriter or other writers to write the frames
		ImageType imageType = writer.getImageType();
		// This single call will trigger the reading of the global scope data
		GIFFrame frame = reader.getGIFFrame(animatedGIF);
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
		
		while(frame != null) {
			int image_x = frame.getLeftPosition();
			int image_y = frame.getTopPosition();
			/* Backup the area to be override by this frame */
			int imageWidth = frame.getFrameWidth();
			int imageHeight = frame.getFrameHeight();
			Rectangle area = new Rectangle(image_x, image_y, imageWidth, imageHeight);
			// Create a backup bufferedImage
			BufferedImage backup = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			backup.setData(baseImage.getData(area));
			/* End of backup */
			// Draw this frame to the base
			g.drawImage(frame.getFrame(), image_x, image_y, null);
			// Write the base to file
			String outFileName = baseFileName + frameCount++;
			FileOutputStream os = new FileOutputStream(outFileName + "." + imageType.getExtension());			
			//writer.setImageMeta(builder.build());
			writer.write(baseImage, os);
			// Assume no transparency
			//builder.transparent(false);
			// Check about disposal method to take action accordingly
			if(frame.getDisposalMethod() == 1 || frame.getDisposalMethod() == 0) // Leave in place or unspecified
				; // No action needed
			else if(frame.getDisposalMethod() == 2) { // Restore to background
				Composite oldComposite = g.getComposite();
				g.setComposite(AlphaComposite.Clear);
				g.fillRect(image_x, image_y, imageWidth, imageHeight);
				g.setComposite(oldComposite);
				//g.setColor(reader.getBackgroundColor());
				//g.fillRect(0, 0, logicalScreenWidth, logicalScreenHeight);
				//builder.transparent(true);
			} else if(frame.getDisposalMethod() == 3) { // Restore to previous
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
			frame = reader.getGIFFrame(animatedGIF);
		}
	}
	
	/** 
	 * Split animated GIF to individual GIF images.
	 * 
	 * @param animatedGIF input animated GIF stream
	 * @param outputFilePrefix optional path prefix for the output image
	 * @throws Exception
	 */
	public static void splitAnimatedGIF(InputStream animatedGIF, String outputFilePrefix) throws Exception {
		splitAnimatedGIF(animatedGIF, ImageIO.getWriter(ImageType.GIF), outputFilePrefix);
	}

	/** 
	 * Split animated GIF to individual frames. The output image
	 * format is determined by the ImageWriter parameter.
	 * 
	 * @param animatedGIF input animated GIF stream
	 * @param writer ImageWriter for the output frame
	 * @param outputFilePrefix optional path prefix for the output image
	 * @throws Exception
	 */
	public static void splitAnimatedGIF(InputStream animatedGIF, ImageWriter writer, String outputFilePrefix) throws Exception {
		// Create a GIFReader to read GIF frames	
		FrameReader reader = new FrameReader();
		// Create a GIFWriter or other writers to write the frames
		ImageType imageType = writer.getImageType();
		GIFFrame frame = reader.getGIFFrameEx(animatedGIF);
		
		int frameCount = 0;		
		String baseFileName = StringUtils.isNullOrEmpty(outputFilePrefix)?"frame_":outputFilePrefix + "_frame_";
		
		while(frame != null) {
			// Write the frame to file
			String outFileName = baseFileName + frameCount++;
			FileOutputStream os = new FileOutputStream(outFileName + "." + imageType.getExtension());			
			writer.write(frame.getFrame(), os);
			frame = reader.getGIFFrameEx(animatedGIF);
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
	public static void writeAnimatedGIF(BufferedImage[] images, int[] delays, int loopCount, OutputStream os) throws Exception {
		writeAnimatedGIF(images, delays, loopCount, ImageParam.getBuilder().applyDither(true).build(), os);
	}
	
	/**
	 * Create animated GIFs from a series of BufferedImage
	 * 
	 * @param images an array of BufferedImage 
	 * @param delays delay times in millisecond between the frames
	 * @param loopCount loop count for the animated GIF
	 * @param param internal GIFWriter write parameters
	 * @param os OutputStream to write the image
	 */
	public static void writeAnimatedGIF(BufferedImage[] images, int[] delays, int loopCount, ImageParam param, OutputStream os) throws Exception {
		GIFWriter writer = new GIFWriter();
		writer.setImageParam(param);
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
		writeAnimatedGIF(frames, loopCount, ImageParam.getBuilder().applyDither(true).build(), os);
	}
	
	/**
	 * Write an array of GIFFrame as an animated GIF. This method gives a user more control over the frame
	 * parameters such as delay, frame position, disposal method etc.
	 * 
	 * @param frames array of GIFFrame
	 * @param loopCount loopCount for the animated GIF
	 * @param os OutputStream to write the image
	 * @param param internal GIFWriter write parameters
	 * @throws Exception
	 */
	public static void writeAnimatedGIF(GIFFrame[] frames, int loopCount, ImageParam param, OutputStream os) throws Exception {
		GIFWriter writer = new GIFWriter();
		writer.setImageParam(param);
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
	
	public static void writeFrame(GIFWriter writer, OutputStream os, BufferedImage frame) throws Exception {
		writer.writeFrame(os, frame);
	}
	
	public static void writeFrame(GIFWriter writer, OutputStream os, BufferedImage frame, int delay) throws Exception {
		writer.writeFrame(os, frame, delay);
	}
	
	public static void writeFrame(GIFWriter writer, OutputStream os, GIFFrame frame) throws Exception {
		writer.writeFrame(os, frame);
	}
	
	private GIFTweaker() {}
}