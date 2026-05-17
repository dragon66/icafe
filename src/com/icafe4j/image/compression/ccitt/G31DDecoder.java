/**
 * COPYRIGHT (C) 2014-2019 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.compression.ccitt;

import com.icafe4j.image.compression.huffman.T4BlackCodeHuffmanTreeNode;
import com.icafe4j.image.compression.huffman.T4CodeHuffmanTreeNode;
import com.icafe4j.image.compression.huffman.T4WhiteCodeHuffmanTreeNode;

/**
 * CCITT T.4 Group 3 1D (Modified Huffman) decoder.
 * * This implementation follows the architecture of OpenJDK and LibTIFF:
 * Separates EOL synchronization from Huffman decoding
 * Uses sliding window search for EOL markers
 * Handles malformed files gracefully
 * * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/02/2026
 */
public class G31DDecoder extends CCITTDecoder {
    // G31D-specific variables
    private boolean fillBits;
    private int endOffset;
    private boolean noEOLMode = false; // For robust EOL handling

    public G31DDecoder(int scanLineWidth, int rowsPerStrip) {
        this(scanLineWidth, rowsPerStrip, false);
    }

    public G31DDecoder(int scanLineWidth, int rowsPerStrip, boolean fillBits) {
        super(scanLineWidth, rowsPerStrip);
        this.fillBits = fillBits;
    }

    public G31DDecoder(byte[] input, int scanLineWidth, int rowsPerStrip) {
        this(input, scanLineWidth, rowsPerStrip, false);
    }

    public G31DDecoder(byte[] input, int scanLineWidth, int rowsPerStrip, boolean fillBits) {
        super(input, scanLineWidth, rowsPerStrip);
        this.fillBits = fillBits;
        this.endOffset = input.length;
    }

    @Override
    protected void reset(int byteOffset, int len, int bitOffset) {
        super.reset(byteOffset, len, bitOffset);
        this.endOffset = byteOffset + len; // Calculate once
        this.noEOLMode = false;
    }

    /**
     * Read next N bits without advancing pointers.
     * Returns -1 if insufficient data.
     */
    private int peekNBits(int n) {
        int tempByteOffset = byteOffset;
        int tempBitOffset = bitOffset;
        int result = 0;
        for (int i = 0; i < n; i++) {
            if (tempByteOffset >= endOffset) return -1;
            int bit = (input[tempByteOffset] >> tempBitOffset) & 0x01;
            result = (result << 1) | bit; // Build MSB first
            tempBitOffset--;
            if (tempBitOffset < 0) {
                tempBitOffset = 7;
                tempByteOffset++;
            }
        }
        return result;
    }

    /**
     * Advance bit pointer by N bits.
     */
    private void skipNBits(int n) {
        for (int i = 0; i < n; i++) {
            bitOffset--;
            if (bitOffset < 0) {
                bitOffset = 7;
                byteOffset++;
                if (byteOffset >= endOffset) break;
            }
        }
    }

    /**
     * Search for the next EOL marker using sliding window approach.
     * Based on OpenJDK's findNextLine() and LibTIFF's SYNC_EOL macro.
     * EOL = 12 bits: 000000000001 (11 zeros followed by 1 one)
     * * If fillBits is enabled, first aligns to byte boundary.
     * * Returns true if EOL found and consumed, false if EOF or not found.
     */
    private boolean findNextEOL() {
        if (noEOLMode) {
            // Don't search for EOL in malformed files
            return true;
        }

        // When fillBits is enabled, fill bits are added BEFORE the EOL
        // so that the EOL ENDS on a byte boundary. We don't skip to byte
        // boundary before searching, just search normally.
        int maxBitsToSearch = fillBits ? 512 : 2048;
        int bitsSearched = 0;
        // Sliding window search for 12-bit EOL pattern
        while (bitsSearched < maxBitsToSearch) {
            if (byteOffset >= endOffset) return false;
            int next12Bits = peekNBits(12);
            if (next12Bits < 0) return false;
            if (next12Bits == 1) {
                // Found EOL! Consume it
                skipNBits(12);
                return true;
            }
            // Not EOL, shift by 1 bit and retry
            skipNBits(1);
            bitsSearched++;
        }
        // EOL not found, switch to no-EOL mode for malformed files
        noEOLMode = true;
        return false;
    }

