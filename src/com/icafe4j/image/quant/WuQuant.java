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
 * WuQuant.java
 *
 * Who   Date       Description
 * ====  =========  ====================================================
 * WY    24Sep2015  Revised to take care of transparent color
 * WY    12Sep2015  Initial creation
 */

package com.icafe4j.image.quant;

/**
 * Java port of
 * C Implementation of Wu's Color Quantizer (v. 2)
 * (see Graphics Gems vol. II, pp. 126-133)
 * Author:	Xiaolin Wu
 * Dept. of Computer Science
 * Univ. of Western Ontario
 * London, Ontario N6A 5B7
 * wu@csd.uwo.ca
 * 
 * Algorithm: Greedy orthogonal bipartition of RGB space for variance
 * minimization aided by inclusion-exclusion tricks.
 * For speed no nearest neighbor search is done. Slightly
 * better performance can be expected by more sophisticated
 * but more expensive versions.
 * 
 * The author thanks Tom Lane at Tom_Lane@G.GP.CS.CMU.EDU for much of
 * additional documentation and a cure to a previous bug.
 * 
 * Free to distribute, comments and suggestions are appreciated.
 */
public class WuQuant {
	private static final int MAXCOLOR =	256;
	private static final int RED = 2;
	private static final int GREEN = 1;
	private static final int BLUE =	0;
	
	private static int QUANT_SIZE = 33;// quant size

	private static final class Box {
		int r0;	 /* min value, exclusive */
		int r1;	 /* max value, inclusive */
		int g0;  
		int g1;  
		int b0;  
		int b1;
		int vol;
	};
	
	private int	size; /*image size*/
	private int	lut_size; /*color look-up table size*/
	private int qadd[];
	private int pixels[];
	private int transparent_color = -1;// Transparent color 
	
    private float m2[][][] = new float[QUANT_SIZE][QUANT_SIZE][QUANT_SIZE];
    private long wt[][][] = new long[QUANT_SIZE][QUANT_SIZE][QUANT_SIZE];
    private long mr[][][] = new long[QUANT_SIZE][QUANT_SIZE][QUANT_SIZE];
    private long mg[][][] = new long[QUANT_SIZE][QUANT_SIZE][QUANT_SIZE];
    private long mb[][][] = new long[QUANT_SIZE][QUANT_SIZE][QUANT_SIZE];
       
    public WuQuant(int[] pixels, int lut_size) {
    	this.pixels = pixels;
    	this.size = pixels.length;
    	this.lut_size = lut_size;
    }
    
    public int quantize(final byte[] newPixels, final int[] lut, int[] colorInfo) {
       Box cube[] = new Box[MAXCOLOR];
       int lut_r, lut_g, lut_b;
       int tag[] = new int[QUANT_SIZE*QUANT_SIZE*QUANT_SIZE];

       int next, i, k;
       long weight;
       float vv[] = new float[MAXCOLOR], temp;
       
       Hist3d(wt, mr, mg, mb, m2);
       M3d(wt, mr, mg, mb, m2);
       
       for(i = 0; i < MAXCOLOR; i++)
    	   cube[i] = new Box();
       
       cube[0].r0 = cube[0].g0 = cube[0].b0 = 0;
       cube[0].r1 = cube[0].g1 = cube[0].b1 = QUANT_SIZE - 1;
       next = 0;
       
       if(transparent_color >= 0) lut_size--;
       
       for(i = 1; i < lut_size; ++i){
    	   if (Cut(cube[next], cube[i])) {
    		   /* volume test ensures we won't try to cut one-cell box */
    		   vv[next] = (cube[next].vol > 1) ? Var(cube[next]) : 0.0f;
    		   vv[i] = (cube[i].vol > 1) ? Var(cube[i]) : 0.0f;
    	   } else {
    		   vv[next] = 0.0f;   /* don't try to split this box again */
    		   i--;              /* didn't create box i */
    	   }
    	   next = 0; temp = vv[0];
    	   for(k = 1; k <= i; ++k)
    		   if (vv[k] > temp) {
    			   temp = vv[k]; next = k;
    		   }
    	   if (temp <= 0.0f) {
    		   k = i + 1;
    		   break;
    	   }
       }
   
       for(k = 0; k < lut_size; ++k){
    	   Mark(cube[k], k, tag);
    	   weight = Vol(cube[k], wt);
    	   if (weight > 0) {
    		   lut_r = (int)(Vol(cube[k], mr) / weight);
    		   lut_g = (int)(Vol(cube[k], mg) / weight);
    		   lut_b = (int)(Vol(cube[k], mb) / weight);
    		   lut[k] = (255 << 24) | (lut_r  << 16) | (lut_g << 8) | lut_b;
    	   }
    	   else	{
      		   lut[k] = 0;
    	   }
       }

       for(i = 0; i < size; ++i) {
    	   if((pixels[i] >>> 24) < 0x80)
    		   newPixels[i] = (byte)lut_size;
    	   else
    		   newPixels[i] = (byte)tag[qadd[i]];
       }
       
       int bitsPerPixel = 0;
       while ((1<<bitsPerPixel) < lut_size)  bitsPerPixel++;
       colorInfo[0] = bitsPerPixel;
       colorInfo[1] = -1;       
       
       if(transparent_color >= 0) {
     	  lut[lut_size] = transparent_color; // Set the transparent color
     	  colorInfo[1] = lut_size;
       }
       
       return lut_size;
    }
    
