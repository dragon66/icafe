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
 * IMGUtils.java
 *
 * Who   Date       Description
 * ====  =========  ==============================================================
 * WY    03Feb2015  Added createThumbnail() to create a thumbnail from an image
 * WY    27Jan2015  Added createThumbnail8BIM() to wrap a BufferedImage to _8BIM
 * WY    22Jan2015  Factored out guessImageType(byte[])
 * WY    24Dec2014  Rename CMYK2RGB() to iccp2rgbRaster()
 * WY    17Dec2014  Bug fix for rgb2bilevelDither() to bypass transparent pixels
 * WY    03Dec2014  Bug fix for getRGB() to fall back to BufferedImage.getRGB()
 * WY    26Nov2014  Changed rgb2bilevel() to take into account transparency
 * WY    03Nov2014  Added CMYK2RGB() to convert CMYK raster to RGB raster
 * WY    22Sep2014  Added guessImageType() to auto detect image type
 * WY    13Aug2014  Added RGB2YCCK_Inverted() to support YCCK JPEG
 * WY    05May2014  Added getRGB() and getRGB2() to replace BufferedImage.getRGB()
 */

package cafe.image.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cafe.image.ImageIO;
import cafe.image.ImageType;
import cafe.image.meta.adobe.ImageResourceID;
import cafe.image.meta.adobe._8BIM;
import cafe.image.writer.ImageWriter;
import cafe.image.writer.JPEGWriter;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.util.IntHashtable;

/** 
 * This utility class contains static methods 
 * to help with image manipulation and IO. 
 * <p>
 * Changed checkColorDepth method to remove potential problems with alpha.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.1.2 04/02/2012
 */
public class IMGUtils {
	// Image magic number constants
	private static byte[] BM = {0x42, 0x4d}; // BM
	private static byte[] GIF = {0x47, 0x49, 0x46, 0x38}; // GIF8
	private static byte[] PNG = {(byte)0x89, 0x50, 0x4e, 0x47}; //.PNG
	private static byte[] TIFF_II = {0x49, 0x49, 0x2a, 0x00}; // II*.
	private static byte[] TIFF_MM = {0x4d, 0x4d, 0x00, 0x2a}; //MM.*
	private static byte[] JPG = {(byte)0xff, (byte)0xd8, (byte)0xff};
	private static byte[] PCX = {0x0a};
	private static byte[] JPG2000 = {0x00, 0x00, 0x00, 0x0C};
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(IMGUtils.class);
		
	/**
	 * Check the color depth of the image and if the color depth is within 8 bits, (i.e.,
	 * a indexed color image), a color map and an index array for the image are also created.
	 * If the color depth is bigger than 8, only the returned color depth is relevant. 
	 * <p>
	 * This method is not yet completely finished. The colorPalette is currently not ordered.
	 * A sorting method could be used to find the most used color from the colorFreq array, 
	 * but more tricks will have to be done in order to re-map the newPixels array to the
	 * ordered colorPalette.
	 * 
	 * @param rgbTriplets an int array of RGB triplets for the image
	 * @param newPixels a byte array to hold the color map indexes for the image
	 * @param colorPalette the color map for the image
	 * @return an int array holding the color depth and the transparent color index if any
	 */
	public static int[] checkColorDepth(int[] rgbTriplets, byte[] newPixels, final int[] colorPalette)
	{
		int index = 0;
		int temp = 0;
		int bitsPerPixel = 1;
		int transparent_index = -1;// Transparent color index
		int transparent_color = -1;// Transparent color
		int[] colorInfo = new int[2];// Return value
		
		// Inner class to hold a RGB color index	
		class ColorEntry
		{
			int index = 0;
			
			ColorEntry(int index)
			{
				this.index = index;				
			}
		}
		
		IntHashtable<ColorEntry> rgbHash = new IntHashtable<ColorEntry>(1023);
				
		ColorEntry rgbEntry = null;
	
		for (int i = 0; i < rgbTriplets.length; i++)
		{
			temp = (rgbTriplets[i]&0x00ffffff);

            if((rgbTriplets[i] >>> 24) < 0x80 )// Transparent
			{
				if (transparent_index < 0)
				{
					transparent_index = index;
				    transparent_color = temp;// Remember transparent color
				}
				temp = Integer.MAX_VALUE;
			}	

			rgbEntry = rgbHash.get(temp);
			
			if (rgbEntry!=null)
			{
				newPixels[i] = (byte)rgbEntry.index;
			}
			else
			{
				if(index > 0xff)
				{// More than 256 colors, have to reduce
				 // Colors before saving as an indexed color image
					colorInfo[0] = 24;
					return colorInfo;
				}
				rgbHash.put(temp, new ColorEntry(index));
				newPixels[i] = (byte)index;
				colorPalette[index++] = ((0xff<<24)|temp);
			}
		}
		if(transparent_index>=0)// This line could be used to set a different background color
			colorPalette[transparent_index] = transparent_color;
		// Return the actual bits per pixel and the transparent color index if any
		while ((1<<bitsPerPixel)<index)  bitsPerPixel++;
		
		colorInfo[0] = bitsPerPixel;
		colorInfo[1] = transparent_index;

        return colorInfo;
	}
	
