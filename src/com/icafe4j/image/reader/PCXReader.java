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
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.util.BytePacker;
import com.icafe4j.io.IOUtils;
import com.icafe4j.util.ArrayUtils;

/** 
 * Decodes and shows true color, 256, 16, 8, 4 as well as 
 * black and white RLE compressed PCX images.
 *
 * Support is added for PCX images with 2 and 4 bits 
 * per pixel, one color plane
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/03/2007
 */
public class PCXReader extends ImageReader {
    short bytesPerLine = 0;
	byte NPlanes = 0;
	PcxHeader pcxHeader;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(PCXReader.class);

   	public BufferedImage read(InputStream is) throws Exception {
		pcxHeader = new PcxHeader();
      	pcxHeader.readHeader(is);
		width = pcxHeader.xmax-pcxHeader.xmin+1;
	    height = pcxHeader.ymax-pcxHeader.ymin+1;
		bytesPerLine = pcxHeader.bytes_per_line;
		rgbColorPalette = pcxHeader.colorPalette;
		NPlanes = pcxHeader.color_plane;

		bitsPerPixel = pcxHeader.bits_per_pixel*pcxHeader.color_plane;

		if((pcxHeader.bits_per_pixel == 8) && (pcxHeader.color_plane == 1)) {
		   return read256ColorPcx(is);
		} else if ((pcxHeader.bits_per_pixel == 8) && (pcxHeader.color_plane == 3)) {
    	    return readTrueColorPcx(is);
		} else {
			if (NPlanes == 1) { // One plane 1, 2, 4 bits
				switch (bitsPerPixel) {
				  case 4: 
					  LOGGER.info("16 color pcx image, 4 bits per pixel, 1 color plane!");
				      break;
				  case 3:
                      throw new UnsupportedOperationException("Invalid bitsPerPixel: " + bitsPerPixel + " for one plane PCX image");
				  case 2:
                      LOGGER.info("4 color pcx image, 2 bits per pixel, 1 color plane!");
				      break;
				  case 1:
					  LOGGER.info("2 color pcx image, 1 bits per pixel, 1 color plane!");
				      break;
                  default: 
				}
				return readOnePlaneEgaPcx(is);
			} else if(pcxHeader.bits_per_pixel == 1) {// One bit N planes
				switch (NPlanes) {
				  case 4: 
					  LOGGER.info("16 color image, 1 bit per plane, 4 planes!");
				      break;
				  case 3:
                      LOGGER.info("8 color image, 1 bit per plane, 3 planes!");
				      break;
				  case 2:
                      LOGGER.info("4 color image, 1 bit per plane, 2 planes!");
				      break;
				  default: 
				}
				return readOneBitEgaPcx(is);
			} else { // Hope this will never happen
                LOGGER.error("unimplemented for this format!");
			}
		}
		
		return null;
    }	
       
    private void readPalette(byte[] buf) throws Exception {
		int index = 0, nindex = 0;
		
		for(int i = 0; i < buf.length; i += 3) {
			rgbColorPalette[index++] = ((0xff<<24)|((buf[nindex++]&0xff)<<16)|((buf[nindex++]&0xff)<<8)|(buf[nindex++]&0xff));
		}
    }
    
    @SuppressWarnings("unused")
	private void readPalette(InputStream is, int color_tb_bytes) throws Exception {
		int index = 0, nindex = 0;
		byte buf[] = new byte[color_tb_bytes];
		is.read(buf,0,color_tb_bytes);

		for(int i = 0; i < color_tb_bytes; i += 3) {
			rgbColorPalette[index++] = ((0xff<<24)|((buf[nindex++]&0xff)<<16)|((buf[nindex++]&0xff)<<8)|(buf[nindex++]&0xff));
		}
    }
   
    private BufferedImage readTrueColorPcx(InputStream is) throws Exception {
      	byte brgb[] = IOUtils.readFully(is, 4096);
    	byte pixels[] = new byte[bytesPerLine*NPlanes*height];

    	/**
    	 * A BufferedInputStream could have been constructed from the InputStream,
    	 * but for maximum decoding speed, one time reading of the image data 
    	 * into memory is ideal though this is memory consuming.
    	 */
       	LOGGER.info("true color pcx image!");
		
    	readScanLines(brgb, brgb.length, pixels);
    	is.close();
    	
	    DataBuffer db = new DataBufferByte(pixels, pixels.length);
	    int trans = Transparency.OPAQUE;
	    int[] nBits = {8, 8, 8};						
	    WritableRaster raster = Raster.createBandedRaster(db, width, height, bytesPerLine*3,
                new int[]{0, 0, 0}, new int[] {0, bytesPerLine, bytesPerLine*2}, null);
	    ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, false, false,
                trans, DataBuffer.TYPE_BYTE);
	    