    public int quantize(final int[] lut, int[] colorInfo) {
       Box cube[] = new Box[MAXCOLOR];
       int lut_r, lut_g, lut_b;
     
       int next, i, k;
       long weight;
       float vv[] = new float[MAXCOLOR], temp;
       
       Hist3d(wt, mr, mg, mb, m2);
       M3d(wt, mr, mg, mb, m2);
       
       for(i = 0; i < MAXCOLOR; i++)
    	   cube[i] = new Box();
       
       cube[0].r0 = cube[0].g0 = cube[0].b0 = 0;
       cube[0].r1 = cube[0].g1 = cube[0].b1 = QUANT_SIZE - 1;
       next = 0;
       
       if(transparent_color >= 0) lut_size--;
       
       for(i = 1; i < lut_size; ++i){
    	   if (Cut(cube[next], cube[i])) {
    		   /* volume test ensures we won't try to cut one-cell box */
    		   vv[next] = (cube[next].vol > 1) ? Var(cube[next]) : 0.0f;
    		   vv[i] = (cube[i].vol > 1) ? Var(cube[i]) : 0.0f;
    	   } else {
    		   vv[next] = 0.0f;   /* don't try to split this box again */
    		   i--;              /* didn't create box i */
    	   }
    	   next = 0; temp = vv[0];
    	   for(k = 1; k <= i; ++k)
    		   if (vv[k] > temp) {
    			   temp = vv[k]; next = k;
    		   }
    	   if (temp <= 0.0f) {
    		   k = i + 1;
    		   break;
    	   }
       }
   
       for(k = 0; k < lut_size; ++k){
    	   weight = Vol(cube[k], wt);
    	   if (weight > 0) {
    		   lut_r = (int)(Vol(cube[k], mr) / weight);
    		   lut_g = (int)(Vol(cube[k], mg) / weight);
    		   lut_b = (int)(Vol(cube[k], mb) / weight);
    		   lut[k] = (255 << 24) | (lut_r  << 16) | (lut_g << 8) | lut_b;
    	   }
    	   else	{
      		   lut[k] = 0;		
    	   }
       }
       
       int bitsPerPixel = 0;
       while ((1<<bitsPerPixel) < lut_size)  bitsPerPixel++;
       colorInfo[0] = bitsPerPixel;
       colorInfo[1] = -1;
       
       if(transparent_color >= 0) {
      	  lut[lut_size] = transparent_color; // Set the transparent color
      	  colorInfo[1] = lut_size;
       }
       
       return lut_size;
    }

