/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.image;

import java.io.UnsupportedEncodingException;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataReader;
import cafe.image.meta.MetadataType;

public class Comment extends Metadata {
	private String comment;
	
	public Comment(byte[] data) {
		super(MetadataType.COMMENT, data);
		try {
			this.comment = new String(data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public String getComment() {
		return comment;
	}
	
	public void showMetadata() {
		System.out.println("Comment: " + comment);
	}

	@Override
	public MetadataReader getReader() {
		return null;
	}
}