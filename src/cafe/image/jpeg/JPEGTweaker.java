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
 * JPEGTweaker.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    15Apr2015  Changed the argument type for insertIPTC() and insertIRB()
 * WY    07Apr2015  Revised insertExif()
 * WY    01Apr2015  Extract IPTC as stand-alone meta data from IRB if any
 * WY    18Mar2015  Revised readAPP13(), insertIPTC() and insertIRB() to work
 *                  with multiple APP13 segments
 * WY    18Mar2015  Removed a few unused readAPPn methods
 * WY    17Mar2015  Revised meta data insertion code to conform to EXIF and 
 *                  JPEG specifications
 * WY    10Mar2015  Added code to read and merge multiple APP13 segments
 * WY    05Mar2015  Combined insertXMP() and insertExtendedXMP() to one
 * WY    04Mar2015  Added insertExtendedXMP() to insert ExtendedXMP data
 * WY    28Feb2015  Added code to extract Google depthMap from JPEG images
 * WY    27Feb2015  Added code to read XMP extension segment
 * WY    24Feb2015  Added code to remove XMP extension segment
 * WY    18Feb2015  Replaced removeExif() with a generic removeMetadata()
 * WY    14Feb2015  Added insertXMP() to add XMP meta data
 * WY    04Feb2015  Revised insertExif() to keep existing EXIF data if needed
 * WY    29Jan2015  Revised insertIPTC() and insertIRB() to keep old data
 * WY    27Jan2015  Added insertIRBThumbnail() to insert Photoshop IRB thumbnail
 * WY    27Jan2015  Added insertIRB() to insert Photoshop IRB into APP13
 * WY    26Jan2015  Added insertIPTC() to insert IPTC with APP13
 * WY    19Jan2015  Renamed snoop() to readMetadata() and revised readAPPn()
 * WY    10Jan2015  Revised extractThumbnails() to use IRBReader and IRBThumbnail
 * WY    05Jan2015  Enhanced to show information for all SOFX and SOS segments
 * WY    07Oct2014  Revised readAPP1() to show Adobe XMP information
 * WY    02Oct2014  Renamed extractExifThumbnail() to extractThumbnails()
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cafe.image.ImageIO;
import cafe.image.ImageType;
import cafe.image.tiff.IFD;
import cafe.image.tiff.TiffTag;
import cafe.image.util.IMGUtils;
import cafe.image.writer.ImageWriter;
import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.image.meta.Thumbnail;
import cafe.image.meta.adobe.IRB;
import cafe.image.meta.adobe.IRBReader;
import cafe.image.meta.adobe.IRBThumbnail;
import cafe.image.meta.adobe.ImageResourceID;
import cafe.image.meta.adobe.ThumbnailResource;
import cafe.image.meta.adobe.XMP;
import cafe.image.meta.adobe._8BIM;
import cafe.image.meta.exif.Exif;
import cafe.image.meta.exif.ExifReader;
import cafe.image.meta.exif.ExifThumbnail;
import cafe.image.meta.exif.JpegExif;
import cafe.image.meta.icc.ICCProfile;
import cafe.image.meta.image.Comment;
import cafe.image.meta.image.ImageMetadata;
import cafe.image.meta.iptc.IPTC;
import cafe.image.meta.iptc.IPTCDataSet;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.string.Base64;
import cafe.string.StringUtils;
import cafe.string.XMLUtils;
import cafe.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * JPEG image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/25/2013
 */
public class JPEGTweaker {
	// Constants
	public static final byte[] XMP_ID = { // "http://ns.adobe.com/xap/1.0/\0"
		0x68, 0x74, 0x74, 0x70, 0x3A, 0x2F, 0x2F, 0x6E, 0x73, 0x2E, 0x61, 0x64,
		0x6F, 0x62, 0x65, 0x2E, 0x63, 0x6F, 0x6D, 0x2F, 0x78, 0x61, 0x70, 0x2F,
		0x31, 0x2E, 0x30, 0x2F, 0x00
	};
	public static final byte[] XMP_EXT_ID = { // "http://ns.adobe.com/xmp/extension/\0"
		0x68, 0x74, 0x74, 0x70, 0x3A, 0x2F, 0x2F, 0x6E, 0x73, 0x2E, 0x61, 0x64,
		0x6F, 0x62, 0x65, 0x2E, 0x63, 0x6F, 0x6D, 0x2F, 0x78, 0x6D,	0x70, 0x2F,
		0x65, 0x78, 0x74, 0x65, 0x6E, 0x73, 0x69, 0x6F, 0x6E, 0x2F, 0x00
	};
	// Photoshop IRB identification with trailing byte [0x00].
	public static final byte[] PHOTOSHOP_IRB_ID = { // "Photoshop 3.0\0"
		0x50, 0x68, 0x6F, 0x74, 0x6F, 0x73, 0x68, 0x6F, 0x70, 0x20, 0x33, 0x2E, 0x30, 0x00
	};
	// EXIF identifier with trailing bytes [0x00, 0x00].
	public static final byte[] EXIF_ID = {0x45, 0x78, 0x69, 0x66, 0x00, 0x00};
	// ICC_PROFILE identifier with trailing byte [0x00].
	public static final byte[] ICC_PROFILE_ID = {0x49, 0x43, 0x43, 0x5f, 0x50, 0x52, 0x4f, 0x46, 0x49, 0x4c, 0x45, 0x00};
	public static final byte[] JFIF_ID = {0x4A, 0x46, 0x49, 0x46, 0x00}; // JFIF
	public static final byte[] JFXX_ID = {0x4A, 0x46, 0x58, 0x58, 0x00}; // JFXX
	public static final byte[] DUCKY_ID = {0x44, 0x75, 0x63, 0x6B, 0x79}; // "Ducky" no trailing NULL
	public static final byte[] PICTURE_INFO_ID = //	"[picture info]" no trailing NULL
		{0x5B, 0x70, 0x69, 0x63, 0x74, 0x75, 0x72, 0x65, 0x20, 0x69, 0x6E, 0x66, 0x6F, 0x5D};
	public static final byte[] ADOBE_ID = {0x41, 0x64, 0x6f, 0x62, 0x65}; //"Adobe" no trailing NULL
	// Largest size for each extended XMP chunk
	public static final int MAX_EXTENDED_XMP_CHUNK_SIZE = 65458;
	public static final int MAX_XMP_CHUNK_SIZE = 65504;
	public static final int GUID_LEN = 32;
	
	public static final EnumSet<Marker> APPnMarkers = EnumSet.range(Marker.APP0, Marker.APP15);
	