	/* Histogram is in elements 1..HISTSIZE along each axis,
	 * element 0 is for base or marginal value
	 * NB: these must start out 0!
	 */
	private void Hist3d(long vwt[][][], long vmr[][][], long vmg[][][], long vmb[][][], float m2[][][]) {
		/* build 3-D color histogram of counts, r/g/b, c^2 */
		int r, g, b;
		int	i, inr, ing, inb, table[] = new int[256];
	
		for(i = 0; i < 256; ++i) table[i]= i*i;
		
		qadd = new int[size];
	
		for(i = 0; i < size; ++i) {
			int rgb = pixels[i];
			if((rgb >>> 24) < 0x80) { // Transparent
				if (transparent_color < 0)	// Find the transparent color	
					transparent_color = rgb;
			}
			r = ((rgb >> 16)& 0xff);
			g = ((rgb >> 8 )& 0xff);
			b = ( rgb       & 0xff);
			inr = (r >> 3) + 1; 
			ing = (g >> 3) + 1; 
			inb = (b >> 3) + 1; 
			qadd[i] = (inr << 10) + (inr << 6) + inr + (ing << 5) + ing + inb;
			/*[inr][ing][inb]*/
			++vwt[inr][ing][inb];
			vmr[inr][ing][inb] += r;
			vmg[inr][ing][inb] += g;
			vmb[inr][ing][inb] += b;
		    m2[inr][ing][inb] += table[r] + table[g] + table[b];
		}
	}
	
	/* At conclusion of the histogram step, we can interpret
	 *   wt[r][g][b] = sum over voxel of P(c)
	 *   mr[r][g][b] = sum over voxel of r*P(c)  ,  similarly for mg, mb
	 *   m2[r][g][b] = sum over voxel of c^2*P(c)
	 * Actually each of these should be divided by 'size' to give the usual
	 * interpretation of P() as ranging from 0 to 1, but we needn't do that here.
	*/

	/* We now convert histogram into moments so that we can rapidly calculate
	 * the sums of the above quantities over any desired box.
	 */
	private void M3d(long vwt[][][], long vmr[][][], long vmg[][][], long vmb[][][], float m2[][][]) {
		/* compute cumulative moments. */
		int i, r, g, b;
		int line, line_r, line_g, line_b;
		int area[] = new int[QUANT_SIZE];
		int area_r[] = new int[QUANT_SIZE];
		int area_g[] = new int[QUANT_SIZE];
		int area_b[] = new int[QUANT_SIZE];
		float line2, area2[] = new float[QUANT_SIZE];
	
		for(r = 1; r < QUANT_SIZE; ++r) {
			for(i = 0; i < QUANT_SIZE; ++i) 
				area2[i] = area[i] = area_r[i] = area_g[i] = area_b[i] = 0;
			for(g = 1; g < QUANT_SIZE; ++g) {
				line2 = line = line_r = line_g = line_b = 0;
				for(b = 1; b < QUANT_SIZE; ++b){
					line   += vwt[r][g][b];
					line_r += vmr[r][g][b]; 
					line_g += vmg[r][g][b]; 
					line_b += vmb[r][g][b];
					line2  += m2[r][g][b];
					
					area[b] += line;
					area_r[b] += line_r;
					area_g[b] += line_g;
					area_b[b] += line_b;
					area2[b] += line2;
					
					vwt[r][g][b] = vwt[r-1][g][b] + area[b];
					vmr[r][g][b] = vmr[r-1][g][b] + area_r[b];
					vmg[r][g][b] = vmg[r-1][g][b] + area_g[b];
					vmb[r][g][b] = vmb[r-1][g][b] + area_b[b];
					m2[r][g][b]  = m2[r-1][g][b]  + area2[b];
				}
			}			
		}
	}
	
