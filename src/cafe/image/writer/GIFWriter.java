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
 * GIFWriter.java
 *
 * Who   Date       Description
 * ====  =======    ==========================================================
 * WY    17Nov2014  Revised writeGraphicControlBlock() to take more parameters
 * WY    17Nov2014  Added new writeFrame() method to take more parameters
 * WY    16Apr2014  Added writeFrame() to support animated GIF
 */

package cafe.image.writer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*; 
import java.util.Arrays;
import java.util.List;

import cafe.image.ImageType;
import cafe.image.gif.GIFFrame;
import cafe.image.gif.GIFTweaker;
import cafe.image.util.IMGUtils;

/**
 * A light-weight GIF encoder implemented using tree search as demonstrated 
 * by Bob Montgomery in "LZW compression used to encode/decode a GIF file"
 *
 * @author Wen Yu, yuwen_66@yahoo.com 
 * @version 1.1 12/05/2007
 */
public class GIFWriter extends ImageWriter {	
	// Fields
	private int codeLen;
	private int codeIndex;
	private int clearCode;
	private int endOfImage;
	private int bufIndex = 0;	
	private int empty_bits = 0x08;
	
	private int bitsPerPixel = 0x08;

	private byte bytes_buf[] = new byte[256];
	private int[] colorPalette;
 
	private static int MASK[] = {0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff};
	
	private static Dimension getLogicalScreenSize(BufferedImage[] images) {
		// Determine the logical screen dimension assuming all the frames have the same
		// left and top coordinates (0, 0)
		int logicalScreenWidth = 0;
		int logicalScreenHeight = 0;
		
		for(BufferedImage image : images) {
			if(image.getWidth() > logicalScreenWidth)
				logicalScreenWidth = image.getWidth();
			if(image.getHeight() > logicalScreenHeight)
				logicalScreenHeight = image.getHeight();
		}
		
		return new Dimension(logicalScreenWidth, logicalScreenHeight);
	}
	
	private static Dimension getLogicalScreenSize(GIFFrame[] frames) {
		// Determine the logical screen dimension given all the frames with different
		// left and top coordinates.
		int logicalScreenWidth = 0;
		int logicalScreenHeight = 0;
		
		for(GIFFrame frame : frames) {
			int frameRightPosition = frame.getFrameWidth() + frame.getLeftPosition();
			int frameBottomPosition = frame.getFrameHeight() + frame.getTopPosition();
			if(frameRightPosition > logicalScreenWidth)
				logicalScreenWidth = frameRightPosition;
			if(frameBottomPosition > logicalScreenHeight)
				logicalScreenHeight = frameBottomPosition;
		}
		
		return new Dimension(logicalScreenWidth, logicalScreenHeight);
	}
	
	/**
	 * A child is made up of a parent(or prefix) code plus a suffix color
	 * and siblings are strings with a common parent(or prefix) and different
	 * suffix colors
	 */
	int child[] = new int[4097];
	
	int siblings[] = new int[4097];
	int suffix[] = new int[4097];
	
	private int logicalScreenWidth;
	private int logicalScreenHeight;
	
	private boolean animated;
	
	private int loopCount;
	
	public GIFWriter() {}
	
