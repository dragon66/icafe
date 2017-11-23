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
 * PNGWriter.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    21Jun2015  Removed copyright notice from generated PNG images
 * WY    01Mar2014  Added apply_adamptive_filter method
 */

package com.icafe4j.image.writer;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.icafe4j.image.ImageColorType;
import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.options.ImageOptions;
import com.icafe4j.image.options.PNGOptions;
import com.icafe4j.image.png.Chunk;
import com.icafe4j.image.png.ChunkType;
import com.icafe4j.image.png.ColorType;
import com.icafe4j.image.png.Filter;
import com.icafe4j.image.png.IDATBuilder;
import com.icafe4j.image.png.IENDBuilder;
import com.icafe4j.image.png.IHDRBuilder;
import com.icafe4j.image.png.PLTEBuilder;
import com.icafe4j.image.png.PNGTweaker;
import com.icafe4j.image.png.TIMEBuilder;
import com.icafe4j.image.png.TRNSBuilder;
import com.icafe4j.image.png.TextBuilder;
import com.icafe4j.image.quant.DitherMethod;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.io.IOUtils;
import com.icafe4j.util.ArrayUtils;

/** 
 * PNG image writer
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/29/2013
 */
public class PNGWriter extends ImageWriter {
	// Parameters to control compression
	boolean isApplyAdaptiveFilter = false;	
	int filterType = Filter.NONE;		
	int compressionLevel = 4;
	ImageParam imageParam;
	// A collection of chunks representing the PNG image.
	private List<Chunk> chunks = new ArrayList<Chunk>(10);
	
	/** PNG signature constant */
    private static final long SIGNATURE = 0x89504E470D0A1A0AL;
    
    public PNGWriter() {}
    
    public PNGWriter(ImageParam param) {
    	super(param);
    }
	
	// Apply dynamic filtering by "the minimum sum of absolute differences" (MSAD) heuristic 
	private static void apply_adaptive_filter(int[] filter_type, byte[] pixBytes, int height, int bytesPerPixel, int bytesPerScanLine) {
		// keep track of the current row
		byte[] tempRow = new byte[bytesPerScanLine];
		byte[] filteredRow = new byte[bytesPerScanLine];
		
		for (int j = height - 1, offset = pixBytes.length - bytesPerScanLine; j >= 0; j--, offset -= bytesPerScanLine)
		{
			System.arraycopy(pixBytes, offset, tempRow, 0, bytesPerScanLine);
			
			Filter.filter_sub(bytesPerPixel, bytesPerScanLine, pixBytes, offset);
			int sum = calculateMSAD(pixBytes, offset, bytesPerScanLine);
			filter_type[j] = Filter.SUB;
			System.arraycopy(pixBytes, offset, filteredRow, 0, bytesPerScanLine);
			
			System.arraycopy(tempRow, 0, pixBytes, offset, bytesPerScanLine);			
			Filter.filter_up(bytesPerScanLine, pixBytes, offset);
			int newSum = calculateMSAD(pixBytes, offset, bytesPerScanLine);
			
			if(newSum < sum) {
				sum = newSum;
				filter_type[j] = Filter.UP;
				System.arraycopy(pixBytes, offset, filteredRow, 0, bytesPerScanLine);		
			}
			
			System.arraycopy(tempRow, 0, pixBytes, offset, bytesPerScanLine);
			Filter.filter_average(bytesPerPixel, bytesPerScanLine, pixBytes, offset);
			newSum = calculateMSAD(pixBytes, offset, bytesPerScanLine);
			
			if(newSum < sum) {
				sum = newSum;
				filter_type[j] = Filter.AVERAGE;
				System.arraycopy(pixBytes, offset, filteredRow, 0, bytesPerScanLine);
			}
			
			System.arraycopy(tempRow, 0, pixBytes, offset, bytesPerScanLine);			
			Filter.filter_paeth(bytesPerPixel, bytesPerScanLine, pixBytes, offset);
			newSum = calculateMSAD(pixBytes, offset, bytesPerScanLine);
			
			if(newSum < sum) {
				sum = newSum;
				filter_type[j] = Filter.PAETH;
				System.arraycopy(pixBytes, offset, filteredRow, 0, bytesPerScanLine);
			}
			
			System.arraycopy(filteredRow, 0, pixBytes, offset, bytesPerScanLine);         			
		}
	}
	
