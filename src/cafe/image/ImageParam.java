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
 * ImageParam.java
 *
 * Who   Date       Description
 * ====  =======    ==================================================
 * WY    30Dec2014  Added new meta data fields hasICCP, containsThumbnail,
 *                  icc_profile, and thumbnails
 * WY    17Dec2014  Replaced different color type fields with ImageColorType
 */

package cafe.image;

import java.awt.image.BufferedImage;

import cafe.image.options.ImageOptions;
import cafe.util.Builder;

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
    private final int ditherThreshold;
    private final boolean transparent;
    private final int transparentColor;
    private final boolean hasICCP;
    private final byte icc_profile[];
    private final boolean containsThumbnail;    
    private final BufferedImage thumbnails[];
    
    private final ImageOptions<?> imageOptions;
    
    // DEFAULT_IMAGE_PARAM is immutable given the fact the arrays it contains are empty
    public static final ImageParam DEFAULT_IMAGE_PARAM = new ImageParamBuilder().build();
    
    private ImageParam(ImageParamBuilder builder) {
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
    
    public ImageColorType getColorType() {
    	return colorType;
    }
    
    public byte[][] getComponentColorPalette() {
    	return componentColorPalette;
    }
	
	public int getDitherThreshold() {
    	return ditherThreshold;
    }
    
    public byte[] getICCProfile() {
    	return icc_profile;
    }
    
    public int getImageHeight() {
    	return height;
    }
    
    public ImageOptions<?> getImageOptions() {
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
		private int width = 0;
		private int height = 0;
		private int bitsPerPixel = 0;
	    private ImageColorType colorType = ImageColorType.FULL_COLOR;
	    private int rgbColorPalette[];
	    private byte componentColorPalette[][]; // For RGBA separate components
	    // Whether alpha channel is included or not
	    private boolean hasAlpha = false;
	    private boolean applyDither = false;
	    private int ditherThreshold = 255;
	    // Transparency related variables
	    private boolean transparent = false;
	    private int transparentColor;
	    private boolean hasICCP = false;
	    private byte icc_profile[];
	    private boolean containsThumbnail = false;
	    private BufferedImage thumbnails[];
	 	    
	    // Additional format-specific parameters
	    private ImageOptions<?> imageOptions;
		
	    public ImageParamBuilder() {	}
	    
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
	    
	    public ImageParamBuilder ditherThreshold(int ditherThreshold) {
	    	this.ditherThreshold = ditherThreshold;
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
	    
	    public ImageParamBuilder imageOptions(ImageOptions<?> imageOptions) {
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
	    	this.ditherThreshold = 255;
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