	private void encode(byte[] pixels, OutputStream os) throws Exception 
	{
		// Define local variables
		int parent = 0;
		int son = 0;
		int brother = 0;
		int color = 0;
		int index = 0;
		int dimension = pixels.length;

		// Write out the length of the root
		os.write(bitsPerPixel = (bitsPerPixel == 1)?2:bitsPerPixel);
		// Initialize the encoder
		init_encoder(bitsPerPixel);
		// Tell the decoder to initialize the string table
		send_code_to_buffer(clearCode, os);
        // Get the first color and assign it to parent
		parent = (pixels[index++]&0xff);

		while (index < dimension)
		{
			color = (pixels[index++]&0xff);
			son = child[parent];

			if ( son > 0){
				if (suffix[son] == color) {
					parent = son;
				} else {
					brother = son;
					while (true)
					{
						if (siblings[brother] > 0)
						{
							brother = siblings[brother];
							if (suffix[brother] == color)
						    {
							   parent = brother;
							   break;
						    }
						} else {
							siblings[brother] = codeIndex;
							suffix[codeIndex] = color;
							send_code_to_buffer(parent,os);
							parent = color;
							codeIndex++;
               				// Check code length
				            if(codeIndex > ((1<<codeLen)))
			                {
								if (codeLen == 12) 
 			                    {
				                    send_code_to_buffer(clearCode,os);
				                    init_encoder(bitsPerPixel);
			                    } else
					                codeLen++;
			                }
							break;
						}
					}
				}
			} else {
				child[parent] = codeIndex;
				suffix[codeIndex] = color;
				send_code_to_buffer(parent,os);
				parent = color;
				codeIndex++;
				// Check code length
				if(codeIndex > ((1<<codeLen)))
			    {
                   if (codeLen == 12) 
			       { 
				       send_code_to_buffer(clearCode,os);
				       init_encoder(bitsPerPixel);
			       } else
					   codeLen++;
			    }
			}
		}
		// Send the last color code to the buffer
		send_code_to_buffer(parent,os);
		// Send the endOfImage code to the buffer
		send_code_to_buffer(endOfImage,os);
		// Flush the last code buffer
		flush_buf(os, bufIndex+1);
    }
    
    private void flush_buf(OutputStream os, int len) throws Exception
	{
		os.write(len);
		os.write(bytes_buf,0,len);
		// Clear the bytes buffer
		bufIndex = 0;
		Arrays.fill(bytes_buf, 0, 0xff, (byte)0x00);
	}
    
    @Override
    public ImageType getImageType() {
    	return ImageType.GIF;
    }
    
    private void init_encoder(int bitsPerPixel )
	{
		clearCode = 1 << bitsPerPixel;
	    endOfImage = clearCode + 1;
  	    codeLen = bitsPerPixel + 1;
	    codeIndex = endOfImage + 1;
	    // Reset arrays
	    Arrays.fill(child, 0);
		Arrays.fill(siblings, 0);
		Arrays.fill(suffix, 0);
    }
    
    // Translate codes into bytes
    private void send_code_to_buffer(int code, OutputStream os)throws Exception
	{
		int temp = codeLen;
		// Shift the code to the left of the last byte in bytes_buf
        bytes_buf[bufIndex] |= ((code&MASK[empty_bits])<<(8-empty_bits));
		code >>= empty_bits;
        temp -= empty_bits;
        // If the code is longer than the empty_bits
		while (temp > 0)
		{
			if ( ++bufIndex >= 0xff)
			{
				flush_buf(os,0xff);
			}
			bytes_buf[bufIndex] |= (code&0xff);
			code >>= 8;
			temp -= 8;
		}
		empty_bits = -temp;
	}
    
    public void setLoopCount(int loopCount) {
    	this.loopCount = loopCount;
    }

    // The entry point for all the image writers
    protected void write(int[] pixels, int imageWidth, int imageHeight, OutputStream os) throws Exception {	
    	// Write GIF header
		writeHeader(os, true);
		// Set logical screen size
		logicalScreenWidth = imageWidth;
		logicalScreenHeight = imageHeight;
		// We only need to write one frame, so disable animated flag
    	animated = false;
		// Write the image frame
		writeFrame(pixels, imageWidth, imageHeight, 0, 0, 0, os, true);
		// Make a clean end up of the image
		os.write(GIFTweaker.IMAGE_TRAILER);
		os.close();
    }

