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
 * ImageParam.java
 *
 * Who   Date       Description
 * ====  =======    ==================================================
 * WY    06Feb2016  Added quantQuality parameter
 * WY    31Jan2016  Removed ditherThreshold parameter
 * WY    03Sep2015  Added support for different dither type
 * WY    30Dec2014  Added new meta data fields hasICCP, containsThumbnail,
 *                  icc_profile, and thumbnails
 * WY    17Dec2014  Replaced different color type fields with ImageColorType
 */

package com.icafe4j.image;

import java.awt.image.BufferedImage;

import com.icafe4j.image.options.ImageOptions;
import com.icafe4j.image.quant.DitherMethod;
import com.icafe4j.image.quant.QuantMethod;
import com.icafe4j.image.quant.QuantQuality;
import com.icafe4j.util.Builder;

/**
 * Image meta data created by internal meta data builder.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 11/06/2012
 */
public class ImageParam {
	// final fields
	private final int width;	
	private final int height;
    private final int bitsPerPixel;
    private final ImageColorType colorType;
    private final int rgbColorPalette[];
    private final byte componentColorPalette[][];
    private final boolean hasAlpha;
    private final boolean isApplyDither;
    private final int[][] ditherMatrix;
    private final DitherMethod ditherMethod;
    private final QuantMethod quantMethod;
    private final QuantQuality quantQuality;
    private final boolean transparent;
    private final int transparentColor;
    private final boolean hasICCP;
    private final byte icc_profile[];
    private final boolean containsThumbnail;    
    private final BufferedImage thumbnails[];
     
    private final ImageOptions imageOptions;
    
    // DEFAULT_IMAGE_PARAM is immutable given the fact the arrays it contains are empty
    public static final ImageParam DEFAULT_IMAGE_PARAM = new ImageParamBuilder().build();
    
    private static final DitherMethod DEFAULT_DITHER_METHOD = DitherMethod.FLOYD_STEINBERG;
	private static final QuantMethod DEFAULT_QUANT_METHOD = QuantMethod.POPULARITY;
	private static final QuantQuality DEFAULT_QUANT_QUALITY = QuantQuality.GOOD;
	
    // Default Bayer 8X8 threshold matrix for ordered dither
    private static final int[][] DEFAULT_DITHER_MATRIX = {
			{ 1, 49, 13, 61,  4, 52, 16, 64},
			{ 33, 17, 45, 29, 36, 20, 48, 32},
			{  9, 57,  5, 53, 12, 60,  8, 56},
			{ 41, 25, 37, 21, 44, 28, 40, 24},
			{  3, 51, 15, 63,  2, 50, 14, 62},
			{ 35, 19, 47, 31, 34, 18, 46, 30},
			{ 11, 59,  7, 55, 10, 58,  6, 54},
			{ 43, 27, 39, 23, 42, 26, 38, 22}
	};
    
    private ImageParam(ImageParamBuilder builder) {
		width = builder.width;
		height = builder.height;
		bitsPerPixel = builder.bitsPerPixel;
		colorType = builder.colorType;
		rgbColorPalette = builder.rgbColorPalette;
		componentColorPalette = builder.componentColorPalette;
		hasAlpha = builder.hasAlpha;
		isApplyDither = builder.applyDither;
		ditherMatrix = builder.ditherMatrix;
		ditherMethod = builder.ditherMethod;
		quantMethod = builder.quantMethod;
		quantQuality = builder.quantQuality;
		transparentColor = builder.transparentColor;
		transparent = builder.transparent;
		hasICCP = builder.hasICCP;
		icc_profile = builder.icc_profile;
		containsThumbnail = builder.containsThumbnail;
		thumbnails = builder.thumbnails;		
		imageOptions = builder.imageOptions;
	}
    
    public boolean containsThumbnail() {
    	return containsThumbnail;
    }
    
    public int getBitsPerPixel() {
    	return bitsPerPixel;
    }
    
    public static ImageParamBuilder getBuilder() {
    	return new ImageParamBuilder();
    }
    
    public ImageColorType getColorType() {
    	return colorType;
    }
    
    public byte[][] getComponentColorPalette() {
    	return componentColorPalette;
    }
	
    public int[][] getDitherMatrix() {
    	int[][] copy = new int[ditherMatrix.length][];
    	for(int i = 0; i < ditherMatrix.length; i++) {
    		copy[i] = ditherMatrix[i].clone();
    	}
    	return copy;
    }
    
    public DitherMethod getDitherMethod() {
    	return ditherMethod;
    }
    
	public QuantMethod getQuantMethod() {
		return quantMethod;
	}
	
	public QuantQuality getQuantQuality() {
		return quantQuality;
	}
    
    public byte[] getICCProfile() {
    	return icc_profile;
    }
    
    public int getImageHeight() {
    	return height;
    }
    
    public ImageOptions getImageOptions() {
    	return imageOptions;
    }
    
    public int getImageWidth() {
    	return width;
    }
    
    public int[] getRGBColorPalette() {
    	return rgbColorPalette;
    }
    
    public BufferedImage[] getThumbnails() {
    	return thumbnails;
    }
    
    public int getTransparentColor() {
		return transparentColor;
	}
    
    /**
	 * Specifies whether or not to include an alpha channel with true-color images.
	 * Only a few image formats support full alpha channel among which are PNG, TGA, and TIFF.
	 */
    public boolean hasAlpha() {
		return hasAlpha;
	}
    
    public boolean hasICCP() {
    	return hasICCP;
    }
    
