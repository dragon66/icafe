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

package com.icafe4j.image.reader;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.bmp.BmpCompression;
import com.icafe4j.image.options.BMPOptions;
import com.icafe4j.io.IOUtils;
import com.icafe4j.util.ArrayUtils;

/** 
 * Decodes and shows true color, 2 color, 16 color or 256 color windows 3.x 
 * BMP images. This class currently support true color, 2 color uncompressed,
 * 256 Color and 16 color uncompressed as well as RLE compressed format. 
 * <p>
 * Changes: 
 * 1. colorPalette to use bitmapHeader.colorsUsed if it's nonzero 
 *    instead of always relying on 1<<bitsPerPixel.
 * 2. check the bitmapHeader.dataOffSet for possible bytes to skip
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.1 03/28/2008
 */
public class BMPReader extends ImageReader {
	private static final int END_OF_LINE = 0;
	private static final int END_OF_BITMAP = 1;
	private static final int DELTA = 2;
	
	private int bytePerScanLine;
	private int alignment = BMPOptions.ALIGN_BOTTOM_UP;
	private int compression = 0;
	
	BitmapHeader bitmapHeader;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(BMPReader.class);
   
    public BufferedImage read(InputStream is) throws Exception {
        bitmapHeader = new BitmapHeader();
		bitmapHeader.readHeader(is);
		width = bitmapHeader.imageWidth;
		height = bitmapHeader.imageHeight;
		compression = bitmapHeader.compression;
		
		if(height < 0) {
			alignment = BMPOptions.ALIGN_TOP_DOWN;
			height = -height;
		}
		
		LOGGER.info("Scanline alignment: {}", ((alignment == BMPOptions.ALIGN_BOTTOM_UP)?"BOTTOM_UP":"TOP_DOWN"));
		
		bitsPerPixel = bitmapHeader.bitCount;

        int bitPerWidth = width*bitsPerPixel;

	   	if(bitPerWidth%32 == 0) { // To make sure scan lines are padded out to even 4-byte boundaries.
 	   		bytePerScanLine = (bitPerWidth>>>3);
		} else {
			bytePerScanLine = (bitPerWidth>>>3)+(4-(bitPerWidth>>>3)%4);
			// A different method to do the same thing as above!
			//bytePerScanLine = (((bitPerWidth+31) & ~31 ) >> 3);
		}
		switch (bitmapHeader.bitCount) {
			case 1:
				return readIndexColorBitmap(is);
			case 4:
			case 8:
				if(compression == BmpCompression.BI_RLE4.getValue() || compression == BmpCompression.BI_RLE8.getValue())
					return readCompressedIndexColorBitmap(is);
		        return readIndexColorBitmap(is);				
			case 16:
				LOGGER.error("16 bit BMP, decoding not implemented!");
		   		//read16bitTrueColorBitmap(is);
                return null;
			case 24:
				return read24bitTrueColorBitmap(is);
   			case 32:
				return read32bitTrueColorBitmap(is);
			default:
				LOGGER.error("Unsupported bitmap format!");
				return null;
		}
    }
    
	private void readPalette(InputStream is) throws Exception {
		int index = 0, nindex = 0;
		int numOfColors = (bitmapHeader.colorsUsed == 0)?(1<<bitsPerPixel):bitmapHeader.colorsUsed;
		byte brgb[] = new byte[numOfColors*4];
		rgbColorPalette = new int[numOfColors];	
     
		IOUtils.readFully(is, brgb, 0, numOfColors*4);

        for(int i = 0; i < numOfColors; i++) {
			rgbColorPalette[index++] = ((0xff<<24)|(brgb[nindex]&0xff)|((brgb[nindex+1]&0xff)<<8)|((brgb[nindex+2]&0xff)<<16));
			nindex += 4;
		}
		// There may be some extra bytes between colorPalette and actual image data
		IOUtils.skipFully(is, bitmapHeader.dataOffSet - numOfColors*4 - bitmapHeader.infoHeaderLen - 14);
    }

