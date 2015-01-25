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
 * VersionInfo.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    24Jan2015  initial creation
 */

package cafe.image.meta.adobe;

import java.io.UnsupportedEncodingException;

import cafe.io.IOUtils;
import cafe.string.StringUtils;
import cafe.util.ArrayUtils;

public class VersionInfo extends _8BIM {

	public VersionInfo(String name, int size, byte[] data) {
		super(ImageResourceID.VERSION_INFO.getValue(), name, size, data);
	}

	public void show() {
		super.show();
		byte[] data = getData();
		int i = 0;
		System.out.println("Version: " + StringUtils.byteArrayToHexString(ArrayUtils.subArray(data, i, 4)));
		i += 4;
        System.out.println("Has Real Merged Data: " + ((data[i++]!=0)?"True":"False"));
        int writer_size = IOUtils.readIntMM(data, i);
        i += 4;
        try {
			System.out.println("Writer name: " + new String(data, i, writer_size*2, "UTF-16BE"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        i += writer_size*2;
        int reader_size = IOUtils.readIntMM(data, i);
        i += 4;
        try {
			System.out.println("Reader name: " + new String(data, i, reader_size*2, "UTF-16BE"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        i += reader_size*2;
        System.out.println("File Version: " + StringUtils.byteArrayToHexString(ArrayUtils.subArray(data, i, 4)));                           
	}
}
