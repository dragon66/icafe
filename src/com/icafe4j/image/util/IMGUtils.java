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
 * IMGUtils.java
 *
 * Who   Date       Description
 * ====  =========  ==============================================================
 * WY    24Nov2017  Added invertBits(short[]) to handle TIFF 16 bit WhiteIsZero
 * WY    07Feb2016  Renamed methods related to popularity quantization
 * WY    31Jan2016  Removed ditherThreshold related method arguments
 * WY    31Dec2015  Removed error limit from dither_FloydSteinberg
 * WY    03Nov2015  Bug fix for reduceColors()
 * WY    16Sep2015  Added getScaledInstance() to IMGUtils
 * WY    15Sep2015  Added parameter to ImageParam to set quantization method
 * WY    10Sep2015  Removed ColorEntry from checkColorDepth()
 * WY    05Sep2015  Added ordered dither support for color images
 * WY    03Sep2015  Added ordered dither support for bilevel images
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

package com.icafe4j.image.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
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
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.ImageIO;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.meta.adobe.ImageResourceID;
import com.icafe4j.image.meta.adobe._8BIM;
import com.icafe4j.image.quant.NeuQuant;
import com.icafe4j.image.quant.QuantMethod;
import com.icafe4j.image.quant.WuQuant;
import com.icafe4j.image.writer.ImageWriter;
import com.icafe4j.image.writer.JPEGWriter;
import com.icafe4j.io.IOUtils;
import com.icafe4j.io.PeekHeadInputStream;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.util.IntHashtable;

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
	
	private static float GAMMA = 0.45455f; // Default gamma
	private static float DISPLAY_EXPONENT = 1.8f; // Default display exponent
	
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
	public static int[] checkColorDepth(int[] rgbTriplets, byte[] newPixels, final int[] colorPalette) {
		int index = 0;
		int temp = 0;
		int bitsPerPixel = 1;
		int transparent_index = -1;// Transparent color index
		int transparent_color = -1;// Transparent color
		int[] colorInfo = new int[2];// Return value
		
		IntHashtable<Integer> rgbHash = new IntHashtable<Integer>(1023);
				
		for (int i = 0; i < rgbTriplets.length; i++) {
			temp = (rgbTriplets[i]&0x00ffffff);

            if((rgbTriplets[i] >>> 24) == 0 ) {// Transparent
				if (transparent_index < 0) {
					transparent_index = index;
				    transparent_color = temp;// Remember transparent color
				}
				temp = Integer.MAX_VALUE;
			}	

            Integer entry = rgbHash.get(temp);
			
			if (entry!=null) {
				newPixels[i] = entry.byteValue();
			} else {
				if(index > 0xff) {// More than 256 colors, have to reduce
				 // Colors before saving as an indexed color image
					colorInfo[0] = 24;
					return colorInfo;
				}
				rgbHash.put(temp, index);
				newPixels[i] = (byte)index;
				colorPalette[index++] = ((0xff<<24)|temp);
			}
		}
		if(transparent_index >= 0)// This line could be used to set a different background color
			colorPalette[transparent_index] = transparent_color;
		// Return the actual bits per pixel and the transparent color index if any
		while ((1<<bitsPerPixel) < index)  bitsPerPixel++;
		
		colorInfo[0] = bitsPerPixel;
		colorInfo[1] = transparent_index;

        return colorInfo;
	}
	
	// Byte type image data gamma correction table
	public static byte[] createGammaTable(float gamma, float displayExponent) {
		 int size =  1 << 8;
		 byte[] gammaTable = new byte[size];
		 double decodingExponent = 1d / ((double)gamma * (double)displayExponent);
		 for (int i = 0; i < size; i++)
			 gammaTable[i] = (byte)(Math.pow((double)i / (size - 1), decodingExponent) * (size - 1));
		 
		 return gammaTable;
    }
	
	// Gamma correction for palette based image data
	public static void correctGamma(int[] rgbColorPalette, byte[] gammaTable) {
		for(int i = 0; i < rgbColorPalette.length; i++) {
			byte red = gammaTable[((rgbColorPalette[i]&0xff0000)>>16)];
			byte green = gammaTable[((rgbColorPalette[i]&0x00ff00)>>8)];
			byte blue = gammaTable[(rgbColorPalette[i]&0x0000ff)];
			rgbColorPalette[i] = ((rgbColorPalette[i]&0xff000000)|((red&0xff)<<16)|((green&0xff)<<8)|(blue&0xff));
		}
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
			
		return getScaledInstance(original, thumbnailWidth, thumbnailHeight,				
				RenderingHints.VALUE_INTERPOLATION_BICUBIC,
				true);
	}
	
	/**
	 * Wraps a BufferedImage inside a Photoshop _8BIM
	 * @param thumbnail input thumbnail image
	 * @return a Photoshop _8BIM
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
	 * Dither gray-scale image using Bayer threshold matrix
	 *
	 * @param gray input gray-scale image array - also as output BW image array
	 * @param mask a mask array for transparent pixels - 0 transparent, 1 opaque
	 * @param width image width
	 * @param height image height
	 * @param threshold Bayer threshold matrix used to convert to BW image
	 */	
	public static void dither_Bayer(byte[] gray, byte[] mask, int width, int height, int[][] threshold) {
		int level = threshold.length;
		int scaler = level*level + 1;
		
		for(int i = 0; i < level; i++)
			for(int j = 0; j < level; j++)
				threshold[i][j] = ((threshold[i][j]<<8)/scaler); // Scale to 256 colors
					
		for (int row = 0, index = 0; row < height; row++)
		{
			for (int col = 0; col < width; index++, col++) {
				if(mask[index] == 0) { 
					// make transparency color white (Assume WHITE_IS_ZERO)
					gray[index] = 0; 
					continue; 
				}
				// Apply ordered dither
				int intensity = (gray[index]&0xff);
				// Find the nearest color index	- black or white
				if(intensity <=  threshold[row%level][col%level]) {
					gray[index] = 1;
				} else {
					gray[index] = 0;
				}
			}
		}
	}
	
	/**
	 * Dither gray-scale image using Floyd-Steinberg error diffusion
	 *
	 * @param gray input gray-scale image array - also as output BW image array
	 * @param mask a mask array for transparent pixels - 0 transparent, 1 opaque
	 * @param width image width
	 * @param height image height
	 * @param threshold gray-scale threshold to convert to BW image
	 */	
	public static void dither_FloydSteinberg(byte[] gray, byte[] mask, int width, int height, int threshold)	{
		// Define error arrays
		// Errors for the current line
		int[] tempErr;
		int[] thisErr = new int[width + 2];
		// Errors for the following line
		int[] nextErr = new int[width + 2];
			
		for (int row = 0, index = 0; row < height; row++)
		{
			for (int col = 0; col < width; index++, col++) {
				if(mask[index] == 0) { 
					// make transparency color white (Assume WHITE_IS_ZERO)
					gray[index] = 0; 
					continue; 
				}
				
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
				// Diffuse error
				thisErr[col + 2] += ((err*7)/16);
				nextErr[col    ] += ((err*3)/16);
				nextErr[col + 1] += ((err*5)/16);
				nextErr[col + 2] += ((err)/16);
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
	 * Dither color image using Bayer threshold matrix. This method sometimes makes the
	 * images look too bright. Gamma correction usually can compensate for this problem.
	 * 
	 * @param rgbTriplet input pixels in ARGB format
	 * @param width image width
 	 * @param height image height
	 * @param newPixels pixel array after dither
	 * @param no_of_color actual number of colors used by the color palette
	 * @param colorPalette color palette
	 * @param transparent_index transparent color index for the color palette
	 * @param threshold Bayer threshold matrix
	 */
	public static void dither_Bayer(int[] rgbTriplet, int width, int height, byte[] newPixels, int no_of_color, 
            int[] colorPalette, int transparent_index, int[][] threshold)
	{
		int index = 0, red, green, blue;
		InverseColorMap invMap;
		
		invMap = new InverseColorMap();
		invMap.createInverseMap(no_of_color, colorPalette);
		
		int level = threshold.length;
		int scaler = threshold.length*threshold.length + 1;
		
		for (int row = 0; row < height; row++)
		{
			for (int col = 0; col < width; index++, col++)
			{				
				if((rgbTriplet[index] >>> 24) < 0x80 )// Transparent, no dither
				{
					newPixels[index] = (byte)transparent_index;	
					continue;
				}
				// Diffuse errors
				red = ((rgbTriplet[index]&0xff0000)>>>16);
				red += red*threshold[row%level][col%level]/scaler;
				if (red > 255) red = 255;
				else if (red < 0) red = 0;
				
				green = ((rgbTriplet[index]&0x00ff00)>>>8);
				green += green*threshold[row%level][col%level]/scaler;
				if (green > 255) green = 255;
				else if (green < 0) green = 0;
				
				blue = (rgbTriplet[index]&0x0000ff);
				blue += blue*threshold[row%level][col%level]/scaler;
				if (blue > 255) blue = 255;
				else if (blue < 0) blue = 0;			
				// Find the nearest color index for this pixel
				newPixels[index] = (byte)invMap.getNearestColorIndex(red, green, blue);
			}
		}
		// Fixed value Gamma correction
		correctGamma(colorPalette, IMGUtils.createGammaTable(GAMMA, DISPLAY_EXPONENT));
	}
	
	/**
	 * Floyd-Steinberg dithering, based on PPMQuant.c by Jef Poskanzer <jef@acme.com>.
	 * For simplicity, only forward error diffusion is implemented.
	 * 
	 * @param rgbTriplet input pixels in ARGB format 
	 * @param width width of the image
	 * @param height height of the image
	 * @param newPixels output pixels
	 * @param no_of_color number of colors used
	 * @param colorPalette color palette
	 * @param transparent_index transparent color index of the color palette
	 */
	public static void dither_FloydSteinberg(int[] rgbTriplet, int width, int height, byte[] newPixels, int no_of_color, 
		                                      int[] colorPalette, int transparent_index)
	{
		int index = 0, index1 = 0, err1, err2, err3, red, green, blue;
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

		InverseColorMap invMap;

		invMap = new InverseColorMap();
		invMap.createInverseMap(no_of_color, colorPalette);

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; index1++, col++)	{
				 // Transparent, no dither
				if((rgbTriplet[index1] >>> 24) < 0x80 ) {
					newPixels[index1] = (byte)transparent_index;
					continue;
		        }
		
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
				err2 = green - ((colorPalette[index]>>8)&0xff);// Green channel
			    err3 = blue  -  (colorPalette[index]&0xff);// Blue channel
			    // Diffuse error
				// Red
                thisErrR[col + 2] += ((err1*7)/16);
				nextErrR[col    ] += ((err1*3)/16);
				nextErrR[col + 1] += ((err1*5)/16);
				nextErrR[col + 2] += ((err1)/16);
                // Green
                thisErrG[col + 2] += ((err2*7)/16);
				nextErrG[col    ] += ((err2*3)/16);
				nextErrG[col + 1] += ((err2*5)/16);
				nextErrG[col + 2] += ((err2)/16);
				// Blue
				thisErrB[col + 2] += ((err3*7)/16);
				nextErrB[col    ] += ((err3*3)/16);
				nextErrB[col + 1] += ((err3*5)/16);
				nextErrB[col + 2] += ((err3)/16);
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
			Arrays.fill(nextErrG, 0);
			Arrays.fill(nextErrB, 0);
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
	
	/**
	 * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in down-scaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
	 // From https://today.java.net/article/2007/03/30/perils-imagegetscaledinstance
    public static BufferedImage getScaledInstance(BufferedImage img,
                                                  int targetWidth,
                                                  int targetHeight,
                                                  Object hint,
                                                  boolean higherQuality)
    {
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;        
        int w = img.getWidth(), h = img.getHeight();
        
        // Use one-step technique: scale directly from original
        // size to target size with a single drawImage() call
        if(w < targetWidth || h < targetHeight || !higherQuality)
            return scaleImage(ret, type, hint, targetWidth, targetHeight);
       
        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached    
        do {
            if (w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            ret = scaleImage(ret, type, hint, w, h);
            
        } while (w != targetWidth || h != targetHeight);

        return ret;
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
	
	public static ImageType guessImageType(PeekHeadInputStream is) throws IOException {
		// Read the first ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes
		byte[] magicNumber = is.peek(ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = guessImageType(magicNumber);
		
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
	public static void invertBits(byte[] input, int pixelStride) {
		for(int i = input.length - 1; i >= 0; i -= pixelStride) {
			input[i] = (byte)~input[i];
		}
	}
	
	// Change the bit color sex of a short array
	public static void invertBits(short[] input, int pixelStride) {
		for(int i = input.length - 1; i >= 0; i -= pixelStride) {
			input[i] = (short)~input[i];
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
	private static int[] reduceColorsPopularity(int[] rgbTriplets, int colorDepth, byte[] newPixels, final int[] colorPalette)	{
		if(colorDepth > 8 || colorDepth < 1) 
			throw new IllegalArgumentException("Invalid color depth " + colorDepth);
		int no_of_color = 1<<colorDepth;
		int[] colorFreq = new int[65536];
		int[] indexColor = new int[65536];
		int[] clrPalRed = new int[no_of_color];
        int[] clrPalGreen = new int[no_of_color];
	    int[] clrPalBlue = new int[no_of_color];
	    int[] clrPalAlpha = new int[no_of_color];
		int[] colorInfo = new int[2];// Return value
		int[] colorIndex;
		int bitsPerPixel = 1;
		int transparent_color = -1;// Transparent color 
		int transparent_index = -1;// Transparent color index
		
		int red, green, blue, alpha, index, colorCount, temp, temp1, err1, err2;
        // Get the 4 most significant bits of red, green, blue, and alpha to
        // form a 16 bits integer and determine the frequencies of different 
        // values
		for (int i = 0; i < rgbTriplets.length; i++) {
			if((rgbTriplets[i] >>> 24) == 0) { // Transparent
				if (transparent_color < 0)	// Find the transparent color	
					transparent_color = rgbTriplets[i];			
			}
			red = ((rgbTriplets[i]&0xf00000)>>>20);
			green = ((rgbTriplets[i]&0x00f000)>>>8);
			blue = ((rgbTriplets[i]&0x0000f0)<<4);
			alpha = ((rgbTriplets[i]&0xf0000000)>>>16);
			
			index = (alpha|red|green|blue);
			colorFreq[index]++;
		}
        // Throw away the zero frequency items and/or transparent items
        colorCount = 0;
        for (int i = 0; i < 65536; i++ ) {
			if (colorFreq[i] != 0 && (i&0xf000) != 0) {
				colorFreq[colorCount] = colorFreq[i];
				indexColor[colorCount++] = i;
			}
        }
        // Sort the colors according to their frequencies
       	// Shell sort
        int gap = 1;
  	    // Generate Knuth sequence 1, 4, 13, 40, 121, 364,1093, 3280, 9841 ...
  	    while(gap < colorCount) gap = 3*gap + 1;
	    while ( gap > 0 ) {
		   for (int i = gap; i < colorCount; i++) {
			   temp = colorFreq[i];
			   temp1 = indexColor[i];
			   int j = i;
			   while ( j >= gap && temp >= colorFreq[j - gap]) {
				   colorFreq[j] = colorFreq[j - gap];
				   indexColor[j] = indexColor[j - gap];
				   j -= gap;
			   }
			   colorFreq[j] = temp;
			   indexColor[j] = temp1;
		   }
		   gap /= 3;
	    }
	   	colorIndex = new int[no_of_color>=colorCount?no_of_color:colorCount];
		// Take the first no_of_color items as the palette 
		for (int i = 0; i < no_of_color; i++) {
            clrPalBlue[i]  = ((indexColor[i]&0xf00)>>>4);
			clrPalGreen[i] =(indexColor[i]&0x0f0);
			clrPalRed[i]  = ((indexColor[i]&0x00f)<<4);
			clrPalAlpha[i] = ((indexColor[i]&0xf000)>>>8);
	
			colorPalette[i] = ((clrPalAlpha[i]<<24)|(clrPalRed[i]<<16)|(clrPalGreen[i]<<8)|clrPalBlue[i]);
			colorIndex[i] = i;
		}
		if(transparent_color >= 0)// There is a transparent color
			no_of_color--;// The available color is one less
		// Find the nearest color for the other colors if there are more colors than no_of_color
        if (colorCount > no_of_color) {
			for (int i = no_of_color; i < colorCount; i++) {
				index = 0;

				blue   = ((indexColor[i]&0xf00)>>>4);
				green = (indexColor[i]&0x0f0);
				red  = ((indexColor[i]&0x00f)<<4);
				alpha = ((indexColor[i]&0xf000)>>>8);

				err1 =  (red-clrPalRed[0])*(red-clrPalRed[0])+(green-clrPalGreen[0])*(green-clrPalGreen[0])+
					    (blue-clrPalBlue[0])*(blue-clrPalBlue[0]) + (alpha - clrPalAlpha[0])*(alpha - clrPalAlpha[0]);
                
				for (int j = 1; j < no_of_color; j++) {
					err2 = (red-clrPalRed[j])*(red-clrPalRed[j])+(green-clrPalGreen[j])*(green-clrPalGreen[j])+
					       (blue-clrPalBlue[j])*(blue-clrPalBlue[j]) + (alpha - clrPalAlpha[j])*(alpha - clrPalAlpha[j]);
					if (err2 < err1) {
						err1 = err2;
                        index = j;
					}
				}
                colorIndex[i] = index;
			}
        }
        // Reduce colors
		for (int i = 0; i < colorCount; i++ ) {
			// Here and after colorFreq is used to keep the
			// index of colorIndex array for different colors 
			colorFreq[indexColor[i]] = i;
		}
		if(transparent_color >= 0)// There is a transparent color
			colorCount++;// Count in the transparent color
		
		// Determine the actual bits we need
		while ((1<<bitsPerPixel) < colorCount)  bitsPerPixel++;
		if(bitsPerPixel > colorDepth) bitsPerPixel = colorDepth;
	
		if(transparent_color >= 0) {
			// Set the colorPalette for the transparent color
			transparent_index = (1<<bitsPerPixel)-1;
			colorPalette[transparent_index] = transparent_color;
		}
				
		for (int i = 0; i < rgbTriplets.length; i++) {
			red = ((rgbTriplets[i]&0xf00000)>>>20);
			green = ((rgbTriplets[i]&0x00f000)>>>8);
			blue = ((rgbTriplets[i]&0x0000f0)<<4);
			alpha = ((rgbTriplets[i]&0xf0000000)>>>16);
			
			index = (alpha|red|green|blue);
			
		    // Write the color index of different pixels to a new data array
			if((index&0xf000) != 0) { // Non-transparent pixel
				newPixels[i] = (byte)colorIndex[colorFreq[index]];
			} else { // Transparent pixel
				newPixels[i] = (byte)transparent_index;	
			}
	    }
		// Return the actual bits per pixel and the transparent color index if any
		colorInfo[0] = bitsPerPixel;
		colorInfo[1] = transparent_index;
		
		return colorInfo;
	}
	
	// Color quantization
	public static int[] reduceColors(QuantMethod quantMethod, int[] rgbTriplets, int colorDepth, byte[] newPixels, final int[] colorPalette)	{
		int[] colorInfo = new int[2];
		if(quantMethod == QuantMethod.WU_QUANT)
			new WuQuant(rgbTriplets, 1<<colorDepth).quantize(newPixels, colorPalette, colorInfo);
		else if(quantMethod == QuantMethod.NEU_QUANT)
			new NeuQuant(rgbTriplets).quantize(newPixels, colorPalette, colorInfo);
		else
			colorInfo = reduceColorsPopularity(rgbTriplets, colorDepth, newPixels, colorPalette);
		
		return colorInfo;
	}
	
	/**
	 * Reduces a true color image to an indexed-color image with no_of_color using "Popularity algorithm"
	 * followed by Floyd-Steinberg error diffusion dithering.
	 */
	public static int[] reduceColorsDiffusionDither(int[] rgbTriplets, int width, int height, int colorDepth, byte[] newPixels, final int[] colorPalette)	{
		if(colorDepth > 8 || colorDepth < 1) 
			throw new IllegalArgumentException("Invalid color depth " + colorDepth);
		int[] colorInfo = new int[2];
		int colors = reduceColors(rgbTriplets, colorDepth, colorPalette, colorInfo);
		// Call Floyd-Steinberg dither
		dither_FloydSteinberg(rgbTriplets, width, height, newPixels, colors, colorPalette, colorInfo[1]);
		// Return the actual bits per pixel and the transparent color index if any

		return colorInfo;
	}
	
	public static int[] reduceColorsDiffusionDither(QuantMethod quantMethod, int[] rgbTriplets, int width, int height, int colorDepth, byte[] newPixels, final int[] colorPalette)	{
		if(colorDepth > 8 || colorDepth < 1) 
			throw new IllegalArgumentException("Invalid color depth " + colorDepth);
		int[] colorInfo = new int[2];
		int colors = 0;
		if(quantMethod == QuantMethod.WU_QUANT)
			colors = new WuQuant(rgbTriplets, 1<<colorDepth).quantize(colorPalette, colorInfo);
		else if(quantMethod == QuantMethod.NEU_QUANT)
			colors = new NeuQuant(rgbTriplets).quantize(colorPalette, colorInfo);
		else
			colors = reduceColors(rgbTriplets, colorDepth, colorPalette, colorInfo);
		// Call Floyd-Steinberg dither
		dither_FloydSteinberg(rgbTriplets, width, height, newPixels, colors, colorPalette, colorInfo[1]);
		// Return the actual bits per pixel and the transparent color index if any

		return colorInfo;
	}
	
	public static int[] reduceColorsOrderedDither(int[] rgbTriplet, int width, int height, int colorDepth, byte[] newPixels, final int[] colorPalette, int[][] threshold)	{
		if(colorDepth > 8 || colorDepth < 1) 
			throw new IllegalArgumentException("Invalid color depth " + colorDepth);
		int[] colorInfo = new int[2];
		int colors = reduceColors(rgbTriplet, colorDepth, colorPalette, colorInfo);
		
		dither_Bayer(rgbTriplet, width, height, newPixels, colors, colorPalette, colorInfo[1], threshold);
		// Return the actual bits per pixel and the transparent color index if any

		return colorInfo;
	}
	
	public static int[] reduceColorsOrderedDither(QuantMethod quantMethod, int[] rgbTriplets, int width, int height, int colorDepth, byte[] newPixels, final int[] colorPalette, int[][] threshold)	{
		if(colorDepth > 8 || colorDepth < 1) 
			throw new IllegalArgumentException("Invalid color depth " + colorDepth);
		int[] colorInfo = new int[2];
		int colors = 0;
		if(quantMethod == QuantMethod.WU_QUANT)
			colors = new WuQuant(rgbTriplets, 1<<colorDepth).quantize(colorPalette, colorInfo);
		else if(quantMethod == QuantMethod.NEU_QUANT)
			colors = new NeuQuant(rgbTriplets).quantize(colorPalette, colorInfo);
		else
			colors = reduceColors(rgbTriplets, colorDepth, colorPalette, colorInfo);
		dither_Bayer(rgbTriplets, width, height, newPixels, colors, colorPalette, colorInfo[1], threshold);
		// Return the actual bits per pixel and the transparent color index if any

		return colorInfo;
	}
	
	private static int reduceColors(int[] rgbTriplet, int colorDepth, final int[] colorPalette, int[] colorInfo)	{
		if(colorDepth > 8 || colorDepth < 1) 
			throw new IllegalArgumentException("Invalid color depth " + colorDepth);
		int no_of_color = 1<<colorDepth;
		int[] colorFreq = new int[4096];
		int[] indexColor = new int[4096];
		int[] colorIndex;
		int bitsPerPixel = 1;
		int transparent_color = -1;// Transparent color 
		int transparent_index = -1;// Transparent color index
		
		int red, green, blue, index, colorCount, temp, temp1;
        // Get the 4 most significant bits of red, green and blue to
        // form a 12 bits integer and determine the frequencies of different 
        // values
		for (int i = 0; i < rgbTriplet.length; i++)	{
			if((rgbTriplet[i] >>> 24) < 0x80 ) { // Transparent
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
        for (int i = 0; i < 4096; i++ ) {
			if (colorFreq[i] != 0) {
				colorFreq[colorCount]=colorFreq[i];
				indexColor[colorCount++] = i;
			}
        }
        // Sort the colors according to their frequencies
     	// Shell sort
  	    int gap = 1;
  	    // Generate Knuth sequence 1, 4, 13, 40, 121, 364,1093, 3280, 9841 ...
  	    while(gap < colorCount) gap = 3*gap + 1;
	    while ( gap > 0 ) {
		   for (int i = gap; i < colorCount; i++) {
			   temp = colorFreq[i];
			   temp1 = indexColor[i];
			   int j = i;
			   while ( j >= gap && temp >= colorFreq[j - gap]) {
				   colorFreq[j] = colorFreq[j - gap];
				   indexColor[j] = indexColor[j - gap];
				   j -= gap;
			   }
			   colorFreq[j] = temp;
			   indexColor[j] = temp1;
		   }
		   gap /= 3;
	    }
	   	colorIndex = new int[no_of_color];
		// Take the first no_of_color items as the palette 
		for (int i = 0; i < no_of_color; i++) {
            blue  = ((indexColor[i]&0xf00)>>>4);
			green = (indexColor[i]&0x0f0);
			red  =  ((indexColor[i]&0x00f)<<4);

			colorPalette[i] = ((0xff << 24)|(red << 16)|(green << 8)|blue);
			colorIndex[i] = i;
		}		
		if(transparent_color >= 0) { // There is a transparent color
			no_of_color--;// The available color is one less
			colorCount++;// Count in the transparent color to determine color depth
		}
		// Determine the actual bits we need
		while ((1<<bitsPerPixel) < colorCount)  bitsPerPixel++;
		
		if(bitsPerPixel > colorDepth) bitsPerPixel = colorDepth;
		
		if(transparent_color >= 0) { // Set the colorPalette for the transparent color
			transparent_index = (1<<bitsPerPixel)-1;
			colorPalette[transparent_index] = transparent_color;
			colorCount--;//We need the actual number of color now
		}	
		// Return the actual bits per pixel and the transparent color index if any
		colorInfo[0] = bitsPerPixel;
		colorInfo[1] = transparent_index;
		
		return (colorCount < no_of_color)?colorCount:no_of_color; // Actual colors
	}
	
	// This works quite well without dither
	public static byte[] rgb2bilevel(int[] rgb) {
		// RGB to gray-scale
		byte[] pixels = new byte[rgb.length];
		long sum = 0;
		
		for(int i = 0; i < rgb.length; i++) {
			if((rgb[i] >>> 24) < 0x80) pixels[i] = (byte)0xff; // Dealing with transparency color
			else
				pixels[i] = (byte)(((rgb[i]>>16)&0xff)*0.2126 + ((rgb[i]>>8)&0xff)*0.7152 + (rgb[i]&0xff)*0.0722);
			sum += (pixels[i]&0xff);
		}
		
		// Calculate threshold
		int threshold = (int)(sum/pixels.length);
		
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
	
	public static byte[] rgb2bilevelOrderedDither(int[] rgb, int imageWidth, int imageHeight, int[][] threshold) {
		// RGB to gray-scale
		byte[] pixels = new byte[rgb.length];
		byte[] mask = new byte[rgb.length];
		
		Arrays.fill(mask, (byte)0x01);
		
		for(int i = 0; i < rgb.length; i++) {
			if((rgb[i] >>> 24) < 0x80) {
				pixels[i] = (byte)0xff; // Dealing with transparency color
				mask[i] = 0x00;
			} else
				pixels[i] = (byte)(((rgb[i]>>16)&0xff)*0.2126 + ((rgb[i]>>8)&0xff)*0.7152 + (rgb[i]&0xff)*0.0722);
		}
		
		IMGUtils.dither_Bayer(pixels, mask, imageWidth, imageHeight, threshold);
		
		return pixels;
	}
	
	/**
	 * RGB to bilevel image conversion with Floyd-Steinberg dither
	 * 
	 * @param rgb input RGB image array (format: ARGBARGBARGB...)
	 * @param imageWidth image width
	 * @param imageHeight image height
	 * @return byte array for the BW image
	 */
	public static byte[] rgb2bilevelDiffusionDither(int[] rgb, int imageWidth, int imageHeight) {
		// RGB to gray-scale
		byte[] pixels = new byte[rgb.length];
		byte[] mask = new byte[rgb.length];
		long sum = 0;
		
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
		int threshold = (int)(sum/pixels.length);
		
		IMGUtils.dither_FloydSteinberg(pixels, mask, imageWidth, imageHeight, threshold);
		
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
	
	// From https://today.java.net/article/2007/03/30/perils-imagegetscaledinstance
	private static BufferedImage scaleImage(BufferedImage orig, int type, Object hint, int w, int h) {
		BufferedImage tmp = new BufferedImage(w, h, type);
		Graphics2D g2 = tmp.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
		g2.drawImage(orig, 0, 0, w, h, null);
		g2.dispose();
		
		return tmp;
	}
	
	// Prevent from instantiation
	private IMGUtils(){}
}