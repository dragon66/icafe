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

package com.icafe4j.document.pdf;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.RandomAccessInputStream;

public class PDFSnoop {
	//
	RandomAccessInputStream randInput;
	long streamLength = -1;
	
	public PDFSnoop(InputStream is) throws IOException {
		this.randInput = new FileCacheRandomAccessInputStream(is);
	}
	
	public void snoop() throws IOException 
	{	
		long position = getLastPositionOf("startxref", getStreamLength());
		
		if(position >= 0) {
			randInput.seek(position);
			System.out.println("Last " + "\"" + new String(readLine(randInput)) + "\"" +" (offset " + position + ")");
		} else {
			System.out.println("startxref not found!");
		}
		
		long streamOffset = randInput.getStreamPointer();
		long xrefOffset = Long.parseLong(new String(readLine(randInput)));
		
		System.out.println("last startxref value " + "\"" + xrefOffset + "\"" +" (offset " + (streamOffset + skip) + ")");
		
		randInput.close();
	}
	
	private byte[] readLine(InputStream is) throws IOException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		byte currByte = (byte)is.read();
		
		if(currByte == LF) {
			currByte = (byte)is.read();
			skip = 1;//Skip the last LF leftover after last readLine if any
		} else {
			skip = 0;
		}
		
		while(currByte != CR && currByte != LF) {
			bo.write(currByte);
			currByte = (byte)is.read();
		}
		
		return bo.toByteArray();
	}
	
	// Search starts backward from startOffset
	private long getLastPositionOf(String strToSearch, long startOffset) throws IOException {
		//
		long offset = startOffset - 1024;
		if(offset < 0) offset = 0;
		randInput.seek(offset);		
		byte[] buf;
		if(offset != 0) {
			buf = new byte[1024];
		} else {
			buf = new byte[(int)startOffset];
		}
		int bytes = randInput.read(buf);
		int pos = new String(buf, 0, bytes).lastIndexOf(strToSearch);
		
		while(pos == -1 && offset > 0) {
			offset -= 1024;
			if(offset < 0) offset = 0;
			randInput.seek(offset);
			bytes = randInput.read(buf);
			pos = new String(buf, 0, bytes).lastIndexOf(strToSearch);
		}
		
		if(pos == -1) return pos;
		
		return offset + pos;
	}
	
	// This method can be used to read from the end of PDF file
	private long getStreamLength() throws IOException {
		//
		if(streamLength != -1) 
			return streamLength;
		long totalBytes = 0;
		int bytesSkipped = randInput.skipBytes(1024);
	
		while(bytesSkipped > 0) {
			totalBytes += bytesSkipped;
			bytesSkipped = randInput.skipBytes(1024);
		}
		
		streamLength = totalBytes;
		
		return streamLength;
	}
	
	public static void main(String[] args) throws IOException {
		BufferedInputStream bi = new BufferedInputStream(new FileInputStream(args[0]));
		PDFSnoop pdfSnoop = new PDFSnoop(bi);
		pdfSnoop.snoop();
		bi.close();
	}
	
	private static final byte CR = 0x0d;
	private static final byte LF = 0x0a;
	
	private int skip = 0;
}
