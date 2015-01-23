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
 * IRB.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    19Jan2015  Intitial creation
 */

package cafe.image.meta.adobe;

import java.io.IOException;
import java.io.InputStream;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.io.IOUtils;

public class IRB extends Metadata {
	private IRBReader reader;
	
	public static void showIRB(byte[] irb) {
		if(irb != null && irb.length > 0) {
			IRBReader reader = new IRBReader(irb);
			try {
				reader.read();
				reader.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void showIRB(InputStream is) {
		try {
			showIRB(IOUtils.inputStreamToByteArray(is));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public IRB(byte[] data) {
		super(MetadataType.PHOTOSHOP_IRB, data);
		reader = new IRBReader(data);
	}
	
	public IRBReader getReader() {
		return reader;
	}
}