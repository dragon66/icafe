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

package com.icafe4j.image.png;

/**
 * PNG scan line filter
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/29/2013
 */
public class Filter {
	
	public static final int NONE = 0;
	public static final int SUB = 1;
	public static final int UP = 2;
	public static final int AVERAGE = 3;
	public static final int PAETH = 4;
	
	public static void defilter_average(int bytesPerPixel, int bytesPerScanLine, byte[] sample, int offset)
	{		
		int previous = 0;
		int upper = 0;
		
		int end = offset + bytesPerScanLine;
		int subStart = offset + bytesPerPixel;
	
		// First scan line, only previous bytes of the same line is used
		if(offset < bytesPerScanLine) 
		{
			for (int i = subStart; i < end; i++)
			{
				previous = (sample[i-bytesPerPixel]&0xff);
				sample[i] = (byte)((sample[i]&0xff) + (previous>>1));
			}
	
			return;			
		}
		
		// Only upper line bytes are used
		for (int i = offset; i < subStart; i++) 
		{
			upper = (sample[i-bytesPerScanLine]&0xff);			
			sample[i] = (byte)((sample[i]&0xff) + (upper>>1));
		}
		
		// Both upper line and previous bytes of the same line are used.
		for (int i = subStart; i < end; i++) 
		{
			upper = (sample[i-bytesPerScanLine]&0xff);			
			previous = (sample[i-bytesPerPixel]&0xff);
			sample[i] = (byte)((sample[i]&0xff) + ((upper + previous)>>1));
		}
	}
	
	public static void defilter_paeth(int bytesPerPixel, int bytesPerScanLine, byte[] sample, int offset)
	{
		int previous = 0;
		int upper = 0;
		int upper_previous = 0;
		
		int end = offset + bytesPerScanLine;
		int subStart = offset + bytesPerPixel;
		
		// First scan line, only previous bytes of the same line is used
		if(offset < bytesPerScanLine) 
		{
			for (int i = subStart; i < end; i++)
			{
				previous = (sample[i-bytesPerPixel]&0xff);
				sample[i] = (byte)((sample[i]&0xff) + previous);
			}
	
			return;			
		}
		
		// Only upper line bytes are used
		for (int i = offset; i < subStart; i++) 
		{
			upper = (sample[i-bytesPerScanLine]&0xff);			
			sample[i] = (byte)((sample[i]&0xff) + upper);
		}
		
		for (int i = subStart; i < end; i++)
		{
			upper = (sample[i-bytesPerScanLine]&0xff);
			previous = (sample[i-bytesPerPixel]&0xff);
	        upper_previous = (sample[i-bytesPerScanLine-bytesPerPixel]&0xff);
	        sample[i] = (byte)((sample[i]&0xff) + paeth_predictor(previous,upper,upper_previous));			 
		}
	}
	
	public static void defilter_sub(int bytesPerPixel, int bytesPerScanLine, byte[] sample, int offset)
	{
		int end = offset + bytesPerScanLine;
	
		for (int i = offset + bytesPerPixel; i < end; i++)
		{
			sample[i] = (byte)((sample[i]&0xff) + (sample[i-bytesPerPixel]&0xff));
		}
	}
	
	public static void defilter_up(int bytesPerScanLine, byte[] sample, int offset)
	{
		if (offset < bytesPerScanLine) { // up is meaningless for the first row
			return;
		}
		
		int end = offset + bytesPerScanLine;
	
		for (int i = offset; i < end; i++)
		{
			sample[i] = (byte)((sample[i]&0xff) + (sample[i-bytesPerScanLine]&0xff));
		}
	}

	public static void filter_average(int bytesPerPixel, int bytesPerScanLine, byte[] sample, int offset)
	{	
		int previous = 0;
		int upper = 0;
		
		int end = offset + bytesPerScanLine;
		int subStart = offset + bytesPerPixel;
	
		// First scan line, only previous bytes of the same line is used
		if(offset < bytesPerScanLine) 
		{
			for (int i = end - 1; i >= subStart; i--)
			{
				previous = (sample[i-bytesPerPixel]&0xff);
				sample[i] = (byte)((sample[i]&0xff) - (previous>>1));
			}
	
			return;			
		}
		
		// Both upper line and previous bytes of the same line are used.
		for (int i = end - 1; i >= subStart; i--) 
		{
			upper = (sample[i-bytesPerScanLine]&0xff);			
			previous = (sample[i-bytesPerPixel]&0xff);
			sample[i] = (byte)((sample[i]&0xff) - ((upper + previous)>>1));
		}
		
		// Only upper line bytes are used
		for (int i = subStart - 1; i >= offset; i--) 
		{
			upper = (sample[i-bytesPerScanLine]&0xff);			
			sample[i] = (byte)((sample[i]&0xff) - (upper>>1));
		}	
	}
	
	public static void filter_paeth(int bytesPerPixel, int bytesPerScanLine, byte[] sample, int offset)
	{
		int previous = 0;
		int upper = 0;
		int upper_left = 0;
			
		int subStart = offset + bytesPerPixel;
		int end = offset + bytesPerScanLine;		
	
		if (offset < bytesPerScanLine) { // First line
			for (int i = end - 1; i >= subStart; i--)
			{
				previous = (sample[i - bytesPerPixel]&0xff);
				sample[i] = (byte)((sample[i]&0xff) - previous);
			}
			
			return;
		}
		
		// Use previous, upper and upper_left bytes
		for (int i = end - 1; i >= subStart; i--)
		{
			upper = (sample[i-bytesPerScanLine]&0xff);
			previous = (sample[i-bytesPerPixel]&0xff);
	        upper_left = (sample[i-bytesPerScanLine-bytesPerPixel]&0xff);
	        sample[i] = (byte)((sample[i]&0xff) - paeth_predictor(previous, upper, upper_left));			 
		}
		
		// Only upper line bytes are used
		for (int i = subStart - 1; i >= offset; i--) 
		{
			upper = (sample[i-bytesPerScanLine]&0xff);			
			sample[i] = (byte)((sample[i]&0xff) -upper);
		}
	}
	
	public static void filter_sub(int bytesPerPixel, int bytesPerScanLine, byte[] sample, int offset)
	{
		int start = offset + bytesPerPixel;
		int end = offset + bytesPerScanLine;
	
		for (int i = end - 1; i >= start; i--)
		{
			sample[i] = (byte)((sample[i]&0xff) - (sample[i-bytesPerPixel]&0xff));
		}
	}
	
	public static void filter_up(int bytesPerScanLine, byte[] sample, int offset)
	{
		if (offset < bytesPerScanLine) { // up is meaningless for the first row
			return;
		}
		
		int start  = offset - bytesPerScanLine;
		int end = offset + bytesPerScanLine;
	
		for (int i = offset; i < end; i++)
		{		
			sample[i] = (byte)((sample[i]&0xff) - (sample[start++]&0xff));
		}
	}
	
	private static int paeth_predictor(int left, int above, int upper_left)
	{
		int p = left+above-upper_left;
		int p_left = (p>left)?(p - left):(left-p);
		int p_above = (p>above)?(p - above):(above-p);
		int p_upper_left = (p>upper_left)?(p - upper_left):(upper_left-p);
		
		if ((p_left<=p_above)&&(p_left<=p_upper_left))
			 return left;
		else if (p_above<=p_upper_left)
			 return above;
		else return upper_left;
	}
	
	private Filter() { }
}
