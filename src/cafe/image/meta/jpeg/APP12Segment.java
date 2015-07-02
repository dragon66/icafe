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

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;

public class APP12Segment extends Metadata {

	private APP12SegmentReader reader;
	
	public APP12Segment(byte[] data) {
		super(MetadataType.JPG_APP12, data);
		this.reader = new APP12SegmentReader(data);
	}

	@Override
	public APP12SegmentReader getReader() {
		return reader;
	}
}