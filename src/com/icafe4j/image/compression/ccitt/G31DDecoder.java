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

package com.icafe4j.image.compression.ccitt;

import com.icafe4j.image.compression.ImageDecoder;
import com.icafe4j.image.compression.huffman.T4BlackCodeHuffmanTreeNode;
import com.icafe4j.image.compression.huffman.T4CodeHuffmanTreeNode;
import com.icafe4j.image.compression.huffman.T4WhiteCodeHuffmanTreeNode;

public class G31DDecoder implements ImageDecoder {
	
	private int empty_bits = 8;
	private int lineOffset = 0;
	
	public G31DDecoder(int scanLineWidth, int rowsPerStrip) {
		this.scanLineWidth = scanLineWidth;
		this.rowsPerStrip = rowsPerStrip;
	}
	
	public G31DDecoder(byte[] input, int scanLineWidth, int rowsPerStrip) {
		this.input = input;
		this.scanLineWidth = scanLineWidth;
		this.rowsPerStrip = rowsPerStrip;
		reset(0, input.length, 7);
	}

	public int decode(byte[] pix, int offset, int len) throws Exception {
		T4CodeHuffmanTreeNode blackNodes = T4BlackCodeHuffmanTreeNode.getInstance();
		T4CodeHuffmanTreeNode whiteNodes = T4WhiteCodeHuffmanTreeNode.getInstance();
		T4CodeHuffmanTreeNode currNode = whiteNodes;
		byte cur = input[byteOffset];
		int endOffset = byteOffset + this.len;
		destByteOffset = offset;
		
		int runLen = 0;
		int remaining = scanLineWidth;
		boolean isWhiteCode = true;
		
		while(true) {
			if(((cur>>bitOffset) & 0x01) == 0) {
				if(currNode.left() != null) {
					currNode = currNode.left();
					bitOffset--;
					if(bitOffset < 0) {
						bitOffset = 7;
						byteOffset++;
						if(byteOffset >= endOffset) break;
						cur = input[byteOffset];				
					}
				} else {
					runLen += currNode.value();			
					if(currNode.value() <= 63) {
						//output runLen
						if(runLen > remaining)
							runLen = remaining;
						remaining -= runLen;
						// Check code
						if(isWhiteCode) {
							destByteOffset = outputRunLen(pix, destByteOffset, runLen,  scanLineWidth, 0, len);
							if(remaining != 0) {
								currNode = blackNodes;
								isWhiteCode = false;
							} else {
								currNode = whiteNodes;
								isWhiteCode = true;
							}
						} else {
							destByteOffset = outputRunLen(pix, destByteOffset, runLen,  scanLineWidth, 1, len);
							currNode = whiteNodes;
							isWhiteCode = true;
						}
						if(remaining == 0) {
							remaining = scanLineWidth;
							if(bitOffset != 7) {
								byteOffset++;
								bitOffset = 7;
								if(byteOffset >= endOffset) break;
								cur = input[byteOffset];								
							}			
						}
						runLen = 0;
					} else {
						if(isWhiteCode) currNode = whiteNodes;
						else currNode = blackNodes;
					}
				}
			} else if(((cur>>bitOffset) & 0x01) == 1) {
				if(currNode.right() != null) {
					currNode = currNode.right();
					bitOffset--;
					if(bitOffset < 0) {
						bitOffset = 7;
						byteOffset++;
						if(byteOffset >= endOffset) break;
						cur = input[byteOffset];				
					}
				} else {
					runLen += currNode.value();
					if(currNode.value() <= 63) {
						//output runLen
						if(runLen > remaining)
							runLen = remaining;
						remaining -= runLen;
						// Check code
						if(isWhiteCode) {
							destByteOffset = outputRunLen(pix, destByteOffset, runLen,  scanLineWidth, 0, len);
							if(remaining != 0) {
								currNode = blackNodes;
								isWhiteCode = false;
							} else {
								currNode = whiteNodes;
								isWhiteCode = true;
							}
						} else {
							destByteOffset = outputRunLen(pix, destByteOffset, runLen,  scanLineWidth, 1, len);
							currNode = whiteNodes;
							isWhiteCode = true;
						}
						if(remaining == 0) {
							remaining = scanLineWidth;
							if(bitOffset != 7) {
								byteOffset++;
								bitOffset = 7;
								if(byteOffset >= endOffset) break;
								cur = input[byteOffset];
							}					
						}
						runLen = 0;
					} else {
						if(isWhiteCode) currNode = whiteNodes;
						else currNode = blackNodes;
					}
				}
			}		
		}
		
		if(totalRunLen < (scanLineWidth*rowsPerStrip))
			destByteOffset = outputRunLen(pix, destByteOffset, scanLineWidth*rowsPerStrip - totalRunLen, scanLineWidth, 0, len);
		
		return uncompressedBytes; 
	}
	
	protected int outputRunLen(byte[] output, int offset, int runLen,  int stride, int color, int len) {
		int i = 0;
		
		for(i = 0; i < runLen; i++) {
			if(uncompressedBytes >= len) break;			
			if(empty_bits >= 1) {
				output[offset] |= (color<<(empty_bits-1));
				empty_bits--;
			} 
			// Check to see if we need to move to next byte
			if(++lineOffset%stride == 0 || empty_bits == 0) {
				offset++;
				uncompressedBytes++;
				empty_bits = 8;
			}
		}
		
		totalRunLen += i;
		
		return offset;
	}
	
	private void reset(int byteOffset, int len, int bitOffset) {
		this.byteOffset = byteOffset;
		this.len = len;
		this.bitOffset = bitOffset;
		this.uncompressedBytes = 0;
		this.totalRunLen = 0;
	}

	public void setInput(byte[] input) {
		setInput(input, 0, input.length);
	}

	public void setInput(byte[] input, int offset, int len) {
		this.input = input;
		reset(offset, len, 7);	
	}
	
	protected byte[] input;
	protected int len;
	protected int scanLineWidth;
	protected int totalRunLen;
	protected int rowsPerStrip;
	protected int byteOffset;
	protected int bitOffset = 7;
	protected int destByteOffset;
	protected int uncompressedBytes;
}