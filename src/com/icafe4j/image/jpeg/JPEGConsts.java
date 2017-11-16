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

package com.icafe4j.image.jpeg;

/**
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/16/2012
 */
public class JPEGConsts {
	public static final int SUBSAMPLING_NONE = 0; // aka 1x1 (4:4:4)
	public static final int SUBSAMPLING_422  = 1; // aka 2x1
	public static final int SUBSAMPLING_420  = 2; // aka 2x2
	
   /**
    * This is the order in which the after-DCT 8x8 block is traversed 
    * used for reordering the QT(quantization Table) and the 
    * zigzag traversed blocks
	*/
   private static final int[] ZIGZAG_TRAVERSE_ORDER = {
	   0,  1,  8, 16,  9,  2,  3, 10,
      17, 24, 32, 25, 18, 11,  4,  5,
      12, 19, 26, 33, 40, 48, 41, 34,
      27, 20, 13,  6,  7, 14, 21, 28,
      35, 42, 49, 56, 57, 50, 43, 36,
      29, 22, 15, 23, 30, 37, 44, 51,
      58, 59, 52, 45, 38, 31, 39, 46,
      53, 60, 61, 54, 47, 55, 62, 63
   };
   
   // Reverses ZigZag reordering of the quantization Table
   private static final int[] DE_ZIGZAG_TRAVERSE_ORDER = {
       0,  1,  5,  6, 14, 15, 27, 28,
       2,  4,  7, 13, 16, 26, 29, 42,
       3,  8, 12, 17, 25, 30, 41, 43,
       9, 11, 18, 24, 31, 40, 44, 53,
      10, 19, 23, 32, 39, 45, 52, 54,
      20, 22, 33, 38, 46, 51, 55, 60,
      21, 34, 37, 47, 50, 56, 59, 61,
      35, 36, 48, 49, 57, 58, 62, 63
   };
   
   /**
    *  This is the default quantization table for luminance
    *  ISO/IEC 10918-1 : 1993(E), Annex, Table K.1
    */
   private static final int[] QUANT_LUMINANCE = {
      16, 11, 10, 16, 24, 40, 51, 61,
      12, 12, 14, 19, 26, 58, 60, 55,
      14, 13, 16, 24, 40, 57, 69, 56,
      14, 17, 22, 29, 51, 87, 80, 62,
      18, 22, 37, 56, 68, 109, 103, 77,
      24, 35, 55, 64, 81, 104, 113, 92,
      49, 64, 78, 87, 103, 121, 120, 101,
      72, 92, 95, 98, 112, 100, 103, 99
   };
   
   /**
    *  This is the default quantization table for chrominance
    *  ISO/IEC 10918-1 : 1993(E), Annex, KTable K.2
    */
   private static final int[] QUANT_CHROMINANCE = {
      17, 18, 24, 47, 99, 99, 99, 99,
      18, 21, 26, 66, 99, 99, 99, 99,
      24, 26, 56, 99, 99, 99, 99, 99,
      47, 66, 99, 99, 99, 99, 99, 99,
      99, 99, 99, 99, 99, 99, 99, 99,
      99, 99, 99, 99, 99, 99, 99, 99,
      99, 99, 99, 99, 99, 99, 99, 99,
      99, 99, 99, 99, 99, 99, 99, 99
   };  
   
   private static final byte[] DC_LUMINANCE_BITS = {0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0};
   