    /**
     * Decode a single scanline using Huffman codes.
     * Based on OpenJDK's decodeNextScanline() method.
     */
    private boolean decodeScanline(byte[] pix, int offset, int len) {
        T4CodeHuffmanTreeNode whiteNodes = T4WhiteCodeHuffmanTreeNode.getInstance();
        T4CodeHuffmanTreeNode blackNodes = T4BlackCodeHuffmanTreeNode.getInstance();
        T4CodeHuffmanTreeNode currNode = whiteNodes;
        int pixelsDecoded = 0; // Pixels decoded in this scanline
        boolean isWhite = true;
        int runLen = 0;

        // Decode until scanline is complete
        while (pixelsDecoded < scanLineWidth) {
            if (byteOffset >= endOffset) {
                // Premature end of data
                return false;
            }

            byte cur = input[byteOffset];
            int bit = (cur >> bitOffset) & 0x01;
            // Traverse Huffman tree
            T4CodeHuffmanTreeNode nextNode = (bit == 0) ? currNode.left() : currNode.right();
            if (nextNode != null) {
                // Continue traversing
                currNode = nextNode;
                bitOffset--;
                if (bitOffset < 0) {
                    bitOffset = 7;
                    byteOffset++;
                }
            } else if (currNode.value() >= 0) {
                // Leaf node got a code
                int code = currNode.value();
                runLen += code;
                if (code <= 63) {
                    // Terminating code, output the run
                    if (runLen > scanLineWidth - pixelsDecoded) {
                        runLen = scanLineWidth - pixelsDecoded;
                    }
                    if (isWhite) {
                        destByteOffset = outputRunLen(pix, destByteOffset, runLen, scanLineWidth, 0, len);
                    } else {
                        destByteOffset = outputRunLen(pix, destByteOffset, runLen, scanLineWidth, 1, len);
                    }
                    pixelsDecoded += runLen;
                    runLen = 0;
                    // Switch color
                    isWhite = !isWhite;
                    currNode = isWhite ? whiteNodes : blackNodes;
                } else {
                    // Make-up code, continue with same color
                    currNode = isWhite ? whiteNodes : blackNodes;
                }
            } else {
                // Invalid code, skip and reset
                bitOffset--;
                if (bitOffset < 0) {
                    bitOffset = 7;
                    byteOffset++;
                    currNode = whiteNodes;
                    isWhite = true;
                }
            }
        }
        return true;
    }

    @Override
    public int decode(byte[] pix, int offset, int len) throws Exception {
        destByteOffset = offset;
        // Decode each scanline
        int linesDecoded = 0;
        while (linesDecoded < rowsPerStrip && byteOffset < endOffset) {
            // Find EOL before this scanline (except in no-EOL mode)
            if (!noEOLMode) {
                if (!findNextEOL()) {
                    // No more EOLs found
                    if (byteOffset >= endOffset) {
                        // either EOF or malformed
                        break;
                    }
                    // Continue in no-EOL mode without EOL
                }
            }

            // Decode the scanline
            if (!decodeScanline(pix, destByteOffset, len)) {
                // Premature end of data
                break;
            }
            linesDecoded++;
        }

        // Fill remaining lines with white if needed
        if (totalRunLen < (scanLineWidth * rowsPerStrip)) {
            destByteOffset = outputRunLen(pix, destByteOffset, (scanLineWidth * rowsPerStrip) - totalRunLen, scanLineWidth, 0, len);
        }

        return uncompressedBytes;
    }
}