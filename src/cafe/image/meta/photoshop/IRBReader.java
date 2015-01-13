/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.photoshop;

import java.io.IOException;
import cafe.image.meta.iptc.IPTCReader;
import cafe.io.IOUtils;
import cafe.string.StringUtils;
import cafe.util.ArrayUtils;
import cafe.util.Reader;

/**
 * Photoshop Image Resource Block reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/10/2015
 */
public class IRBReader implements Reader {
	private byte[] data;
	private boolean containsThumbnail;
	private IRBThumbnail thumbnail;
	
	public IRBReader(byte[] data) {
		this.data = data;
	}
	
	public boolean containsThumbnail() {
		return containsThumbnail;
	}
	
	public IRBThumbnail getThumbnail()  {
		return thumbnail;
	}
	
	@Override
	public void read() throws IOException {
		int i = 0;
		while((i+4) < data.length) {
			String _8bim = new String(data, i, 4);
			System.out.println("Type: " + _8bim);
			i += 4;			
			if(_8bim.equals("8BIM")) {
				short id = IOUtils.readShortMM(data, i);
				i += 2;
				int nameLen = (data[i++]&0xff);
				System.out.println("Name: " + new String(data, i, nameLen).trim());
				i += nameLen;
				if((nameLen%2) == 0) i++;
				int size = IOUtils.readIntMM(data, i);
				i += 4;
				System.out.println("Size: " + size);
				
				ImageResourceID eId =ImageResourceID.fromShort(id); 
				
				if((id >= ImageResourceID.PATH_INFO0.getValue()) && (id <= ImageResourceID.PATH_INFO998.getValue())) {
					System.out.println("PATH_INFO" + " [Value: " + StringUtils.shortToHexStringMM(id) +"]" + " - Path Information (saved paths).");
				}
				else if((id >= ImageResourceID.PLUGIN_RESOURCE0.getValue()) && (id <= ImageResourceID.PLUGIN_RESOURCE999.getValue())) {
					System.out.println("PLUGIN_RESOURCE" + " [Value: " + StringUtils.shortToHexStringMM(id) +"]" + " - Plug-In resource.");
				}
				else if (eId == ImageResourceID.UNKNOWN) {
					System.out.println(eId + " [Value: " + StringUtils.shortToHexStringMM(id) +"]");
				}
				else {
					System.out.println(eId);
				}
				
				switch (eId) {
					case IPTC_NAA: // IPTC
						/* Structure of an IPTC data set
						   [Record name]    [size]   [description]
						   ---------------------------------------
						   (Tag marker)     1 byte   this must be 0x1c
						   (Record number)  1 byte   always 2 for 2:xx datasets
						   (Dataset number) 1 byte   this is what we call a "tag"
						   (Size specifier) 2 bytes  data length (< 32768 bytes) or length of ...
						   (Size specifier)  ...     data length (> 32767 bytes only)
						   (Data)            ...     (its length is specified before)
						 */
						new IPTCReader(ArrayUtils.subArray(data, i, size)).read();
						i += size;
						if(size%2 != 0) i++;
						break;
					case JPEG_QUALITY: // PhotoShop Save As Quality
						// index 0: Quality level
						int value = IOUtils.readShortMM(data, i);
						i += 2;
						switch (value) {
							case 0xfffd:
								System.out.print("Quality 1 (Low)");
								break;
							case 0xfffe:
								System.out.print("Quality 2 (Low)");
								break;
							case 0xffff:
								System.out.print("Quality 3 (Low)");
								break;
							case 0x0000:
								System.out.print("Quality 4 (Low)");
								break;
							case 0x0001:
								System.out.print("Quality 5 (Medium)");
								break;
							case 0x0002:
								System.out.print("Quality 6 (Medium)");
								break;
							case 0x0003:
								System.out.print("Quality 7 (Medium)");
								break;
							case 0x0004:
								System.out.print("Quality 8 (High)");
								break;
							case 0x0005:
								System.out.print("Quality 9 (High)");
								break;
							case 0x0006:
								System.out.print("Quality 10 (Maximum)");
								break;
							case 0x0007:
								System.out.print("Quality 11 (Maximum)");
								break;
							case 0x0008:
								System.out.print("Quality 12 (Maximum)");
								break;
							default:
						}
						
						int format = IOUtils.readShortMM(data, i);
						i += 2;
						System.out.print(" : ");
						
						switch (format) {
							case 0x0000:
								System.out.print("Standard Format");
								break;
							case 0x0001:
								System.out.print("Optimised Format");
								break;
							case 0x0101:
								System.out.print("Progressive Format");
								break;
							default:
						}
						
						int progressiveScans = IOUtils.readShortMM(data, i);
						i += 2;
						System.out.print(" : ");
						
						switch (progressiveScans) {
							case 0x0001:
								System.out.print("3 Scans");
								break;
							case 0x0002:
								System.out.print("4 Scans");
								break;
							case 0x0003:
								System.out.print("5 Scans");
								break;
							default:
						}
						
						System.out.println(" - Plus 1 byte unknown trailer value = " + data[i++]); // Always seems to be 0x01
						if(size%2 != 0) i++;
						break;
					case THUMBNAIL_RESOURCE_PS4:
					case THUMBNAIL_RESOURCE_PS5:
						containsThumbnail = true;
						int thumbnailFormat = IOUtils.readIntMM(data, i); //1 = kJpegRGB. Also supports kRawRGB (0).
						i += 4;
						switch (thumbnailFormat) {
							case IRBThumbnail.FORMAT_KJpegRGB:
								System.out.println("Thumbnail format: KJpegRGB");
								break;
							case IRBThumbnail.FORMAT_KRawRGB:
								System.out.println("Thumbnail format: KRawRGB");
								break;
						}
						int width = IOUtils.readIntMM(data, i);
						System.out.println("Thumbnail width: " + width);
						i += 4;
						int height = IOUtils.readIntMM(data, i);
						System.out.println("Thumbnail height: " + height);
						i += 4;
						// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
						int widthBytes = IOUtils.readIntMM(data, i);
						System.out.println("Padded row bytes: " + widthBytes);
						i += 4;
						// Total size = widthbytes * height * planes
						int totalSize = IOUtils.readIntMM(data, i);
						System.out.println("Total size: "  + totalSize);
						i += 4;
						// Size after compression. Used for consistency check.
						int sizeAfterCompression = IOUtils.readIntMM(data, i);
						System.out.println("Size after compression: " + sizeAfterCompression);
						i += 4;
						short bitsPerPixel = IOUtils.readShortMM(data, i); // Bits per pixel. = 24
						System.out.println("Bits per pixel: " + bitsPerPixel);
						i += 2;
						short numOfPlanes = IOUtils.readShortMM(data, i); // Number of planes. = 1
						System.out.println("Number of planes: "  + numOfPlanes);
						i += 2;
						byte[] thumbnailData = null;
						if(thumbnailFormat == IRBThumbnail.FORMAT_KJpegRGB)
							thumbnailData = ArrayUtils.subArray(data, i, sizeAfterCompression);
						else if(thumbnailFormat == IRBThumbnail.FORMAT_KRawRGB)
							thumbnailData = ArrayUtils.subArray(data, i, totalSize);
						// JFIF data in RGB format. For resource ID 1033 (0x0409) the data is in BGR format.
						i += sizeAfterCompression; 
						if(size%2 != 0) i++;
						thumbnail = new IRBThumbnail(eId, thumbnailFormat, width, height, widthBytes, totalSize, sizeAfterCompression, bitsPerPixel, numOfPlanes, thumbnailData);
						break;
					case VERSION_INFO:
						System.out.println("Version: " + StringUtils.byteArrayToHexString(ArrayUtils.subArray(data, i, 4)));
						i += 4;
                        System.out.println("Has Real Merged Data: " + ((data[i++]!=0)?"True":"False"));
                        int writer_size = IOUtils.readIntMM(data, i);
                        i += 4;
                        System.out.println("Writer name: " + new String(data, i, writer_size*2, "UTF-16BE"));
                        i += writer_size*2;
                        int reader_size = IOUtils.readIntMM(data, i);
                        i += 4;
                        System.out.println("Reader name: " + new String(data, i, reader_size*2, "UTF-16BE"));
                        i += reader_size*2;
                        System.out.println("File Version: " + StringUtils.byteArrayToHexString(ArrayUtils.subArray(data, i, 4)));                           
                        i += 4;
                        if(size%2 != 0) i++;
                        break;
					default:							
						i += size;
						if(size%2 != 0) i++;
				}					
			}
		}
	}
}
