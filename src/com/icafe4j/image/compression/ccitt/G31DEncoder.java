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
 * G31DEncoder.java
 *
 * Who   Date       Description
 * ====  =========  ============================================================
 * WY    28Mar2016  Made scanLineWidth protected and removed getScanLineWidth()
 */

package com.icafe4j.image.compression.ccitt;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.icafe4j.image.compression.ImageEncoder;
import com.icafe4j.util.ArrayUtils;
import com.icafe4j.util.CollectionUtils;
import com.icafe4j.util.Updatable;

import static com.icafe4j.image.compression.ccitt.T4Code.runLenArray;

/**
 * CCITT Group 3 one dimensional encoding (AKA CCITT RLE)
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/20/2013
 */
public class G31DEncoder implements ImageEncoder {

	private int empty_bits;
	private byte[] bytes_buf;
	private int buf_length;
	private OutputStream os;
	private Updatable<Integer> writer;
	private boolean extraFlush;
	private int bufIndex;
	
	private int compressedDataLen = 0;
	
	private static final short mask[] = {
		0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff, 0x01ff, 0x03ff, 0x07ff, 0x0fff, 0x1fff
	};
	
	public G31DEncoder(OutputStream os, int scanLineWidth, int buf_length, Updatable<Integer> writer) {	
		this.scanLineWidth = scanLineWidth;
		bytes_buf = new byte[buf_length];		
		this.buf_length = buf_length;
		this.os = os;
		this.writer = writer;
	}
		
	/**
	 * This method assumes "len" is a multiplication of scan line length.
	 * 
	 * @param len the number of pixels to be encoded
	 */
	public void encode(byte[] pixels, int start, int len) throws Exception {
		//
		int numOfScanLines = len/scanLineWidth;
				
		for(int i = 0; i < numOfScanLines; i++) {
			start = encode1DLine(pixels, start);
		}			
	}
	
	protected int encode1DLine(byte[] pixels, int start) throws Exception {
		List<Integer> runLen = new ArrayList<Integer>();
		List<Integer> bits = new ArrayList<Integer>();
		int l = 0;
		int offset = 0; // Offset within the scan line
			
		int k = ((pixels[start]>>>currPos)&0x01);
	
		int i = start;
		
		while(currPos >= 0) {
			if(((pixels[i]>>>currPos)&0x01)==k) {
				l++;					
			} else {
				runLen.add(l);
				bits.add(k);
				k = ((pixels[i]>>>currPos)&0x01);
				l = 1;
			}
			
			offset++;
			currPos--;
			
			if(currPos < 0 ) {
				currPos = 7;
				i++;
			}				
			
			if(offset >= scanLineWidth) { // End of line
				break;
			}
		}
			
		// Count in the last run length
		runLen.add(l);
		bits.add(k);
		
		int[] runs = CollectionUtils.integerListToIntArray(runLen);
		int[] bitType = CollectionUtils.integerListToIntArray(bits);
		
		for(int m = 0; m <bitType.length; m++) {
			//
			int len = runs[m];
			
			if(bitType[m]==0) {
				// White code
				outputRunLengthCode(len, 0);
			} else {
				// Scan line starts with black code, send a zero run-length white code first
				if(m == 0)
					send_code_to_buffer((short)0x3500, 8);
				// Send black run length after
				outputRunLengthCode(len, 1);
			}
		}
		
		flush_buf(bufIndex+1);
		empty_bits = 8;
		
		return i;
	}
	
