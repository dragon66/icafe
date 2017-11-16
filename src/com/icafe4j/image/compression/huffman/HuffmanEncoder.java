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

package com.icafe4j.image.compression.huffman;

import java.io.OutputStream;
import java.util.Arrays;

import com.icafe4j.image.compression.ImageEncoder;
import com.icafe4j.image.jpeg.JPEGConsts;

/**
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/07/2014
 */
public class HuffmanEncoder implements ImageEncoder {
	// Fields
	private int empty_bits;
	private byte[] bytes_buf;
		
	private int buf_length;	
	private int bufIndex;
	
	private int[][] DC_EHUFCO = new int[4][];
	private int[][] DC_EHUFSI = new int[4][];
	private int[][] AC_EHUFCO = new int[4][];
	private int[][] AC_EHUFSI = new int[4][];
	
	private int[] PREDICTION = new int[4];
	
	private boolean extraFlush = true;
	
	private boolean useCustomTables = false; 
	
	private OutputStream os;
	
	private static final short mask[] = {
		0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff, 0x01ff, 0x03ff, 0x07ff, 0x0fff, 0x1fff
	};
	
	private static final int[] ZIGZAG_TRAVERSE_ORDER = JPEGConsts.getZigzagMatrix();
	
	public HuffmanEncoder(OutputStream os, int buf_length) {
		this.os = os;
		bytes_buf = new byte[buf_length];
		this.buf_length = buf_length;
	}
	
	private void createDefaultEncodingTables() {
		// Create default Huffman tables
		HuffmanTbl huffTbl = new HuffmanTbl();
		// DC first
		huffTbl.setBits(JPEGConsts.getDCLuminanceBits());
		huffTbl.setValues(JPEGConsts.getDCLuminanceValues());
		huffTbl.generateEncoderTables();
		DC_EHUFCO[0] = huffTbl.getEncoderCodeTable();
		DC_EHUFSI[0] = huffTbl.getEncoderSizeTable();
		huffTbl.setBits(JPEGConsts.getDCChrominanceBits());
		huffTbl.setValues(JPEGConsts.getDCChrominanceValues());
		huffTbl.generateEncoderTables();
		DC_EHUFCO[1] = huffTbl.getEncoderCodeTable();
		DC_EHUFSI[1] = huffTbl.getEncoderSizeTable();
		DC_EHUFCO[2] = huffTbl.getEncoderCodeTable();
		DC_EHUFSI[2] = huffTbl.getEncoderSizeTable();
		DC_EHUFCO[3] = huffTbl.getEncoderCodeTable();
		DC_EHUFSI[3] = huffTbl.getEncoderSizeTable();
		// Then AC
		huffTbl.setBits(JPEGConsts.getACLuminanceBits());
		huffTbl.setValues(JPEGConsts.getACLuminanceValues());
		huffTbl.generateEncoderTables();
		AC_EHUFCO[0] = huffTbl.getEncoderCodeTable();
		AC_EHUFSI[0] = huffTbl.getEncoderSizeTable();
		huffTbl.setBits(JPEGConsts.getACChrominanceBits());
		huffTbl.setValues(JPEGConsts.getACChrominanceValues());
		huffTbl.generateEncoderTables();
		AC_EHUFCO[1] = huffTbl.getEncoderCodeTable();
		AC_EHUFSI[1] = huffTbl.getEncoderSizeTable();
		AC_EHUFCO[2] = huffTbl.getEncoderCodeTable();
		AC_EHUFSI[2] = huffTbl.getEncoderSizeTable();
		AC_EHUFCO[3] = huffTbl.getEncoderCodeTable();
		AC_EHUFSI[3] = huffTbl.getEncoderSizeTable();
	}
	
	public void encode(byte[] pixels, int start, int len) throws Exception { 
		throw new UnsupportedOperationException("Call encode(int[] ZZ, int component_id) instead.");
	}
	
	public void encode(int[] ZZ, int component_id) throws Exception {
		int temp, ssss, k, r = 0, rs;       
		// The AC part
		temp = ZZ[0] - PREDICTION[component_id];
        ssss = CSIZE(temp);
        
        if(temp < 0) {
        	temp--;
        }        
        
        send_code_to_buffer(DC_EHUFCO[component_id][ssss], DC_EHUFSI[component_id][ssss]);
  
        if (ssss != 0) {
        	send_code_to_buffer(temp, ssss);
        }
        
        PREDICTION[component_id] = ZZ[0];
        // The AC coefficients
        for(k = 1; k < 64; k++) {
			if ((temp = ZZ[ZIGZAG_TRAVERSE_ORDER[k]]) == 0) {
				r++;
			} else {
				while (r > 15) {
					send_code_to_buffer(AC_EHUFCO[component_id][0xF0], AC_EHUFSI[component_id][0xF0]);
					r -= 16;
				}
				ssss = CSIZE(temp);
				if (temp < 0) {
					temp--;
				}				
				rs = (r << 4) + ssss;
				send_code_to_buffer(AC_EHUFCO[component_id][rs], AC_EHUFSI[component_id][rs]);
				send_code_to_buffer(temp, ssss);
				r = 0;
			}
		}

		if (r > 0) {
			send_code_to_buffer(AC_EHUFCO[component_id][0], AC_EHUFSI[component_id][0]);
		}
	}
	