	    return new BufferedImage(cm, raster, false, null);
    }
    
    private void readScanLines(byte[] brgb, int buf_len, byte[] pixels) throws Exception {
    	int counter = 0, bt = 0, bt1 = 0, index = 0, nindex = 0, num_of_rep = 0;
    	int totalBytes = NPlanes * bytesPerLine;
    	
    	image:// Label to break
    	for(int i = 0; i < height; i++, counter = 0) {    	
			do {
				bt = (brgb[nindex++]&0xff);
				if((bt&0xC0) == 0xC0) {
					num_of_rep = bt&0x3F;
					bt1 = (brgb[nindex++]&0xff);
					for(int k = 0; k < num_of_rep && counter < totalBytes; k++, counter++) {
						pixels[index++] = (byte)bt1; 
					}
					if (nindex >= buf_len) {
						break image;
					}
				} else {
					pixels[index++] = (byte)bt;
					counter++;
					if (nindex >= buf_len) {
						break image;
					}
				}
			} while(counter < totalBytes);		
    	}
    }
   	
	private BufferedImage read256ColorPcx(InputStream is) throws Exception {
		int totalBytes = bytesPerLine*NPlanes;
        byte pixels[] = new byte[totalBytes*height];		
	
        byte[] data = IOUtils.readFully(is, 4096);
	
		int colorsUsed = (1<<NPlanes*pcxHeader.bits_per_pixel);
		int color_tb_bytes = 3*colorsUsed;
		
		rgbColorPalette = new int[colorsUsed];

		int buf_len = data.length - color_tb_bytes;
		byte brgb[] = ArrayUtils.subArray(data, 0, buf_len);
      
        readPalette(ArrayUtils.subArray(data, buf_len, color_tb_bytes));

	    /** 
	     * If a BufferedInputStream is used for a 256-color image, we have to use 
		 * mark(int), skip(long) and reset() methods to put the stream pointer to
		 * the beginning of the color palette, fill the color palette and reset the pointer to
		 * the beginning of the image data. This is awkward and equally memory consuming since
		 * the system must maintain a buffer for the reset() method and we have to keep track
		 * of the skip(long) method to ensure reading of the required bytes as shown below:
		 * 	
		 * <p> 
		 * is.mark(available);
         * long i = 0, count = 0;
		 *
         * while(count < buf_len)
		 *  {
		 *     i = is.skip(buf_len-count);
         *     count += i;
		 *  }
		 *
		 * readPalette(is, color_tb_bytes);
		 * is.reset();
         *
	 	 * @see java.io.BufferedInputStream
		 */

		LOGGER.info("256 color pcx image!");

		readScanLines(brgb, buf_len, pixels);   	
    	is.close();    	
		//Create a BufferedImage
		int[] off = {0};//band offset, we have only one band start at 0
		DataBuffer db = new DataBufferByte(pixels, pixels.length);
		WritableRaster raster = Raster.createInterleavedRaster(db, width, height, bytesPerLine, 1, off, null);
		ColorModel cm = new IndexColorModel(8, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
		
		return new BufferedImage(cm, raster, false, null);
	}

	private BufferedImage readOneBitEgaPcx(InputStream is) throws Exception {
	    int index = 0, counter = 0, abyte = 0, nindex = 0;
	    int bt = 0, bt1 = 0; 
	    int totalBytes = 0, num_of_rep = 0;
        int  buf[];
	  
  		byte brgb[] = IOUtils.readFully(is, 4096);

	    int buf_len = brgb.length;

		totalBytes = bytesPerLine*NPlanes;
        buf = new int[totalBytes];
        
        byte[] pixels;
        
        BytePacker bytePacker = new BytePacker(bitsPerPixel, width, width*height);

   		image:
		for(int i = 0; i < height; i++, index = 0) {	
			do {
				bt = brgb[nindex++]&0xff;
				if((bt&0xC0) == 0xC0) {
					num_of_rep = bt&0x3F;
					bt1 = brgb[nindex++]&0xff;
				  
					for(int k = 0; k < num_of_rep && index < totalBytes; k++) {
						buf[index++] = bt1;
					}
					if (nindex >= buf_len) {
						break image;
					}
				} else {
					buf[index++] = bt;

					if (nindex >= buf_len) {
						break image;
					}
				}
			} while(index < totalBytes);
			
			scanLine:
			for(int k = 0; k < bytesPerLine; k++) {							
				for(int l = 7; l >= 0; l--) {
					for(int m = 0; m < NPlanes; m++) {
						abyte |= (((buf[k + bytesPerLine*m]>>l)&0x01)<<m);						
					}
					bytePacker.packByte(abyte);
					abyte = 0; // Must reset here
					if(++counter%width == 0)
						break scanLine;
				}
			}
		}
		
		is.close();
		
		pixels = bytePacker.getPackedBytes();
		DataBuffer db = new DataBufferByte(pixels, pixels.length);
		WritableRaster  raster = Raster.createPackedRaster(db, width, height, bitsPerPixel, null);
	    ColorModel cm = new IndexColorModel(bitsPerPixel, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
	   
	    return new BufferedImage(cm, raster, false, null);	
	}

	// Need to find test images for this case
	private BufferedImage readOnePlaneEgaPcx(InputStream is) throws Exception {
		// Try to decode 2, 4 and 16 color images as implemented 
		// by using 1, 2 and 4 bits per pixel and one color plane
		byte brgb[] = IOUtils.readFully(is, 4096);
	    byte[] pixels = new byte[bytesPerLine*height];
	    readScanLines(brgb, brgb.length, pixels);
		is.close();
		
		DataBuffer db = new DataBufferByte(pixels, pixels.length);
		WritableRaster  raster = Raster.createPackedRaster(db, width, height, bitsPerPixel, null);
		if(bitsPerPixel == 1) {
			int BW_palette[] = new int[2];	        
			BW_palette[0] = 0xff000000;
			BW_palette[1] =0xff000000|0xff0000|0xff00|0xff;
			rgbColorPalette = BW_palette;
		}
	    ColorModel cm = new IndexColorModel(bitsPerPixel, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
	   
	    return new BufferedImage(cm, raster, false, null);
	}
	
	@SuppressWarnings("unused")
    private static class PcxHeader {
		byte  manufacturer;
		/**
		 ************************************************************************************************
		 * Version  PC Paintbrush Version                            Description                        *
		 *    0         Version 2.5                          with fixed EGA palette information         *
		 *	  2         Version 2.8                          with modifiable EGA palette information    *
		 *	  3         Version 2.8                          without (useful) palette information       *
		 *	                                                 (a decoder should provide its own palette) *
		 *    4     PC Paintbrush for windows                ********************************************
		 *	  5     Version 3.0 of PC Paintbrush, PC         * Possible VGA 256 color palette attached  *
		 *	        Paintbrush Plus, PC Paintbrush for       *                                          *
		 *			Windows, Publisher's Paintbrush,         *                                          *
		 *			and all 24-bit image files.                                                         * 
		 ************************************************************************************************
		 */
		byte  version;
		byte  encoding;
		byte  bits_per_pixel;
		short xmin,ymin;
		short xmax,ymax;
		short hres;
		short vres;
		int  colorPalette[] = new int[16];
        byte  reserved;
		byte  color_plane;
		short bytes_per_line;
		short palette_type;
		byte  filler[] = new byte[58];

	    void readHeader(InputStream is) throws Exception {
		   int nindex = 0;
	       byte buf[] = new byte[128];

	       IOUtils.readFully(is, buf, 0, 128);
		
		   manufacturer = buf[nindex++];
		   version = buf[nindex++];
		   encoding = buf[nindex++];
		   bits_per_pixel = buf[nindex++];
		
		   xmin = (short)((buf[nindex++]&0xff)|((buf[nindex++]&0xff)<<8));
		   ymin = (short)((buf[nindex++]&0xff)|((buf[nindex++]&0xff)<<8));
		   xmax = (short)((buf[nindex++]&0xff)|((buf[nindex++]&0xff)<<8));
		   ymax = (short)((buf[nindex++]&0xff)|((buf[nindex++]&0xff)<<8));
		   hres = (short)((buf[nindex++]&0xff)|((buf[nindex++]&0xff)<<8));
		   vres = (short)((buf[nindex++]&0xff)|((buf[nindex++]&0xff)<<8));
  
		   for(int i = 0; i < 16; i++) {
		      colorPalette[i] = ((0xff<<24)|((buf[nindex++]&0xff)<<16)|((buf[nindex++]&0xff)<<8)|(buf[nindex++]&0xff));
		   }
    	
		   reserved = buf[nindex++];
		   color_plane = buf[nindex++];
		
		   bytes_per_line = (short)((buf[nindex++]&0xff)|((buf[nindex++]&0xff)<<8));
		   palette_type = (short)((buf[nindex++]&0xff)|((buf[nindex++]&0xff)<<8));

		   for(int i = 0; i < 58; i++) {
		      filler[i] = buf[nindex++];
		   }
	    }
   }
}