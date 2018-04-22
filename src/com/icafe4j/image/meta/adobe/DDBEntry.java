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
 * DDBEntry.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    24Jul2015  initial creation
 */

package com.icafe4j.image.meta.adobe;

import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.io.ReadStrategy;
import com.icafe4j.string.StringUtils;

//Building block for DDB
public class DDBEntry {
	private int type;
	private int size;
	protected byte[] data;
	protected ReadStrategy readStrategy;
	
	public DDBEntry(DataBlockType etype, int size, byte[] data, ReadStrategy readStrategy) {
		this(etype.getValue(), size, data, readStrategy);
	}
	
	public DDBEntry(int type, int size, byte[] data, ReadStrategy readStrategy) {
		this.type = type;
		if(size < 0) throw new IllegalArgumentException("Input size is negative");
		this.size = size;
		this.data = data;
		if(readStrategy == null) throw new IllegalArgumentException("Input readStrategy is null");
		this.readStrategy = readStrategy;
	}

	public int getType() {
		return type;
	}
	
	public DataBlockType getTypeEnum() {
		return DataBlockType.fromInt(type);
	}
	
	protected MetadataEntry getMetadataEntry() {
		//	
		DataBlockType eType  = DataBlockType.fromInt(type);
		
		if (eType == DataBlockType.UNKNOWN) {
			return new MetadataEntry("UNKNOWN [" + StringUtils.intToHexStringMM(type) + "]:", eType.getDescription());
		} else {
			return new MetadataEntry("" + eType, eType.getDescription());
		}
	}
	
	public int getSize() {
		return size;
	}
	
	public byte[] getData() {
		return data.clone();
	}
}
