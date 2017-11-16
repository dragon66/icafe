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

public class SOSReader implements Reader {
	//
	private Segment segment;
	private SOFReader reader;
	
	int Ss, Se, Ah_Al, Ah, Al;
	
	public SOSReader(Segment segment) throws IOException {
		//
		if(segment.getMarker() != Marker.SOS) {
			throw new IllegalArgumentException("Not a valid SOS segment!");
		}
		
		this.segment = segment;
		read();
	}
	
	public SOSReader(Segment segment, SOFReader reader) throws IOException {
		//
		if(segment.getMarker() != Marker.SOS) {
			throw new IllegalArgumentException("Not a valid SOS segment!");
		}
		
		this.segment = segment;
		this.reader = reader;
		read();
	}
	
	public void read() throws IOException {
		//
		byte[] data = segment.getData();		
		int count = 0;
		
		byte numOfComponents = data[count++];
		Component[] components = reader.getComponents();		
		
		for(int i = 0; i < numOfComponents; i++) {
			byte id = data[count++];
			byte tbl_no = data[count++];			
		
			for(Component component : components) {
				if(component.getId() == id) {					
					component.setACTableNumber((byte)(tbl_no&0x0f));
					component.setDCTableNumber((byte)((tbl_no>>4)&0x0f));
					break;
				}
			}
		}
		
		//Start of spectral or predictor selection
		Ss = data[count++];
	    //End of spectral selection
		Se = data[count++];
		//Ah: Successive approximation bit position high
		//Al: Successive approximation bit position low or point transform
		Ah_Al = data[count++];
	    Ah = (Ah_Al>>4)&0x0f;
		Al = Ah_Al&0x0f;
	}
	
	public void setSOFReader(SOFReader reader) {
		this.reader = reader;
	}
}