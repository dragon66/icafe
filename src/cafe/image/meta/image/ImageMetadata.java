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
 * WY    21Jan2015  Initial creation
 */

package cafe.image.meta.image;

import org.w3c.dom.Document;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataReader;
import cafe.image.meta.MetadataType;
import cafe.image.meta.Thumbnail;

public class ImageMetadata extends Metadata {
	
	private MetadataReader reader;

	public ImageMetadata(Document document) {
		super(MetadataType.IMAGE, null);
		this.reader = new ImageMetadataReader(document);
	}
	
	public ImageMetadata(Document document, Thumbnail thumbnail) {
		super(MetadataType.IMAGE, null);
		this.reader = new ImageMetadataReader(document, thumbnail);
	}

	@Override
	public MetadataReader getReader() {
		return reader;
	}
}