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
 * APP14Segment.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package cafe.image.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.io.IOUtils;

public class APP14Segment extends Metadata {
	//
	private int m_DCTEncodeVersion;
	private int m_APP14Flags0;
	private int m_APP14Flags1;
	private int m_ColorTransform;
	
	private APP14SegmentReader reader;
	
	public APP14Segment(byte[] data) {
		super(MetadataType.JPG_APP14, data);
		this.reader = new APP14SegmentReader(data);
	}
	
	public APP14Segment(int dctEncodeVersion, int app14Flags0, int app14Flags1, int colorTransform) {
		super(MetadataType.JPG_APP14, null);
		this.m_DCTEncodeVersion = dctEncodeVersion;
		this.m_APP14Flags0 = app14Flags0;
		this.m_APP14Flags1 = app14Flags1;
		this.m_ColorTransform = colorTransform;		
	}
	
	public int getDCTEncodeVersion() {
		if(reader != null)
			return reader.getDCTEncodeVersion();
		return m_DCTEncodeVersion;
	}
	
	public int getAPP14Flags0() {
		if(reader != null)
			return reader.getAPP14Flags0();
		return m_APP14Flags0;
	}
	
	public int getAPP14Flags1() {
		if(reader != null)
			return reader.getAPP14Flags1();
		return m_APP14Flags1;
	}
	
	public int getColorTransform() {
		if(reader != null)
			return reader.getColorTransform();
		return m_ColorTransform;
	}

	@Override
	public APP14SegmentReader getReader() {
		return reader;
	}
	
	public void write(OutputStream os) throws IOException {
		IOUtils.writeShortMM(os, getDCTEncodeVersion());
		IOUtils.writeShortMM(os, getAPP14Flags0());
		IOUtils.writeShortMM(os, getAPP14Flags1());
		IOUtils.write(os, getColorTransform());
	}
}
