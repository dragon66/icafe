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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cafe.io.IOUtils;
import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;

public class APP12Segment extends Metadata {

	private Map<APP12Tag, APP12DataSet> datasetMap;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(APP12Segment.class);
		
	public APP12Segment() {
		super(MetadataType.JPG_APP12, null);
		datasetMap =  new EnumMap<APP12Tag, APP12DataSet>(APP12Tag.class);
		isDataRead = true;
	}
	
	public APP12Segment(byte[] data) {
		super(MetadataType.JPG_APP12, data);
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
		ensureDataRead();
		return Collections.unmodifiableMap(datasetMap);
	}
	
	@Override
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			datasetMap = new EnumMap<APP12Tag, APP12DataSet>(APP12Tag.class);
			
			for(;;) {
				if(i + 4 > data.length) break;
				int tag = IOUtils.readUnsignedShortMM(data, i);
				i += 2;
				int size = IOUtils.readUnsignedShortMM(data, i);
				i += 2;
				APP12Tag etag = APP12Tag.fromTag(tag);
				datasetMap.put(etag, new APP12DataSet(tag, size, data, i));
				i += size;
			}
			
		    isDataRead = true;
		}
	}

	public void showMetadata() {
		ensureDataRead();
		LOGGER.info("JPEG APP12Segment output starts =>");
		// Print APP12DataSet
		for(APP12DataSet dataset : datasetMap.values()) {
			dataset.print();
		}
		LOGGER.info("<= JPEG APP12Segment output ends");
	}
	
	public void write(OutputStream os) throws IOException {
		ensureDataRead();
		for(APP12DataSet dataset : getDataSets().values())
			dataset.write(os);
	}
}