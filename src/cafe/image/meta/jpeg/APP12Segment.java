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
 * APP12Segment.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package cafe.image.meta.jpeg;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;

public class APP12Segment extends Metadata {

	private APP12SegmentReader reader;
	private Map<APP12Tag, APP12DataSet> datasetMap;
	
	public APP12Segment() {
		super(MetadataType.JPG_APP12, null);
		datasetMap =  new EnumMap<APP12Tag, APP12DataSet>(APP12Tag.class);
	}
	
	public APP12Segment(byte[] data) {
		super(MetadataType.JPG_APP12, data);
		this.reader = new APP12SegmentReader(data);
	}
	
	public void addDataSet(APP12DataSet dataSet) {
		if(datasetMap != null) {
			datasetMap.put(APP12Tag.fromTag(dataSet.getTag()), dataSet);				
		}
	}
	
	public void addDataSets(Collection<? extends APP12DataSet> dataSets) {
		if(datasetMap != null) {
			for(APP12DataSet dataSet: dataSets) {
				datasetMap.put(APP12Tag.fromTag(dataSet.getTag()), dataSet);				
			}
		}
	}
	
	public Map<APP12Tag, APP12DataSet> getDataSets() {
		if(datasetMap != null)
			return datasetMap;
		return reader.getDataSets();
	}

	@Override
	public APP12SegmentReader getReader() {
		return reader;
	}
	
	public void showMetadata() {
		if(datasetMap != null){
			// Print APP12DataSet
			for(APP12DataSet dataSet : datasetMap.values()) {
				dataSet.print();
			}
		} else
			super.showMetadata();
	}
}