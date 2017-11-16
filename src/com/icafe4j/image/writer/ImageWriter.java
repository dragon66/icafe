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

package com.icafe4j.image.writer;

import java.io.*;
import java.awt.*;
import java.awt.image.*;

import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.util.IMGUtils;

/** 
 * The template class for other image writers
 * This class defines a set of common data and
 * methods for all image writers.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com  
 * @version 1.2 08/01/2013
 */
public abstract class ImageWriter {
	private ImageParam imageParam = ImageParam.DEFAULT_IMAGE_PARAM;
	
	protected ImageWriter() {}
	
	protected ImageWriter(ImageParam param) {
		this.imageParam = param;
	}
	
	private int[] getPixels(Image img, int imageWidth, int imageHeight) throws Exception {	
		int[] pixels = null;
		
		if(img instanceof BufferedImage) {
			pixels = IMGUtils.getRGB((BufferedImage)img);
		} else {	 
			pixels = new int[imageWidth * imageHeight];
			PixelGrabber pg = new PixelGrabber(img, 0, 0, imageWidth, imageHeight, pixels, 0, imageWidth);
		
		    try {
		    	pg.grabPixels();
		    }
		    catch (InterruptedException e) {
		        System.err.println("interrupted waiting for pixels!");
		    }
		    
		    if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
		    	System.err.println("image fetch aborted or errored");
			}
		}
	    
	    if(imageParam.isTransparent()) {
	    	int transColor = (imageParam.getTransparentColor() & 0x00ffffff);
		
			for(int i = pixels.length - 1; i > 0; i--) {
				int pixel = (pixels[i] & 0x00ffffff);
				if(pixel == transColor) pixels[i] = pixel; 
			}
	    }
	    
	    return pixels;
	}
	
	public ImageParam getImageParam() {
		return imageParam;
	}
	
	public abstract ImageType getImageType();
	
	public void setImageParam(ImageParam imageParam) {
		this.imageParam = imageParam;
	}
	
	public void write(Image img, OutputStream os) throws Exception {
		int imageWidth = img.getWidth(null);
		int imageHeight = img.getHeight(null);
		
		write(getPixels(img, imageWidth, imageHeight), imageWidth, imageHeight, os);
	}
	
	/**
	 * The actual image writing method to be implemented by any specific ImageWriter subclass
	 * 
	 * @param pixels input image array in ARGB format
	 * @param imageWidth image width
	 * @param imageHeight image height
	 * @param os OutputSteam to write the image
	 * @throws Exception
	 */
	protected abstract void write(int[] pixels, int imageWidth, 
	                 int imageHeight, OutputStream os) throws Exception;	
}