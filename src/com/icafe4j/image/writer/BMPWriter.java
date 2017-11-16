/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.writer;

import java.io.*; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.ImageColorType;
import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.quant.DitherMethod;
import com.icafe4j.image.util.IMGUtils;

/** 
 * BMP image writer
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 06/20/2007
 */
public class BMPWriter extends ImageWriter {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(BMPWriter.class);
	
	// Image header
	private static class BitmapHeader
	{
		// Bitmap file header, 14 bytes
		final byte signature[] = {'B','M'};// always "BM", decimal 19778, hex 0x4d42
		int   fileSize = 0x36; // Total size of file in bytes, to be set
		final short reserved1 = 0x00;
		final short reserved2 = 0x00;
 	    /**
		 * Offset from the beginning of the file
		 * to the raster data (54 bytes)
		 */
		int   dataOffSet = 0x36;// To be set		                       
		// Bitmap info header, 40 bytes
		final int   infoHeaderLen = 0x28; // 40 bytes
		int   imageWidth; // To be set
		int   imageHeight;// To be set
		final short planes = 0x01; // Always=1
        /**
		 * bits per pixel
		 * 1 - Colors Used = 2 (Palette)
         * 4 - Colors Used = 16 (Palette)
         * 8 - Colors Used = 256 (Palette)
         * 16 - Colors Used = 0 (RGB)
         * 24 - Colors Used = 0 (RGB)
         * 32 - Colors Used = 0 (RGB)
		 */
		short bitCount;// To be set
        /**
		 * Attribute of compression
         * 0 = BI_RGB: No compression
         * 1 = BI_RLE8: 8 bit RLE Compression (8 bit only)
         * 2 = BI_RLE4: 4 bit RLE Compression (4 bit only)
         * 3 = BI_BITFIELDS: No compression (16 & 32 bit only) 
		 */
		int   compression; // To be set
		/**
		 * Size of compressed image, to be set, can be 0 if Compression = 0
         * In the case that the 'imageSize' field has been set
		 * to zero, the size of the uncompressed data can be
		 * calculated using the following formula:
         * imageSize = int(((imageWidth * planes * bitCount) + 31) / 32) * 4 * imageHeight
         * where int(x) returns the integral part of x. 
		 */
		int   imageSize; // To be set
		int   xResolution = 0x00;
		int   yResolution = 0x00;
		/** 
		 * The number of color indexes in the color table used 
		 * by the bitmap. If set to zero, the bitmap uses maximum 
		 * number of colors specified by the bitCount.
		 */
		int   colorsUsed; // To be set    
		int   colorsImportant; // Number of important colors (0 = all), to be set