    /**
     * Writes an array of BufferedImage as an animated GIF
     * 
     * @param images an array of BufferedImage
     * @param delays delays in millisecond for each frame
     * @param os OutputStream for the animated GIF
     * @throws Exception
     */
    public void writeAnimatedGIF(BufferedImage[] images, int[] delays, OutputStream os) throws Exception {
    	// Header first
    	writeHeader(os, true);
    	
    	Dimension logicalScreenSize = getLogicalScreenSize(images);
    	
    	logicalScreenWidth = logicalScreenSize.width;
    	logicalScreenHeight = logicalScreenSize.height;
    	// We are going to write animated GIF, so enable animated flag
    	animated = true;
    	
    	for(int i = 0; i < images.length; i++) {
    		// Retrieve image dimension
			int imageWidth = images[i].getWidth();
			int imageHeight = images[i].getHeight();
			int[] pixels = IMGUtils.getRGB(images[i]);//images[i].getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
			
			if(i == 0) writeFrame(pixels, imageWidth, imageHeight, 0, 0, delays[i], os, true);
			else writeFrame(pixels, imageWidth, imageHeight, 0, 0, delays[i], os, false);
    	}
    	
    	os.write(GIFTweaker.IMAGE_TRAILER);
		os.close();    	
    }
    
    /**
     * Writes an array of GIFFrame as an animated GIF
     * 
     * @param frames an array of GIFFrame
     * @param os OutputStream for the animated GIF
     * @throws Exception
     */
    public void writeAnimatedGIF(GIFFrame[] frames, OutputStream os) throws Exception {
    	// Header first
    	writeHeader(os, true);
    	
    	Dimension logicalScreenSize = getLogicalScreenSize(frames);
    	
    	logicalScreenWidth = logicalScreenSize.width;
    	logicalScreenHeight = logicalScreenSize.height;
    	// We are going to write animated GIF, so enable animated flag
    	animated = true;
    	
    	for(int i = 0; i < frames.length; i++) {
    		// Retrieve image dimension
			int imageWidth = frames[i].getFrameWidth();
			int imageHeight = frames[i].getFrameHeight();
			int[] pixels = IMGUtils.getRGB(frames[i].getFrame());//images[i].getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
			if(frames[i].getTransparencyFlag() == GIFFrame.TRANSPARENCY_INDEX_SET && frames[i].getTransparentColor() != -1) {
				int transColor = (frames[i].getTransparentColor() & 0x00ffffff);				
				for(int j = pixels.length - 1; j > 0; j--) {
					int pixel = (pixels[j] & 0x00ffffff);
					if(pixel == transColor) pixels[j] = pixel; 
				}
			}
			if(i == 0) writeFrame(pixels, imageWidth, imageHeight, frames[i].getLeftPosition(), frames[i].getTopPosition(),
					frames[i].getDelay(), frames[i].getDisposalMethod(), frames[i].getUserInputFlag(), os, true);
			else writeFrame(pixels, imageWidth, imageHeight, frames[i].getLeftPosition(), frames[i].getTopPosition(), 
					frames[i].getDelay(), frames[i].getDisposalMethod(), frames[i].getUserInputFlag(), os, false);
    	}
    	
    	os.write(GIFTweaker.IMAGE_TRAILER);
		os.close();    	
    }
    
    /**
     * Writes a list of GIFFrame as an animated GIF
     * 
     * @param frames a list of GIFFrame
     * @param os OutputStream for the animated GIF
     * @throws Exception
     */
    public void writeAnimatedGIF(List<GIFFrame> frames, OutputStream os) throws Exception {
    	writeAnimatedGIF(frames.toArray(new GIFFrame[0]), os);
    }

