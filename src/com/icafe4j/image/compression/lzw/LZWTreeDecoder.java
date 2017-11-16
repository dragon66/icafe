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
 * LZWTreeDecoder.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    14Oct2014  Revised to show specification violation TIFF LZW compression
 *                  which actually falls back to GIF LZW compression completely
 */

package com.icafe4j.image.compression.lzw;

import java.io.*;

import com.icafe4j.image.compression.ImageDecoder;
import com.icafe4j.io.IOUtils;

/** 
 * General purpose LZW decoder to decode LZW encoded GIF or TIFF images.
 * Implemented using tree search as demonstrated by Bob Montgomery in
 * "LZW compression used to encode/decode a GIF file."
 * <p>
 * There are some subtle differences between the LZW algorithm used by TIFF and GIF images.
 * <ul>
 * <li> Variable Length Codes:
 * Both TIFF and GIF use a variation of the LZW algorithm that uses variable length codes.
 * In both cases, the maximum code size is 12 bits. The initial code size, however, is different
 * between the two formats. TIFF's initial code size is always 9 bits. GIF's initial code size 
 * is specified on a per-file basis at the beginning of the image descriptor block, 
 * with a minimum of 3 bits.
 * <li>
 * TIFF and GIF each switch to the next code size using slightly different algorithms. 
 * GIF increments the code size as soon as the LZW string table's length is equal to 2**code-size,
 * while TIFF increments the code size when the table's length is equal to 2**code-size - 1.
 * <li>
 * Packing Bits into Bytes
 * TIFF and GIF LZW algorithms differ in how they pack the code bits into the byte stream.
 * The least significant bit in a TIFF code is stored in the most significant bit of the bytestream,
 * while the least significant bit in a GIF code is stored in the least significant bit of the bytestream.
 * <li>
 * Special Codes
 * TIFF and GIF both add the concept of a 'Clear Code' and a 'End of Information Code' to the LZW algorithm. 
 * In both cases, the 'Clear Code' is equal to 2**(code-size - 1) and the 'End of Information Code' is equal
 * to the Clear Code + 1. These 2 codes are reserved in the string table. So in both cases, the LZW string
 * table is initialized to have a length equal to the End of Information Code + 1.	
 * </ul>
 * <p>
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/11/2012
 */
public class LZWTreeDecoder implements ImageDecoder {
	// Variables for code reading
	private int bits_remain = 0;
	private int bytes_available = 0;
	private int temp_byte = 0;        
	private int bufIndex = 0;
	private byte bytes_buf[] = new byte[256];
    
	private int oldcode = 0 ;
	private int code = 0;
	private int[] prefix = new int[4097];
	private int[] suffix = new int[4097];

	private int min_code_size;
	private int clearCode;
	// End of image for GIF or end of information for TIFF
	private int endOfImage;

	// Variables to clear table
	private int codeLen;
	private int codeIndex;
	private int limit;

	private int first_code_index;
	private int first_char;

	private InputStream is;
	private boolean isTIFF;// Taking care of the difference between GIF and TIFF.
	
	private boolean isCodeBigEndian;

	private static final int MASK[] = {0x00,0x001,0x003,0x007,0x00f,0x01f,0x03f,0x07f,0x0ff,0x1ff,0x3ff,0x7ff,0xfff};
	
    private int leftOver = 0;// Used to keep track of the not fully expanded code string.
	private int buf[] = new int[4097];
	
	private static final int MAX_CODE = (1<<12);
	
	public LZWTreeDecoder(InputStream is, int min_code_size) {
		if(min_code_size < 2 || min_code_size > 12)
			   throw new IllegalArgumentException("invalid min_code_size: " + min_code_size);
		this.is = is;
		this.min_code_size = min_code_size;
	   	clearCode = (1<<min_code_size);
	   	endOfImage = clearCode+1;
	   	first_code_index = endOfImage+1;
	   	isCodeBigEndian = true;
	   	// Reset string table
	   	clearStringTable();
	}
	
	public LZWTreeDecoder(int min_code_size, boolean isTIFF) {
		this(null, min_code_size);
		this.isTIFF = isTIFF;
	}
	