		void writeHeader(OutputStream os) throws Exception
		{
			byte bhdr[]=new byte[54];
			// Bitmap file header
			// signature
			bhdr[0] = signature[0];
			bhdr[1] = signature[1];
			// fileSize 
			bhdr[2] = (byte)(fileSize&0xff);
			bhdr[3] = (byte)((fileSize>>8)&0xff);
			bhdr[4] = (byte)((fileSize>>16)&0xff);
			bhdr[5] = (byte)((fileSize>>24)&0xff);
			// reserved1
			bhdr[6] = (byte)(reserved1&0xff);
			bhdr[7] = (byte)((reserved1>>8)&0xff);
			// reserved2
			bhdr[8] = (byte)(reserved2&0xff);
			bhdr[9] = (byte)((reserved2>>8)&0xff);
			// dataOffset
			bhdr[10] = (byte)(dataOffSet&0xff);
			bhdr[11] = (byte)((dataOffSet>>8)&0xff);
			bhdr[12] = (byte)((dataOffSet>>16)&0xff);
			bhdr[13] = (byte)((dataOffSet>>24)&0xff);
			// Bitmap info header
			// infoHeaderLen
			bhdr[14] = (byte)(infoHeaderLen&0xff);
			bhdr[15] = (byte)((infoHeaderLen>>8)&0xff);
			bhdr[16] = (byte)((infoHeaderLen>>16)&0xff);
			bhdr[17] = (byte)((infoHeaderLen>>24)&0xff);
			// imageWith
			bhdr[18] = (byte)(imageWidth&0xff);
			bhdr[19] = (byte)((imageWidth>>8)&0xff);
			bhdr[20] = (byte)((imageWidth>>16)&0xff);
			bhdr[21] = (byte)((imageWidth>>24)&0xff);
			// imageHeight
			bhdr[22] = (byte)(imageHeight&0xff);
			bhdr[23] = (byte)((imageHeight>>8)&0xff);
			bhdr[24] = (byte)((imageHeight>>16)&0xff);
			bhdr[25] = (byte)((imageHeight>>24)&0xff);
			// planes
			bhdr[26] = (byte)(planes&0xff);
			bhdr[27] = (byte)((planes>>8)&0xff);
			// bitCount
			bhdr[28] = (byte)(bitCount&0xff);
			bhdr[29] = (byte)((bitCount>>8)&0xff);
			// compression
			bhdr[30] = (byte)(compression&0xff);
			bhdr[31] = (byte)((compression>>8)&0xff);
			bhdr[32] = (byte)((compression>>16)&0xff);
			bhdr[33] = (byte)((compression>>24)&0xff);
			// imageSize
			bhdr[34] = (byte)(imageSize&0xff);
			bhdr[35] = (byte)((imageSize>>8)&0xff);
			bhdr[36] = (byte)((imageSize>>16)&0xff);
			bhdr[37] = (byte)((imageSize>>24)&0xff);
			// xResolution
			bhdr[38] = (byte)(xResolution&0xff);
			bhdr[39] = (byte)((xResolution>>8)&0xff);
			bhdr[40] = (byte)((xResolution>>16)&0xff);
			bhdr[41] = (byte)((xResolution>>24)&0xff);
			// yResolution
			bhdr[42] = (byte)(yResolution&0xff);
			bhdr[43] = (byte)((yResolution>>8)&0xff);
			bhdr[44] = (byte)((yResolution>>16)&0xff);
			bhdr[45] = (byte)((yResolution>>24)&0xff);
			// colorsUsed 
			bhdr[46] = (byte)(colorsUsed&0xff);
			bhdr[47] = (byte)((colorsUsed>>8)&0xff);
			bhdr[48] = (byte)((colorsUsed>>16)&0xff);
			bhdr[49] = (byte)((colorsUsed>>24)&0xff);
			// colorsImportant
			bhdr[50] = (byte)(colorsImportant&0xff);
			bhdr[51] = (byte)((colorsImportant>>8)&0xff);
			bhdr[52] = (byte)((colorsImportant>>16)&0xff);
			bhdr[53] = (byte)((colorsImportant>>24)&0xff);
			// End of BMP header, write to the output stream
			os.write(bhdr,0,54);
		}
	}

	// Define type of compression 
	/**
	 * 0 = BI_RGB: No compression
	 * 1 = BI_RLE8: 8 bit RLE Compression (8 bit only)
	 * 2 = BI_RLE4: 4 bit RLE Compression (4 bit only)
	 * 3 = BI_BITFIELDS: No compression (16 & 32 bit only)
	*/
	//private static int BI_RGB       = 0;
	//private static int BI_RLE8      = 1;
	//private static int BI_RLE4      = 2;
	//private static int BI_BITFIELDS = 3;
  
	private BitmapHeader bitmapHeader;
	
	public BMPWriter() {}
	
	public BMPWriter(ImageParam param) {
		super(param);
	}

	@Override
	public ImageType getImageType() {
		return ImageType.BMP;
	}

	protected void write (int[] pixels, int imageWidth, int imageHeight, 
			OutputStream os) throws Exception {   
		// The entry point for all the image writers		
		if(getImageParam().getColorType() == ImageColorType.INDEXED) write256ColorBitmap(pixels, imageWidth, imageHeight, os);
		else writeTrueColorBitmap(pixels, imageWidth, imageHeight, os);
	}

