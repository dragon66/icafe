/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * PageReader.java
 *
 * Who   Date       Description
 * ====  =========  ====================================================
 * WY    28May2018  Initial creation
 */

package com.icafe4j.image.tiff;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import com.icafe4j.image.reader.TIFFReader;

//Helper class to read TIFF pages one at a time
public class PageReader extends TIFFReader {	
	private int cursor;
	
	public BufferedImage getNextPage(InputStream is) throws Exception {
		// One time reading of all the IFDs into a list
		if(ifds == null) readIFDs(is);
		
		if(cursor < ifds.size())
			return decode(ifds.get(cursor++));
		
		return null;
	}
	
	public BufferedImage read(InputStream is) throws Exception {
		return null; // NOOP
	}
}
