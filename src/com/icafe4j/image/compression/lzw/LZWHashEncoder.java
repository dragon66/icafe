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
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/20/2012
 */
public class LZWHashEncoder implements ImageEncoder {
	private static short EMPTY = -1;         
 	
	private int codeSize;
	private int codeLen;
	private int clearCode;
	private int endOfImage;
	private int limit;
	private short prefix = EMPTY;

	private int bufIndex = 0;
	private int empty_bits;
	private int buf_length;
	private byte bytes_buf[];
		
	private OutputStream os;
	private boolean isTIFF=false;
	private Updatable<Integer> writer;
	private LZWCompressionTable stringTable = new LZWCompressionTable();
	
	private static final short mask[] = {0x00,0x01,0x03,0x07,0x0f,0x1f,0x3f,0x7f,0xff};

    // Constructor for GIF
 	public LZWHashEncoder(OutputStream os, int codesize, int buf_length) { 		  
 		codeSize = codesize; 		
 		bytes_buf = new byte[buf_length];
 		
 		this.buf_length = buf_length;
 		this.os = os;
 	}
 	
 	// Constructor for TIFF    
    public LZWHashEncoder(OutputStream os, int codesize, int buf_length, Updatable<Integer> writer) {
     	this(os, codesize, buf_length);
 		this.isTIFF = true;
 		this.writer = writer;
 	}    
    
	public void initialize() throws Exception {
		clearCode = 1 << codeSize;
 	    endOfImage = clearCode + 1;
   	    codeLen = codeSize + 1;
 	    limit = (1<<codeLen) - 1;      
		empty_bits = 0x08;
		stringTable.clearTable(codeSize);
		
		compressedDataLen = 0;
		
		prefix = EMPTY;
		
		if(!isTIFF)
			// Write out the length of the root
			os.write(codeSize);
		// Write out the first code - clear code
		// Tell the decoder to initialize string table
		send_code_to_buffer(clearCode);
	}
	
	/**
	 * @param len the number of bytes to be encoded
	 */
	public void encode(byte[] pixels, int start, int len) throws Exception {	
		if(start < 0 || len <= 0) {
			return;
		}
		// Define local variables
		byte c = 0;
		short cur_str = 0;// Current string
		
		for (int i=start;i<start+len;i++) {
			c = pixels[i];
			
            if((cur_str = stringTable.findCharString(prefix, c)) != EMPTY)// In table
				prefix = cur_str;
			else {// Not in table
				send_code_to_buffer(prefix);
				if(stringTable.addCharString(prefix, c) > (isTIFF?limit-1:limit)) {
					if(++codeLen > 12) {
						codeLen--;
						send_code_to_buffer(clearCode);
						stringTable.clearTable(codeSize);
						codeLen = codeSize + 1;
					}
					limit = (1<<codeLen) - 1;
				}
				prefix = (short)(c&0xff);
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
	   if(prefix != EMPTY)
	      send_code_to_buffer(prefix);
	   // Send the endOfImage code to the buffer
	    send_code_to_buffer(endOfImage);
		// Flush the last code buffer
		flush_buf(bufIndex+1);
		
		if(isTIFF) {
			writer.update(compressedDataLen);
		}
	}
	
	public int getCompressedDataLen() {
		return compressedDataLen;
	}
	
	// Translate codes into bytes
    private void send_code_to_buffer(int code)throws Exception {
    	int temp = codeLen;
    	
    	if(isTIFF) {
    		temp = codeLen - empty_bits;
    		bytes_buf[bufIndex] |= ((code>>>temp)&mask[empty_bits]);
    		
    		while (temp > 8) {
    			if (++bufIndex >= buf_length)
					flush_buf(buf_length);
				bytes_buf[bufIndex] |= ((code>>>(temp - 8))&mask[8]);
				temp -= 8;
			} 
    		
    		if(temp > 0) {
    			if (++bufIndex >= buf_length)
					flush_buf(buf_length);
				bytes_buf[bufIndex] |= ((code&mask[temp])<<(8 - temp));
    			temp -= 8;
    		}
       	} else { // GIF
			// Shift the code to the left of the last byte in bytes_buf
			bytes_buf[bufIndex] |= ((code&mask[empty_bits])<<(8-empty_bits));
			code >>= empty_bits;
	        temp -= empty_bits;
	        // If the code is longer than the empty_bits
			while (temp > 0) {
				// For GIF, the buf_length is no longer than 0xff
				if (++bufIndex >= buf_length)
					flush_buf(buf_length);
				bytes_buf[bufIndex] |= (code&0xff);
				code >>= 8;
				temp -= 8;
			}
		}
		empty_bits = -temp;	    
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
	
	int compressedDataLen = 0;
}