	private void clearStringTable() {
	   	// Reset string table
	   	codeLen = min_code_size+1;
	   	limit = (1<<codeLen)-1;
	   	codeIndex = endOfImage;	
	}
	
	public int decode(byte[] pix, int offset, int len) throws Exception {
		int counter = 0;// Keep track of how many bytes have been decoded.
		///////////////
		int tempcode = 0;
		int i = 0;
        //////////////////////////////////////////////////////////
		if(leftOver>0){//flush out left over first.
			for( int j = leftOver-1; j >= 0; j--, leftOver-- ) {
				   if ((offset >= pix.length)||(counter>=len))// Will this ever happen?!
					   return counter;
				   pix[offset++] = (byte)buf[j];
				   counter++;
	       }
		}
        //////////////////////////////////////////////////////////
        label:
		do {
			i = 0;
			code = readLZWCode();
			tempcode = code;

			if(code == clearCode) {
				clearStringTable();
			} else if(code == endOfImage) {  
			    break;
			} else {
			   if(code >= codeIndex) {
                    tempcode = oldcode;
  				    buf[i++] = first_char;
			   }
		       while (tempcode >= first_code_index) {
			       buf[i++] = suffix[tempcode];
		           tempcode = prefix[tempcode];
		       }
		       buf[i++] = tempcode;

			   suffix[codeIndex] = first_char = tempcode;
		       prefix[codeIndex] = oldcode;
		       // Check boundary to deal with deferred clear code in LZW compression
		       if(codeIndex < MAX_CODE) codeIndex++; 
		       
		       oldcode = code;
	           
			   if((codeIndex > (isTIFF && isCodeBigEndian?limit-1:limit)) && (codeLen<12)) {
		           codeLen++;
			       limit = (1<<codeLen)-1;			  
			   }
			   // Output strings for the current code
		       leftOver = i;
			   for( int j = i-1; j >= 0; j--, leftOver--, counter++ ) {
				   if ((offset >= pix.length)||(counter>=len))
			             break label;
				   pix[offset++] = (byte)buf[j];
		       }
		    }
        } while(true);

		return counter;
 	}
   
	private int readLZWCode() throws Exception {
        int temp = 0;
	
		if(!isTIFF || !isCodeBigEndian) 
			temp = (temp_byte >> (8-bits_remain));
		else {
			// Different packing order from GIF
			temp = (temp_byte & MASK[bits_remain]); 
		}			
	
		while (codeLen > bits_remain) {
			if(!isTIFF) { // GIF
				if(bytes_available == 0) {
					// find another data block available
					// Start a new image data sub-block if possible!
	            	// The block size bytes_available is no bigger than 0xff
					bytes_available = is.read();
					
					if(bytes_available > 0) {
						IOUtils.readFully(is,bytes_buf,0,bytes_available);
						bufIndex = 0;
					} else if(bytes_available == 0)
						return endOfImage;
					else {
						return endOfImage;
					}
				}				
				temp_byte = bytes_buf[bufIndex++]&0xff;
				bytes_available--;
				temp |= (temp_byte<<bits_remain);
			} else {
				temp_byte = is.read();
				if(temp_byte == -1)
					return endOfImage;
				if(isCodeBigEndian)
					temp = ((temp<<8)|temp_byte);
				else
					temp |= (temp_byte<<bits_remain);
			}
			bits_remain += 8;
		}
		
		if(isTIFF && isCodeBigEndian) 
			temp = (temp>>(bits_remain-codeLen));
        
		bits_remain -= codeLen;
        
		return (temp&MASK[codeLen]);
	}
	
	public void setInput(byte[] input) {
		setInput(input, 0, input.length);
	}

	public void setInput(byte[] input, int offset, int len) {
		if(input[offset] == 0x00 && input[offset+1] == 0x01)  
			isCodeBigEndian = false;
		else
			isCodeBigEndian = true;
		is = new ByteArrayInputStream(input, offset, len);
		// Must discard the remaining bits!!!
		bits_remain = 0;
		// and the leftover!!!
		leftOver = 0;
		// Reset string table
		clearStringTable();		
	}
}