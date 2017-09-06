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

import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.image.meta.iptc.IPTCDataSet;

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
	
	public void print() {
		super.print();
		// Print multiple entry IPTCDataSet
		for(List<IPTCDataSet> datasets : iptc.getDataSets().values())
			for(IPTCDataSet dataset : datasets)
				dataset.print();			
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