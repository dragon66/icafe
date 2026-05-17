/**
 * COPYRIGHT (C) 2014-2019 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Any modifications to this file must keep this entire header intact.
 * Implementation of TIFF/CCITT T.6 Group 4 decompression.
 * Group 4 uses pure 2D Modified READ encoding without EOL markers.
 */

package com.icafe4j.image.compression.ccitt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder for TIFF CCITT T.6 Group 4 compression (pure 2D Modified READ).
 * * Format:
 * No EOL markers between lines (unlike Group 3)
 * Every line is 2D encoded using Modified READ algorithm
 * Terminated with EOFB (two consecutive EOL markers)
 * Similar to G3 2D but without EOL+tag overhead
 */
public class G42DDecoder extends G32DDecoder {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(G42DDecoder.class);

    public G42DDecoder(int scanLineWidth, int rowsPerStrip) {
        super(scanLineWidth, rowsPerStrip);
    }

    public G42DDecoder(byte[] input, int scanLineWidth, int rowsPerStrip) {
        super(input, scanLineWidth, rowsPerStrip);
    }

    /**
     * Decode Group 4 compressed data.
     * All lines are 2D encoded without EOL markers (except EOFB at end).
     */
    @Override
    public int decode(byte[] buffer, int offset, int len) throws Exception {
        int scanlineStride = (this.scanLineWidth + 7) / 8;
        this.bitPointer = 0;
        this.bytePointer = 0;
        int linesDecoded = 0;
        int[] temp;

        // Initialize prevChangingElems with imaginary all-white reference line
        // For an all-white line, there's only one changing element at position w
        this.prevChangingElems[0] = this.scanLineWidth;
        this.changingElemSize = 1;

        try {
            // In Group 4, all lines are 2D encoded without EOL markers
            // The first line uses an imaginary all-white reference line
            for (int lines = 0; lines < rowsPerStrip; lines++) {
                // Decode this 2D line
                decode2DLine(buffer, lineOffset, 0);
                // Swap reference and current line arrays for next iteration
                temp = this.prevChangingElems;
                this.prevChangingElems = this.currChangingElems;
                this.currChangingElems = temp;
                lineOffset += scanlineStride;
                linesDecoded++;
            }
        } catch (Exception e) {
            // End of data or error encountered
            LOGGER.error("Decoding terminated after {} lines as opposed to {}: {}", linesDecoded, rowsPerStrip, e.getMessage());
        }
        return scanlineStride * linesDecoded;
    }
}