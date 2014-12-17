/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * ImageMeta.java
 *
 * Who   Date       Description
 * ====  =======    ==================================================
 * WY    17Dec2014  Replaced different color type fields with ColorType
 */

package cafe.image.core;

import cafe.image.options.ImageOptions;
import cafe.util.Builder;

/**
 * Image meta data created by internal meta data builder.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 11/06/2012
 */
public class ImageMeta {	
	// final fields
	private final int width;
	private final int height;	
	private final int bitsPerPixel;
    private final ColorType colorType;
    private final int rgbColorPalette[];
    private final byte componentColorPalette[][];
    private final boolean hasAlpha;
    private final boolean isApplyDither;
    private final int ditherThreshold;
    private final boolean transparent;
    private final int transparentColor;
    private final ImageOptions imageOptions;

    // DEFAULT_IMAGE_META is immutable given the fact the arrays it contains are empty
    public static final ImageMeta DEFAULT_IMAGE_META = new ImageMetaBuilder().build();
    
    private ImageMeta(ImageMetaBuilder builder) {
		width = builder.width;
		height = builder.height;
		bitsPerPixel = builder.bitsPerPixel;
		colorType = builder.colorType;
		rgbColorPalette = builder.rgbColorPalette;
		componentColorPalette = builder.componentColorPalette;
		hasAlpha = builder.hasAlpha;
		isApplyDither = builder.applyDither;
		ditherThreshold = builder.ditherThreshold;
		transparentColor = builder.transparentColor;
		transparent = builder.transparent;
		imageOptions = builder.imageOptions;
	}
    
    public int getBitsPerPixel() {
    	return bitsPerPixel;
    }
    
    public int[] getRGBColorPalette() {
    	return rgbColorPalette;
    }
    
    public byte[][] getComponentColorPalette() {
    	return componentColorPalette;
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
    
    public boolean isApplyDither() {
    	return isApplyDither;
    }
    
    public int getDitherThreshold() {
    	return ditherThreshold;
    }
    
    public ColorType getColorType() {
    	return colorType;
    }
    
	/**
	 * Specifies whether or not single-color transparency is set. If true,
	 * the subsequent call to getTransparentColor will get the transparent
	 * color as an integer.
	 */	
	public boolean isTransparent() {
		return transparent;
	}
	
	// Internal meta data builder
	public static class ImageMetaBuilder implements Builder<ImageMeta> {
		// Parameters - initialized to default values
		// Common parameters for all image formats
		private int width = 0;
		private int height = 0;
		private int bitsPerPixel = 0;
	    private ColorType colorType = ColorType.FULL_COLOR;
	    private int rgbColorPalette[];
	    private byte componentColorPalette[][]; // For RGBA separate components
	    // Whether alpha channel is included or not
	    private boolean hasAlpha = false;
	    private boolean applyDither = false;
	    private int ditherThreshold = 255;
	    // Transparency related variables
	    private boolean transparent = false;
	    private int transparentColor;
	 	    
	    // Additional format-specific parameters
	    private ImageOptions imageOptions;
		
	    public ImageMetaBuilder() {	}
	    
	    public ImageMetaBuilder colorType(ColorType colorType) {
	    	this.colorType = colorType;
	    	return this;
	    }
	    
	    public ImageMetaBuilder bitsPerPixel(int bitsPerPixel) {
			this.bitsPerPixel = bitsPerPixel;
			return this; 
		}
		
	    public ImageMeta build() {
		   return new ImageMeta(this);
		}
	    
	    public ImageMetaBuilder rgbColorPalette(int[] rgbColorPalette) {
			this.rgbColorPalette = rgbColorPalette;
			return this;
		}
	    
	    public ImageMetaBuilder componentColorPalette(byte[][] componentPalette) {
	    	this.componentColorPalette = componentPalette;
	    	return this;
	    }
		    		
	    public ImageMetaBuilder hasAlpha(boolean hasAlpha) {
			this.hasAlpha = hasAlpha;
			return this;
		}
	    
	    public ImageMetaBuilder applyDither(boolean applyDither) {
	    	this.applyDither = applyDither;
	    	return this;
	    }
	    
	    public ImageMetaBuilder ditherThreshold(int ditherThreshold) {
	    	this.ditherThreshold = ditherThreshold;
	    	return this;
	    }
	    
		public ImageMetaBuilder height(int height) {
	    	this.height = height;
	    	return this;
	    }
		
		public ImageMetaBuilder imageOptions(ImageOptions imageOptions) {
			this.imageOptions = imageOptions;
			return this;
		}
		
		public ImageMetaBuilder transparent(boolean transparent) {
			this.transparent = transparent;
			return this;
		}
		
		/**
	     * ImageReader can reset this ImageBuilder to read another image.
	     */
	    public void reset() {
	    	this.width = 0;
	    	this.height = 0;
	      	this.bitsPerPixel = 0;
	    	this.colorType = ColorType.FULL_COLOR;
	    	this.rgbColorPalette = null;
	    	this.componentColorPalette = null;
	    	this.hasAlpha = false;
	    	this.applyDither = false;
	    	this.ditherThreshold = 255;
	    	this.transparent = false;
	    	this.transparentColor = 0;
	    	this.imageOptions = null;
	    }
		
		public ImageMetaBuilder transparentColor(int transparentColor) {
			this.transparentColor = transparentColor;
			return this;
		}
		
		public ImageMetaBuilder width(int width) {
	    	this.width = width; 
	    	return this; 
	    }		
	} // End of internal builder
}