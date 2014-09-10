/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.jpeg;

import java.io.IOException;

import cafe.io.IOUtils;
import cafe.util.Reader;

/**
 * JPEG SOF0 segment reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/09/2013
 */
public class SOF0Reader implements Reader {

	private int precision;
	private int imageHeight;
	private int imageWidth;
	private int numOfComponents;
	private Component[] components;
	
	private Segment segment;
	
	public SOF0Reader(Segment segment) throws IOException {
		//
		if(segment.getMarker() != Marker.SOF0) {
			throw new IllegalArgumentException("Not a valid SOF0 segment!");
		}
		
		this.segment = segment;
		read();
	}
	
	public int getLength() {
		return segment.getLength();
	}
	
	public int getPrecision() {
		return precision;
	}
	
	public int getImageHeight() {
		return imageHeight;
	}
	
	public int getImageWidth() {
		return imageWidth;
	}
	
	public int getNumOfComponents() {
		return numOfComponents;
	}
	
	public Component[] getComponents() {
		return components.clone();
	}
	
	@Override
	public void read() throws IOException {
		//
		byte[] data = segment.getData();
		precision = data[0];
		imageHeight = IOUtils.readUnsignedShortMM(data, 1);
		imageWidth = IOUtils.readUnsignedShortMM(data, 3);
		
		numOfComponents = data[5];
		components = new Component[numOfComponents];
	
		int offset = 6;
		
		for (int i = 0; i < numOfComponents; i++) {
			byte componentId = data[offset++];		
			byte sampleFactor = data[offset++];
			byte hSampleFactor = (byte)((sampleFactor>>4)&0x0f);
			byte vSampleFactor = (byte)((sampleFactor&0x0f));
			byte qTableNumber = data[offset++];
					
			components[i] = new Component(componentId, hSampleFactor, vSampleFactor, qTableNumber);
		}
	}
}