	/** Copy a single SOS segment */	
	@SuppressWarnings("unused")
	private static short copySOS(InputStream is, OutputStream os) throws IOException {
		// Need special treatment.
		int nextByte = 0;
		short marker = 0;	
		
		while((nextByte = IOUtils.read(is)) != -1) {
			if(nextByte == 0xff) {
				nextByte = IOUtils.read(is);
				
				if (nextByte == -1) {
					throw new IOException("Premature end of SOS segment!");					
				}								
				
				if (nextByte != 0x00) { // This is a marker
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
							continue;
						default:											
					}
					break;
				}
				IOUtils.write(os, 0xff);
				IOUtils.write(os, nextByte);
			} else {
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
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		// Flag when we are done
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.EOI)	{
				finished = true;
			} else { // Read markers
				emarker = Marker.fromShort(marker);
	
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
				    	readAPP2(is, bo);
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
	
	// Extract depth map from google phones
	public static void extractDepthMap(InputStream is, String pathToDepthMap) throws IOException {
		Map<MetadataType, Metadata> meta = readMetadata(is);
		XMP xmp = (XMP)meta.get(MetadataType.XMP);
		if(xmp != null && xmp.hasExtendedXmp()) {
			Document xmpDocument = xmp.getMergedDocument();
			String depthMapMime = XMLUtils.findAttribute(xmpDocument, "rdf:Description", "GDepth:Mime");
			if(!StringUtils.isNullOrEmpty(depthMapMime)) {
				String data = XMLUtils.findAttribute(xmpDocument, "rdf:Description", "GDepth:Data");
				if(!StringUtils.isNullOrEmpty(data)) {
					String outpath = "";
					if(pathToDepthMap.endsWith("\\") || pathToDepthMap.endsWith("/"))
						outpath = pathToDepthMap + "google_depthmap";
					else
						outpath = pathToDepthMap.replaceFirst("[.][^.]+$", "") + "_depthmap";
					if(depthMapMime.equalsIgnoreCase("image/png")) {
						outpath += ".png";
					} else if(depthMapMime.equalsIgnoreCase("image/jpeg")) {
						outpath += ".jpg";
					}
					try {
						byte[] image = Base64.decodeToByteArray(data);							
						FileOutputStream fout = new FileOutputStream(new File(outpath));
						fout.write(image);
						fout.close();
					} catch (Exception e) {							
						e.printStackTrace();
					}
				}		
			}
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
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.EOI)	{
				finished = true;
			} else { // Read markers
				emarker = Marker.fromShort(marker);
		
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
				    	length = IOUtils.readUnsignedShortMM(is);
						byte[] jfif_buf = new byte[length - 2];
					    IOUtils.readFully(is, jfif_buf);
					    // EXIF segment
					    if(Arrays.equals(ArrayUtils.subArray(jfif_buf, 0, JFIF_ID.length), JFIF_ID) || Arrays.equals(ArrayUtils.subArray(jfif_buf, 0, JFXX_ID.length), JFXX_ID)) {
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
				    	// EXIF identifier with trailing bytes [0x00,0x00].
						byte[] exif_buf = new byte[EXIF_ID.length];
						length = IOUtils.readUnsignedShortMM(is);						
						IOUtils.readFully(is, exif_buf);						
						// EXIF segment.
						if (Arrays.equals(exif_buf, EXIF_ID)) {
							exif_buf = new byte[length - 8];
						    IOUtils.readFully(is, exif_buf);
						    Exif exif = new JpegExif(exif_buf);
						    if(exif.containsThumbnail()) {
						    	String outpath = "";
								if(pathToThumbnail.endsWith("\\") || pathToThumbnail.endsWith("/"))
									outpath = pathToThumbnail + "exif_thumbnail";
								else
									outpath = pathToThumbnail.replaceFirst("[.][^.]+$", "") + "_exif_t";
						    	Thumbnail thumbnail = exif.getThumbnail();
						    	OutputStream fout = null;
						    	if(thumbnail.getDataType() == Thumbnail.DATA_TYPE_KJpegRGB) {// JPEG format, save as JPEG
						    		 fout = new FileOutputStream(outpath + ".jpg");						    	
						    	} else { // Uncompressed, save as TIFF
						    		fout = new FileOutputStream(outpath + ".tif");
						    	}
						    	fout.write(thumbnail.getCompressedImage());
					    		fout.close();
						    }						  			
						} else {
							IOUtils.skipFully(is, length - 8);
						}
						marker = IOUtils.readShortMM(is);
						break;
				    case APP13:
				    	length = IOUtils.readUnsignedShortMM(is);
						byte[] data = new byte[length - 2];
						IOUtils.readFully(is, data, 0, length - 2);						
						int i = 0;
						
						while(data[i] != 0) i++;
						
						if(new String(data, 0, i++).equals("Photoshop 3.0")) {
							IRB irb = new IRB(ArrayUtils.subArray(data, i, data.length - i));
							if(irb.containsThumbnail()) {
								Thumbnail thumbnail = irb.getThumbnail();
								// Create output path
								String outpath = "";
								if(pathToThumbnail.endsWith("\\") || pathToThumbnail.endsWith("/"))
									outpath = pathToThumbnail + "photoshop_thumbnail.jpg";
								else
									outpath = pathToThumbnail.replaceFirst("[.][^.]+$", "") + "_photoshop_t.jpg";
								FileOutputStream fout = new FileOutputStream(outpath);
								if(thumbnail.getDataType() == IRBThumbnail.DATA_TYPE_KJpegRGB) {
									fout.write(thumbnail.getCompressedImage());
								} else {
									ImageWriter writer = ImageIO.getWriter(ImageType.JPG);
									try {
										writer.write(thumbnail.getRawImage(), fout);
									} catch (Exception e) {
										throw new IOException("Writing thumbnail failed!");
									}
								}
								fout.close();								
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
	 * @param update True to keep the original data, otherwise false
	 * @throws Exception 
	 */
	public static void insertExif(InputStream is, OutputStream os, Exif exif, boolean update) throws IOException {
		// We need thumbnail image but don't have one, create one from the current image input stream
		if(exif.isThumbnailRequired() && !exif.containsThumbnail()) {
			is = new FileCacheRandomAccessInputStream(is);
			// Insert thumbnail into EXIF wrapper
			exif.setThumbnailImage(IMGUtils.createThumbnail(is));
		}
		Exif oldExif = null;
		int oldExifIndex = -1;
		// Copy the original image and insert EXIF data
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)	{
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");
		}
		
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		// Create a list to hold the temporary Segments 
		List<Segment> segments = new ArrayList<Segment>();
		
		while (!finished) { // Read through and add the segments to a list until SOS 
			if (Marker.fromShort(marker) == Marker.SOS) {
				// Write the items in segments list excluding the old EXIF
				for(int i = 0; i < oldExifIndex; i++) {
					segments.get(i).write(os);
				}
				// Now we insert the EXIF data
		    	IFD newExifSubIFD = exif.getExifIFD();
		    	IFD newGpsSubIFD = exif.getGPSIFD();
		    	IFD newImageIFD = exif.getImageIFD();
		    	ExifThumbnail newThumbnail = exif.getThumbnail();
		    	// Got to do something to keep the old data
		    	if(update && oldExif != null) {
		    		IFD imageIFD = oldExif.getImageIFD();
			    	IFD exifSubIFD = oldExif.getExifIFD();
			    	IFD gpsSubIFD = oldExif.getGPSIFD();
			    	ExifThumbnail thumbnail = oldExif.getThumbnail();
			    	
			    	if(imageIFD != null) {
			    		if(newImageIFD != null)
			    			imageIFD.addFields(newImageIFD.getFields());
			    		newImageIFD = imageIFD;
	    			}
			    	if(thumbnail != null) {
			    		if(newThumbnail == null)
			    			newThumbnail = thumbnail;
					}
			    	if(exifSubIFD != null) {
			    		if(newExifSubIFD != null)
			    			exifSubIFD.addFields(newExifSubIFD.getFields());
			    		newExifSubIFD = exifSubIFD;
	    			}
			    	if(gpsSubIFD != null) {
			    		if(newGpsSubIFD != null)
			    			gpsSubIFD.addFields(newGpsSubIFD.getFields());
			    		newGpsSubIFD = gpsSubIFD;
	    			}
		    	} 
		    	// If we have ImageIFD, set Image IFD attached with EXIF and GPS
		     	if(newImageIFD != null) {
		    		if(newExifSubIFD != null)
			    		newImageIFD.addChild(TiffTag.EXIF_SUB_IFD, newExifSubIFD);
		    		if(newGpsSubIFD != null)
			    		newImageIFD.addChild(TiffTag.GPS_SUB_IFD, newGpsSubIFD);
		    		exif.setImageIFD(newImageIFD);
		    	} else { // Otherwise, set EXIF and GPS IFD separately
		    		exif.setExifIFD(newExifSubIFD);
		    		exif.setGPSIFD(newGpsSubIFD);
		    	}
		   		exif.setThumbnail(newThumbnail);
		   		// Now insert the new EXIF to the JPEG
		   		exif.write(os);		     	
		     	// Copy the remaining segments
				for(int i = oldExifIndex + 1; i < segments.size(); i++) {
					segments.get(i).write(os);
				}	    	
				// Copy the leftover stuff
				IOUtils.writeShortMM(os, marker);
				copyToEnd(is, os);
				// We are done
				finished = true;
			} else { // Read markers
				emarker = Marker.fromShort(marker);
		
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone marker besides SOI, EOI, and RSTn.
				    	segments.add(new Segment(emarker, 0, null));
						marker = IOUtils.readShortMM(is);
						break;
				    case APP1:
				    	// Read and remove the old EXIF data
				    	length = IOUtils.readUnsignedShortMM(is);
				    	byte[] exifBytes = new byte[length - 2];
						IOUtils.readFully(is, exifBytes);
						// Add data to segment list
						segments.add(new Segment(emarker, length, exifBytes));
						// Read the EXIF data.
						if(Arrays.equals(ArrayUtils.subArray(exifBytes, 0, EXIF_ID.length), EXIF_ID)) { // We assume EXIF data exist only in one APP1
							oldExif = new JpegExif(ArrayUtils.subArray(exifBytes, EXIF_ID.length, length - EXIF_ID.length - 2));
							oldExifIndex = segments.size() - 1;
						}											
						marker = IOUtils.readShortMM(is);
						break;
				    default:
					    length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					    segments.add(new Segment(emarker, length, buf));
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
		// Close the input stream in case it's an instance of RandomAccessInputStream
		if(is instanceof RandomAccessInputStream)
			is.close();
	}
	
	/**
	 * Insert ICC_Profile as one or more APP2 segments
	 * 
	 * @param is input stream for the original image
	 * @param os output stream to write the ICC_Profile
	 * @param data ICC_Profile data array to be inserted
	 * @throws IOException
	 */	
	public static void insertICCProfile(InputStream is, OutputStream os, byte[] data) throws IOException {
		// Copy the original image and insert ICC_Profile data
		byte[] icc_profile_id = {0x49, 0x43, 0x43, 0x5f, 0x50, 0x52, 0x4f, 0x46, 0x49, 0x4c, 0x45, 0x00};
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
		int app0Index = -1;
		int app1Index = -1;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");
	
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		// Create a list to hold the temporary Segments 
		List<Segment> segments = new ArrayList<Segment>();
		
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.SOS) {
				int index = Math.max(app0Index, app1Index);
				// Write the items in segments list excluding the APP13
				for(int i = 0; i <= index; i++)
					segments.get(i).write(os);	
				writeICCProfile(os, data);
		    	// Copy the remaining segments
				for(int i = (index < 0 ? 0 : index + 1); i < segments.size(); i++) {
					segments.get(i).write(os);
				}
				// Copy the rest of the data
				IOUtils.writeShortMM(os, marker);
				copyToEnd(is, os);
				// No more marker to read, we are done.
				finished = true;  
			}  else { // Read markers
				emarker = Marker.fromShort(marker);
			
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
					case TEM: // The only stand alone marker besides SOI, EOI, and RSTn.
						segments.add(new Segment(emarker, 0, null));
						marker = IOUtils.readShortMM(is);
						break;
				    case APP2: // Remove old ICC_Profile
				    	byte[] icc_profile_buf = new byte[12];
						length = IOUtils.readUnsignedShortMM(is);						
						if(length < 14) { // This is not an ICC_Profile segment, copy it
							icc_profile_buf = new byte[length - 2];
							IOUtils.readFully(is, icc_profile_buf);
							segments.add(new Segment(emarker, length, icc_profile_buf));
						} else {
							IOUtils.readFully(is, icc_profile_buf);		
							// ICC_PROFILE segment.
							if (Arrays.equals(icc_profile_buf, icc_profile_id)) {
								IOUtils.skipFully(is, length - 14);
							} else {// Not an ICC_Profile segment, copy it
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short)length);
								IOUtils.write(os, icc_profile_buf);
								byte[] temp = new byte[length - ICC_PROFILE_ID.length - 2];
								IOUtils.readFully(is, temp);
								segments.add(new Segment(emarker, length, ArrayUtils.concat(icc_profile_buf, temp)));
							}
						}						
						marker = IOUtils.readShortMM(is);
						break;
				    case APP0:
				    	app0Index = segments.size();
				    case APP1:
				    	app1Index = segments.size();
				    default:
				    	 length = IOUtils.readUnsignedShortMM(is);					
				    	 byte[] buf = new byte[length - 2];
				    	 IOUtils.readFully(is, buf);
				    	 segments.add(new Segment(emarker, length, buf));
				    	 marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	public static void insertICCProfile(InputStream is, OutputStream os, ICC_Profile icc_profile) throws IOException {
		insertICCProfile(is, os, icc_profile.getData());
	}
	
	public static void insertICCProfile(InputStream is, OutputStream os, ICCProfile icc_profile) throws Exception {
		insertICCProfile(is, os, icc_profile.getData());
	}
	
	/**
	 * Inserts a list of IPTCDataSet into a JPEG APP13 Photoshop IRB segment
	 * 
	 * @param is InputStream for the original image
	 * @param os OutputStream for the image with IPTC APP13 inserted
	 * @param iptcs a list of IPTCDataSet to be inserted
	 * @param update if true, keep the original data, otherwise, replace the complete APP13 data 
	 * @throws IOException
	 */
	public static void insertIPTC(InputStream is, OutputStream os, Collection<IPTCDataSet> iptcs, boolean update) throws IOException {
		// Copy the original image and insert Photoshop IRB data
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
		int app0Index = -1;
		int app1Index = -1;		
		
		Map<Short, _8BIM> bimMap = null;
		// Used to read multiple segment Adobe APP13
		ByteArrayOutputStream eightBIMStream = null;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)	{
			is.close();
			os.close();		
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");			
		}
		
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		// Create a list to hold the temporary Segments 
		List<Segment> segments = new ArrayList<Segment>();
		
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.SOS) {
				if(eightBIMStream != null) {
					IRB irb = new IRB(eightBIMStream.toByteArray());
		    		// Shallow copy the map.
		    		bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());
					_8BIM iptcBIM = bimMap.remove(ImageResourceID.IPTC_NAA.getValue());
					if(iptcBIM != null) { // Keep the original values
						IPTC iptc = new IPTC(iptcBIM.getData());
						// Shallow copy the map
						Map<String, List<IPTCDataSet>> dataSetMap = new HashMap<String, List<IPTCDataSet>>(iptc.getDataSets());
						for(IPTCDataSet set : iptcs)
							if(!set.allowMultiple())
								dataSetMap.remove(set.getName());
						for(List<IPTCDataSet> iptcList : dataSetMap.values())
							iptcs.addAll(iptcList);
					}
			  	}				
				int index = Math.max(app0Index, app1Index);
				// Write the items in segments list excluding the APP13
				for(int i = 0; i <= index; i++)
					segments.get(i).write(os);	
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				// Insert IPTC data as one of IRB 8BIM block
				for(IPTCDataSet iptc : iptcs)
					iptc.write(bout);
				// Create 8BIM for IPTC
				_8BIM newBIM = new _8BIM(ImageResourceID.IPTC_NAA.getValue(), "iptc", bout.toByteArray());
				if(bimMap != null) {
					bimMap.put(newBIM.getID(), newBIM); // Add the IPTC_NAA 8BIM to the map
					writeIRB(os, bimMap.values()); // Write the whole thing as one APP13
				} else {
					writeIRB(os, newBIM); // Write the one and only one 8BIM as one APP13
				}						
				// Copy the remaining segments
				for(int i = (index < 0 ? 0 : index + 1); i < segments.size(); i++) {
					segments.get(i).write(os);
				}
				// Copy the rest of the data
				IOUtils.writeShortMM(os, marker);
				copyToEnd(is, os);
				// No more marker to read, we are done.
				finished = true;  
			} else {// Read markers
		   		emarker = Marker.fromShort(marker);
		
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
					case TEM: // The only stand alone marker besides SOI, EOI, and RSTn.
						segments.add(new Segment(emarker, 0, null));
						marker = IOUtils.readShortMM(is);
						break;
					case APP13:
				    	if(update) {
				    		if(eightBIMStream == null)
				    			eightBIMStream = new ByteArrayOutputStream();
				    		readAPP13(is, eightBIMStream);
				    	} else {
				    		length = IOUtils.readUnsignedShortMM(is);					
						    IOUtils.skipFully(is, length - 2);
				    	}
				    	marker = IOUtils.readShortMM(is);
				    	break;
					case APP0:
						app0Index = segments.size();
					case APP1:
						app1Index = segments.size();
				    default:
				    	length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					    segments.add(new Segment(emarker, length, buf));
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	public static void insertIRB(InputStream is, OutputStream os, Collection<_8BIM> bims, boolean update) throws IOException {
		// Copy the original image and insert Photoshop IRB data
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
		int app0Index = -1;
		int app1Index = -1;		
		// Used to read multiple segment Adobe APP13
		ByteArrayOutputStream eightBIMStream = null;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)	{
			is.close();
			os.close();
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");
		}
		
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		// Create a list to hold the temporary Segments 
		List<Segment> segments = new ArrayList<Segment>();
		
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.SOS) {
				if(eightBIMStream != null) {
					IRB irb = new IRB(eightBIMStream.toByteArray());
			    	// Shallow copy the map.
		    		Map<Short, _8BIM> bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());
					for(_8BIM bim : bims) // Replace the original data
						bimMap.put(bim.getID(), bim);
					// In case we have two ThumbnailResource IRB, remove the Photoshop4.0 one
					if(bimMap.containsKey(ImageResourceID.THUMBNAIL_RESOURCE_PS4.getValue()) 
							&& bimMap.containsKey(ImageResourceID.THUMBNAIL_RESOURCE_PS5.getValue()))
						bimMap.remove(ImageResourceID.THUMBNAIL_RESOURCE_PS4.getValue());
					bims = bimMap.values();					
		    	}
				int index = Math.max(app0Index, app1Index);
				// Write the items in segments list excluding the APP13
				for(int i = 0; i <= index; i++)
					segments.get(i).write(os);	
				writeIRB(os, bims);
				// Copy the remaining segments
				for(int i = (index < 0 ? 0 : index + 1); i < segments.size(); i++) {
					segments.get(i).write(os);
				}
				// Copy the rest of the data
				IOUtils.writeShortMM(os, marker);
				copyToEnd(is, os);
				// No more marker to read, we are done.
				finished = true;  
			} else {// Read markers
		   		emarker = Marker.fromShort(marker);
		
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
					case TEM: // The only stand alone marker besides SOI, EOI, and RSTn.
						segments.add(new Segment(emarker, 0, null));
						marker = IOUtils.readShortMM(is);
						break;
				    case APP13: // We will keep the other IRBs from the original APP13
				    	if(update) {
				    		if(eightBIMStream == null)
				    			eightBIMStream = new ByteArrayOutputStream();
					    	readAPP13(is, eightBIMStream);					    	
				    	} else {
				    		length = IOUtils.readUnsignedShortMM(is);					
						    IOUtils.skipFully(is, length - 2);
				    	}
				    	marker = IOUtils.readShortMM(is);
				    	break;
				    case APP0:
				    	app0Index = segments.size();
				    case APP1:
				    	app1Index = segments.size();
				    default:
				    	length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					    segments.add(new Segment(emarker, length, buf));
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	public static void insertIRBThumbnail(InputStream is, OutputStream os, BufferedImage thumbnail) throws IOException {
		// Sanity check
		if(thumbnail == null) throw new IllegalArgumentException("Input thumbnail is null");
		_8BIM bim = new ThumbnailResource(thumbnail);
		insertIRB(is, os, Arrays.asList(bim), true); // Set true to keep other IRB blocks
	}
	
	/*
	 * Insert XMP into single APP1 or multiple segments. Support ExtendedXMP.
	 * 
	 * The standard part of the XMP must be a valid XMP with packet wrapper and,
	 * should already include the GUID for the ExtendedXMP in case of ExtendedXMP.
	 */	
	private static void insertXMP(InputStream is, OutputStream os, byte[] xmp, byte[] extendedXmp, String guid) throws IOException {
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
		int app0Index = -1;
		int exifIndex = -1;
		
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)	{
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");
		}
		
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		// Create a list to hold the temporary Segments 
		List<Segment> segments = new ArrayList<Segment>();
		
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.SOS)	{
				int index = Math.max(app0Index, exifIndex);
				// Write the items in segments list excluding the old XMP
				for(int i = 0; i <= index; i++)
					segments.get(i).write(os);				
				// Now we insert the XMP data
				writeXMP(os, xmp, extendedXmp, guid);
				// Copy the remaining segments
				for(int i = (index < 0 ? 0 : index + 1); i < segments.size(); i++) {
					segments.get(i).write(os);
				}	
				// Copy the leftover stuff
				IOUtils.writeShortMM(os, marker);
				copyToEnd(is, os); // Copy the rest of the data
				finished = true; // No more marker to read, we are done.				
			} else { // Read markers
				emarker = Marker.fromShort(marker);
				
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone marker besides SOI, EOI, and RSTn.
				    	segments.add(new Segment(emarker, 0, null));
						marker = IOUtils.readShortMM(is);
						break;
				    case APP1:
				    	// Read and remove the old XMP data
				    	length = IOUtils.readUnsignedShortMM(is);
						byte[] xmpExtId = new byte[XMP_EXT_ID.length];
						IOUtils.readFully(is, xmpExtId);
						// Remove XMP and ExtendedXMP segments.
						if(Arrays.equals(xmpExtId, XMP_EXT_ID)) {
							IOUtils.skipFully(is, length - XMP_EXT_ID.length - 2);
						} else if(Arrays.equals(ArrayUtils.subArray(xmpExtId, 0, XMP_ID.length), XMP_ID)) {
							IOUtils.skipFully(is,  length - XMP_EXT_ID.length - 2);
						} else { // We are going to keep other types of data							
							byte[] temp = new byte[length - XMP_EXT_ID.length - 2];
							IOUtils.readFully(is, temp);
							segments.add(new Segment(emarker, length, ArrayUtils.concat(xmpExtId, temp)));
							// If it's EXIF, we keep the index
							if(Arrays.equals(ArrayUtils.subArray(xmpExtId, 0, EXIF_ID.length), EXIF_ID)) {
								exifIndex = segments.size() - 1;
							}
						}
						marker = IOUtils.readShortMM(is);
						break;
				    case APP0:
				    	app0Index = segments.size();
				    default:
					    length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					    segments.add(new Segment(emarker, length, buf));
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	/**
	 * Insert a XMP string into the image. When converted to bytes, 
	 * the XMP part should be able to fit into one APP1.
	 * 
	 * @param is InputStream for the image.
	 * @param os OutputStream for the image.
	 * @param xmp XML string for the XMP - Assuming in UTF-8 format.
	 * @throws IOException
	 */
	public static void insertXMP(InputStream is, OutputStream os, String xmp, String extendedXmp) throws IOException {
		// Add packet wrapper to the XMP document
		// Add PI at the beginning and end of the document, we will support only UTF-8, no BOM
		Document xmpDoc = XMLUtils.createXML(xmp);
		XMLUtils.insertLeadingPI(xmpDoc, "xpacket", "begin='' id='W5M0MpCehiHzreSzNTczkc9d'");
		XMLUtils.insertTrailingPI(xmpDoc, "xpacket", "end='w'");
		Document extendedDoc = null;
		byte[] extendedXmpBytes = null;
		String guid = null;
		if(extendedXmp != null) { // We have ExtendedXMP
			extendedDoc = XMLUtils.createXML(extendedXmp);
			extendedXmpBytes = XMLUtils.serializeToByteArray(extendedDoc);
			guid = StringUtils.generateMD5(extendedXmpBytes);
			NodeList descriptions = xmpDoc.getElementsByTagName("rdf:Description");
			int length = descriptions.getLength();
			if(length > 0) {
				Element node = (Element)descriptions.item(length - 1);
				node.setAttribute("xmlns:xmpNote", "http://ns.adobe.com/xmp/extension/");
				node.setAttribute("xmpNote:HasExtendedXMP", guid);
			}
		}
		// Serialize XMP to byte array
		byte[] xmpBytes = XMLUtils.serializeToByteArray(xmpDoc);
		if(xmpBytes.length > MAX_XMP_CHUNK_SIZE)
			throw new RuntimeException("XMP data size exceededs JPEG segment size");
		// Insert XMP and ExtendedXMP into image
		insertXMP(is, os, xmpBytes, extendedXmpBytes, guid);
	}
	
	public static void printHTables(List<HTable> tables) {
		final String[] HT_class_table = {"DC Component", "AC Component"};
		System.out.println("Huffman table information =>:");
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
			
			System.out.println("<<End of Huffman table information>>");
		}
	}
	
	public static void printQTables(List<QTable> qTables) {
		System.out.println("Quantization table information =>:");
		
		int count = 0;
		
		for(QTable table : qTables) {
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
		System.out.println("End of quantization table information");
	}
	
	public static void printSOF(SOFReader reader) {
		System.out.println("SOF informtion =>");
		System.out.println("Precision: " + reader.getPrecision());
		System.out.println("Image height: " + reader.getFrameHeight());
		System.out.println("Image width: " + reader.getFrameWidth());
		System.out.println("# of Components: " + reader.getNumOfComponents());
		System.out.println(" (1 = grey scaled, 3 = color YCbCr or YIQ, 4 = color CMYK)");		
		    
		for(Component component : reader.getComponents()) {
			System.out.println();
			System.out.println("Component ID: " + component.getId());
			System.out.println("Herizontal sampling factor: " + component.getHSampleFactor());
			System.out.println("Vertical sampling factor: " + component.getVSampleFactor());
			System.out.println("Quantization table #: " + component.getQTableNumber());
			System.out.println("DC table number: " + component.getDCTableNumber());
			System.out.println("AC table number: " + component.getACTableNumber());
		}
		
		System.out.println("End of SOF information");
	}
	
	private static void readAPP13(InputStream is, OutputStream os) throws IOException {
		int length = IOUtils.readUnsignedShortMM(is);
		byte[] temp = new byte[length - 2];
		IOUtils.readFully(is, temp, 0, length - 2);
	
		if (Arrays.equals(ArrayUtils.subArray(temp, 0, PHOTOSHOP_IRB_ID.length), PHOTOSHOP_IRB_ID)) {
			os.write(ArrayUtils.subArray(temp, PHOTOSHOP_IRB_ID.length, temp.length - PHOTOSHOP_IRB_ID.length));	
		}
	}
	
	private static void readAPP2(InputStream is, OutputStream bo) throws IOException {
		byte[] icc_profile_buf = new byte[12];
		int length = IOUtils.readUnsignedShortMM(is);
		IOUtils.readFully(is, icc_profile_buf);
		// ICC_PROFILE segment.
		if (Arrays.equals(icc_profile_buf, ICC_PROFILE_ID)) {
			icc_profile_buf = new byte[length - 14];
		    IOUtils.readFully(is, icc_profile_buf);
		    bo.write(icc_profile_buf, 2, length - 16);
		} else {
  			IOUtils.skipFully(is, length - 14);
  		}
	}
	
	private static void readDHT(InputStream is, List<HTable> m_acTables, List<HTable> m_dcTables) throws IOException {	
		int len = IOUtils.readUnsignedShortMM(is);
        byte buf[] = new byte[len - 2];
        IOUtils.readFully(is, buf);
		
		DHTReader reader = new DHTReader(new Segment(Marker.DHT, len, buf));
		
		List<HTable> dcTables = reader.getDCTables();
		List<HTable> acTables = reader.getACTables();
		
		m_acTables.addAll(acTables);
		m_dcTables.addAll(dcTables);
   	}
	
	// Process define Quantization table
	private static void readDQT(InputStream is, List<QTable> m_qTables) throws IOException {
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		DQTReader reader = new DQTReader(new Segment(Marker.DQT, len, buf));
		List<QTable> qTables = reader.getTables();
		m_qTables.addAll(qTables);		
	}
	
	public static Map<MetadataType, Metadata> readMetadata(InputStream is) throws IOException {
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		Map<String, Thumbnail> thumbnails = new HashMap<String, Thumbnail>();
		// Need to wrap the input stream with a BufferedInputStream to
		// speed up reading SOS
		is = new BufferedInputStream(is);
		// Definitions
		List<QTable> m_qTables = new ArrayList<QTable>(4);
		List<HTable> m_acTables = new ArrayList<HTable>(4);	
		List<HTable> m_dcTables = new ArrayList<HTable>(4);		
		// Each SOFReader is associated with a single SOF segment
		// Usually there is only one SOF segment, but for hierarchical
		// JPEG, there could be more than one SOF
		List<SOFReader> readers = new ArrayList<SOFReader>();
		// Used to read multiple segment ICCProfile
		ByteArrayOutputStream iccProfileStream = null;
		// Used to read multiple segment Adobe APP13
		ByteArrayOutputStream eightBIMStream = null;
		// Used to read multiple segment XMP
		byte[] extendedXMP = null;
		String xmpGUID = ""; // 32 byte ASCII hex string
		
		List<Segment> appnSegments = new ArrayList<Segment>();
	
		boolean finished = false;
		int length = 0;
		short marker;
		Marker emarker;
		
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
			throw new IllegalArgumentException("Invalid JPEG image, expected SOI marker not found!");
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.EOI)	{
				finished = true;
			} else {// Read markers
				emarker = Marker.fromShort(marker);
	
				switch (emarker) {
					case APP0:
					case APP1:
					case APP2:
					case APP3:
					case APP4:
					case APP5:
					case APP6:
					case APP7:
					case APP8:
					case APP9:
					case APP10:
					case APP11:
					case APP12:
					case APP13:
					case APP14:
					case APP15:
						byte[] appBytes = readSegmentData(is);
						appnSegments.add(new Segment(emarker, appBytes.length + 2, appBytes));
						marker = IOUtils.readShortMM(is);
						break;
					case COM:
						metadataMap.put(MetadataType.COMMENT, new Comment(readSegmentData(is)));
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
					    IOUtils.skipFully(is, length - 2);
					    marker = IOUtils.readShortMM(is);			    
				}
			}
	    }
		
		is.close();
		
		for(Segment segment : appnSegments) {
			byte[] data = segment.getData();
			length = segment.getLength();
			if(segment.getMarker() == Marker.APP1) {
				// Check for EXIF
				if(Arrays.equals(ArrayUtils.subArray(data, 0, EXIF_ID.length), EXIF_ID)) {
					// We found EXIF
					JpegExif exif = new JpegExif(ArrayUtils.subArray(data, EXIF_ID.length, length - EXIF_ID.length - 2));
					metadataMap.put(MetadataType.EXIF, exif);
				} else if(Arrays.equals(ArrayUtils.subArray(data, 0, XMP_ID.length), XMP_ID)) {
					// We found XMP, add it to metadata list (We may later revise it if we have ExtendedXMP)
					XMP xmp = new XMP(ArrayUtils.subArray(data, XMP_ID.length, length - XMP_ID.length - 2));
					metadataMap.put(MetadataType.XMP, xmp);
					// Retrieve and remove XMP GUID if available
					xmpGUID = XMLUtils.findAttribute(xmp.getXmpDocument(), "rdf:Description", "xmpNote:HasExtendedXMP");
				} else if(Arrays.equals(ArrayUtils.subArray(data, 0, XMP_EXT_ID.length), XMP_EXT_ID)) {
					// We found ExtendedXMP, add the data to ExtendedXMP memory buffer				
					int i = XMP_EXT_ID.length;
					// 128-bit MD5 digest of the full ExtendedXMP serialization
					byte[] guid = ArrayUtils.subArray(data, i, 32);
					if(Arrays.equals(guid, xmpGUID.getBytes())) { // We have matched the GUID, copy it
						i += 32;
						long extendedXMPLength = IOUtils.readUnsignedIntMM(data, i);
						i += 4;
						if(extendedXMP == null)
							extendedXMP = new byte[(int)extendedXMPLength];
						// Offset for the current segment
						long offset = IOUtils.readUnsignedIntMM(data, i);
						i += 4;
						byte[] xmpBytes = ArrayUtils.subArray(data, i, length - XMP_EXT_ID.length - 42);
						System.arraycopy(xmpBytes, 0, extendedXMP, (int)offset, xmpBytes.length);
					}
				}
			} else if(segment.getMarker() == Marker.APP2) {
				// We're only interested in ICC_Profile
				if (Arrays.equals(ArrayUtils.subArray(data, 0, ICC_PROFILE_ID.length), ICC_PROFILE_ID)) {
					if(iccProfileStream == null)
						iccProfileStream = new ByteArrayOutputStream();
					iccProfileStream.write(ArrayUtils.subArray(data, ICC_PROFILE_ID.length + 2, length - ICC_PROFILE_ID.length - 4));
				}
			} else if(segment.getMarker() == Marker.APP13) {
				if (Arrays.equals(ArrayUtils.subArray(data, 0, PHOTOSHOP_IRB_ID.length), PHOTOSHOP_IRB_ID)) {
					if(eightBIMStream == null)
						eightBIMStream = new ByteArrayOutputStream();
					eightBIMStream.write(ArrayUtils.subArray(data, PHOTOSHOP_IRB_ID.length, length - PHOTOSHOP_IRB_ID.length - 2));
				}
			}
		}
		
		// Now it's time to join multiple segments ICC_PROFILE and/or XMP		
		if(iccProfileStream != null) { // We have ICCProfile data
			ICCProfile icc_profile = new ICCProfile(iccProfileStream.toByteArray());
			icc_profile.showMetadata();
			metadataMap.put(MetadataType.ICC_PROFILE, icc_profile);
		}
		
		if(eightBIMStream != null) {
			IRB irb = new IRB(eightBIMStream.toByteArray());	
			metadataMap.put(MetadataType.PHOTOSHOP, irb);
			_8BIM iptc = irb.get8BIM(ImageResourceID.IPTC_NAA.getValue());
			// Extract IPTC as stand-alone meta
			if(iptc != null) {
				metadataMap.put(MetadataType.IPTC, new IPTC(iptc.getData()));
			}
		}
		
		if(extendedXMP != null) {
			XMP xmp = ((XMP)metadataMap.get(MetadataType.XMP));
			if(xmp != null)
				xmp.setExtendedXMPData(extendedXMP);
		}
		
		// Extract thumbnails to ImageMetadata
		Metadata meta = metadataMap.get(MetadataType.EXIF);
		if(meta != null) {
			Exif exif = (Exif)meta;
			ExifReader reader = exif.getReader();
			if(!reader.isDataLoaded())
				reader.read();
			if(reader.containsThumbnail()) {
				thumbnails.put("EXIF", reader.getThumbnail());
			}
		}
		
		meta = metadataMap.get(MetadataType.PHOTOSHOP);
		if(meta != null) {
			IRB irb = (IRB)meta;
			IRBReader reader = irb.getReader();
			if(!reader.isDataLoaded())
				reader.read();
			if(reader.containsThumbnail()) {
				thumbnails.put("PHOTOSHOP", reader.getThumbnail());
			}
		}
		
		metadataMap.put(MetadataType.IMAGE, new ImageMetadata(null, thumbnails));
		
		return metadataMap;
	}
	
	private static byte[] readSegmentData(InputStream is) throws IOException {
		int length = IOUtils.readUnsignedShortMM(is);
		byte[] data = new byte[length - 2];
		IOUtils.readFully(is, data);
		
		return data;
	}
	
	private static SOFReader readSOF(InputStream is, Marker marker) throws IOException {		
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		Segment segment = new Segment(marker, len, buf);		
		SOFReader reader = new SOFReader(segment);
		
		return reader;
	}	
	
	// This method is very slow if not wrapped in some kind of cache stream but it works for multiple
	// SOSs in case of progressive JPEG
	private static short readSOS(InputStream is, SOFReader sofReader) throws IOException {
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		Segment segment = new Segment(Marker.SOS, len, buf);
		new SOSReader(segment, sofReader);
		
		// Actual image data follow.
		int nextByte = 0;
		short marker = 0;	
		
		while((nextByte = IOUtils.read(is)) != -1) {
			if(nextByte == 0xff) {
				nextByte = IOUtils.read(is);
				
				if (nextByte == -1) {
					throw new IOException("Premature end of SOS segment!");					
				}								
				
				if (nextByte != 0x00) {
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
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");

		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.EOI)	{
				IOUtils.writeShortMM(os, Marker.EOI.getValue());
				finished = true;
			} else { // Read markers
				emarker = Marker.fromShort(marker);
		
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
					    
					    if(emarker != APPn) { // Copy the data
					    	IOUtils.writeShortMM(os, marker);
					    	IOUtils.writeShortMM(os, (short)length);
					    	IOUtils.write(os, buf);
					    }
					    
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	public static void removeMetadata(InputStream is, OutputStream os, MetadataType ... metadataTypes) throws IOException {
		removeMetadata(new HashSet<MetadataType>(Arrays.asList(metadataTypes)), is, os);
	}
	
	// Remove meta data segments
	public static void removeMetadata(Set<MetadataType> metadataTypes, InputStream is, OutputStream os) throws IOException {
		// Flag when we are done
		boolean finished = false;
		int length = 0;
		short marker;
		Marker emarker;

		// The very first marker should be the start_of_image marker!
		if (Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");
			
		IOUtils.writeShortMM(os, Marker.SOI.getValue());

		marker = IOUtils.readShortMM(is);

		while (!finished) {
			if (Marker.fromShort(marker) == Marker.EOI) {
				IOUtils.writeShortMM(os, Marker.EOI.getValue());
				finished = true;
			} else { // Read markers
				emarker = Marker.fromShort(marker);
		
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
						while ((nextByte = IOUtils.read(is)) == 0xff) {
							IOUtils.write(os, nextByte);
						}
						marker = (short) ((0xff << 8) | nextByte);
						break;
					case SOS: // There should be no meta data after this segment
						IOUtils.writeShortMM(os, marker);
						copyToEnd(is, os);
						finished = true;
						break;
					case COM:
						if(metadataTypes.contains(MetadataType.COMMENT)) {
							length = IOUtils.readUnsignedShortMM(is);
							IOUtils.skipFully(is, length - 2);
							marker = IOUtils.readShortMM(is);
							break;
						} // Otherwise go to default
					case APP1:
						// We are only interested in EXIF and XMP
						if(metadataTypes.contains(MetadataType.EXIF) || metadataTypes.contains(MetadataType.XMP)) {
							length = IOUtils.readUnsignedShortMM(is);
							byte[] temp = new byte[XMP_EXT_ID.length];
							IOUtils.readFully(is, temp);
							// XMP segment.
							if(Arrays.equals(temp, XMP_EXT_ID) && metadataTypes.contains(MetadataType.XMP)) {
								IOUtils.skipFully(is, length - XMP_EXT_ID.length  - 2);
							} else if(Arrays.equals(ArrayUtils.subArray(temp, 0, XMP_ID.length), XMP_ID) && metadataTypes.contains(MetadataType.XMP)) {
								IOUtils.skipFully(is,  length - XMP_EXT_ID.length - 2);
							} else if(Arrays.equals(ArrayUtils.subArray(temp, 0, EXIF_ID.length), EXIF_ID)
									&& metadataTypes.contains(MetadataType.EXIF)) { // EXIF
								IOUtils.skipFully(is, length - XMP_EXT_ID.length - 2);
							} else { // We don't want to remove any of them
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short) length);
								IOUtils.write(os, temp); // Write the already read bytes
								temp = new byte[length - XMP_EXT_ID.length - 2];
								IOUtils.readFully(is, temp);
								IOUtils.write(os, temp);
							}
							marker = IOUtils.readShortMM(is);
							break;
						} // Otherwise go to default
					case APP2:
						if(metadataTypes.contains(MetadataType.ICC_PROFILE)) {
							length = IOUtils.readUnsignedShortMM(is);
							byte[] temp = new byte[ICC_PROFILE_ID.length];
							IOUtils.readFully(is, temp);	
							// ICC_Profile segment
							if (Arrays.equals(temp, ICC_PROFILE_ID) && metadataTypes.contains(MetadataType.ICC_PROFILE)) {
								IOUtils.skipFully(is, length - ICC_PROFILE_ID.length  - 2);
							} else {
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short) length);
								IOUtils.write(os, temp); // Write the already read bytes
								temp = new byte[length - ICC_PROFILE_ID.length - 2];
								IOUtils.readFully(is, temp);
								IOUtils.write(os, temp);
							}
							marker = IOUtils.readShortMM(is);
							break;
						} // Otherwise go to default
					case APP13:
						if(metadataTypes.contains(MetadataType.PHOTOSHOP) || metadataTypes.contains(MetadataType.IPTC)) {
							length = IOUtils.readUnsignedShortMM(is);
							byte[] temp = new byte[PHOTOSHOP_IRB_ID.length];
							IOUtils.readFully(is, temp);	
							// PHOTOSHOP IRB segment
							if (Arrays.equals(temp, PHOTOSHOP_IRB_ID)) {
								temp = new byte[length - PHOTOSHOP_IRB_ID.length - 2];
								IOUtils.readFully(is, temp);
								IRB irb = new IRB(temp);
								// Shallow copy the map.
								Map<Short, _8BIM> bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());								
								if(metadataTypes.contains(MetadataType.PHOTOSHOP)) {
									IOUtils.skipFully(is, length - PHOTOSHOP_IRB_ID.length  - 2);
								} else {
									if(metadataTypes.contains(MetadataType.IPTC)) {
										// We only remove IPTC_NAA and keep the other IRB data untouched.
										bimMap.remove(ImageResourceID.IPTC_NAA.getValue());
									} else if(metadataTypes.contains(MetadataType.XMP)) {
										// We only remove XMP and keep the other IRB data untouched.
										bimMap.remove(ImageResourceID.XMP_METADATA.getValue());
									} else if(metadataTypes.contains(MetadataType.EXIF)) {
										// We only remove EXIF and keep the other IRB data untouched.
										bimMap.remove(ImageResourceID.EXIF_DATA1.getValue());
										bimMap.remove(ImageResourceID.EXIF_DATA3.getValue());
									}
									// Write back the IRB
									writeIRB(os, bimMap.values());
								}							
							} else {
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short) length);
								IOUtils.write(os, temp); // Write the already read bytes
								temp = new byte[length - PHOTOSHOP_IRB_ID.length - 2];
								IOUtils.readFully(is, temp);
								IOUtils.write(os, temp);
							}
							marker = IOUtils.readShortMM(is);
							break;
						} // Otherwise go to default
					default:
						length = IOUtils.readUnsignedShortMM(is);
						byte[] buf = new byte[length - 2];
						IOUtils.readFully(is, buf);
						IOUtils.writeShortMM(os, marker);
						IOUtils.writeShortMM(os, (short) length);
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
		
		while((nextByte = IOUtils.read(is)) != -1) {
			if(nextByte == 0xff) {
				nextByte = IOUtils.read(is);
						
				if (nextByte == -1) {
					throw new IOException("Premature end of SOS segment!");					
				}								
				
				if (nextByte != 0x00) { // This is a marker
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
		int maxSegmentLen = 65535;
		int maxICCDataLen = 65519;
		int numOfSegment = data.length/maxICCDataLen;
		int leftOver = data.length%maxICCDataLen;
		int totalSegment = (numOfSegment == 0)? 1: ((leftOver == 0)? numOfSegment: (numOfSegment + 1));
		for(int i = 0; i < numOfSegment; i++) {
			IOUtils.writeShortMM(os, Marker.APP2.getValue());
			IOUtils.writeShortMM(os, maxSegmentLen);
			IOUtils.write(os, ICC_PROFILE_ID);
			IOUtils.writeShortMM(os, totalSegment|(i+1)<<8);
			IOUtils.write(os, data, i*maxICCDataLen, maxICCDataLen);
		}
		if(leftOver != 0) {
			IOUtils.writeShortMM(os, Marker.APP2.getValue());
			IOUtils.writeShortMM(os, leftOver + 16);
			IOUtils.write(os, ICC_PROFILE_ID);
			IOUtils.writeShortMM(os, totalSegment|totalSegment<<8);
			IOUtils.write(os, data, data.length - leftOver, leftOver);
		}
	}
	
	private static void writeXMP(OutputStream os, byte[] xmp, byte[] extendedXmp, String guid) throws IOException {
		// Write XMP
		IOUtils.writeShortMM(os, Marker.APP1.getValue());
		// Write segment length
		IOUtils.writeShortMM(os, XMP_ID.length + 2 + xmp.length);
		// Write segment data
		os.write(XMP_ID);
		os.write(xmp);
		// Write ExtendedXMP if we have
		if(extendedXmp != null) {
			int numOfChunks = extendedXmp.length / MAX_EXTENDED_XMP_CHUNK_SIZE;
			int extendedXmpLen = extendedXmp.length;
			int offset = 0;
			
			for(int i = 0; i < numOfChunks; i++) {
				IOUtils.writeShortMM(os, Marker.APP1.getValue());
				// Write segment length
				IOUtils.writeShortMM(os, 2 + XMP_EXT_ID.length + GUID_LEN + 4 + 4 + MAX_EXTENDED_XMP_CHUNK_SIZE);
				// Write segment data
				os.write(XMP_EXT_ID);
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
				IOUtils.writeShortMM(os, 2 + XMP_EXT_ID.length + GUID_LEN + 4 + 4 + leftOver);
				// Write segment data
				os.write(XMP_EXT_ID);
				os.write(guid.getBytes());
				IOUtils.writeIntMM(os, extendedXmpLen);
				IOUtils.writeIntMM(os, offset);
				os.write(ArrayUtils.subArray(extendedXmp, offset, leftOver));
			}
		}
	}
	
	public static void writeIRB(OutputStream os, _8BIM ... bims) throws IOException {
		if(bims != null && bims.length > 0)
			writeIRB(os, Arrays.asList(bims));
	}
	
	public static void writeIRB(OutputStream os, Collection<_8BIM> bims) throws IOException {
		if(bims != null && bims.size() > 0) {
			IOUtils.writeShortMM(os, Marker.APP13.getValue());
	    	ByteArrayOutputStream bout = new ByteArrayOutputStream();
			for(_8BIM bim : bims)
				bim.write(bout);
			// Write segment length
			IOUtils.writeShortMM(os, 14 + 2 +  bout.size());
			// Write segment data
			os.write(PHOTOSHOP_IRB_ID);
			os.write(bout.toByteArray());
		}
	}
	
	// Prevent from instantiation
	private JPEGTweaker() {}
}