	// Borrowed from itext with some changes
	protected void outputRunLengthCode(int len, int color) throws Exception {
		int index;
		T4Code code;
		// Find out and output the code given the run length and color		
		if (len >= 2624) {
			// Find the index for the runLenArray
			index = 64 + (2560>>6);
			// Find the code given the runLenArray index
			code = (color == 0)?T4WhiteCode.fromRunLen(runLenArray[index]):T4BlackCode.fromRunLen(runLenArray[index]);
			short codeValue = code.getCode();
			int codeLen = code.getCodeLen();
			int currRunLen = code.getRunLen();
			// Out put the code
			while(len >= 2624) {
				send_code_to_buffer(codeValue, codeLen);
				len -= currRunLen;
			}
		}		
		while(len >= 64) {
			index = 64 + (len>>6);
			code = (color == 0)?T4WhiteCode.fromRunLen(runLenArray[index]):T4BlackCode.fromRunLen(runLenArray[index]);
			send_code_to_buffer(code.getCode(), code.getCodeLen());
			len -= code.getRunLen();
		}		
		code = (color == 0)?T4WhiteCode.fromRunLen(len):T4BlackCode.fromRunLen(len);
		send_code_to_buffer(code.getCode(), code.getCodeLen());
	}
	
	protected void outputRunLengthCode2(int len, int color) throws Exception {
		// This alternative code is shorter and neater but it turns out we don't really need the search
		int index = ArrayUtils.findEqualOrLess(runLenArray, len);
	
		// Send codes
		while(index > 0) {
			T4Code code = (color == 0)?T4WhiteCode.fromRunLen(runLenArray[index]):T4BlackCode.fromRunLen(runLenArray[index]);
			short codeValue = code.getCode();
			int codeLen = code.getCodeLen();
			int currRunLen = code.getRunLen();
			// Writes out as many codes as possible
			while(len >= currRunLen) {
				send_code_to_buffer(codeValue, codeLen);
				len -= currRunLen;
			}
			// Try to find smaller codes
			index = ArrayUtils.findEqualOrLess(runLenArray, 0, index, len);
		}		
	}

	public void finish() throws Exception {
		//
		if(extraFlush) {
			flush_buf(bufIndex + 1);
		}
		
		writer.update(compressedDataLen);		
	}

	// Flush the buffer as needed
   	private void flush_buf(int len) throws Exception {	
   		os.write(bytes_buf,0,len);		
   		// Reset the bytes buffer index
   		bufIndex = 0;
   		Arrays.fill(bytes_buf, 0, len, (byte)0x00);
   		
   		compressedDataLen += len;   		
   	}
   	
   	public int getCompressedDataLen() {
   		return compressedDataLen;
   	}
   	
 	public void initialize() throws Exception {
		empty_bits = 0x08;
		compressedDataLen = 0;
	}
    
    // Translate codes into bytes
    protected void send_code_to_buffer(int code, int codeLen)throws Exception {
    	if(empty_bits == 0) {
    		if (++bufIndex >= buf_length)
				flush_buf(buf_length);
    		empty_bits = 8;
    	}
    	// If we have enough space for the code
    	if(codeLen <= empty_bits) {
    		int shift = 16 - codeLen;   		
    		bytes_buf[bufIndex] |= (((code >>> shift)&mask[codeLen])<<(empty_bits - codeLen));
    		empty_bits -= codeLen;    		
    	} else {// Otherwise    		
			bytes_buf[bufIndex] |= ((code>>>(16-empty_bits))&mask[empty_bits]);
						
			int temp = codeLen - empty_bits;
			
			if(temp > 8) {	
				if (++bufIndex >= buf_length)
					flush_buf(buf_length);
			
				bytes_buf[bufIndex] |= ((code>>>(8 - codeLen + temp))&mask[8]);
				temp -= 8;
			}
			
			if(temp > 0) {
				if (++bufIndex >= buf_length)
					flush_buf(buf_length);
			
				bytes_buf[bufIndex] |= (((code>>>(16 - codeLen))&mask[temp])<<(8 - temp));
				temp -= 8;
			}
			
			empty_bits = -temp;
    	}
    }
   	
   	protected void setExtraFlush(boolean extraFlush) {
   		this.extraFlush = extraFlush;
   	}
   	
	protected int scanLineWidth;
   	protected int currPos = 7;
}
