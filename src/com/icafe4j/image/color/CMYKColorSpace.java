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
 * CMYKColorSpace.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    22Oct2014  Initial creation to read CMYK TIFF image
 */

package com.icafe4j.image.color;


import java.awt.color.ColorSpace;

/**
 * CMYK color space to work with CMYK image without embedded ICC_Profile.
 * Conversion to and from RGB using simple formula.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/22/2014
 */
public class CMYKColorSpace extends ColorSpace {
	// TODO add ICC_Profile support - ICC_ColorSpace.toRGB() is way too slow
	// to make it feasible to add ICC_Profile to this class. If we have embedded
	// ICC_Profile, we would do a color conversion to RGB elsewhere instead
   	private static final long serialVersionUID = -4823887516599874355L;
	private static CMYKColorSpace instance;

    /**
     * Returns an instance of CMYKColorSpace.
     * @return an instance of CMYKColorSpace
     */
    public static CMYKColorSpace getInstance() {
        if (instance == null) {
            instance = new CMYKColorSpace(TYPE_CMYK, 4);
        }
        return instance;
    }

    /**
     * @see java.awt.color.ColorSpace#ColorSpace(int, int)
     */
    protected CMYKColorSpace(int type, int numcomponents) {
        super(type, numcomponents);
    }

    /**
     * @see java.awt.color.ColorSpace#fromCIEXYZ(float[])
     */
    public float[] fromCIEXYZ(float[] colorvalue) {
        throw new UnsupportedOperationException("fromCIEXYZ is not implemented");
    }

    /**
     * @see java.awt.color.ColorSpace#fromRGB(float[])
     */
    public float[] fromRGB(float[] rgb) {
    	float r = rgb[0];
    	float g = rgb[1];
    	float b = rgb[2];
    	
    	float c = 1 - r;
    	float m = 1 - g;
	    float y = 1 - b;
	    
	    float tempK = 1.0f;
	        
	    if(c < tempK) tempK = c;
	    if(m < tempK) tempK = m;
	    if(y < tempK) tempK = y;
	        
	    if(tempK == 1.0f) c = m = y = 0.f;
	    else {
	    	c = (c - tempK)/(1 - tempK);
	    	m = (m - tempK)/(1 - tempK);
	    	y = (y - tempK)/(1 - tempK);
	    }
	    
	    return new float[] {c, m, y, tempK};
    }

    /**
     * @see java.awt.color.ColorSpace#toCIEXYZ(float[])
     */
    public float[] toCIEXYZ(float[] colorvalue) {
        throw new UnsupportedOperationException("toCIEXYZ is not implemented");
    }

    /**
     * @see java.awt.color.ColorSpace#toRGB(float[])
     */
    public float[] toRGB(float[] cmyk) {
        float c = cmyk[0];
        float m = cmyk[1];
        float y = cmyk[2];
        float k = cmyk[3];
        
        if (k < 1.0f) {
        	c = c * (1.0f - k) + k;
        	m = m * (1.0f - k) + k;
        	y = y * (1.0f - k) + k;
        } else {
        	c = m = y = 1.0f;
        }
      
        return new float [] { 1 - c, 1 - m, 1 - y};        		
     }
}