	// Apply filter using the predefined filter type array
	private static void apply_filter(int[] filter_type, byte[] pixBytes, int height, int bytesPerPixel, int bytesPerScanLine) {
		//
		for (int j = height - 1, offset = pixBytes.length - bytesPerScanLine; j >= 0; j--, offset -= bytesPerScanLine)
		{
			switch (filter_type[j]) {
		  		case Filter.NONE:
		  			break;
		  		case Filter.SUB:		  			
		  			Filter.filter_sub(bytesPerPixel, bytesPerScanLine, pixBytes, offset);
		  			break;
		  		case Filter.UP:
		  			Filter.filter_up(bytesPerScanLine, pixBytes, offset);
		  			break;
		  		case Filter.AVERAGE:
		  			Filter.filter_average(bytesPerPixel, bytesPerScanLine, pixBytes, offset);
		  			break;
		  		case Filter.PAETH:
		  			Filter.filter_paeth(bytesPerPixel, bytesPerScanLine, pixBytes, offset);
		  			break;
		  		default:
		  			break;
            }
		}
	}
	
	// Calculate minimum sum of absolute differences
	private static int calculateMSAD(byte[] input, int offset, int length) {
		int sum = 0;
		
		for(int i = offset + length - 1; i >= offset; i--) {
			sum += ((input[i] > 0)? input[i]:-input[i]);
		}
		
		return sum;
	}
	
	// Determine number of bytes per scan-line for gray-scale image
	private static int getBytesPerScanLine(int bitsPerPixel, int imageWidth, boolean hasAlpha) {
		//
		int bytesPerPixel = (hasAlpha?2:1);		
			
		int bytesPerScanLine = imageWidth*bytesPerPixel;
		
		switch(bitsPerPixel) {
			case 1:
				bytesPerScanLine = (imageWidth>>>3) + (((imageWidth%8)==0)?0:1);
				break;
			case 2:
				bytesPerScanLine = (imageWidth>>>2) + (((imageWidth%4)==0)?0:1);
				break;
			case 4:
				bytesPerScanLine = (imageWidth>>>1) + (((imageWidth%2)==0)?0:1);
				break;
			default:
		}
		
		return bytesPerScanLine;
	}
	
	private void addTextChunks(List<Chunk> chunks) {
		// Add text chunks
		TextBuilder txtBuilder = new TextBuilder(ChunkType.TEXT);
		txtBuilder.keyword("Software").text("ICAFE - https://github.com/dragon66/icafe");
        chunks.add(txtBuilder.build());
  	}
	
	private void addTimeChunk(List<Chunk> chunks) {
		// PNG sTIME chunk format is always UTC
		TIMEBuilder timeBuilder = new TIMEBuilder();
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		timeBuilder.calendar(calendar);
		chunks.add(timeBuilder.build());		
	}
	
	@Override
	public ImageType getImageType() {
		return ImageType.PNG;
	}
	
	// Reset writer to write another image
	private void reset() {
		chunks.clear();
		isApplyAdaptiveFilter = false;	
		filterType = Filter.NONE;		
		compressionLevel = 4;	
	}
	
	protected void write(int[] pixels, int imageWidth, int imageHeight, OutputStream os) throws Exception 
    {	
		IOUtils.writeLongMM(os, SIGNATURE);
		
		reset(); // Reset writer in case we are going to write multiple images
		
		addTextChunks(chunks);
		addTimeChunk(chunks);
		
		imageParam = getImageParam();		
		ImageOptions options = imageParam.getImageOptions();
		
		if(options instanceof PNGOptions) {
			PNGOptions pngOptions = (PNGOptions)options;
			isApplyAdaptiveFilter = pngOptions.isApplyAdaptiveFilter();
			filterType = pngOptions.getFilterType();
			compressionLevel = pngOptions.getCompressionLevel();
		}
		
		boolean noAlpha = !imageParam.hasAlpha();
		// Determine type of image to write
		if(imageParam.getColorType() == ImageColorType.INDEXED) {
			writeIndexed(pixels, imageWidth, imageHeight, os);
		} else if(imageParam.getColorType() == ImageColorType.GRAY_SCALE) {
			if(noAlpha) {
				writeGrayScale(IMGUtils.rgb2grayscale(pixels), imageWidth, imageHeight, false, os);
			} else
				writeGrayScale(IMGUtils.rgb2grayscaleA(pixels), imageWidth, imageHeight, true, os);
		} else {
			writeRGB(pixels, imageWidth, imageHeight, os);
		}		
		/*
		 * We could have put all the chunks including IDAT and IEND into the chunks list and serialize them in the
		 * end. But this is memory consuming. To save memory, we don't put IDAT chunks into chunks list but write 
		 * them right before IEND.
		 */
		new IENDBuilder().build().write(os);
    }
	
