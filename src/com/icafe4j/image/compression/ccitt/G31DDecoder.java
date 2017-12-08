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
import com.icafe4j.util.ArrayUtils;

public class G31DDecoder implements ImageDecoder {

	private byte[] input;
	
	public G31DDecoder(int scanLineWidth) {
		this(null, scanLineWidth);
	}
	
	public G31DDecoder(byte[] input, int scanLineWidth) {
		this.input = input;
		this.scanLineWidth = scanLineWidth;
		reset((input == null)? 0: input.length, 0, 7);
	}

	public int decode(byte[] pix, int offset, int len) throws Exception {
		T4CodeHuffmanTreeNode blackNodes = T4BlackCodeHuffmanTreeNode.getInstance();
		T4CodeHuffmanTreeNode whiteNodes = T4WhiteCodeHuffmanTreeNode.getInstance();
		T4CodeHuffmanTreeNode currNode = whiteNodes;
		int totalBytes = scanLineWidth; // Will use len later
		byte[] result = new byte[totalBytes];
		byte cur = input[byteOffset];
		int lineOffset = 0;
		int count = 0;
		
		boolean isWhiteCode = true;
		int runLen = 0;
		int remaining = scanLineWidth;
		
		while(true) {
			if(((cur>>bitOffset) & 0x01) == 0) {
				if(currNode.left() != null) {
					//System.out.println("0");
					currNode = currNode.left();
					bitOffset--;
					if(bitOffset < 0) {
						bitOffset = 7;
						byteOffset++;
						if(byteOffset >= input.length ) break;
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
							for(int k = 0; k < runLen; k++) {
								result[lineOffset++] = 0;
							}
							if(remaining != 0) {
								currNode = blackNodes;
								isWhiteCode = false;
							} else {
								currNode = whiteNodes;
								isWhiteCode = true;
							}
						}
						else {
							for(int k = 0; k < runLen; k++) {
								result[lineOffset++] = 1;
							}
							currNode = whiteNodes;
							isWhiteCode = true;
						}
						if(remaining == 0) {
							remaining = scanLineWidth;
							result =  ArrayUtils.packByteArray(result, scanLineWidth, 0, 1, result.length);
							System.arraycopy(result, 0, pix, offset, result.length);
							offset += result.length;
							count += result.length;
							result = new byte[scanLineWidth];
							lineOffset = 0;
							if(bitOffset != 7) {
								byteOffset++;
								bitOffset = 7;
								if(byteOffset >= input.length ) break;
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
						if(byteOffset >= input.length ) break;
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
							for(int k = 0; k < runLen; k++) {
								result[lineOffset++] = 0;
							} 
							if(remaining != 0) {
								currNode = blackNodes;
								isWhiteCode = false;
							} else {
								currNode = whiteNodes;
								isWhiteCode = true;
							}
						}
						else {
							for(int k = 0; k < runLen; k++) {
								result[lineOffset++] = 1;
							}
							currNode = whiteNodes;
							isWhiteCode = true;
						}
						if(remaining == 0) {
							remaining = scanLineWidth;
							result =  ArrayUtils.packByteArray(result, scanLineWidth, 0, 1, result.length);
							System.arraycopy(result, 0, pix, offset, result.length);
							offset += result.length;
							count += result.length;
							result = new byte[scanLineWidth];
							lineOffset = 0;
							if(bitOffset != 7) {
								byteOffset++;
								bitOffset = 7;
								if(byteOffset >= input.length ) break;
								cur = input[byteOffset];}					
						}
						runLen = 0;
					} else {
						if(isWhiteCode) currNode = whiteNodes;
						else currNode = blackNodes;
					}
				}
			}		
		}		
		
		return count; 
	}
	
	private void reset(int len, int byteOffset, int bitOffset) {
		this.len = len;
		this.byteOffset = byteOffset;
		this.bitOffset = bitOffset;
	}

	public void setInput(byte[] input) {
		setInput(input, 0, input.length);
	}

	public void setInput(byte[] input, int offset, int len) {
		this.input = input;
		reset(len, offset, 7);	
	}
	
	private int len; // Will be used later by decode
	private int scanLineWidth;
	private int byteOffset = 0;
	private int bitOffset = 7;
}