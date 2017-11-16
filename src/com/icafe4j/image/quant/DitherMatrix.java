/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * DitherMatrix.java
 *
 * Who   Date       Description
 * ====  =========  ====================================================
 * WY    26Oct2015  Initial creation
 */

package com.icafe4j.image.quant;

/**
 * Predefined ordered dither matrices
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 */
public class DitherMatrix {
	 // Default Bayer 8X8 threshold matrix for ordered dither
    private static final int[][] BAYER8X8DEFAULT = {
			{ 1, 49, 13, 61,  4, 52, 16, 64},
			{ 33, 17, 45, 29, 36, 20, 48, 32},
			{  9, 57,  5, 53, 12, 60,  8, 56},
			{ 41, 25, 37, 21, 44, 28, 40, 24},
			{  3, 51, 15, 63,  2, 50, 14, 62},
			{ 35, 19, 47, 31, 34, 18, 46, 30},
			{ 11, 59,  7, 55, 10, 58,  6, 54},
			{ 43, 27, 39, 23, 42, 26, 38, 22}
	};
    
    // Bayer 8X8 diagonal variant threshold matrix for ordered
    // dither - perfect for gradient texture
    private static final int[][] BAYER8X8DIAG = {
	    	{24, 10, 12, 26, 35, 47, 49, 37},
	    	{ 8,  0,  2, 14, 45, 59, 61, 51},
	    	{22,  6,  4, 16, 43, 57, 63, 53},
	      	{30, 20, 18, 28, 33, 41, 55, 39},
	        {34, 46, 48, 36, 25, 11, 13, 27},
	        {44, 58, 60, 50,  9,  1,  3, 15},
	      	{42, 56, 62, 52, 23,  7,  5, 17},
	       	{32, 40, 54, 38, 31, 21, 19, 29}
	};
    
    public static int[][] getBayer8x8Default() {
    	int[][] copy = new int[BAYER8X8DEFAULT.length][];
    	for(int i = 0; i < BAYER8X8DEFAULT.length; i++) {
    		copy[i] = BAYER8X8DEFAULT[i].clone();
    	}
    	return copy;
    }
    
    public static int[][] getBayer8x8Diag() {
    	int[][] copy = new int[BAYER8X8DIAG.length][];
    	for(int i = 0; i < BAYER8X8DIAG.length; i++) {
    		copy[i] = BAYER8X8DIAG[i].clone();
    	}
    	return copy;
    }
    
    private DitherMatrix() {}
}