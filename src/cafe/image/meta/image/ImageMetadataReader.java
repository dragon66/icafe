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
 * ImageMetadataReader.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    21Jan2015  Initial creation
 */

package cafe.image.meta.image;

import java.io.IOException;

import org.w3c.dom.Document;

import cafe.image.meta.MetadataReader;
import cafe.image.meta.Thumbnail;
import cafe.string.StringUtils;

public class ImageMetadataReader implements MetadataReader {
	// Fields definition
	private boolean loaded;
	private Document document;
	private Thumbnail thumbnail;
	private boolean containsThumbnail;
	
	public ImageMetadataReader(Document document) {
		this.document = document;
	}
	
	public ImageMetadataReader(Document document, Thumbnail thumbnail) {
		this.document = document;
		this.thumbnail = thumbnail;
		this.containsThumbnail = true;
	}
	
	public boolean containsThumbnail() {
		return containsThumbnail;
	}
	
	public Document getDocument() {
		return document;
	}
	
	public Thumbnail getThumbnail() {
		return thumbnail;
	}
	
	@Override
	public boolean isDataLoaded() {
		return loaded;
	}
	
	@Override
	public void read() throws IOException {
		// No implementation
		this.loaded = true;
	}
	
	@Override
	public void showMetadata() {
		StringUtils.showXML(document);
		// Thumbnail information
		if(containsThumbnail) { // We have thumbnail
			System.out.println("Thumbnail width: " + thumbnail.getWidth());
			System.out.println("Thumbanil height: " + thumbnail.getHeight());
			System.out.println("Thumbnail data type: " + thumbnail.getDataType());
		}		
	}
}