	/**
	 * Creates a thumbnail from image input stream
	 * @param is InputStream for the image
	 * @return thumbnail as a BufferedImage
	 * @throws IOException
	 */
	public static BufferedImage createThumbnail(InputStream is) throws IOException {
		BufferedImage original = null;
		if(is instanceof RandomAccessInputStream) {
			RandomAccessInputStream rin = (RandomAccessInputStream)is;
			long streamPointer = rin.getStreamPointer();
			rin.seek(streamPointer);
			original = javax.imageio.ImageIO.read(rin);
			if(original == null) { // Java ImageIO failed, try our own stuff
				rin.seek(streamPointer); // Reset stream pointer
				try {
					original = ImageIO.read(rin);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Reset the stream pointer
			rin.seek(streamPointer);
		} else {
			original = javax.imageio.ImageIO.read(is);
		}		
		int imageWidth = original.getWidth();
		int imageHeight = original.getHeight();
		int thumbnailWidth = 160;
		int thumbnailHeight = 120;
		if(imageWidth < imageHeight) { 
			// Swap thumbnail width and height to keep a relative aspect ratio
			int temp = thumbnailWidth;
			thumbnailWidth = thumbnailHeight;
			thumbnailHeight = temp;
		}			
		if(imageWidth < thumbnailWidth) thumbnailWidth = imageWidth;			
		if(imageHeight < thumbnailHeight) thumbnailHeight = imageHeight;
		BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = thumbnail.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(original, 0, 0, thumbnailWidth, thumbnailHeight, null);
				
		return thumbnail;
	}
	
	/**
	 * Wraps a BufferedImage inside a Photoshop _8BIM
	 * @param thumbnail input thumbnail image
	 * @return a Photoshop _8BMI
	 * @throws IOException
	 */
	public static _8BIM createThumbnail8BIM(BufferedImage thumbnail) throws IOException {
		// Create memory buffer to write data
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		// Compress the thumbnail
		ImageWriter writer = new JPEGWriter();
		try {
			writer.write(thumbnail, bout);
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte[] data = bout.toByteArray();
		bout.reset();
		// Write thumbnail format
		IOUtils.writeIntMM(bout, 1); // 1 = kJpegRGB. We are going to write JPEG format thumbnail
		// Write thumbnail dimension
		int width = thumbnail.getWidth();
		int height = thumbnail.getHeight();
		IOUtils.writeIntMM(bout, width);
		IOUtils.writeIntMM(bout, height);
		// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
		int bitsPerPixel = 24;
		int planes = 1;
		int widthBytes = (width*bitsPerPixel + 31)/32*4;
		IOUtils.writeIntMM(bout, widthBytes);
		// Total size = widthbytes * height * planes
		IOUtils.writeIntMM(bout, widthBytes*height*planes);
		// Size after compression. Used for consistency check.
		IOUtils.writeIntMM(bout, data.length);
		IOUtils.writeShortMM(bout, bitsPerPixel);
		IOUtils.writeShortMM(bout, planes);
		bout.write(data);
		// Create 8BIM
		_8BIM bim = new _8BIM(ImageResourceID.THUMBNAIL_RESOURCE_PS5, "thumbnail", bout.toByteArray());
	
		return bim;
	}
	
	/**
	 * Dither gray-scale image using Floyd-Steinberg error diffusion
	 *
	 * @param gray input gray-scale image array - also as output BW image array
	 * @param mask a mask array for transparent pixels - 0 transparent, 1 opaque
	 * @param width image width
	 * @param height image height
	 * @param threshold gray-scale threshold to convert to BW image
	 * @param err_limit limit of the error (range 0-255 inclusive)
	 */	
	public static void dither_FloydSteinberg(byte[] gray, byte[] mask, int width, int height, int threshold, int err_limit)
	{
		// Define error arrays
		// Errors for the current line
		int[] tempErr;
		int[] thisErr = new int[width + 2];
		// Errors for the following line
		int[] nextErr = new int[width + 2];
			
		java.util.Random random = new java.util.Random();
		
		for(int i = 0; i < width + 2; i++)
		{
			thisErr[i] = random.nextInt(3) - 1;
		}
			
		for (int row = 0, index = 0; row < height; row++)
		{
			for (int col = 0; col < width; index++, col++) {
				if(mask[index] == 0) { 
					// make transparency color white (Assume WHITE_IS_ZERO)
					gray[index] = 0; 
					continue; 
				}
				// Diffuse errors
				int intensity = (gray[index]&0xff) + thisErr[col + 1];
				if (intensity > 255) intensity = 255;
				else if (intensity < 0) intensity = 0;
				
				// Find the nearest color index	- black or white
				int newIntensity = 0;
				if(intensity <= threshold) {
					gray[index] = 1;
					newIntensity = 0;
				} else {
					gray[index] = 0;
					newIntensity = 255;
				}
				
				// Find errors
				int err = intensity - newIntensity;
				if (err > err_limit) err = err_limit;
				else if (err < -err_limit)	err = -err_limit;
				
				// Diffuse error
				thisErr[col + 2] += ((err*7)>>4);
				nextErr[col    ] += ((err*3)>>4);
				nextErr[col + 1] += ((err*5)>>4);
				nextErr[col + 2] += ((err)>>4);
			}			
			// We have finished one row, switch the error arrays
			tempErr = thisErr;
			thisErr = nextErr;
			nextErr = tempErr;
			// Clear the error arrays
			Arrays.fill(nextErr, 0);
		}
	}
	
	/**
	 * Floyd-Steinberg dithering, based on PPMQuant.c by Jef Poskanzer <jef@acme.com>.
	 * For simplicity, only forward error diffusion is implemented.
	 */
	public static void dither_FloydSteinberg(int[] rgbTriplet, int width, int height, byte[] newPixels, int no_of_color, 
		                                      int[] colorPalette, int transparent_index)
	{
		int index = 0, index1 = 0, err1, err2, err3, red, green, blue;
		int err_limit = 8;// Error threshold
        // Define error arrays
		// Errors for the current line
		int[] tempErr;
		int[] thisErrR = new int[width + 2];
		int[] thisErrG = new int[width + 2];
		int[] thisErrB = new int[width + 2];
        // Errors for the following line
		int[] nextErrR = new int[width + 2];
		int[] nextErrG = new int[width + 2];
		int[] nextErrB = new int[width + 2];

		java.util.Random random = new java.util.Random();

        for(int i = 0; i < width + 2; i++)
        {
            thisErrR[i] = random.nextInt(3) - 1;
            thisErrG[i] = random.nextInt(3) - 1;
            thisErrB[i] = random.nextInt(3) - 1;
        }

		InverseColorMap invMap;

		invMap = new InverseColorMap();
		invMap.createInverseMap(no_of_color, colorPalette);

		for (int row = 0; row < height; row++)
		{
			for (int col = 0; col < width; index1++, col++)
			{				
				if((rgbTriplet[index1] >>> 24) < 0x80 )// Transparent, no dither
		        {
				     newPixels[index1] = (byte)transparent_index;	
					 continue;
		        }
				// Diffuse errors
				red = ((rgbTriplet[index1]&0xff0000)>>>16) + thisErrR[col + 1];
				if (red > 255) red = 255;
			    else if (red < 0) red = 0;
	  		    
				green = ((rgbTriplet[index1]&0x00ff00)>>>8) + thisErrG[col + 1];
				if (green > 255) green = 255;
			    else if (green < 0) green = 0;
				
				blue = (rgbTriplet[index1]&0x0000ff) + thisErrB[col + 1];
				if (blue > 255) blue = 255;
			    else if (blue < 0) blue = 0;

                // Find the nearest color index
			    index = invMap.getNearestColorIndex(red, green, blue);
				
				newPixels[index1] = (byte)index;// The colorPalette index for this pixel

				// Find errors for different channels
		        err1 = red   - ((colorPalette[index]>>16)&0xff);// Red channel
				if (err1 > err_limit) err1 = err_limit;
				else if (err1 < -err_limit)	err1 = -err_limit;
		        err2 = green - ((colorPalette[index]>>8)&0xff);// Green channel
				if (err2 > err_limit) err2 = err_limit;
				else if (err2 < -err_limit)	err2 = -err_limit;
		        err3 = blue  -  (colorPalette[index]&0xff);// Blue channel
				if (err3 > err_limit) err3 = err_limit;
				else if (err3 < -err_limit)	err3 = -err_limit;
		        // Diffuse error
				// Red
                thisErrR[col + 2] += ((err1*7)>>4);
				nextErrR[col    ] += ((err1*3)>>4);
				nextErrR[col + 1] += ((err1*5)>>4);
				nextErrR[col + 2] += ((err1)>>4);
                // Green
                thisErrG[col + 2] += ((err2*7)>>4);
				nextErrG[col    ] += ((err2*3)>>4);
				nextErrG[col + 1] += ((err2*5)>>4);
				nextErrG[col + 2] += ((err2)>>4);
				// Blue
				thisErrB[col + 2] += ((err3*7)>>4);
				nextErrB[col    ] += ((err3*3)>>4);
				nextErrB[col + 1] += ((err3*5)>>4);
				nextErrB[col + 2] += ((err3)>>4);
		    }
			// We have finished one row, switch the error arrays
			tempErr = thisErrR;
			thisErrR = nextErrR;
            nextErrR = tempErr;

			tempErr = thisErrG;
			thisErrG = nextErrG;
			nextErrG = tempErr;
	
	        tempErr = thisErrB;
			thisErrB = nextErrB;
			nextErrB = tempErr;
            // Clear the error arrays
			Arrays.fill(nextErrR, 0);
			System.arraycopy(nextErrR, 0, nextErrG, 0, width + 2);
		    System.arraycopy(nextErrR, 0, nextErrB, 0, width + 2);
		}
	}
	
	// Convert RGB to CMYK w/o alpha the cheap way
	public static byte[] easyRGB2CMYK(int[] rgb, boolean hasAlpha) {
		byte[] cmyk = (hasAlpha? new byte[rgb.length*5] : new byte[rgb.length*4]);
	
		for(int i = 0, index = 0; i < rgb.length; i++) {
			int red = ((rgb[i] >> 16) & 0xff);
			int green = ((rgb[i] >> 8) & 0xff);
			int blue = (rgb[i] & 0xff);
	        float c = 1 - (red/255.0f);
	        float m = 1 - (green/255.0f);
	        float y = 1 - (blue/255.0f);
	        float tempK = 1.0f;
	        
	        if(c < tempK) tempK = c;
	        if(m < tempK) tempK = m;
	        if(y < tempK) tempK = y;
	        
	        if(tempK == 1.0f) cmyk[index++] = cmyk[index++] = cmyk[index++] = 0;
	        else {
	         	cmyk[index++] = (byte)((c - tempK)/(1 - tempK)*255);
	        	cmyk[index++] = (byte)((m - tempK)/(1 - tempK)*255);
	        	cmyk[index++] = (byte)((y - tempK)/(1 - tempK)*255);
	        }
	        
	        cmyk[index++] = (byte)(tempK*255);
	        
	        if(hasAlpha) cmyk[index++] = (byte)((rgb[i]>>24)&0xff);
		}
		
		return cmyk;
	}
	
	/**
     * Entry point for image filtering operation. Simply delegates to the underlying ImageFilter.
	 */
	public static BufferedImage filterImage(BufferedImageOp bufferedImageOp, BufferedImage srcImg, BufferedImage dstImg)
	{
		return bufferedImageOp.filter(srcImg, dstImg);
	}
	
	// Helper method to get the bit depth for a gray-scale image
	public static int getBitDepth(byte[] input, boolean hasAlpha) {
		//
		int[] freq = new int[256];
		
		if(hasAlpha) {
			for(int i = (input.length<<1) - 2; i >= 0; i-=2) {
				freq[input[i]&0xff]++;
			}
		} else {
			for(int i = input.length - 1; i >= 0; i--) {
				freq[input[i]&0xff]++;
			}
		}
		
		int numOfColor = 0;
		
		for(int j = 0; j < freq.length; j++) {
			if(freq[j] != 0) numOfColor++;
		}
		
		int k = 0;
		while((1<<k) < numOfColor) k++;		
	
		return k;
	}
	
	public static ICC_ColorSpace getICCColorSpace(String pathToICCProfile) throws IOException {
		return new ICC_ColorSpace(ICC_Profile.getInstance(IMGUtils.class.getResourceAsStream(pathToICCProfile)));
	}
	
	public static ICC_Profile getICCProfile(String pathToICCProfile) throws IOException {
		ICC_ColorSpace icc_colorspace = getICCColorSpace(pathToICCProfile);
		if(icc_colorspace != null)
			return icc_colorspace.getProfile();
		return null;
	}
	
	/**
	 * Retrieves RGB values from Raster. This seems to be the fastest way to get RGB values.
	 *
	 * TYPE_INT_RGB        = 1
	 * TYPE_INT_ARGB       = 2
	 * TYPE_INT_ARGB_PRE   = 3
	 * TYPE_INT_BGR        = 4
	 * TYPE_3BYTE_BGR      = 5
	 * TYPE_4BYTE_ABGR     = 6
	 * TYPE_4BYTE_ABGR_PRE = 7
	 * TYPE_BYTE_GRAY      = 10
	 * TYPE_BYTE_BINARY    = 12
	 * TYPE_BYTE_INDEXED   = 13
	 * TYPE_USHORT_GRAY    = 11
	 * TYPE_USHORT_565_RGB = 8
	 * TYPE_USHORT_555_RGB = 9
	 * TYPE_CUSTOM         = 0.
	 * 
	 * @param image input BufferedImage 
	 * @return integer array - default RGB color space representation
	 */
	public static int[] getRGB(BufferedImage image) {
		// Get the BufferedImageType
		int type = image.getType();
		// Grab the internal data array
		Raster raster = image.getRaster();
		Object object = raster.getDataElements(0, 0, image.getWidth(), image.getHeight(), null);
		// Determine the data transfer type
		int transferType = raster.getTransferType();
		// Get image dimension
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		int imageSize = imageWidth*imageHeight;
		int[] rgbs = new int[imageSize];
		// Branch by transfer type
		switch(transferType) {
			case DataBuffer.TYPE_INT:
				rgbs = (int[])object;				
				if(type == BufferedImage.TYPE_INT_ARGB_PRE) {										
					for(int i = 0; i < imageSize; i++) {
						float alpha = 255.0f/((rgbs[i]>>24)&0xff);
						byte red = (byte)(((rgbs[i]>>16)&0xff)*alpha);
						byte green = (byte)(((rgbs[i]>>8)&0xff)*alpha);
						byte blue = (byte)((rgbs[i]&0xff)*alpha);
						rgbs[i] = (rgbs[i]&0xff000000)|((red&0xff)<<16)|((green&0xff)<<8)|(blue&0xff);						 
					}
				} else if(type == BufferedImage.TYPE_INT_BGR) { // Convert BGR to RGB
					for(int i = 0; i < rgbs.length; i++) {
						int blue = (rgbs[i]>>16)&0xff;
						int green = (rgbs[i]>>8) & 0xff;
						int red = rgbs[i]&0xff;
						rgbs[i] = 0xff000000|(red << 16)|(green << 8)|blue;
					}
				} else if(type == BufferedImage.TYPE_INT_RGB) {
					for(int i = 0; i < rgbs.length; i++)
						rgbs[i] = 0xff000000|rgbs[i];
				} else if(type == BufferedImage.TYPE_INT_ARGB) {
					;  // Do nothing
				} else {
					LOGGER.warn("### Warning: IMGUtils.getRGB() found custom type BufferedImage, fall back to BufferedImage.getRGB() ###");
					return image.getRGB(0, 0, imageWidth, imageHeight, rgbs, 0, imageWidth);
				}
				return rgbs;
			case DataBuffer.TYPE_BYTE:
				byte[] bpixels = (byte[])object;
				// BufferedImage.getRGB() seems a bit faster in this case for small images.
				if(type == BufferedImage.TYPE_BYTE_INDEXED || type == BufferedImage.TYPE_BYTE_BINARY) {
					IndexColorModel indexModel = (IndexColorModel)image.getColorModel();
					int mapSize = indexModel.getMapSize();
					byte[] reds = new byte[mapSize];
					byte[] greens = new byte[mapSize];
					byte[] blues = new byte[mapSize];
					byte[] alphas = new byte[mapSize];
					int[] palette = new int[mapSize];
					indexModel.getReds(reds); 
					indexModel.getGreens(greens);
					indexModel.getBlues(blues);
					indexModel.getAlphas(alphas);
					for(int i = 0; i < mapSize; i++)
						palette[i] = (alphas[i]&0xff)<<24|(reds[i]&0xff)<<16|(greens[i]&0xff)<<8|blues[i]&0xff;
					for(int i = 0; i < imageSize; i++)
						rgbs[i] = palette[bpixels[i]&0xff];
				} else if(type == BufferedImage.TYPE_4BYTE_ABGR) {
					for(int i = 0, index = 0; i < imageSize; i++)
						rgbs[i] = (((bpixels[index++]&0xff)<<16)|((bpixels[index++]&0xff)<<8)|(bpixels[index++]&0xff)|((bpixels[index++]&0xff)<<24));
				} else if(type == BufferedImage.TYPE_3BYTE_BGR) {
					for(int i = 0, index = 0; i < imageSize; i++)
						rgbs[i] = ((0xff000000)|((bpixels[index++]&0xff)<<16)|((bpixels[index++]&0xff)<<8)|(bpixels[index++]&0xff));
				} else if(type == BufferedImage.TYPE_4BYTE_ABGR_PRE) {				
					for(int i = 0, index = 0; i < imageSize; i++, index += 4) {
						float alpha = 255.0f*(bpixels[index+3]&0xff);
						byte blue = (byte)((bpixels[index+2]&0xff)*alpha);
						byte green = (byte)((bpixels[index+1]&0xff)*alpha);
						byte red = (byte)((bpixels[index]&0xff)*alpha);
						rgbs[i] =  (bpixels[index+3]&0xff000000)|((red&0xff)<<16)|((green&0xff)<<8)|(blue&0xff);						 
					}
				} else if(type == BufferedImage.TYPE_BYTE_GRAY) {
					for(int i = 0; i < imageSize; i++)
						rgbs[i] = (0xff000000)|((bpixels[i]&0xff)<<16)|((bpixels[i]&0xff)<<8)|(bpixels[i]&0xff);						 
				} else {
					LOGGER.warn("### Warning: IMGUtils.getRGB() found custom type BufferedImage, fall back to BufferedImage.getRGB() ###");
					return image.getRGB(0, 0, imageWidth, imageHeight, rgbs, 0, imageWidth);
				}
				return rgbs;
			case DataBuffer.TYPE_USHORT:
				short[] spixels = (short[])object;
				if(type == BufferedImage.TYPE_USHORT_GRAY) {
					for(int i = 0; i < imageSize; i++) {
						int gray = ((spixels[i]>>8)&0xff);
						rgbs[i] = (0xff000000)|(gray<<16)|(gray<<8)|gray;
					}
				} else if(type == BufferedImage.TYPE_USHORT_565_RGB) {
					for(int i = 0; i < imageSize; i++) {
						int red = ((spixels[i]>>11)&0x1f);
						int green = ((spixels[i]>>5)&0x3f);
						int blue = (spixels[i]&0x1f);
						rgbs[i] = (0xff000000)|(red<<19)|(green<<10)|(blue<<3);
					}
				} else if(type == BufferedImage.TYPE_USHORT_555_RGB) {
					for(int i = 0; i < imageSize; i++) {
						int red = ((spixels[i]>>>10)&0x1f);
						int green = ((spixels[i]>>>5)&0x1f);
						int blue = (spixels[i]&0x1f);
						rgbs[i] = (0xff000000)|(red<<19)|(green<<11)|(blue<<3);
					}
				} else {
					LOGGER.warn("### Warning: IMGUtils.getRGB() found custom type BufferedImage, fall back to BufferedImage.getRGB() ###");
					return image.getRGB(0, 0, imageWidth, imageHeight, rgbs, 0, imageWidth);
				}		
				return rgbs;
			default:
				return image.getRGB(0, 0, imageWidth, imageHeight, rgbs, 0, imageWidth);
		}
	}
	
	/**
	 * Retrieves RGB values from DataBuffer. This one is slower than getRGB()
	 * 
	 * @param image input BufferedImage 
	 * @return integer array - default RGB color space representation
	 */
	public static int[] getRGB2(BufferedImage image) {
		// Get the BufferedImageType
		int type = image.getType();
		Raster raster = image.getRaster();	
		DataBuffer dataBuffer = raster.getDataBuffer();
		int numOfBanks = dataBuffer.getNumBanks();
		// Determine the data type
		int dataType = dataBuffer.getDataType();
		// Get image dimension
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		int imageSize = imageWidth*imageHeight;
		int[] rgbs = new int[imageSize];	
		// Branch by data type
		switch(dataType) {
			case DataBuffer.TYPE_INT:
				rgbs = ((DataBufferInt)dataBuffer).getData(0);				
				if(type == BufferedImage.TYPE_INT_ARGB_PRE) {										
					for(int i = 0; i < imageSize; i++) {
						float alpha = 255.0f/((rgbs[i]>>24)&0xff);
						byte red = (byte)(((rgbs[i]>>16)&0xff)*alpha);
						byte green = (byte)(((rgbs[i]>>8)&0xff)*alpha);
						byte blue = (byte)((rgbs[i]&0xff)*alpha);
						rgbs[i] = (rgbs[i]&0xff000000)|((red&0xff)<<16)|((green&0xff)<<8)|(blue&0xff);						 
					}
				} else if(type == BufferedImage.TYPE_INT_BGR) { // Convert BGR to RGB
					for(int i = 0; i < rgbs.length; i++) {
						int blue = (rgbs[i]>>16)&0xff;
						int green = (rgbs[i]>>8) & 0xff;
						int red = rgbs[i]&0xff;
						rgbs[i] = 0xff000000|(red << 16)|(green << 8)|blue;
					}
				} else if(type == BufferedImage.TYPE_INT_RGB) {
					for(int i = 0; i < rgbs.length; i++)
						rgbs[i] = 0xff000000|rgbs[i];
				} else if(type == BufferedImage.TYPE_INT_ARGB) { ; } // Do nothing 
				return rgbs;
			case DataBuffer.TYPE_BYTE:
				byte[][] bpixels = ((DataBufferByte)dataBuffer).getBankData();
				if(type == BufferedImage.TYPE_BYTE_INDEXED || type == BufferedImage.TYPE_BYTE_BINARY) {
					IndexColorModel indexModel = (IndexColorModel)image.getColorModel();
					int mapSize = indexModel.getMapSize();
					byte[] reds = new byte[mapSize];
					byte[] greens = new byte[mapSize];
					byte[] blues = new byte[mapSize];
					byte[] alphas = new byte[mapSize];
					int[] palette = new int[mapSize];
					indexModel.getReds(reds); 
					indexModel.getGreens(greens);
					indexModel.getBlues(blues);
					indexModel.getAlphas(alphas);
					for(int i = 0; i < mapSize; i++)
						palette[i] = (alphas[i]&0xff)<<24 | (reds[i]&0xff)<<16 | (greens[i]&0xff)<<8 | blues[i]&0xff;
					int bitsPerPixel = raster.getSampleModel().getSampleSize(0);
					int i = 0, index = 0, padding = 0, safeEnd = 0;
					switch (bitsPerPixel) {
					    case 8:							
							for(; i < imageSize; i++)
								rgbs[i] = palette[bpixels[0][i]&0xff];
							break;
						case 4:
							padding = imageWidth%2;
							safeEnd = imageWidth - padding;
							for(int j = 0; j < imageHeight; j++) {
					 			for (int k = 0; k< safeEnd; k+=2, i++)
						 		{
						 			rgbs[index++] = palette[(bpixels[0][i]>>>4)&0x0f];
						 			rgbs[index++] = palette[bpixels[0][i]&0x0f];
						 		}
					 			if(padding != 0) rgbs[index++] = palette[(bpixels[0][i]>>>4)&0x0f];
					 		}
							break;
						case 2:
							padding = imageWidth%4;
							safeEnd = imageWidth - padding;
							for(int j = 0; j < imageHeight; j++) {
					 			for (int k = 0; k< safeEnd; k+=4, i++)
						 		{
					 				for(int l = 6; l >= 0; l-=2, index++)
					 					rgbs[index] = palette[(bpixels[0][i]>>>l)&0x03];
						 		}
					 			if(padding != 0) {
						 			for(int m = 0, n = 6; m < padding; m++, n-=2, index++)
						 				rgbs[index] =  palette[(bpixels[0][i]>>>n)&0x03];
						 			i++;
					 			}
					 		}
							break;
						case 1:
							padding = imageWidth%8;
							safeEnd = imageWidth - padding;
							int maxValue = 8 - padding;
							for(int j = 0; j < imageHeight; j++) {
					 			for (int k = 0; k< safeEnd; k+=8, i++) {
					 				for(int l = 7; l >= 0; l--, index++)
					 					rgbs[index] = palette[(bpixels[0][i]>>>l)&0x01];
					 			}
					 			if(padding != 0) {
						 			for(int m = 7; m >= maxValue; m--, index++)
						 				rgbs[index] = palette[(bpixels[0][i]>>>m)&0x01];
						 			i++;
					 			}
					 		}
				 			break;
						default: 
							LOGGER.error(bitsPerPixel + " bit color depth is not valid for indexed image...");
					}
				} else if(type == BufferedImage.TYPE_4BYTE_ABGR) {
					if(numOfBanks == 1) {
						for(int i = 0, index = 0; i < imageSize; i++)
							rgbs[i] = ((bpixels[0][index++]&0xff)<<24)|(bpixels[0][index++]&0xff)|((bpixels[0][index++]&0xff)<<8)|((bpixels[0][index++]&0xff)<<16);
					} else { // We assume 4 banks
						for(int i = 0; i < imageSize; i++)
							rgbs[i] = ((bpixels[0][i]&0xff)<<24)|(bpixels[1][i]&0xff)|((bpixels[2][i]&0xff)<<8)|((bpixels[3][i]&0xff)<<16);
					}
				} else if(type == BufferedImage.TYPE_3BYTE_BGR) {
					if(numOfBanks == 1) {
						for(int i = 0, index = 0; i < imageSize; i++)
							rgbs[i] = 0xff000000|(bpixels[0][index++]&0xff)|((bpixels[0][index++]&0xff)<<8)|((bpixels[0][index++]&0xff)<<16);
					} else { // We assume 3 banks
						for(int i = 0; i < imageSize; i++)
							rgbs[i] = 0xff000000|(bpixels[0][i]&0xff)|((bpixels[1][i]&0xff)<<8)|((bpixels[2][i]&0xff)<<16);
					}
				} else if(type == BufferedImage.TYPE_4BYTE_ABGR_PRE) {
					if(numOfBanks == 1) {
						for(int i = 0, index = 0; i < imageSize; i++, index += 4) {
							float alpha = 255.0f*(bpixels[0][index]&0xff);
							byte blue = (byte)((bpixels[0][index+1]&0xff)*alpha);
							byte green = (byte)((bpixels[0][index+2]&0xff)*alpha);
							byte red = (byte)((bpixels[0][index+3]&0xff)*alpha);
							rgbs[i] =  ((bpixels[0][index]&0xff)<<24)|((red&0xff)<<16)|((green&0xff)<<8)|(blue&0xff);						 
						}
					} else { // We assume 4 banks
						for(int i = 0; i < imageSize; i++) {
							float alpha = 255.0f*(bpixels[0][i]&0xff);
							byte blue = (byte)((bpixels[1][i]&0xff)*alpha);
							byte green = (byte)((bpixels[2][i]&0xff)*alpha);
							byte red = (byte)((bpixels[3][i]&0xff)*alpha);
							rgbs[i] =  ((bpixels[0][i]&0xff)<<24)|((red&0xff)<<16)|((green&0xff)<<8)|(blue&0xff);						 
						}
					}
				} else if(type == BufferedImage.TYPE_BYTE_GRAY) {
					for(int i = 0; i < imageSize; i++)
						rgbs[i] = 0xff000000|((bpixels[0][i]&0xff)<<16)|((bpixels[0][i]&0xff)<<8)|(bpixels[0][i]&0xff);						 
				}
				return rgbs;
			case DataBuffer.TYPE_USHORT:
				short[] spixels =  ((DataBufferUShort)dataBuffer).getBankData()[0];
				if(type == BufferedImage.TYPE_USHORT_GRAY) {
					for(int i = 0; i < imageSize; i++) {
						int gray = ((spixels[i]>>8)&0xff);
						rgbs[i] = 0xff000000|(gray<<16)|(gray<<8)|gray;
					}
				} else if(type == BufferedImage.TYPE_USHORT_565_RGB) {
					for(int i = 0; i < imageSize; i++) {
						int red = ((spixels[i]>>11)&0x1f);
						int green = ((spixels[i]>>5)&0x3f);
						int blue = (spixels[i]&0x1f);
						rgbs[i] = 0xff000000|(red<<19)|(green<<10)|(blue<<3);
					}
				} else if(type == BufferedImage.TYPE_USHORT_555_RGB) {
					for(int i = 0; i < imageSize; i++) {
						int red = ((spixels[i]>>>10)&0x1f);
						int green = ((spixels[i]>>>5)&0x1f);
						int blue = (spixels[i]&0x1f);
						rgbs[i] = 0xff000000|(red<<19)|(green<<11)|(blue<<3);
					}
				}			
				return rgbs;
			default:
				return image.getRGB(0, 0, imageWidth, imageHeight, rgbs, 0, imageWidth);
		}
	}
	
	public static ImageType guessImageType(byte[] magicNumber) {
		ImageType imageType = ImageType.UNKNOWN;
		// Check image type
		if(Arrays.equals(magicNumber, TIFF_II) || Arrays.equals(magicNumber, TIFF_MM))
			imageType = ImageType.TIFF;
		else if(Arrays.equals(magicNumber, PNG))
			imageType = ImageType.PNG;
		else if(Arrays.equals(magicNumber, GIF))
			imageType = ImageType.GIF;
		else if(magicNumber[0] == JPG[0] && magicNumber[1] == JPG[1] && magicNumber[2] == JPG[2])
			imageType = ImageType.JPG;
		else if(magicNumber[0] == BM[0] && magicNumber[1] == BM[1])
			imageType = ImageType.BMP;
		else if(magicNumber[0] == PCX[0])
			imageType = ImageType.PCX;
		else if(Arrays.equals(magicNumber, JPG2000)) {
			imageType = ImageType.JPG2000;
		} else if(magicNumber[1] == 0 || magicNumber[1] == 1) {
			switch(magicNumber[2]) {
				case 0:
				case 1:
				case 2:
				case 3:
				case 9:
				case 10:
				case 11:
				case 32:
				case 33:
					imageType = ImageType.TGA;					
			}
		} else {
			LOGGER.error("Unknown image type format!");		
		}
		
		return imageType;
	}
	
	public static ImageType guessImageType(PushbackInputStream is) throws IOException {
		// Read the first ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes
		byte[] magicNumber = new byte[ImageIO.IMAGE_MAGIC_NUMBER_LEN];
		is.read(magicNumber);
		ImageType imageType = guessImageType(magicNumber);
		is.unread(magicNumber);// reset stream pointer
		
		return imageType;
	}
	
	public static ImageType guessImageType(RandomAccessInputStream is) throws IOException {
		// Read the first ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes
		byte[] magicNumber = new byte[ImageIO.IMAGE_MAGIC_NUMBER_LEN];
		long streamPointer = is.getStreamPointer();
		is.read(magicNumber);		
		ImageType imageType = guessImageType(magicNumber);
		is.seek(streamPointer);// reset stream pointer
		
		return imageType;
	}
	
	/**
	 * Convert ICC_ColorSpace raster to RGB raster w/o alpha
	 * 
	 * @param raster WritableRaster for ICC_Profile ColorSpace
	 * @param cm ColorModel for ICC_Profile ColorSpace
	 * @return WritableRaster for RGB ColorSpace
	 */
	public static WritableRaster iccp2rgbRaster(WritableRaster raster, ColorModel cm) {
		ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		ColorConvertOp cco = new ColorConvertOp(cm.getColorSpace(), sRGB, null);
		WritableRaster rgbRaster = null;		
		BufferedImage iccpImage = new BufferedImage(cm, raster, false, null);
		// Filter on BufferedImage to keep alpha channel
		rgbRaster = cco.filter(iccpImage, null).getRaster();
		
		return rgbRaster;
	}
	
	// Change the bit color sex of a byte array
	public static void invertBits(byte[] input) {
		for(int i = input.length - 1; i >= 0; i--) {
			input[i] = (byte)~input[i];
		}
	}
	
	/**
	 * Reduces a true color image to an indexed-color image with no_of_color using "Popularity algorithm".
	 * 
	 * @param rgbTriplets an int array of RGB triplets for the image
	 * @param colorDepth the desired color depth - actual value might be smaller
	 * @param newPixels a byte array to hold the color map indexes for the image
	 * @param colorPalette the color map for the image
	 * @return a two element int array holding the color depth and the transparent color index if any
	 */
	public static int[] reduceColors(int[] rgbTriplets, int colorDepth, byte[] newPixels, final int[] colorPalette) 
	{
		int no_of_color = 1<<colorDepth;
		int[] colorFreq = new int[4096];
		int[] indexColor = new int[4096];
	    int[] clrPalRed = new int[no_of_color];
        int[] clrPalGreen = new int[no_of_color];
	    int[] clrPalBlue = new int[no_of_color];
		int[] colorInfo = new int[2];// Return value
		int[] colorIndex;
		int bitsPerPixel = 1;
		int transparent_color = -1;// Transparent color 
		int transparent_index = -1;// Transparent color index
		
		int red, green, blue, index, colorCount, temp, temp1, err1, err2;
        // Get the 4 most significant bits of red, green and blue to
        // form a 12 bits integer and determine the frequencies of different 
        // values
		for (int i = 0; i < rgbTriplets.length; i++)
		{
			if((rgbTriplets[i] >>> 24) < 0x80 )// Transparent
			{
				if (transparent_color < 0)	// Find the transparent color	
					transparent_color = rgbTriplets[i];			
			}
			red = ((rgbTriplets[i]&0xf00000)>>>20);
			green = ((rgbTriplets[i]&0x00f000)>>>8);
			blue = ((rgbTriplets[i]&0x0000f0)<<4);
			index = (red|green|blue);
			colorFreq[index]++;
		}
        // Throw away the zero items
        colorCount = 0;
        for (int i = 0; i < 4096; i++ )
        {
			if (colorFreq[i] != 0)
			{
				colorFreq[colorCount]=colorFreq[i];
				indexColor[colorCount++] = i;
			}
        }
        // Sort the colors according to their frequencies
        // Bubble sort
        /**
        for (int i=0; i<colorCount-1; i++)
        {
			for (int j=i+1; j<colorCount; j++)
			{
               if(colorFreq[i]<colorFreq[j])
               {
				   temp = colorFreq[i];
				   colorFreq[i] = colorFreq[j];
				   colorFreq[j] = temp;
				   temp = indexColor[i];
				   indexColor[i] = indexColor[j];
				   indexColor[j] = temp;
			   }
			}
        }
	  	*/
		// Shell sort
  	    int mid = colorCount/2;
	    while ( mid > 0 )
	    {
		   for (int i = mid; i < colorCount; i++)
		   {
			   temp = colorFreq[i];
			   temp1 = indexColor[i];
			   int j = i;
			   while ( j >= mid && temp >= colorFreq[j - mid])
			   {
				   colorFreq[j] = colorFreq[j - mid];
				   indexColor[j] = indexColor[j - mid];
				   j -= mid;
			   }
			   colorFreq[j] = temp;
			   indexColor[j] = temp1;
		   }
		   mid /= 2;
	    }
	   	colorIndex = new int[no_of_color>=colorCount?no_of_color:colorCount];
		// Take the first no_of_color items as the palette 
		for (int i = 0; i < no_of_color; i++)
		{
            clrPalBlue[i]  = ((indexColor[i]&0xf00)>>>4);
			clrPalGreen[i] =(indexColor[i]&0x0f0);
			clrPalRed[i]  = ((indexColor[i]&0x00f)<<4);

			colorPalette[i] = ((0xff<<24)|(clrPalRed[i]<<16)|(clrPalGreen[i]<<8)|clrPalBlue[i]);
			colorIndex[i] = i;
		}
		if(transparent_color >= 0)// There is a transparent color
			no_of_color--;// The available color is one less
		// Find the nearest color for the other colors if there are more colors than no_of_color
        if (colorCount > no_of_color)
        {
			for (int i = no_of_color; i < colorCount; i++)
			{
				index = 0;

				blue   = ((indexColor[i]&0xf00)>>>4);
				green = (indexColor[i]&0x0f0);
				red  = ((indexColor[i]&0x00f)<<4);

				err1 =  (red-clrPalRed[0])*(red-clrPalRed[0])+(green-clrPalGreen[0])*(green-clrPalGreen[0])+
					    (blue-clrPalBlue[0])*(blue-clrPalBlue[0]);
                
				for (int j = 1; j < no_of_color; j++)
				{
					err2 = (red-clrPalRed[j])*(red-clrPalRed[j])+(green-clrPalGreen[j])*(green-clrPalGreen[j])+
					       (blue-clrPalBlue[j])*(blue-clrPalBlue[j]);
					if (err2 < err1)
					{
						err1 = err2;
                        index = j;
					}
				}
                colorIndex[i] = index;
			}
        }
        // Reduce colors
		for (int i = 0; i < colorCount; i++ )
		{
			// Here and after colorFreq is used to keep the
			// index of colorIndex array for different colors 
			colorFreq[indexColor[i]] = i;
		}
		if(transparent_color >= 0)// There is a transparent color
			colorCount++;// Count in the transparent color
		// Determine the actual bits we need
		while ((1<<bitsPerPixel) < colorCount)  bitsPerPixel++;
		
		if(transparent_color >= 0)// Set the colorPalette for the transparent color
		{
			transparent_index = (bitsPerPixel <= colorDepth)?(1<<bitsPerPixel)-1:no_of_color;
			colorPalette[transparent_index] = transparent_color;
		}
				
		for (int i = 0; i < rgbTriplets.length; i++)
		{
			red = ((rgbTriplets[i]&0xf00000)>>>20);
			green = ((rgbTriplets[i]&0x00f000)>>>8);
			blue = ((rgbTriplets[i]&0x0000f0)<<4);

			index = (red|green|blue);
			
		    // Write the color index of different pixels to a new data array
			newPixels[i] = (byte)colorIndex[colorFreq[index]];
			if((rgbTriplets[i] >>> 24) < 0x80 )//Transparent
			{
				newPixels[i] = (byte)transparent_index;	
			}
	    }
		// Return the actual bits per pixel and the transparent color index if any
		colorInfo[0] = bitsPerPixel;
		colorInfo[1] = transparent_index;
		return colorInfo;
	}
	
	/**
	 * Reduces a true color image to an indexed-color image with no_of_color using "Popularity algorithm"
	 * followed by Floyd-Steinberg error diffusion dithering.
	 */
	public static int[] reduceColorsFloydSteinberg(int[] rgbTriplet, int width, int height, int colorDepth, byte[] newPixels, final int[] colorPalette) 
	{
		int no_of_color = 1<<colorDepth;
		int[] colorFreq = new int[4096];
		int[] indexColor = new int[4096];
		int[] colorInfo = new int[2];// Return value
		int[] colorIndex;
		int bitsPerPixel = 1;
		int transparent_color = -1;// Transparent color 
		int transparent_index = -1;// Transparent color index
		
		int red, green, blue, index, colorCount, temp, temp1;
        // Get the 4 most significant bits of red, green and blue to
        // form a 12 bits integer and determine the frequencies of different 
        // values
		for (int i = 0; i < rgbTriplet.length; i++)
		{
			if((rgbTriplet[i] >>> 24) < 0x80 )// Transparent
			{
				if (transparent_color < 0)	// Find the transparent color	
					transparent_color = rgbTriplet[i];			
			}
			red = ((rgbTriplet[i]&0xf00000)>>>20);
			green = ((rgbTriplet[i]&0x00f000)>>>8);
			blue = ((rgbTriplet[i]&0x0000f0)<<4);
			index = (red|green|blue);
			colorFreq[index]++;
		}
        // Throw away the zero items
        colorCount = 0;
        for (int i = 0; i < 4096; i++ )
        {
			if (colorFreq[i] != 0)
			{
				colorFreq[colorCount]=colorFreq[i];
				indexColor[colorCount++] = i;
			}
        }
        // Sort the colors according to their frequencies
     	// Shell sort
  	    int mid = colorCount/2;
	    while ( mid > 0 )
	    {
		   for (int i = mid; i < colorCount; i++)
		   {
			   temp = colorFreq[i];
			   temp1 = indexColor[i];
			   int j = i;
			   while ( j >= mid && temp >= colorFreq[j - mid])
			   {
				   colorFreq[j] = colorFreq[j - mid];
				   indexColor[j] = indexColor[j - mid];
				   j -= mid;
			   }
			   colorFreq[j] = temp;
			   indexColor[j] = temp1;
		   }
		   mid /= 2;
	    }
	   	colorIndex = new int[no_of_color];
		// Take the first no_of_color items as the palette 
		for (int i = 0; i < no_of_color; i++)
		{
            blue  = ((indexColor[i]&0xf00)>>>4);
			green = (indexColor[i]&0x0f0);
			red  =  ((indexColor[i]&0x00f)<<4);

			colorPalette[i] = ((0xff << 24)|(red << 16)|(green << 8)|blue);
			colorIndex[i] = i;
		}		
		if(transparent_color >= 0)// There is a transparent color
	    {
			no_of_color--;// The available color is one less
			colorCount++;// Count in the transparent color to determine color depth
		}
		// Determine the actual bits we need
		while ((1<<bitsPerPixel) < colorCount)  bitsPerPixel++;
		
		if(transparent_color >= 0)// Set the colorPalette for the transparent color
		{
			transparent_index = (bitsPerPixel <= colorDepth)?(1<<bitsPerPixel)-1:no_of_color;
			colorPalette[transparent_index] = transparent_color;
			colorCount--;//We need the actual number of color now
		}	
		// Call Floyd-Steinberg dither
		dither_FloydSteinberg(rgbTriplet, width, height, newPixels, (colorCount < no_of_color)?colorCount:no_of_color, colorPalette, transparent_index);
		// Return the actual bits per pixel and the transparent color index if any
		colorInfo[0] = bitsPerPixel;
		colorInfo[1] = transparent_index;
		return colorInfo;
	}
	
	// This works quite well without dither
	public static byte[] rgb2bilevel(int[] rgb) {
		// RGB to gray-scale
		byte[] pixels = new byte[rgb.length];
		int sum = 0;
		
		for(int i = 0; i < rgb.length; i++) {
			if((rgb[i] >>> 24) < 0x80) pixels[i] = (byte)0xff; // Dealing with transparency color
			else
				pixels[i] = (byte)(((rgb[i]>>16)&0xff)*0.2126 + ((rgb[i]>>8)&0xff)*0.7152 + (rgb[i]&0xff)*0.0722);
			sum += (pixels[i]&0xff);
		}
		
		// Calculate threshold
		int threshold = (sum/pixels.length);
		
		// Reduce gray-scale to BW - we assume PhotoMetric.WHITE_IS_ZERO
		for(int l = 0; l < pixels.length; l++) {
			if((pixels[l]&0xff) <= threshold) {
				pixels[l] = 1; // Black
			} else {
				pixels[l] = 0; // White
			}
		}	
		
		return pixels;
	}
	
	/**
	 * RGB to GrayScale image conversion with Floyd-Steinberg dither
	 * 
	 * @param rgb input RGB image array (format: ARGBARGBARGB...)
	 * @param imageWidth image width
	 * @param imageHeight image height
	 * @param err_limit Floyd-Steinberg error diffusion error limit (range 0-255 inclusive)
	 * @return byte array for the BW image
	 */
	public static byte[] rgb2bilevelDither(int[] rgb, int imageWidth, int imageHeight, int err_limit) {
		// RGB to gray-scale
		byte[] pixels = new byte[rgb.length];
		byte[] mask = new byte[rgb.length];
		int sum = 0;
		
		Arrays.fill(mask, (byte)0x01);
		
		for(int i = 0; i < rgb.length; i++) {
			if((rgb[i] >>> 24) < 0x80) {
				pixels[i] = (byte)0xff; // Dealing with transparency color
				mask[i] = 0x00;
			} else
				pixels[i] = (byte)(((rgb[i]>>16)&0xff)*0.2126 + ((rgb[i]>>8)&0xff)*0.7152 + (rgb[i]&0xff)*0.0722);
			sum += (pixels[i]&0xff);
		}
		
		// Calculate threshold
		int threshold = (sum/pixels.length);
		
		IMGUtils.dither_FloydSteinberg(pixels, mask, imageWidth, imageHeight, threshold, err_limit);
		
		return pixels;
	}
	
	// Convert RGB to CMYK with level shift (minus 128)
	public static void RGB2CMYK(ICC_ColorSpace cmykColorSpace, int[] rgb, float[][] C, float[][] M, float[][] Y, float[][] K, int imageWidth, int imageHeight) {
		DataBuffer db = new DataBufferInt(rgb, rgb.length);
		WritableRaster raster = Raster.createPackedRaster(db, imageWidth, imageHeight, imageWidth,  new int[] {0x00ff0000, 0x0000ff00, 0x000000ff}, null);
		ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

		ColorConvertOp cco = new ColorConvertOp(sRGB, cmykColorSpace, null);
		
		BufferedImage rgbImage = new BufferedImage(new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff), raster, false, null);
		BufferedImage cmykImage = cco.filter(rgbImage, null);
		WritableRaster cmykRaster = cmykImage.getRaster();
		
		byte[] cmyk = (byte[])cmykRaster.getDataElements(0, 0, imageWidth, imageHeight, null);
		
		for(int i = 0, index = 0; i < imageHeight; i++) {
			for(int j = 0; j < imageWidth; j++) {
				C[i][j] = (cmyk[index++]&0xff) - 128.0f;
				M[i][j] = (cmyk[index++]&0xff) - 128.0f;
				Y[i][j] = (cmyk[index++]&0xff) - 128.0f;
				K[i][j] = (cmyk[index++]&0xff) - 128.0f;
			}
		}
	}
	
	// Convert RGB to CMYK w/o alpha
	public static byte[] RGB2CMYK(ICC_ColorSpace cmykColorSpace, int[] rgb, int imageWidth, int imageHeight, boolean hasAlpha) {
		DataBuffer db = new DataBufferInt(rgb, rgb.length);
		int[] bandMasks = new int[]{0x00ff0000, 0x0000ff00, 0x000000ff};
		ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		ColorConvertOp cco = new ColorConvertOp(sRGB, cmykColorSpace, null);
		ColorModel cm = null;
		WritableRaster cmykRaster = null;		
		if(hasAlpha) {
			cm = ColorModel.getRGBdefault();
			bandMasks = new int[]{0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000};	
		} else 
			cm = new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff);
		WritableRaster raster = Raster.createPackedRaster(db, imageWidth, imageHeight, imageWidth, bandMasks, null);
		BufferedImage rgbImage = new BufferedImage(cm, raster, false, null);
		BufferedImage cmykImage = cco.filter(rgbImage, null);
		cmykRaster = cmykImage.getRaster();

		return (byte[])cmykRaster.getDataElements(0, 0, imageWidth, imageHeight, null);
	}
	
	// Convert RGB to inverted CMYK with level shift (128 minus)
	public static void RGB2CMYK_Inverted(ICC_ColorSpace cmykColorSpace, int[] rgb, float[][] C, float[][] M, float[][] Y, float[][] K, int imageWidth, int imageHeight) {
		DataBuffer db = new DataBufferInt(rgb, rgb.length);
		WritableRaster raster = Raster.createPackedRaster(db, imageWidth, imageHeight, imageWidth,  new int[] {0x00ff0000, 0x0000ff00, 0x000000ff}, null);
		ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

		ColorConvertOp cco = new ColorConvertOp(sRGB, cmykColorSpace, null);
		
		BufferedImage rgbImage = new BufferedImage(new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff), raster, false, null);
		BufferedImage cmykImage = cco.filter(rgbImage, null);
		WritableRaster cmykRaster = cmykImage.getRaster();
		
		byte[] cmyk = (byte[])cmykRaster.getDataElements(0, 0, imageWidth, imageHeight, null);		
		
		for(int i = 0, index = 0; i < imageHeight; i++) {
			for(int j = 0; j < imageWidth; j++) {
				C[i][j] = 128.0f - (cmyk[index++]&0xff);
				M[i][j] = 128.0f - (cmyk[index++]&0xff);
				Y[i][j] = 128.0f - (cmyk[index++]&0xff);
				K[i][j] = 128.0f - (cmyk[index++]&0xff);
			}
		}
	}
	
