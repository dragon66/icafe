/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * _8BIM.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    24Jan2015  initial creation
 */

package cafe.image.meta.adobe;

import java.io.IOException;
import java.io.OutputStream;

import cafe.io.IOUtils;
import cafe.string.StringUtils;

public class _8BIM {
	private short id;
	private String name;
	private int size;
	private byte[] data;
	
	public _8BIM(short id, String name, byte[] data) {
		this( id, name, data.length, data);
	}
	
	public _8BIM(short id, String name, int size, byte[] data) {
		this.id = id;
		this.name = name;
		this.size = size;
		this.data = data;
	}
	
	public _8BIM(ImageResourceID eId, String name, byte[] data) {
		this(eId.getValue(), name, data);
	}
	
	public byte[] getData() {
		return data;
	}
	
	public String getName() {
		return name;
	}
	
	public short getID() {
		return id;
	}
	
	public int getSize() {
		return size;
	}
	
	public void print() {
		ImageResourceID eId  = ImageResourceID.fromShort(id);
		
		if((id >= ImageResourceID.PATH_INFO0.getValue()) && (id <= ImageResourceID.PATH_INFO998.getValue())) {
			System.out.println("PATH_INFO" + " [Value: " + StringUtils.shortToHexStringMM(id) +"]" + " - Path Information (saved paths).");
		}
		else if((id >= ImageResourceID.PLUGIN_RESOURCE0.getValue()) && (id <= ImageResourceID.PLUGIN_RESOURCE999.getValue())) {
			System.out.println("PLUGIN_RESOURCE" + " [Value: " + StringUtils.shortToHexStringMM(id) +"]" + " - Plug-In resource.");
		}
		else if (eId == ImageResourceID.UNKNOWN) {
			System.out.println(eId + " [Value: " + StringUtils.shortToHexStringMM(id) +"]");
		}
		else {
			System.out.println(eId);
		}
		
		System.out.println("Type: 8BIM");
		System.out.println("Name: " + name);
		System.out.println("Size: " + size);
		
		eId.show(getData());
	}
	
	public void write(OutputStream os) throws IOException {
		// Write IRB id
		os.write("8BIM".getBytes());
		// Write resource id
		IOUtils.writeShortMM(os, id); 		
		// Write name (Pascal string - first byte denotes length of the string)
		byte[] temp = name.trim().getBytes();
		os.write(temp.length); // Size of the string, may be zero
		os.write(temp);
		if(temp.length%2 == 0)
			os.write(0);
		// Now write data size
		IOUtils.writeIntMM(os, size);
		os.write(data); // Write the data itself
		if(data.length%2 != 0)
			os.write(0); // Padding the data to even size if needed
	}
}