    private BufferedImage read24bitTrueColorBitmap(InputStream is) throws Exception {
    	LOGGER.info("24 bits bitmap color image!");
        int npad = bytePerScanLine - 3*width;
		if(npad == 4) npad = 0;
	
        IOUtils.skipFully(is, bitmapHeader.dataOffSet - 54);
		
        int bytePerWidth = bytePerScanLine - npad;
		byte[] buffer = new byte[bytePerScanLine];
		
		byte[] pixels = new byte[bytePerWidth * height];  
		        
		LOGGER.info("Scanline padding: {}", npad);		
		
		if(alignment == BMPOptions.ALIGN_BOTTOM_UP) {
			for(int i = 0, startIndex =  (height-1)*bytePerWidth; i < height; i++, startIndex -= bytePerWidth) {
				IOUtils.readFully(is, buffer);
				System.arraycopy(buffer, 0, pixels, startIndex, bytePerWidth);
			}
		} else {
			for(int i = 0, startIndex =  0; i < height; i++, startIndex += bytePerWidth) {
				IOUtils.readFully(is, buffer);
				System.arraycopy(buffer, 0, pixels, startIndex, bytePerWidth);
			}
		}
		
		is.close();
		
		//Create a BufferedImage
		DataBuffer db = new DataBufferByte(pixels, pixels.length);
		
		int[] off = {2, 1, 0};//band offset, we have 3 bands
		int numOfBands = 3;
		int trans = Transparency.OPAQUE;
			
		WritableRaster raster = Raster.createInterleavedRaster(db, width, height, bytePerWidth, numOfBands, off, null);
		ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, trans, DataBuffer.TYPE_BYTE);
   	
