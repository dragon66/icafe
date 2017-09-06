/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * Int32ComponentColorModel.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    13Nov2014  Initial creation
 */

package com.icafe4j.image.color;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;

// Based on HaraldK's answer to one of the StackOverFlow questions I raised:
// http://stackoverflow.com/questions/26875429/how-to-create-bufferedimage-for-32-bits-per-sample-3-samples-image-data
/**
 * A workaround for the bug of Java ComponentColorModel cannot handle 32 bit
 * sample correctly.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 11/13/2014
 */
public class Int32ComponentColorModel extends ComponentColorModel {
	//
	public Int32ComponentColorModel(ColorSpace cs, boolean alpha) {
		super(cs, alpha, false, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE, DataBuffer.TYPE_INT);
	}

	@Override
	public float[] getNormalizedComponents(Object pixel, float[] normComponents, int normOffset) {
		int numComponents = getNumComponents();
		
		if (normComponents == null || normComponents.length < numComponents + normOffset) {
			normComponents = new float[numComponents + normOffset];
		}

		switch (transferType) {
			case DataBuffer.TYPE_INT:
				int[] ipixel = (int[]) pixel;
				for (int c = 0, nc = normOffset; c < numComponents; c++, nc++) {
					normComponents[nc] = ipixel[c] / ((float) ((1L << getComponentSize(c)) - 1));
				}
				break;
			default: // I don't think we can ever come this far. Just in case!!!
				throw new UnsupportedOperationException("This method has not been implemented for transferType " + transferType);
        }

        return normComponents;
    }
}