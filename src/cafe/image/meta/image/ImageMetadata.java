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
 * ImageMetadata.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    22Jan2015  Revised to take care of more than one thumbnails
 * WY    21Jan2015  Initial creation
 */

package cafe.image.meta.image;

import java.util.Map;

import org.w3c.dom.Document;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.image.meta.Thumbnail;

public class ImageMetadata extends Metadata {
	
	private ImageMetadataReader reader;

	public ImageMetadata(Document document) {
		super(MetadataType.IMAGE, null);
		this.reader = new ImageMetadataReader(document);
	}
	
	public ImageMetadata(Document document, Map<String, Thumbnail> thumbnails) {
		super(MetadataType.IMAGE, null);
		this.reader = new ImageMetadataReader(document, thumbnails);
	}

	@Override
	public ImageMetadataReader getReader() {
		return reader;
	}
}