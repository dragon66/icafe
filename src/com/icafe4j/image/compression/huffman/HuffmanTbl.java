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

/**
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/20/2007
 */
public class HuffmanTbl {
	private byte[] BITS;
	private byte[] HUFFVAL;
	// Tables to be constructed from the above two tables
	private int HUFFSIZE[] = new int[257];
	private int HUFFCODE[] = new int[257];
	private int EHUFCO[]   = new int[257];
	private int EHUFSI[]   = new int[257];
	// Decoder tables
	private int MAXCODE[] = new int[16];
	private int MINCODE[] = new int[16];
	private int VALPTR[]  = new int[16];
	//////////////////////////////////////
	private int LASTK = 0;// no use for the decoder 

	// Create an empty Huffman table
	public HuffmanTbl() { }
	
	// Create a Huffman table with the input  BITS and HUFFVAL
	public HuffmanTbl(byte[] BITS, byte[] HUFFVAL) {
		this.BITS = BITS;
		this.HUFFVAL = HUFFVAL;
	}
	
	// Generation of table of Huffman codes (CCITT Rec. T.81(1993 E) Annex C, Page 52, Figure C.2) 
	private void generate_code_table() {
		int k = 0, code = 0;
		int size = HUFFSIZE[0];
      
		while(true) {
			HUFFCODE[k++] = code++;

			if(HUFFSIZE[k] == size) continue;

			if(HUFFSIZE[k] == 0) break;
		 
			do {
				code <<= 1;
				size++;
			} while(HUFFSIZE[k] != size);		 
		}
	}  
   
	// Generate decoder tables (CCITT Rec. T.81(1993 E) Annex F, Page 108, Figure F.15)
	private void generate_decoder_tables() {
		int I = -1, J = 0;

		while (true) {
			if (++I > 15) return;
		    
			if (BITS[I] == 0) 
			   MAXCODE[I] = -1;
	        else {
           	   VALPTR[I] = J;
		       MINCODE[I] = HUFFCODE[J];
               J += ((BITS[I]-1)&0xff);
               MAXCODE[I] = HUFFCODE[J++];
			}
		}
	}
	
	// Generation of table of Huffman code size (CCITT Rec. T.81(1993 E) Annex C, Page 51, Figure C.1) 
	private void generate_size_table() {
		int i = 1, j = 1, k = 0;
		while (i <= 16) {
			while (j <= (BITS[i-1]&0xff)) {
				HUFFSIZE[k++] = i;
				j++;
			}
			i++;
			j = 1;
		}
		HUFFSIZE[k] = 0;
		LASTK = k;//the last index of the node
	}
	
	public void generateDecoderTables()	{
		generate_size_table();
		generate_code_table();
		generate_decoder_tables();
	}
	
	public void generateEncoderTables()	{
		generate_size_table();
		generate_code_table();
		order_codes();
	}
	
	public int[] getCodeTable() {
		return HUFFCODE.clone();
	}
	
	public int[] getEncoderCodeTable() {
		return EHUFCO.clone();
	}
	
	public int[] getEncoderSizeTable() {
		return EHUFSI.clone();
	}
	
	public int[] getMaxCodeTable() {
		return MAXCODE.clone();
	}
	
	public int[] getMinCodeTable() {
		return MINCODE.clone();
	}
	
	public int[] getValPTRTable() {
		return VALPTR.clone();
	}
	
	public byte[] getValueTable() {
		return HUFFVAL.clone();
	}
	
	// Order the code and size tables
	private void order_codes() {
		int k = 0;
		int i;
		
		while(k < LASTK) {
			i = (HUFFVAL[k]&0xff);
			EHUFCO[i] = HUFFCODE[k];
			EHUFSI[i] = HUFFSIZE[k++];
		}
	}
	
	public void setBits(byte[] BITS) {
		this.BITS = BITS;
	}
	
	public void setValues(byte[] VALUE) {
		this.HUFFVAL = VALUE;
	}
}