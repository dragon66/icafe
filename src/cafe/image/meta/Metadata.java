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
import java.util.HashMap;
import java.util.Map;

import cafe.image.ImageIO;
import cafe.image.ImageType;
import cafe.image.bmp.BMPTweaker;
import cafe.image.gif.GIFTweaker;
import cafe.image.jpeg.JPEGTweaker;
import cafe.image.png.PNGTweaker;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.util.IMGUtils;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.RandomAccessInputStream;

/**
 * Base class for image metadata.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/12/2015
 */
public abstract class Metadata {
	// Fields
	private MetadataType type;
	private byte[] data;
	
	public static Map<MetadataType, Metadata> readMetadata(File image) throws IOException {
		FileInputStream fin = new FileInputStream(image);
		Map<MetadataType, Metadata> metadataMap = readMetadata(fin);
		fin.close();
		
		return metadataMap; 
	}
	
	/**
	 * Reads all metadata associated with the input image
	 *
	 * @param is InputStream for the image
	 * @return a list of Metadata for the input stream
	 * @throws IOException
	 */
	public static Map<MetadataType, Metadata> readMetadata(InputStream is) throws IOException {
		// Metadata map for all the Metadata read
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		// 4 byte as image magic number
		PushbackInputStream pushbackStream = new PushbackInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(pushbackStream);		
		// Delegate metadata reading to corresponding image tweakers.
		switch(imageType) {
			case JPG:
				metadataMap = JPEGTweaker.readMetadata(pushbackStream);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(pushbackStream);
				metadataMap = TIFFTweaker.readMetadata(randIS);
				randIS.close();
				break;
			case PNG:
				metadataMap = PNGTweaker.readMetadata(pushbackStream);
				break;
			case GIF:
				metadataMap = GIFTweaker.readMetadata(pushbackStream);
				break;
			case BMP:
				metadataMap = BMPTweaker.readMetadata(pushbackStream);
				break;
			default:
				pushbackStream.close();
				throw new IllegalArgumentException("Metadata reading is not supported for " + imageType + " image");
				
		}		
		pushbackStream.close();
		
		return metadataMap;
	}
	
	public static Map<MetadataType, Metadata> readMetadata(String image) throws IOException {
		return readMetadata(new File(image));
	}
	
	public Metadata(MetadataType type, byte[] data) {
		this.type = type;
		this.data = data;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public abstract MetadataReader getReader();
	
	public MetadataType getType() {
		return type;
	}
	
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