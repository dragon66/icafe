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
 * FilterMask.java - Adobe Photoshop Document Data Block LMsk
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    28Jul2015  Initial creation
 */

package com.icafe4j.image.meta.adobe;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.io.ReadStrategy;

public class FilterMask extends DDBEntry {
	private int colorSpaceId;
	private int[] colors = new int[4];
	private int opacity;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterMask.class);
	
	public FilterMask(int size, byte[] data, ReadStrategy readStrategy) {
		super(DataBlockType.FMsk, size, data, readStrategy);
		read();
	}
	
	public int[] getColors() {
		return colors.clone();
	}
	
	public int getOpacity() {
		return opacity;
	}

	public int getColorSpace() {
		return colorSpaceId;
	}
	
	public ColorSpaceID getColorSpaceID() {
		return ColorSpaceID.fromInt(colorSpaceId);
	}
	
	public void print() {
		super.print();
		LOGGER.info("Color space: {}", getColorSpaceID());
		LOGGER.info("Color values: {}", Arrays.toString(colors));
		LOGGER.info("Opacity: {}", opacity);
	}
	
	private void read() {
		int i = 0;
		colorSpaceId = readStrategy.readShort(data, i);
		i += 2;
		colors[0] = readStrategy.readUnsignedShort(data, i);
		i += 2;
		colors[1] = readStrategy.readUnsignedShort(data, i);
		i += 2;
		colors[2] = readStrategy.readUnsignedShort(data, i);
		i += 2;
		colors[3] = readStrategy.readUnsignedShort(data, i);
		i += 2;
		opacity = readStrategy.readShort(data, i);
	}
}