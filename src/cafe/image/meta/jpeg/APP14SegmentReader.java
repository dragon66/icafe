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
 * APP14SegmentReader.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package cafe.image.meta.jpeg;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cafe.image.meta.MetadataReader;
import cafe.io.IOUtils;
import cafe.string.StringUtils;

public class APP14SegmentReader implements MetadataReader {
	private boolean loaded;
	private byte[] data;
	private int m_DCTEncodeVersion;
	private int m_APP14Flags0;
	private int m_APP14Flags1;
	private int m_ColorTransform;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(APP14SegmentReader.class);
	
	public APP14SegmentReader(byte[] app14) {
		this.data = app14;
	}
	
	public APP14SegmentReader(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public int getDCTEncodeVersion() {
		return m_DCTEncodeVersion;
	}
	
	public int getAPP14Flags0() {
		return m_APP14Flags0;
	}
	
	public int getAPP14Flags1() {
		return m_APP14Flags1;
	}
	
	public int getColorTransform() {
		return m_ColorTransform;
	}
	
	@Override
	public void read() throws IOException {
		int expectedLen = 7;
		int offset = 0;
		
		if (data.length >= expectedLen) {
			m_DCTEncodeVersion = IOUtils.readUnsignedShortMM(data, offset);
			offset += 2;
			m_APP14Flags0 = IOUtils.readUnsignedShortMM(data, offset);
			offset += 2;
			m_APP14Flags1 = IOUtils.readUnsignedShortMM(data, offset);
			offset += 2;
			m_ColorTransform = data[offset]&0xff;			
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
		String[] colorTransform = {"Unknown (RGB or CMYK)", "YCbCr", "YCCK"};
		LOGGER.info("JPEG APP14Segment reader output starts =>");
		LOGGER.info("DCTEncodeVersion: {}", m_DCTEncodeVersion);
		LOGGER.info("APP14Flags0: {}", StringUtils.shortToHexStringMM((short)m_APP14Flags0));
		LOGGER.info("APP14Flags1: {}", StringUtils.shortToHexStringMM((short)m_APP14Flags1));
		LOGGER.info("ColorTransform: {}", (m_ColorTransform <= 2)?colorTransform[m_ColorTransform]:m_ColorTransform);
		LOGGER.info("<= JPEG APP14Segment reader output ends");
	}

	@Override
	public boolean isDataLoaded() {
		return loaded;
	}
}