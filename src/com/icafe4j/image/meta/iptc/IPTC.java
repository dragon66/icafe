/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * IPTC.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    25Apr2015  Renamed getDataSet() to getDataSets()
 * WY    25Apr2015  Added addDataSets()
 * WY    13Apr2015  Added write()
 */

package com.icafe4j.image.meta.iptc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.io.IOUtils;

public class IPTC extends Metadata {
	public static void showIPTC(byte[] data) {
		if(data != null && data.length > 0) {
			IPTC iptc = new IPTC(data);
			try {
				iptc.read();
				iptc.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static void showIPTC(InputStream is) {
		try {
			showIPTC(IOUtils.inputStreamToByteArray(is));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Map<String, List<IPTCDataSet>> datasetMap;
	
	public IPTC() {
		super(MetadataType.IPTC, null);
		datasetMap =  new HashMap<String, List<IPTCDataSet>>();
		isDataRead = true;
	}
	
	public IPTC(byte[] data) {
		super(MetadataType.IPTC, data);
		ensureDataRead();
	}
	
	public void addDataSet(IPTCDataSet dataSet) {
		addDataSets(Arrays.asList(dataSet));
	}
	
	public void addDataSets(Collection<? extends IPTCDataSet> dataSets) {
		if(datasetMap != null) {
			for(IPTCDataSet dataSet: dataSets) {
				String name = dataSet.getName();
				if(datasetMap.get(name) == null) {
					List<IPTCDataSet> list = new ArrayList<IPTCDataSet>();
					list.add(dataSet);
					datasetMap.put(name, list);
				} else if(dataSet.allowMultiple()) {
					datasetMap.get(name).add(dataSet);
				}
			}
		}
	}

	/**
	 * Get a string representation of the IPTCDataSet associated with the key
	 *  
	 * @param key the name for the IPTCDataSet
	 * @return a String representation of the IPTCDataSet, separated by ";"
	 */
	public String getAsString(String key) {
		// Retrieve the IPTCDataSet list associated with this key
		// Most of the time the list will only contain one item
		List<IPTCDataSet> list = getDataSet(key);
		
		String value = "";
	
		if(list != null) {
			if(list.size() == 1) {
				value = list.get(0).getDataAsString();
			} else {
				for(int i = 0; i < list.size() - 1; i++)
					value += list.get(i).getDataAsString() + ";";
				value += list.get(list.size() - 1).getDataAsString();
			}
		}
			
		return value;
	}
	
	/**
	 * Get a list of IPTCDataSet associated with a key
	 * 
	 * @param key name of the data set
	 * @return a list of IPTCDataSet associated with the key
	 */
	public List<IPTCDataSet> getDataSet(String key) {
		return getDataSets().get(key);
	}
	
	/**
	 * Get all the IPTCDataSet as a map for this IPTC data
	 * 
	 * @return a map with the key for the IPTCDataSet name and a list of IPTCDataSet as the value
	 */
	public Map<String, List<IPTCDataSet>> getDataSets() {
		ensureDataRead();
		return datasetMap;
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			int tagMarker = data[i];
			datasetMap = new HashMap<String, List<IPTCDataSet>>();
			while (tagMarker == 0x1c) {
				i++;
				int recordNumber = data[i++]&0xff;
				int tag = data[i++]&0xff;
				int recordSize = IOUtils.readUnsignedShortMM(data, i);
				i += 2;
				IPTCDataSet dataSet = new IPTCDataSet(recordNumber, tag, recordSize, data, i);
				String name = dataSet.getName();
				if(datasetMap.get(name) == null) {
					List<IPTCDataSet> list = new ArrayList<IPTCDataSet>();
					list.add(dataSet);
					datasetMap.put(name, list);
				} else
					datasetMap.get(name).add(dataSet);
				i += recordSize;
				// Sanity check
				if(i >= data.length) break;	
				tagMarker = data[i];							
			}
			// Remove possible duplicates
			for (Map.Entry<String, List<IPTCDataSet>> entry : datasetMap.entrySet()){
			    entry.setValue(new ArrayList<IPTCDataSet>(new LinkedHashSet<IPTCDataSet>(entry.getValue())));
			}
			
			isDataRead = true;
		}
	}
	
	public void showMetadata() {
		ensureDataRead();
		if(datasetMap != null){
			// Print multiple entry IPTCDataSet
			for(List<IPTCDataSet> iptcs : datasetMap.values()) {
				for(IPTCDataSet iptc : iptcs)
					iptc.print();
			}
		}
	}

	public void write(OutputStream os) throws IOException {
		for(List<IPTCDataSet> datasets : getDataSets().values())
			for(IPTCDataSet dataset : datasets)
				dataset.write(os);
	}
}
