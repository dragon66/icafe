/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * JPEGTweaker.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    05Jan2015  Enhanced to show information for all SOFX and SOS segments
 * WY    07Oct2014  Revised readAPP1() to show Adobe XMP information
 * WY    02Oct2014  Renamed extractExifThumbnail() to extractThumbnail()
 * WY    02Oct2014  Removed readExif()
 * WY    01Oct2014  Added code to read APP13 thumbnail
 * WY    29Sep2014  Added insertICCProfile(InputStream, OutputStream, ICCProfile)
 * WY    29Sep2014  Added writeICCProfile(OutputStream, ICCProfile)
 * WY    29Sep2014  Added getICCProfile(InputStream)
 * WY    29Sep2014  Removed showICCProfile(byte[])
 * WY    14Sep2014  Added removeExif() to remove EXIF data
 * WY    14Sep2014  Changed insertICCProfile() to remove old profile
 * WY    29Aug2014  Changed removeAPP1() to more general removeAPPn()
 * WY    07Jun2014  Added extractExifThumbnail() to extract thumbnail
 * WY    07Jun2014  Added insertICCProfile() to insert ICC_Profile
 * WY    06Jun2014  Added extractICCProfile() to extract ICC_Profile
 * WY    03Apr2014  Added snoop() as a result of delete JPEGSnoop
 */

package cafe.image.jpeg;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cafe.image.ImageIO;
import cafe.image.ImageType;
import cafe.image.writer.ImageWriter;
import cafe.image.meta.exif.Exif;
import cafe.image.meta.icc.ICCProfile;
import cafe.image.meta.photoshop.IRBReader;
import cafe.image.meta.photoshop.ImageResourceID;
import cafe.image.tiff.IFD;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffTag;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;
import cafe.string.StringUtils;
import cafe.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JPEG image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/25/2013
 */
public class JPEGTweaker {
	/** Copy a single SOS segment */	
	@SuppressWarnings("unused")
	private static short copySOS(InputStream is, OutputStream os) throws IOException {
		// Need special treatment.
		int nextByte = 0;
		short marker = 0;	
		
		while((nextByte = IOUtils.read(is)) != -1)
		{
			if(nextByte == 0xff)
			{
				nextByte = IOUtils.read(is);
				
				if (nextByte == -1) {
					throw new IOException("Premature end of SOS segment!");					
				}								
				
				if (nextByte != 0x00) // This is a marker
				{
					marker = (short)((0xff<<8)|nextByte);
					
					switch (Marker.fromShort(marker)) {										
						case RST0:  
						case RST1:
						case RST2:
						case RST3:
						case RST4:
						case RST5:
						case RST6:
						case RST7:
							IOUtils.writeShortMM(os, marker);
							System.out.println(Marker.fromShort(marker));
							continue;
						default:											
					}
					break;
				}
				IOUtils.write(os, 0xff);
				IOUtils.write(os, nextByte);
			}
			else {
				IOUtils.write(os,  nextByte);				
			}			
		}
		
		if (nextByte == -1) {
			throw new IOException("Premature end of SOS segment!");
		}

		return marker;
	}
	
	private static void copyToEnd(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[10240]; // 10k buffer
		int bytesRead = -1;
		
		while((bytesRead = is.read(buffer)) != -1) {
			os.write(buffer, 0, bytesRead);
		}
	}
	
	public static byte[] extractICCProfile(InputStream is) throws IOException {
		// ICC_PROFILE identifier with trailing bytes [0x00].
		byte[] icc_profile_id = {0x49, 0x43, 0x43, 0x5f, 0x50, 0x52, 0x4f, 0x46, 0x49, 0x4c, 0x45, 0x00};		
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		// Flag when we are done
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
		{
			System.out.println("Invalid JPEG image, expected SOI marker not found!");
			return null;
		}
		
		System.out.println(Marker.SOI);
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished)
	    {	        
			if (Marker.fromShort(marker) == Marker.EOI)
			{
				System.out.println(Marker.EOI);
				finished = true;
			}
		   	else // Read markers
			{
		   		emarker = Marker.fromShort(marker);
				System.out.println(emarker); 
	
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone marker besides SOI, EOI, and RSTn. 
						marker = IOUtils.readShortMM(is);
						break;
				    case PADDING:	
				    	int nextByte = 0;
				    	while((nextByte = IOUtils.read(is)) == 0xff) {;}
				    	marker = (short)((0xff<<8)|nextByte);
				    	break;				
				    case SOS:	
				    	//marker = skipSOS(is);
				    	finished = true;
						break;
				    case APP2:
				    	byte[] icc_profile_buf = new byte[12];
						length = IOUtils.readUnsignedShortMM(is);						
						IOUtils.readFully(is, icc_profile_buf);		
						// ICC_PROFILE segment.
						if (Arrays.equals(icc_profile_buf, icc_profile_id)) {
							icc_profile_buf = new byte[length-14];
						    IOUtils.readFully(is, icc_profile_buf);
						    bo.write(icc_profile_buf, 2, length-16);
				  		} else {
				  			IOUtils.skipFully(is, length-14);
				  		}
						marker = IOUtils.readShortMM(is);
						break;
				    default:
					    length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];					   
					    IOUtils.readFully(is, buf);				
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
		
		return bo.toByteArray();
	}
	
	public static void extractICCProfile(InputStream is, String pathToICCProfile) throws IOException {
		byte[] icc_profile = extractICCProfile(is);
		
		if(icc_profile != null && icc_profile.length > 0) {
			String outpath = "";
			if(pathToICCProfile.endsWith("\\") || pathToICCProfile.endsWith("/"))
				outpath = pathToICCProfile + "icc_profile";
			else
				outpath = pathToICCProfile.replaceFirst("[.][^.]+$", "");
			OutputStream os = new FileOutputStream(outpath + ".icc");
			os.write(icc_profile);
			os.close();
		}	
	}
	
