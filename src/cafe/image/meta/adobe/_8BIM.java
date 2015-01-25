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

import cafe.string.StringUtils;

public class _8BIM {
	private short id;
	private String name;
	private int size;
	private byte[] data;
	
	public _8BIM(short id, String name, int size, byte[] data) {
		this.id = id;
		this.name = name;
		this.size = size;
		this.data = data;
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
	
	public void show() {
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
}
