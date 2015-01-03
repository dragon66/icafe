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
 * GIFReader.java
 *
 * Who   Date       Description
 * ====  =========  ==========================================================
 * WY    02Jan2015  Added getFrames() and getFrameCount() for animated GIF 
 * WY    18Nov2014  Fixed getFrameAsBufferedImageEx() bug with disposal method 
 *                  "RESTORE_TO_PREVIOUS" 
 * WY    03Oct2014  Added getFrameAsBufferedImageEx()
 */

package cafe.image.reader;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import cafe.image.compression.lzw.LZWTreeDecoder;
import cafe.io.IOUtils;

/** 
 * Decodes and shows images in GIF format, supports both Gif87a and Gif89a.
 * The current class doesn't support interlaced or animated GIFs,but can 
 * anyway shows these kinds of images! Supports transparent GIFs!
 *
 * Change log: the LZW decoding part becomes a general purpose class which
 * could be used to decode TIFF image as well.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.1 03/16/2012
 */
public class GIFReader extends ImageReader {	   
	private GifHeader gifHeader;
	private int flags;
	private int flags2;
	private int disposalMethod = -1;
	private boolean transparent;
	private int transparent_color = -1;
	private int colorsUsed;
	private int image_x;
	private int image_y;
	private int logicalScreenWidth;
	private int logicalScreenHeight;
	private Color backgroundColor = new Color(255, 255, 255);
	private int[] globalColorPalette;
	private List<BufferedImage> frames; // To keep track of all the frames
	
	// BufferedImage with the width and height of the logical screen to draw frames upon
	BufferedImage baseImage;
	
	private byte[] decodeLZW(InputStream is) throws Exception
	{
		int dimension = width*height;		
		byte[] temp_ = new byte[dimension];

		int min_code_size = is.read();// The length of the root
		LZWTreeDecoder decoder = new LZWTreeDecoder(is, min_code_size, false);
		decoder.decode(temp_, 0, dimension);
		
		return temp_;
	}
   
	private byte[] decodeLZWInterLaced(InputStream is) throws Exception
	{
		int index = 0;
		int index2 = 0;
		int passParam[] = {0,8,4,8,2,4,1,2};
		int passStart[] = {0,width*passParam[2],width*passParam[4],width*passParam[6]};
		int passInc[]   = {width*passParam[1],width*passParam[3],width*passParam[5],width*passParam[7]};
		int passHeight[]= {((height-1)>>3)+1,((height+3)>>3),((height+1)>>2),((height)>>1)}; 

		/////////////////////////////////////
		int min_code_size = is.read();// The length of the root

		int dimension = width*height;
		byte[] buf = new byte[dimension];
		byte[] temp_ = new byte[dimension];

		LZWTreeDecoder decoder = new LZWTreeDecoder(is, min_code_size, false);
		decoder.decode(buf, 0, dimension);
   
		for (int pass=1;pass<5;pass++)
		{
			// pass 1: start at row 0, scan every 8 rows
			// pass 2: start at row 4, scan every 8 rows
			// pass 3: start at row 2, scan every 4 rows
			// pass 4: start at row 1, scan every 2 rows
			index = passStart[pass-1];
			int inc = (passInc[pass-1]-width);
			for(int row=0;row<passHeight[pass-1];row++,index+=inc)
			{
				for(int col=0;col<width;col++,index++,index2++)
				{
					temp_[index] = buf[index2];
				}
			}
		}
	
		return temp_;
	}
   
	public Color getBackgroundColor() {
		return backgroundColor;
	}
   
	public int getDisposalMethod() {
		return disposalMethod;
	}
   
