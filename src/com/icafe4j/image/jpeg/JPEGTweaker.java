/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
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
 * ====  =======    =====================================================================
 * WY    06Apr2018  Added extractThumbnails(InputStream)
 * WY    02Mar2017  Added insertMetadata(Collection<Metadata>, InputStream, OutputStream)
 * WY    13Feb2017  Fixed bug with APP1 segment length too small
 * WY    06Nov2016  Added support for Cardboard Camera image and audio
 * WY    03Apr2016  Rewrite insertXMP() to leverage new JpegXMP write()
 * WY    27Mar2016  Rewrite writeComment() to leverage COMBuilder
 * WY    28Sep2015  Fixed "unsupportedOperationException" with insertExif()
 * WY    26Sep2015  Added insertComments(InputStream, OutputStream, List<String>)
 * WY    06Jul2015  Added insertXMP(InputSream, OutputStream, XMP)
 * WY    02Jul2015  Added support for APP14 segment reading
 * WY    02Jul2015  Added support for APP12 segment reading
 * WY    01Jul2015  Added support for non-standard XMP identifier
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

package com.icafe4j.image.jpeg;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.ImageIO;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.Thumbnail;
import com.icafe4j.image.meta.adobe.IRB;
import com.icafe4j.image.meta.adobe.IRBThumbnail;
import com.icafe4j.image.meta.adobe.ImageResourceID;
import com.icafe4j.image.meta.adobe.ThumbnailResource;
import com.icafe4j.image.meta.adobe._8BIM;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.exif.ExifThumbnail;
import com.icafe4j.image.meta.icc.ICCProfile;
import com.icafe4j.image.meta.image.Comments;
import com.icafe4j.image.meta.image.ImageMetadata;
import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.image.meta.iptc.IPTCDataSet;
import com.icafe4j.image.meta.jpeg.Adobe;
import com.icafe4j.image.meta.jpeg.Ducky;
import com.icafe4j.image.meta.jpeg.JFIF;
import com.icafe4j.image.meta.jpeg.JpegExif;
import com.icafe4j.image.meta.jpeg.JpegXMP;
import com.icafe4j.image.meta.xmp.XMP;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.image.writer.ImageWriter;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.IOUtils;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.string.Base64;
import com.icafe4j.string.StringUtils;
import com.icafe4j.string.XMLUtils;
import com.icafe4j.util.ArrayUtils;

/**
 * JPEG image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/25/2013
 */
public class JPEGTweaker {
	// Constants
	public static final String XMP_ID = "http://ns.adobe.com/xap/1.0/\0";
	// This is a non_standard XMP identifier which sometimes found in images from GettyImages
	public static final String NON_STANDARD_XMP_ID = "XMP\0://ns.adobe.com/xap/1.0/\0";
	public static final String XMP_EXT_ID = "http://ns.adobe.com/xmp/extension/\0";
	// Photoshop IRB identifier with trailing byte [0x00].
	public static final String PHOTOSHOP_IRB_ID = "Photoshop 3.0\0";
	// EXIF identifier with trailing bytes [0x00, 0x00].
	public static final String EXIF_ID = "Exif\0\0";
	// ICC_PROFILE identifier with trailing byte [0x00].
	public static final String ICC_PROFILE_ID = "ICC_PROFILE\0";
	public static final String JFIF_ID = "JFIF\0";
	public static final String JFXX_ID = "JFXX\0";
	public static final String DUCKY_ID = "Ducky"; // no trailing NULL
	public static final String PICTURE_INFO_ID = "[picture info]"; // no trailing NULL
	public static final String ADOBE_ID = "Adobe"; // no trailing NULL
		
	@SuppressWarnings("unused")
	private static final EnumSet<Marker> APPnMarkers = EnumSet.range(Marker.APP0, Marker.APP15);
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(JPEGTweaker.class);
	
	private static short copySegment(short marker, InputStream is, OutputStream os) throws IOException {
		int length = IOUtils.readUnsignedShortMM(is);
		byte[] buf = new byte[length - 2];
		IOUtils.readFully(is, buf);
		IOUtils.writeShortMM(os, marker);
		IOUtils.writeShortMM(os, (short) length);
		IOUtils.write(os, buf);
		
		return (IOUtils.readShortMM(is));
	}
	
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
	
