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

/**
 * A common interface for T4 black and white codes
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/20/2013
 */
public interface T4Code {
	//
	public short getCode();
	public int getCodeLen();
	public int getRunLen();
	
	// A better practice is to make this one private with a public getter method returning a clone of the array.
	// But this is not possible for an interface. 
	public final static int[] runLenArray = {
		0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28,
		29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 48, 49, 50, 51, 52, 53,
		54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 128, 192, 256, 320, 384, 448, 512, 576, 640, 704, 768, 832,
		896, 960, 1024, 1088, 1152, 1216, 1280, 1344, 1408, 1472, 1536, 1600, 1664, 1728, 1792, 1856, 1920, 1984,
		2048, 2112, 2176, 2240, 2304, 2368, 2432, 2496, 2560
	};
	
	public static final short EOL = (short)0x0010; // Code length 12
	public static final short EOL_PLUS_ONE = (short)0x0018; // Code length 13
	public static final short EOL_PLUS_ZERO = (short)0x0010; // Code length 13
}