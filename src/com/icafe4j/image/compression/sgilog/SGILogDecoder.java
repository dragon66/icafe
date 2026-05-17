/**
 * COPYRIGHT (C) 2014-2026 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.compression.sgilog;

import com.icafe4j.image.compression.ImageDecoder;

/**
 * SGI LogLuv Decoder for TIFF images
 * <p>
 * Supports two compression formats:
 * <ul>
 * <li>SGILog (34676) RLE compressed LogLuv (16-bit LogL or 32-bit LogLuv with 16-bit L + 8-bit u + 8-bit v)</li>
 * <li>SGILog24 (34677) 24-bit packed LogLuv (10-bit Le + 14-bit encoded u',v')</li>
 * </ul>
 * 
 * <p>
 * * SGI LogLuv encoding stores high dynamic range images in a logarithmic color space.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.1 02/08/2026
 */
public class SGILogDecoder implements ImageDecoder {

    private byte[] input;
    private int inPos;
    private int width;
    private int height;
    private int samplesPerPixel;
    private boolean isSGILog24;

    /**
     * Constructor for SGILog format (RLE compressed)
     * @param width image width
     * @param height image height
     * @param samplesPerPixel 1 for LOGL, 3 for LOGLUV
     */
    public SGILogDecoder(int width, int height, int samplesPerPixel) {
        this.width = width;
        this.height = height;
        this.samplesPerPixel = samplesPerPixel;
        this.isSGILog24 = false;
    }

    /**
     * Constructor for SGILog24 format (24-bit packed, not RLE compressed)
     * @param width image width
     * @param height image height
     * @param isSGILog24 must be true to indicate SGILog24 format
     */
    public SGILogDecoder(int width, int height, boolean isSGILog24) {
        this.width = width;
        this.height = height;
        this.samplesPerPixel = 3;
        this.isSGILog24 = isSGILog24;
    }

    @Override
    public void setInput(byte[] input) {
        setInput(input, 0, input.length);
    }

    @Override
    public void setInput(byte[] input, int offset, int length) {
        this.input = input;
        this.inPos = offset;
    }

    /**
     * Decode to RGB output buffer
     * @param output RGB output buffer (must be width * height * 3 bytes)
     * @param start starting position in output buffer
     * @param len maximum bytes to write
     * @return number of bytes written
     */
    @Override
    public int decode(byte[] output, int start, int len) throws Exception {
        if (isSGILog24) {
            return decodeSGILog24(output, start, len);
        } else {
            return decodeSGILog(output, start, len);
        }
    }

    /**
     * Decode SGILog format (RLE compressed) to RGB
     * For LOGL: 16-bit luminance per pixel
     * For LOGLUV: 32-bit per pixel (16-bit L + 8-bit u + 8-bit v)
     */
    private int decodeSGILog(byte[] output, int start, int len) {
        int outPos = start;
        int inputPos = inPos;

        if (samplesPerPixel == 1) {
            // LOGL format: grayscale luminance
            for (int row = 0; row < height; row++) {
                int[] rowData = new int[width];
                inputPos = decodeRLEScanline16(input, inputPos, rowData, width);

                // Convert LogL to grayscale RGB
                for (int col = 0; col < width; col++) {
                    int L = rowData[col] & 0xFFFF;
                    double Y = logL16ToY(L);

                    // From libtiff L16toGry: apply sqrt for gamma 2.0
                    // *gp++ = (Y <= 0.) ? 0 : (Y >= 1.) ? 255 : (int(256. * sqrt(Y))
                    int gray;
                    if (Y <= 0.0) {
                        gray = 0;
                    } else if (Y >= 1.0) {
                        gray = 255;
                    } else {
                        gray = (int) (256.0 * Math.sqrt(Y));
                        if (gray > 255) gray = 255;
                    }

                    output[outPos++] = (byte) gray;
                    output[outPos++] = (byte) gray;
                    output[outPos++] = (byte) gray;
                }
            }
        } else {
            // LOGLUV format: color (32-bit: 16-bit L + 8-bit u + 8-bit v)
            for (int row = 0; row < height; row++) {
                int[] rowData = new int[width * 4]; // L, u, v, padding
                inputPos = decodeRLEScanline32(input, inputPos, rowData, width);

                // Convert LogLuv to RGB for this scanline
                for (int col = 0; col < width; col++) {
                    int offset = col * 4;
                    int L = (rowData[offset] << 8) | rowData[offset + 1]; //16-bit big-endian
                    int u = rowData[offset + 2] & 0xFF;
                    int v = rowData[offset + 3] & 0xFF;

                    double[] rgb = logLuvToRGB(L, u, v);
                    output[outPos++] = (byte) Math.min(255, Math.max(0, (int)(rgb[0] * 255.0)));
                    output[outPos++] = (byte) Math.min(255, Math.max(0, (int)(rgb[1] * 255.0)));
                    output[outPos++] = (byte) Math.min(255, Math.max(0, (int)(rgb[2] * 255.0)));
                }
            }
        }

        inPos = inputPos;
        return outPos - start;
    }

