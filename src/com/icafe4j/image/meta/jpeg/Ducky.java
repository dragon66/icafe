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
 * Ducky.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package com.icafe4j.image.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.io.IOUtils;

public class Ducky extends Metadata {

	private Map<DuckyTag, DuckyDataSet> datasetMap;
	
	public Ducky() {
		super(MetadataType.JPG_DUCKY);
		datasetMap =  new EnumMap<DuckyTag, DuckyDataSet>(DuckyTag.class);
		isDataRead = true;
	}
	
	public Ducky(byte[] data) {
		super(MetadataType.JPG_DUCKY, data);
		ensureDataRead();
	}
	
	public void addDataSet(DuckyDataSet dataSet) {
		if(datasetMap != null) {
			datasetMap.put(DuckyTag.fromTag(dataSet.getTag()), dataSet);				
		}
	}
	
	public void addDataSets(Collection<? extends DuckyDataSet> dataSets) {
		if(datasetMap != null) {
			for(DuckyDataSet dataSet: dataSets) {
				datasetMap.put(DuckyTag.fromTag(dataSet.getTag()), dataSet);				
			}
		}
	}
	
	public Map<DuckyTag, DuckyDataSet> getDataSets() {
		ensureDataRead();
		return Collections.unmodifiableMap(datasetMap);
	}
	
	public Iterator<MetadataEntry> iterator() {
		ensureDataRead();
		
		List<MetadataEntry> entries = new ArrayList<MetadataEntry>();
		MetadataEntry root = new MetadataEntry("JPEG", "Ducky", true);
		
		for(DuckyDataSet dataset : datasetMap.values()) {
			root.addEntry(dataset.getMetadataEntry());
		}
		
		entries.add(root);
		
		return Collections.unmodifiableCollection(entries).iterator();
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			datasetMap = new EnumMap<DuckyTag, DuckyDataSet>(DuckyTag.class);
			
			for(;;) {
				if(i + 4 > data.length) break;
				int tag = IOUtils.readUnsignedShortMM(data, i);
				i += 2;
				int size = IOUtils.readUnsignedShortMM(data, i);
				i += 2;
				DuckyTag etag = DuckyTag.fromTag(tag);
				datasetMap.put(etag, new DuckyDataSet(tag, size, data, i));
				i += size;
			}
			
		    isDataRead = true;
		}
	}

	public void write(OutputStream os) throws IOException {
		ensureDataRead();
		for(DuckyDataSet dataset : getDataSets().values())
			dataset.write(os);
	}
}