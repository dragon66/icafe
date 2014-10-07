/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.reader;

import java.awt.image.BufferedImage;
import java.io.*;

import cafe.image.core.ImageMeta;
/** 
 * Abstract base class for image readers.
 *
 * @author Wen Yu, yuwen_66@yahoo.com 
 * @version 1.1 11/08/2012   
 */
public abstract class ImageReader
{
	// Define common variables 
    protected int width;
	protected int height;
	protected int bitsPerPixel = 0;
	protected int bytesPerScanLine = 0;
    protected int rgbColorPalette[] = null;
    
    protected ImageMeta meta = ImageMeta.DEFAULT_IMAGE_META;
    
    // Entry method, to be implemented by specific ImageReader subclass
    public abstract BufferedImage read(InputStream is) throws Exception;
        
	public ImageMeta getImageMeta() {
		return meta;
	}
} 