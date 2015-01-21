/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * XMPReader.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    19Jan2015  Initial creation
 */

package cafe.image.meta.adobe;

import java.io.IOException;

import org.w3c.dom.Document;
import cafe.image.meta.MetadataReader;
import cafe.string.StringUtils;

public class XMPReader implements MetadataReader {
	private byte[] data;
	private boolean loaded;
	//document contains the complete XML as a Tree.
	Document document = null;

	public XMPReader(byte[] data) {
		this.data = data;
	}
	
	public Document getXMLDocument() {
		return document;
	}
	
	@Override
	public void read() throws IOException {
		document = StringUtils.createXML(data);
	}

	@Override
	public boolean isDataLoaded() {
		return loaded;
	}
	
	public void showMetadata() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(document != null)
				StringUtils.showXML(document);
		}		
	}
}