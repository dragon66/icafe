/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.gif;

import java.io.IOException;
import java.io.OutputStream;

/**
 * GIF Application Extension wrapper
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/06/2015
 */
public class ApplicationExtension {
	// Sequence of eight printable ASCII characters used to identify
	// the application owning the Application Extension.
	private byte[] applicationId; // 8 byte
	// Sequence of three bytes used to authenticate the Application Identifier
	private byte[] authenticationCode; // 3 byte
	private byte[] data;
	
	public static final byte EXTENSION_INTRODUCER = 0x21;
	public static final byte EXTENSION_LABEL = (byte)0xFF; 
	// Number of bytes in this extension block, following the Block Size field,
	// up to but not including the beginning of the Application Data.
	// This field contains the fixed value 11.
	public static final byte BLOCK_SIZE = 11; //
	
	public ApplicationExtension(byte[] applicationId, byte[] authenticationCode, byte[] data) {
		this.applicationId = applicationId;
		this.authenticationCode = authenticationCode;
		this.data = data;
	}
	
	public byte[] getApplicationId() {
		return applicationId;
	}
	
	public byte[] getAuthenticationCode() {
		return authenticationCode;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void write(OutputStream os) throws IOException {
		os.write(EXTENSION_INTRODUCER);
		os.write(EXTENSION_LABEL);
		os.write(BLOCK_SIZE);
		os.write(applicationId);
		os.write(authenticationCode);
		os.write(data);
	}
}