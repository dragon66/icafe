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
 * APP12SegmentReader.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package cafe.image.meta.jpeg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cafe.image.meta.MetadataReader;
import cafe.io.IOUtils;

public class APP12SegmentReader implements MetadataReader {
	private Map<APP12Tag, APP12DataSet> datasetMap = new EnumMap<APP12Tag, APP12DataSet>(APP12Tag.class);
	private boolean loaded;
	private byte[] data;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(APP12SegmentReader.class);
	
	public APP12SegmentReader(byte[] app12) {
		this.data = app12;
	}
	
	public APP12SegmentReader(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public Map<APP12Tag, APP12DataSet> getDataSets() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableMap(datasetMap);
	}
	
	@Override
	public void read() throws IOException {
		int i = 0;
		
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
		
	    loaded = true;
	}

	@Override
	public void showMetadata() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		LOGGER.info("JPEG APP12Segment reader output starts =>");
		// Print APP12DataSet
		for(APP12DataSet dataset : datasetMap.values()) {
			dataset.print();
		}
		LOGGER.info("<= JPEG APP12Segment reader output ends");
	}

	@Override
	public boolean isDataLoaded() {
		return loaded;
	}
}