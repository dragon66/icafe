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

public class G32DDecoder extends G31DDecoder implements ImageDecoder {
	
	private boolean is1DEncoding;
	
	public G32DDecoder(int scanLineWidth, int rowsPerStrip) {
		this(scanLineWidth, rowsPerStrip, false);
	}
	
	public G32DDecoder(int scanLineWidth, int rowsPerStrip, boolean is1DEncoding) {
		super(scanLineWidth, rowsPerStrip);
		this.is1DEncoding = is1DEncoding;
	}
	
	public G32DDecoder(byte[] input, int scanLineWidth, int rowsPerStrip) {
		this(input, scanLineWidth, rowsPerStrip, false);
	}
	
	public G32DDecoder(byte[] input, int scanLineWidth, int rowsPerStrip, boolean is1DEncoding) {
		super(input, scanLineWidth, rowsPerStrip);
		this.is1DEncoding = is1DEncoding;
	}
	
	public int decode(byte[] pix, int offset, int len) throws Exception {
		if(is1DEncoding)
			return decode1D(pix, offset, len);
		return decode2D(pix, offset, len);
	}

	private int decode1D(byte[] pix, int offset, int len) throws Exception {
		T4CodeHuffmanTreeNode blackNodes = T4BlackCodeHuffmanTreeNode.getInstance();
		T4CodeHuffmanTreeNode whiteNodes = T4WhiteCodeHuffmanTreeNode.getInstance();
		T4CodeHuffmanTreeNode currNode = whiteNodes;
		byte cur = input[byteOffset];
		int endOffset = byteOffset + this.len;
		destByteOffset = offset;
		
		int runLen = 0;
		int remaining = scanLineWidth;
		boolean isWhiteCode = true;
		
		// Used to fix wrong byte alignment
		int markByteOffset = offset;
		int markBitOffset = 7;
		boolean markIsWhiteCode = true;
		T4CodeHuffmanTreeNode markCurrNode = whiteNodes;
		boolean expectEOL = true;
		
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
				} else if (currNode.value() >= 0) {					
					runLen += currNode.value();			
					if(currNode.value() <= 63) {
						if(expectEOL) {
							currNode = markCurrNode;
							byteOffset = markByteOffset;
							bitOffset = markBitOffset;
							isWhiteCode = markIsWhiteCode;
							if(bitOffset != 7) {
								byteOffset++;
								bitOffset = 7;
								if(byteOffset >= endOffset) break;
							}
							cur = input[byteOffset];
							runLen = 0;
							expectEOL = false;
							continue;
						}
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
							markByteOffset = byteOffset;
							markBitOffset = bitOffset;
							if(isWhiteCode) {
								markCurrNode = whiteNodes; 
								markIsWhiteCode = true;
							} else {
								markCurrNode = blackNodes;
								markIsWhiteCode = false;
							}
							expectEOL = true;
						}
						runLen = 0;
					} else {
						if(isWhiteCode) currNode = whiteNodes;
						else currNode = blackNodes;
					}
				} else {
					if(remaining != scanLineWidth) {
						destByteOffset = outputRunLen(pix, destByteOffset, remaining, scanLineWidth, 0, len);
						remaining = scanLineWidth;
					}
					expectEOL = false;
					isWhiteCode = true;
					currNode = whiteNodes;
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
				} else if (currNode.value() >= 0) {					
					runLen += currNode.value();
					if(currNode.value() <= 63) {
						if(expectEOL) {
							currNode = markCurrNode;
							isWhiteCode = markIsWhiteCode;
							byteOffset = markByteOffset;
							bitOffset = markBitOffset;
							if(bitOffset != 7) {
								byteOffset++;
								bitOffset = 7;
								if(byteOffset >= endOffset) break;
							}
							cur = input[byteOffset];
							runLen = 0;
							expectEOL = false;

							continue;
						}
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
							markByteOffset = byteOffset;
							markBitOffset = bitOffset;
							if(isWhiteCode) {
								markCurrNode = whiteNodes; 
								markIsWhiteCode = true;
							} else {
								markCurrNode = blackNodes;
								markIsWhiteCode = false;
							}
							expectEOL = true;
						}
						runLen = 0;
					} else {
						if(isWhiteCode) currNode = whiteNodes;
						else currNode = blackNodes;
					}
				} else {
					if(remaining != scanLineWidth) {
						destByteOffset = outputRunLen(pix, destByteOffset, remaining, scanLineWidth, 0, len);
						remaining = scanLineWidth;
					}
					expectEOL = false;
					isWhiteCode = true;
					currNode = whiteNodes;
				}
			}
		}		
		if(totalRunLen < (scanLineWidth*rowsPerStrip))
			destByteOffset = outputRunLen(pix, destByteOffset, scanLineWidth*rowsPerStrip - totalRunLen, scanLineWidth, 0, len);
		return uncompressedBytes; 
	}
	
	private int decode2D(byte[] pix, int offset, int len) throws Exception {
		T4CodeHuffmanTreeNode blackNodes = T4BlackCodeHuffmanTreeNode.getInstance();
		T4CodeHuffmanTreeNode whiteNodes = T4WhiteCodeHuffmanTreeNode.getInstance();
		T4CodeHuffmanTreeNode currNode = whiteNodes;
		byte cur = input[byteOffset];
		int endOffset = byteOffset + this.len;
		destByteOffset = offset;
		
		int runLen = 0;
		int remaining = scanLineWidth;
		boolean isWhiteCode = true;
		
		// Used to fix wrong byte alignment
		int markByteOffset = offset;
		int markBitOffset = 7;
		boolean markIsWhiteCode = true;
		T4CodeHuffmanTreeNode markCurrNode = whiteNodes;
		boolean expectEOL = true;
		
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
				} else if (currNode.value() >= 0) {					
					runLen += currNode.value();			
					if(currNode.value() <= 63) {
						if(expectEOL) {
							currNode = markCurrNode;
							byteOffset = markByteOffset;
							bitOffset = markBitOffset;
							isWhiteCode = markIsWhiteCode;
							if(bitOffset != 7) {
								byteOffset++;
								bitOffset = 7;
								if(byteOffset >= endOffset) break;
							}
							cur = input[byteOffset];
							runLen = 0;
							expectEOL = false;
							continue;
						}
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
							markByteOffset = byteOffset;
							markBitOffset = bitOffset;
							if(isWhiteCode) {
								markCurrNode = whiteNodes; 
								markIsWhiteCode = true;
							} else {
								markCurrNode = blackNodes;
								markIsWhiteCode = false;
							}
							expectEOL = true;
						}
						runLen = 0;
					} else {
						if(isWhiteCode) currNode = whiteNodes;
						else currNode = blackNodes;
					}
				} else {
					if(remaining != scanLineWidth) {
						destByteOffset = outputRunLen(pix, destByteOffset, remaining, scanLineWidth, 0, len);
						remaining = scanLineWidth;
					}
					expectEOL = false;
					isWhiteCode = true;
					currNode = whiteNodes;
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
				} else if (currNode.value() >= 0) {					
					runLen += currNode.value();
					if(currNode.value() <= 63) {
						if(expectEOL) {
							currNode = markCurrNode;
							isWhiteCode = markIsWhiteCode;
							byteOffset = markByteOffset;
							bitOffset = markBitOffset;
							if(bitOffset != 7) {
								byteOffset++;
								bitOffset = 7;
								if(byteOffset >= endOffset) break;
							}
							cur = input[byteOffset];
							runLen = 0;
							expectEOL = false;

							continue;
						}
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
							markByteOffset = byteOffset;
							markBitOffset = bitOffset;
							if(isWhiteCode) {
								markCurrNode = whiteNodes; 
								markIsWhiteCode = true;
							} else {
								markCurrNode = blackNodes;
								markIsWhiteCode = false;
							}
							expectEOL = true;
						}
						runLen = 0;
					} else {
						if(isWhiteCode) currNode = whiteNodes;
						else currNode = blackNodes;
					}
				} else {
					if(remaining != scanLineWidth) {
						destByteOffset = outputRunLen(pix, destByteOffset, remaining, scanLineWidth, 0, len);
						remaining = scanLineWidth;
					}
					expectEOL = false;
					isWhiteCode = true;
					currNode = whiteNodes;
				}
			}
		}		
		if(totalRunLen < (scanLineWidth*rowsPerStrip))
			destByteOffset = outputRunLen(pix, destByteOffset, scanLineWidth*rowsPerStrip - totalRunLen, scanLineWidth, 0, len);
		return uncompressedBytes; 
	}
}