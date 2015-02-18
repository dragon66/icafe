/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.adobe;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cafe.image.meta.MetadataReader;
import cafe.io.IOUtils;
import cafe.util.ArrayUtils;

/**
 * Photoshop Image Resource Block reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/10/2015
 */
public class IRBReader implements MetadataReader {
	private byte[] data;
	private boolean containsThumbnail;
	private IRBThumbnail thumbnail;
	private boolean loaded;
	Map<Short, _8BIM> _8bims = new HashMap<Short, _8BIM>();
	
	public IRBReader(byte[] data) {
		this.data = data;
	}
	
	public boolean containsThumbnail() {
		return containsThumbnail;
	}
	
	public Map<Short, _8BIM> get8BIM() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableMap(_8bims);
	}
	
	public IRBThumbnail getThumbnail()  {
		return thumbnail;
	}
	
	public boolean isDataLoaded() {
		return loaded;
	}
	
	@Override
	public void read() throws IOException {
		int i = 0;
		while((i+4) < data.length) {
			String _8bim = new String(data, i, 4);
			i += 4;			
			if(_8bim.equals("8BIM")) {
				short id = IOUtils.readShortMM(data, i);
				i += 2;
				// Pascal string for name follows
				// First byte denotes string length -
				int nameLen = data[i++]&0xff;
				if((nameLen%2) == 0) nameLen++;
				String name = new String(data, i, nameLen).trim();
				i += nameLen;
				//
				int size = IOUtils.readIntMM(data, i);
				i += 4;
				
				ImageResourceID eId =ImageResourceID.fromShort(id); 
				
				_8bims.put(id, new _8BIM(id, name, size, ArrayUtils.subArray(data, i, size)));
				
				if(eId == ImageResourceID.THUMBNAIL_RESOURCE_PS4 || eId == ImageResourceID.THUMBNAIL_RESOURCE_PS5) {
					containsThumbnail = true;
					int thumbnailFormat = IOUtils.readIntMM(data, i); //1 = kJpegRGB. Also supports kRawRGB (0).
					int width = IOUtils.readIntMM(data, i + 4);
					int height = IOUtils.readIntMM(data, i + 8);
					// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
					int widthBytes = IOUtils.readIntMM(data, i + 12);
					// Total size = widthbytes * height * planes
					int totalSize = IOUtils.readIntMM(data, i + 16);
					// Size after compression. Used for consistency check.
					int sizeAfterCompression = IOUtils.readIntMM(data, i + 20);
					short bitsPerPixel = IOUtils.readShortMM(data, i + 24); // Bits per pixel. = 24
					short numOfPlanes = IOUtils.readShortMM(data, i + 26); // Number of planes. = 1
					byte[] thumbnailData = null;
					if(thumbnailFormat == IRBThumbnail.DATA_TYPE_KJpegRGB)
						thumbnailData = ArrayUtils.subArray(data, i + 28, sizeAfterCompression);
					else if(thumbnailFormat == IRBThumbnail.DATA_TYPE_KRawRGB)
						thumbnailData = ArrayUtils.subArray(data, i + 28, totalSize);
					// JFIF data in RGB format. For resource ID 1033 (0x0409) the data is in BGR format.
					thumbnail = new IRBThumbnail(eId, thumbnailFormat, width, height, widthBytes, totalSize, sizeAfterCompression, bitsPerPixel, numOfPlanes, thumbnailData);
				}				
				i += size;
				if(size%2 != 0) i++; // Skip padding byte
			}
		}
		loaded = true;
	}
	
	public void showMetadata() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		System.out.println("<<Adobe IRB information starts>>");
		for(_8BIM _8bim : _8bims.values()) {
			_8bim.print();
		}
		if(containsThumbnail) {
			System.out.println(thumbnail.getResouceID());
			int thumbnailFormat = thumbnail.getDataType(); //1 = kJpegRGB. Also supports kRawRGB (0).
			switch (thumbnailFormat) {
				case IRBThumbnail.DATA_TYPE_KJpegRGB:
					System.out.println("Thumbnail format: KJpegRGB");
					break;
				case IRBThumbnail.DATA_TYPE_KRawRGB:
					System.out.println("Thumbnail format: KRawRGB");
					break;
			}
			System.out.println("Thumbnail width: " + thumbnail.getWidth());
			System.out.println("Thumbnail height: " + thumbnail.getHeight());
			// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
			System.out.println("Padded row bytes: " + thumbnail.getPaddedRowBytes());
			// Total size = widthbytes * height * planes
			System.out.println("Total size: "  + thumbnail.getTotalSize());
			// Size after compression. Used for consistency check.
			System.out.println("Size after compression: " + thumbnail.getCompressedSize());
			// Bits per pixel. = 24
			System.out.println("Bits per pixel: " + thumbnail.getBitsPerPixel());
			// Number of planes. = 1
			System.out.println("Number of planes: "  + thumbnail.getNumOfPlanes());
		}
		
		System.out.println("<<Adobe IRB information ends>>");
	}
}