	/**
	 * Extracts thumbnail images from JFIF/APP0, Exif APP1 and/or Adobe APP13 segment if any.
	 * 
	 * @param is InputStream for the JPEG image.
	 * @param pathToThumbnail a path or a path and name prefix combination for the extracted thumbnails.
	 * @throws IOException
	 */
	public static void extractThumbnails(InputStream is, String pathToThumbnail) throws IOException {
		// Flag when we are done
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
		{
			System.out.println("Invalid JPEG image, expected SOI marker not found!");
			return;
		}
		
		System.out.println(Marker.SOI);
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished)
	    {	        
			if (Marker.fromShort(marker) == Marker.EOI)
			{
				System.out.println(Marker.EOI);
				finished = true;
			}
		   	else // Read markers
			{
		   		emarker = Marker.fromShort(marker);
				System.out.println(emarker); 
	
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone marker besides SOI, EOI, and RSTn. 
				    	marker = IOUtils.readShortMM(is);
						break;
				    case PADDING:	
				    	int nextByte = 0;
				    	while((nextByte = IOUtils.read(is)) == 0xff) {;}
				    	marker = (short)((0xff<<8)|nextByte);
				    	break;				
				    case SOS:	
						finished = true;
						break;
				    case APP0:
				    	byte[] jfif = {0x4A, 0x46, 0x49, 0x46, 0x00}; // JFIF
						byte[] jfxx = {0x4A, 0x46, 0x58, 0x58, 0x00}; // JFXX
						length = IOUtils.readUnsignedShortMM(is);
						byte[] jfif_buf = new byte[length-2];
					    IOUtils.readFully(is, jfif_buf);
					    // EXIF segment
					    if(Arrays.equals(ArrayUtils.subArray(jfif_buf, 0, 5), jfif) || Arrays.equals(ArrayUtils.subArray(jfif_buf, 0, 5), jfxx)) {
					      	int thumbnailWidth = jfif_buf[12]&0xff;
					    	int thumbnailHeight = jfif_buf[13]&0xff;
					    	String outpath = "";
							if(pathToThumbnail.endsWith("\\") || pathToThumbnail.endsWith("/"))
								outpath = pathToThumbnail + "jfif_thumbnail";
							else
								outpath = pathToThumbnail.replaceFirst("[.][^.]+$", "") + "_jfif_t";
					    	
					    	if(thumbnailWidth != 0 && thumbnailHeight != 0) { // There is a thumbnail
					    		// Extract the thumbnail
					    		//Create a BufferedImage
					    		int size = 3*thumbnailWidth*thumbnailHeight;
								DataBuffer db = new DataBufferByte(ArrayUtils.subArray(jfif_buf, 14, size), size);
								int[] off = {0, 1, 2};//RGB band offset, we have 3 bands
								int numOfBands = 3;
								int trans = Transparency.OPAQUE;
									
								WritableRaster raster = Raster.createInterleavedRaster(db, thumbnailWidth, thumbnailHeight, 3*thumbnailWidth, numOfBands, off, null);
								ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, trans, DataBuffer.TYPE_BYTE);
						   		BufferedImage bi = new BufferedImage(cm, raster, false, null);
								// Create a new writer to write the image
								ImageWriter writer = ImageIO.getWriter(ImageType.JPG);
								FileOutputStream fout = new FileOutputStream(outpath + ".jpg");
								try {
									writer.write(bi, fout);
								} catch (Exception e) {
									e.printStackTrace();
								}
								fout.close();		
					    	}
					    }
				    	marker = IOUtils.readShortMM(is);
						break;
				    case APP1:
				    	// EXIF identifier with trailing bytes [0x00,0x00] or [0x00,0xff].
						byte[] exif = {0x45, 0x78, 0x69, 0x66, 0x00, 0x00};
						byte[] exif2 = {0x45, 0x78, 0x69, 0x66, 0x00, (byte)0xff};
						byte[] exif_buf = new byte[6];
						length = IOUtils.readUnsignedShortMM(is);						
						IOUtils.readFully(is, exif_buf);						
						// EXIF segment.
						if (Arrays.equals(exif_buf, exif)||Arrays.equals(exif_buf, exif2)) {
							exif_buf = new byte[length-8];
						    IOUtils.readFully(is, exif_buf);
						    RandomAccessInputStream tiffin = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(exif_buf));
							List<IFD> ifds = new ArrayList<IFD>();
						    TIFFTweaker.readIFDs(ifds, tiffin);
						    if(ifds.size() >= 2) {
						    	String outpath = "";
								if(pathToThumbnail.endsWith("\\") || pathToThumbnail.endsWith("/"))
									outpath = pathToThumbnail + "exif_thumbnail";
								else
									outpath = pathToThumbnail.replaceFirst("[.][^.]+$", "") + "_exif_t";
						    	IFD thumbnailIFD = ifds.get(1);
						    	TiffField<?> field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue());
						    	if(field != null) { // JPEG format, save as JPEG
						    		int thumbnailOffset = field.getDataAsLong()[0];
						    		field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue());
						    		int thumbnailLen = field.getDataAsLong()[0];
						    		tiffin.seek(thumbnailOffset);
						    		byte[] data = new byte[thumbnailLen];
						    		tiffin.readFully(data);
						    		OutputStream fout = new FileOutputStream(outpath + ".jpg");
						    		fout.write(data);
						    		fout.close();
						    	} else { // Uncompressed, save as TIFF
						    		field = thumbnailIFD.getField(TiffTag.STRIP_OFFSETS.getValue());
						    		if(field == null) 
						    			field = thumbnailIFD.getField(TiffTag.TILE_OFFSETS.getValue());
						    		if(field != null) {
						    			 tiffin.seek(0);
						    			 OutputStream fout = new FileOutputStream(outpath + ".tif");
						    			 RandomAccessOutputStream tiffout = new FileCacheRandomAccessOutputStream(fout);
						    			 TIFFTweaker.retainPages(tiffin, tiffout, 1);
						    			 tiffout.close(); // Auto flush when closed
						    			 fout.close();
						    		}
						    	}
						    }
						    tiffin.close();
						} else {
							IOUtils.skipFully(is, length - 8);
						}
						marker = IOUtils.readShortMM(is);
						break;
				    case APP13:
				    	length = IOUtils.readUnsignedShortMM(is);
						byte[] data = new byte[length-2];
						IOUtils.readFully(is, data, 0, length-2);						
						int i = 0;
						boolean done = false;
						
						while(data[i] != 0) i++;
						
						if(new String(data, 0, i++).equals("Photoshop 3.0")) {
							while((i+4) < data.length && !done) {
								String _8bim = new String(data, i, 4);
								i += 4;			
								if(_8bim.equals("8BIM")) {
									short id = IOUtils.readShortMM(data, i);
									i += 2;
									int nameLen = (data[i++]&0xff);
									i += nameLen;
									if((nameLen%2) == 0) i++;
									int size = IOUtils.readIntMM(data, i);
									i += 4;
									
									ImageResourceID eId =ImageResourceID.fromShort(id); 
									
									switch (eId) {										
										case THUMBNAIL_RESOURCE_PS4:
										case THUMBNAIL_RESOURCE_PS5:
											String outpath = "";
											if(pathToThumbnail.endsWith("\\") || pathToThumbnail.endsWith("/"))
												outpath = pathToThumbnail + "photoshop_thumbnail";
											else
												outpath = pathToThumbnail.replaceFirst("[.][^.]+$", "") + "_photoshop_t";
											int thumbnailFormat = IOUtils.readIntMM(data, i); //1 = kJpegRGB. Also supports kRawRGB (0).
											i += 4;
											int width = IOUtils.readIntMM(data, i);
											i += 4;
											int height = IOUtils.readIntMM(data, i);
											i += 4;
											// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
											int widthBytes = IOUtils.readIntMM(data, i);
											i += 4;
											// Total size = widthbytes * height * planes
											int totalSize = IOUtils.readIntMM(data, i);
											i += 4;
											// Size after compression. Used for consistency check.
											int sizeAfterCompression = IOUtils.readIntMM(data, i);
											i += 4;
											IOUtils.readShortMM(data, i); // Bits per pixel. = 24
											i += 2;
											IOUtils.readShortMM(data, i); // Number of planes. = 1
											i += 2; 	
											// JFIF data in RGB format. For resource ID 1033 (0x0409) the data is in BGR format.
											if(thumbnailFormat == 1) {
												// Note: Not sure whether or not this will create wrong color JPEG
												// if it's written by Photoshop 4.0!
												OutputStream fout = new FileOutputStream(outpath + ".jpg");
									    		fout.write(data, i, sizeAfterCompression);
									    		fout.close();
											} else if(thumbnailFormat == 0) {
												// kRawRGB - NOT tested yet!
												//Create a BufferedImage
												DataBuffer db = new DataBufferByte(ArrayUtils.subArray(data, i, totalSize), totalSize);
												int[] off = {0, 1, 2};//RGB band offset, we have 3 bands
												if(eId == ImageResourceID.THUMBNAIL_RESOURCE_PS4)
													off = new int[]{2, 1, 0}; // RGB band offset for BGR for photoshop4.0 BGR format
												int numOfBands = 3;
												int trans = Transparency.OPAQUE;
													
												WritableRaster raster = Raster.createInterleavedRaster(db, width, height, widthBytes, numOfBands, off, null);
												ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, trans, DataBuffer.TYPE_BYTE);
										   		BufferedImage bi = new BufferedImage(cm, raster, false, null);
												// Create a new writer to write the image
												ImageWriter writer = ImageIO.getWriter(ImageType.JPG);
												FileOutputStream fout = new FileOutputStream(outpath + ".jpg");
												try {
													writer.write(bi, fout);
												} catch (Exception e) {
													e.printStackTrace();
												}
												fout.close();								
											}
											i += sizeAfterCompression; 
											if(size%2 != 0) i++;
											done = true;
											break;										
										default:							
											i += size;
											if(size%2 != 0) i++;
									}					
								}
							}
						}				
				    	marker = IOUtils.readShortMM(is);
				    	break;
				    default:
					    length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	public static ICCProfile getICCProfile(InputStream is) throws IOException {
		ICCProfile profile = null;
		byte[] buf = extractICCProfile(is);
		if(buf.length > 0)
			profile = new ICCProfile(buf);
		return profile;
	}
	
	/**
	 * @param is input image stream 
	 * @param os output image stream
	 * @param exif Exif instance
	 * @throws Exception 
	 */
	public static void insertExif(InputStream is, OutputStream os, Exif exif) throws Exception {
		// We need thumbnail image but don't have one, create one from the current image input stream
		if(exif.isThumbnailRequired() && !exif.hasThumbnail()) {
			is = new FileCacheRandomAccessInputStream(is);
			BufferedImage original = javax.imageio.ImageIO.read(is);
			int imageWidth = original.getWidth();
			int imageHeight = original.getHeight();
			// Default thumbnail dimension
			int thumbnailWidth = 160;
			int thumbnailHeight = 120;
			if(imageWidth < imageHeight) {
				// Swap width and height to keep a relative aspect ratio
				int temp = thumbnailWidth;
				thumbnailWidth = thumbnailHeight;
				thumbnailHeight = temp;
			}			
			if(imageWidth < thumbnailWidth) thumbnailWidth = imageWidth;
			if(imageHeight < thumbnailHeight) thumbnailHeight = imageHeight;
			BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = thumbnail.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(original, 0, 0, thumbnailWidth, thumbnailHeight, null);
			// Insert thumbnail into EXIF wrapper
			exif.setThumbnail(thumbnail);
			// Reset the stream pointer
			((RandomAccessInputStream)is).seek(0);
		}
		// Copy the original image and insert EXIF data
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
		{
			System.out.println("Invalid JPEG image, expected SOI marker not found!");
			is.close();
			os.close();		
			return;
		}
		
		System.out.println(Marker.SOI);
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished)
	    {	        
			if (Marker.fromShort(marker) == Marker.EOI)
			{
				IOUtils.writeShortMM(os, Marker.EOI.getValue());
				System.out.println(Marker.EOI);
				finished = true;
			}
		   	else // Read markers
			{
		   		emarker = Marker.fromShort(marker);
				System.out.println(emarker); 
	
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone marker besides SOI, EOI, and RSTn. 
						IOUtils.writeShortMM(os, marker);
				    	marker = IOUtils.readShortMM(is);
						break;
				    case PADDING:	
				    	IOUtils.writeShortMM(os, marker);
				    	int nextByte = 0;
				    	while((nextByte = IOUtils.read(is)) == 0xff) {
				    		IOUtils.write(os, nextByte);
				    	}
				    	marker = (short)((0xff<<8)|nextByte);
				    	break;				
				    case SOS: 
				    	// We add EXIF data right before the SOS segment.
				    	// Another position to add EXIF data would be right
				    	// after SOI marker
				    	exif.write(os);
				    	IOUtils.writeShortMM(os, marker);
						copyToEnd(is, os);
						finished = true; // No more marker to read, we are done. 
						break;
				    case APP1:
				    	readAPP1(is);
						marker = IOUtils.readShortMM(is);
						break;
				    default:
					    length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.writeShortMM(os, marker);
					    IOUtils.writeShortMM(os, (short)length);
					    IOUtils.readFully(is, buf);
					    IOUtils.write(os, buf);
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
		// Close the input stream in case it's an instance of RandomAccessInputStream
		if(is instanceof RandomAccessInputStream)
			is.close();
	}
	
	public static void insertICCProfile(InputStream is, OutputStream os, byte[] data) throws Exception {
		// Copy the original image and insert ICC_Profile data
		byte[] icc_profile_id = {0x49, 0x43, 0x43, 0x5f, 0x50, 0x52, 0x4f, 0x46, 0x49, 0x4c, 0x45, 0x00};
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
		{
			System.out.println("Invalid JPEG image, expected SOI marker not found!");
			return;
		}
		
		System.out.println(Marker.SOI);
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished)
	    {	        
			if (Marker.fromShort(marker) == Marker.EOI)
			{
				IOUtils.writeShortMM(os, Marker.EOI.getValue());
				System.out.println(Marker.EOI);
				finished = true;
			}
		   	else // Read markers
			{
		   		emarker = Marker.fromShort(marker);
				System.out.println(emarker); 
	
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone marker besides SOI, EOI, and RSTn. 
						IOUtils.writeShortMM(os, marker);
				    	marker = IOUtils.readShortMM(is);
						break;
				    case PADDING:	
				    	IOUtils.writeShortMM(os, marker);
				    	int nextByte = 0;
				    	while((nextByte = IOUtils.read(is)) == 0xff) {
				    		IOUtils.write(os, nextByte);
				    	}
				    	marker = (short)((0xff<<8)|nextByte);
				    	break;				
				    case SOS: 
				    	// We add ICC_Profile data right before the SOS segment.
				    	writeICCProfile(os, data);
				    	IOUtils.writeShortMM(os, marker);
						copyToEnd(is, os);
						finished = true; // No more marker to read, we are done. 
						break;
				    case APP2: // Remove old ICC_Profile
				    	byte[] icc_profile_buf = new byte[12];
						length = IOUtils.readUnsignedShortMM(is);						
						if(length < 14) { // This is not an ICC_Profile segment, copy it
							IOUtils.writeShortMM(os, marker);
							IOUtils.writeShortMM(os, (short)length);
							IOUtils.readFully(is, icc_profile_buf, 0, length-2);
							IOUtils.write(os, icc_profile_buf, 0, length-2);
						} else {
							IOUtils.readFully(is, icc_profile_buf);		
							// ICC_PROFILE segment.
							if (Arrays.equals(icc_profile_buf, icc_profile_id)) {
								IOUtils.skipFully(is, length-14);
							} else {// Not an ICC_Profile segment, copy it
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short)length);
								IOUtils.write(os, icc_profile_buf);
								icc_profile_buf = new byte[length-14];
								IOUtils.readFully(is, icc_profile_buf);
								IOUtils.write(os, icc_profile_buf);
							}
						}						
						marker = IOUtils.readShortMM(is);
						break;
				    default:
					    length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.writeShortMM(os, marker);
					    IOUtils.writeShortMM(os, (short)length);
					    IOUtils.readFully(is, buf);
					    IOUtils.write(os, buf);
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	public static void insertICCProfile(InputStream is, OutputStream os, ICC_Profile icc_profile) throws Exception {
		insertICCProfile(is, os, icc_profile.getData());
	}
	
	public static void insertICCProfile(InputStream is, OutputStream os, ICCProfile icc_profile) throws Exception {
		insertICCProfile(is, os, icc_profile.getData());
	}
	
	private static void readAPP0(InputStream is) throws IOException
	{
		byte[] jfif = {0x4A, 0x46, 0x49, 0x46, 0x00}; // JFIF
		byte[] jfxx = {0x4A, 0x46, 0x58, 0x58, 0x00}; // JFXX
		int length = IOUtils.readUnsignedShortMM(is);
		byte[] buf = new byte[length-2];
	    IOUtils.readFully(is, buf);
	    // JFIF segment
	    if(Arrays.equals(ArrayUtils.subArray(buf, 0, 5), jfif) || Arrays.equals(ArrayUtils.subArray(buf, 0, 5), jfxx)) {
	    	System.out.print(new String(buf, 0, 4));
	    	System.out.println(" - version " + (buf[5]&0xff) + "." + (buf[6]&0xff));
	    	System.out.print("Density unit: ");
	    	
	    	switch(buf[7]&0xff) {
	    		case 0:
	    			System.out.println("No units, aspect ratio only specified");
	    			break;
	    		case 1:
	    			System.out.println("Dots per inch");
	    			break;
	    		case 2:
	    			System.out.println("Dots per centimeter");
	    			break;
	    		default:
	    	}
	    	
	    	System.out.println("X density: " + IOUtils.readUnsignedShortMM(buf, 8));
	    	System.out.println("Y density: " + IOUtils.readUnsignedShortMM(buf, 10));
	    	int thumbnailWidth = buf[12]&0xff;
	    	int thumbnailHeight = buf[13]&0xff;
	    	System.out.println("Thumbnail dimension: " + thumbnailWidth + "X" + thumbnailHeight);	   
	    }
	}
	
	private static void readAPP1(InputStream is) throws IOException 
	{
		// EXIF identifier with trailing bytes [0x00,0x00] or [0x00,0xff].
		byte[] exif = {0x45, 0x78, 0x69, 0x66, 0x00, 0x00};
		byte[] exif2 = {0x45, 0x78, 0x69, 0x66, 0x00, (byte)0xff};
		String xmp = "http://ns.adobe.com/xap/1.0/\0";
		int length = IOUtils.readUnsignedShortMM(is);
		byte[] buf = new byte[length-2];
		IOUtils.readFully(is, buf);		
		// EXIF segment.
		if (Arrays.equals(ArrayUtils.subArray(buf, 0, 6), exif)||Arrays.equals(buf, exif2)) {
			RandomAccessInputStream randInputStream = new FileCacheRandomAccessInputStream(
					new ByteArrayInputStream(ArrayUtils.subArray(buf, 6, length-8)));
			List<IFD> list = new ArrayList<IFD>();
		    TIFFTweaker.readIFDs(list, randInputStream);
		   	randInputStream.close();
		} else if(new String(ArrayUtils.subArray(buf, 0, xmp.length())).equals(xmp)) {
			StringUtils.showXML(ArrayUtils.subArray(buf, xmp.length(), length - xmp.length() - 2));
			// For comparison purpose only
			//System.out.println(new String(ArrayUtils.subArray(buf, xmp.length(), length - xmp.length() - 2), "utf-8"));
  		}
	}
	
	private static void readAPP12(InputStream is) throws IOException 
	{
		// APP12 is either used by some old cameras to set PictureInfo
		// or Adobe PhotoShop to store Save for Web data - called Ducky segment.
		String[] duckyInfo = {"Ducky", "Photoshop Save For Web Quality: ", "Comment: ", "Copyright: "};
		byte[] ducky = {0x44, 0x75, 0x63, 0x6B, 0x79}; // "Ducky"
		byte[] pictureInfo = {0x51, 0x69, 0x63, 0x74, 0x75, 0x49, 0x6E, 0x66, 0x70}; // "PictureInfo"
		int length = IOUtils.readUnsignedShortMM(is);
		byte[] data = new byte[length-2];
		System.out.println("Length: " + length);
		IOUtils.readFully(is, data);
		int currPos = 0;
		byte[] buf = ArrayUtils.subArray(data, 0, 5);
		currPos += 5;
		
		if(Arrays.equals(ducky, buf)) {
			System.out.println("=>" + duckyInfo[0]);
			short tag = IOUtils.readShortMM(data, currPos);
			currPos += 2;
			
			while (tag != 0x0000) {
				System.out.println("Tag value: " + StringUtils.shortToHexStringMM(tag));
				
				int len = IOUtils.readUnsignedShortMM(data, currPos);
				currPos += 2;
				System.out.println("Tag length: " + len);
				
				switch (tag) {
					case 0x0001: // Image quality
						System.out.print(duckyInfo[1]);
						System.out.println(IOUtils.readUnsignedIntMM(data, currPos));
						currPos += 4;
						break;
					case 0x0002: // Comment
						System.out.print(duckyInfo[2]);
						System.out.println(new String(data, currPos, currPos + len).trim());
						currPos += len;
						break;
					case 0x0003: // Copyright
						System.out.print(duckyInfo[3]);
						System.out.println(new String(data, currPos, currPos + len).trim());
						currPos += len;
						break;
					default: // Do nothing!					
				}
				
				tag = IOUtils.readShortMM(data, currPos);
				currPos += 2;
			}			
		} else {
			buf = ArrayUtils.subArray(data, 0, 10);
			
			if (Arrays.equals(pictureInfo, buf)) {
				// TODO process PictureInfo.
			}			
		}
	}
	
	private static void readAPP13(InputStream is) throws IOException {
		int length = IOUtils.readUnsignedShortMM(is);
		byte[] data = new byte[length-2];
		IOUtils.readFully(is, data, 0, length-2);
		int i = 0;
		
		while(data[i] != 0) i++;
		
		if(new String(data, 0, i++).equals("Photoshop 3.0")) {
			System.out.println("Photoshop 3.0");
			new IRBReader(ArrayUtils.subArray(data, i, data.length - i)).read();			
		}		
	}
	
	private static void readAPP14(InputStream is) throws IOException 
	{
		byte[] adobe = {0x41, 0x64, 0x6f, 0x62, 0x65};
		String[] app14Info = {"DCTEncodeVersion: ", "APP14Flags0: ", "APP14Flags1: ", "ColorTransform: "};		
		int expectedLen = 14; // Expected length of this segment is 14.
		int length = IOUtils.readUnsignedShortMM(is);
		if (length >= expectedLen) { 
			byte[] data = new byte[length-2];
			IOUtils.readFully(is, data, 0, length-2);
			byte[] buf = ArrayUtils.subArray(data, 0, 5);
			
			if(Arrays.equals(buf, adobe)) {
				for (int i = 0, j = 5; i < 3; i++, j += 2) {
					System.out.println(app14Info[i] + StringUtils.shortToHexStringMM(IOUtils.readShortMM(data, j)));
				}
				System.out.println(app14Info[3] + (((data[11]&0xff) == 0)? "Unknown (RGB or CMYK)":
					((data[11]&0xff) == 1)? "YCbCr":"YCCK" ));
			}
		}		
	}
	
	private static void readAPP2(InputStream is) throws IOException {
		// ICC_PROFILE identifier with trailing bytes [0x00].
		byte[] icc_profile_id = {0x49, 0x43, 0x43, 0x5f, 0x50, 0x52, 0x4f, 0x46, 0x49, 0x4c, 0x45, 0x00};		
		byte[] buf = new byte[12];
		int length = IOUtils.readUnsignedShortMM(is);
		
		IOUtils.readFully(is, buf);		
		// ICC_PROFILE segment.
		if (Arrays.equals(buf, icc_profile_id)) {
			buf = new byte[length-14];
		    IOUtils.readFully(is, buf);
		    System.out.println("ICC_Profile marker #" + (buf[0]&0xff) + " of " + (buf[1]&0xff));
		    System.out.println("ICC_Profile data length : " + (length-16));
  		} else {
			IOUtils.skipFully(is, length-14);
		}
	}
	
	public static void readAPPn(InputStream is, Marker marker) throws IOException {
		switch (marker) {
			case APP0:
				readAPP0(is);
				break;
			case APP1:
				readAPP1(is);
				break;
			case APP2:
				readAPP2(is);
				break;
			case APP12:
				readAPP12(is);
				break;
			case APP13:
				readAPP13(is);
				break;
			case APP14:
				readAPP14(is);
				break;
			default:
		}
	}
	
	public static String readCOM(InputStream is) throws IOException 
	{
		int length = IOUtils.readUnsignedShortMM(is);
		byte[] data = new byte[length-2];
		IOUtils.readFully(is, data, 0, length-2);
		return new String(data).trim();
	}
	
	private static void readDHT(InputStream is, List<HTable> m_acTables, List<HTable> m_dcTables) throws IOException 
	{	
		final String[] HT_class_table = {"DC Component", "AC Component"};
	 			
		int len = IOUtils.readUnsignedShortMM(is);
        System.out.println("DHT segment length: " + len);       	
        byte buf[] = new byte[len - 2];
        IOUtils.readFully(is, buf);
		
		DHTReader reader = new DHTReader(new Segment(Marker.DHT, len, buf));
		
		List<HTable> dcTables = reader.getDCTables();
		List<HTable> acTables = reader.getACTables();
		
		m_acTables.addAll(acTables);
		m_dcTables.addAll(dcTables);
		
		List<HTable> tables = new ArrayList<HTable>(dcTables);
		tables.addAll(acTables);
		
		for(HTable table : tables )
		{
			System.out.println("Class: " + table.getComponentClass() + " (" + HT_class_table[table.getComponentClass()] + ")");
			System.out.println("Destination ID: " + table.getDestinationID());
			
			byte[] bits = table.getBits();
			byte[] values = table.getValues();
			
		    int count = 0;
			
			for (int i = 0; i < bits.length; i++)
			{
				count += (bits[i]&0xff);
			}
			
            System.out.println("Number of codes: " + count);
			
            if (count > 256)
			{
				System.out.println("invalid huffman code count!");			
				return;
			}
	        
            int j = 0;
            
			for (int i = 0; i < 16; i++) {
			
				System.out.print("Codes of length " + (i+1) + " (" + (bits[i]&0xff) +  " total): [ ");
				
				for (int k = 0; k < (bits[i]&0xff); k++) {
					System.out.print((values[j++]&0xff) + " ");
				}
				
				System.out.println("]");
			}
			
			System.out.println("**********************************");
		}
   	}
	
	// Process define Quantization table
	private static void readDQT(InputStream is, List<QTable> m_qTables) throws IOException
	{
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		DQTReader reader = new DQTReader(new Segment(Marker.DQT, len, buf));
		List<QTable> qTables = reader.getTables();
		m_qTables.addAll(qTables);
		
		int count = 0;
		  
		for(QTable table : qTables)
		{
			int QT_precision = table.getPrecision();
			short[] qTable = table.getTable();
			System.out.println("precision of QT is " + QT_precision);
			System.out.println("Quantization table #" + table.getIndex() + ":");
			
		   	if(QT_precision == 0) {
				for (int j = 0; j < 64; j++)
			    {
					if (j != 0 && j%8 == 0) {
						System.out.println();
					}
					
					System.out.print((qTable[j]&0xff) + " ");			
			    }
			} else { // 16 bit big-endian
								
				for (int j = 0; j < 64; j++) {
					if (j != 0 && j%8 == 0) {
						System.out.println();
					}
					
					System.out.print((qTable[j]&0xffff) + " ");	
				}				
			}
		   	
		   	count++;
		
			System.out.println();
			System.out.println("***************************");
		}
		
		System.out.println("Total number of Quantation tables: " + count);
		System.out.println("**********************************");
	}
	
	private static SOFReader readSOF(InputStream is, Marker marker) throws IOException 
	{		
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		Segment segment = new Segment(marker, len, buf);		
		SOFReader reader = new SOFReader(segment);
		
		System.out.println("Data length: " + len);		
		System.out.println("Precision: " + reader.getPrecision());
		System.out.println("Image height: " + reader.getFrameHeight());
		System.out.println("Image width: " + reader.getFrameWidth());
		System.out.println("# of Components: " + reader.getNumOfComponents());
		System.out.println(" (1 = grey scaled, 3 = color YCbCr or YIQ, 4 = color CMYK)");		
		    
		for(Component component:reader.getComponents()) {
			System.out.println();
			System.out.println("Component ID: " + component.getId());
			System.out.println("Herizontal sampling factor: " + component.getHSampleFactor());
			System.out.println("Vertical sampling factor: " + component.getVSampleFactor());
			System.out.println("Quantization table #: " + component.getQTableNumber());
		}
		
		System.out.println("**********************************");
		
		return reader;
	}	
	
	// This method is very slow if not wrapped in some kind of cache stream but it works for multiple
	// SOSs in case of progressive JPEG
	private static short readSOS(InputStream is, SOFReader sofReader) throws IOException 
	{
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		Segment segment = new Segment(Marker.SOS, len, buf);	
		new SOSReader(segment, sofReader);
		
		Component[] components = sofReader.getComponents();
		
		for(Component component : components) {
			System.out.println("Component ID: " + component.getId());
			System.out.println("Horizontal sampling factor: " + component.getHSampleFactor());
			System.out.println("Vertical sampling factor: " + component.getVSampleFactor());
			System.out.println("Quantization table #: " + component.getQTableNumber());
			System.out.println("DC table number: " + component.getDCTableNumber());
			System.out.println("AC table number: " + component.getACTableNumber());
		}
		
		System.out.println("****************************************");
		
		// Actual image data follow.
		int nextByte = 0;
		short marker = 0;	
		
		while((nextByte = IOUtils.read(is)) != -1)
		{
			if(nextByte == 0xff)
			{
				nextByte = IOUtils.read(is);
				
				if (nextByte == -1) {
					throw new IOException("Premature end of SOS segment!");					
				}								
				
				if (nextByte != 0x00)
				{
					marker = (short)((0xff<<8)|nextByte);
					
					switch (Marker.fromShort(marker)) {										
						case RST0:  
						case RST1:
						case RST2:
						case RST3:
						case RST4:
						case RST5:
						case RST6:
						case RST7:
							System.out.println(Marker.fromShort(marker));
							continue;
						default:											
					}
					break;
				}
			}
		}
		
		if (nextByte == -1) {
			throw new IOException("Premature end of SOS segment!");
		}

		return marker;
	}
	
	// Remove APPn segment
	public static void removeAPPn(Marker APPn, InputStream is, OutputStream os) throws IOException {
		if(APPn.getValue() < (short)0xffe0 || APPn.getValue() > (short)0xffef)
			throw new IllegalArgumentException("Input marker is not an APPn marker");		
		// Flag when we are done
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
		{
			System.out.println("Invalid JPEG image, expected SOI marker not found!");
			return;
		}
		
		System.out.println(Marker.SOI);
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished)
	    {	        
			if (Marker.fromShort(marker) == Marker.EOI)
			{
				IOUtils.writeShortMM(os, Marker.EOI.getValue());
				System.out.println(Marker.EOI);
				finished = true;
			}
		   	else // Read markers
			{
		   		emarker = Marker.fromShort(marker);
				System.out.println(emarker); 
	
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone marker besides SOI, EOI, and RSTn. 
						IOUtils.writeShortMM(os, marker);
				    	marker = IOUtils.readShortMM(is);
						break;
				    case PADDING:	
				    	IOUtils.writeShortMM(os, marker);
				    	int nextByte = 0;
				    	while((nextByte = IOUtils.read(is)) == 0xff) {
				    		IOUtils.write(os, nextByte);
				    	}
				    	marker = (short)((0xff<<8)|nextByte);
				    	break;				
				    case SOS:	
				    	IOUtils.writeShortMM(os, marker);
						// use copyToEnd instead for multiple SOS
				    	//marker = copySOS(is, os);
				    	copyToEnd(is, os);
						finished = true; 
						break;
				    default:
					    length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					    
					    if(emarker != APPn) {
					    	IOUtils.writeShortMM(os, marker);
					    	IOUtils.writeShortMM(os, (short)length);
					    	IOUtils.write(os, buf);
					    }
					    
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	// Remove EXIF segment
	public static void removeExif(InputStream is, OutputStream os) throws IOException {
		// Flag when we are done
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
		{
			System.out.println("Invalid JPEG image, expected SOI marker not found!");
			return;
		}
		
		System.out.println(Marker.SOI);
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished)
	    {	        
			if (Marker.fromShort(marker) == Marker.EOI)
			{
				IOUtils.writeShortMM(os, Marker.EOI.getValue());
				System.out.println(Marker.EOI);
				finished = true;
			}
		   	else // Read markers
			{
		   		emarker = Marker.fromShort(marker);
				System.out.println(emarker); 
	
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone marker besides SOI, EOI, and RSTn. 
						IOUtils.writeShortMM(os, marker);
				    	marker = IOUtils.readShortMM(is);
						break;
				    case PADDING:	
				    	IOUtils.writeShortMM(os, marker);
				    	int nextByte = 0;
				    	while((nextByte = IOUtils.read(is)) == 0xff) {
				    		IOUtils.write(os, nextByte);
				    	}
				    	marker = (short)((0xff<<8)|nextByte);
				    	break;				
				    case SOS:	
				    	IOUtils.writeShortMM(os, marker);
						// use copyToEnd instead for multiple SOS
				    	//marker = copySOS(is, os);
				    	copyToEnd(is, os);
						finished = true; 
						break;
				    case APP1:
				    	// EXIF identifier with trailing bytes [0x00,0x00] or [0x00,0xff].
						byte[] exif = {0x45, 0x78, 0x69, 0x66, 0x00, 0x00};
						byte[] exif2 = {0x45, 0x78, 0x69, 0x66, 0x00, (byte)0xff};
						byte[] exif_buf = new byte[6];
						length = IOUtils.readUnsignedShortMM(is);						
						IOUtils.readFully(is, exif_buf);		
						// EXIF segment.
						if (Arrays.equals(exif_buf, exif)||Arrays.equals(exif_buf, exif2)) {
						    IOUtils.skipFully(is, length-8);
						} else { // Might be XMP
							byte[] tmp = new byte[length-2];
							IOUtils.readFully(is, tmp, 6, length-8);
							System.arraycopy(exif_buf, 0, tmp, 0, 6);
							IOUtils.writeShortMM(os, marker);
						   	IOUtils.writeShortMM(os, (short)length);
						   	IOUtils.write(os, tmp);		
				  		}
						marker = IOUtils.readShortMM(is);
						break;
				    default:
					    length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					   	IOUtils.writeShortMM(os, marker);
					   	IOUtils.writeShortMM(os, (short)length);
					   	IOUtils.write(os, buf);
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	public static void showICCProfile(InputStream is) throws IOException {
		byte[] icc_profile = extractICCProfile(is);
		ICCProfile.showProfile(icc_profile);
	}
	
	@SuppressWarnings("unused")
	private static short skipSOS(InputStream is) throws IOException {
		int nextByte = 0;
		short marker = 0;	
		
		while((nextByte = IOUtils.read(is)) != -1)
		{
			if(nextByte == 0xff)
			{
				nextByte = IOUtils.read(is);
						
				if (nextByte == -1) {
					throw new IOException("Premature end of SOS segment!");					
				}								
				
				if (nextByte != 0x00) // This is a marker
				{
					marker = (short)((0xff<<8)|nextByte);
					
					switch (Marker.fromShort(marker)) {										
						case RST0:  
						case RST1:
						case RST2:
						case RST3:
						case RST4:
						case RST5:
						case RST6:
						case RST7:
							System.out.println(Marker.fromShort(marker));
							continue;
						default:											
					}
					break;
				}
			}
		}
		
		if (nextByte == -1) {
			throw new IOException("Premature end of SOS segment!");
		}

		return marker;
	}
	
	public static void snoop(InputStream is) throws IOException 
	{
		// Need to wrap the input stream with a BufferedInputStream to
		// speed up reading the SOS
		is = new BufferedInputStream(is);
		// Definitions
		List<QTable> m_qTables = new ArrayList<QTable>(4);
		List<HTable> m_acTables = new ArrayList<HTable>(4);	
		List<HTable> m_dcTables = new ArrayList<HTable>(4);
		
		// Each SOFReader is associated with a single SOF segment
		// Usually there is only one SOF segment, but for hierarchical
		// JPEG, there could be more than one SOF
		List<SOFReader> readers = new ArrayList<SOFReader>();
		
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
		
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
		{
			System.out.println("Invalid JPEG image, expected SOI marker not found!");
			return;
		}
		
		System.out.println("*** JPEG snooping starts ***");
		
		System.out.println(Marker.SOI);
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished)
	    {	        
			if (Marker.fromShort(marker) == Marker.EOI)
			{
				System.out.println(Marker.EOI);
				finished = true;
			}
		   	else {// Read markers
				emarker = Marker.fromShort(marker);
				System.out.println(emarker); 
	
				switch (emarker) {
					case APP0:
					case APP1:
					case APP2:
					case APP12:						
					case APP13:						
					case APP14:
						JPEGTweaker.readAPPn(is, emarker);
						marker = IOUtils.readShortMM(is);
						break;									
				    case COM:
				    	String comment = JPEGTweaker.readCOM(is);
				    	System.out.println("=>" + comment);	
				    	marker = IOUtils.readShortMM(is);
				    	break;				   				
					case DHT:
						readDHT(is, m_acTables, m_dcTables);
						marker = IOUtils.readShortMM(is);
						break;
					case DQT:
						readDQT(is, m_qTables);
						marker = IOUtils.readShortMM(is);
						break;
					case SOF0:
					case SOF1:
					case SOF2:
					case SOF3:
					case SOF5:
					case SOF6:
					case SOF7:
					case SOF9:
					case SOF10:
					case SOF11:
					case SOF13:
					case SOF14:
					case SOF15:
						readers.add(readSOF(is, emarker));
						marker = IOUtils.readShortMM(is);
						break;
					case SOS:					
						marker = readSOS(is, readers.get(readers.size() - 1));
						break;
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone mark besides SOI, EOI, and RSTn. 
						marker = IOUtils.readShortMM(is);
						break;
				    case PADDING:	
				    	int nextByte = 0;
				    	while((nextByte = IOUtils.read(is)) == 0xff) {;}
				    	marker = (short)((0xff<<8)|nextByte);
				    	break;
				    default:
					    length = IOUtils.readUnsignedShortMM(is);
					    IOUtils.skipFully(is, length-2);
					    marker = IOUtils.readShortMM(is);					    
				}
			}
	    }
		
		is.close();
		
		System.out.println("*** JPEG snooping ends ***");
	}
	
	/**
	 * Write ICC_Profile as one or more APP2 segments
	 * <p>
	 * Due to the JPEG segment length limit, we have
	 * to split ICC_Profile data and put them into 
	 * different APP2 segments if the data can not fit
	 * into one segment.
	 * 
	 * @param os output stream to write the ICC_Profile
	 * @param data ICC_Profile data
	 * @throws IOException
	 */
	public static void writeICCProfile(OutputStream os, byte[] data) throws IOException {
		// ICC_Profile ID
		byte[] icc_profile_id = {0x49, 0x43, 0x43, 0x5f, 0x50, 0x52, 0x4f, 0x46, 0x49, 0x4c, 0x45, 0x00};
		int maxSegmentLen = 65535;
		int maxICCDataLen = 65519;
		int numOfSegment = data.length/maxICCDataLen;
		int leftOver = data.length%maxICCDataLen;
		int totalSegment = (numOfSegment == 0)? 1: ((leftOver == 0)? numOfSegment: (numOfSegment + 1));
		for(int i = 0; i < numOfSegment; i++) {
			IOUtils.writeShortMM(os, Marker.APP2.getValue());
			IOUtils.writeShortMM(os, maxSegmentLen);
			IOUtils.write(os, icc_profile_id);
			IOUtils.writeShortMM(os, totalSegment|(i+1)<<8);
			IOUtils.write(os, data, i*maxICCDataLen, maxICCDataLen);
		}
		if(leftOver != 0) {
			IOUtils.writeShortMM(os, Marker.APP2.getValue());
			IOUtils.writeShortMM(os, leftOver + 16);
			IOUtils.write(os, icc_profile_id);
			IOUtils.writeShortMM(os, totalSegment|totalSegment<<8);
			IOUtils.write(os, data, data.length - leftOver, leftOver);
		}
	}
	
	/**
	 * Write ICC_Profile as one or more APP2 segments
	 * 
	 * @param os output stream to write the ICC_Profile
	 * @param icc_profile ICC_Profile read from a file or other means
	 * @throws IOException
	 */
	public static void writeICCProfile(OutputStream os, ICC_Profile icc_profile) throws IOException {
		byte[] data = icc_profile.getData();
		writeICCProfile(os, data);
	}
	
	/**
	 * Write ICCProfile as one or more APP2 segments
	 * 
	 * @param os output stream to write the ICC_Profile
	 * @param icc_profile ICCProfile read from a file or other means
	 * @throws IOException
	 */
	public static void writeICCProfile(OutputStream os, ICCProfile icc_profile) throws IOException {
		byte[] data = icc_profile.getData();
		writeICCProfile(os, data);
	}
	
	// Prevent from instantiation
	private JPEGTweaker() {}
}