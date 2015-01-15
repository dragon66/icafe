/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.List;

import cafe.image.ImageType;
import cafe.image.util.IMGUtils;

/**
 * Base class for image metadata.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/12/2015
 */
public abstract class Metadata {
	private MetadataType type;
	private byte[] data;
	
	/**
	 * Reads all metadata associated with the input image
	 *
	 * @param is InputStream for the image
	 * @return a list of Metadata for the input stream
	 * @throws IOException
	 */
	public static List<Metadata> readMetadata(InputStream is) throws IOException {
		// 4 byte as image magic number
		PushbackInputStream pushBackStream = new PushbackInputStream(is, 4); 
		@SuppressWarnings("unused")
		ImageType imageType = IMGUtils.guessImageType(pushBackStream);
		// TODO - change corresponding tweaker snoop() to return a list of Metadata
		pushBackStream.close();
		
		return null;
	}
	
	public static List<Metadata> readMetadata(File image) throws IOException {
		FileInputStream fin = new FileInputStream(image);
		List<Metadata> metadataList = readMetadata(fin);
		fin.close();
		
		return metadataList; 
	}
	
	public static List<Metadata> readMetadata(String image) throws IOException {
		return readMetadata(new File(image));
	}
	
	public Metadata(MetadataType type, byte[] data) {
		this.type = type;
		this.data = data;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public MetadataType getType() {
		return type;
	}
	
	public abstract MetadataReader getReader();
	
	public void showMetadata() {
		MetadataReader reader = getReader();
		if(reader != null)
			reader.showMetadata();
	}
	
	/**
	 * Writes the metadata out to the output stream
	 * 
	 * @param out OutputStream to write the metadata to
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		out.write(data);
	}	
}