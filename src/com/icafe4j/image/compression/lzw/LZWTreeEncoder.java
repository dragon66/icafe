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

package com.icafe4j.image.compression.lzw;

import java.io.*;
import java.util.Arrays;

import com.icafe4j.image.compression.ImageEncoder;
import com.icafe4j.util.Updatable;

/** 
 * General purpose LZW Encoder to encode GIF or TIFF images.
 * Implemented using tree search as demonstrated by Bob Montgomery
 * in "LZW compression used to encode/decode a GIF file"
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/20/2012
 */
public class LZWTreeEncoder implements ImageEncoder {             
 	private int codeSize;
	private int codeLen;
	private int codeIndex;
	private int clearCode;
	private int endOfImage;
	private int limit;
	private boolean firstTime;

	/**
	 * A child is made up of a parent(or prefix) code plus a suffix color
	 * and siblings are strings with a common parent(or prefix) and different
	 * suffix colors
	 */
	private int child[] = new int[4097];
	private int siblings[] = new int[4097];
	private int suffix[] = new int[4097];
	private int parent = 0;

	private int bufIndex;
	private int empty_bits;
	private int buf_length;
	private byte bytes_buf[];
	
	private OutputStream os;
	private boolean isTIFF = false;
	private Updatable<Integer> writer;
	
	private static final short MASK[] = {0x00,0x01,0x03,0x07,0x0f,0x1f,0x3f,0x7f,0xff};
	
	int compressedDataLen = 0;
	
	// Constructor for GIF
	public LZWTreeEncoder(OutputStream os, int codesize, int buf_length) {		    
		codeSize = codesize;
		bytes_buf = new byte[buf_length];
		
		this.buf_length = buf_length;
		this.os = os;
	}    
   
	/**
	 * There are some subtle differences between the LZW algorithm used by TIFF and GIF images.
	 *
	 * Variable Length Codes:
	 * Both TIFF and GIF use a variation of the LZW algorithm that uses variable length codes.
	 * In both cases, the maximum code size is 12 bits. The initial code size, however, is different
	 * between the two formats. TIFF's initial code size is always 9 bits. GIF's initial code size 
	 * is specified on a per-file basis at the beginning of the image descriptor block, 
	 * with a minimum of 3 bits.
	 * <p>
	 * TIFF and GIF each switch to the next code size using slightly different algorithms. 
	 * GIF increments the code size as soon as the LZW string table's length is equal to 2**code_size,
	 * while TIFF increments the code size when the table's length is equal to 2**code_size - 1.
	 * <p>
	 * Packing Bits into Bytes
	 * TIFF and GIF LZW algorithms differ in how they pack the code bits into the byte stream.
	 * The least significant bit in a TIFF code is stored in the most significant bit of the byte stream,
	 * while the least significant bit in a GIF code is stored in the least significant bit of the byte stream.
	 * <p>
	 * Special Codes
	 * TIFF and GIF both add the concept of a 'Clear Code' and a 'End of Information Code' to the LZW algorithm. 
	 * In both cases, the 'Clear Code' is equal to 2**(code_size - 1) and the 'End of Information Code' is equal
	 * to the Clear Code + 1. These 2 codes are reserved in the string table. So in both cases, the LZW string
	 * table is initialized to have a length equal to the End of Information Code + 1.	
	 */
	// Constructor for TIFF    
    public LZWTreeEncoder(OutputStream os, int codesize, int buf_length, Updatable<Integer> writer) {
    	this(os, codesize, buf_length);
		this.isTIFF = true;
		this.writer = writer;
	}
	
