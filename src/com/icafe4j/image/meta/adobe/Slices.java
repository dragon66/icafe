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

package com.icafe4j.image.meta.adobe;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class Slices extends _8BIM {
	List<Slice> slices;
	
	public Slices() {
		this("Slices");
	}
	
	public Slices(String name) {
		super(ImageResourceID.SLICES, name, null);
	}

	public Slices(String name, byte[] data) {
		super(ImageResourceID.SLICES, name, data);
		read();
	}
	
	public List<Slice> getSlices() {
		return slices;
	}
	
	public void print() {
		super.print();
	
	}

	private void read() {
		
	}
	
	public void write(OutputStream os) throws IOException {
		if(data == null) {
		
			size = data.length;
		}
		super.write(os);
	}
	
	public static final class SliceHeader {
		
	}
}