    /**
     * Decode SGILog24 format (24-bit packed: 10-bit Le + 14-bit UV) to RGB
     * This format is NOT RLE compressed
     */
    private int decodeSGILog24(byte[] output, int start, int len) {
        int inputPos = inPos;
        int outPos = start;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (inputPos + 2 >= input.length) break;

                // Read 3 bytes (24 bits): LLLLLLLLLL UUUUUUUVVV VVVVV
                int byte1 = input[inputPos++] & 0xFF;
                int byte2 = input[inputPos++] & 0xFF;
                int byte3 = input[inputPos++] & 0xFF;

                // Extract 10-bit Le and 14-bit UV (big-endian)
                int Le = ((byte1 << 2) | (byte2 >> 6)) & 0x3FF; // 10 bits
                int uv = ((byte2 & 0x3F) << 8) | byte3; // 14 bits

                // Decode to RGB
                double[] rgb = logLuv24ToRGB(Le, uv);
                output[outPos++] = (byte) Math.min(255, Math.max(0, (int)(rgb[0] * 255.0)));
                output[outPos++] = (byte) Math.min(255, Math.max(0, (int)(rgb[1] * 255.0)));
                output[outPos++] = (byte) Math.min(255, Math.max(0, (int)(rgb[2] * 255.0)));
            }
        }

        inPos = inputPos;
        return outPos - start;
    }

    /**
     * Decode RLE-compressed 16-bit scanline (for LogL)
     * libtiff decodes TWO separate byte streams (high byte, low byte)
     */
    private int decodeRLEScanline16(byte[] input, int inPos, int[] output, int width) {
        // Initialize output to zero
        for (int i = 0; i < width; i++) {
            output[i] = 0;
        }

        // Decode each byte string separately (high byte at shift 8, then low byte at shift 0)
        for (int shft = 8; shft >= 0; shft -= 8) {
            int i = 0;
            while (i < width && inPos < input.length) {
                int count = input[inPos++] & 0xFF;

                if (count >= 128) {
                    // Run: rc = *bp++ + (2 - 128) = count - 126
                    count = count + 2 - 128; // This is count - 126
                    if (inPos >= input.length) break;
                    int b = (input[inPos++] & 0xFF) << shft;
                    while (count-- > 0 && i < width) {
                        output[i++] |= b;
                    }
                } else {
                    // Literal: rc = *bp++ (then read rc bytes)
                    // Note: count of 0 is a noop
                    while (count > 0 && i < width && inPos < input.length) {
                        int b = (input[inPos++] & 0xFF) << shft;
                        output[i++] |= b;
                        count--;
                    }
                }
            }
        }

        return inPos;
    }

    /**
     * Decode RLE-compressed 32-bit scanline (for LogLuv: L, u, v, pad)
     * libtiff decodes FOUR separate byte streams, one for each byte position
     */
    private int decodeRLEScanline32(byte[] input, int inPos, int[] output, int width) {
        // Create array of 32-bit values
        int[] pixels = new int[width];
        for (int i = 0; i < width; i++) {
            pixels[i] = 0;
        }
        
        // Decode each byte string separately (from MSB to LSB: shifts 24, 16, 8, 0)
        for (int shft = 24; shft >= 0; shft -= 8) {
            int i = 0;
            while (i < width && inPos < input.length) {
                int count = input[inPos++] & 0xFF;

                if (count >= 128) {
                    // Run: rc = *bp++ + (2 - 128) = count - 126)
                    count = count + 2 - 128; // This is count - 126
                    if (inPos >= input.length) break;
                    int b = (input[inPos++] & 0xFF) << shft;
                    while (count-- > 0 && i < width) {
                        pixels[i++] |= b;
                    }
                } else {
                    // Literal: rc = *bp++ (then read rc bytes)
                    // Note: count of 0 is a noop
                    while (count > 0 && i < width && inPos < input.length) {
                        int b = (input[inPos++] & 0xFF) << shft;
                        pixels[i++] |= b;
                        count--;
                    }
                }
            }
        }

        // Unpack 32-bit values into byte array: byte0, byte1, byte2, byte3
        for (int i = 0; i < width; i++) {
            output[i * 4 + 0] = (pixels[i] >> 24) & 0xFF;
            output[i * 4 + 1] = (pixels[i] >> 16) & 0xFF;
            output[i * 4 + 2] = (pixels[i] >> 8) & 0xFF;
            output[i * 4 + 3] = pixels[i] & 0xFF;
        }

        return inPos;
    }

    /**
     * Convert 16-bit LogL to luminance Y
     */
    private double logL16ToY(int L) {
        // LogL16 encoding from libtiff: Y = exp(M_LN2 / 256. * (Le + .5) - M_LN2 * 64.)
        // Where M_Ln2 = ln(2) = 0.69314718055994530942
        // This simplifies to: Y = 2^((Le + 0.5) / 256 - 64)
        final double M_LN2 = 0.69314718055994530942;
        int Le = L & 0x7FFF; // 15 bits (ignore sign bit)
        if (Le == 0) return 0.0;
        double Y = Math.exp(M_LN2 / 256.0 * (Le + 0.5) - M_LN2 * 64.0);
        // Check sign bit
        return ((L & 0x8000) != 0) ? -Y : Y;
    }

    /**
     * Convert LogLuv (16-bit L + 8-bit u + 8-bit v) to RGB
     */
    private double[] logLuvToRGB(int L, int u, int v) {
        // Decode luminance from 16-bit log encoding
        double Y = logL16ToY(L);
        if (Y <= 0.0) {
            return new double[] { 0.0, 0.0, 0.0 };
        }

        // Decode chromaticity u', v' from 8-bit values
        // From libtiff: u = 1. / UVSCALE * ((p >> 8 & 0xff) + .5)
        // Where UVSCALE = 410.0
        final double UVSCALE = 410.0;
        double up = (u + 0.5) / UVSCALE;
        double vp = (v + 0.5) / UVSCALE;

        // Convert from CIE 1976 u', v' to XYZ
        double[] xyz = uvToXYZ(Y, up, vp);

        // Convert XYZ to RGB
        return xyzToRGB(xyz[0], xyz[1], xyz[2]);
    }

    /**
     * Convert SGILog24 (10-bit Le + 14-bit UV) to RGB
     */
    private double[] logLuv24ToRGB(int Le, int Ce) {
        // Decode luminance from 10-bit log encoding
        // From libtiff: L = exp(M_LN2 / 64. * (p10 + .5) - M_LN2 * 12.)
        // This simplifies to: Y = 2^((Le + 0.5) / 64 - 12)
        final double M_LN2 = 0.69314718055994530942;
        if (Le == 0) {
            return new double[] { 0.0, 0.0, 0.0 };
        }
        double Y = Math.exp(M_LN2 / 64.0 * (Le + 0.5) - M_LN2 * 12.0);

        // Decode chromaticity from 14-bit Ce index using uvDecode
        final double U_NEU = 0.210526316;
        final double V_NEU = 0.473684211;
        double[] up_vp = new double[2];
        if (!UVCode.uvDecode(up_vp, Ce)) {
            // If decoding fails, use neutral white point
            up_vp[0] = U_NEU;
            up_vp[1] = V_NEU;
        }

        // Convert from CIE 1976 u', v' to XYZ
        double[] xyz = uvToXYZ(Y, up_vp[0], up_vp[1]);

        // Convert XYZ to RGB
        return xyzToRGB(xyz[0], xyz[1], xyz[2]);
    }
    
    /**
     * Convert CIE 1976 (Y, u', v') to XYZ
     * From libtiff LogLuv32toXYZ function
     */
    private double[] uvToXYZ(double Y, double up, double vp) {
        if (Y <= 0.0) {
            return new double[] { 0.0, 0.0, 0.0 };
        }

        // From libtiff:
        // s = 1. / (6. * u - 16. * v + 12.);
        // x = 9. * u * s;
        // y = 4. * v * s;
        // XYZ[0] = (float)(x / y * L);
        // XYZ[1] = (float)L;
        // XYZ[2] = (float)((1. - x - y) / y * L);

        double s = 1.0 / (6.0 * up - 16.0 * vp + 12.0);
        double x = 9.0 * up * s;
        double y = 4.0 * vp * s;

        double X = (x / y) * Y;
        double Z = ((1.0 - x - y) / y) * Y;

        return new double[] { X, Y, Z };
    }

    /**
     * Convert XYZ to RGB (using CCIR-709 primaries as in libtiff)
     */
    private double[] xyzToRGB(double X, double Y, double Z) {
        double R = 2.690 * X + -1.276 * Y + -0.414 * Z;
        double G = -1.022 * X + 1.978 * Y + 0.044 * Z;
        double B = 0.061 * X + -0.224 * Y + 1.163 * Z;

        // Apply gamma 2.0 (square root) as in libtiff
        // From libtiff: (int)(256. * sqrt(r))
        R = (R <= 0.0) ? 0.0 : Math.sqrt(R);
        G = (G <= 0.0) ? 0.0 : Math.sqrt(G);
        B = (B <= 0.0) ? 0.0 : Math.sqrt(B);

        // Clamp to [0, 1]
        R = Math.max(0.0, Math.min(1.0, R));
        G = Math.max(0.0, Math.min(1.0, G));
        B = Math.max(0.0, Math.min(1.0, B));

        return new double[] { R, G, B };
    }
}