	private void writeGrayScale(byte[] pixels, int imageWidth, int imageHeight, boolean hasAlpha, OutputStream os) throws Exception {
		// The rule of thumb is don't use any filter for gray-scale image but in some cases, PAETH or adaptive filter does much better
		// Add IHDR chunk
		IHDRBuilder hdrBuilder = new IHDRBuilder().width(imageWidth).height(imageHeight).compressionMethod(0).
				filterMethod(0).interlaceMethod(0);
		// Determine the gray-scale type
		if(hasAlpha) hdrBuilder.colorType(ColorType.GRAY_SCALE_WITH_ALPHA);
		else hdrBuilder.colorType(ColorType.GRAY_SCALE);
		// PNG allows for 8 and 16 bits for gray-scale with alpha, for now, only 8 bit is supported
		int bitsPerPixel = 8;		
		
		if(!hasAlpha) {
			// Get the actually bits needed to represent this gray-scale image
			bitsPerPixel = IMGUtils.getBitDepth(pixels, false);
			// PNG allows for 1, 2, 4, 8 and 16 bits grays-scale image without alpha sample
			switch(bitsPerPixel) {
				case 3:
					bitsPerPixel = 4;
					break;
				case 5:
				case 6:
				case 7:
					bitsPerPixel = 8;
					break;
				default:
			}
		}
		
		// Scale input if needed.
		if(bitsPerPixel != 8) {
			for(int l = 0; l < pixels.length; l++) {
				pixels[l] = (byte)((pixels[l]<<bitsPerPixel)>>8);
			}
			// Pack bytePixels according to bitsPerPixel value
			pixels = ArrayUtils.packByteArray(pixels, imageWidth, 0, bitsPerPixel, imageWidth*imageHeight);
		}
		
		chunks.add(hdrBuilder.bitDepth(bitsPerPixel).build());
	
		if(!hasAlpha && imageParam.isTransparent()) {
			// Add Transparent chunk
			TRNSBuilder tBuilder = new TRNSBuilder(0);
			int transparentColor = imageParam.getTransparentColor();		
			byte trans_color = (byte)(((transparentColor>>16)&0xff)*0.2126 + ((transparentColor>>8)&0xff)*0.7152 + (transparentColor&0xff)*0.0722);
			byte[] alpha = new byte[] {0, (byte)((trans_color<<bitsPerPixel)>>8)};
			
			chunks.add(tBuilder.alpha(alpha).build());
		}
		
		PNGTweaker.serializeChunks(chunks, os);
		
		int[] filter_type = new int[imageHeight];
		Arrays.fill(filter_type, filterType);
		
		int bytesPerPixel = (hasAlpha?2:1);		
		
		int bytesPerScanLine = getBytesPerScanLine(bitsPerPixel, imageWidth, hasAlpha);
		
		if(bitsPerPixel == 8) {
			if(isApplyAdaptiveFilter) {
				apply_adaptive_filter(filter_type, pixels, imageHeight, bytesPerPixel, bytesPerScanLine);
			} else if(filterType != Filter.NONE) {
				apply_filter(filter_type, pixels, imageHeight, bytesPerPixel, bytesPerScanLine);
			}
		}
		
		byte[] buffer = new byte[bytesPerScanLine + 1];
		
		IDATBuilder builder = new IDATBuilder(compressionLevel);
		
		// How many bytes to buffer before creating an IDAT chunk
		int bufferLen = bytesPerPixel * imageWidth * imageHeight / 5; // We are expecting 5 IDAT chunks
		int counter = 0;
		
		for (int i = 0, j = 0; i < imageHeight; i++, j += bytesPerScanLine) {
			buffer = new byte[bytesPerScanLine + 1];
			buffer[0] = (byte)filter_type[i];
			System.arraycopy(pixels, j, buffer, 1, bytesPerScanLine);
			builder.data(buffer);
			
			counter += bytesPerScanLine;
			
			if(counter > bufferLen) {
				Chunk chunk = builder.build();
				if(chunk.getData().length > 0) {
					chunk.write(os);
				}					
				counter = 0;
			}
		}
		
		// This should be called for the last chunk to make sure we get all the input data compressed
		builder.setFinish(true);		
		
		Chunk chunk = builder.build();
		
		if(chunk.getData().length > 0) 
			chunk.write(os);	
	}
	
