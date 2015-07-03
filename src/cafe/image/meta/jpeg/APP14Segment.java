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

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;

public class APP14Segment extends Metadata {

	private APP14SegmentReader reader;
	
	public APP14Segment(byte[] data) {
		super(MetadataType.JPG_APP14, data);
		this.reader = new APP14SegmentReader(data);
	}

	@Override
	public APP14SegmentReader getReader() {
		return reader;
	}
}
