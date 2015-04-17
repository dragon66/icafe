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
 * <p>
 * Adobe IRB version info resource wrapper
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    17Apr2015  Added new constructor
 * WY    17Apr2015  Changed version and fileVersion data type to int  
 */

package cafe.image.meta.adobe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import cafe.io.IOUtils;
import cafe.string.StringUtils;

public class VersionInfo extends _8BIM {
	//
	private int version;
	private boolean hasRealMergedData;
	private String writerName;
	private String readerName;
	private int fileVersion;
	
	public VersionInfo() {
		this("VersionInfo");
	}
	
	public VersionInfo(String name) {
		super(ImageResourceID.VERSION_INFO, name, null);
	}

	public VersionInfo(String name, byte[] data) {
		super(ImageResourceID.VERSION_INFO, name, data);
		read();
	}
	
	public VersionInfo(String name, int version, boolean hasRealMergedData, String writerName, String readerName, int fileVersion) {
		super(ImageResourceID.VERSION_INFO, name, null);
		this.version = version;
		this.hasRealMergedData = hasRealMergedData;
		this.writerName = writerName;
		this.readerName = readerName;
		this.fileVersion = fileVersion;		
	}
	
	public int getFileVersion() {
		return fileVersion;
	}
	
	public int getVersion() {
		return version;
	}
	
	public boolean hasRealMergedData() {
		return hasRealMergedData;
	}
	
	public String getReaderName() {
		return readerName;
	}
	
	public String getWriterName() {
		return writerName;
	}
	
	private void read() {
		int i = 0;
		version = IOUtils.readIntMM(data, i);
		i += 4;
	    hasRealMergedData = ((data[i++]!=0)?true:false);
	    int writer_size = IOUtils.readIntMM(data, i);
	    i += 4;
	    writerName = StringUtils.toUTF16BE(data, i, writer_size*2);
	    i += writer_size*2;
	    int reader_size = IOUtils.readIntMM(data, i);
    	i += 4;
    	readerName = StringUtils.toUTF16BE(data, i, reader_size*2);
    	i += reader_size*2;
	    fileVersion = IOUtils.readIntMM(data, i);  
	}
	
	public void print() {
		super.print();
		System.out.println("Version: " + getVersion());
		System.out.println("Has Real Merged Data: " + hasRealMergedData);
        System.out.println("Writer name: " + writerName);
		System.out.println("Reader name: " + readerName);
		System.out.println("File Version: " + getFileVersion()); 
	}

	public void setHasRealMergedData(boolean hasRealMergedData) {
		this.hasRealMergedData = hasRealMergedData;
	}
	
	public void setFileVersion(int fileVersion) {
		if(fileVersion < 0)
			throw new IllegalArgumentException("File version number is negative");
		this.fileVersion = fileVersion;
	}
	
	public void setVersion(int version) {
		if(version < 0)
			throw new IllegalArgumentException("Version number is negative");
		this.version = version;
	}
	
	public void setWriterName(String writerName) {
		this.writerName = writerName;
	}
	
	public void setReaderName(String readerName) {
		this.readerName = readerName;
	}
	
	public void write(OutputStream os) throws IOException {
		if(data == null) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			IOUtils.writeIntMM(bout, version);
			bout.write(hasRealMergedData?1:0);
			byte[] writerNameBytes = null;
			writerNameBytes = writerName.getBytes("UTF-16BE");
			IOUtils.writeIntMM(bout, writerName.length());
			bout.write(writerNameBytes);
			byte[] readerNameBytes = null;
			readerNameBytes = readerName.getBytes("UTF-16BE");
			IOUtils.writeIntMM(bout, readerName.length());
			bout.write(readerNameBytes);
			IOUtils.writeIntMM(bout, fileVersion);
			data = bout.toByteArray();
			size = data.length;
		}
		super.write(os);
	}
}