	private void writeIndexed(int[] pixels, int imageWidth, int imageHeight, OutputStream os) throws Exception {
		ImageParam param = getImageParam();
		// The rule of thumb is never apply any filter to index color image
		int[] filter_type = new int[imageHeight];
		int bytesPerScanLine = imageWidth * 1;
		byte[] bytePixels = new byte[imageHeight * bytesPerScanLine];

		int[] colorPalette = new int[256];
		int[] colorInfo = IMGUtils.checkColorDepth(pixels, bytePixels, colorPalette);
		int bitsPerPixel = colorInfo[0];
		
		if(colorInfo[0]>0x08) {
			bitsPerPixel = 8;
			if(param.isApplyDither()) {
				if(param.getDitherMethod() == DitherMethod.FLOYD_STEINBERG)
					colorInfo = IMGUtils.reduceColorsDiffusionDither(param.getQuantMethod(), pixels, imageWidth, imageHeight, bitsPerPixel, bytePixels, colorPalette);
				else
					colorInfo = IMGUtils.reduceColorsOrderedDither(param.getQuantMethod(), pixels, imageWidth, imageHeight, bitsPerPixel, bytePixels, colorPalette, param.getDitherMatrix());				
			} else
	    		colorInfo = IMGUtils.reduceColors(param.getQuantMethod(), pixels, bitsPerPixel, bytePixels, colorPalette);
		}
		
		switch(bitsPerPixel) {
			case 3:
				bitsPerPixel = 4;
				break;
			case 5:
			case 6:
			case 7:
				bitsPerPixel = 8;
				break;
			default:
		}
		
		bytesPerScanLine = getBytesPerScanLine(bitsPerPixel, imageWidth, false);

		// Add IHDR chunk
		chunks.add(new IHDRBuilder().width(imageWidth).height(imageHeight).bitDepth(bitsPerPixel)
				.colorType(ColorType.INDEX_COLOR).compressionMethod(0).filterMethod(0).interlaceMethod(0).build());
		
		int numOfColors = (1<<bitsPerPixel);
		
		byte[] redMap = new byte[numOfColors];
		byte[] greenMap = new byte[numOfColors];
		byte[] blueMap = new byte[numOfColors];
		
		for(int i = 0; i < numOfColors; i++) {
			redMap[i] = (byte)(colorPalette[i]>>16);
			greenMap[i] = (byte)(colorPalette[i]>>8);
			blueMap[i] = (byte)(colorPalette[i]);
		}
		
		PLTEBuilder pBuilder = new PLTEBuilder();
		pBuilder.redMap(redMap).greenMap(greenMap).blueMap(blueMap);
		
		chunks.add(pBuilder.build());
		
		if (colorInfo[1] >= 0) { // There is a transparent color
			/* For color type 3,the tRNS chunk must not contain more
			 * alpha values than there are palette entries, but tRNS
			 * can contain fewer values than there are palette entries.
			 */
			TRNSBuilder tBuilder = new TRNSBuilder(3);			
			byte[] alpha = new byte[numOfColors];
			Arrays.fill(alpha, (byte)255);
			alpha[colorInfo[1]] = (byte)0;
			
			chunks.add(tBuilder.alpha(alpha).build());
		}
		
		PNGTweaker.serializeChunks(chunks, os);
		
		Arrays.fill(filter_type, filterType);
		
		if(bitsPerPixel == 8) {
			if(isApplyAdaptiveFilter) {
				apply_adaptive_filter(filter_type, bytePixels, imageHeight, 1, bytesPerScanLine);
			} else if(filterType != Filter.NONE) {
				apply_filter(filter_type, bytePixels, imageHeight, 1, bytesPerScanLine);
			}
		}		
		
		byte[] buffer = new byte[bytesPerScanLine + 1];
		
		IDATBuilder builder = new IDATBuilder(compressionLevel);
		
		// How many bytes to buffer before creating an IDAT chunk
		int bufferLen = imageWidth * imageHeight / 5; // We are expecting 5 IDAT chunks
		int counter = 0;
	
		// Pack bytePixels according to bitsPerPixel value
		if(bitsPerPixel != 8)
			bytePixels = ArrayUtils.packByteArray(bytePixels, imageWidth, 0, bitsPerPixel, imageWidth*imageHeight);
		
		for (int i = 0, j = 0; i < imageHeight; i++, j += bytesPerScanLine) {
			buffer = new byte[bytesPerScanLine + 1];
			buffer[0] = (byte)filter_type[i];
			System.arraycopy(bytePixels, j, buffer, 1, bytesPerScanLine);
			builder.data(buffer);
			
			counter += bytesPerScanLine;
			
			if(counter > bufferLen) {
				Chunk chunk = builder.build();
				if(chunk.getData().length > 0) {
					chunk.write(os);
				}
				counter = 0;
			}
		}
		
		// This should be called for the last chunk to make sure we get all the input data compressed
		builder.setFinish(true);
		
		Chunk chunk = builder.build();
		
		if(chunk.getData().length > 0) 
			chunk.write(os);
	}
	
