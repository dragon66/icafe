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
 * WY    22Jan2015  Revised to take care of more than one thumbnails
 * WY    21Jan2015  Initial creation
 */

package cafe.image.meta.image;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.w3c.dom.Document;

import cafe.image.meta.MetadataReader;
import cafe.image.meta.Thumbnail;
import cafe.string.StringUtils;

public class ImageMetadataReader implements MetadataReader {
	// Fields definition
	private boolean loaded;
	private Document document;
	private Map<String, Thumbnail> thumbnails;
		
	public ImageMetadataReader(Document document) {
		this.document = document;
	}
	
	public ImageMetadataReader(Document document, Map<String, Thumbnail> thumbnails) {
		this.document = document;
		this.thumbnails = thumbnails;
	}
	
	public boolean containsThumbnail() {
		return thumbnails != null && thumbnails.size() > 0;
	}
	
	public Document getDocument() {
		return document;
	}
	
	public Map<String, Thumbnail> getThumbnails() {
		return thumbnails;
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
		if(containsThumbnail()) { // We have thumbnail
			Iterator<Map.Entry<String, Thumbnail>> entries = thumbnails.entrySet().iterator();
			System.out.println("Total number of thumbnails: " + thumbnails.size());
			int i = 0;
			while (entries.hasNext()) {
			    Map.Entry<String, Thumbnail> entry = entries.next();
			    System.out.println("Thumbnail #" + i + ": " + entry.getKey() + " thumbnail:");
			    Thumbnail thumbnail = entry.getValue();
			    System.out.println("Thumbnail width: " + ((thumbnail.getWidth() < 0)? " Unavailable": thumbnail.getWidth()));
				System.out.println("Thumbanil height: " + ((thumbnail.getHeight() < 0)? " Unavailable": thumbnail.getHeight()));
				System.out.println("Thumbnail data type: " + thumbnail.getDataTypeAsString());
				i++;
			}
		}		
	}
}