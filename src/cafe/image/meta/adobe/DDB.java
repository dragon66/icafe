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
 * DDB.java - Adobe Photoshop Document Data Block
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    23Jul2015  Initial creation
 */

package cafe.image.meta.adobe;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.io.IOUtils;

public class DDB extends Metadata {
	public static final String DDB_ID = "Adobe Photoshop Document Data Block\0";
		
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(DDB.class);
	
	public static void showDDB(byte[] data) {
		if(data != null && data.length > 0) {
			DDB ddb = new DDB(data);
			try {
				ddb.read();
				ddb.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void showDDB(InputStream is) {
		try {
			showDDB(IOUtils.inputStreamToByteArray(is));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public DDB(byte[] data) {
		super(MetadataType.PHOTOSHOP_DDB, data);
	}
	
	@Override
	// TODO this is not actually working yet. Need further analyzing of DDB structure
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			if(!new String(data, i, DDB_ID.length()).equals(DDB_ID)) {
				throw new RuntimeException("Invalid Photoshop Document Data Block");
			}
			i += DDB_ID.length();
			while((i+4) < data.length) {
				String _8bim = new String(data, i, 4);
				i += 4;
				if(_8bim.equals("8BIM")) {
					String id = new String(data, i, 4);
					i += 4;
					if(id.equals("Layr")) {
						LOGGER.info("Layer Data");
					} else if(id.equals("LMsk")) {
						LOGGER.info("User Data");
					} else if(id.equals("Patt")) {
						LOGGER.info("Pattern");
					} else if(id.equals("Anno")) {
						LOGGER.info("Annotations");
					} else {
						LOGGER.info(id.trim());
					}
					long size = IOUtils.readUnsignedIntMM(data, i); // For some reason, this value is incorrect!!!
					i += 4;
					LOGGER.info("Data length: {}", size);
					i += ((size + 3)>>2)<<2;// Skip data with padding bytes (padded to a 4 byte offset)
				}
			}
			isDataRead = true;
		}
	}
	
	public void showMetadata() {
		ensureDataRead();
		LOGGER.info("<<Adobe DDB information starts>>");
		LOGGER.info("<<Adobe DDB information ends>>");
	}
}