	private void writeRGB(int[] pixels, int imageWidth, int imageHeight, OutputStream os) throws Exception {
		// The rule of thumb is always use PAETH filter which, in most cases, is as good as adaptive filter and much faster
		boolean noAlpha = !imageParam.hasAlpha();
		// Add IHDR chunk
		IHDRBuilder hdrBuilder = new IHDRBuilder().width(imageWidth).height(imageHeight).bitDepth(8).
				compressionMethod(0).filterMethod(0).interlaceMethod(0);
		
		if(noAlpha) hdrBuilder.colorType(ColorType.TRUE_COLOR);
		else hdrBuilder.colorType(ColorType.TRUE_COLOR_WITH_ALPHA);
				
		chunks.add(hdrBuilder.build());
		
		int[] filter_type = new int[imageHeight];
		int bytesPerPixel = (noAlpha)?3:4;
		int bytesPerScanLine = imageWidth*bytesPerPixel;
		int imageSize = imageWidth * imageHeight;
		byte[] bytePixels = new byte[imageHeight * bytesPerScanLine];

		if(filterType == Filter.NONE) filterType = Filter.PAETH;
		
		Arrays.fill(filter_type, filterType);
		
		if(noAlpha) {
			for (int i = 0, j = 0; i < imageSize; i++) {
				bytePixels[j++] =  (byte) ((pixels[i] >> 16) & 0xff);
				bytePixels[j++] =  (byte) ((pixels[i] >>  8) & 0xff);
				bytePixels[j++] =  (byte) ((pixels[i]) & 0xff);
			}
		} else {
			for (int i = 0, j = 0; i < imageSize; i++) {
				bytePixels[j++] =  (byte) ((pixels[i] >> 16) & 0xff);
				bytePixels[j++] =  (byte) ((pixels[i] >>  8) & 0xff);
				bytePixels[j++] =  (byte) ((pixels[i]) & 0xff);
				bytePixels[j++] =  (byte) ((pixels[i] >> 24) & 0xff);
			}
		}
		
		if(noAlpha && imageParam.isTransparent()) {
			// Add Transparent chunk
			TRNSBuilder tBuilder = new TRNSBuilder(2);
			int transparentColor = imageParam.getTransparentColor();		
			
			byte[] alpha = new byte[] {0, (byte)(transparentColor>>>16), 0, (byte)(transparentColor>>>8), 0, (byte)(transparentColor>>>0)};
			
			chunks.add(tBuilder.alpha(alpha).build());
		}
		
		PNGTweaker.serializeChunks(chunks, os);
		
		if(isApplyAdaptiveFilter) {
			// PNG specification suggests adaptive filter for RGB or grayscale image with 8 or more bit depth
			apply_adaptive_filter(filter_type, bytePixels, imageHeight, bytesPerPixel, bytesPerScanLine);
		} else {
			apply_filter(filter_type, bytePixels, imageHeight, bytesPerPixel, bytesPerScanLine);
		}
		
		byte[] buffer = new byte[bytesPerScanLine + 1];
		
		// Now build the data
		IDATBuilder builder = new IDATBuilder(compressionLevel);
		
		// How many bytes to buffer before creating an IDAT chunk
		int bufferLen = bytesPerPixel * imageWidth * imageHeight / 5; // We are expecting 5 IDAT chunks
		int counter = 0;
		
		for (int i = 0, j = 0; i < imageHeight; i++, j += bytesPerScanLine) {
			buffer = new byte[bytesPerScanLine + 1];
			buffer[0] = (byte)filter_type[i];
			System.arraycopy(bytePixels, j, buffer, 1, bytesPerScanLine);
			builder.data(buffer);
			
			counter += bytesPerScanLine;
			
			if(counter > bufferLen) {
				Chunk chunk = builder.build();
				if(chunk.getData().length > 0) {
					chunk.write(os);
				}
				counter = 0;
			}
		}
		
		// This should be called for the last chunk to make sure we get all the input data compressed
		builder.setFinish(true);
		
		Chunk chunk = builder.build();
		
		if(chunk.getData().length > 0) 
			chunk.write(os);
		// End of IDAT builder
	}
}