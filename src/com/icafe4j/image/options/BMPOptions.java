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

package com.icafe4j.image.options;

import com.icafe4j.image.ImageType;
import com.icafe4j.image.bmp.BmpCompression;

public class BMPOptions extends ImageOptions {
	// Image alignment
    public static final int ALIGN_BOTTOM_UP = 0; // Height > 0
    public static final int ALIGN_TOP_DOWN  = 1; // Height < 0
    
    private int alignment = ALIGN_BOTTOM_UP;
    private BmpCompression bmpCompression = BmpCompression.BI_RGB;
    
	@Override
	public ImageType getImageType() {
		return ImageType.BMP;
	}
	
	public int getAlignment() {
		return alignment;
	}
	
	public BmpCompression getBmpComression() {
		return bmpCompression;
	}
	
	public void setAlignment(int alignment) {
		if(alignment != ALIGN_BOTTOM_UP && alignment != ALIGN_TOP_DOWN)
			throw new IllegalArgumentException("Invalid alignment: " + alignment);
		this.alignment = alignment;
	}
	
	public void setBmpCompression(BmpCompression bmpCompression) {
		this.bmpCompression = bmpCompression;
	}
}