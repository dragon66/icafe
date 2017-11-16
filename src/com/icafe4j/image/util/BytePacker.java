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

package com.icafe4j.image.util;

/** 
 * Pack byte array one byte at a time (alternative to ArrayUtils.packByteArray()).
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/21/2014
 */
public class BytePacker {
	// Short mask
	private static final short mask[] = {0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff};
	// Field variables
	private int bits;
	private int stride;
	int index;	
	private byte[] packedBytes;
	int empty_bits;
	int strideCounter = 0;
	
	/**
	 * @param bits number of bit for a single pixel
	 * @param stride scan line stride
	 * @param len total number of byte to pack
	 */
	public BytePacker(int bits, int stride, int len) {
		reset(bits, stride, len);
	}
	
	// Get the packed byte array
	public byte[] getPackedBytes() {
		return packedBytes;
	}

	public void packByte(int abyte) {		
		// If we have enough space for input byte, one step operation
		if(empty_bits >= bits) {
			packedBytes[index] |= ((abyte&mask[bits])<<(empty_bits-bits));
			empty_bits -= bits;
		} else { // Otherwise, split the pixel between two bytes.			
			//This will never happen for 1, 2, 4, 8 bits color depth image
			packedBytes[index++] |= ((abyte>>(bits-empty_bits))&mask[empty_bits]);
			packedBytes[index] |= ((abyte&mask[bits-empty_bits])<<(8-bits+empty_bits));
			empty_bits += (8-bits);
		}
		
		if(++strideCounter%stride == 0 || empty_bits == 0) {
			index++;
			empty_bits = 8;			
		}
	}
	
	// Reset BytePacker
	public void reset(int bits, int stride, int len) {
		if(bits >= 8 || bits <= 0)
			throw new IllegalArgumentException("Invalid value of bits: " + bits);
		this.bits = bits;
		this.stride = stride;
		int bitsPerStride = bits*stride;
		int numOfStrides = len/stride;
		packedBytes = new byte[((bitsPerStride + 7)>>3)*numOfStrides];
		index = 0;	
		empty_bits = 8;
		strideCounter = 0;
	}
}