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

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.io.IOUtils;

/** 
 * Decodes and shows 8 bit color mapped,black and white and 16,24 and 32 bit
 * true color uncompressed and RLE TGA images. The compressed color mapped
 * formats 32 and 33 are not implemented in this version because I have not
 * found any information on these two formats.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/26/2007
 */
public class TGAReader extends ImageReader {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TGAReader.class);
	// Wrapper for the TGA header
	private static class TgaHeader {
		byte  id_length;
		byte  colourmap_type;
		byte  image_type; 
		short first_entry_index;
		short colourmap_length;
		byte  colourmap_entry_size; 
		short x_origin;
		short y_origin;
		short width;
		short height;
		byte  bits_per_pixel; 
		byte  image_descriptor;	

		void readHeader(InputStream is) throws Exception {
			int nindex = 0;
			byte header[] = new byte[18]; // For the 18 bit header trunk

			IOUtils.readFully(is, header, 0, 18);
         
			id_length = header[nindex++];
			colourmap_type = header[nindex++];
			image_type = header[nindex++];
			first_entry_index = (short)((header[nindex++]&0xff)|((header[nindex++]&0xff)<<8));
			colourmap_length = (short)((header[nindex++]&0xff)|((header[nindex++]&0xff)<<8));
			colourmap_entry_size = header[nindex++];
			x_origin = (short)((header[nindex++]&0xff)|((header[nindex++]&0xff)<<8));
			y_origin = (short)((header[nindex++]&0xff)|((header[nindex++]&0xff)<<8));
			width = (short)((header[nindex++]&0xff)|((header[nindex++]&0xff)<<8));
			height = (short)((header[nindex++]&0xff)|((header[nindex++]&0xff)<<8));
			bits_per_pixel = header[nindex++];
			image_descriptor = header[nindex++];
		} 
   	}
	
	// Scan mode
	public static final int SCAN_MODE_BOTTOM_LEFT = 0;
	public static final int SCAN_MODE_BOTTOM_RIGHT = 1;
	public static final int SCAN_MODE_TOP_LEFT = 2;	
	public static final int SCAN_MODE_TOP_RIGHT = 3;
	
	// TGA header
	private TgaHeader tgaHeader;
	
   	private int scanMode = 0;
	private int l = 0, m = 0, n = 0, o = 0;
	
	private int[] pix;
  
	public BufferedImage read(InputStream is) throws Exception {
		tgaHeader = new TgaHeader();
		tgaHeader.readHeader(is);
    
		bitsPerPixel = tgaHeader.bits_per_pixel;
 	   	width = tgaHeader.width;
 	   	height = tgaHeader.height;
 	   	pix = new int[width*height];
	
 	   	if (tgaHeader.colourmap_type != 0 && tgaHeader.colourmap_type != 1) {
 	   		LOGGER.error("Can only handle colour map types of 0 and 1");    
 	   		return null;    
 	   	}

 	   	scanMode = ((tgaHeader.image_descriptor&0x30)>>4);

 	   	switch (scanMode) { 	   	
 	   		case SCAN_MODE_BOTTOM_LEFT:
				l = height-1; m = -1; n = 0; o = 1;
				break;
            case SCAN_MODE_BOTTOM_RIGHT:
                l = height-1; m = -1; n = width-1; o = -1;
				break;
			case SCAN_MODE_TOP_LEFT:
                l = 0; m = 1; n = 0; o = 1;
				break;
            case SCAN_MODE_TOP_RIGHT:
				l = 0; m = 1; n = width-1; o = -1; 
				break;
			default:
 	   	}
       
 	   	LOGGER.info("Image x_origin: {}", tgaHeader.x_origin);
 	   	LOGGER.info("Image y_origin: {}", tgaHeader.y_origin);

 	   	switch (tgaHeader.image_type) {
	   		case 0:
	   			LOGGER.info("There are no data in the image file");
	   			System.exit(1);
	   		case 1:
	   			readCMPTga(is);
	   			break;
	   		case 2: 
	   			readTrueColorTga(is);
	   			break;
	   		case 3:
	   			read_BW_Tga(is);
	   			break;
	   		case 9:
	   			read_RLE_CMP_Tga(is);
	   			break;
	   		case 10:
	   			read_RLE_TrueColor_Tga(is);
	   			break;
	   		case 11:
	   			read_RLE_BW_Tga(is);
	   			break;
	   		case 32: 
	   		case 33:
	   			LOGGER.error("Not implemented for compressed color mapped images");
	   			return null;
	   		default:
	   			LOGGER.error("I can't find a type matches this");
	   			return null;			
 	   	}
	   
 	   	//Create a BufferedImage
 	   	DataBuffer db = new DataBufferInt(pix, pix.length);
 	   	WritableRaster raster = Raster.createPackedRaster(db, width, height, width,  new int[] {0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000}, null);
 	   	ColorModel cm = new DirectColorModel(32, 0x00FF0000, 0x0000ff00, 0x000000ff, 0xff000000);
			
 	   	return new BufferedImage(cm, raster, false, null);
	}
   	
	private void read_BW_Tga(InputStream is) throws Exception {
		bitsPerPixel = 1;
		LOGGER.info("Uncompressed Black and White Tga image!");

		int index = 0;
		  
		IOUtils.skipFully(is, tgaHeader.id_length);
		IOUtils.skipFully(is, tgaHeader.colourmap_type * tgaHeader.colourmap_length);

		byte brgb[] = new byte[width*height];
		IOUtils.readFully(is, brgb, 0, width*height);

		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				pix[width*(l+m*i)+n+o*j] = (0xff<<24)|((brgb[index]&0xff))|((brgb[index]&0xff)<<8)|((brgb[index++]&0xff)<<16);
			}
		}
		is.close();
	}

	private void read_RLE_BW_Tga(InputStream is) throws Exception {
		bitsPerPixel = 1;
		LOGGER.info("Black and White Tga RLE image!");

		int nindex = 0;
		int p = 0, k = 0, i = 0, j = 0;
   
		IOUtils.skipFully(is, tgaHeader.id_length);
		IOUtils.skipFully(is, tgaHeader.colourmap_type * tgaHeader.colourmap_length);

		byte brgb[] = IOUtils.readFully(is, 4096);
	
		while(p < width*height) { 
			            
			k = (brgb[nindex++] & 0x7f)+1; 
						 					       
			if ((brgb[nindex-1] & 0x80) != 0) {
				for(int q = 0; q < k; q++){
					pix[width*(l+m*i)+n+o*j] = (0xff<<24)|((brgb[nindex]&0xff))|((brgb[nindex]&0xff)<<8)|((brgb[nindex]&0xff)<<16);
					j++;
					if(j%width == 0) {
						i++;
						j = 0;
					}
					p++;
					if (p >= width*height) {
						break;
					}
				}
				nindex += 1;
			} else {
				for (int q = 0; q < k; q++) {
					pix[width*(l+m*i)+n+o*j] = (0xff<<24)|((brgb[nindex]&0xff))|((brgb[nindex]&0xff)<<8)|((brgb[nindex++]&0xff)<<16);
					j++;
					if(j%width == 0) {
						i++;
						j = 0;
					}
					p++;
					if (p >= width*height) {
						break;
					}
				}
			}
		}
		is.close();
	}

	private void  read_RLE_CMP_Tga(InputStream is) throws Exception {
		LOGGER.info("color mapped Tga RLE image!");
	   
		int nindex = 0;
		int p = 0, k = 0, i = 0, j = 0;

		if (tgaHeader.bits_per_pixel != 8) {
			LOGGER.error("Can only handle 8 bit color mapped tga file");
			return;
		}

		readPalette(is);

		byte brgb[] = IOUtils.readFully(is, 4096);
	     
		while(p < width*height) { 

			k = (brgb[nindex++] & 0x7f)+1; 
						 					       
			if ((brgb[nindex-1] & 0x80) != 0) {
				for(int q = 0; q < k; q++){
					pix[width*(l+m*i)+n+o*j] = rgbColorPalette[brgb[nindex]&0xff];
					j++;
					if(j%width == 0) {
						i++;
						j = 0;
					}
					p++;
					if (p >= width*height) {
						break;
					}
				}
				nindex += 1;
			} else {
				for (int q = 0; q < k; q++)	{
					pix[width*(l+m*i)+n+o*j] = rgbColorPalette[brgb[nindex++]&0xff];
					j++;
					if(j%width == 0) {
						i++;
						j = 0;
					}
					p++;
					if (p >= width*height) {
						break;
					}
				}
			}
		} 
		is.close();
	}

	private void  read_RLE_TrueColor_Tga(InputStream is) throws Exception {
		int skipover = 0;
		int nindex = 0;
		int p = 0, k = 0, i = 0, j = 0;

		skipover += tgaHeader.id_length; 
		skipover += tgaHeader.colourmap_type * tgaHeader.colourmap_length; 
		IOUtils.skipFully(is, skipover);
	          
		byte brgb[] = IOUtils.readFully(is, 4096);
		 
		if(tgaHeader.bits_per_pixel  == 24) { // 24 bit image
			LOGGER.info("24 bits Tga RLE image!");
			 
			while(p < width*height) { 
				k = (brgb[nindex++] & 0x7f)+1; 
						 					       
				if ((brgb[nindex-1] & 0x80) != 0) {
					for(int q = 0; q < k; q++){
						pix[width*(l+m*i)+n+o*j] = (0xff<<24)|((brgb[nindex]&0xff))|((brgb[nindex+1]&0xff)<<8)|((brgb[nindex+2]&0xff)<<16);
						j++;
						if(j%width == 0) {
							i++;
							j = 0;
						}
						p++;
						if (p >= width*height) {
							break;
						}
					}
					nindex += 3;
				} else {
					for (int q = 0; q < k; q++)	{
						pix[width*(l+m*i)+n+o*j] = (0xff<<24)|((brgb[nindex++]&0xff))|((brgb[nindex++]&0xff)<<8)|((brgb[nindex++]&0xff)<<16);  
						j++;
						if(j%width == 0) {
							i++;
							j = 0;
						}
						p++;
						if (p >= width*height) {
							break;
						}
					}
				}
			} 
		} else if (tgaHeader.bits_per_pixel  == 32) { // 32 bit image 
			LOGGER.info("32 bits Tga RLE image!");            
			
			while(p < width*height) { 
				k = (brgb[nindex++] & 0x7f)+1; 
						 					       
				if ((brgb[nindex-1] & 0x80) != 0) {
					for(int q = 0; q < k; q++){
						pix[width*(l+m*i)+n+o*j] = ((brgb[nindex]&0xff))|((brgb[nindex+1]&0xff)<<8)|((brgb[nindex+2]&0xff)<<16)|(((brgb[nindex+3]&0xff)<<24));
						j++;
						if(j%width == 0) {
							i++;
							j = 0;
						}
						p++;
						if (p >= width*height) {
							break;
						}
					}
					nindex += 4;
				} else {
					for (int q = 0; q < k; q++) {
						pix[width*(l+m*i)+n+o*j] = ((brgb[nindex++]&0xff))|((brgb[nindex++]&0xff)<<8)|((brgb[nindex++]&0xff)<<16)|(((brgb[nindex++]&0xff)<<24));  
						j++;
						if(j%width == 0) {
							i++;
							j = 0;
						}
						p++;
						if (p >= width*height) {
							break;
						}
					}
				}
			}	 
		} else if (tgaHeader.bits_per_pixel == 15
					|| tgaHeader.bits_per_pixel == 16) {			
		
			LOGGER.info("16 bits Tga RLE image!");
			 
			int r = 0, g = 0, b = 0, a = 0;  
  
			/** 
			 * The two byte entry is broken down as follows:
			 * ARRRRRGG GGGBBBBB, where each letter represents a bit.
			 * but, because of the lo-hi storage order, the first byte
			 * coming from the file will actually be GGGBBBBB, and the
			 * second will be ARRRRRGG. "A" represents an attribute.
			 */	 
	
			while(p < width*height) { 
			            
				k = (brgb[nindex++] & 0x7f)+1; 
						 					       
				if ((brgb[nindex-1] & 0x80) != 0) {
					r = ((brgb[++nindex] & 0x7c) <<1);
					g = (((brgb[nindex] & 0x03) << 6) | ((brgb[nindex-1] & 0xe0) >> 2));
					b = ((brgb[nindex-1] & 0x1f)<<3);
					a = 0xff;
					nindex++;
					for(int q = 0; q < k; q++) {
						pix[width*(l+m*i)+n+o*j] = ((a<<24)|(r<<16)|(g<<8)|b);
						j++;
						if(j%width == 0) {
							i++;
							j = 0;
						}
						p++;
						if (p >= width*height) {
							break;
						}
					}
				} else {
					for (int q = 0; q < k; q++)	{
						r = ((brgb[++nindex] & 0x7c) <<1);
						g = (((brgb[nindex] & 0x03) << 6) | ((brgb[nindex-1] & 0xe0) >> 2));
						b = ((brgb[nindex-1] & 0x1f)<<3);
						a = 0xff;
						nindex++;
						pix[width*(l+m*i)+n+o*j] = ((a<<24)|(r<<16)|(g<<8)|b);
						j++;
						if(j%width == 0) {
							i++;
							j = 0;
						}
						p++;
						if (p >= width*height) {
							break;
						}
					}
				}
			} 
		}	
		is.close();
	}

	private void  readCMPTga(InputStream is) throws Exception {
		LOGGER.info("color mapped Tga uncompressed image!");

		int index = 0;

		readPalette(is);
         
		if (tgaHeader.bits_per_pixel != 8) {	
			LOGGER.error("Can only handle 8 bit color mapped tga file");
			return;
		}
		
		byte brgb[] = new byte[width*height];
		IOUtils.readFully(is, brgb, 0, width*height);

		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				pix[width*(l+m*i)+n+o*j] = rgbColorPalette[brgb[index++]&0xff];
			}
		}
		is.close();
	}

	private void readPalette(InputStream is) throws Exception {
		int index = 0, r = 0, g = 0, b = 0, a = 0;
		int byte_per_pixel = (tgaHeader.colourmap_entry_size+1)/8;
		int readbytes = byte_per_pixel*(tgaHeader.colourmap_length-tgaHeader.first_entry_index);
		byte brgb[] = new byte[readbytes];
		  
		int colorsUsed = tgaHeader.colourmap_length-tgaHeader.first_entry_index; // Actual colors used
		rgbColorPalette = new int[colorsUsed];
	 
		IOUtils.skipFully(is, tgaHeader.id_length);
		IOUtils.skipFully(is, tgaHeader.first_entry_index);
		IOUtils.readFully(is, brgb, 0, readbytes);

		switch (tgaHeader.colourmap_entry_size) {
			case 15:
			case 16:
				for (int i = 0; i < tgaHeader.colourmap_length - tgaHeader.first_entry_index; i++) {
					r = ((brgb[++index] & 0x7c) <<1);
		            g = (((brgb[index] & 0x03) << 6) | ((brgb[index-1] & 0xe0) >> 2));
	                b = ((brgb[index-1] & 0x1f)<<3); 
                    a = 0xff;
					rgbColorPalette[i] = ((a<<24)|(r<<16)|(g<<8)|b);
					index++;
                } 
				break;
			case 24:
				for (int i = 0; i < tgaHeader.colourmap_length - tgaHeader.first_entry_index; i++)         
					rgbColorPalette[i] = (0xff<<24)|((brgb[index++]&0xff))|((brgb[index++]&0xff)<<8)|((brgb[index++]&0xff)<<16);
				break;
			case 32:
				for (int i = 0; i < tgaHeader.colourmap_length - tgaHeader.first_entry_index; i++)         
					rgbColorPalette[i] = ((brgb[index++]&0xff))|((brgb[index++]&0xff)<<8)|((brgb[index++]&0xff)<<16)|((brgb[index++]&0xff)<<24);
				break;
			default:
		}
	}

	private void readTrueColorTga(InputStream is) throws Exception {
		int skipover = 0;
		int nindex = 0;
	
		skipover +=  tgaHeader.id_length;
		skipover += tgaHeader.colourmap_type * tgaHeader.colourmap_length; 
		IOUtils.skipFully(is, skipover);
	
		int bytes2read = (tgaHeader.bits_per_pixel + 7) / 8;
		
		byte brgb[] = new byte[bytes2read*width*height];
		IOUtils.readFully(is, brgb, 0, bytes2read*width*height);
	   
		 
		if (tgaHeader.bits_per_pixel == 24) { // 24 bit image
			LOGGER.info("24 bits Tga uncompressed image!");
            
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					pix[width*(l+m*i)+n+o*j] = (0xff<<24)|((brgb[nindex++]&0xff))|((brgb[nindex++]&0xff)<<8)|((brgb[nindex++]&0xff)<<16);
				}
			}
		} else if (tgaHeader.bits_per_pixel == 32) {// 32 bit image 
			LOGGER.info("32 bits Tga uncompressed image!");
            
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					pix[width*(l+m*i)+n+o*j] = ((brgb[nindex++]&0xff))|((brgb[nindex++]&0xff)<<8)|((brgb[nindex++]&0xff)<<16)|((brgb[nindex++]&0xff)<<24);
				}
			}
		} else if (tgaHeader.bits_per_pixel == 15 
					|| tgaHeader.bits_per_pixel == 16) { // 16 bit image
			LOGGER.info("16 bits Tga uncompressed image!");
			int r = 0, g = 0, b = 0, a = 0;
			/** 
			 * The two byte entry is broken down as follows:
			 * ARRRRRGG GGGBBBBB, where each letter represents a bit.
			 * but, because of the lo-hi storage order, the first byte
			 * coming from the file will actually be GGGBBBBB, and the
			 * second will be ARRRRRGG. "A" represents an attribute.
			 */
			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					r = ((brgb[++nindex] & 0x7c) <<1);
					g = (((brgb[nindex] & 0x03) << 6) | ((brgb[nindex-1] & 0xe0) >> 2));
					b = ((brgb[nindex-1] & 0x1f)<<3);
					//a = (brgb[nindex] & 0x80);
					a = 0xff;
					nindex++;
					pix[width*(l+m*i)+n+o*j] = ((a<<24)|(r<<16)|(g<<8)|b);
				}
			}
		}
		is.close();
	}
}