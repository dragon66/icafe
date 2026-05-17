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

/**
 * UV encoding/decoding for SGI LogLuv24 format
 * Based on libtiff uvcode.h UV lookup table for optimal gamut coverage
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 02/08/2026
 */
public class UVCode {

    public static final double UV_SQSIZ = 0.003500;
    public static final double UV_VSTART = 0.016940;
    public static final int UV_NDIVS = 16289;
    public static final int UV_NVS = 163;

    // UV row structure from libtiff uvcode.h
    // Each row has: ustart (starting u' value), ncum (cumulative count)
    private static final UVRow[] uv_row = {
        new UVRow(0.247663f, 0),
        new UVRow(0.243779f, 4),
        new UVRow(0.241684f, 10),
        new UVRow(0.237874f, 17),
        new UVRow(0.235906f, 26),
        new UVRow(0.232153f, 36),
        new UVRow(0.228352f, 48),
        new UVRow(0.226259f, 62),
        new UVRow(0.222371f, 77),
        new UVRow(0.220410f, 94),
        new UVRow(0.214710f, 112),
        new UVRow(0.212714f, 133),
        new UVRow(0.210721f, 155),
        new UVRow(0.204976f, 178),
        new UVRow(0.202986f, 204),
        new UVRow(0.199245f, 231),
        new UVRow(0.195525f, 260),
        new UVRow(0.193560f, 291),
        new UVRow(0.189878f, 323),
        new UVRow(0.186216f, 357),
        new UVRow(0.186216f, 393),
        new UVRow(0.182592f, 429),
        new UVRow(0.179003f, 467),
        new UVRow(0.175466f, 507),
        new UVRow(0.172001f, 549),
        new UVRow(0.172001f, 593),
        new UVRow(0.168612f, 637),
        new UVRow(0.168612f, 683),
        new UVRow(0.163575f, 729),
        new UVRow(0.158642f, 778),
        new UVRow(0.158642f, 830),
        new UVRow(0.158642f, 882),
        new UVRow(0.153815f, 934),
        new UVRow(0.153815f, 989),
        new UVRow(0.149097f, 1044),
        new UVRow(0.149097f, 1102),
        new UVRow(0.142746f, 1160),
        new UVRow(0.142746f, 1222),
        new UVRow(0.142746f, 1284),
        new UVRow(0.138270f, 1346),
        new UVRow(0.138270f, 1411),
        new UVRow(0.138270f, 1476),
        new UVRow(0.132166f, 1541),
        new UVRow(0.132166f, 1610),
        new UVRow(0.126204f, 1679),
        new UVRow(0.126204f, 1752),
        new UVRow(0.126204f, 1825),
        new UVRow(0.120381f, 1898),
        new UVRow(0.120381f, 1975),
        new UVRow(0.120381f, 2052),
        new UVRow(0.120381f, 2129),
        new UVRow(0.112962f, 2206),
        new UVRow(0.112962f, 2288),
        new UVRow(0.112962f, 2370),
        new UVRow(0.107450f, 2452),
        new UVRow(0.107450f, 2538),
        new UVRow(0.107450f, 2624),
        new UVRow(0.107450f, 2710),
        new UVRow(0.100343f, 2796),
        new UVRow(0.100343f, 2887),
        new UVRow(0.100343f, 2978),
        new UVRow(0.095126f, 3069),
        new UVRow(0.095126f, 3164),
        new UVRow(0.095126f, 3259),
        new UVRow(0.095126f, 3354),
        new UVRow(0.088276f, 3449),
        new UVRow(0.088276f, 3549),
        new UVRow(0.088276f, 3649),
        new UVRow(0.088276f, 3749),
        new UVRow(0.081523f, 3849),
        new UVRow(0.081523f, 3954),
        new UVRow(0.081523f, 4059),
        new UVRow(0.081523f, 4164),
        new UVRow(0.074861f, 4269),
        new UVRow(0.074861f, 4379),
        new UVRow(0.074861f, 4489),
        new UVRow(0.074861f, 4599),
        new UVRow(0.068290f, 4709),
        new UVRow(0.068290f, 4824),
        new UVRow(0.068290f, 4939),
        new UVRow(0.068290f, 5054),
        new UVRow(0.063573f, 5169),
        new UVRow(0.063573f, 5288),
        new UVRow(0.063573f, 5407),
        new UVRow(0.063573f, 5526),
        new UVRow(0.057219f, 5645),
        new UVRow(0.057219f, 5769),
        new UVRow(0.057219f, 5893),
        new UVRow(0.057219f, 6017),
        new UVRow(0.050985f, 6141),
        new UVRow(0.050985f, 6270),
        new UVRow(0.050985f, 6399),
        new UVRow(0.050985f, 6528),
        new UVRow(0.050985f, 6657),
        new UVRow(0.044859f, 6786),
        new UVRow(0.044859f, 6920),
        new UVRow(0.044859f, 7054),
        new UVRow(0.044859f, 7188),
        new UVRow(0.040571f, 7322),
        new UVRow(0.040571f, 7460),
        new UVRow(0.040571f, 7598),
        new UVRow(0.040571f, 7736),
        new UVRow(0.036339f, 7874),
        new UVRow(0.036339f, 8016),
        new UVRow(0.036339f, 8158),
        new UVRow(0.036339f, 8300),
        new UVRow(0.032139f, 8442),
        new UVRow(0.032139f, 8588),
        new UVRow(0.032139f, 8734),
        new UVRow(0.032139f, 8880),
        new UVRow(0.027947f, 9026),
        new UVRow(0.027947f, 9176),
        new UVRow(0.027947f, 9326),
        new UVRow(0.023739f, 9476),
        new UVRow(0.023739f, 9630),
        new UVRow(0.023739f, 9784),
        new UVRow(0.023739f, 9938),
        new UVRow(0.019504f, 10092),
        new UVRow(0.019504f, 10250),
        new UVRow(0.019504f, 10408),
        new UVRow(0.016976f, 10566),
        new UVRow(0.016976f, 10727),
        new UVRow(0.016976f, 10888),
        new UVRow(0.016976f, 11049),
        new UVRow(0.012639f, 11210),
        new UVRow(0.012639f, 11375),
        new UVRow(0.012639f, 11540),
        new UVRow(0.009991f, 11705),
        new UVRow(0.009991f, 11873),
        new UVRow(0.009991f, 12041),
        new UVRow(0.009016f, 12209),
        new UVRow(0.009016f, 12379),
        new UVRow(0.009016f, 12549),
        new UVRow(0.006217f, 12719),
        new UVRow(0.006217f, 12892),
        new UVRow(0.005097f, 13065),
        new UVRow(0.005097f, 13240),
        new UVRow(0.005097f, 13415),
        new UVRow(0.003909f, 13590),
        new UVRow(0.003909f, 13767),
        new UVRow(0.002340f, 13944),
        new UVRow(0.002389f, 14121),
        new UVRow(0.001068f, 14291),
        new UVRow(0.001653f, 14455),
        new UVRow(0.000717f, 14612),
        new UVRow(0.001614f, 14762),
        new UVRow(0.000270f, 14905),
        new UVRow(0.000484f, 15041),
        new UVRow(0.001103f, 15170),
        new UVRow(0.001242f, 15293),
        new UVRow(0.001188f, 15408),
        new UVRow(0.001011f, 15517),
        new UVRow(0.000709f, 15620),
        new UVRow(0.000301f, 15717),
        new UVRow(0.002416f, 15806),
        new UVRow(0.003251f, 15888),
        new UVRow(0.003246f, 15964),
        new UVRow(0.004141f, 16033),
        new UVRow(0.005963f, 16095),
        new UVRow(0.008839f, 16150),
        new UVRow(0.010490f, 16197),
        new UVRow(0.016994f, 16237),
        new UVRow(0.023659f, 16268)
    };

    /**
     * Helper class to store UV row data
     */
    private static class UVRow {
        float ustart; // Starting u' value for this row
        int ncum; // Cumulative count up to this row

        UVRow(float ustart, int ncum) {
            this.ustart = ustart;
            this.ncum = ncum;
        }
    }

    /**
     * Decode 14-bit chromaticity index to u',v' coordinates
     * Directly ported from libtiff uv_decode function
     */
    public static boolean uvDecode(double[] up_vp, int c) {
        if (c < 0 || c >= UV_NDIVS) {
            return false;
        }

        // Binary search to find which row contains this index
        int lower = 0;
        int upper = UV_NVS;

        while (upper - lower > 1) {
            int vi = (lower + upper) >> 1;
            int ui = c - uv_row[vi].ncum;

            if (ui > 0) {
                lower = vi;
            } else if (ui < 0) {
                upper = vi;
            } else {
                lower = vi;
                break;
            }
        }

        int vi = lower;
        int ui = c - uv_row[vi].ncum;

        // Calculate u' and v' from table
        up_vp[0] = uv_row[vi].ustart + (ui + 0.5) * UV_SQSIZ;
        up_vp[1] = UV_VSTART + (vi + 0.5) * UV_SQSIZ;

        return true;
    }
}