	private long Vol(Box cube, long mmt[][][]) {
		/* Compute sum over a box of any given statistic */
		return ( mmt[cube.r1][cube.g1][cube.b1] 
				-mmt[cube.r1][cube.g1][cube.b0]
				-mmt[cube.r1][cube.g0][cube.b1]
				+mmt[cube.r1][cube.g0][cube.b0]
				-mmt[cube.r0][cube.g1][cube.b1]
				+mmt[cube.r0][cube.g1][cube.b0]
				+mmt[cube.r0][cube.g0][cube.b1]
				-mmt[cube.r0][cube.g0][cube.b0] );
	}
	
	/* The next two routines allow a slightly more efficient calculation
	* of Vol() for a proposed subbox of a given box.  The sum of Top()
	* and Bottom() is the Vol() of a subbox split in the given direction
	* and with the specified new upper bound.
	*/
	
	private long Bottom(Box cube, int dir, long mmt[][][]) {
		/* Compute part of Vol(cube, mmt) that doesn't depend on r1, g1, or b1 */
		/* (depending on dir) */
		switch(dir) {
			case RED:
				return( -mmt[cube.r0][cube.g1][cube.b1]
						+mmt[cube.r0][cube.g1][cube.b0]
						+mmt[cube.r0][cube.g0][cube.b1]
						-mmt[cube.r0][cube.g0][cube.b0] );
			case GREEN:
				return( -mmt[cube.r1][cube.g0][cube.b1]
						+mmt[cube.r1][cube.g0][cube.b0]
						+mmt[cube.r0][cube.g0][cube.b1]
						-mmt[cube.r0][cube.g0][cube.b0] );
			case BLUE:
				return( -mmt[cube.r1][cube.g1][cube.b0]
						+mmt[cube.r1][cube.g0][cube.b0]
						+mmt[cube.r0][cube.g1][cube.b0]
						-mmt[cube.r0][cube.g0][cube.b0] );
			default:
				return 0;
		}
	}

	private long Top(Box cube, int dir, int pos, long mmt[][][]) {
		/* Compute remainder of Vol(cube, mmt), substituting pos for */
		/* r1, g1, or b1 (depending on dir) */
		switch(dir) {
			case RED:
				return( mmt[pos][cube.g1][cube.b1] 
				   -mmt[pos][cube.g1][cube.b0]
				   -mmt[pos][cube.g0][cube.b1]
				   +mmt[pos][cube.g0][cube.b0] );
			case GREEN:
				return( mmt[cube.r1][pos][cube.b1] 
				   -mmt[cube.r1][pos][cube.b0]
				   -mmt[cube.r0][pos][cube.b1]
				   +mmt[cube.r0][pos][cube.b0] );
			case BLUE:
				return( mmt[cube.r1][cube.g1][pos]
				   -mmt[cube.r1][cube.g0][pos]
				   -mmt[cube.r0][cube.g1][pos]
				   +mmt[cube.r0][cube.g0][pos] );
			default:
				return 0;
		}
	}
	
	private float Var(Box cube) {
		/* Compute the weighted variance of a box */
		/* NB: as with the raw statistics, this is really the variance * size */
		float dr, dg, db, xx;
		dr = Vol(cube, mr); 
		dg = Vol(cube, mg); 
		db = Vol(cube, mb);
		xx =  m2[cube.r1][cube.g1][cube.b1] 
			  -m2[cube.r1][cube.g1][cube.b0]
			  -m2[cube.r1][cube.g0][cube.b1]
			  +m2[cube.r1][cube.g0][cube.b0]
			  -m2[cube.r0][cube.g1][cube.b1]
			  +m2[cube.r0][cube.g1][cube.b0]
			  +m2[cube.r0][cube.g0][cube.b1]
			  -m2[cube.r0][cube.g0][cube.b0];
		return  xx - (dr*dr + dg*dg + db*db)/Vol(cube,wt);    
	}

