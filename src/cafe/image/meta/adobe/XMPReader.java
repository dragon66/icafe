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
 * XMPReader.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    19Feb2015  Added Adobe XMPMeta as one of the meta data format
 * WY    19Jan2015  Initial creation
 */

package cafe.image.meta.adobe;

import java.io.IOException;

import org.w3c.dom.Document;

import cafe.image.meta.MetadataReader;
import cafe.string.XMLUtils;

public class XMPReader implements MetadataReader {
	private byte[] data;
	private String xmp;
	private boolean loaded;
	//document contains the complete XML as a Tree.
	private Document document = null;
	
	public XMPReader(byte[] data) {
		this.data = data;
	}
	
	public XMPReader(String xmp) {
		this.xmp = xmp;
	}
	
	public Document getXmpDocument() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return document;
	}
	
	@Override
	public void read() throws IOException {
		if(xmp != null)
			document = XMLUtils.createXML(xmp);
		else if(data != null)
			document = XMLUtils.createXML(data);
		
		loaded = true;
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
		}	
		if(document != null)
			XMLUtils.showXML(document);
	}
}