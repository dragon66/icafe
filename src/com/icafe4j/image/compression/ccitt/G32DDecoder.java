/**
 * CCITT T.4 Group 3 2D (Modified READ) fax decoder implementation.
 * Optimized with Huffman tree navigation and inline 2D mode pattern matching.
 */

package com.icafe4j.image.compression.ccitt;

import com.icafe4j.image.compression.huffman.T4BlackCodeHuffmanTreeNode;
import com.icafe4j.image.compression.huffman.T4CodeHuffmanTreeNode;
import com.icafe4j.image.compression.huffman.T4WhiteCodeHuffmanTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class G32DDecoder extends CCITTDecoder {

    // Huffman trees for white and black run decoding
    private static final T4CodeHuffmanTreeNode whiteTree = T4WhiteCodeHuffmanTreeNode.getInstance();
    private static final T4CodeHuffmanTreeNode blackTree = T4BlackCodeHuffmanTreeNode.getInstance();

    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(G32DDecoder.class);

    // Bit extraction masks for multi-byte bit reading
    private static int[] rightBitsMask = {0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff};
    private static int[] leftBitsMask = {0x00, 0x80, 0xc0, 0xe0, 0xf0, 0xf8, 0xfc, 0xfe, 0xff};

    protected int bitPointer;
    protected int bytePointer;
    private boolean align = false;
    protected int changingElemSize = 0;
    protected int[] prevChangingElems;
    protected int[] currChangingElems;
    private int lastChangingElement = 0;
    private boolean fillBits = false;
    protected int lineOffset = 0;

    public G32DDecoder(int scanLineWidth, int rowsPerStrip) {
        this(scanLineWidth, rowsPerStrip, false);
    }

    public G32DDecoder(int scanLineWidth, int rowsPerStrip, boolean fillBits) {
        super(scanLineWidth, rowsPerStrip);
        if(scanLineWidth < 2) {
            scanLineWidth = 2;
        }
        this.bitPointer = 0;
        this.bytePointer = 0;
        this.fillBits = fillBits;
        this.prevChangingElems = new int[scanLineWidth];
        this.currChangingElems = new int[scanLineWidth];
    }

    public G32DDecoder(byte[] input, int scanLineWidth, int rowsPerStrip) {
        this(input, scanLineWidth, rowsPerStrip, false);
    }

    public G32DDecoder(byte[] input, int scanLineWidth, int rowsPerStrip, boolean fillBits) {
        super(input, scanLineWidth, rowsPerStrip);
        if (scanLineWidth < 2) {
            scanLineWidth = 2;
        }
        this.bitPointer = 0;
        this.bytePointer = 0;
        this.fillBits = fillBits;
        this.prevChangingElems = new int[scanLineWidth];
        this.currChangingElems = new int[scanLineWidth];
    }

    /**
     * Decode a 1D-encoded scanline using Modified Huffman coding.
     * Alternates between white and black runs until line width is reached.
     */
    protected void decodeNextScanline(byte[] buffer, int lineOffset, int bitOffset) {
        boolean isWhite = true;
        this.changingElemSize = 0;
        while (bitOffset < this.scanLineWidth) {
            if (isWhite) {
                int runLength = decodeCodeWord(whiteTree);
                bitOffset += runLength;
                this.currChangingElems[this.changingElemSize++] = bitOffset;
                isWhite = false;
            } else {
                int runLength = decodeCodeWord(blackTree);
                setToBlack(buffer, lineOffset, bitOffset, runLength);
                bitOffset += runLength;
                this.currChangingElems[this.changingElemSize++] = bitOffset;
                isWhite = true;
            }
            if (bitOffset == this.scanLineWidth) {
                align();
                break;
            }
        }
        this.currChangingElems[this.changingElemSize++] = bitOffset;
    }

    /**
     * Decode a run length using Huffman tree navigation.
     * Handles both terminating codes (0-63) and makeup codes (64+).
     */
    private int decodeCodeWord(T4CodeHuffmanTreeNode tree) {
        int runLength = 0;
        boolean isTerminating = false;
        while (!isTerminating) {
            T4CodeHuffmanTreeNode node = tree;
            int value = -1;
            // Navigate the Huffman tree
            while (node.left() != null || node.right() != null) {
                int bit = nextLesserThan8Bits(1);
                if (bit == 0) {
                    node = node.left();
                } else {
                    node = node.right();
                }
                if (node == null) {
                    throw new RuntimeException("Invalid code encountered during Huffman decoding.");
                }
            }
            value = node.value();
            runLength += value;
            // Terminating codes are 0-63, makeup codes are 64+
            if (value < 64) {
                isTerminating = true;
            }
        }
        return runLength;
    }

    /**
     * Find b1 and b2 changing elements on the reference line for 2D coding.
     * Returns positions in ret[0] (b1) and ret[1] (b2).
     */
    private void getNextChangingElement(int a, boolean isWhite, int[] ret) {
        int[] pce = this.prevChangingElems;
        int ces = this.changingElemSize;
        int start = (this.lastChangingElement > 0) ? (this.lastChangingElement - 1) : 0;
        if (isWhite) {
            start &= ~0x1;
        } else {
            start |= 0x1;
        }
        int i = start;
        for(; i < ces; i += 2) {
            int temp = pce[i];
            if (temp > a) {
                this.lastChangingElement = i;
                ret[0] = temp;
                break;
            }
        }
        if ((i + 1) < ces) {
            ret[1] = pce[i + 1];
        }
    }

    private boolean align() {
        if (this.align && this.bitPointer != 0) {
            this.bytePointer++;
            this.bitPointer = 0;
            return true;
        }
        return false;
    }

    /**
     * Read End-Of-Line marker and tag bit.
     * Returns tag bit: 1 for 1D-encoded line, 0 for 2D-encoded line.
     */
    private int readEOL(boolean isFirstEOL) {
        if (!seekEOL()) {
            throw new RuntimeException("EOL not found");
        }
        if (!this.fillBits) {
            int next12Bits = nextNBits(12);
            if (isFirstEOL && (next12Bits == 0)) {
                if (nextNBits(4) == 1) {
                    this.fillBits = true;
                    return 1;
                }
            }
            if (next12Bits != 1) {
                throw new RuntimeException("Scanline must begin with EOL code word.");
            }
        } else {
            int bitsLeft = 8 - this.bitPointer;
            if (nextNBits(bitsLeft) != 0) {
                throw new RuntimeException("All fill bits preceding EOL code must be 0.");
            }
            if (bitsLeft < 4) {
                if (nextNBits(8) != 0) {
                    throw new RuntimeException("All fill bits preceding EOL code must be 0.");
                }
            }
            int next8 = nextNBits(8);
            if (isFirstEOL && (next8 & 0xf0) == 0x10) {
                this.fillBits = false;
                updatePointer(4);
            } else {
                while (next8 != 1) {
                    if (next8 != 0) {
                        throw new RuntimeException("0 bits expected before EOL");
                    }
                    next8 = nextNBits(8);
                }
            }
        }
        return nextLesserThan8Bits(1);
    }

    /**
     * Search for EOL marker (12 consecutive zero bits followed by 1).
     * Returns true if EOL found, false otherwise.
     */
    private boolean seekEOL() {
        int bitIndexMax = this.input.length * 8 - 1;
        int bitIndex = this.bytePointer * 8 + this.bitPointer;
        while (bitIndex <= bitIndexMax - 12) {
            int next12Bits = nextNBits(12);
            bitIndex += 12;
            while (next12Bits != 1 && bitIndex < bitIndexMax) {
                next12Bits = ((next12Bits & 0x000007ff) << 1) | (nextLesserThan8Bits(1) & 0x00000001);
                bitIndex++;
            }
            if (next12Bits == 1) {
                updatePointer(12);
                return true;
            }
        }
        return false;
    }

    /**
     * Set a run of pixels to black (set bits to 1) in the output buffer.
     * Handles bit-aligned writes spanning multiple bytes.
     */
    private void setToBlack(byte[] buffer, int lineOffset, int bitOffset, int numBits) {
        int bitNum = (8 * lineOffset) + bitOffset;
        int lastBit = bitNum + numBits;
        int byteNum = bitNum >> 3;
        int shift = bitNum & 0x7;
        if (shift > 0) {
            int maskVal = 1 << (7 - shift);
            byte val = buffer[byteNum];
            while ((maskVal > 0) && (bitNum < lastBit)) {
                val |= maskVal;
                maskVal >>= 1;
                ++bitNum;
            }
            buffer[byteNum] = val;
        }
        byteNum = bitNum >> 3;
        while (bitNum < (lastBit - 7)) {
            buffer[byteNum++] = (byte) 255;
            bitNum += 8;
        }
        while (bitNum < lastBit) {
            byteNum = bitNum >> 3;
            buffer[byteNum] |= (1 << (7 - (bitNum & 0x7)));
            ++bitNum;
        }
    }

    /**
     * Read up to 8 bits from the bit stream.
     * Handles reading across byte boundaries.
     */
    private int nextLesserThan8Bits(int bitsToGet) {
        byte b, next;
        int bp = this.bytePointer;
        b = this.input[bp];
        if (bp == this.input.length - 1) {
            next = 0x00;
        } else {
            next = this.input[bp + 1];
        }
        int bitsLeft = 8 - this.bitPointer;
        int bitsFromNextByte = bitsToGet - bitsLeft;
        int shift = bitsLeft - bitsToGet;
        int i1, i2;
        if (shift >= 0) {
            i1 = (b & rightBitsMask[bitsLeft]) >>> shift;
            this.bitPointer += bitsToGet;
            if (this.bitPointer == 8) {
                this.bitPointer = 0;
                this.bytePointer++;
            }
        } else {
            i1 = (b & rightBitsMask[bitsLeft]) << (-shift);
            i2 = (next & leftBitsMask[bitsFromNextByte]) >>> (8 - bitsFromNextByte);
            i1 |= i2;
            this.bytePointer++;
            this.bitPointer = bitsFromNextByte;
        }
        return i1;
    }

    /**
     * Read more than 8 bits from the bit stream (up to 16 bits).
     * Handles reading across up to 3 byte boundaries.
     */
    private int nextNBits(int bitsToGet) {
        byte b, next, next2next;
        int bp = this.bytePointer;
        b = this.input[bp];
        if (bp == this.input.length - 1) {
            next = 0x00;
            next2next = 0x00;
        } else if ((bp + 1) == this.input.length - 1) {
            next = this.input[bp + 1];
            next2next = 0x00;
        } else {
            next = this.input[bp + 1];
            next2next = this.input[bp + 2];
        }
        int bitsLeft = 8 - this.bitPointer;
        int bitsFromNextByte = bitsToGet - bitsLeft;
        int bitsFromNext2NextByte = 0;
        if (bitsFromNextByte > 8) {
            bitsFromNext2NextByte = bitsFromNextByte - 8;
            bitsFromNextByte = 8;
        }
        int i1 = (b & rightBitsMask[bitsLeft]) << (bitsToGet - bitsLeft);
        int i2 = (next & leftBitsMask[bitsFromNextByte]) >>> (8 - bitsFromNextByte);
        int i3 = 0;
        if (bitsFromNext2NextByte != 0) {
            i2 <<= bitsFromNext2NextByte;
            i3 = (next2next & leftBitsMask[bitsFromNext2NextByte]) >>> (8 - bitsFromNext2NextByte);
            i1 |= i2 | i3;
            this.bytePointer += 2;
            this.bitPointer = bitsFromNext2NextByte;
        } else {
            if (bitsFromNextByte == 8) {
                this.bitPointer = 0;
                this.bytePointer += 2;
            } else {
                this.bytePointer++;
                this.bitPointer = bitsFromNextByte;
            }
            i1 |= i2;
        }
        return i1;
    }

    /**
     * Check if there are enough bits remaining in the data stream.
     */
    private boolean hasEnoughBits(int bitsNeeded) {
        int totalBits = this.input.length * 8;
        int currentBit = this.bytePointer * 8 + this.bitPointer;
        return (currentBit + bitsNeeded) <= totalBits;
    }

    /**
     * Decode a 2D-encoded line using Modified READ algorithm.
     * Processes Pass, Horizontal, and Vertical modes with reference to previous line.
     * Returns the number of processed scanlines (always 1).
     */
    protected int decode2DLine(byte[] buffer, int lineOffset, int bitOffset) {
        int b1, b2;
        int[] b = new int[2];
        T42DCodingMode mode;
        boolean isWhite = true;
        int currIndex = 0;
        this.lastChangingElement = 0;

        while (bitOffset < this.scanLineWidth) {
            int a0 = (bitOffset > 0 ? bitOffset : -1);
            getNextChangingElement(a0, isWhite, b);
            b1 = b[0];
            b2 = b[1];

            // Read 2D mode code using inline pattern matching (1-7 bits)
            int bit1 = nextLesserThan8Bits(1);
            if (bit1 == 1) {
                // V(0) mode: '1'
                mode = T42DCodingMode.VERTICAL0;
            } else {
                int bit2 = nextLesserThan8Bits(1);
                if (bit2 == 1) {
                    int bit3 = nextLesserThan8Bits(1);
                    if (bit3 == 0) {
                        // VL(1) mode: '010'
                        mode = T42DCodingMode.VERTICAL_LEFT1;
                    } else {
                        // VR(1) mode: '011'
                        mode = T42DCodingMode.VERTICAL_RIGHT1;
                    }
                } else {
                    int bit3 = nextLesserThan8Bits(1);
                    if (bit3 == 1) {
                        // Horizontal mode: '001'
                        mode = T42DCodingMode.HORIZONTAL;
                    } else {
                        int bit4 = nextLesserThan8Bits(1);
                        if (bit4 == 1) {
                            // Pass mode: '0001'
                            mode = T42DCodingMode.PASS;
                        } else {
                            int bit5 = nextLesserThan8Bits(1);
                            if (bit5 == 1) {
                                int bit6 = nextLesserThan8Bits(1);
                                if (bit6 == 0) {
                                    // VL(2) mode: '000010'
                                    mode = T42DCodingMode.VERTICAL_LEFT2;
                                } else {
                                    // VR(2) mode: '000011'
                                    mode = T42DCodingMode.VERTICAL_RIGHT2;
                                }
                            } else {
                                int bit6 = nextLesserThan8Bits(1);
                                if (bit6 == 1) {
                                    int bit7 = nextLesserThan8Bits(1);
                                    if (bit7 == 0) {
                                        // VL(3) mode: '0000010'
                                        mode = T42DCodingMode.VERTICAL_LEFT3;
                                    } else {
                                        // VR(3) mode: '0000011'
                                        mode = T42DCodingMode.VERTICAL_RIGHT3;
                                    }
                                } else {
                                    // This could be EOFB marker or corrupted data
                                    if (hasEnoughBits(18)) {
                                        int savedBytePointer = this.bytePointer;
                                        int savedBitPointer = this.bitPointer;
                                        int next18Bits = nextNBits(18);
                                        if (next18Bits == 0x1001) { // EOFB
                                            break;
                                        }
                                        this.bytePointer = savedBytePointer;
                                        this.bitPointer = savedBitPointer;
                                    }
                                    throw new RuntimeException("Invalid 2D mode code");
                                }
                            }
                        }
                    }
                }
            }

            switch (mode) {
                case PASS:
                    if (!isWhite) {
                        setToBlack(buffer, lineOffset, bitOffset, b2 - bitOffset);
                    }
                    bitOffset = b2;
                    break;
                case HORIZONTAL:
                    int number;
                    if (isWhite) {
                        number = decodeCodeWord(whiteTree);
                        bitOffset += number;
                        this.currChangingElems[currIndex++] = bitOffset;
                        number = decodeCodeWord(blackTree);
                        setToBlack(buffer, lineOffset, bitOffset, number);
                        bitOffset += number;
                        this.currChangingElems[currIndex++] = bitOffset;
                    } else {
                        number = decodeCodeWord(blackTree);
                        setToBlack(buffer, lineOffset, bitOffset, number);
                        bitOffset += number;
                        this.currChangingElems[currIndex++] = bitOffset;
                        number = decodeCodeWord(whiteTree);
                        bitOffset += number;
                        this.currChangingElems[currIndex++] = bitOffset;
                    }
                    break;
                case VERTICAL0:
                    int a1 = b1;
                    this.currChangingElems[currIndex++] = a1;
                    if (!isWhite) {
                        setToBlack(buffer, lineOffset, bitOffset, a1 - bitOffset);
                    }
                    bitOffset = a1;
                    isWhite = !isWhite;
                    break;
                case VERTICAL_RIGHT1:
                    a1 = b1 + 1;
                    this.currChangingElems[currIndex++] = a1;
                    if (!isWhite) {
                        setToBlack(buffer, lineOffset, bitOffset, a1 - bitOffset);
                    }
                    bitOffset = a1;
                    isWhite = !isWhite;
                    break;
                case VERTICAL_RIGHT2:
                    a1 = b1 + 2;
                    this.currChangingElems[currIndex++] = a1;
                    if (!isWhite) {
                        setToBlack(buffer, lineOffset, bitOffset, a1 - bitOffset);
                    }
                    bitOffset = a1;
                    isWhite = !isWhite;
                    break;
                case VERTICAL_RIGHT3:
                    a1 = b1 + 3;
                    this.currChangingElems[currIndex++] = a1;
                    if (!isWhite) {
                        setToBlack(buffer, lineOffset, bitOffset, a1 - bitOffset);
                    }
                    bitOffset = a1;
                    isWhite = !isWhite;
                    break;
                case VERTICAL_LEFT1:
                    a1 = b1 - 1;
                    this.currChangingElems[currIndex++] = a1;
                    if (!isWhite) {
                        setToBlack(buffer, lineOffset, bitOffset, a1 - bitOffset);
                    }
                    bitOffset = a1;
                    isWhite = !isWhite;
                    break;
                case VERTICAL_LEFT2:
                    a1 = b1 - 2;
                    this.currChangingElems[currIndex++] = a1;
                    if (!isWhite) {
                        setToBlack(buffer, lineOffset, bitOffset, a1 - bitOffset);
                    }
                    bitOffset = a1;
                    isWhite = !isWhite;
                    break;
                case VERTICAL_LEFT3:
                    a1 = b1 - 3;
                    this.currChangingElems[currIndex++] = a1;
                    if (!isWhite) {
                        setToBlack(buffer, lineOffset, bitOffset, a1 - bitOffset);
                    }
                    bitOffset = a1;
                    isWhite = !isWhite;
                    break;
                default:
                    throw new RuntimeException("Invalid code encountered while decoding 2D group 3 compressed data.");
            }
        }
        this.currChangingElems[currIndex++] = bitOffset;
        this.changingElemSize = currIndex;
        return 1;
    }

    /**
     * Move the bit stream pointer backward by specified number of bits.
     */
    private void updatePointer(int bitsToMoveBack) {
        if (bitsToMoveBack > 8) {
            this.bytePointer -= bitsToMoveBack / 8;
            bitsToMoveBack %= 8;
        }
        int i = this.bitPointer - bitsToMoveBack;
        if (i < 0) {
            this.bytePointer--;
            this.bitPointer = 8 + i;
        } else {
            this.bitPointer = i;
        }
    }

    @Override
    public int decode(byte[] buffer, int offset, int len) throws Exception {
        int scanlineStride = (this.scanLineWidth + 7) / 8;
        this.bitPointer = 0;
        this.bytePointer = 0;
        this.lineOffset = offset;
        int[] temp;

        if (readEOL(true) != 1) {
            throw new RuntimeException("First scanline must be 1D encoded.");
        }
        int linesDecoded = 0;
        try {
            decodeNextScanline(buffer, lineOffset, 0);
            lineOffset += scanlineStride;
            linesDecoded++;
            for (int lines = 1; lines < rowsPerStrip; lines++) {
                if (readEOL(false) == 0) {
                    // 2D encoded line
                    temp = this.prevChangingElems;
                    this.prevChangingElems = this.currChangingElems;
                    this.currChangingElems = temp;
                    decode2DLine(buffer, lineOffset, 0);
                } else {
                    decodeNextScanline(buffer, lineOffset, 0);
                }
                lineOffset += scanlineStride;
                linesDecoded++;
            }
        } catch (Exception e) {
            LOGGER.error("Decoding terminated after {} lines as opposed to {}: {}", linesDecoded, rowsPerStrip, e.getMessage());
        }
        return scanlineStride * linesDecoded;
    }
}