    /**
	 * LZW encode the pixel byte array.
	 * 
	 * @param pixels pixel array to be encoded
	 * @param start offset to the pixel array to start encoding
	 * @param len number of bytes to be encoded
	 * @throws Exception
	 */
	public void encode(byte[] pixels, int start, int len) throws Exception {
		if(start < 0 || len <= 0) return;
		if(start + len > pixels.length) len = pixels.length - start;
		// Define local variables
		int son = 0;
		int brother = 0;
		int color = 0;
		int counter = 0;
		
		if(firstTime) {
			// Get the first color and assign it to parent
			parent = (pixels[start++]&0xff);
			counter++;
			firstTime = false;
	    }
		
		while (counter < len) {
			color = (pixels[start++]&0xff);
			counter++;
			son = child[parent];

			if ( son > 0) {
				if (suffix[son] == color) {
					parent = son;
				} else {
					brother = son;
					while (true) {
						if (siblings[brother] > 0) {
							brother = siblings[brother];
							if (suffix[brother] == color) {
							   parent = brother;
							   break;
						    }
						} else {
							siblings[brother] = codeIndex;
							suffix[codeIndex] = color;
							send_code_to_buffer(parent);
							parent = color;
							codeIndex++;
               				// Check code length
				            if(codeIndex > (isTIFF?limit-1:limit)) {
								if (codeLen == 12) {
				                    send_code_to_buffer(clearCode);
				                    init_encoder(codeSize);
			                    } else {
					                codeLen++;
									limit = 1<<codeLen;
								}
			                }
							break;
						}
					}
				}
			} else {
				child[parent] = codeIndex;
				suffix[codeIndex] = color;
				send_code_to_buffer(parent);
				parent = color;
				codeIndex++;
				// Check code length
				if(codeIndex > (isTIFF?limit-1:limit)) {
                   if (codeLen == 12) { 
				       send_code_to_buffer(clearCode);
				       init_encoder(codeSize);
			       } else {
					   codeLen++;
					   limit = 1<<codeLen;
				   }
			    }
			}
		}
	}
	
	/**
	 * Finish up the compression. This stand-alone method is
	 * useful when compression is done through multiple calls
	 * to encode.
	 */
	public void finish() throws Exception {
	   	// Send the last color code to the buffer
		send_code_to_buffer(parent);
		// Send the endOfImage code to the buffer
		send_code_to_buffer(endOfImage);
		// Flush the last code buffer
		flush_buf(bufIndex+1);
		
		if(isTIFF && writer != null) {
			writer.update(compressedDataLen);
		}
	}
	
	// Flush the buffer as needed
	private void flush_buf(int len) throws Exception {	
		if(!isTIFF) {
			os.write(len);
		}
		os.write(bytes_buf,0,len);
		// Reset the bytes buffer index
		bufIndex = 0;
		Arrays.fill(bytes_buf, 0, len, (byte)0x00);
		
		compressedDataLen += len;
	}
	
	/**
	 * This method is only intended to be called after calling finish()
	 * @return total compressed bytes
	 */
	public int getCompressedDataLen() {
		return compressedDataLen;
	}
    
    private void init_encoder(int codesize) {
		Arrays.fill(child, 0x00);
		Arrays.fill(siblings, 0x00);
		codeLen = codesize + 1;
		limit = 1<<codeLen;
	    codeIndex = endOfImage + 1;
    }
	
	public void initialize() throws Exception {
		clearCode = 1 << codeSize;
	    endOfImage = clearCode + 1;
  	   	firstTime = true;
  	    empty_bits = 0x08;
  	    
		compressedDataLen = 0;
  	   	
  		init_encoder(codeSize);
		// Write out the length of the root for GIF
		if(!isTIFF)
			os.write(codeSize);
		// Write out the first code - clear code
		// Tell the decoder to initialize string table
		send_code_to_buffer(clearCode);
	}
	
	// Translate codes into bytes
    private void send_code_to_buffer(int code)throws Exception {
    	int temp = codeLen;
    	
    	if(isTIFF) {
    		temp = codeLen - empty_bits;
    		bytes_buf[bufIndex] |= ((code>>>temp)&MASK[empty_bits]);
    		
    		while (temp > 8) {
    			if (++bufIndex >= buf_length)
					flush_buf(buf_length);
				bytes_buf[bufIndex] |= ((code>>>(temp - 8))&MASK[8]);
				temp -= 8;
			}
    		
    		if(temp > 0) {
    			if (++bufIndex >= buf_length)
					flush_buf(buf_length);
				bytes_buf[bufIndex] |= ((code&MASK[temp])<<(8 - temp));
    			temp -= 8;
    		}
       	} else { // GIF			
			// Shift the code to the left of the last byte in bytes_buf
			bytes_buf[bufIndex] |= ((code&MASK[empty_bits])<<(8-empty_bits));
			code >>= empty_bits;
	        temp -= empty_bits;
	        // If the code is longer than the empty_bits
			while (temp > 0) {	// For GIF, the buf_length is no longer than 0xff
				if (++bufIndex >= buf_length)
					flush_buf(buf_length);
				bytes_buf[bufIndex] |= (code&0xff);
				code >>= 8;
				temp -= 8;
			}
		}
		empty_bits = -temp;   
	}
}