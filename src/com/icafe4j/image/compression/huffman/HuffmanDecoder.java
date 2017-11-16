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

package com.icafe4j.image.compression.huffman;

import java.io.*;

/**
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/20/2007
 */
public class HuffmanDecoder {
	// Define number of lines
	private static final int DNL  = 0xDC; 
   
	private InputStream is;
	
	private int[] VALPTR;
	private int[] MINCODE;
	private int[] MAXCODE;
	private byte[] HUFFVAL;
		
	private int CNT = 0;
	private int ZZ[] = new int[64];

	// CCITT Rec. T.81(1992 E) Annex F, Page 109, Figure F.16
	public HuffmanDecoder(InputStream is, HuffmanTbl ht) {
		this.is = is;
		this.VALPTR  = ht.getValPTRTable();
		this.MINCODE = ht.getMinCodeTable();
		this.MAXCODE = ht.getMaxCodeTable();
		this.HUFFVAL = ht.getValueTable();
	}

	private int DECODE() {
		int CODE, I = 1, J;
		CODE = NEXTBIT();
    
	    while (CODE > MAXCODE[I]) {
		   I++;
		   CODE = (CODE<<1) + NEXTBIT();
	    }

        J = VALPTR[I] + CODE - MINCODE[I];
        
        return (HUFFVAL[J]&0xff);
	}
	
	// CCITT Rec. T.81(1992 E) Annex F, Page 106, Figure F.13
	public void decode_AC_coefficients() {
		int K = 1;
		int RS, SSSS, R;
	    
		while (true) {
			RS = DECODE();
			SSSS = (RS%16);
			R = (RS>>4);
            
			if (SSSS == 0) {
				if (R == 15) {
					K += 16;
					continue;
				}
				
				break;
			}

			K += R;
            decode_ZZ(K,SSSS);
				
            if (K != 63) K++;
				
			break;			
		}
    }
	
	// CCITT Rec. T.81(1992 E) Annex F, Page 107, Figure F.14
    private void decode_ZZ(int K, int SSSS) {
		ZZ[K] = RECEIVE(SSSS);
		ZZ[K] = EXTEND(ZZ[K],SSSS);
    }
    
	// CCITT Rec. T.81(1992 E) Annex F, Page 105, Figure F.12
	private static int EXTEND(int V, int T) {
		int Vt = (1<<(T-1));

		if (V < Vt) {
			Vt = ((-1)<<T)+1;
			V += Vt; 
		}

		return V;
    }
	
	// CCITT Rec. T.81(1992 E) Annex F, Page 111, Figure F.18
	private int NEXTBIT() {
		int BIT, B = 0, B2;

		if (CNT == 0) {
			B = NEXTBYTE();
			CNT = 8;

			if (B == 0xFF) {
				B2 = NEXTBYTE();
				
				if (B2 != 0) {
					if (B2==DNL) {
						// do_DNL();
						// terminate scan;
					} else; // showError();
				}
			}
		}

		BIT = (B>>7);
		CNT--;
		B <<= 1;

		return BIT;
	}
	
	private int NEXTBYTE() {
        int readByte;
		
		try {
        	readByte = is.read();
        } catch (IOException ioe) {
			throw new RuntimeException("Error reading NEXTBYTE...");
        }
		
		return readByte;
	}

	// CCITT Rec. T.81(1992 E) Annex F, Page 110, Figure F.17
	private int RECEIVE(int SSSS) {
		int I = 0, V = 0;

		while (I != SSSS) {
			I++;
			V= (V<<1) + NEXTBIT();
		}

		return V;
    }
}