package com.icafe4j.image.compression.thunderscan;

import com.icafe4j.image.compression.ImageDecoder;

public class ThunderScanDecoder implements ImageDecoder {

    private byte[] input;
    private int inPos;
    private int inputLength;

    public ThunderScanDecoder() {
    }

    public ThunderScanDecoder(byte[] input) {
        setInput(input);
    }

    public void setInput(byte[] input) {
        setInput(input, 0, input.length);
    }

    @Override
    public void setInput(byte[] input, int offset, int length) {
        this.inPos = offset;
        this.input = input;
        this.inputLength = length;
    }

    /**
     * Decodes ThunderScan (compression 32809) RLE for 4-bit images.
     * @param pixels uncompressed data (output buffer, 2 pixels per byte, high nibble first)
     * @param start starting position in the output array
     * @param len length of the output data in bytes
     * @return decompressed 4-bit image data (packed, 2 pixels per byte)
     */
    // ThunderScan constants
    private static final int THUNDER_DATA = 0x3F;
    private static final int THUNDER_CODE = 0xC0;
    private static final int THUNDER_RUN = 0x00;
    private static final int THUNDER_2BITDELTAS = 0x40;
    private static final int DELTA2_SKIP = 2;
    private static final int THUNDER_3BITDELTAS = 0x80;
    private static final int DELTA3_SKIP = 4;
    private static final int THUNDER_RAW = 0xC0;
    private static final int[] twobitdeltas = {0, 1, 0, -1};
    private static final int[] threebitdeltas = {0, 1, 2, 3, 0, -3, -2, -1};

    @Override
    public int decode(byte[] pixels, int start, int len) throws Exception {
        int outPos = start;
        int outNibblePos = 0; // 0: high nibble, 1: low nibble
        int maxInputPos = inPos + inputLength;
        int npixels = 0;
        int lastpixel = 0;
        int inputPos = inPos;
        while (inputPos < maxInputPos && npixels < len * 2) {
            int n = input[inputPos++] & 0xFF;
            switch (n & THUNDER_CODE) {
                case THUNDER_RUN: {
                    int run = n & THUNDER_DATA;
                    for (int i = 0; i < run && npixels < len * 2; i++) {
                        if (outNibblePos == 0) {
                            pixels[outPos] = (byte) (lastpixel << 4);
                            outNibblePos = 1;
                        } else {
                            pixels[outPos] |= (byte) (lastpixel & 0x0F);
                            outPos++;
                            outNibblePos = 0;
                        }
                        npixels++;
                    }
                    break;
                }
                case THUNDER_2BITDELTAS: {
                    int nval = n;
                    for (int shift = 4; shift >= 0 && npixels < len * 2; shift -= 2) {
                        int delta = (nval >> shift) & 0x03;
                        if (delta != DELTA2_SKIP) {
                            lastpixel = (lastpixel + twobitdeltas[delta]) & 0x0F;
                            if (outNibblePos == 0) {
                                pixels[outPos] = (byte) (lastpixel << 4);
                                outNibblePos = 1;
                            } else {
                                pixels[outPos] |= (byte) (lastpixel & 0x0F);
                                outPos++;
                                outNibblePos = 0;
                            }
                            npixels++;
                        }
                    }
                    break;
                }
                case THUNDER_3BITDELTAS: {
                    int nval = n;
                    for (int shift = 3; shift >= 0 && npixels < len * 2; shift -= 3) {
                        int delta = (nval >> shift) & 0x07;
                        if (delta != DELTA3_SKIP) {
                            lastpixel = (lastpixel + threebitdeltas[delta]) & 0x0F;
                            if (outNibblePos == 0) {
                                pixels[outPos] = (byte) (lastpixel << 4);
                                outNibblePos = 1;
                            } else {
                                pixels[outPos] |= (byte) (lastpixel & 0x0F);
                                outPos++;
                                outNibblePos = 0;
                            }
                            npixels++;
                        }
                    }
                    break;
                }
                case THUNDER_RAW: {
                    lastpixel = n & 0x0F;
                    if (outNibblePos == 0) {
                        pixels[outPos] = (byte) (lastpixel << 4);
                        outNibblePos = 1;
                    } else {
                        pixels[outPos] |= (byte) (lastpixel & 0x0F);
                        outPos++;
                        outNibblePos = 0;
                    }
                    npixels++;
                    break;
                }
            }
        }
        if (outNibblePos != 0) outPos++;
        inPos = inputPos;
        return outPos - start;
    }
}