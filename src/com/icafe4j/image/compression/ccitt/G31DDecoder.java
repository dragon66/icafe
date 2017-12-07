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
	
	public G31DDecoder(byte[] input, int scanLineWidth) {
		this.input = input;
		this.scanLineWidth = scanLineWidth;
		reset(input.length, 0, 7);
	}

	public int decode(byte[] pix, int offset, int len) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}
	
//	private decode1DLine() {
//	    isWhite = true;
//	    int n = 0;
//	    while (n < rowSize) {
//	        int runLength = decodeRunLength();
//	        if (runLength < 0) return false;
//	        n += runLength;
//	        setNextBits(isWhite ? whiteValue : blackValue, runLength);
//	        isWhite = !isWhite;
//	    }
//	    return true;
//	}
//
//	private int decodeRunLength() {
//	    int runLength = 0;
//	    int partialRun = 0;
//	    short[][][] huffmanCode = isWhite ? WHITE_CODE : BLACK_CODE;
//	    while (true) {
//	        bool found = false;
//	        nbrBits = isWhite ? WHITE_MIN_BITS : BLACK_MIN_BITS;
//	        code = getNextBits(nbrBits);
//	        for (int i = 0; i < huffmanCode.length; i++) {
//	            for (int j = 0; j < huffmanCode[i].length; j++) {
//	                if (huffmanCode[i][j][0] is code) {
//	                    found = true;
//	                    partialRun = huffmanCode[i][j][1];
//	                    if (partialRun is -1) {
//	                        /* Stop when reaching final EOL on last byte */
//	                        if (byteOffsetSrc is src.length - 1) return -1;
//	                        /* Group 3 starts each row with an EOL - ignore it */
//	                    } else {
//	                        runLength += partialRun;
//	                        if (partialRun < 64) return runLength;
//	                    }
//	                    break;
//	                }
//	            }
//	            if (found) break;
//	            code = code << 1 | getNextBit();
//	        }
//	        if (!found) DWT.error(DWT.ERROR_INVALID_IMAGE);
//	    }
//	}
//
//	private int getNextBit() {
//	    int value = (src[byteOffsetSrc] >>> (7 - bitOffsetSrc)) & 0x1;
//	    bitOffsetSrc++;
//	    if (bitOffsetSrc > 7) {
//	        byteOffsetSrc++;
//	        bitOffsetSrc = 0;
//	    }
//	    return value;
//	}
//	
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
	
//	void setNextBits(int value, int cnt) {
//	    int n = cnt;
//	    while (bitOffsetDest > 0 && bitOffsetDest <= 7 && n > 0) {
//	        dest[byteOffsetDest] = value is 1 ?
//	            cast(byte)(dest[byteOffsetDest] | (1 << (7 - bitOffsetDest))) :
//	            cast(byte)(dest[byteOffsetDest] & ~(1 << (7 - bitOffsetDest)));
//	        n--;
//	        bitOffsetDest++;
//	    }
//	    if (bitOffsetDest is 8) {
//	        byteOffsetDest++;
//	        bitOffsetDest = 0;
//	    }
//	    while (n >= 8) {
//	        dest[byteOffsetDest++] = cast(byte) (value is 1 ? 0xFF : 0);
//	        n -= 8;
//	    }
//	    while (n > 0) {
//	        dest[byteOffsetDest] = value is 1 ?
//	            cast(byte)(dest[byteOffsetDest] | (1 << (7 - bitOffsetDest))) :
//	            cast(byte)(dest[byteOffsetDest] & ~(1 << (7 - bitOffsetDest)));
//	        n--;
//	        bitOffsetDest++;
//	    }
//	}
	
	// This is to be moved to G31DDecoder
	public static byte[] decodeCode(byte[] bytes, int imageWidth, int imageHeight) {
			T4CodeHuffmanTreeNode blackNodes = T4BlackCodeHuffmanTreeNode.getInstance();
			T4CodeHuffmanTreeNode whiteNodes = T4WhiteCodeHuffmanTreeNode.getInstance();
			T4CodeHuffmanTreeNode currNode = whiteNodes;
			int totalBytes = imageWidth*imageHeight;
			byte[] result = new byte[totalBytes];
			int i = 0;
			int j = 0;
			int off = 7;
			byte cur = bytes[i];

			boolean isWhiteCode = true;
			int runLen = 0;
			int remaining = imageWidth;
			
			while(true) {
				if(((cur>>off) & 0x01) == 0) {
					if(currNode.left() != null) {
						//System.out.println("0");
						currNode = currNode.left();
						off--;
						if(off < 0) {
							off = 7;
							i++;
							if(i >= bytes.length ) break;
							cur = bytes[i];				
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
									result[j++] = 0;
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
									result[j++] = 1;
								}
								currNode = whiteNodes;
								isWhiteCode = true;
							}
							if(remaining == 0) {
								remaining = imageWidth;
								if(off != 7) {
								i++;
								off = 7;
								if(i >= bytes.length ) break;
								cur = bytes[i];}			
							}
							runLen = 0;
						} else {
							if(isWhiteCode) currNode = whiteNodes;
							else currNode = blackNodes;
						}
					}
				} else if(((cur>>off) & 0x01) == 1) {
					if(currNode.right() != null) {
						currNode = currNode.right();
						off--;
						if(off < 0) {
							off = 7;
							i++;
							if(i >= bytes.length ) break;
							cur = bytes[i];				
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
									result[j++] = 0;
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
									result[j++] = 1;
								}
								currNode = whiteNodes;
								isWhiteCode = true;
							}
							if(remaining == 0) {
								remaining = imageWidth;
								if(off != 7) {
									i++;
									off = 7;
									if(i >= bytes.length ) break;
									cur = bytes[i];}					
							}
							runLen = 0;
						} else {
							if(isWhiteCode) currNode = whiteNodes;
							else currNode = blackNodes;
						}
					}
				}
			
			}
			
			return ArrayUtils.packByteArray(result, imageWidth, 0, 1, result.length);
		}
	
	private int len;
	private int scanLineWidth;
	private int byteOffset = 0;
	private int bitOffset = 7;
}