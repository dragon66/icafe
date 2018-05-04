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
 * ImageReader.java
 *
 * Who   Date       Description
 * ====  =========  ===============================================================
 * WY    30May2015  Changed getFrames() to return an empty list instead of null
 * WY    02Jan2015  Added getFrames() and getFrameCount() for multiple frame images
 * WY    29May2015  Removed debug field, replace with logging
 */

package com.icafe4j.image.reader;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collections;
import java.util.List;

import com.icafe4j.image.ImageParam;

/** 
 * Abstract base class for image readers.
 *
 * @author Wen Yu, yuwen_66@yahoo.com 
 * @version 1.1 11/08/2012   
 */
public abstract class ImageReader {
	// Define common variables 
    protected int width;
	protected int height;
	protected int bitsPerPixel;
	protected int bytesPerScanLine;
    protected int rgbColorPalette[];
       
    protected ImageParam param = ImageParam.DEFAULT_IMAGE_PARAM;
    
    public int getFrameCount() {
    	return 0;
    }
    
    public BufferedImage getFrame(int i) {
    	return null;
	}
    
    // Return empty list instead of null
    public List<BufferedImage> getFrames() {
    	return Collections.emptyList();
    }
    
    public ImageParam getImageParam() {
		return param;
	}
        
	// Entry method, to be implemented by specific ImageReader subclass
    public abstract BufferedImage read(InputStream is) throws Exception;
} 