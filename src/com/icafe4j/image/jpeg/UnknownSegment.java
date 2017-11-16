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

package com.icafe4j.image.jpeg;

import java.io.IOException;
import java.io.OutputStream;

import com.icafe4j.io.IOUtils;

/**
 * Special segment to handle JPEG Marker.UNKNOWN.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/22/2013
 */
public class UnknownSegment extends Segment {

	private short markerValue;
	
	public UnknownSegment(short markerValue, int length, byte[] data) {
		super(Marker.UNKNOWN, length, data);
		this.markerValue = markerValue;
	}
	
	public short getMarkerValue() {
		return markerValue;
	}
	
	@Override public void write(OutputStream os) throws IOException{
		IOUtils.writeIntMM(os, getLength());
		IOUtils.writeIntMM(os, this.markerValue);
		IOUtils.write(os, getData());
	}
	
	@Override public String toString() {
		return super.toString() + "[Marker value: 0x"+ Integer.toHexString(markerValue&0xffff)+"]";
	}
}
