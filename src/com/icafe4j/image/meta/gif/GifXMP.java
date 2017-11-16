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

package com.icafe4j.image.meta.gif;

import java.io.IOException;
import java.io.OutputStream;

import org.w3c.dom.Document;

import com.icafe4j.image.meta.xmp.XMP;
import com.icafe4j.string.XMLUtils;

import static com.icafe4j.image.gif.GIFTweaker.*;

public class GifXMP extends XMP {

	public GifXMP(byte[] data) {
		super(data);
	}

	public GifXMP(String xmp) {
		super(xmp);
	}
	
	public GifXMP(String xmp, String extendedXmp) {
		super(xmp, extendedXmp);
	}

	public void write(OutputStream os) throws IOException {
		byte[] buf = new byte[14];
 		buf[0] = EXTENSION_INTRODUCER; // Extension introducer
 		buf[1] = APPLICATION_EXTENSION_LABEL; // Application extension label
 		buf[2] = 0x0b; // Block size
 		buf[3] = 'X'; // Application Identifier (8 bytes)
 		buf[4] = 'M';
 		buf[5] = 'P';
 		buf[6] = ' ';
 		buf[7] = 'D';
 		buf[8] = 'a';
 		buf[9] = 't';
 		buf[10]= 'a';
 		buf[11]= 'X';// Application Authentication Code (3 bytes)
 		buf[12]= 'M';
 		buf[13]= 'P'; 		
 		// Create a byte array from 0x01, 0xFF - 0x00, 0x00
 		byte[] magic_trailer = new byte[258];
 		
 		magic_trailer[0] = 0x01;
 		
 		for(int i = 255; i >= 0; i--)
 			magic_trailer[256 - i] = (byte)i;
 		
 		// Insert XMP here
 		// Write extension introducer and application identifier
 		os.write(buf);
 		// Write the XMP packet
 		Document doc = getXmpDocument();
		XMLUtils.insertLeadingPI(doc, "xpacket", "begin='' id='W5M0MpCehiHzreSzNTczkc9d'");
		XMLUtils.insertTrailingPI(doc, "xpacket", "end='r'");
		os.write(XMLUtils.serializeToByteArray(doc));
 		// Write the magic trailer
 		os.write(magic_trailer);
 		// End of XMP data 		
	}
}