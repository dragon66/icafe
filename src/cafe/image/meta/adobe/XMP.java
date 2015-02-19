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
 * ====  =========  =================================================
 * WY    19Feb2015  Removed showMetadata() and added getXmpMeta()
 * WY    19Feb2015  Renamed getXMLDocument() to getXmpDocument()
 * WY    11Feb2015  Added getXMLDocument() and showMetadata()
 * WY    19Jan2015  Initial creation
 */

package cafe.image.meta.adobe;

import java.io.IOException;

import org.w3c.dom.Document;

import com.adobe.xmp.XMPMeta;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;

public class XMP extends Metadata {
	
	private XMPReader reader;
	
	public XMP(byte[] data) {
		super(MetadataType.XMP, data);
		reader = new XMPReader(data);
	}
	
	public XMP(String xmp) {
		super(MetadataType.XMP, null);
		reader = new XMPReader(xmp);
	}
	
	public XMP(Document document) {
		this((byte[])null);		
	}
	
	public Document getXmpDocument() {
		if(reader != null && !reader.isDataLoaded()) {
			try {
				reader.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return reader.getXmpDocument();
		}
		
		return null;
	}
	
	public XMPMeta getXmpMeta() {
		if(reader != null && !reader.isDataLoaded()) {
			try {
				reader.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return reader.getXmpMeta();
		}
		
		return null;
	}
		
	@Override
	public XMPReader getReader() {
		return reader;
	}
}