	private void write256ColorBitmap(int[] pixels, int imageWidth, 
	             int imageHeight, OutputStream os) throws Exception {
		ImageParam param = getImageParam();
		int nindex = 0;
		int index = 0;
		int npad = 0;
		// Calculate padding per scan line
		npad = 4 - (imageWidth%4);
	  
		if (npad == 4) 
			npad = 0;
      
		int bytePerScanLine = imageWidth + npad;

		byte brgb[]=new byte[256*4];
      
		LOGGER.info("Saving as 256 bits bitmap color image!");

		bitmapHeader = new BitmapHeader();
		// Set header parameters
		bitmapHeader.imageWidth = imageWidth;
		bitmapHeader.imageHeight = imageHeight;
		bitmapHeader.bitCount = 0x08;// 8 bits
		bitmapHeader.compression = 0x00; // No compression
		bitmapHeader.imageSize = bytePerScanLine*imageHeight;
		bitmapHeader.fileSize += bitmapHeader.imageSize;
		bitmapHeader.dataOffSet += 1024;
		bitmapHeader.colorsUsed = 0xff+1;     
		bitmapHeader.colorsImportant = 0xff+1;
		// Write bitmap image header
		bitmapHeader.writeHeader(os);
		// Reduce colors to 256
		byte[] newPixels = new byte[imageWidth*imageHeight];
		int[] colorPalette = new int[256];
		
		if(param.isApplyDither()) {
    		if(param.getDitherMethod() == DitherMethod.FLOYD_STEINBERG)
        		IMGUtils.reduceColorsDiffusionDither(param.getQuantMethod(), pixels, imageWidth, imageHeight, 8, newPixels, colorPalette);	        		
    		else
        		IMGUtils.reduceColorsOrderedDither(param.getQuantMethod(), pixels, imageWidth, imageHeight, 8, newPixels, colorPalette, param.getDitherMatrix());
    	} else
    		IMGUtils.reduceColors(param.getQuantMethod(), pixels, 8, newPixels, colorPalette);
		
		// Write out the color palette
		for (int i=0; i<256; i++)
		{
			brgb[nindex++] = (byte)(colorPalette[i]&0xff);
			brgb[nindex++] = (byte)(((colorPalette[i]>>8)&0xff));
			brgb[nindex++] = (byte)(((colorPalette[i]>>16)&0xff));
			brgb[nindex++] = (byte)0xff;
		}
	  
		os.write(brgb,0,1024);
		// Write out the color index of the raster data
		brgb = new byte[bytePerScanLine];

		for(int i=1; i<=imageHeight; i++)
		{		
			nindex = 0;
			index = imageWidth*(imageHeight-i);
		  
			for(int j=0; j<imageWidth; j++)
			{
				brgb[nindex++] = newPixels[index++];
			}
		  
			os.write(brgb,0,bytePerScanLine);		  
		}
		
		os.close();
	}

	private void writeTrueColorBitmap(int[] pixels, int imageWidth, 
	             int imageHeight, OutputStream os) throws Exception {
		int nindex = 0;
		int index = 0;
		int npad = 0;
		// Calculate padding per scan line
		npad = 4 - ((imageWidth*3)%4);
	  
		if (npad == 4)
			npad = 0;
      
		int bytePerScanLine = imageWidth*3 + npad;

		byte brgb[]=new byte[bytePerScanLine];
      
		LOGGER.info("Saving as 24 bits bitmap color image!");

		bitmapHeader = new BitmapHeader();
		// Set header parameters
		bitmapHeader.imageWidth = imageWidth;
		bitmapHeader.imageHeight = imageHeight;
		bitmapHeader.bitCount = 0x18;// 24 bits
		bitmapHeader.compression = 0x00; // No compression
		bitmapHeader.imageSize = bytePerScanLine*imageHeight;
		bitmapHeader.fileSize += bitmapHeader.imageSize;
		bitmapHeader.colorsUsed = 0x00;     
		bitmapHeader.colorsImportant = 0x00;
		// Write bitmap image header
		bitmapHeader.writeHeader(os);
		// Write raster data
		for(int i=1; i<=imageHeight; i++)
		{
			nindex = 0;
			index = imageWidth*(imageHeight-i);
		  
			for(int j=0; j<imageWidth; j++)
			{
				brgb[nindex++] = (byte)(pixels[index]&0xff);
				brgb[nindex++] = (byte)((pixels[index]>>8)&0xff);
				brgb[nindex++] = (byte)((pixels[index++]>>16)&0xff);
			}
			
			os.write(brgb,0,bytePerScanLine);		  
		}
		os.close();
	}
} 
