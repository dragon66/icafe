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
 * IPTC_NAA.java
 *
 * Who   Date       Description
 * ====  =========  ==================================================
 * WY    25Apr2015  Added addDataSets()
 * WY    25Apr2015  Renamed getDataSet(0 to getDataSets()
 * WY    13Apr2015  Changed write() to use ITPC.write()
 * WY    12Apr2015  Removed unnecessary read()
 */

package com.icafe4j.image.meta.adobe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.image.meta.iptc.IPTCDataSet;
import com.icafe4j.string.StringUtils;

public class IPTC_NAA extends _8BIM {
	//
	private IPTC iptc;
		
	public IPTC_NAA() {
		this("IPTC_NAA");
	}
	
	public IPTC_NAA(String name) {
		super(ImageResourceID.IPTC_NAA, name, null);
		iptc = new IPTC();
	}

	public IPTC_NAA(String name, byte[] data) {
		super(ImageResourceID.IPTC_NAA, name, data);
		iptc = new IPTC(data);
	}
	
	public void addDataSet(IPTCDataSet dataSet) {
		iptc.addDataSet(dataSet);
	}
	
	public void addDataSets(Collection<? extends IPTCDataSet> dataSets) {
		iptc.addDataSets(dataSets);
	}
	
	/**
	 * Get all the IPTCDataSet as a map for this IPTC data
	 * 
	 * @return a map with the key for the IPTCDataSet name and a list of IPTCDataSet as the value
	 */
	public Map<String, List<IPTCDataSet>> getDataSets() {
		return iptc.getDataSets();			
	}
	
	/**
	 * Get a list of IPTCDataSet associated with a key
	 * 
	 * @param key name of the data set
	 * @return a list of IPTCDataSet associated with the key
	 */
	public List<IPTCDataSet> getDataSet(String key) {
		return iptc.getDataSet(key);
	}
	
	protected MetadataEntry getMetadataEntry() {
		//
		ImageResourceID eId  = ImageResourceID.fromShort(getID());
		MetadataEntry entry = new MetadataEntry(eId.name(), eId.getDescription(), true);
		
		Map<String, List<IPTCDataSet>> datasetMap = this.getDataSets();
		
		if(datasetMap != null) {
			// Print multiple entry IPTCDataSet
			Set<Map.Entry<String, List<IPTCDataSet>>> entries = datasetMap.entrySet();
			
			for(Entry<String, List<IPTCDataSet>> entryMap : entries) {
				StringBuilder strBuilder = new StringBuilder();
				//
				for(IPTCDataSet item : entryMap.getValue())
					strBuilder.append(item.getDataAsString()).append(";");
				
				String key = entryMap.getKey();				
				String value = StringUtils.replaceLast(strBuilder.toString(), ";", "");
				
				entry.addEntry(new MetadataEntry(key, value));
		    }
			
			return entry;
			
		} else 
			return super.getMetadataEntry();
	}
	
	public void write(OutputStream os) throws IOException {
		if(data == null) {			
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			iptc.write(bout);
			data = bout.toByteArray();
			size = data.length;
		}
		super.write(os);
	}
}