	// Luma method to convert RGB to grayscale
	public static byte[] rgb2grayscale(int[] rgb) {
		byte[] grayscale = new byte[rgb.length];
		
		for(int i = 0; i < rgb.length; i++) {
			grayscale[i] = (byte)(((rgb[i]>>16)&0xff)*0.2126 + ((rgb[i]>>8)&0xff)*0.7152 + (rgb[i]&0xff)*0.0722);
		}
		
		return grayscale;
	}
	
	// Luma method to convert RGB to grayscale (level shift included for JPEG image)
	public static float[][] rgb2grayscale(int[] rgb, int imageWidth, int imageHeight) {
		float[][] grayscale = new float[imageHeight][imageWidth];
		
		for(int i = 0, index = 0; i < imageHeight; i++) {
			for(int j = 0; j < imageWidth; j++, index++) {
				grayscale[i][j] = (float)(((rgb[index]>>16)&0xff)*0.2126 + ((rgb[index]>>8)&0xff)*0.7152 + (rgb[index]&0xff)*0.0722 - 128.0);
			}
		}
		
		return grayscale;
	}
	
	// Luma method to convert RGBA to grayscale (keeping alpha channel)
	public static byte[] rgb2grayscaleA(int[] rgb) {
		// We need to double the array length because of the alpha channel
		byte[] grayscale = new byte[rgb.length<<1];
		
		for(int i = 0, j = 0; i < rgb.length; i++) {
			grayscale[j++] = (byte)(((rgb[i]>>16)&0xff)*0.2126 + ((rgb[i]>>8)&0xff)*0.7152 + (rgb[i]&0xff)*0.0722);
			grayscale[j++] = (byte)((rgb[i]>>24)&0xff);
		}
		
		return grayscale;
	}
	
