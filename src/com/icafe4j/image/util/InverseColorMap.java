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

package com.icafe4j.image.util;

/**
 * Helper class used for the dithering process to provide an efficient 
 * nearest color searching in a predefined color map for a given color.
 *
 * See "Efficient Inverse Color Map Computation" by Spencer W. Thomas
 * in "Graphics Gems Volume II"
 *
 * @version 1.05 03/17/2008
 * @author Wen Yu, yuwen_66@yahoo.com
 */
public class InverseColorMap {
	private int bitsReserved;// Number of bits used in color quantization.
	private int bitsDiscarded;// Number of discarded bits
	private int maxColorVal;// Maximum value for each quantized color
	private int invMapLen;// Length of the inverse color map
	// The inverse color map itself
	private byte[] invColorMap;
	
	// Default constructor using 5 for quantization bits
	public InverseColorMap() {
		this(5);
	}
	
    // Constructor using bitsReserved bits for quantization
	public InverseColorMap(int rbits) {
		bitsReserved = rbits;
		bitsDiscarded  = 8 - bitsReserved;
		maxColorVal = 1 << bitsReserved;
		invMapLen = maxColorVal * maxColorVal * maxColorVal;
        invColorMap = new byte[invMapLen];
	}
	
    // Fetch the forward color map index for this color
	public int getNearestColorIndex(int color) {
		return invColorMap[((((color&0xff0000) >> (16 + bitsDiscarded)) << (bitsReserved<<1))) | 
					 (((color&0x00ff00) >> (8 + bitsDiscarded)) << bitsReserved) |
					 ((color&0x0000ff) >> bitsDiscarded)]&0xff;
	}
	
	// Fetch the forward color map index for this RGB 
	public int getNearestColorIndex(int red, int green, int blue) {
		return invColorMap[(((red >> bitsDiscarded) << (bitsReserved<<1))) | 
					 ((green >> bitsDiscarded) << bitsReserved) |
					 (blue >> bitsDiscarded)]&0xff;
	}
	
	// Fetch the forward color map index for this RGB represented by bytes
	public int getNearestColorIndex(byte red, byte green, byte blue) {
		return invColorMap[((((red&0xff) >> bitsDiscarded) << (bitsReserved<<1))) | 
					 (((green&0xff) >> bitsDiscarded) << bitsReserved) |
					 ((blue&0xff) >> bitsDiscarded)]&0xff;
	}
	
	/**
	 * Create an inverse color map using the input forward RGB map.
	 */
	public void createInverseMap(int no_of_colors, int[] colorPalette) {   
		int red, green, blue, r, g, b;
        int rdist, gdist, bdist, dist;
        int rinc, ginc, binc;

		int x = (1 << bitsDiscarded);// Step size for each color
		int xsqr = (1 << (bitsDiscarded + bitsDiscarded));
        int txsqr = xsqr + xsqr;
		int buf_index;

        int[] dist_buf = new int[invMapLen];
		
		// Initialize the distance buffer array with the largest integer value
		for (int i = invMapLen; --i >= 0;)
			dist_buf[i] = 0x7FFFFFFF;
        // Now loop through all the colors in the color map
		for (int i = 0; i < no_of_colors; i++) {
			red   = ((colorPalette[i]>>16)&0xff);
			green = ((colorPalette[i]>>8)&0xff);
			blue  = (colorPalette[i]&0xff);
			/**
			 * We start from the origin (0,0,0) of the quantized colors, calculate
			 * the distance between the cell center of the quantized colors and
			 * the current color map entry as follows:
			 * (rcenter * x + x/2) - red, where rcenter is the center of the 
			 * Quantized red color map entry which is 0 since we start from 0.
			 */
	        rdist = (x>>1) - red;// Red distance
	        gdist = (x>>1) - green;// Green distance
	        bdist = (x>>1) - blue;// Blue distance
	        dist = rdist*rdist + gdist*gdist + bdist*bdist;//The modular
            // The distance increment with each step value x
	        rinc = txsqr - (red   << (bitsDiscarded + 1));
	        ginc = txsqr - (green << (bitsDiscarded + 1));
            binc = txsqr - (blue  << (bitsDiscarded + 1));

			buf_index = 0;
			// Loop through quantized RGB space
			for (r = 0, rdist = dist; r < maxColorVal; rdist += rinc, rinc += txsqr, r++ ) {
				for (g = 0, gdist = rdist; g < maxColorVal; gdist += ginc, ginc += txsqr, g++) {
					for (b = 0, bdist = gdist; b < maxColorVal; bdist += binc, binc += txsqr, buf_index++, b++) {
						if (bdist < dist_buf[buf_index]) {
							dist_buf[buf_index] = bdist;
							invColorMap[buf_index] = (byte)i;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Create an inverse color map using the input forward Red, Green and Blue maps.
	 */
	public void createInverseMap(int no_of_colors, byte[] redPalette, byte[] greenPalette, byte[] bluePalette) {   
		int red, green, blue, r, g, b;
        int rdist, gdist, bdist, dist;
        int rinc, ginc, binc;

		int x = (1 << bitsDiscarded);// Step size for each color
		int xsqr = (1 << (bitsDiscarded + bitsDiscarded));
        int txsqr = xsqr + xsqr;
		int buf_index;

        int[] dist_buf = new int[invMapLen];
		
		// Initialize the distance buffer array with the largest integer value
		for (int i = invMapLen; --i >= 0;)
			dist_buf[i] = 0x7FFFFFFF;
        // Now loop through all the colors in the color map
		for (int i = 0; i < no_of_colors; i++) {
			red   = (redPalette[i]&0xff);
			green = (greenPalette[i]&0xff);
			blue  = (bluePalette[i]&0xff);
			/**
			 * We start from the origin (0,0,0) of the quantized colors, calculate
			 * the distance between the cell center of the quantized colors and
			 * the current color map entry as follows:
			 * (rcenter * x + x/2) - red, where rcenter is the center of the 
			 * Quantized red color map entry which is 0 since we start from 0.
			 */
	        rdist = (x>>1) - red;// Red distance
	        gdist = (x>>1) - green;// Green distance
	        bdist = (x>>1) - blue;// Blue distance
	        dist = rdist*rdist + gdist*gdist + bdist*bdist;// The modular
            // The distance increment with each step value x
	        rinc = txsqr - (red   << (bitsDiscarded + 1));
	        ginc = txsqr - (green << (bitsDiscarded + 1));
            binc = txsqr - (blue  << (bitsDiscarded + 1));

			buf_index = 0;
			// Loop through quantized RGB space
			for (r = 0, rdist = dist; r < maxColorVal; rdist += rinc, rinc += txsqr, r++ ) {
				for (g = 0, gdist = rdist; g < maxColorVal; gdist += ginc, ginc += txsqr, g++) {
					for (b = 0, bdist = gdist; b < maxColorVal; bdist += binc, binc += txsqr, buf_index++, b++) {
						if (bdist < dist_buf[buf_index]) {
							dist_buf[buf_index] = bdist;
							invColorMap[buf_index] = (byte)i;
						}
					}
				}
			}
		}
	}
}