	/**
	 * Gets the current frame as a BufferedImage. The frames created this may assume
	 * different sizes and are intended to be located at different positions in the
	 * case of an animated GIF. Therefore, the frames may only occupy part of the
	 * logical screen and may also rely on transparency and previous frames to work properly.
	 * <p>
	 * Note: do not mix this method with {@link #read(InputStream) read} 
	 *       or {@link #getFrameAsBufferedImageEx(InputStream) getFrameAsBufferedImageEx}.
	 *       Use them separately.
	 * <p> One way to use this method to retrieve all the frames from an animated GIF:
	 * <pre>
	 * {@code
	 * GIFReader reader = new GIFReader();
	 * InputStream is = new FileInputStream(new File(pathToImage));
	 * List<BufferedImage> frames = new ArrayList<BufferedImage>();
	 * BufferedImage bi = null; 
	 * while((bi = reader.getFrameAsBufferedImage(is) != null)
	 * 	frames.add(bi);
	 * }
	 * </pre>
	 * 
	 * @param is InputStream for the GIF/Animated GIF
	 * @return a BufferedImage for the image or current frame in case of animated GIF
	 * @throws Exception
	 */
	public BufferedImage getFrameAsBufferedImage(InputStream is) throws Exception {
		// Read frame into a byte array
		byte[] pixels = readFrame(is);
		if(pixels == null) return null;
		//Create a BufferedImage
		int[] off = {0};//band offset, we have only one band start at 0
		DataBuffer db = new DataBufferByte(pixels, pixels.length);
		WritableRaster raster = Raster.createInterleavedRaster(db, width, height, width, 1, off, null);
		ColorModel cm = new IndexColorModel(bitsPerPixel, rgbColorPalette.length, rgbColorPalette, 0, false, transparent_color, DataBuffer.TYPE_BYTE);
   	
		return new BufferedImage(cm, raster, false, null);
	}
	
	/**
	 * Gets the current frame as a BufferedImage the same size as the logical screen.
	 * Graphic Control Extension and Image Descriptor parameters are taken into account
	 * When creating the frame. The resulting frame is actually a "composite" one or a
	 * snapshot as seen in an animated GIF. Such frames may not be the same as the frames
	 * created by {@link #getFrameAsBufferedImage(InputStream) getFrameAsBufferedImage}
	 * which could be of different sizes and may also have to rely on the previous frames
	 * to look the same as the frames created here.
	 *  
	 * <p>
	 * Note: do not mix this method with {@link #read(InputStream) read} 
	 *       or {@link #getFrameAsBufferedImage(InputStream) getFrameAsBufferedImage}.
	 *       Use them separately.
	 *  
	 * @param is input stream for the image - single frame or multiple frame animated GIF
	 * @return java BufferedImage or null if there is no more frames
	 * @throws Exception
	 */
	public BufferedImage getFrameAsBufferedImageEx(InputStream is) throws Exception {
		// This single call will trigger the reading of the global scope data
		BufferedImage bi = getFrameAsBufferedImage(is);
		if(bi == null) return null;
		if(baseImage == null)
			baseImage = new BufferedImage(logicalScreenWidth, logicalScreenHeight, BufferedImage.TYPE_INT_ARGB);
		Rectangle area = new Rectangle(image_x, image_y, width, height);
		// Create a backup bufferedImage from the base image for the area of the current frame
		BufferedImage backup = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		backup.setData(baseImage.getData(area));
		/* End of backup */
		Graphics2D g = baseImage.createGraphics();
		// Draw this frame to the base
		g.drawImage(bi, image_x, image_y, null);
		// We need to clone the base image since we are going to dispose it later according to the disposal method
		BufferedImage clone = new BufferedImage(logicalScreenWidth, logicalScreenHeight, BufferedImage.TYPE_INT_ARGB);
		clone.setData(baseImage.getData());
		// Check about disposal method to take action accordingly
		if(disposalMethod == 1 || disposalMethod == 0) // Leave in place or unspecified
			; // No action needed
		else if(disposalMethod == 2) { // Restore to background
			Composite oldComposite = g.getComposite();
			g.setComposite(AlphaComposite.Clear);
			g.fillRect(image_x, image_y, width, height);
			g.setComposite(oldComposite);
		} else if(disposalMethod == 3) { // Restore to previous
			Composite oldComposite = g.getComposite();
			g.setComposite(AlphaComposite.Src);
			g.drawImage(backup, image_x, image_y, null);
			g.setComposite(oldComposite);
		} else { // To be defined - should never come here
			baseImage = new BufferedImage(logicalScreenWidth, logicalScreenHeight, BufferedImage.TYPE_INT_ARGB);
			g = baseImage.createGraphics();
		}	
		
		return clone;
	}
	