	// Extract depth map from google phones and cardboard camera audio & stereo pair
	public static void extractDepthMap(InputStream is, String pathToDepthMap) throws IOException {
		Map<MetadataType, Metadata> meta = readMetadata(is);
		XMP xmp = (XMP)meta.get(MetadataType.XMP);
		if(xmp != null && xmp.hasExtendedXmp()) {
			Document xmpDocument = xmp.getMergedDocument();
			String depthMapMime = XMLUtils.getAttribute(xmpDocument, "rdf:Description", "GDepth:Mime");
			String depthData = "GDepth:Data";
			String audioMime = XMLUtils.getAttribute(xmpDocument, "rdf:Description", "GAudio:Mime");
			if(StringUtils.isNullOrEmpty(depthMapMime)) {
				depthMapMime = XMLUtils.getAttribute(xmpDocument, "rdf:Description", "GImage:Mime");
				depthData = "GImage:Data";
			}					
			if(!StringUtils.isNullOrEmpty(depthMapMime)) {
				String data = XMLUtils.getAttribute(xmpDocument, "rdf:Description", depthData);
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
			if(!StringUtils.isNullOrEmpty(audioMime)) { // Cardboard Camera Audio
				String data = XMLUtils.getAttribute(xmpDocument, "rdf:Description", "GAudio:Data");
				if(!StringUtils.isNullOrEmpty(data)) {
					String outpath = "";
					if(pathToDepthMap.endsWith("\\") || pathToDepthMap.endsWith("/"))
						outpath = pathToDepthMap + "google_cardboard_audio";
					else
						outpath = pathToDepthMap.replaceFirst("[.][^.]+$", "") + "_cardboard_audio";
					if(audioMime.equalsIgnoreCase("audio/mp4a-latm")) {
						outpath += ".mp4";
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
	 * Extracts a single thumbnail image from JFIF/APP0, Exif APP1 and/or Adobe APP13 segment if any.
	 * 
	 * @param is InputStream for the JPEG image.
	 * @throws IOException
	 * @return thumbnail as a BufferedImage
	 */
	public static BufferedImage extractThumbnail(InputStream is) throws IOException {
		Collection<BufferedImage> thumbnails = extractThumbnails(is);
		if(thumbnails.size() > 0) return thumbnails.iterator().next();
		return null;
	}
	
	/**
	 * Extracts thumbnail images from JFIF/APP0, Exif APP1 and/or Adobe APP13 segment if any.
	 * 
	 * @param is InputStream for the JPEG image.
	 * @throws IOException
	 * @return A collection of BufferedImage
	 */
	public static Collection<BufferedImage> extractThumbnails(InputStream is) throws IOException {
		// Flag when we are done
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
		
		Collection<BufferedImage> thumbnails = new ArrayList<BufferedImage>();
				
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
					    if(jfif_buf.length >= JFIF_ID.length() && new String(jfif_buf, 0, JFIF_ID.length()).equals(JFIF_ID) || jfif_buf.length >= JFXX_ID.length() && new String(jfif_buf, 0, JFXX_ID.length()).equals(JFXX_ID)) {
					      	int thumbnailWidth = jfif_buf[12]&0xff;
					    	int thumbnailHeight = jfif_buf[13]&0xff;
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
								thumbnails.add(bi);
					    	}
					    }
				    	marker = IOUtils.readShortMM(is);
						break;
				    case APP1:
				    	// EXIF identifier with trailing bytes [0x00,0x00].
						byte[] exif_buf = new byte[EXIF_ID.length()];
						length = IOUtils.readUnsignedShortMM(is);						
						IOUtils.readFully(is, exif_buf);						
						// EXIF segment.
						if (Arrays.equals(exif_buf, EXIF_ID.getBytes())) {
							exif_buf = new byte[length - 8];
						    IOUtils.readFully(is, exif_buf);
						    Exif exif = new JpegExif(exif_buf);
						    if(exif.containsThumbnail()) {
						    	Thumbnail thumbnail = exif.getThumbnail();
						    	thumbnails.add(thumbnail.getAsBufferedImage());
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
								thumbnails.add(thumbnail.getAsBufferedImage());
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
		return thumbnails;
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
					    if(jfif_buf.length >= JFIF_ID.length() && new String(jfif_buf, 0, JFIF_ID.length()).equals(JFIF_ID) || jfif_buf.length >= JFXX_ID.length() && new String(jfif_buf, 0, JFXX_ID.length()).equals(JFXX_ID)) {
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
						byte[] exif_buf = new byte[EXIF_ID.length()];
						length = IOUtils.readUnsignedShortMM(is);						
						IOUtils.readFully(is, exif_buf);						
						// EXIF segment.
						if (Arrays.equals(exif_buf, EXIF_ID.getBytes())) {
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
	
	public static void insertComments(InputStream is, OutputStream os, List<String> comments) throws IOException {
		boolean finished = false;
		short marker;
		Marker emarker;
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
			throw new IOException("Invalid JPEG image, expected SOI marker not found!");
	
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.SOS) {
				// Write comment
				for(String comment : comments)
					writeComment(comment, os);
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
				    default:
						marker = copySegment(marker, is, os);
				}
			}
	    }
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
		int app0Index = -1;
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
				for(int i = 0; i <= app0Index; i++) {
					segments.get(i).write(os);
				}
				// Now we insert the EXIF data
		    	IFD newExifSubIFD = exif.getExifIFD();
		    	IFD newGpsSubIFD = exif.getGPSIFD();
		    	IFD newImageIFD = exif.getImageIFD();
		    	ExifThumbnail newThumbnail = exif.getThumbnail();  
		    	// Define new IFDs
		    	IFD exifSubIFD = null;
		    	IFD gpsSubIFD = null;
		    	IFD imageIFD = null;
		    	// Got to do something to keep the old data
		    	if(update && oldExif != null) {
		    		IFD oldImageIFD = oldExif.getImageIFD();
			    	IFD oldExifSubIFD = oldExif.getExifIFD();
			    	IFD oldGpsSubIFD = oldExif.getGPSIFD();
			    	ExifThumbnail thumbnail = oldExif.getThumbnail();
			    	
			    	if(oldImageIFD != null) {
			    		imageIFD = new IFD();
			    		imageIFD.addFields(oldImageIFD.getFields());
			    	}
			    	if(thumbnail != null) {
			    		if(newThumbnail == null)
			    			newThumbnail = thumbnail;
					}
			    	if(oldExifSubIFD != null) {
			    		exifSubIFD = new IFD();
			    		exifSubIFD.addFields(oldExifSubIFD.getFields());
			    	}
			    	if(oldGpsSubIFD != null) {
			    		gpsSubIFD = new IFD();
			    		gpsSubIFD.addFields(oldGpsSubIFD.getFields());
					}
		    	}
		    	if(newImageIFD != null) {
		    		if(imageIFD == null)
		    			imageIFD = new IFD();
		    		imageIFD.addFields(newImageIFD.getFields());
		    	}	    	
		    	if(exifSubIFD != null) {
		    		if(newExifSubIFD != null)
		    			exifSubIFD.addFields(newExifSubIFD.getFields());
		    	} else
		    		exifSubIFD = newExifSubIFD;
		    	if(gpsSubIFD != null) {
		    		if(newGpsSubIFD != null)
		    			gpsSubIFD.addFields(newGpsSubIFD.getFields());
		    	} else
		    		gpsSubIFD = newGpsSubIFD;
		    	// If we have ImageIFD, set Image IFD attached with EXIF and GPS
		     	if(imageIFD != null) {
		    		if(exifSubIFD != null)
			    		imageIFD.addChild(TiffTag.EXIF_SUB_IFD, exifSubIFD);
		    		if(gpsSubIFD != null)
			    		imageIFD.addChild(TiffTag.GPS_SUB_IFD, gpsSubIFD);
		    		exif.setImageIFD(imageIFD);
		    	} else { // Otherwise, set EXIF and GPS IFD separately
		    		exif.setExifIFD(exifSubIFD);
		    		exif.setGPSIFD(gpsSubIFD);
		    	}
		   		exif.setThumbnail(newThumbnail);
		   		// Now insert the new EXIF to the JPEG
		   		exif.write(os);
		     	// Copy the remaining segments
				for(int i = app0Index + 1; i < segments.size(); i++) {
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
						if(exifBytes.length >= EXIF_ID.length() && new String(exifBytes, 0, EXIF_ID.length()).equals(EXIF_ID)) { // We assume EXIF data exist only in one APP1
							oldExif = new JpegExif(ArrayUtils.subArray(exifBytes, EXIF_ID.length(), length - EXIF_ID.length() - 2));
							segments.remove(segments.size() - 1);
						}											
						marker = IOUtils.readShortMM(is);
						break;
				    case APP0:
				    	app0Index = segments.size();
				    default:
					    length = IOUtils.readUnsignedShortMM(is);				
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					    if(emarker == Marker.UNKNOWN)
					    	segments.add(new UnknownSegment(marker, length, buf));
					    else
					    	segments.add(new Segment(emarker, length, buf));
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
		// Close the input stream in case it's an instance of RandomAccessInputStream
		if(is instanceof RandomAccessInputStream)
			((RandomAccessInputStream)is).shallowClose();
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
				for(int i = index + 1; i < segments.size(); i++) {
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
								byte[] temp = new byte[length - ICC_PROFILE_ID.length() - 2];
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
				    	 if(emarker == Marker.UNKNOWN)
				    		 segments.add(new UnknownSegment(marker, length, buf));
				    	 else
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
	 * @param os OutputStream for the image with IPTC inserted
	 * @param iptcs a collection of IPTCDataSet to be inserted
	 * @param update boolean if true, keep the original IPTC data; otherwise, replace it completely with the new IPTC data 
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
					if(iptcBIM != null && update) { // Keep the original values
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
				for(int i = index + 1; i < segments.size(); i++) {
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
			    		if(eightBIMStream == null)
			    			eightBIMStream = new ByteArrayOutputStream();
				   		readAPP13(is, eightBIMStream);			    	
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
					    if(emarker == Marker.UNKNOWN)
					    	segments.add(new UnknownSegment(marker, length, buf));
					    else
					    	segments.add(new Segment(emarker, length, buf));
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}
	
	/**
	 * Inserts a collection of _8BIM into a JPEG APP13 Photoshop IRB segment
	 * 
	 * @param is InputStream for the original image
	 * @param os OutputStream for the image with _8BIMs inserted
	 * @param bims a collection of _8BIM to be inserted
	 * @param update boolean if true, keep the other _8BIMs; otherwise, replace the whole IRB with the inserted _8BIMs 
	 * @throws IOException
	 */
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
				for(int i = index + 1; i < segments.size(); i++) {
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
					    if(emarker == Marker.UNKNOWN)
					    	segments.add(new UnknownSegment(marker, length, buf));
					    else
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
	
	/**
	 * Insert a collection of Metadata into an image
	 * 
	 * @param metadata a collection of Metadata to be inserted
	 * @param is InputStream for the image
	 * @param os OutputStream for the image with Metadata inserted
	 * @throws IOException
	 */
	public static void insertMetadata(Collection<Metadata> metadata, InputStream is, OutputStream os) throws IOException {
		boolean finished = false;
		int length = 0;
		short marker;
		Marker emarker;
		int app0Index = -1;
		int exifIndex = -1;
		
		// Used to read multiple segment Adobe APP13
		ByteArrayOutputStream eightBIMStream = null;
		
		// Create a map to hold all the metadata
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		for(Metadata meta : metadata)
			metadataMap.put(meta.getType(), meta);
		
		// Remove duplicate IPTC if we have them in both places of IPTC and IRB
		if(metadataMap.get(MetadataType.IPTC) != null) {
			IRB irb = (IRB)metadataMap.get(MetadataType.PHOTOSHOP_IRB);
			if(irb != null) {
				// Shallow copy the map.
	    		Map<Short, _8BIM> bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());
				bimMap.remove(ImageResourceID.IPTC_NAA.getValue());
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				for(_8BIM bim : bimMap.values())
					bim.write(bout);
				metadataMap.put(MetadataType.PHOTOSHOP_IRB, new IRB(bout.toByteArray()));
			}
		}
		
		// Create a list to hold the temporary Segments 
		List<Segment> segments = new ArrayList<Segment>();
		
		// Before we actually read anything, see if we want to generate a thumbnail from the input first
		// We need thumbnail image but don't have one, create one from the current image input stream
		Exif exif = (Exif)metadataMap.remove(MetadataType.EXIF);
	
		if(exif != null && exif.isThumbnailRequired() && !exif.containsThumbnail()) {
			is = new FileCacheRandomAccessInputStream(is);
			// Insert thumbnail into EXIF wrapper
			exif.setThumbnailImage(IMGUtils.createThumbnail(is));
		}
		
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
			throw new IllegalArgumentException("Invalid JPEG image, expected SOI marker not found!");
		
		IOUtils.writeShortMM(os, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(is);
		
		while (!finished) {
			if (Marker.fromShort(marker) == Marker.SOS)	{
				/* Insert the items from the metadata map we created from the input
				 * metadata collection. Not all types of metadata are supported.
				 * Only the ones we can handle are inserted.
				 */
				// Insert APP0 JFIF first
				JFIF jfif = (JFIF)metadataMap.remove(MetadataType.JPG_JFIF);
				for(int i = 0; i < app0Index; i++) // Write until before APP0 index
					segments.get(i).write(os);
				if(jfif != null) {
					//placeholder - writeAPP0;
				}
				// Insert all the other items from the segment until we come to EXIF
				int index = Math.max(app0Index, exifIndex);
				for(int i = (app0Index < 0 ? 0 : app0Index); i < index; i++)
					segments.get(i).write(os);
				if (exif != null) {
					// Now we insert the EXIF data
			   		exif.write(os);
				}
				XMP xmp = (XMP)metadataMap.remove(MetadataType.XMP);
				if (xmp instanceof JpegXMP) {
					// Now we insert the XMP data
			   		xmp.write(os);
				}
				ICCProfile profile = (ICCProfile)metadataMap.remove(MetadataType.ICC_PROFILE);
				if(profile != null) {
					profile.write(os);
				}			
				IPTC iptc = (IPTC)metadataMap.remove(MetadataType.IPTC);
				if(iptc != null) {
					Map<Short, _8BIM> bimMap = null;
					if(eightBIMStream != null) {
						IRB irb = new IRB(eightBIMStream.toByteArray());
			    		// Shallow copy the map.
			    		bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());
						bimMap.remove(ImageResourceID.IPTC_NAA.getValue());
						// Reset stream for further use
				  		eightBIMStream.reset();						
					} else {
		    			eightBIMStream = new ByteArrayOutputStream();
				  	}
					// Insert IPTC data as one of IRB 8BIM block
					iptc.write(eightBIMStream);
					// Create 8BIM for IPTC
					_8BIM newBIM = new _8BIM(ImageResourceID.IPTC_NAA.getValue(), "iptc", eightBIMStream.toByteArray());
					if(bimMap != null) {
						bimMap.put(newBIM.getID(), newBIM); // Add the IPTC_NAA 8BIM to the map
						writeIRB(os, bimMap.values()); // Write the whole thing as one APP13
					} else {
						writeIRB(os, newBIM); // Write the one and only one 8BIM as one APP13
					}
				}
				IRB irb = (IRB)metadataMap.remove(MetadataType.PHOTOSHOP_IRB);
				if(irb != null) {
					writeIRB(os, irb.get8BIM().values());
				}
				Comments comments = (Comments)metadataMap.remove(MetadataType.COMMENT);
				if(comments != null) {
					// Write comment
					for(String comment : comments.getComments())
						writeComment(comment, os);
				}
				// Write the other segments
				for(int i = index; i < segments.size(); i++)
					segments.get(i).write(os);
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
				    case APP0:
				    	length = IOUtils.readUnsignedShortMM(is);
				    	byte[] app0Bytes = new byte[length - 2];
				    	IOUtils.readFully(is, app0Bytes);
				    	if(metadataMap.get(MetadataType.JPG_JFIF) != null) {
				    		// We are supposed to skip this, but as we haven't actually implemented
				    		// inserting of the APP0 JFIF, we still keep it in the segment list
				    		segments.add(new Segment(emarker, length, app0Bytes));
				    	} else
				    		segments.add(new Segment(emarker, length, app0Bytes));
				    	app0Index = segments.size(); // Keep the APP0 index
						marker = IOUtils.readShortMM(is);
				    	break;
				    case APP1:
				        // Read and remove the old XMP data
				        length = IOUtils.readUnsignedShortMM(is);
				        byte[] temp = new byte[length - 2];
				        IOUtils.readFully(is, temp);
				        // Remove XMP and ExtendedXMP segments.
				        if(metadataMap.get(MetadataType.XMP) != null && temp.length >= XMP_EXT_ID.length() && new String(temp, 0, XMP_EXT_ID.length()).equals(XMP_EXT_ID)) {
			                ;
				        } else if (metadataMap.get(MetadataType.XMP) != null && temp.length >= XMP_ID.length() && new String(temp, 0, XMP_ID.length()).equals(XMP_ID)) {
			                ;					        
				        } else if(exif != null && temp.length >= EXIF_ID.length() && new String(temp, 0, EXIF_ID.length()).equals(EXIF_ID)) {
					        // We keep the EXIF index
				        	exifIndex = segments.size();
			            } else { // Put other data into segment list
			            	segments.add(new Segment(emarker, length, temp));
				        }
				        marker = IOUtils.readShortMM(is);
				        break;
				    case APP2:
				  		length = IOUtils.readUnsignedShortMM(is);
				  	  	byte[] icc_profile_buf = new byte[length - 2];
				  	  	IOUtils.readFully(is, icc_profile_buf);	
						// ICC_PROFILE segment.
						if(metadataMap.get(MetadataType.ICC_PROFILE) != null && icc_profile_buf.length >= ICC_PROFILE_ID.length() && new String(icc_profile_buf, 0, ICC_PROFILE_ID.length()).equals(ICC_PROFILE_ID)) {
							;
						} else {// Not an ICC_Profile segment, copy it
							segments.add(new Segment(emarker, length, icc_profile_buf));
						}
						marker = IOUtils.readShortMM(is);
						break;
				    case APP13:
				    	if(eightBIMStream == null)
			    			eightBIMStream = new ByteArrayOutputStream();
				    	readAPP13(is, eightBIMStream);				    	
				    	marker = IOUtils.readShortMM(is);
				    	break;
				    default:
					    length = IOUtils.readUnsignedShortMM(is);				
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					    if(emarker == Marker.UNKNOWN)
					    	segments.add(new UnknownSegment(marker, length, buf));
					    else
					    	segments.add(new Segment(emarker, length, buf));
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
		// Close the input stream in case it's an instance of RandomAccessInputStream
		if(is instanceof RandomAccessInputStream)
			((RandomAccessInputStream)is).shallowClose();
	}
	
	/**
	 * Insert a XMP instance into the image.
	 * <p>
	 * The XMP instance can be created either by passing both standard and extended XMP or
	 * simply by passing a single standard XMP. If the serialized standard XMP size exceeds
	 * the size of one APP1 segment, it will be split into a standard part and an extended
	 * part automatically. This process is transparent to the user and is the preferred way
	 * of inserting XMP into JPEG images.
	 * 
	 * @param is InputStream for the image.
	 * @param os OutputStream for the image.
	 * @param jpegXmp XMP instance
	 * @throws IOException
	 * @see JpegXMP
	 */
	public static void insertXMP(InputStream is, OutputStream os, XMP jpegXmp) throws IOException {
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
				jpegXmp.write(os);
				// Copy the remaining segments
				for(int i = index + 1; i < segments.size(); i++) {
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
				        byte[] temp = new byte[length - 2];
				        IOUtils.readFully(is, temp);
				        // Remove XMP and ExtendedXMP segments.
				        if(temp.length >= XMP_EXT_ID.length() && new String(temp, 0, XMP_EXT_ID.length()).equals(XMP_EXT_ID)) {
			                ;
				        } else if (temp.length >= XMP_ID.length() && new String(temp, 0, XMP_ID.length()).equals(XMP_ID)) {
			                ;
				        } else {                                               
			                segments.add(new Segment(emarker, length, temp));
			                // If it's EXIF, we keep the index
			                if(temp.length >= EXIF_ID.length() && new String(temp, 0, EXIF_ID.length()).equals(EXIF_ID)) {
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
					    if(emarker == Marker.UNKNOWN)
					    	segments.add(new UnknownSegment(marker, length, buf));
					    else
					    	segments.add(new Segment(emarker, length, buf));
					    marker = IOUtils.readShortMM(is);
				}
			}
	    }
	}	
	
	/**
	 * Insert XMP into single APP1 or multiple segments. Support ExtendedXMP.
	 * The standard part of the XMP must be a valid XMP w/o packet wrapper and,
	 * In case of ExtendedXMP the required namespace and the tag xmpNote:HasExtendedXMP
	 * will be added.
	 * <p>
	 * When converted to bytes, the XMP part should be able to fit into one APP1.
	 * 
	 * @param is InputStream for the image.
	 * @param os OutputStream for the image.
	 * @param xmp XML string for the XMP - Assuming in UTF-8 format.
	 * @param extendedXmp XML string for the extended XMP - Assuming in UTF-8 format
	 * 
	 * @throws IOException
	 */
	public static void insertXMP(InputStream is, OutputStream os, String xmp, String extendedXmp) throws IOException {
		insertXMP(is, os, new JpegXMP(xmp, extendedXmp));
	}
	
	private static String hTablesToString(List<HTable> hTables) {
		final String[] HT_class_table = {"DC Component", "AC Component"};
		
		StringBuilder hufTable = new StringBuilder();
		
		hufTable.append("Huffman table information =>:\n");
		
		for(HTable table : hTables )
		{
			hufTable.append("Class: " + table.getClazz() + " (" + HT_class_table[table.getClazz()] + ")\n");
			hufTable.append("Huffman table #: " + table.getID() + "\n");
			
			byte[] bits = table.getBits();
			byte[] values = table.getValues();
			
		    int count = 0;
			
			for (int i = 0; i < bits.length; i++)
			{
				count += (bits[i]&0xff);
			}
			
            hufTable.append("Number of codes: " + count + "\n");
			
            if (count > 256)
            	throw new RuntimeException("Invalid huffman code count: " + count);			
	        
            int j = 0;
            
			for (int i = 0; i < 16; i++) {
			
				hufTable.append("Codes of length " + (i+1) + " (" + (bits[i]&0xff) +  " total): [ ");
				
				for (int k = 0; k < (bits[i]&0xff); k++) {
					hufTable.append((values[j++]&0xff) + " ");
				}
				
				hufTable.append("]\n");
			}
			
			hufTable.append("<= End of Huffman table information>>\n");
		}
		
		return hufTable.toString();
	}
	
	private static String qTablesToString(List<QTable> qTables) {
		StringBuilder qtTables = new StringBuilder();
				
		qtTables.append("Quantization table information =>:\n");
		
		int count = 0;
		
		for(QTable table : qTables) {
			int QT_precision = table.getPrecision();
			int[] qTable = table.getData();
			qtTables.append("precision of QT is " + QT_precision + "\n");
			qtTables.append("Quantization table #" + table.getID() + ":\n");
			
		   	if(QT_precision == 0) {
				for (int j = 0; j < 64; j++)
			    {
					if (j != 0 && j%8 == 0) {
						qtTables.append("\n");
					}
					qtTables.append(qTable[j] + " ");			
			    }
			} else { // 16 bit big-endian
								
				for (int j = 0; j < 64; j++) {
					if (j != 0 && j%8 == 0) {
						qtTables.append("\n");
					}
					qtTables.append(qTable[j] + " ");	
				}				
			}
		   	
		   	count++;
		
			qtTables.append("\n");
			qtTables.append("***************************\n");
		}
		
		qtTables.append("Total number of Quantation tables: " + count + "\n");
		qtTables.append("End of quantization table information\n");
		
		return qtTables.toString();		
	}
	
	private static String sofToString(SOFReader reader) {
		StringBuilder sof = new StringBuilder();		
		sof.append("SOF information =>\n");
		sof.append("Precision: " + reader.getPrecision() + "\n");
		sof.append("Image height: " + reader.getFrameHeight() +"\n");
		sof.append("Image width: " + reader.getFrameWidth() + "\n");
		sof.append("# of Components: " + reader.getNumOfComponents() + "\n");
		sof.append("(1 = grey scaled, 3 = color YCbCr or YIQ, 4 = color CMYK)\n");		
		    
		for(Component component : reader.getComponents()) {
			sof.append("\n");
			sof.append("Component ID: " + component.getId() + "\n");
			sof.append("Herizontal sampling factor: " + component.getHSampleFactor() + "\n");
			sof.append("Vertical sampling factor: " + component.getVSampleFactor() + "\n");
			sof.append("Quantization table #: " + component.getQTableNumber() + "\n");
			sof.append("DC table number: " + component.getDCTableNumber() + "\n");
			sof.append("AC table number: " + component.getACTableNumber() + "\n");
		}
		
		sof.append("<= End of SOF information");
		
		return sof.toString();
	}
	
	private static void readAPP13(InputStream is, OutputStream os) throws IOException {
		int length = IOUtils.readUnsignedShortMM(is);
		byte[] temp = new byte[length - 2];
		IOUtils.readFully(is, temp, 0, length - 2);
	
		if (new String(temp, 0, PHOTOSHOP_IRB_ID.length()).equals(PHOTOSHOP_IRB_ID)) {
			os.write(ArrayUtils.subArray(temp, PHOTOSHOP_IRB_ID.length(), temp.length - PHOTOSHOP_IRB_ID.length()));	
		}
	}
	
	private static void readAPP2(InputStream is, OutputStream os) throws IOException {
		byte[] icc_profile_buf = new byte[12];
		int length = IOUtils.readUnsignedShortMM(is);
		IOUtils.readFully(is, icc_profile_buf);
		// ICC_PROFILE segment.
		if (Arrays.equals(icc_profile_buf, ICC_PROFILE_ID.getBytes())) {
			icc_profile_buf = new byte[length - 14];
		    IOUtils.readFully(is, icc_profile_buf);
		    os.write(icc_profile_buf, 2, length - 16);
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
		// Create a map to hold all the metadata and thumbnails
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		Map<String, Thumbnail> thumbnails = new HashMap<String, Thumbnail>();
		
		// Need to wrap the input stream with a BufferedInputStream to
		// speed up reading SOS
		if(!(is instanceof BufferedInputStream))
			is = new BufferedInputStream(is);
		
		// Used to read Quantization and Huffman tables
		List<QTable> m_qTables = new ArrayList<QTable>(4);
		List<HTable> m_acTables = new ArrayList<HTable>(4);	
		List<HTable> m_dcTables = new ArrayList<HTable>(4);
		
		/* Each SOFReader is associated with a single SOF segment
		 * Usually there is only one SOF segment, but for hierarchical
		 * JPEG, there could be more than one SOF
		 */
		List<SOFReader> readers = new ArrayList<SOFReader>();
		
		// Used to read multiple segment ICCProfile
		ByteArrayOutputStream iccProfileStream = null;
		
		// Used to read multiple segment Adobe APP13
		ByteArrayOutputStream eightBIMStream = null;
		
		// Used to read multiple segment XMP
		byte[] extendedXMP = null;
		String xmpGUID = ""; // 32 byte ASCII hex string
		
		Comments comments = null;
		
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
						if(comments == null) comments = new Comments();
						comments.addComment(readSegmentData(is));
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
						SOFReader reader = readers.get(readers.size() - 1);
						marker = readSOS(is, reader);
						LOGGER.debug("\n{}", sofToString(reader));
						break;
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
				    default:
					    length = IOUtils.readUnsignedShortMM(is);
					    IOUtils.skipFully(is, length - 2);
					    marker = IOUtils.readShortMM(is);			    
				}
			}
	    }
		
		is.close();
		
		// Debugging
		LOGGER.debug("\n{}", qTablesToString(m_qTables));
		LOGGER.debug("\n{}", hTablesToString(m_acTables));	
		LOGGER.debug("\n{}", hTablesToString(m_dcTables));
		
		for(Segment segment : appnSegments) {
			byte[] data = segment.getData();
			length = segment.getLength();
			if(segment.getMarker() == Marker.APP0) {
				if (data.length >= JFIF_ID.length() && new String(data, 0, JFIF_ID.length()).equals(JFIF_ID)) {
					metadataMap.put(MetadataType.JPG_JFIF, new JFIF(ArrayUtils.subArray(data, JFIF_ID.length(), length - JFIF_ID.length() - 2)));
				}
			} else if(segment.getMarker() == Marker.APP1) {
				// Check for EXIF
				if(data.length >= EXIF_ID.length() && new String(data, 0, EXIF_ID.length()).equals(EXIF_ID)) {
					// We found EXIF
					JpegExif exif = new JpegExif(ArrayUtils.subArray(data, EXIF_ID.length(), length - EXIF_ID.length() - 2));
					metadataMap.put(MetadataType.EXIF, exif);
				} else if(data.length >= XMP_ID.length() && new String(data, 0, XMP_ID.length()).equals(XMP_ID) ||
						data.length >= NON_STANDARD_XMP_ID.length() && new String(data, 0, NON_STANDARD_XMP_ID.length()).equals(NON_STANDARD_XMP_ID)) {
					// We found XMP, add it to metadata list (We may later revise it if we have ExtendedXMP)
					XMP xmp = new JpegXMP(ArrayUtils.subArray(data, XMP_ID.length(), length - XMP_ID.length() - 2));
					metadataMap.put(MetadataType.XMP, xmp);
					// Retrieve XMP GUID if available
					xmpGUID = XMLUtils.getAttribute(xmp.getXmpDocument(), "rdf:Description", "xmpNote:HasExtendedXMP");
				} else if(data.length >= XMP_EXT_ID.length() && new String(data, 0, XMP_EXT_ID.length()).equals(XMP_EXT_ID)) {
					// We found ExtendedXMP, add the data to ExtendedXMP memory buffer				
					int i = XMP_EXT_ID.length();
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
						byte[] xmpBytes = ArrayUtils.subArray(data, i, length - XMP_EXT_ID.length() - 42);
						System.arraycopy(xmpBytes, 0, extendedXMP, (int)offset, xmpBytes.length);
					}
				}
			} else if(segment.getMarker() == Marker.APP2) {
				// We're only interested in ICC_Profile
				if (data.length >= ICC_PROFILE_ID.length() && new String(data, 0, ICC_PROFILE_ID.length()).equals(ICC_PROFILE_ID)) {
					if(iccProfileStream == null)
						iccProfileStream = new ByteArrayOutputStream();
					iccProfileStream.write(ArrayUtils.subArray(data, ICC_PROFILE_ID.length() + 2, length - ICC_PROFILE_ID.length() - 4));
				}
			} else if(segment.getMarker() == Marker.APP12) {
				if (data.length >= DUCKY_ID.length() && new String(data, 0, DUCKY_ID.length()).equals(DUCKY_ID)) {
					metadataMap.put(MetadataType.JPG_DUCKY, new Ducky(ArrayUtils.subArray(data, DUCKY_ID.length(), length - DUCKY_ID.length() - 2)));
				}
			} else if(segment.getMarker() == Marker.APP13) {
				if (data.length >= PHOTOSHOP_IRB_ID.length() && new String(data, 0, PHOTOSHOP_IRB_ID.length()).equals(PHOTOSHOP_IRB_ID)) {
					if(eightBIMStream == null)
						eightBIMStream = new ByteArrayOutputStream();
					eightBIMStream.write(ArrayUtils.subArray(data, PHOTOSHOP_IRB_ID.length(), length - PHOTOSHOP_IRB_ID.length() - 2));
				}
			} else if(segment.getMarker() == Marker.APP14) {
				if (data.length >= ADOBE_ID.length() && new String(data, 0, ADOBE_ID.length()).equals(ADOBE_ID)) {
					metadataMap.put(MetadataType.JPG_ADOBE, new Adobe(ArrayUtils.subArray(data, ADOBE_ID.length(), length - ADOBE_ID.length() - 2)));
				}
			}
		}
		
		// Now it's time to join multiple segments ICC_PROFILE and/or XMP		
		if(iccProfileStream != null) { // We have ICCProfile data
			ICCProfile icc_profile = new ICCProfile(iccProfileStream.toByteArray());
			metadataMap.put(MetadataType.ICC_PROFILE, icc_profile);
		}
		
		if(eightBIMStream != null) {
			IRB irb = new IRB(eightBIMStream.toByteArray());	
			metadataMap.put(MetadataType.PHOTOSHOP_IRB, irb);
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
		
		if(comments != null)
			metadataMap.put(MetadataType.COMMENT, comments);
		
		// Extract thumbnails to ImageMetadata
		Metadata meta = metadataMap.get(MetadataType.EXIF);
		if(meta != null) {
			Exif exif = (Exif)meta;
			if(!exif.isDataRead())
				exif.read();
			if(exif.containsThumbnail()) {
				thumbnails.put("EXIF", exif.getThumbnail());
			}
		}
		
		meta = metadataMap.get(MetadataType.PHOTOSHOP_IRB);
		if(meta != null) {
			IRB irb = (IRB)meta;
			if(!irb.isDataRead())
				irb.read();
			if(irb.containsThumbnail()) {
				thumbnails.put("PHOTOSHOP_IRB", irb.getThumbnail());
			}
		}
		
		metadataMap.put(MetadataType.IMAGE, new ImageMetadata(thumbnails));
		
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
						} else
							marker = copySegment(marker, is, os);
						break;						
					case APP0:
						if(metadataTypes.contains(MetadataType.JPG_JFIF)) {
							length = IOUtils.readUnsignedShortMM(is);
						    byte[] temp = new byte[length - 2];
						    IOUtils.readFully(is, temp);
							//byte[] temp = new byte[JFIF_ID.length()];
							// JFIF segment
							if (temp.length >= JFIF_ID.length() && new String(temp, 0, JFIF_ID.length()).equals(JFIF_ID)) {
								;
							} else {
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short) length);
								IOUtils.write(os, temp);
							}
							marker = IOUtils.readShortMM(is);
						} else
							marker = copySegment(marker, is, os);
						break;
					case APP1:
						// We are only interested in EXIF and XMP
						if(metadataTypes.contains(MetadataType.EXIF) || metadataTypes.contains(MetadataType.XMP)) {
							length = IOUtils.readUnsignedShortMM(is);
							byte[] temp = new byte[length - 2];
							IOUtils.readFully(is, temp);
							// XMP segment.
							if(metadataTypes.contains(MetadataType.XMP) && temp.length >= XMP_EXT_ID.length() && new String(temp, 0, XMP_EXT_ID.length()).equals(XMP_EXT_ID)) {
								;
							} else if(metadataTypes.contains(MetadataType.XMP) && temp.length >= XMP_ID.length() && new String(temp, 0, XMP_ID.length()).equals(XMP_ID)) {
								;
							} else if(metadataTypes.contains(MetadataType.XMP) && temp.length >= NON_STANDARD_XMP_ID.length() && new String(temp, 0, NON_STANDARD_XMP_ID.length()).equals(NON_STANDARD_XMP_ID)) {
								;
							} else if(metadataTypes.contains(MetadataType.EXIF) && temp.length >= EXIF_ID.length() && new String(temp, 0, EXIF_ID.length()).equals(EXIF_ID)) { // EXIF
								;
							} else { // We don't want to remove any of them
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short) length);
								IOUtils.write(os, temp);
							}
							marker = IOUtils.readShortMM(is);
						} else
							marker = copySegment(marker, is, os);
						break;
					case APP2:
						if(metadataTypes.contains(MetadataType.ICC_PROFILE)) {
							length = IOUtils.readUnsignedShortMM(is);
							byte[] temp = new byte[length - 2];
							IOUtils.readFully(is, temp);	
							// ICC_Profile segment
							if (temp.length >= ICC_PROFILE_ID.length() && new String(temp, 0, ICC_PROFILE_ID.length()).equals(ICC_PROFILE_ID)) {
								;
							} else {
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short) length);
								IOUtils.write(os, temp);
							}
							marker = IOUtils.readShortMM(is);
						} else
							marker = copySegment(marker, is, os);
						break;
					case APP12:
						if(metadataTypes.contains(MetadataType.JPG_DUCKY)) {
							length = IOUtils.readUnsignedShortMM(is);
							byte[] temp = new byte[length - 2];
							IOUtils.readFully(is, temp);	
							// Ducky segment
							if (temp.length >= DUCKY_ID.length() && new String(temp, 0, DUCKY_ID.length()).equals(DUCKY_ID)) {
								;
							} else {
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short) length);
								IOUtils.write(os, temp);
							}
							marker = IOUtils.readShortMM(is);
						} else
							marker = copySegment(marker, is, os);
						break;
					case APP13:
						if(metadataTypes.contains(MetadataType.PHOTOSHOP_IRB) || metadataTypes.contains(MetadataType.IPTC)
							|| metadataTypes.contains(MetadataType.XMP) || metadataTypes.contains(MetadataType.EXIF)) {
							length = IOUtils.readUnsignedShortMM(is);
							byte[] temp = new byte[length - 2];
							IOUtils.readFully(is, temp);	
							// PHOTOSHOP IRB segment
							if (temp.length >= PHOTOSHOP_IRB_ID.length() && new String(temp, 0, PHOTOSHOP_IRB_ID.length()).equals(PHOTOSHOP_IRB_ID)) {
								IRB irb = new IRB(ArrayUtils.subArray(temp, PHOTOSHOP_IRB_ID.length(), temp.length - PHOTOSHOP_IRB_ID.length()));
								// Shallow copy the map.
								Map<Short, _8BIM> bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());								
								if(!metadataTypes.contains(MetadataType.PHOTOSHOP_IRB)) {
									if(metadataTypes.contains(MetadataType.IPTC)) {
										// We only remove IPTC_NAA and keep the other IRB data untouched.
										bimMap.remove(ImageResourceID.IPTC_NAA.getValue());
									} 
									if(metadataTypes.contains(MetadataType.XMP)) {
										// We only remove XMP and keep the other IRB data untouched.
										bimMap.remove(ImageResourceID.XMP_METADATA.getValue());
									} 
									if(metadataTypes.contains(MetadataType.EXIF)) {
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
								IOUtils.write(os, temp);
							}
							marker = IOUtils.readShortMM(is);
						} else
							marker = copySegment(marker, is, os);
						break;
					case APP14:
						if(metadataTypes.contains(MetadataType.JPG_ADOBE)) {
							length = IOUtils.readUnsignedShortMM(is);
							byte[] temp = new byte[length - 2];
							IOUtils.readFully(is, temp);	
							// Adobe segment
							if (temp.length >= ADOBE_ID.length() && new String(temp, 0, ADOBE_ID.length()).equals(ADOBE_ID)) {
								;
							} else {
								IOUtils.writeShortMM(os, marker);
								IOUtils.writeShortMM(os, (short) length);
								IOUtils.write(os, temp);
							}
							marker = IOUtils.readShortMM(is);
						} else
							marker = copySegment(marker, is, os);
						break;
					default:
						marker = copySegment(marker, is, os);
				}
			}
		}
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
	
	private static void writeComment(String comment, OutputStream os) throws IOException	{
		new COMBuilder().comment(comment).build().write(os);
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
	private static void writeICCProfile(OutputStream os, byte[] data) throws IOException {
		// ICC_Profile ID
		int maxSegmentLen = 65535;
		int maxICCDataLen = 65519;
		int numOfSegment = data.length/maxICCDataLen;
		int leftOver = data.length%maxICCDataLen;
		int totalSegment = (numOfSegment == 0)? 1: ((leftOver == 0)? numOfSegment: (numOfSegment + 1));
		for(int i = 0; i < numOfSegment; i++) {
			IOUtils.writeShortMM(os, Marker.APP2.getValue());
			IOUtils.writeShortMM(os, maxSegmentLen);
			IOUtils.write(os, ICC_PROFILE_ID.getBytes());
			IOUtils.writeShortMM(os, totalSegment|(i+1)<<8);
			IOUtils.write(os, data, i*maxICCDataLen, maxICCDataLen);
		}
		if(leftOver != 0) {
			IOUtils.writeShortMM(os, Marker.APP2.getValue());
			IOUtils.writeShortMM(os, leftOver + 16);
			IOUtils.write(os, ICC_PROFILE_ID.getBytes());
			IOUtils.writeShortMM(os, totalSegment|totalSegment<<8);
			IOUtils.write(os, data, data.length - leftOver, leftOver);
		}
	}
	
	private static void writeIRB(OutputStream os, _8BIM ... bims) throws IOException {
		if(bims != null && bims.length > 0)
			writeIRB(os, Arrays.asList(bims));
	}
	
	private static void writeIRB(OutputStream os, Collection<_8BIM> bims) throws IOException {
		if(bims != null && bims.size() > 0) {
			IOUtils.writeShortMM(os, Marker.APP13.getValue());
	    	ByteArrayOutputStream bout = new ByteArrayOutputStream();
			for(_8BIM bim : bims)
				bim.write(bout);
			// Write segment length
			IOUtils.writeShortMM(os, 14 + 2 +  bout.size());
			// Write segment data
			os.write(PHOTOSHOP_IRB_ID.getBytes());
			os.write(bout.toByteArray());
		}
	}
	
	// Prevent from instantiation
	private JPEGTweaker() {}
}