	/* We want to minimize the sum of the variances of two subboxes.
	* The sum(c^2) terms can be ignored since their sum over both subboxes
	* is the same (the sum for the whole box) no matter where we split.
	* The remaining terms have a minus sign in the variance formula,
	* so we drop the minus sign and MAXIMIZE the sum of the two terms.
	*/
	private float Maximize(Box cube, int dir, int first, int last, int cut[],
			long whole_r, long whole_g, long whole_b, long whole_w) {
		long half_r, half_g, half_b, half_w;
		long base_r, base_g, base_b, base_w;
		int i;
		float temp, max;

		base_r = Bottom(cube, dir, mr);
		base_g = Bottom(cube, dir, mg);
		base_b = Bottom(cube, dir, mb);
		base_w = Bottom(cube, dir, wt);
		
		max = 0.0f;
		cut[0] = -1;
	
		for(i = first; i < last; ++i){
			half_r = base_r + Top(cube, dir, i, mr);
			half_g = base_g + Top(cube, dir, i, mg);
			half_b = base_b + Top(cube, dir, i, mb);
			half_w = base_w + Top(cube, dir, i, wt);
			/* now half_x is sum over lower half of box, if split at i */
			if (half_w == 0) /* subbox could be empty of pixels! */
				continue;    /* never split into an empty box */
			temp = (half_r*half_r + half_g*half_g +	half_b*half_b)/(float)half_w;
			half_r = whole_r - half_r;
			half_g = whole_g - half_g;
			half_b = whole_b - half_b;
			half_w = whole_w - half_w;
			if (half_w == 0) /* subbox could be empty of pixels! */
				continue; /* never split into an empty box */
			temp += (half_r*half_r + half_g*half_g + half_b*half_b)/(float)half_w;
			
			if (temp > max) { max = temp; cut[0] = i;}
		}
		
		return max;
	}
	
	private boolean Cut(Box set1, Box set2) {
		int dir;
		int cutr[] = new int[1];
        int cutg[] = new int[1];
        int cutb[] = new int[1];
		float maxr, maxg, maxb;
		long whole_r, whole_g, whole_b, whole_w;

		whole_r = Vol(set1, mr);
		whole_g = Vol(set1, mg);
		whole_b = Vol(set1, mb);
		whole_w = Vol(set1, wt);

		maxr = Maximize(set1, RED, set1.r0 + 1, set1.r1, cutr,
				whole_r, whole_g, whole_b, whole_w);
		maxg = Maximize(set1, GREEN, set1.g0 + 1, set1.g1, cutg,
				whole_r, whole_g, whole_b, whole_w);
		maxb = Maximize(set1, BLUE, set1.b0 + 1, set1.b1, cutb,
				whole_r, whole_g, whole_b, whole_w);

		if(maxr >= maxg && maxr >= maxb) {
			dir = RED;
			if (cutr[0] < 0) return false; /* can't split the box */
		} else if(maxg >= maxr && maxg >= maxb) 
			dir = GREEN;
		else
			dir = BLUE; 

		set2.r1 = set1.r1;
		set2.g1 = set1.g1;
		set2.b1 = set1.b1;

		switch (dir){
			case RED:
				set2.r0 = set1.r1 = cutr[0];
				set2.g0 = set1.g0;
				set2.b0 = set1.b0;
				break;
			case GREEN:
				set2.g0 = set1.g1 = cutg[0];
				set2.r0 = set1.r0;
				set2.b0 = set1.b0;
				break;
			case BLUE:
				set2.b0 = set1.b1 = cutb[0];
				set2.r0 = set1.r0;
				set2.g0 = set1.g0;
				break;
		}
		set1.vol = (set1.r1 - set1.r0)*(set1.g1 - set1.g0)*(set1.b1 - set1.b0);
		set2.vol = (set2.r1 - set2.r0)*(set2.g1 - set2.g0)*(set2.b1 - set2.b0);
	
		return true;  
	}
	
	private void Mark(Box cube, int label, int tag[]) {
		int r, g, b;

		for(r = cube.r0 + 1; r <= cube.r1; ++r)
			for(g = cube.g0 + 1; g <= cube.g1; ++g)
				for(b = cube.b0 + 1; b <= cube.b1; ++b)
					tag[(r<<10) + (r<<6) + r + (g<<5) + g + b] = label;
	}
}