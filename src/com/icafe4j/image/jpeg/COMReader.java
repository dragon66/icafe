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

import com.icafe4j.util.Reader;

/**
 * JPEG COM segment reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/11/2013
 */
public class COMReader implements Reader {

	private Segment segment;
	private String comment;
	
	public COMReader(Segment segment) throws IOException {
		//
		if(segment.getMarker() != Marker.COM) {
			throw new IllegalArgumentException("Not a valid COM segment!");
		}
		
		this.segment = segment;
		read();
	}
	
	public String getComment() {
		return this.comment;
	}
	
	public void read() throws IOException {
		this.comment = new String(segment.getData()).trim();
	}
}
