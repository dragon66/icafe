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
 * XMP.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    19Jan2015  Initial creation
 */

package cafe.image.meta.adobe;

import org.w3c.dom.Document;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataReader;
import cafe.image.meta.MetadataType;

public class XMP extends Metadata {
	
	private MetadataReader reader;

	public XMP(byte[] data) {
		super(MetadataType.XMP, data);
		reader = new XMPReader(data);
	}
	
	public XMP(Document document) {
		this((byte[])null);		
	}

	@Override
	public MetadataReader getReader() {
		return reader;
	}
}