	// Convert RGB to YCbCr		
	public static byte[] RGB2YCbCr(int[] rgb) {
		int red,green,blue, index = 0;
		byte[] ycbcr = new byte[rgb.length*3];
		
		for(int i = 0; i < rgb.length; i++) {
			red = ((rgb[i] >> 16) & 0xff);
			green = ((rgb[i] >> 8) & 0xff);
			blue = (rgb[i] & 0xff);
			ycbcr[index++] = (byte)(0.299f*red + 0.587f*green + 0.114f*blue);
			ycbcr[index++] = (byte)(- 0.1687f*red - 0.3313f*green + 0.5f*blue + 128.0f);
			ycbcr[index++] = (byte)(0.5f*red - 0.4187f*green - 0.0813f*blue + 128.f);
		}
		
		return ycbcr;
	}
	
	// Convert RGB to YCbCr with level shift (minus 128)
	public static void RGB2YCbCr(int[] rgb, float[][] Y, float[][] Cb, float[][] Cr, int imageWidth, int imageHeight) {
		// TODO: Add down-sampling
		int red,green,blue, index = 0;
		//
		for(int i = 0; i < imageHeight; i++) {
			for(int j = 0; j < imageWidth; j++) {
				red = ((rgb[index] >> 16) & 0xff);
				green = ((rgb[index] >> 8) & 0xff);
				blue = (rgb[index++] & 0xff);
				Y[i][j] = (0.299f*red + 0.587f*green + 0.114f*blue) - 128.0f;
				Cb[i][j] = - 0.1687f*red - 0.3313f*green + 0.5f*blue;
				Cr[i][j] = 0.5f*red - 0.4187f*green - 0.0813f*blue;
			}
		}
	}
	