   private static final byte[] DC_LUMINANCE_VALUES = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b};
   
   private static final byte[] DC_CHROMINANCE_BITS = {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
   
   private static final byte[] DC_CHROMINANCE_VALUES = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b};
   
   private static final byte[] AC_LUMINANCE_BITS = {0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 125};
   
   private static final byte[] AC_LUMINANCE_VALUES = {0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21,
	   0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07, 0x22, 0x71, 0x14, 0x32, (byte)0x81, (byte)0x91, (byte)0xA1,
	   0x08, 0x23, 0x42, (byte)0xB1, (byte)0xC1, 0x15, 0x52, (byte)0xD1, (byte)0xF0, 0x24, 0x33, 0x62, 0x72,
	   (byte)0x82, 0x09, 0x0A, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x34, 0x35,
	   0x36, 0x37, 0x38, 0x39, 0x3A, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x53, 0x54, 0x55, 0x56,
	   0x57, 0x58, 0x59, 0x5A, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x73, 0x74, 0x75, 0x76, 0x77,
	   0x78, 0x79, 0x7A, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87, (byte)0x88, (byte)0x89,
	   (byte)0x8A, (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x96, (byte)0x97, (byte)0x98, 
	   (byte)0x99, (byte)0x9A, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5, (byte)0xA6, (byte)0xA7,
	   (byte)0xA8, (byte)0xA9, (byte)0xAA, (byte)0xB2, (byte)0xB3, (byte)0xB4, (byte)0xB5, (byte)0xB6,
	   (byte)0xB7, (byte)0xB8, (byte)0xB9, (byte)0xBA, (byte)0xC2, (byte)0xC3, (byte)0xC4, (byte)0xC5,
	   (byte)0xC6, (byte)0xC7, (byte)0xC8, (byte)0xC9, (byte)0xCA, (byte)0xD2, (byte)0xD3, (byte)0xD4,
	   (byte)0xD5, (byte)0xD6, (byte)0xD7, (byte)0xD8, (byte)0xD9, (byte)0xDA, (byte)0xE1, (byte)0xE2,
	   (byte)0xE3, (byte)0xE4, (byte)0xE5, (byte)0xE6, (byte)0xE7, (byte)0xE8, (byte)0xE9, (byte)0xEA,
	   (byte)0xF1, (byte)0xF2, (byte)0xF3, (byte)0xF4, (byte)0xF5, (byte)0xF6, (byte)0xF7, (byte)0xF8,
	   (byte)0xF9, (byte)0xFA
   };
   
   private static final byte[] AC_CHROMINANCE_BITS = {0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 119};
   
   private static final byte[] AC_CHROMINANCE_VALUES = {0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21,
	   0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71, 0x13, 0x22, (byte)0x32, (byte)0x81, (byte)0x08,
	   0x14, 0x42, (byte)0x91, (byte)0xA1, (byte)0xB1, (byte)0xC1, 0x09, 0x23, 0x33, 0x52, (byte)0xF0,
	   0x15, 0x62, 0x72, (byte)0xD1, 0x0A, 0x16, 0x24, 0x34, (byte)0xE1, 0x25, (byte)0xF1,
	   0x17, 0x18, 0x19, 0x1A, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x43,
	   0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x63,
	   0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A,
	   (byte)0x82, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87, (byte)0x88, (byte)0x89,
	   (byte)0x8A, (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x96, (byte)0x97, (byte)0x98,
	   (byte)0x99, (byte)0x9A, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5, (byte)0xA6, (byte)0xA7,
	   (byte)0xA8, (byte)0xA9, (byte)0xAA, (byte)0xB2, (byte)0xB3, (byte)0xB4, (byte)0xB5, (byte)0xB6,
	   (byte)0xB7, (byte)0xB8, (byte)0xB9, (byte)0xBA, (byte)0xC2, (byte)0xC3, (byte)0xC4, (byte)0xC5,
	   (byte)0xC6, (byte)0xC7, (byte)0xC8, (byte)0xC9, (byte)0xCA, (byte)0xD2, (byte)0xD3, (byte)0xD4,
	   (byte)0xD5, (byte)0xD6, (byte)0xD7, (byte)0xD8, (byte)0xD9, (byte)0xDA, (byte)0xE2, (byte)0xE3,
	   (byte)0xE4, (byte)0xE5, (byte)0xE6, (byte)0xE7, (byte)0xE8, (byte)0xE9, (byte)0xEA, (byte)0xF2,
	   (byte)0xF3, (byte)0xF4, (byte)0xF5, (byte)0xF6, (byte)0xF7, (byte)0xF8, (byte)0xF9, (byte)0xFA
   };
   
   public static final byte[] getACChrominanceBits() {
	   return AC_CHROMINANCE_BITS.clone();
   }
   
   public static final byte[] getACChrominanceValues() {
	   return AC_CHROMINANCE_VALUES.clone();
   }
   
   public static final byte[] getACLuminanceBits() {
	   return AC_LUMINANCE_BITS.clone();
   }
   
   public static final byte[] getACLuminanceValues() {
	   return AC_LUMINANCE_VALUES.clone();
   }
   
   public static final byte[] getDCChrominanceBits() {
	   return DC_CHROMINANCE_BITS.clone();
   }
   
   public static final byte[] getDCChrominanceValues() {
	   return DC_CHROMINANCE_VALUES.clone();
   }
   
   public static final byte[] getDCLuminanceBits() {
	   return DC_LUMINANCE_BITS.clone();
   }
   
   public static final byte[] getDCLuminanceValues() {
	   return DC_LUMINANCE_VALUES.clone();
   }
   
   public static final int[] getDefaultChrominanceMatrix(int quality) {
	   //
	   int[] quant_chrominance = QUANT_CHROMINANCE.clone();
	   
	   if (quality <= 0)
           quality = 1;
	   if (quality > 100)
           quality = 100;
	   if (quality < 50)
           quality = 5000 / quality;
	   else
           quality = 200 - quality * 2;
	   
	   for (int j = 0; j < 64; j++) {
               int temp = (quant_chrominance[j] * quality + 50) / 100;
               if ( temp <= 0) temp = 1;
               if (temp >= 255) temp = 255;
               quant_chrominance[j] = temp;
       }
	   
	   return quant_chrominance;
   }
   
   public static final int[] getDefaultLuminanceMatrix(int quality) {
	   //
	   int[] quant_luminance = QUANT_LUMINANCE.clone();
	   
	   if (quality <= 0)
           quality = 1;
	   if (quality > 100)
           quality = 100;
	   if (quality < 50)
           quality = 5000 / quality;
	   else
           quality = 200 - quality * 2;
	   
	   for (int j = 0; j < 64; j++)
       {
               int temp = (quant_luminance[j] * quality + 50) / 100;
               if ( temp <= 0) temp = 1;
               if (temp >= 255) temp = 255;
               quant_luminance[j] = temp;
       }
	   
	   return quant_luminance;
   }
   
   public static final int[] getDeZigzagMatrix() {
	   return DE_ZIGZAG_TRAVERSE_ORDER.clone();
   }
   
   public static final int[] getZigzagMatrix() {
	   return ZIGZAG_TRAVERSE_ORDER.clone();
   }
   
   private JPEGConsts() {}
}  