	private void writeFrame(int[] pixels, int imageWidth, int imageHeight, int imageLeftPosition, int imageTopPosition,  int delay, int disposalMethod, int userInputFlag, OutputStream os, boolean writeLSD) throws Exception
	{	
		// Reset empty_bits
    	empty_bits = 0x08;
    	
    	int transparent_color = -1;
		int[] colorInfo; 
		
		// Reduce colors, if the color depth is less than 8 bits, reduce colors
		// to the actual bits needed, otherwise reduce to 8 bits.
		byte[] newPixels = new byte[imageWidth*imageHeight];
	    colorPalette = new int[256];
	    
	    colorInfo = IMGUtils.checkColorDepth(pixels, newPixels, colorPalette);
		
	    if(colorInfo[0] > 0x08)
			colorInfo = IMGUtils.reduceColorsFloydSteinberg(pixels, imageWidth, imageHeight, bitsPerPixel, newPixels, colorPalette);
			//colorInfo = IMGUtils.reduceColors(pixels, bitsPerPixel, newPixels, colorPalette);
		
	    bitsPerPixel = colorInfo[0];
	    
	    transparent_color = colorInfo[1];
	    
	    if(bitsPerPixel > 0x08) bitsPerPixel = 0x08;
        
	    int num_of_color = 1<<bitsPerPixel;
	    
	    if(writeLSD) {
	    	// Logical screen descriptor
			byte  flags = (byte)0x88;//0b10001000 (having sorted global color map) - To be updated
			byte  bgcolor = 0x00;// To be set
			byte  aspectRatio = 0x00;			
			int colorResolution = 0x07;
			// Set GIF logical screen descriptor parameters
			flags |= ((colorResolution<<4)|(bitsPerPixel - 1)); 			
			if(transparent_color >= 0)
				bgcolor = (byte)transparent_color;
			// Write logical screen descriptor
			writeLSD(os, (short)logicalScreenWidth, (short)logicalScreenHeight, flags, bgcolor, aspectRatio);
			// Write the global colorPalette
			writePalette(os, num_of_color);
			if(animated)// Write Netscape extension block
				writeNetscapeApplicationBlock(os, loopCount);
	    }		
		
      	// Output the graphic control block
	    writeGraphicControlBlock(os, delay, (byte)transparent_color, disposalMethod, userInputFlag);
        // Output image descriptor
        if(writeLSD)
        	writeImageDescriptor(os, imageWidth, imageHeight, imageLeftPosition, imageTopPosition, -1);
        else {
        	writeImageDescriptor(os, imageWidth, imageHeight, imageLeftPosition, imageTopPosition, bitsPerPixel - 1);
        	// Write local colorPalette
        	writePalette(os, num_of_color);
        }
        // LZW encode the image
        encode(newPixels, os);
		/** Write out a zero length data sub-block */
		os.write(0x00);
	}
	
	private void writeFrame(int[] pixels, int imageWidth, int imageHeight, int imageLeftPosition, int imageTopPosition,  int delay, OutputStream os, boolean writeLSD) throws Exception
	{	
    	writeFrame(pixels, imageWidth, imageHeight, imageLeftPosition, imageTopPosition, delay, GIFFrame.DISPOSAL_RESTORE_TO_BACKGROUND, GIFFrame.USER_INPUT_NONE, os, writeLSD);
    }
	
	// Unit of delay is supposed to be in millisecond
    private void writeGraphicControlBlock(OutputStream os, int delay, byte transparent_color, int disposalMethod, int userInputFlag) throws Exception
    {
    	// Scale delay
    	delay = Math.round(delay/10.0f);
    	
        byte[] buf = new byte[8];
		buf[0] = GIFTweaker.EXTENSION_INTRODUCER; // Extension introducer
		buf[1] = GIFTweaker.GRAPHIC_CONTROL_LABEL; // Graphic control label
		buf[2] = 0x04; // Block size
		// Add disposalMethod and userInputFlag
		buf[3] |= (((disposalMethod&0x07) << 2)|((userInputFlag&0x01) << 1));
		buf[4] = (byte)(delay&0xff);// Delay time
		buf[5] = (byte)((delay>>8)&0xff);
		buf[6] = transparent_color;
		buf[7] = 0x00;
		
		if(transparent_color >= 0) // Add transparency indicator
			buf[3] |= 0x01;
		
		os.write(buf, 0, 8);
	}
	