	// Convert RGB to YCbCr with level shift (minus 128)		
	public static void RGB2YCbCr(int[][] red, int[][] green, int[][] blue, float[][] Y, float[][] Cb, float[][] Cr, int imageWidth, int imageHeight) {
		//
		for(int i = 0; i < imageHeight; i++) {
			for(int j = 0; j < imageWidth; j++) {
				Y[i][j] = 0.299f*red[i][j] + 0.587f*green[i][j] + 0.114f*blue[i][j] - 128.0f;
				Cb[i][j] = - 0.1687f*red[i][j] - 0.3313f*green[i][j] + 0.5f*blue[i][j];
				Cr[i][j] = 0.5f*red[i][j] - 0.4187f*green[i][j] - 0.0813f*blue[i][j];
			}
		}
	}
	
	// Convert RGB to YCbCr with alpha	
	public static byte[] RGB2YCbCrA(int[] rgba) {
		int alpha, red,green,blue, index = 0;
		byte[] ycbcra = new byte[rgba.length*4];
		//
		for(int i = 0; i < rgba.length; i++) {
			alpha = ((rgba[i] >> 24) & 0xff);
			red = ((rgba[i] >> 16) & 0xff);
			green = ((rgba[i] >> 8) & 0xff);
			blue = (rgba[i] & 0xff);
			ycbcra[index++] = (byte)(0.299f*red + 0.587f*green + 0.114f*blue);
			ycbcra[index++] = (byte)(- 0.1687f*red - 0.3313f*green + 0.5f*blue + 128.0f);
			ycbcra[index++] = (byte)(0.5f*red - 0.4187f*green - 0.0813f*blue + 128.f);
			ycbcra[index++] = (byte)alpha;
		}
		
		return ycbcra;
	}
	