	public boolean isApplyDither() {
    	return isApplyDither;
    }
	
	/**
	 * Specifies whether or not single-color transparency is set. If true,
	 * the subsequent call to getTransparentColor will get the transparent
	 * color as an integer.
	 */	
	public boolean isTransparent() {
		return transparent;
	}
	
	// Internal parameter data builder
	public static class ImageParamBuilder implements Builder<ImageParam> {
		// Parameters - initialized to default values
		// Common parameters for all image formats
		private int width;
		private int height;
		private int bitsPerPixel;
	    private ImageColorType colorType = ImageColorType.FULL_COLOR;
	    private int rgbColorPalette[];
	    private byte componentColorPalette[][]; // For RGBA separate components
	    // Whether alpha channel is included or not
	    private boolean hasAlpha = false;
	    private boolean applyDither = false;
	    private DitherMethod ditherMethod = DEFAULT_DITHER_METHOD;
	    private QuantMethod quantMethod = DEFAULT_QUANT_METHOD;
	    private QuantQuality quantQuality = DEFAULT_QUANT_QUALITY;
	    // Bayer 8X8 matrix
	    private int[][] ditherMatrix = DEFAULT_DITHER_MATRIX;
	    // Transparency related variables
	    private boolean transparent = false;
	    private int transparentColor;
	    private boolean hasICCP = false;
	    private byte icc_profile[];
	    private boolean containsThumbnail = false;
	    private BufferedImage thumbnails[];
	 	    
	    // Additional format-specific parameters
	    private ImageOptions imageOptions;
		
	    private ImageParamBuilder() {;}
	    
	    public ImageParamBuilder applyDither(boolean applyDither) {
	    	this.applyDither = applyDither;
	    	return this;
	    }
	    
	    public ImageParamBuilder bitsPerPixel(int bitsPerPixel) {
			this.bitsPerPixel = bitsPerPixel;
			return this; 
		}
		
	    public ImageParam build() {
		   return new ImageParam(this);
		}
	    
	    public ImageParamBuilder colorType(ImageColorType colorType) {
	    	this.colorType = colorType;
	    	return this;
	    }
	    
	    public ImageParamBuilder componentColorPalette(byte[][] componentPalette) {
	    	this.componentColorPalette = componentPalette;
	    	return this;
	    }
	    
	    public ImageParamBuilder containsThumbnail(boolean containsThumbnail) {
	    	this.containsThumbnail = containsThumbnail;
	    	return this;
	    }
	    
	    public ImageParamBuilder ditherMatrix(int[][] ditherMatrix) {
	    	this.ditherMatrix = ditherMatrix;
	    	return this;
	    }
	    
	    public ImageParamBuilder ditherMethod(DitherMethod ditherMethod) {
	    	this.ditherMethod = ditherMethod;
	    	return this;
	    }
	    
	    public ImageParamBuilder quantMethod(QuantMethod quantMethod) {
	    	this.quantMethod = quantMethod;
	    	return this;
	    }
	    
	    public ImageParamBuilder quantQuanlity(QuantQuality quantQuality) {
	    	this.quantQuality = quantQuality;
	    	return this;
	    }
	    
	    public ImageParamBuilder hasAlpha(boolean hasAlpha) {
			this.hasAlpha = hasAlpha;
			return this;
		}
		    		
	    public ImageParamBuilder hasICCP(boolean hasICCP) {
	    	this.hasICCP = hasICCP;
	    	return this;
	    }
	    
	    public ImageParamBuilder height(int height) {
	    	this.height = height;
	    	return this;
	    }
	    
	    public ImageParamBuilder iccProfile(byte[] icc_profile) {
	    	this.icc_profile = icc_profile;
	    	return this;
	    }
	    
	    public ImageParamBuilder imageOptions(ImageOptions imageOptions) {
			this.imageOptions = imageOptions;
			return this;
		}
	    
		/**
	     * ImageReader can reset this ImageBuilder to read another image.
	     */
	    public void reset() {
	    	this.width = 0;
	    	this.height = 0;
	      	this.bitsPerPixel = 0;
	    	this.colorType = ImageColorType.FULL_COLOR;
	    	this.rgbColorPalette = null;
	    	this.componentColorPalette = null;
	    	this.hasAlpha = false;
	    	this.applyDither = false;
	      	this.ditherMatrix = DEFAULT_DITHER_MATRIX;
	      	this.ditherMethod = DEFAULT_DITHER_METHOD;
	       	this.quantMethod = DEFAULT_QUANT_METHOD;
	       	this.quantQuality = DEFAULT_QUANT_QUALITY;
	      	this.transparent = false;
	    	this.transparentColor = 0;
	    	this.hasICCP = false;
	    	this.icc_profile = null;
	    	this.containsThumbnail = false;
	    	this.thumbnails = null;
	    	this.imageOptions = null;
	    }
		
		public ImageParamBuilder rgbColorPalette(int[] rgbColorPalette) {
			this.rgbColorPalette = rgbColorPalette;
			return this;
		}
		
		public ImageParamBuilder thumbnails(BufferedImage[] thumbnails) {
	    	this.thumbnails = thumbnails;
	    	return this;
	    }
		
		public ImageParamBuilder transparent(boolean transparent) {
			this.transparent = transparent;
			return this;
		}
		
		public ImageParamBuilder transparentColor(int transparentColor) {
			this.transparentColor = transparentColor;
			return this;
		}
		
		public ImageParamBuilder width(int width) {
	    	this.width = width; 
	    	return this; 
	    }		
	} // End of internal builder	
}