	/**
	 * Get the total number of frames read by this GIFReader.
	 *  
	 * @return number of frames read by this GIFReader or -1 if not read yet
	 */
	public int getFrameCount() {
		if(frames != null) // We have already read the image
			return frames.size();
		return -1; // We haven't read the image yet
	}
	
	/**
	 * Get the total frames read by this GIFRreader.
	 * 
	 * @return a list of the total frames read by this GIFRreader or null if not read yet
	 */
	public List<BufferedImage> getFrames() {
		return frames;
	}

	public int getImageX() {
		return image_x;
	}

	public int getImageY() {
		return image_y;
	}
    
	public int getLogicalScreenHeight() {
		return logicalScreenHeight;
	}
    
	public int getLogicalScreenWidth() {
		return logicalScreenWidth;
	}
   
	public int getTransparentColor() {
		if(transparent_color >= 0)
			return rgbColorPalette[transparent_color]&0xffffff; // We only need RGB, no alpha
		return -1;
	}
    
	public boolean isTransparent() {
		return transparent;
	}
    
	private byte[] readFrame(InputStream is) throws Exception {
		// One time read of global scope data
		if(gifHeader == null) {
			if(!readGlobalScopeData(is)) return null;
		}
		// Need to reset some of the fields
		disposalMethod = -1;
		transparent = false;
		transparent_color = -1;
		// End of fields reset
	   
		int image_separator = 0;
	
		do {		   
			image_separator = is.read();
			    
			if(image_separator == -1 || image_separator == 0x3b) { // End of stream 
				System.out.println("End of stream!");
				return null;
			}
			    
			if (image_separator == 0x21) // (!) Extension Block
			{
				int func = is.read();
				int len = is.read();
	
				if (func == 0xf9) // Graphic Control Label - identifies the current block as a Graphic Control Extension
				{
					int packedFields = is.read();
					// Determine the disposal method
					disposalMethod = ((packedFields&0x1c)>>2);
					// Check for transparent color flag
					if((packedFields&0x01) == 0x01)
					{
						IOUtils.skipFully(is,2);
						transparent = true;
						System.out.println("transparent gif...");					 
						transparent_color = is.read();
						len = is.read();// len=0, block terminator!
					} else {
						IOUtils.skipFully(is,3);
						len = is.read();// len=0, block terminator!
					}
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
	
		readImageDescriptor(is);
		   
		boolean hasLocalColorMap = false;
	
		if((flags2&0x80) == 0x80)
		{
			hasLocalColorMap = true;
			// A local color map is present
			System.out.println("local color map is present");
	
			bitsPerPixel = (flags2&0x07)+1;
			colorsUsed = (1<<bitsPerPixel);
	
			System.out.println(colorsUsed + " color image");
	
			readLocalPalette(is, colorsUsed);
		}
		   
		if(!hasLocalColorMap) rgbColorPalette = globalColorPalette;
		   
		if (transparent && transparent_color < colorsUsed)
			rgbColorPalette[transparent_color] &= 0x00ffffff;
			
		if((flags2&0x40) == 0x40) 
		{
			System.out.println("Interlaced gif image!"); 
			return decodeLZWInterLaced(is);
		}
		
		return decodeLZW(is);
	}
    
	private void readGlobalPalette(InputStream is,int num_of_color) throws Exception {
		int index1 = 0;
		int bytes2read = num_of_color*3;
		byte brgb[] = new byte[bytes2read];  
		IOUtils.readFully(is,brgb,0,bytes2read);
	
		globalColorPalette = new int[num_of_color];
				
		for(int i = 0; i < num_of_color; i++)
			globalColorPalette[i]  = ((255<<24)|((brgb[index1++]&0xff)<<16)|((brgb[index1++]&0xff)<<8)|(brgb[index1++]&0xff));
	}
    
	private boolean readGlobalScopeData(InputStream is) throws Exception {
		// Global scope data including header, logical screen descriptor, global colorPalette if presents
		gifHeader = new GifHeader();
		gifHeader.readHeader(is);
		   
		logicalScreenWidth = gifHeader.screen_width;
		logicalScreenHeight = gifHeader.screen_height;
	
		String signature = new String(gifHeader.signature) + new String(gifHeader.version);
		System.out.println(signature);
			
		if ((!signature.equalsIgnoreCase("GIF87a")) && (!signature.equalsIgnoreCase("GIF89a")))
		{
			System.out.println("Only GIF87a and GIF89a is supported by this decoder!");
			return false;
		}
	      
		flags = gifHeader.flags;
					
		if((flags&0x80) == 0x80) // A global color map is present 
		{
			System.out.println("a global color map is present!");
			bitsPerPixel = (flags&0x07)+1;
			colorsUsed = (1<<bitsPerPixel);
	
			System.out.println(colorsUsed + " color image");
	
			// # bits of color resolution, insignificant 
			@SuppressWarnings("unused")
			int bitsPerColor = ((flags&0x70)>>4)+1;
	
			readGlobalPalette(is, colorsUsed);
			int bgcolor = gifHeader.bgcolor&0xff;
			if(bgcolor >= 0 && bgcolor < colorsUsed)
			   backgroundColor = new Color(globalColorPalette[bgcolor]);
		   	}
		   
		   	return true;
	}
    
	public BufferedImage read(InputStream is) throws Exception {
		frames = new ArrayList<BufferedImage>();
		BufferedImage bi = null;
		
		while((bi = getFrameAsBufferedImage(is)) != null) {
			frames.add(bi);
		}
		
		return frames.get(0);
	}
    
	private void readImageDescriptor(InputStream is) throws Exception {	 	
		int nindex = 0;
		byte ides[] = new byte[9];
	
		IOUtils.readFully(is,ides,0,9);
	
		image_x = (ides[nindex++]&0xff)|((ides[nindex++]&0xff)<<8);
		image_y = (ides[nindex++]&0xff)|((ides[nindex++]&0xff)<<8);
		width =  (ides[nindex++]&0xff)|((ides[nindex++]&0xff)<<8);
		height = (ides[nindex++]&0xff)|((ides[nindex++]&0xff)<<8);
		flags2 = ides[nindex++];
	}
    
	private void readLocalPalette(InputStream is,int num_of_color) throws Exception	{
		int index1 = 0;
		int bytes2read = num_of_color*3;
		byte brgb[] = new byte[bytes2read];  
		IOUtils.readFully(is,brgb,0,bytes2read);
	
		rgbColorPalette = new int[num_of_color];
			
		for(int i = 0; i < num_of_color; i++)
			rgbColorPalette[i] = ((255<<24)|((brgb[index1++]&0xff)<<16)|((brgb[index1++]&0xff)<<8)|(brgb[index1++]&0xff));
	}
	
	private static class GifHeader {
		private byte  signature[] = new byte[3];
		private byte  version[] = new byte[3];

		private short screen_width;
		private short screen_height;
		private byte  flags;
		private byte  bgcolor;
		@SuppressWarnings("unused")
		private byte  aspectRatio;
  
		void readHeader(InputStream is) throws Exception {
			int nindex = 0;
			byte bhdr[] = new byte[13];

			IOUtils.readFully(is,bhdr,0,13);
	
			for(int i = 0; i < 3; i++)
				signature[i] = bhdr[nindex++];
	      
			for(int i = 0; i < 3; i++)
				version[i] = bhdr[nindex++];
	      
			screen_width = (short)((bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8));
			screen_height = (short)((bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8));
			flags = bhdr[nindex++];
			bgcolor = bhdr[nindex++];
			aspectRatio = bhdr[nindex++];
			// The end
		}
	}
}