	private void writeHeader(OutputStream os, boolean newFormat) throws IOException {
		// 6 bytes: GIF signature (always "GIF") plus GIF version ("87a" or "89a")
		if(newFormat)
			os.write("GIF89a".getBytes());
		else
			os.write("GIF87a".getBytes());			
	}
	
    private void writeImageDescriptor(OutputStream os, int imageWidth, int imageHeight, int imageLeftPosition, int imageTopPosition, int colorTableSize) throws Exception
    {
		byte imageDescriptor[] = new byte[10];
		imageDescriptor[0] = GIFTweaker.IMAGE_SEPARATOR;// Image separator ","
		imageDescriptor[1] = (byte)(imageLeftPosition&0xff);// Image left position
		imageDescriptor[2] = (byte)((imageLeftPosition>>8)&0xff);
		imageDescriptor[3] = (byte)(imageTopPosition&0xff);// Image top position
		imageDescriptor[4] = (byte)((imageTopPosition>>8)&0xff);
        imageDescriptor[5] = (byte)(imageWidth&0xff);
		imageDescriptor[6] = (byte)((imageWidth>>8)&0xff);
		imageDescriptor[7] = (byte)(imageHeight&0xff);
		imageDescriptor[8] = (byte)((imageHeight>>8)&0xff);
		imageDescriptor[9] = (byte)0x20;//0b00100000 - Packed fields
		
		if(colorTableSize >= 0) // Local color table will follow
			imageDescriptor[9] |= (1<<7|colorTableSize);
		
		os.write(imageDescriptor, 0, 10);
	}
    
    // Write logical screen descriptor
	private void writeLSD(OutputStream os, short screen_width, short screen_height, short flags, byte bgcolor, byte aspectRatio) throws IOException
	{
		byte[] descriptor = new byte[7]; 
		// Screen_width
	    descriptor[0] = (byte)(screen_width&0xff);
	    descriptor[1] = (byte)((screen_width>>8)&0xff);
	    // Screen_height
	    descriptor[2] = (byte)(screen_height&0xff);
	    descriptor[3] = (byte)((screen_height>>8)&0xff);
		// Global flags
        descriptor[4] = (byte)(flags&0xff);
		// Background color
        descriptor[5] = (byte)(bgcolor&0xff);
		// AspectRatio
	    descriptor[6] = (byte)(aspectRatio&0xff);
	    
	    os.write(descriptor);
	}
	
    private void writeNetscapeApplicationBlock(OutputStream os, int loopCounts) throws Exception
    {
    	byte[] buf = new byte[19];
 		buf[0] = GIFTweaker.EXTENSION_INTRODUCER; // Extension introducer
 		buf[1] = GIFTweaker.APPLICATION_EXTENSION_LABEL; // Application extension label
 		buf[2] = 0x0b; // Block size
 		buf[3] = 'N'; // Application Identifier (8 bytes)
 		buf[4] = 'E';
 		buf[5] = 'T';
 		buf[6] = 'S';
 		buf[7] = 'C';
 		buf[8] = 'A';
 		buf[9] = 'P';
 		buf[10]= 'E';
 		buf[11]= '2';// Application Authentication Code (3 bytes)
 		buf[12]= '.';
 		buf[13]= '0';
 		buf[14]= 0x03;
 		buf[15]= 0x01;
 		buf[16]= (byte)(loopCounts&0xff); // Loop counts
 		buf[17]= (byte)((loopCounts>>8)&0xff);
 		buf[18]= 0x00; // Block terminator 
 		
 		os.write(buf);
    }
    
    private void writePalette(OutputStream os, int num_of_color) throws Exception
	{
        int index = 0;
        byte colors[] = new byte[num_of_color*3];
        
        for (int i=0; i<num_of_color; i++)
	    {
		  colors[index++] = (byte)(((colorPalette[i]>>16)&0xff));
		  colors[index++] = (byte)(((colorPalette[i]>>8)&0xff));
		  colors[index++] = (byte)(colorPalette[i]&0xff);
	    }
        
	    os.write(colors, 0, num_of_color*3);
	}
}