	// Convert RGB to inverted YCCK with level shift (128 minus)
	public static void RGB2YCCK_Inverted(ICC_ColorSpace cmykColorSpace, int[] rgb, float[][] Y, float[][] Cb, float[][] Cr, float[][] K, int imageWidth, int imageHeight) {
		DataBuffer db = new DataBufferInt(rgb, rgb.length);
		WritableRaster raster = Raster.createPackedRaster(db, imageWidth, imageHeight, imageWidth,  new int[] {0x00ff0000, 0x0000ff00, 0x000000ff}, null);
		ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

		ColorConvertOp cco = new ColorConvertOp(sRGB, cmykColorSpace, null);
		
		BufferedImage rgbImage = new BufferedImage(new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff), raster, false, null);
		BufferedImage cmykImage = cco.filter(rgbImage, null);
		WritableRaster cmykRaster = cmykImage.getRaster();
		
		byte[] cmyk = (byte[])cmykRaster.getDataElements(0, 0, imageWidth, imageHeight, null);		
		float c, m, y;
		for(int i = 0, index = 0; i < imageHeight; i++) {
			for(int j = 0; j < imageWidth; j++) {
				c = 255.0f - (cmyk[index++]&0xff); // Red
				m = 255.0f - (cmyk[index++]&0xff); // Green
				y = 255.0f - (cmyk[index++]&0xff); // Blue							
				Y[i][j] = 128.0f - (c*0.299f + m*0.587f + y*0.114f);
				Cb[i][j] = 0.16874f*c + 0.33126f*m - 0.5f*y;
				Cr[i][j] = - 0.5f*c + 0.41869f*m + 0.08131f*y;
				K[i][j] = 128.0f - (cmyk[index++]&0xff);
			}
		}
	}
	
	// Prevent from instantiation
	private IMGUtils(){}
}