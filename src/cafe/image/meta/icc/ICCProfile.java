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
 * ICCProfile.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    30Sep2014  Rewrite to use byte array as input
 * WY    29Sep2014  Added getData()
 * WY    29Sep2014  Added new constructor ICCProfile(byte[])
 */

package cafe.image.meta.icc;

import java.io.IOException;
import java.io.InputStream;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.io.IOUtils;

/**
 * International Color Consortium Profile (ICC Profile)
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 07/02/2013
 */
public class ICCProfile extends Metadata {

	public static final int TAG_TABLE_OFFSET = 128;
	private ICCProfileReader reader;
	
	public static void showProfile(byte[] icc_profile) {
		if(icc_profile != null && icc_profile.length > 0) {
			ICCProfileReader reader = new ICCProfileReader(icc_profile);
			try {
				reader.read();
				reader.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void showProfile(InputStream is) {
		try {
			showProfile(IOUtils.inputStreamToByteArray(is));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ICCProfile(byte[] profile) {
		super(MetadataType.ICC_PROFILE, profile);
		this.reader = new ICCProfileReader(profile);
	}
	
	public ICCProfile(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public ICCProfileReader getReader() {
		return reader;
	}			
}