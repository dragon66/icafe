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

import com.icafe4j.util.Builder;

/**
 * PNG IHDR chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/25/2013
 */
public class IHDRBuilder extends ChunkBuilder implements Builder<Chunk> {
	/*
	 * Color   Allowed         Interpretation
   	 * Type    Bit Depths
   	 *
   	 *	0      1, 2, 4, 8, 16  Each pixel is a grayscale sample.
     *
   	 *	2      8, 16           Each pixel is an R,G,B triple.
     *
   	 *	3      1, 2, 4, 8      Each pixel is a palette index;
     *                         a PLTE chunk must appear.
     *
   	 *	4      8, 16           Each pixel is a grayscale sample,
     *                         followed by an alpha sample.
     *
   	 *	6      8, 16           Each pixel is an R,G,B triple,
     *                         followed by an alpha sample.
	 */
	private int width = 0;
	private int height = 0;
	private int bitDepth = 0;
	private int colorType = 0;
	private int compressionMethod = 0;
	private int filterMethod = 0;
	private int interlaceMethod = 0;

	public IHDRBuilder width(int width) {
		if (width <= 0) throw new IllegalArgumentException("Invalid width: " + width);
		this.width = width;
		return this;
	}
	
	public IHDRBuilder height(int height) {
		if (height <= 0) throw new IllegalArgumentException("Invalid height: " + height);
		this.height = height;
		return this;
	}
	
	public IHDRBuilder bitDepth(int bitDepth) {
		switch(bitDepth) {
			case 1:
			case 2:
			case 4:
			case 8:
			case 16:
				this.bitDepth = bitDepth;
				return this;
			default:
				throw new IllegalArgumentException("Invalid bitDepth: " + bitDepth);
		}		
	}
	
	public IHDRBuilder colorType(ColorType colorType) {
		switch(colorType) {
			case GRAY_SCALE:
			case TRUE_COLOR:
			case INDEX_COLOR:
			case GRAY_SCALE_WITH_ALPHA:
			case TRUE_COLOR_WITH_ALPHA:
				this.colorType = colorType.getValue();
				return this;
			default:
				throw new IllegalArgumentException("Invalid colorType: " + colorType);
		}		
	}
	
	// Only compression method 0 => deflate/inflate is allowed.
	public IHDRBuilder compressionMethod(int compressionMethod) {
		if (compressionMethod != 0) throw new IllegalArgumentException("Invalid comressionMethod" + compressionMethod);
		this.compressionMethod = compressionMethod;
		return this;
	}
	
	// Only filter method 0 (adaptive filtering with five basic filter types) is defined.
	public IHDRBuilder filterMethod(int filterMethod) {
		if(filterMethod != 0) throw new IllegalArgumentException("Invalid filterMethod: " + filterMethod);
		this.filterMethod = filterMethod;
		return this;
	}
	
	// 0 => no interlace; 1 => Adam7 interlace
	public IHDRBuilder interlaceMethod(int interlaceMethod) {
		if ((interlaceMethod != 0) && (interlaceMethod != 1) )throw new IllegalArgumentException("Invalid interlaceMethod" + interlaceMethod);
		this.interlaceMethod = interlaceMethod;
		return this;
	}
	
	public IHDRBuilder() {
		super(ChunkType.IHDR);		
	}	

	@Override
	protected byte[] buildData() {
		// 13 bytes
		byte[] data = {(byte)(width >>> 24),
		         (byte)(width >>> 16), (byte)(width >>> 8),
		         (byte)width, (byte)(height >>> 24),
		         (byte)(height >>> 16), (byte)(height >>> 8),
		         (byte)height, (byte)bitDepth, (byte)colorType, (byte)compressionMethod,
		         (byte)filterMethod, (byte)interlaceMethod };
		
		return data;
	}
}
