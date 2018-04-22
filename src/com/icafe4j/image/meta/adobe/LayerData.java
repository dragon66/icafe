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
 * LayerData.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    27Jul2015  initial creation
 */

package com.icafe4j.image.meta.adobe;

import java.util.ArrayList;
import java.util.List;

import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.io.ReadStrategy;

public class LayerData extends DDBEntry {
	private int layerCount;
	private List<Channel> channels = new ArrayList<Channel>();
		
	public LayerData(int size, byte[] data, ReadStrategy readStrategy) {
		super(DataBlockType.Layr, size, data, readStrategy);
		read();
	}
	
	protected MetadataEntry getMetadataEntry() {
		MetadataEntry root = new MetadataEntry(DataBlockType.Layr.name(), DataBlockType.Layr.getDescription(), true);
		root.addEntry(new MetadataEntry("Size", getSize() +""));
		root.addEntry(new MetadataEntry("Number of layers", layerCount +""));
		//
		return root;	
	}

	@SuppressWarnings("unused")
	private void read() {
		int i = 0;
		layerCount = readStrategy.readUnsignedShort(data, i);
		i += 2;
		for(int j = 0; j < layerCount; j++) { // For each layer
			int topCoord = readStrategy.readInt(data, i);
			i += 4;
			int leftCoord = readStrategy.readInt(data, i);
			i += 4;
			int bottomCoord = readStrategy.readInt(data, i);
			i += 4;
			int rightCoord = readStrategy.readInt(data, i);
			i += 4;
			int channelCount = readStrategy.readUnsignedShort(data, i);
			i += 2;
			for(int k = 0; k < channelCount; k++) {
				int id = readStrategy.readShort(data, i);
				i += 2;
				int len = readStrategy.readInt(data, i);
				i += 4;
				channels.add(new Channel(id, len));
			}
			int blendModeSignature = readStrategy.readInt(data, i);
			i += 4;
			int blendMode = readStrategy.readInt(data, i);
			i += 4;
			int opacity = data[i++]&0xff;
			int clipping = data[i++]&0xff;
			int flags = data[i++]&0xff;
			int filler = data[i++]&0xff;
			int extraLen = readStrategy.readInt(data, i);
			i += 4;
			i += extraLen; // Skip the extra data for now
			// TODO: read the following structure:
			//Layer mask data
			//Layer blending ranges
			//Layer name: Pascal string, padded to a multiple of 4 bytes
			//Additional layer information (optional)
		}	
	}
}