		return new BufferedImage(cm, raster, false, null);
    }
  
    // This actually deals with the case of RGB888 mask case for the 32 bits image but it seems to work for other mask too
    private BufferedImage read32bitTrueColorBitmap(InputStream is) throws Exception { 
    	LOGGER.info("32 bits bitmap color image!");
 		
 		byte brgb[] = new byte[bytePerScanLine];
 		int pix[] = new int[width*height];
        IOUtils.skipFully(is, bitmapHeader.dataOffSet - 54);
        
        if(alignment == BMPOptions.ALIGN_BOTTOM_UP) {
        	for(int i = 1, index = 0; i <= height; i++)	{
	 			IOUtils.readFully(is, brgb, 0, bytePerScanLine);
	 			index = width*(height-i);
	 			for(int j = 0, nindex = 0; j < width; j++) {
	 				pix[index++] = ((brgb[nindex++]&0xff)|((brgb[nindex++]&0xff)<<8)|((brgb[nindex++]&0xff)<<16)|(0xff<<24));
	 				nindex++;
	 			}
        	}
		} else {
			for(int i = 0, index = 0; i < height; i++) {
	 			IOUtils.readFully(is, brgb, 0, bytePerScanLine);
	 			for(int j = 0, nindex = 0; j < width; j++) {
	 				pix[index++] = ((brgb[nindex++]&0xff)|((brgb[nindex++]&0xff)<<8)|((brgb[nindex++]&0xff)<<16)|(0xff<<24));
	 				nindex++;
	 			}
        	}
		} 		
 		
 		is.close();
 	
 		//Create a BufferedImage
 		DataBuffer db = new DataBufferInt(pix, pix.length);
 		WritableRaster raster = Raster.createPackedRaster(db, width, height, width,  new int[] {0x00ff0000, 0x0000ff00, 0x000000ff}, null);
 		ColorModel cm = new DirectColorModel(24, 0x00FF0000, 0x0000ff00, 0x000000ff);
 			
 		return new BufferedImage(cm, raster, false, null);
    }
    
    @SuppressWarnings("unused")
	private BufferedImage read32bitTrueColorBitmap2(InputStream is) throws Exception {
    	LOGGER.info("32 bits bitmap color image!");
       
        IOUtils.skipFully(is, bitmapHeader.dataOffSet - 54);
		
        int bytePerWidth = bytePerScanLine;
		byte[] buffer = new byte[bytePerScanLine];
		
		byte[] pixels = new byte[bytePerWidth * height];
		Arrays.fill(pixels, (byte)0xff);
		        
		if(alignment == BMPOptions.ALIGN_BOTTOM_UP) {
			for(int i = 1, index = 0; i <= height; i++) {
				IOUtils.readFully(is, buffer);
				index = bytePerWidth*(height - i);
				for(int j = 0; j < bytePerWidth; j++, index++) {
					pixels[index++] = buffer[j++];
					pixels[index++] = buffer[j++];
					pixels[index++] = buffer[j++];
				}
			}
		} else {
			for(int i = 0, index = 0; i < height; i++) {
				IOUtils.readFully(is, buffer);
				for(int j = 0; j < bytePerWidth; j++, index++) {
					pixels[index++] = buffer[j++];
					pixels[index++] = buffer[j++];
					pixels[index++] = buffer[j++];
				}
			}
		}
		
		is.close();
		
		//Create a BufferedImage
		DataBuffer db = new DataBufferByte(pixels, pixels.length);
		
		int[] off = {2, 1, 0, 3}; //band offset, we have 4 bands
		int numOfBands = 4;
		int trans = Transparency.OPAQUE;
		int[] nBits = {8, 8, 8, 8};
			
		WritableRaster raster = Raster.createInterleavedRaster(db, width, height, bytePerScanLine, numOfBands, off, null);
		ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, true, false,
                trans, DataBuffer.TYPE_BYTE);
   	
		return new BufferedImage(cm, raster, false, null);
    }
      
    private BufferedImage readCompressedIndexColorBitmap(InputStream is) throws Exception {
    	byte pixels[] = null;
      	
    	if(bitsPerPixel == 8)
      		pixels = read256ColorCompressedBitmap(is);
      	else if(bitsPerPixel == 4)	
			pixels = read16ColorCompressedBitmap(is);
      	else
      		throw new IllegalArgumentException("Invalid bitsPerPixel: " + bitsPerPixel);
    		// Create BufferedImage
		DataBuffer db = new DataBufferByte(pixels, pixels.length);
		WritableRaster raster = null;
		if(bitsPerPixel != 8) {
			raster = Raster.createPackedRaster(db, width, height, bitsPerPixel, null);
		} else {
			int[] off = {0};//band offset, we have only one band start at 0
			raster = Raster.createInterleavedRaster(db, width, height, width, 1, off, null);
		}
		ColorModel cm = new IndexColorModel(bitsPerPixel, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
		
		return new BufferedImage(cm, raster, false, null);		
    }
    
    private BufferedImage readIndexColorBitmap(InputStream is) throws Exception {
    	LOGGER.info("{} color bitmap color image!", (1<<bitsPerPixel));
  		readPalette(is);
  		int npad = 0;
  		
  		switch(bitsPerPixel) {
  		case 1:
  			npad = (32-(width%32))/8;
  			break;
  		case 4:
  			npad = (32-((width*4)%32))/8;
  			break;
  		case 8:
  			npad = bytePerScanLine - width;
  			break;
  		default:
  			throw new IllegalArgumentException("Invalid bitsPerPixel: " + bitsPerPixel + " for BMP indexColor image!");  				
  		}
  		
  		if(npad == 4) npad = 0;
  		
  		int bytePerWidth = bytePerScanLine - npad;
		byte[] buffer = new byte[bytePerScanLine];
		
		byte[] pixels = new byte[bytePerWidth * height];  
		        
		LOGGER.info("Scanline padding: {}", npad);		
		
		if(alignment == BMPOptions.ALIGN_BOTTOM_UP) {
			for(int i = 0, startIndex =  (height-1)*bytePerWidth; i < height; i++, startIndex -= bytePerWidth) {
				IOUtils.readFully(is, buffer);
				System.arraycopy(buffer, 0, pixels, startIndex, bytePerWidth);
			}
		} else {
			for(int i = 0, startIndex =  0; i < height; i++, startIndex += bytePerWidth) {
				IOUtils.readFully(is, buffer);
				System.arraycopy(buffer, 0, pixels, startIndex, bytePerWidth);
			}
		}
		
		is.close();
		
		// Create BufferedImage
		DataBuffer db = new DataBufferByte(pixels, pixels.length);
		WritableRaster raster = null;
		if(bitsPerPixel != 8) {
			raster = Raster.createPackedRaster(db, width, height, bitsPerPixel, null);
		} else {
			int[] off = {0};//band offset, we have only one band start at 0
			raster = Raster.createInterleavedRaster(db, width, height, width, 1, off, null);
		}
		ColorModel cm = new IndexColorModel(bitsPerPixel, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
		
		return new BufferedImage(cm, raster, false, null);		
    }

    private byte[] read256ColorCompressedBitmap(InputStream is) throws Exception {
    	LOGGER.info("256 color bitmap color image!");
 		LOGGER.info("compressed format!");
    	
 		readPalette(is);

 		int index = 0, nindex = 0;
 		int len = 0, esc = 0, count = 0;
 		int vert_offset = 0, horz_offset = 0, horz = 0, vert = height-1;
 		int bufferSize = 2048;//2k

 		boolean done_with_bitmap = false;
 		
 		byte brgb[] = new byte[bufferSize];
 		int readSize = is.read(brgb, 0, bufferSize);
 		
 		byte pixels[] = new byte[width*height];
           
 		index = width*vert+horz;

 		do {
 			if (nindex >= readSize)	{
 				//if ((is.read(brgb,0,bufferSize)) == -1) break;
 				readSize = is.read(brgb, 0, bufferSize);
 				nindex = 0;
 			}
 				   	
 			len = brgb[nindex++]&0xff;
 					
 			if (nindex >= readSize) {
 				readSize = is.read(brgb, 0, bufferSize);
 				nindex = 0;
 			}
 			
 			if(len == 0) {
 				esc = (brgb[nindex++]&0xff);
 				if (nindex >= readSize) {
 					readSize = is.read(brgb, 0, bufferSize);
 					nindex = 0;
 				}
 				if(esc > 2) {
 					count = 0;
 					for(int k = 1; k <= esc; k++) {
 						pixels[index++] = brgb[nindex++];
 						if (nindex >= readSize) {
 							readSize = is.read(brgb, 0, bufferSize);
 							nindex = 0;
 						}
 						count++;
 						horz++;
 						if (horz >= width) {
 							break;
 						}
 					}
 					if((count%2) != 0) nindex++;// Each absolute run must be aligned on a word boundary!
 				}
 				if (esc == DELTA) {
 					LOGGER.info("found delta");
 					horz_offset = brgb[nindex++]&0xff;
 					if (nindex >= readSize) {
 						readSize = is.read(brgb, 0, bufferSize);
 						nindex = 0;
 					}
 					vert_offset = brgb[nindex++]&0xff;
 					if (nindex >= readSize) {
 						readSize = is.read(brgb, 0, bufferSize);
 						nindex = 0;
 					}
 					horz += horz_offset;// This is to be verified!
 					vert -= vert_offset;// This is to be verified!
 					index = width*vert+horz;
 				}
 				if(esc == END_OF_LINE) {
 					vert--;
 					horz = 0;
 					index = width*vert+horz;
 				}
 				if(esc == END_OF_BITMAP) done_with_bitmap = true;
 			} else {
 				byte b = brgb[nindex++];
 				
 				if (nindex >= readSize) {
 					readSize = is.read(brgb, 0, bufferSize);
 					nindex = 0;
 				} 						
 				for(int l = 0; l < len; l++) {
 					pixels[index++] = b;
 					horz++;
 					if (horz >= width) {
 						break;
 					}
 				}
 			}
 			if(vert < 0) done_with_bitmap = true;
 		} while(!done_with_bitmap);
 		
 		is.close();
		
		return pixels;
	}
    
    private byte[] read16ColorCompressedBitmap(InputStream is) throws Exception {
    	LOGGER.info("16 color bitmap color image!");
    	LOGGER.info("compressed format!");
    	
    	readPalette(is);

    	int nindex = 0, index = 0;
       	int horz = 0, vert = height-1, horz_offset  =  0, vert_offset  =  0;
    	int count = 0, counter = 0, len = 0, esc = 0;	
    	int bufferSize = 2048;//2k

    	boolean done_with_bitmap = false;
       
    	byte brgb[] = new byte[bufferSize];
		int readSize = is.read(brgb,0,bufferSize);
		
		byte[] pixels = new byte[width*height];
		  
		index = width*vert+horz;
		  
		do {
			if (nindex >= readSize) {
				readSize = is.read(brgb, 0, bufferSize);
				nindex = 0;
			}

			len = brgb[nindex++]&0xff;
                   
			if (nindex >= readSize) {
				readSize = is.read(brgb, 0, bufferSize);
				nindex = 0;
			}
			if(len == 0) {
				esc = brgb[nindex++]&0xff;
				if (nindex >= readSize) {
					readSize = is.read(brgb, 0, bufferSize);
					nindex = 0;
				}

				if(esc == END_OF_BITMAP) done_with_bitmap = true;
				
				if (esc == DELTA) {
					LOGGER.info("found delta");
					horz_offset = brgb[nindex++]&0xff;
					if (nindex >= readSize) {
						readSize = is.read(brgb, 0, bufferSize);
						nindex = 0;
					}
					vert_offset = brgb[nindex++]&0xff;
					if (nindex >= readSize) {
						readSize = is.read(brgb, 0, bufferSize);
						nindex = 0;
					}
					horz += horz_offset;
					vert -= vert_offset;
					index = width*vert+horz;
				}

				if(esc == END_OF_LINE) {
					vert--;
					horz = 0;
					index = width*vert+horz;
				}
										        
				if(esc>2) {
					count = 0;
					do {
						int b = brgb[nindex++]&0xff;
						if (nindex >= readSize) {
							readSize = is.read(brgb, 0, bufferSize);
							nindex = 0;
						}
						count++;
						
						pixels[index++] = (byte)((b>>>4)&0x0F);
						counter++;
						horz++;
						// Be careful in this case if it runs out of the line bound, 
						// don't update horz and vert variables at this time,just break 
						// and wait until the end_of_line flag to show up.
						if (horz >= width) {
							break;   
						}        
						
						if (counter < esc) {
							pixels[index++] = (byte)(b&0x0F);
							counter++;
							horz++;
						}

						if (horz >= width) {
							break;
						}
						
					} while(counter < esc);
					counter = 0;
					if((count%2) != 0) nindex += 1;
				}
			} else {
				int b = brgb[nindex++]&0xff;
				if (nindex >= readSize) {
					readSize = is.read(brgb, 0, bufferSize);
					nindex = 0;
				}
				do {
					pixels[index++] = (byte)((b>>>4)&0x0F);
					counter++;
					horz++;
					
					if (horz >= width) {
						break;
					}

					if (counter < len) {
						pixels[index++] = (byte)(b&0x0F);
						counter++;
						horz++;
						
						if (horz >= width) {
							break;
						}
					}
				} while(counter < len);
				counter = 0;
			}
			if(vert < 0) done_with_bitmap = true;
		} while(!done_with_bitmap);
		
		is.close();
		
		return ArrayUtils.packByteArray(pixels, width, 0, bitsPerPixel, pixels.length);
	}
 	 
	 @SuppressWarnings("unused")
     private static class BitmapHeader {
		// Bitmap file header, 14 bytes
		short signiture;// Always "BM"
		int   fileSize; // Total size of file in bytes
		short reserved1;
		short reserved2;
		int   dataOffSet;
		// Bitmap info header, 40 bytes
		int   infoHeaderLen; // 40 bytes
		int   imageWidth;
		int   imageHeight;
		short planes; // Always = 1
		/**
		 * 1 - Colors Used = 2 (Palette)
         * 4 - Colors Used = 16 (Palette)
         * 8 - Colors Used = 256 (Palette)
         * 16 - Colors Used = 0 (RGB)
         * 24 - Colors Used = 0 (RGB)
         * 32 - Colors Used = 0 (RGB)
		 */
		short bitCount;
		/**
		 * Attribute of compression:
         * 0 = BI_RGB: No compression
         * 1 = BI_RLE8: 8 bit RLE Compression (8 bit only)
         * 2 = BI_RLE4: 4 bit RLE Compression (4 bit only)
         * 3 = BI_BITFIELDS: No compression (16 & 32 bit only)
		 */
		int compression; 
		/** 
		 * Size of compressed image, can be 0 if Compression = 0
         * In the case that the 'imageSize' field has been set
		 * to zero, the size of the uncompressed data can be
		 * calculated using the following formula:
         * imageSize = int(((imageWidth * planes * bitCount) + 31) / 32) * 4 * imageHeight
         * where int(x) returns the integral part of x.
		 */
		int imageSize;
		
		int xResolution;
		int yResolution;
		/**
		 * The number of color indexes in the color table used 
		 * by the bitmap. If set to zero, the bitmap uses maximum 
		 * number of colors specified by the bitCount.
		 */
		int colorsUsed;		
		int colorsImportant; // Number of important colors (0 = all) 
    
	    void readHeader(InputStream is) throws Exception {
	    	int nindex = 0;
	    	byte bhdr[] = new byte[18];
				  
	    	IOUtils.readFully(is, bhdr, 0, 18);
	    	
	    	signiture = (short)((bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8));
	    	fileSize = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	reserved1 = (short)((bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8));
	    	reserved2 = (short)((bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8));
	    	dataOffSet = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	infoHeaderLen = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	bhdr = new byte[infoHeaderLen - 4]; // infoHeaderLen is different for different bitmap versions
	    	IOUtils.readFully(is, bhdr, 0, infoHeaderLen - 4);
	    	nindex = 0;
	    	imageWidth = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	imageHeight = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	planes = (short)((bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8));
	    	bitCount = (short)((bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8));
	    	compression = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	imageSize = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	xResolution = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	yResolution = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	colorsUsed = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    	colorsImportant = (bhdr[nindex++]&0xff)|((bhdr[nindex++]&0xff)<<8)|((bhdr[nindex++]&0xff)<<16)|((bhdr[nindex++]&0xff)<<24);
	    }
     }
}