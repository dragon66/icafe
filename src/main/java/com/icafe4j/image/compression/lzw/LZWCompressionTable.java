/*
 * @(#)From GIFEncoder.java 0.90 4/21/96 Adam Doppelt
 *
 * @version 0.90 21 Apr 1996
 * @author <A HREF="http://www.cs.brown.edu/people/amd/">Adam Doppelt</A> 
 */

package com.icafe4j.image.compression.lzw;

public class LZWCompressionTable {
    private final static int RES_CODES = 2;
    private final static short EMPTY = (short)0xFFFF;
    private final static int MAXBITS = 12;
    private final static int MAXSTR = (1 << MAXBITS);
    private final static short HASHSIZE = 9973;
    private final static short HASHSTEP = 2039;

    byte strChr_[];
    short strPref_[];
    short strHsh_[];
    short numStrings_;

    public LZWCompressionTable() {
    	strChr_ = new byte[MAXSTR];
	    strPref_ = new short[MAXSTR];
	    strHsh_ = new short[HASHSIZE]; 
    }
    
    //Return the current code value
    public int addCharString(short prefix, byte b) {
	   int hshidx;

	   if (numStrings_ >= MAXSTR)
	      return 0xFFFF;
	
	   hshidx = hash(prefix, b);
	   while (strHsh_[hshidx] != EMPTY)
	      hshidx = (hshidx + HASHSTEP) % HASHSIZE;
	
	   strHsh_[hshidx] = numStrings_;
	   strChr_[numStrings_] = b;
	   strPref_[numStrings_] = prefix;

	   return numStrings_++;
    }
    
    //Return the current string
    public short findCharString(short prefix, byte b) {
	   int hshidx, nxtidx;

	   if (prefix == EMPTY)//Single character string
	      return b;

	   hshidx = hash(prefix, b);
	   while ((nxtidx = strHsh_[hshidx]) != EMPTY) {
	      if (strPref_[nxtidx] == prefix && strChr_[nxtidx] == b)
		     return (short)nxtidx;//Found it
	      hshidx = (hshidx + HASHSTEP) % HASHSIZE;//Linear probing hash
	   }
	   
	   return EMPTY;
    }

    public void clearTable(int codesize) {
	   numStrings_ = 0;
	
	   for (int q = 0; q < HASHSIZE; q++) {
	      strHsh_[q] = EMPTY;
	   } 

	   int w = (1 << codesize) + RES_CODES;
	   for (int q = 0; q < w; q++)
	      addCharString(EMPTY, (byte)q);
    }
    
    private static int hash(short prefix, byte lastbyte) {
	   return (((short)(lastbyte << 8) ^ prefix) & 0xFFFF) % HASHSIZE;
    }
}