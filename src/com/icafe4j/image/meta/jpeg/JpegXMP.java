/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.icafe4j.image.jpeg.Marker;
import com.icafe4j.image.meta.xmp.XMP;
import com.icafe4j.io.IOUtils;
import com.icafe4j.string.StringUtils;
import com.icafe4j.string.XMLUtils;
import com.icafe4j.util.ArrayUtils;

import static com.icafe4j.image.jpeg.JPEGTweaker.*;

public class JpegXMP extends XMP {
	
	// Largest size for each extended XMP chunk
	private static final int MAX_EXTENDED_XMP_CHUNK_SIZE = 65458;
	private static final int MAX_XMP_CHUNK_SIZE = 65504;
	private static final int GUID_LEN = 32;

	public JpegXMP(byte[] data) {
		super(data);
	}
	
	public JpegXMP(String xmp) {
		super(xmp);
	}
	
	/**
	 * @param xmp XML string for the XMP - Assuming in UTF-8 format.
	 * @param extendedXmp XML string for the extended XMP - Assuming in UTF-8 format
	 */
	public JpegXMP(String xmp, String extendedXmp) {
		super(xmp, extendedXmp);
	}

	public void write(OutputStream os) throws IOException {
		// Add packet wrapper to the XMP document
		// Add PI at the beginning and end of the document, we will support only UTF-8, no BOM
		Document xmpDoc = getXmpDocument();
		XMLUtils.insertLeadingPI(xmpDoc, "xpacket", "begin='' id='W5M0MpCehiHzreSzNTczkc9d'");
		XMLUtils.insertTrailingPI(xmpDoc, "xpacket", "end='r'");
		byte[] extendedXmp = getExtendedXmpData();
		String guid = null;
		if(extendedXmp != null) { // We have ExtendedXMP
			guid = StringUtils.generateMD5(extendedXmp);
			NodeList descriptions = xmpDoc.getElementsByTagName("rdf:Description");
			int length = descriptions.getLength();
			if(length > 0) {
				Element node = (Element)descriptions.item(length - 1);
				node.setAttribute("xmlns:xmpNote", "http://ns.adobe.com/xmp/extension/");
				node.setAttribute("xmpNote:HasExtendedXMP", guid);
			}
		}
		// Serialize XMP to byte array
		byte[] xmp = XMLUtils.serializeToByteArray(xmpDoc);
		if(xmp.length > MAX_XMP_CHUNK_SIZE)
			throw new RuntimeException("XMP data size exceededs JPEG segment size");
		// Write XMP segment
		IOUtils.writeShortMM(os, Marker.APP1.getValue());
		// Write segment length
		IOUtils.writeShortMM(os, XMP_ID.length() + 2 + xmp.length);
		// Write segment data
		os.write(XMP_ID.getBytes());
		os.write(xmp);
		// Write ExtendedXMP if we have
		if(extendedXmp != null) {
			int numOfChunks = extendedXmp.length / MAX_EXTENDED_XMP_CHUNK_SIZE;
			int extendedXmpLen = extendedXmp.length;
			int offset = 0;
			
			for(int i = 0; i < numOfChunks; i++) {
				IOUtils.writeShortMM(os, Marker.APP1.getValue());
				// Write segment length
				IOUtils.writeShortMM(os, 2 + XMP_EXT_ID.length() + GUID_LEN + 4 + 4 + MAX_EXTENDED_XMP_CHUNK_SIZE);
				// Write segment data
				os.write(XMP_EXT_ID.getBytes());
				os.write(guid.getBytes());
				IOUtils.writeIntMM(os, extendedXmpLen);
				IOUtils.writeIntMM(os, offset);
				os.write(ArrayUtils.subArray(extendedXmp, offset, MAX_EXTENDED_XMP_CHUNK_SIZE));
				offset += MAX_EXTENDED_XMP_CHUNK_SIZE;			
			}
			
			int leftOver = extendedXmp.length % MAX_EXTENDED_XMP_CHUNK_SIZE;
			
			if(leftOver != 0) {
				IOUtils.writeShortMM(os, Marker.APP1.getValue());
				// Write segment length
				IOUtils.writeShortMM(os, 2 + XMP_EXT_ID.length() + GUID_LEN + 4 + 4 + leftOver);
				// Write segment data
				os.write(XMP_EXT_ID.getBytes());
				os.write(guid.getBytes());
				IOUtils.writeIntMM(os, extendedXmpLen);
				IOUtils.writeIntMM(os, offset);
				os.write(ArrayUtils.subArray(extendedXmp, offset, leftOver));
			}
		}
	}
}