	public void finish() throws Exception {
		// Cleanup
		if(extraFlush) {
			flush_buf(bufIndex + 1);
		}		
	}
	
   	// Flush the buffer as needed
   	private void flush_buf(int len) throws Exception {   		
   		os.write(bytes_buf, 0, len);		
   		// Reset the bytes buffer index
   		bufIndex = 0;
   		Arrays.fill(bytes_buf, 0, len, (byte)0x00);
   		
   		totalBytes += len;
   	}
   	
   	// For book keeping purpose
   	public int getCompressedDataLen() {
   		return totalBytes;
   	}
	
	public void initialize() {
		if(!useCustomTables)
			createDefaultEncodingTables();
   		empty_bits = 0x08;
   		totalBytes = 0;
   	}
	
	// Translate codes into bytes
    private void send_code_to_buffer(int code, int codeLen)throws Exception {
    	if(empty_bits == 0) {
    		if (++bufIndex >= buf_length)
				flush_buf(buf_length);
    		empty_bits = 8;
    	}
    	// If we have enough space for the code
    	if(codeLen < empty_bits) {
    		bytes_buf[bufIndex] |= ((code&mask[codeLen])<<(empty_bits - codeLen));
    		empty_bits -= codeLen;    		
    	} else {// Otherwise    		
			bytes_buf[bufIndex] |= ((code>>>(codeLen-empty_bits))&mask[empty_bits]);
			
			int temp = codeLen - empty_bits;
			
			if((bytes_buf[bufIndex]&0xFF)==0xFF) {
				if (++bufIndex >= buf_length)
					flush_buf(buf_length);
				bytes_buf[bufIndex] |= 0x00;
			}
			
			if(temp >= 8) {	
				if (++bufIndex >= buf_length)
					flush_buf(buf_length);
			
				bytes_buf[bufIndex] |= ((code>>>(temp - 8))&mask[8]);
				temp -= 8;
				
				if((bytes_buf[bufIndex]&0xFF)==0xFF) {
					if (++bufIndex >= buf_length)
						flush_buf(buf_length);
					bytes_buf[bufIndex] |= 0x00;
				}
			}
			
			if(temp > 0) {
				if (++bufIndex >= buf_length)
					flush_buf(buf_length);
			
				bytes_buf[bufIndex] |= ((code&mask[temp])<<(8 - temp));
				temp -= 8;
			}
			
			empty_bits = -temp;
    	}
	}

    // Set custom encoding tables
	public void setEncodingTables(int[][] DC_EHUFCO, int[][] DC_EHUFSI, int[][] AC_EHUFCO, int[][] AC_EHUFSI) {
		this.DC_EHUFCO = DC_EHUFCO;
		this.DC_EHUFSI = DC_EHUFSI;
		this.AC_EHUFCO = AC_EHUFCO;
		this.AC_EHUFSI = AC_EHUFSI;
		// Let the encoder know we are using custom tables
		useCustomTables = true;
	}
	
	// Mapping SSSS or ZZ_K value to code size
	private static int CSIZE(int ZZ_K) {
		if(ZZ_K == 0)
			return 0;
		else if(ZZ_K >= -1 && ZZ_K <= 1)
			return 1;
		else if(ZZ_K >= -3 && ZZ_K <= 3)
			return 2;
		else if(ZZ_K >= -7 && ZZ_K <= 7)
			return 3;
		else if(ZZ_K >= -15 && ZZ_K <= 15)
			return 4;
		else if(ZZ_K >= -31 && ZZ_K <= 31)
			return 5;
		else if(ZZ_K >= -63 && ZZ_K <= 63)
			return 6;
		else if(ZZ_K >= -127 && ZZ_K <= 127)
			return 7;
		else if(ZZ_K >= -255 && ZZ_K <= 255)
			return 8;
		else if(ZZ_K >= -511 && ZZ_K <= 511)
			return 9;
		else if(ZZ_K >= -1023 && ZZ_K <= 1023)
			return 10;
		else if(ZZ_K >= -2047 && ZZ_K <= 2047)
			return 11;
		else
			throw new RuntimeException("Invalid ZZ_K value: " + ZZ_K);		
	}
	
	int totalBytes = 0;
}