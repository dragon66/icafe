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
 * TIFFTweaker.java
 *
 * Who   Date       Description
 * ====  =========  ===================================================================
 * WY    07Apr2015  Merge Adobe IPTC and TIFF IPTC if both exist
 * WY    15Mar2015  Cleanup debugging output
 * WY    18Feb2015  Added removeMetadata() to remove meta data from TIFF
 * WY    14Feb2015  Added insertXMP() to insert XMP data to TIFF
 * WY    06Feb2015  Added printIFDs() and printIFD()
 * WY    04Feb2015  Revised insertExif() to keep original EXIF data is needed
 * WY    03Feb2015  Added removeExif() to remove EXIF and GPS data from TIFF image
 * WY    01Feb2015  Revised to remove duplicates when combining normal and Photoshop IPTC
 * WY    29Jan2015  Revised insertIPTC() and insertIRB() to keep old data
 * WY    27Jan2015  Implemented insertThumbnail() to insert thumbnail to Photoshop IRB
 * WY    27Jan2015  Added insertIRB() to insert Photoshop IRB data
 * WY    26Jan2015  Added insertIPTC() to insert IPTC data
 * WY    20Jan2015  Revised to work with Metadata.showMetadata()
 * WY    12Jan2015  Added showIPTC() to show IPTC private tag information
 * WY    11Jan2015  Added extractThumbnail() to extract Photoshop thumbnail
 * WY    10Jan2015  Added showICCProfile() and showPhotoshop()
 * WY    23Dec2014  Added extractICCProfile() to extract ICC_Profile from TIFF image
 * WY    22Dec2014  Added insertICCProfile() to insert ICC_Profile into TIFF image
 * WY    17Dec2014  Changed TIFFFrame to ImageFrame due to rename of TIFFFrame
 * WY    15Dec2014  Added insertTiffImage() to insert one TIFF image into another
 * WY    15Dec2014  Added append() to append new pages to the end of existing TIFF
 * WY    24Nov2014  Changed removePages() to remove the actual pages from the arguments
 * WY    24Nov2014  Changed write(TIFFImage) to write(TIFFImage, RandomAccessOutputStream)
 * WY    22Nov2014  Removed unnecessary TIFFWriter argument from corresponding methods
 * WY    21Nov2014  Added new writeMultipageTIFF() to use TIFFFrame array as argument
 * WY    12Nov2014  Added support for up to 32 BitsPerSample image to mergeTiffImagesEx()
 * WY    11Nov2014  Added getRowWidth() to determine scan line stride
 * WY    07Nov2014  Fixed bug for mergeTiffImagesEx() when there is no compression field
 * WY    06Nov2014  Fixed bug for getUncompressedStripByteCounts() with YCbCr image
 * WY    28Oct2014  Changed mergeTiffImagesEx() to use flipEndian() from ArrayUtils
 * WY    24Oct2014  Added getBytes2Read() to fix bug of uncompressed image with only one
 *                  strip/SamplesPerPixel strips for PlanaryConfiguration = 2 and wrong
 *                  StripByteCounts value/values
 * WY    24Oct2014  Revised getUncompressedStripByteCounts to include YCbCrSubSampling
 * WY    21Oct2014  Changed copyPageData() and mergeTiffImagesEx() to use the extracted
 *                  getUncompressedStripByteCounts() method
 * WY    21Oct2014  Extracted method getUncompressedStripByteCounts()
 * WY    20Oct2014  Added mergeTiffImagesEx(RandomAccessOutputStream, File...)
 * WY    09Oct2014  Added mergeTiffImages(RandomAccessOutputStream, File...)
 * WY    07Oct2014  Added mergeTiffImages() to merge two multiple page TIFFs
 * WY    19Sep2014  Reset pointer to the stream head in getPageCount()
 * WY    08May2014  Added insertExif() to insert EXIF data to TIFF page
 * WY    26Apr2014  Rewrite insertPage() to insert multiple pages one at a time
 * WY    11Apr2014  Added writeMultipageTIFF() to support creating multiple page TIFFs
 * WY    09Apr2014  Added splitPages() to split multiple page TIFFs into single page TIFFs
 * WY    09Apr2014  Added insertPages() to insert pages to multiple page TIFFs
 * WY    08Apr2014  Added insertPage() to insert a single page to multiple page TIFFs
 * WY    07Apr2014  Added getPageCount() to get the total pages for a TIFF image
 * WY    06Apr2014  Added retainPages() to keep pages from multiple page TIFFs
 * WY    04Apr2014  Added removePages() to remove pages from multiple page TIFFs
 * WY    02Apr2014  Added writePageData() for multiple page TIFFs
 */

package cafe.image.tiff;

import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import cafe.image.ImageFrame;
import cafe.image.ImageIO;
import cafe.image.ImageParam;
import cafe.image.ImageType;
import cafe.image.compression.ImageDecoder;
import cafe.image.compression.ImageEncoder;
import cafe.image.compression.deflate.DeflateDecoder;
import cafe.image.compression.deflate.DeflateEncoder;
import cafe.image.compression.lzw.LZWTreeDecoder;
import cafe.image.compression.lzw.LZWTreeEncoder;
import cafe.image.compression.packbits.Packbits;
import cafe.image.jpeg.Marker;
import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.image.meta.adobe.IRB;
import cafe.image.meta.adobe.IRBThumbnail;
import cafe.image.meta.adobe.ImageResourceID;
import cafe.image.meta.adobe.XMP;
import cafe.image.meta.adobe._8BIM;
import cafe.image.meta.exif.Exif;
import cafe.image.meta.exif.ExifTag;
import cafe.image.meta.exif.GPSTag;
import cafe.image.meta.exif.InteropTag;
import cafe.image.meta.exif.TiffExif;
import cafe.image.meta.icc.ICCProfile;
import cafe.image.meta.iptc.IPTC;
import cafe.image.meta.iptc.IPTCDataSet;
import cafe.image.util.IMGUtils;
import cafe.image.writer.ImageWriter;
import cafe.image.writer.TIFFWriter;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;
import cafe.io.ReadStrategyII;
import cafe.io.ReadStrategyMM;
import cafe.io.WriteStrategyII;
import cafe.io.WriteStrategyMM;
import cafe.string.StringUtils;
import cafe.string.XMLUtils;
import cafe.util.ArrayUtils;
import static cafe.image.writer.TIFFWriter.*;

/**
 * TIFF image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/28/2014
 */
public class TIFFTweaker {
	public static void append(RandomAccessInputStream rin, RandomAccessOutputStream rout, BufferedImage ... images) throws IOException {
		append(rin, rout, null, images);
	}
	
	/**
	 * Append ImageFrames to the end of the original TIFF image
	 * 
	 * @param frames an array of ImageFrame to be appended
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws IOException
	 */
	public static void append(RandomAccessInputStream rin, RandomAccessOutputStream rout, ImageFrame ... frames) throws IOException {
		insertPages(rin, rout, getPageCount(rin), frames);
	}
	
	//
	// Append an array of BufferedImage to the end of the existing TIFF image
	public static void append(RandomAccessInputStream rin, RandomAccessOutputStream rout, ImageParam[] imageParam, BufferedImage ... images) throws IOException {
		insertPages(rin, rout, getPageCount(rin), imageParam, images);
	}
	
	/**
	 * Read an input TIFF and write it to a new TIFF.
	 * The EXIF and GPS information, if present, are preserved.
	 * 
	 * @param rin a RandomAccessInputStream 
	 * @param rout a RandomAccessOutputStream
	 * @throws IOException
	 */
	public static void copyCat(RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		List<IFD> list = new ArrayList<IFD>();
	   
		int offset = copyHeader(rin, rout);
		
		int writeOffset = FIRST_WRITE_OFFSET;
		// Read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, list, offset, rin);
		offset = copyPages(list, writeOffset, rin, rout);
		int firstIFDOffset = list.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);
	}
	
	private static int copyHeader(RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {		
		rin.seek(STREAM_HEAD);
		// First 2 bytes determine the byte order of the file, "MM" or "II"
	    short endian = rin.readShort();
	
		if (endian == IOUtils.BIG_ENDIAN) {
		    rin.setReadStrategy(ReadStrategyMM.getInstance());
		    rout.setWriteStrategy(WriteStrategyMM.getInstance());
		} else if(endian == IOUtils.LITTLE_ENDIAN) {
		    rin.setReadStrategy(ReadStrategyII.getInstance());
		    rout.setWriteStrategy(WriteStrategyII.getInstance());
		} else {
			rin.close();
			rout.close();
			throw new RuntimeException("Invalid TIFF byte order");
	    } 
		
		rout.writeShort(endian);
		// Read TIFF identifier
		rin.seek(0x02);
		short tiff_id = rin.readShort();
		
		if(tiff_id!=0x2a)//"*" 42 decimal
		{
		   rin.close();
		   rout.close();
		   throw new RuntimeException("Invalid TIFF identifier");
		}
		
		rout.writeShort(tiff_id);
		rin.seek(OFFSET_TO_WRITE_FIRST_IFD_OFFSET);
		
		return rin.readInt();
	}
	
	private static List<IPTCDataSet> copyIPTCDataSet(List<IPTCDataSet> iptcs, byte[] data) throws IOException {
		IPTC iptc = new IPTC(data);
		// Shallow copy the map
		Map<String, List<IPTCDataSet>> dataSetMap = new HashMap<String, List<IPTCDataSet>>(iptc.getDataSet());
		for(IPTCDataSet set : iptcs)
			if(!set.allowMultiple())
				dataSetMap.remove(set.getName());
		for(List<IPTCDataSet> iptcList : dataSetMap.values())
			iptcs.addAll(iptcList);
		
		return iptcs;
	}
	
	private static TiffField<?> copyJPEGHufTable(RandomAccessInputStream rin, RandomAccessOutputStream rout, TiffField<?> field, int curPos) throws IOException	{
		int[] data = field.getDataAsLong();
		int[] tmp = new int[data.length];
	
		for(int i = 0; i < data.length; i++) {
			rin.seek(data[i]);
			tmp[i] = curPos;
			byte[] htable = new byte[16];
			IOUtils.readFully(rin, htable);
			IOUtils.write(rout, htable);			
			curPos += 16;
			
			int numCodes = 0;
			
            for(int j = 0; j < 16; j++) {
                numCodes += htable[j]&0xff;
            }
            
            curPos += numCodes;
            
            htable = new byte[numCodes];
            IOUtils.readFully(rin, htable);
			IOUtils.write(rout, htable);
		}
		
		if(TiffTag.fromShort(field.getTag()) == TiffTag.JPEG_AC_TABLES)
			return new LongField(TiffTag.JPEG_AC_TABLES.getValue(), tmp);
	
		return new LongField(TiffTag.JPEG_DC_TABLES.getValue(), tmp);
	}
	
	private static void copyJPEGIFByteCount(RandomAccessInputStream rin, RandomAccessOutputStream rout, int offset, int outOffset) throws IOException {		
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
		
		rin.seek(offset);
		rout.seek(outOffset);
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(rin)) != Marker.SOI) {
			return;
		}
		
		IOUtils.writeShortMM(rout, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(rin);
			
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.EOI) {
				IOUtils.writeShortMM(rout, marker);
				finished = true;
			} else { // Read markers
		  		emarker = Marker.fromShort(marker);
				
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone mark besides SOI, EOI, and RSTn. 
				    	marker = IOUtils.readShortMM(rin);
				    	break;
				    case SOS:						
						marker = copyJPEGSOS(rin, rout);
						break;
				    case PADDING:	
				    	int nextByte = 0;
				    	while((nextByte = rin.read()) == 0xff) {;}
				    	marker = (short)((0xff<<8)|nextByte);
				    	break;
				    default:
					    length = IOUtils.readUnsignedShortMM(rin);
					    byte[] buf = new byte[length - 2];
					    rin.read(buf);
					    IOUtils.writeShortMM(rout, marker);
					    IOUtils.writeShortMM(rout, length);
					    rout.write(buf);
					    marker = IOUtils.readShortMM(rin);					 
				}
			}
	    }
	}
	
	private static TiffField<?> copyJPEGQTable(RandomAccessInputStream rin, RandomAccessOutputStream rout, TiffField<?> field, int curPos) throws IOException {
		byte[] qtable = new byte[64];
		int[] data = field.getDataAsLong();
		int[] tmp = new int[data.length];
		
		for(int i = 0; i < data.length; i++) {
			rin.seek(data[i]);
			tmp[i] = curPos;
			IOUtils.readFully(rin, qtable);
			IOUtils.write(rout, qtable);
			curPos += 64;
		}
		
		return new LongField(TiffTag.JPEG_Q_TABLES.getValue(), tmp);
	}
	
	private static short copyJPEGSOS(RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException	{
		int len = IOUtils.readUnsignedShortMM(rin);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(rin, buf);
		IOUtils.writeShortMM(rout, Marker.SOS.getValue());
		IOUtils.writeShortMM(rout, len);
		rout.write(buf);		
		// Actual image data follow.
		int nextByte = 0;
		short marker = 0;	
		
		while((nextByte = IOUtils.read(rin)) != -1)	{
			rout.write(nextByte);
			
			if(nextByte == 0xff)
			{
				nextByte = IOUtils.read(rin);
			    rout.write(nextByte);
			    
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
	
	/**
	 * @param offset offset to write page image data
	 * 
	 * @return the position where to write the IFD for the current image page
	 */
	private static int copyPageData(IFD ifd, int offset, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		// Move stream pointer to the right place
		rout.seek(offset);

		// Original image data start from these offsets.
		TiffField<?> stripOffSets = ifd.removeField(TiffTag.STRIP_OFFSETS);
		
		if(stripOffSets == null)
			stripOffSets = ifd.removeField(TiffTag.TILE_OFFSETS);
				
		TiffField<?> stripByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS);
		
		if(stripByteCounts == null)
			stripByteCounts = ifd.getField(TiffTag.TILE_BYTE_COUNTS);	
		/* 
		 * Make sure this will work in the case when neither STRIP_OFFSETS nor TILE_OFFSETS presents.
		 * Not sure if this will ever happen for TIFF. JPEG EXIF data do not contain these fields. 
		 */
		if(stripOffSets != null) { 
			int[] counts = stripByteCounts.getDataAsLong();		
			int[] off = stripOffSets.getDataAsLong();
			int[] temp = new int[off.length];
			
			TiffField<?> tiffField = ifd.getField(TiffTag.COMPRESSION);
			
			// Uncompressed image with one strip or tile (may contain wrong StripByteCounts value)
			// Bug fix for uncompressed image with one strip and wrong StripByteCounts value
			if((tiffField == null ) || (tiffField != null && tiffField.getDataAsLong()[0] == 1)) { // Uncompressed data
				int planaryConfiguration = 1;
				
				tiffField = ifd.getField(TiffTag.PLANAR_CONFIGURATTION);		
				if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
				
				tiffField = ifd.getField(TiffTag.SAMPLES_PER_PIXEL);
				
				int samplesPerPixel = 1;
				if(tiffField != null) samplesPerPixel = tiffField.getDataAsLong()[0];
				
				// If there is only one strip/samplesPerPixel strips for PlanaryConfiguration = 2
				if((planaryConfiguration == 1 && off.length == 1) || (planaryConfiguration == 2 && off.length == samplesPerPixel))
				{
					int[] totalBytes2Read = getBytes2Read(ifd);
				
					for(int i = 0; i < off.length; i++)
						counts[i] = totalBytes2Read[i];					
				}				
			} // End of bug fix
			
			// We are going to write the image data first
			rout.seek(offset);
		
			// Copy image data from offset
			for(int i = 0; i < off.length; i++) {
				rin.seek(off[i]);
				byte[] buf = new byte[counts[i]];
				rin.readFully(buf);
				rout.write(buf);
				temp[i] = offset;
				offset += buf.length;
			}
						
			if(ifd.getField(TiffTag.STRIP_BYTE_COUNTS) != null)
				stripOffSets = new LongField(TiffTag.STRIP_OFFSETS.getValue(), temp);
			else
				stripOffSets = new LongField(TiffTag.TILE_OFFSETS.getValue(), temp);		
			ifd.addField(stripOffSets);		
		}
		
		// add copyright and software fields.
		String copyRight = "Copyright (c) Wen Yu, 2014 (yuwen_66@yahoo.com)\0";
		ifd.addField(new ASCIIField(TiffTag.COPYRIGHT.getValue(), copyRight));
		
		String softWare = "TIFFTweaker 1.0\0";
		ifd.addField(new ASCIIField(TiffTag.SOFTWARE.getValue(), softWare));
		// End of copyright and software field.
	
		/* The following are added to work with old-style JPEG compression (type 6) */		
		/* One of the flavors (found in JPEG EXIF thumbnail IFD - IFD1) of the old JPEG compression contains this field */
		TiffField<?> jpegIFOffset = ifd.removeField(TiffTag.JPEG_INTERCHANGE_FORMAT);
		if(jpegIFOffset != null) {
			TiffField<?> jpegIFByteCount = ifd.removeField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH);			
			try {
				if(jpegIFByteCount != null) {
					rin.seek(jpegIFOffset.getDataAsLong()[0]);
					byte[] bytes2Read = new byte[jpegIFByteCount.getDataAsLong()[0]];
					rin.readFully(bytes2Read);
					rout.seek(offset);
					rout.write(bytes2Read);
					ifd.addField(jpegIFByteCount);
				} else {
					long startOffset = rout.getStreamPointer();
					copyJPEGIFByteCount(rin, rout, jpegIFOffset.getDataAsLong()[0], offset);
					long endOffset = rout.getStreamPointer();
					ifd.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue(), new int[]{(int)(endOffset - startOffset)}));
				}
				jpegIFOffset = new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue(), new int[]{offset});
				ifd.addField(jpegIFOffset);
			} catch (EOFException ex) {;};
		}		
		/* Another flavor of the old style JPEG compression type 6 contains separate tables */
		TiffField<?> jpegTable = ifd.removeField(TiffTag.JPEG_DC_TABLES);
		if(jpegTable != null) {
			try {
				ifd.addField(copyJPEGHufTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
			} catch(EOFException ex) {;}
		}
		
		jpegTable = ifd.removeField(TiffTag.JPEG_AC_TABLES);
		if(jpegTable != null) {
			try {
				ifd.addField(copyJPEGHufTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
			} catch(EOFException ex) {;}
		}
	
		jpegTable = ifd.removeField(TiffTag.JPEG_Q_TABLES);
		if(jpegTable != null) {
			try {
				ifd.addField(copyJPEGQTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
			} catch(EOFException ex) {;}
		}
		/* End of code to work with old-style JPEG compression */
		
		// Return the actual stream position (we may have lost track of it)  
		return (int)rout.getStreamPointer();	
	}
	
	// Copy a list of IFD and associated image data if any
	private static int copyPages(List<IFD> list, int writeOffset, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		// Write the first page data
		writeOffset = copyPageData(list.get(0), writeOffset, rin, rout);
		// Then write the first IFD
		writeOffset = list.get(0).write(rout, writeOffset);
		// We are going to write the remaining image pages and IFDs if any
		for(int i = 1; i < list.size(); i++) {
			writeOffset = copyPageData(list.get(i), writeOffset, rin, rout);
			// Tell the IFD to update next IFD offset for the following IFD
			list.get(i-1).setNextIFDOffset(rout, writeOffset); 
			writeOffset = list.get(i).write(rout, writeOffset);
		}
		
		return writeOffset;
	}
	
	/**
	 * Extracts ICC_Profile from certain page of TIFF if any
	 * 
	 * @param pageNumber page number from which to extract ICC_Profile
	 * @param rin RandomAccessInputStream for the input TIFF
	 * @return a byte array for the extracted ICC_Profile or null if none exists
	 * @throws Exception
	 */
	public static byte[] extractICCProfile(int pageNumber, RandomAccessInputStream rin) throws Exception {
		// Read pass image header
		int offset = readHeader(rin);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		TiffField<?> f_iccProfile = workingPage.getField(TiffTag.ICC_PROFILE);
		if(f_iccProfile != null) {
			return (byte[])f_iccProfile.getData();
		}
		
		return null;
	}
	
	public static byte[] extractICCProfile(RandomAccessInputStream rin) throws Exception {
		return extractICCProfile(0, rin);
	}
	
	public static IRBThumbnail extractThumbnail(int pageNumber, RandomAccessInputStream rin) throws IOException {
		// Read pass image header
		int offset = readHeader(rin);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		TiffField<?> f_photoshop = workingPage.getField(TiffTag.PHOTOSHOP);
		if(f_photoshop != null) {
			byte[] data = (byte[])f_photoshop.getData();
			IRB irb = new IRB(data);
			if(irb.containsThumbnail()) {
				IRBThumbnail thumbnail = irb.getThumbnail();
				return thumbnail;					
			}		
		}
		
		return null;
	}
	
	public static IRBThumbnail extractThumbnail(RandomAccessInputStream rin) throws IOException {
		return extractThumbnail(0, rin);
	}
	
	public static void extractThumbnail(RandomAccessInputStream rin, String pathToThumbnail) throws IOException {
		IRBThumbnail thumbnail = extractThumbnail(rin);				
		if(thumbnail != null) {
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
	
	public static void finishInsert(RandomAccessOutputStream rout, List<IFD> list) throws IOException {
		// Reset pageNumber and total pages
		for(int i = 0; i < list.size(); i++) {
			int offset = list.get(i).getField(TiffTag.PAGE_NUMBER).getDataOffset();
			rout.seek(offset);
			rout.writeShort((short)i); // Update page number for this page
			rout.writeShort((short)list.size()); // Update total page number
		}		
		// Link the IFDs
		for(int i = 0; i < list.size() - 1; i++)
			list.get(i).setNextIFDOffset(rout, list.get(i+1).getStartOffset());
				
		int firstIFDOffset = list.get(0).getStartOffset();

		writeToStream(rout, firstIFDOffset);
	}
	
	// Used to calculate how many bytes to read in case we have only one strip or tile
	private static int[] getBytes2Read(IFD ifd) {
		// Let's calculate how many bytes we are supposed to read
		TiffField<?> tiffField = ifd.getField(TiffTag.IMAGE_WIDTH);
		int imageWidth = tiffField.getDataAsLong()[0];
		tiffField = ifd.getField(TiffTag.IMAGE_LENGTH);
		int imageHeight = tiffField.getDataAsLong()[0];
		
		// For YCbCr image only
		int horizontalSampleFactor = 2; // Default 2X2
		int verticalSampleFactor = 2; // Not 1X1
		
		int photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION).getDataAsLong()[0];
		
		// Correction for imageWidth and imageHeight for YCbCr image
		if(photoMetric == TiffFieldEnum.PhotoMetric.YCbCr.getValue()) {
			TiffField<?> f_YCbCrSubSampling = ifd.getField(TiffTag.YCbCr_SUB_SAMPLING);
			
			if(f_YCbCrSubSampling != null) {
				int[] sampleFactors = f_YCbCrSubSampling.getDataAsLong();
				horizontalSampleFactor = sampleFactors[0];
				verticalSampleFactor = sampleFactors[1];
			}
			imageWidth = ((imageWidth + horizontalSampleFactor - 1)/horizontalSampleFactor)*horizontalSampleFactor;
			imageHeight = ((imageHeight + verticalSampleFactor - 1)/verticalSampleFactor)*verticalSampleFactor;	
		}
		
		int samplesPerPixel = 1;
		
		tiffField = ifd.getField(TiffTag.SAMPLES_PER_PIXEL);
		if(tiffField != null) {
			samplesPerPixel = tiffField.getDataAsLong()[0];
		}				
		
		int bitsPerSample = 1;
		
		tiffField = ifd.getField(TiffTag.BITS_PER_SAMPLE);
		if(tiffField != null) {
			bitsPerSample = tiffField.getDataAsLong()[0];
		}
		
		int tileWidth = -1;
		int tileLength = -1;			
		
		TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH);
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH);
		
		if(f_tileWidth != null) {
			tileWidth = f_tileWidth.getDataAsLong()[0];
			tileLength = f_tileLength.getDataAsLong()[0];
		}
		
		int rowsPerStrip = imageHeight;
		int rowWidth = imageWidth;
		
		TiffField<?> f_rowsPerStrip = ifd.getField(TiffTag.ROWS_PER_STRIP);
		if(f_rowsPerStrip != null) rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];					
		
		if(rowsPerStrip > imageHeight) rowsPerStrip = imageHeight;
		
		if(tileWidth > 0) {
			rowsPerStrip = tileLength;
			rowWidth = tileWidth;
		}
	
		int planaryConfiguration = 1;
		
		tiffField = ifd.getField(TiffTag.PLANAR_CONFIGURATTION);
		if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
		
		int[] totalBytes2Read = new int[samplesPerPixel];		
		
		if(planaryConfiguration == 1)
			totalBytes2Read[0] = ((rowWidth*bitsPerSample*samplesPerPixel + 7)/8)*rowsPerStrip;
		else
			totalBytes2Read[0] = totalBytes2Read[1] = totalBytes2Read[2] = ((rowWidth*bitsPerSample + 7)/8)*rowsPerStrip;
		
		if(photoMetric == TiffFieldEnum.PhotoMetric.YCbCr.getValue()) {
			if(samplesPerPixel != 3) samplesPerPixel = 3;
			
			int[] sampleBytesPerRow = new int[samplesPerPixel];
			sampleBytesPerRow[0] = (bitsPerSample*rowWidth + 7)/8;
			sampleBytesPerRow[1] = (bitsPerSample*rowWidth/horizontalSampleFactor + 7)/8;
			sampleBytesPerRow[2] = sampleBytesPerRow[1];
			
			int[] sampleRowsPerStrip = new int[samplesPerPixel];
			sampleRowsPerStrip[0] = rowsPerStrip;
			sampleRowsPerStrip[1] = rowsPerStrip/verticalSampleFactor;
			sampleRowsPerStrip[2]= sampleRowsPerStrip[1];
			
			totalBytes2Read[0] = sampleBytesPerRow[0]*sampleRowsPerStrip[0];
			totalBytes2Read[1] = sampleBytesPerRow[1]*sampleRowsPerStrip[1];
			totalBytes2Read[2] = totalBytes2Read[1];
		
			if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
		
			if(planaryConfiguration == 1)
				totalBytes2Read[0] = totalBytes2Read[0] + totalBytes2Read[1] + totalBytes2Read[2];			
		}
		
		return totalBytes2Read;
	}
	
	/**
	 * Get the total number of pages for the TIFF image
	 * 
	 * @param rin RandomAccessInputStream to read the image
	 * @return total number of pages for the image
	 * @throws IOException
	 */
	public static int getPageCount(RandomAccessInputStream rin) throws IOException {
		// Keep track of the current stream pointer
		long streamPointer = rin.getStreamPointer();
		// Go the the stream head
		rin.seek(STREAM_HEAD);
		List<IFD> list = new ArrayList<IFD>();
		readIFDs(list, rin);
		// Reset stream pointer
		rin.seek(streamPointer); 
		
		return list.size();
	}
	
	private static int getRowWidth(IFD ifd) {
		// Get image dimension first
		TiffField<?> tiffField = ifd.getField(TiffTag.IMAGE_WIDTH);
		int imageWidth = tiffField.getDataAsLong()[0];
		
		// For YCbCr image only
		int horizontalSampleFactor = 2; // Default 2X2
		int photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION).getDataAsLong()[0];
		
		// Correction for imageWidth and imageHeight for YCbCr image
		if(photoMetric == TiffFieldEnum.PhotoMetric.YCbCr.getValue()) {
			TiffField<?> f_YCbCrSubSampling = ifd.getField(TiffTag.YCbCr_SUB_SAMPLING);
			
			if(f_YCbCrSubSampling != null) {
				int[] sampleFactors = f_YCbCrSubSampling.getDataAsLong();
				horizontalSampleFactor = sampleFactors[0];
			}
			imageWidth = ((imageWidth + horizontalSampleFactor - 1)/horizontalSampleFactor)*horizontalSampleFactor;
		}
		
		int rowWidth = imageWidth;		
		
		int tileWidth = -1;
		
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH);
		
		if(f_tileWidth != null) {
			tileWidth = f_tileWidth.getDataAsLong()[0];
			if(tileWidth > 0) 
				rowWidth = tileWidth;
		}
			
		return rowWidth;
	}
	
	// Calculate the expected StripByteCounts values for uncompressed image
	public static int[] getUncompressedStripByteCounts(IFD ifd, int strips) {
		// Get image dimension first
		TiffField<?> tiffField = ifd.getField(TiffTag.IMAGE_WIDTH);
		int imageWidth = tiffField.getDataAsLong()[0];
		tiffField = ifd.getField(TiffTag.IMAGE_LENGTH);
		int imageHeight = tiffField.getDataAsLong()[0];
		
		// For YCbCr image only
		int horizontalSampleFactor = 2; // Default 2X2
		int verticalSampleFactor = 2; // Not 1X1
		
		int photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION).getDataAsLong()[0];
		
		// Correction for imageWidth and imageHeight for YCbCr image
		if(photoMetric == TiffFieldEnum.PhotoMetric.YCbCr.getValue()) {
			TiffField<?> f_YCbCrSubSampling = ifd.getField(TiffTag.YCbCr_SUB_SAMPLING);
			
			if(f_YCbCrSubSampling != null) {
				int[] sampleFactors = f_YCbCrSubSampling.getDataAsLong();
				horizontalSampleFactor = sampleFactors[0];
				verticalSampleFactor = sampleFactors[1];
			}
			imageWidth = ((imageWidth + horizontalSampleFactor - 1)/horizontalSampleFactor)*horizontalSampleFactor;
			imageHeight = ((imageHeight + verticalSampleFactor - 1)/verticalSampleFactor)*verticalSampleFactor;	
		}
		
		int samplesPerPixel = 1;
		
		tiffField = ifd.getField(TiffTag.SAMPLES_PER_PIXEL);
		if(tiffField != null) {
			samplesPerPixel = tiffField.getDataAsLong()[0];
		}				
		
		int bitsPerSample = 1;
		
		tiffField = ifd.getField(TiffTag.BITS_PER_SAMPLE);
		if(tiffField != null) {
			bitsPerSample = tiffField.getDataAsLong()[0];
		}
		
		int tileWidth = -1;
		int tileLength = -1;			
		
		TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH);
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH);
		
		if(f_tileWidth != null) {
			tileWidth = f_tileWidth.getDataAsLong()[0];
			tileLength = f_tileLength.getDataAsLong()[0];
		}
		
		int rowsPerStrip = imageHeight;
		int rowWidth = imageWidth;
		
		TiffField<?> f_rowsPerStrip = ifd.getField(TiffTag.ROWS_PER_STRIP);
		if(f_rowsPerStrip != null) rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];					
		
		if(rowsPerStrip > imageHeight) rowsPerStrip = imageHeight;
		
		if(tileWidth > 0) {
			rowsPerStrip = tileLength;
			rowWidth = tileWidth;
		}
		
		int bytesPerRow = ((bitsPerSample*rowWidth*samplesPerPixel + 7)/8);
	
		int planaryConfiguration = 1;
		
		tiffField = ifd.getField(TiffTag.PLANAR_CONFIGURATTION);		
		if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
		
		if(planaryConfiguration == 2) {
			bytesPerRow = (bitsPerSample*rowWidth + 7)/8;
		}
		
		int bytesPerStrip = bytesPerRow*rowsPerStrip;
		
		int[] counts = new int[strips];
		
		if(photoMetric != TiffFieldEnum.PhotoMetric.YCbCr.getValue()) {
			// File the StripByteCounts first
			Arrays.fill(counts, bytesPerStrip);
			// We may need to adjust the last strip in case we are dealing with stripped structure
			if(tileWidth < 0) { // Stripped structure, last strip/strips may be smaller
				int lastStripBytes = bytesPerRow*imageHeight - bytesPerStrip*(strips - 1);
				counts[counts.length - 1] = lastStripBytes;
				// Structure |sample1sample1 ...|sample2sample2 ...| ...
				if(planaryConfiguration == 2) {
					int stripsPerSample = strips/samplesPerPixel;
					lastStripBytes = bytesPerRow*imageHeight - bytesPerStrip*(stripsPerSample - 1);
					if(lastStripBytes > 0) {
						for(int i = 0, stripOffset = stripsPerSample - 1; i < samplesPerPixel; i++) {
							counts[stripOffset] = lastStripBytes;
							stripOffset += stripsPerSample;
						}
					}
				}				
			}
		} else { // Deal with YCbCr down sampling
			if(samplesPerPixel != 3) samplesPerPixel = 3;
			
			int[] sampleBytesPerRow = new int[samplesPerPixel];
			sampleBytesPerRow[0] = (bitsPerSample*rowWidth + 7)/8;
			sampleBytesPerRow[1] = (bitsPerSample*rowWidth/horizontalSampleFactor + 7)/8;
			sampleBytesPerRow[2] = sampleBytesPerRow[1];
			
			int[] sampleRowsPerStrip = new int[samplesPerPixel];
			sampleRowsPerStrip[0] = rowsPerStrip;
			sampleRowsPerStrip[1] = rowsPerStrip/verticalSampleFactor;
			sampleRowsPerStrip[2]= sampleRowsPerStrip[1];
			
			int[] columnHeight = new int[samplesPerPixel];
			columnHeight[0] = imageHeight;
			columnHeight[1] = imageHeight/verticalSampleFactor;
			columnHeight[2] = columnHeight[1];
						
			if(planaryConfiguration == 1) {
				bytesPerStrip = sampleBytesPerRow[0]*sampleRowsPerStrip[0] + sampleBytesPerRow[1]*sampleRowsPerStrip[1] + sampleBytesPerRow[2]*sampleRowsPerStrip[2];
				Arrays.fill(counts, bytesPerStrip);
				if(tileWidth < 0) { // Stripped structure, last strip may be smaller
					int lastStripBytes = (sampleBytesPerRow[0]*columnHeight[0] + sampleBytesPerRow[1]*columnHeight[1] + sampleBytesPerRow[2]*columnHeight[2]) - bytesPerStrip*(strips - 1);
					counts[counts.length - 1] = lastStripBytes;										
				}
			} else { // Separate sample planes -
				int[] sampleBytesPerStrip = new int[samplesPerPixel];
				sampleBytesPerStrip[0] = sampleRowsPerStrip[0]*sampleBytesPerRow[0];
				sampleBytesPerStrip[1] = sampleRowsPerStrip[1]*sampleBytesPerRow[1];
				sampleBytesPerStrip[2] = sampleBytesPerStrip[1];
				
				int stripsPerSample = strips/samplesPerPixel;
				int startOffset = 0;
				int endOffset = stripsPerSample;
				for(int i = 0; i < samplesPerPixel; i++) {
					Arrays.fill(counts, startOffset, endOffset, sampleBytesPerStrip[i]);
					startOffset = endOffset;
					endOffset += stripsPerSample;
				}
				if(tileWidth < 0) { // Stripped structure, last strip may be smaller
					int[] lastStripBytes = new int[samplesPerPixel];
					lastStripBytes[0] = sampleBytesPerRow[0]*columnHeight[0] - sampleBytesPerStrip[0]*(stripsPerSample - 1);
					lastStripBytes[1] = sampleBytesPerRow[1]*columnHeight[1] - sampleBytesPerStrip[1]*(stripsPerSample - 1);
					lastStripBytes[2] = lastStripBytes[1];
					startOffset = stripsPerSample - 1;
					for(int i = 0; i < samplesPerPixel; i++) {
						counts[startOffset] = lastStripBytes[i];
						startOffset += stripsPerSample;
					}
				}
			}			
		}
			
		return counts;
	}
	
	public static void insertExif(RandomAccessInputStream rin, RandomAccessOutputStream rout, Exif exif, boolean update) throws IOException {
		insertExif(rin, rout, exif, 0, update);
	}
	
	/**
	 * Insert EXIF data with optional thumbnail IFD
	 * 
	 * @param rin input image stream
	 * @param rout output image stream
	 * @param exif EXIF wrapper instance
	 * @param pageNumber page offset where to insert EXIF (zero based)
	 * @param update True to keep the original data, otherwise false
	 * @throws Exception
	 */
	public static void insertExif(RandomAccessInputStream rin, RandomAccessOutputStream rout, Exif exif, int pageNumber, boolean update) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD imageIFD = ifds.get(pageNumber);
		IFD exifSubIFD = imageIFD.getChild(TiffTag.EXIF_SUB_IFD);
		IFD gpsSubIFD = imageIFD.getChild(TiffTag.GPS_SUB_IFD);
		IFD newImageIFD = exif.getImageIFD();
		IFD newExifSubIFD = exif.getExifIFD();
		IFD newGpsSubIFD = exif.getGPSIFD();
		
		if(newImageIFD != null) { // Copy the Image IFD fields - this is dangerous.
			imageIFD.addFields(newImageIFD.getFields());
		}
		
		if(update && exifSubIFD != null && newExifSubIFD != null) {
			exifSubIFD.addFields(newExifSubIFD.getFields());
			newExifSubIFD = exifSubIFD;
		}
		
		if(newExifSubIFD != null) {
			imageIFD.addField(new LongField(TiffTag.EXIF_SUB_IFD.getValue(), new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.EXIF_SUB_IFD, newExifSubIFD);		
		}
		
		if(update && gpsSubIFD != null && newGpsSubIFD != null) {
			gpsSubIFD.addFields(newGpsSubIFD.getFields());
			newGpsSubIFD = gpsSubIFD;
		}
		
		if(newGpsSubIFD != null) {
			imageIFD.addField(new LongField(TiffTag.GPS_SUB_IFD.getValue(), new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.GPS_SUB_IFD, newGpsSubIFD);		
		}
		
		int writeOffset = FIRST_WRITE_OFFSET;
		// Copy pages
		writeOffset = copyPages(ifds, writeOffset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();

		writeToStream(rout, firstIFDOffset);
	}
	
	/**
	 * Insert ICC_Profile into TIFF page
	 * 
	 * @param icc_profile byte array holding the ICC_Profile
	 * @param pageNumber page offset where to insert ICC_Profile
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws Exception
	 */
	public static void insertICCProfile(byte[] icc_profile, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		workingPage.addField(new UndefinedField(TiffTag.ICC_PROFILE.getValue(), icc_profile));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertICCProfile(ICC_Profile icc_profile, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertICCProfile(icc_profile.getData(), 0, rin, rout);
	}
	
	/**
	 * Insert ICC_Profile into TIFF page
	 * 
	 * @param icc_profile ICC_Profile
	 * @param pageNumber page number to insert the ICC_Profile
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws Exception
	 */
	public static void insertICCProfile(ICC_Profile icc_profile, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertICCProfile(icc_profile.getData(), pageNumber, rin, rout);
	}
	
	public static void insertIPTC(RandomAccessInputStream rin, RandomAccessOutputStream rout, List<IPTCDataSet> iptcs, boolean update) throws IOException {
		insertIPTC(rin, rout, 0, iptcs, update);
	}
	
	/**
	 * Insert IPTC data into TIFF image. If the original TIFF image contains IPTC data, we either keep
	 * or override them depending on the input parameter "update."
	 * <p>
	 * There is a possibility that IPTC data presents in more than one places such as a normal TIFF
	 * tag, or buried inside a Photoshop IPTC-NAA Image Resource Block (IRB), or even in a XMP block.
	 * Currently this method does the following thing: if no IPTC data was found from both Photoshop or 
	 * normal IPTC tag, we insert the IPTC data with a normal IPTC tag. If IPTC data is found both as
	 * a Photosho tag and a normal IPTC tag, depending on the "update" parameter, we will either delete
	 * the IPTC data from both places and insert the new IPTC data into the Photoshop tag or we will
	 * synchronize the two sets of IPTC data, delete the original IPTC from both places and insert the
	 * synchronized IPTC data along with the new IPTC data into the Photoshop tag. In both cases, we
	 * will keep the other IRBs from the original Photoshop tag unchanged. 
	 * 
	 * @param rin RandomAccessInputStream for the original TIFF
	 * @param rout RandomAccessOutputStream for the output TIFF with IPTC inserted
	 * @param pageNumber page offset where to insert IPTC
	 * @param iptcs A list of IPTCDataSet to insert into the TIFF image
	 * @param update whether we want to keep the original image or create a completely new IPTC data set
	 * @throws IOException
	 */
	public static void insertIPTC(RandomAccessInputStream rin, RandomAccessOutputStream rout, int pageNumber, List<IPTCDataSet> iptcs, boolean update) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
	
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		// See if we also have regular IPTC tag field
		TiffField<?> f_iptc = workingPage.removeField(TiffTag.IPTC);		
		TiffField<?> f_photoshop = workingPage.getField(TiffTag.PHOTOSHOP);
		if(f_photoshop != null) { // Read 8BIMs
			IRB irb = new IRB((byte[])f_photoshop.getData());
			// Shallow copy the map.
			Map<Short, _8BIM> bims = new HashMap<Short, _8BIM>(irb.get8BIM());
			_8BIM photoshop_iptc = bims.remove(ImageResourceID.IPTC_NAA.getValue());
			if(photoshop_iptc != null) { // If we have IPTC
				if(update) { // If we need to keep the old data, copy it
					if(f_iptc != null) {// We are going to synchronize the two IPTC data
						byte[] data = null;
						if(f_iptc.getType() == FieldType.LONG)
							data = ArrayUtils.toByteArray(f_iptc.getDataAsLong(), rin.getEndian() == IOUtils.BIG_ENDIAN);
						else
							data = (byte[])f_iptc.getData();
						copyIPTCDataSet(iptcs, data);
					}
					// Now copy the Photoshop IPTC data
					copyIPTCDataSet(iptcs, photoshop_iptc.getData());
					// Remove duplicates
					iptcs = new ArrayList<IPTCDataSet>(new HashSet<IPTCDataSet>(iptcs));
				}
			}
			// Create IPTC 8BIM
			for(IPTCDataSet dataset : iptcs) {
				dataset.write(bout);
			}
			_8BIM iptc_bim = new _8BIM(ImageResourceID.IPTC_NAA, "iptc", bout.toByteArray());
			bout.reset();
			iptc_bim.write(bout); // Write the IPTC 8BIM first
			for(_8BIM bim : bims.values()) // Copy the other 8BIMs if any
				bim.write(bout);
			// Add a new Photoshop tag field to TIFF
			workingPage.addField(new UndefinedField(TiffTag.PHOTOSHOP.getValue(), bout.toByteArray()));
		} else { // We don't have photoshop, add IPTC to regular IPTC tag field
			if(f_iptc != null && update) {
				byte[] data = null;
				if(f_iptc.getType() == FieldType.LONG)
					data = ArrayUtils.toByteArray(f_iptc.getDataAsLong(), rin.getEndian() == IOUtils.BIG_ENDIAN);
				else
					data = (byte[])f_iptc.getData();
				copyIPTCDataSet(iptcs, data);
			}
			for(IPTCDataSet dataset : iptcs) {
				dataset.write(bout);
			}		
			workingPage.addField(new UndefinedField(TiffTag.IPTC.getValue(), bout.toByteArray()));
		}		
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertIRB(RandomAccessInputStream rin, RandomAccessOutputStream rout, List<_8BIM> bims, boolean update) throws IOException {
		insertIRB(rin, rout, 0, bims, update);
	}
	
	public static void insertIRB(RandomAccessInputStream rin, RandomAccessOutputStream rout, int pageNumber, List<_8BIM> bims, boolean update) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
	
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		if(update) {
			TiffField<?> f_irb = workingPage.getField(TiffTag.PHOTOSHOP);
			if(f_irb != null) {
				IRB irb = new IRB((byte[])f_irb.getData());
				// Shallow copy the map
				Map<Short, _8BIM> bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());
				for(_8BIM bim : bims)
					bimMap.remove(bim.getID());
				bims.addAll(bimMap.values());
			}
		}
		
		for(_8BIM bim : bims)
			bim.write(bout);
		
		workingPage.addField(new UndefinedField(TiffTag.PHOTOSHOP.getValue(), bout.toByteArray()));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	/**
	 * Insert a single page into a TIFF image
	 * 
	 * @param image a BufferedImage to insert
	 * @param pageNumber page number (relative to the existing pages) to insert the page
	 * @param rout RandomAccessOutputStream to write new image
	 * @param ifds a list of IFDs for all the existing and inserted pages
	 * @param writeOffset stream offset to insert this page
	 * @param writer TIFFWriter instance
	 * @throws IOException
	 * 
	 * @return stream offset after inserting this page
	 */
	public static int insertPage(BufferedImage image, int pageNumber, RandomAccessOutputStream rout, List<IFD> ifds, int writeOffset, TIFFWriter writer) throws IOException {
		// Sanity check
		if(pageNumber < 0) pageNumber = 0;
		else if(pageNumber > ifds.size()) pageNumber = ifds.size();		
		
		// Grab image pixels in ARGB format
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		int[] pixels = IMGUtils.getRGB(image);//image.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
		
		try {
			writeOffset = writer.writePage(pixels, pageNumber, ifds.size(), imageWidth, imageHeight, rout, writeOffset);
			ifds.add(pageNumber, writer.getIFD());
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		return writeOffset;
	}
	
	// Insert images into existing TIFF image using default writer parameters
	public static void insertPages(RandomAccessInputStream rin, RandomAccessOutputStream rout, int pageNumber, BufferedImage... images) throws IOException {
		insertPages(rin, rout, pageNumber, null, images);
	}
	
	/**
	 * Insert ImageFrames into existing TIFF image. If knowledge of total pages for the
	 * original image is desirable, call {@link #getPageCount(RandomAccessInputStream) getPageCount}
	 * first.
	 * 
	 * @param rin RandomAccessInputStream for the original image
	 * @param rout RandomAccessOutputStream to write new image
	 * @param pageNumber page offset to start page insertion
	 * @param frames an array of ImageFrame
	 * @throws IOException
	 */	
	public static void insertPages(RandomAccessInputStream rin, RandomAccessOutputStream rout, int pageNumber, ImageFrame ... frames) throws IOException {
		rin.seek(STREAM_HEAD);
		int offset = copyHeader(rin, rout);
		
		List<IFD> list = new ArrayList<IFD>();
		List<IFD> insertedList = new ArrayList<IFD>(frames.length);
		
		// Read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, list, offset, rin);
		
		if(pageNumber < 0) pageNumber = 0;
		else if(pageNumber > list.size()) pageNumber = list.size();
		
		int minPageNumber = pageNumber;
		
		int maxPageNumber = list.size() + frames.length;
		
		int writeOffset = FIRST_WRITE_OFFSET;
		
		TIFFWriter writer = new TIFFWriter(); 
		
		for(int i = 0; i < frames.length; i++) {
			// Retrieve image dimension
			BufferedImage frame = frames[i].getFrame();
			ImageParam param = frames[i].getFrameParam();
			int imageWidth = frame.getWidth();
			int imageHeight = frame.getHeight();
			// Grab image pixels in ARGB format and write image
			int[] pixels = IMGUtils.getRGB(frame);//frame.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
			
			try {
				writer.setImageParam(param);
				writeOffset = writer.writePage(pixels, pageNumber++, maxPageNumber, imageWidth, imageHeight, rout, writeOffset);
				insertedList.add(writer.getIFD());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Reset pageNumber for the existing pages
		for(int i = 0; i < minPageNumber; i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)maxPageNumber}));
		}
		
		for(int i = minPageNumber; i < list.size(); i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)(i + frames.length), (short)maxPageNumber}));
		}
		
		if(list.size() == 1) { // Make the original image one page of the new multiple page TIFF
			if(list.get(0).removeField(TiffTag.SUBFILE_TYPE) == null)
				list.get(0).removeField(TiffTag.NEW_SUBFILE_TYPE);
			list.get(0).addField(new ShortField(TiffTag.SUBFILE_TYPE.getValue(), new short[]{3}));
		}
		
		// Copy pages
		writeOffset = copyPages(list, writeOffset, rin, rout);
		// Re-link the IFDs
		// Internally link inserted IFDs first
		for(int i = 0; i < frames.length - 1; i++) {
			insertedList.get(i).setNextIFDOffset(rout, insertedList.get(i+1).getStartOffset());
		}
		// Link first inserted image IFD with the old previous one
		if(minPageNumber != 0) // Not added at the head
			list.get(minPageNumber - 1).setNextIFDOffset(rout, insertedList.get(0).getStartOffset());
		if(minPageNumber != list.size()) // Link the last inserted image with the old next one
			insertedList.get(insertedList.size() - 1).setNextIFDOffset(rout, list.get(minPageNumber).getStartOffset());
		
		int firstIFDOffset = 0;
			
		if(minPageNumber == 0) {
			firstIFDOffset = insertedList.get(0).getStartOffset();			
		} else {
			firstIFDOffset = list.get(0).getStartOffset();
		}
		
		writeToStream(rout, firstIFDOffset);
	}
	
	/**
	 * Insert pages into a TIFF image. If knowledge of total pages for the
	 * original image is desirable, call {@link #getPageCount(RandomAccessInputStream) getPageCount}
	 * first.
	 * 
	 * @param rin RandomAccessInputStream to read original image
	 * @param rout RandomAccessOutputStream to write new image
	 * @param pageNumber page offset to start page insertion
	 * @param imageParam an array of ImageParam for TIFFWriter
	 * @param images a number of BufferedImage to insert
		
	 * @throws IOException
	 */
	public static void insertPages(RandomAccessInputStream rin, RandomAccessOutputStream rout, int pageNumber, ImageParam[] imageParam, BufferedImage... images) throws IOException {
		rin.seek(STREAM_HEAD);
		int offset = copyHeader(rin, rout);
		
		List<IFD> list = new ArrayList<IFD>();
		List<IFD> insertedList = new ArrayList<IFD>(images.length);
		
		// Read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, list, offset, rin);
		
		if(pageNumber < 0) pageNumber = 0;
		else if(pageNumber > list.size()) pageNumber = list.size();
		
		int minPageNumber = pageNumber;
		
		int maxPageNumber = list.size() + images.length;
		
		int writeOffset = FIRST_WRITE_OFFSET;
		
		ImageParam[] param = null;
		
		if(imageParam == null) {
			param = new ImageParam[images.length];
			Arrays.fill(param, ImageParam.DEFAULT_IMAGE_PARAM);
		} else if(images.length > imageParam.length && imageParam.length > 0) {
				param = new ImageParam[images.length];
				System.arraycopy(imageParam, 0, param, 0, imageParam.length);
				Arrays.fill(param, imageParam.length, images.length, imageParam[imageParam.length - 1]);
		} else {
			param = imageParam;
		}
	
		TIFFWriter writer = new TIFFWriter(); 
		
		for(int i = 0; i < images.length; i++) {
			// Retrieve image dimension
			int imageWidth = images[i].getWidth();
			int imageHeight = images[i].getHeight();
			// Grab image pixels in ARGB format and write image
			int[] pixels = IMGUtils.getRGB(images[i]);//images[i].getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
			
			try {
				writer.setImageParam(param[i]);
				writeOffset = writer.writePage(pixels, pageNumber++, maxPageNumber, imageWidth, imageHeight, rout, writeOffset);
				insertedList.add(writer.getIFD());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Reset pageNumber for the existing pages
		for(int i = 0; i < minPageNumber; i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)maxPageNumber}));
		}
		
		for(int i = minPageNumber; i < list.size(); i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)(i + images.length), (short)maxPageNumber}));
		}
		
		if(list.size() == 1) { // Make the original image one page of the new multiple page TIFF
			if(list.get(0).removeField(TiffTag.SUBFILE_TYPE) == null)
				list.get(0).removeField(TiffTag.NEW_SUBFILE_TYPE);
			list.get(0).addField(new ShortField(TiffTag.SUBFILE_TYPE.getValue(), new short[]{3}));
		}
		
		// Copy pages
		writeOffset = copyPages(list, writeOffset, rin, rout);
		// Re-link the IFDs
		// Internally link inserted IFDs first
		for(int i = 0; i < images.length - 1; i++) {
			insertedList.get(i).setNextIFDOffset(rout, insertedList.get(i+1).getStartOffset());
		}
		// Link first inserted image IFD with the old previous one
		if(minPageNumber != 0) // Not added at the head
			list.get(minPageNumber - 1).setNextIFDOffset(rout, insertedList.get(0).getStartOffset());
		if(minPageNumber != list.size()) // Link the last inserted image with the old next one
			insertedList.get(insertedList.size() - 1).setNextIFDOffset(rout, list.get(minPageNumber).getStartOffset());
		
		int firstIFDOffset = 0;
			
		if(minPageNumber == 0) {
			firstIFDOffset = insertedList.get(0).getStartOffset();			
		} else {
			firstIFDOffset = list.get(0).getStartOffset();
		}
		
		writeToStream(rout, firstIFDOffset);
	}
	
	/**
	 * Insert a thumbnail into PHOTOSHOP private tag field
	 *  
	 * @param rin RandomAccessInputStream for the input TIFF
	 * @param rout RandomAccessOutputStream for the output TIFF
	 * @param thumbnail a BufferedImage to be inserted
	 * @throws Exception
	 */
	public static void insertThumbnail(RandomAccessInputStream rin, RandomAccessOutputStream rout, BufferedImage thumbnail) throws IOException {
		// Sanity check
		if(thumbnail == null) throw new IllegalArgumentException("Input thumbnail is null");
		insertIRB(rin, rout, Arrays.asList(IMGUtils.createThumbnail8BIM(thumbnail)), true);
	}
	
	/**
	 * Insert a TIFF image into another TIFF image or append it to the end of the first image in case the first image
	 * is only one page.
	 * <p>
	 * This method doesn't need to decode the images if SamplesPerPixel value is less than 8 or
	 * SamplesPerPixel & 8 != 0.
	 *
	 * @param original File for the original TIFF image
	 * @param toBeInserted File for the TIFF image to be inserted
	 * @param pageNumber page offset where to insert the TIFF image (zero based)
	 * @param output File for the output TIFF image
	 * 
	 * @throws IOException
	 */
	public static void insertTiffImage(File original, File toBeInserted, int pageNumber, File output) throws IOException {
		// Create FileInputStream for input and output images
		FileInputStream fin1 = new FileInputStream(original);
		FileInputStream fin2 = new FileInputStream(toBeInserted);
		FileOutputStream fout = new FileOutputStream(output);
		// Wrap the FileInputStream and FileOutputStream in the RandomAccessInputStream and RandomAccessOutputStream 
		RandomAccessInputStream rin1 = new FileCacheRandomAccessInputStream(fin1);
		RandomAccessInputStream rin2 = new FileCacheRandomAccessInputStream(fin2);		
		RandomAccessOutputStream rout = new FileCacheRandomAccessOutputStream(fout);
		// Delegate the task
		insertTiffImage(rin1, rin2, pageNumber, rout);
		// Release resources
		rin1.close();
		rin2.close();		
		rout.close();
		// We still need to explicitly close the internal streams 
		fin1.close();
		fin2.close();
		fout.close();
	}
	
	/**
	 * Insert a TIFF image into another TIFF image or append it to the end of the first image in case the first image
	 * is only one page.
	 * <p>
	 * This method doesn't need to decode the images if SamplesPerPixel value is less than 8 or
	 * SamplesPerPixel & 8 != 0.
	 *
	 * @param original RandomAccessInputStream for the original TIFF image
	 * @param toBeInserted RandomAccessInputStream for the TIFF image to be inserted
	 * @param pageNumber offset where to insert the second TIFF image (zero based)
	 * @param output RandomAccessOutputStream for the output TIFF image
	 * 
	 * @throws IOException
	 */
	public static void insertTiffImage(RandomAccessInputStream original, RandomAccessInputStream toBeInserted, int pageNumber, RandomAccessOutputStream output) throws IOException {
		List<IFD> ifds1 = new ArrayList<IFD>();
		int offset1 = copyHeader(original, output);
		// Read IFDs for the first image
		readIFDs(null, null, TiffTag.class, ifds1, offset1, original);
		// Sanity check
		if(pageNumber < 0) pageNumber = 0;
		else if(pageNumber > ifds1.size()) pageNumber = ifds1.size();
		// Remove page number field if any and put in place holder
		for(int i = 0; i < ifds1.size(); i++) {
			ifds1.get(i).removeField(TiffTag.PAGE_NUMBER);
			// Place holder, to be updated afterwards
			ifds1.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{0, 0}));
		}
		int offset = copyPages(ifds1, FIRST_WRITE_OFFSET, original, output);
		short writeEndian = output.getEndian();
		List<IFD> ifds2 = new ArrayList<IFD>();
		readIFDs(ifds2, toBeInserted);
		for(int j = 0; j < ifds2.size(); j++) {
			ifds2.get(j).removeField(TiffTag.PAGE_NUMBER);
			// Place holder, to be updated afterwards
			ifds2.get(j).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{0, 0})); 
		}
		List<IFD> newList = new ArrayList<IFD>(ifds1.size() + ifds2.size());
		short readEndian = toBeInserted.getEndian();
		if(readEndian == writeEndian) // Copy as is
			offset = copyPages(ifds2, offset, toBeInserted, output);
		else {
			// Need to check BitsPerSample to see if we are dealing with images with BitsPerSample > 8
			IFD prevIFD = null;
			for(int j = 0; j < ifds2.size(); j++) {
				IFD currIFD = ifds2.get(j);
				int bitsPerSample = 1; // Default
				TiffField<?> f_bitsPerSample = currIFD.getField(TiffTag.BITS_PER_SAMPLE);
				if(f_bitsPerSample != null) bitsPerSample = f_bitsPerSample.getDataAsLong()[0];
				if(bitsPerSample <= 8) { // Just copy data
					offset = copyPageData(currIFD, offset, toBeInserted, output);							
				} else if(bitsPerSample%8 == 0) {
					/*
					 * TIFF viewers seem to have problem interpreting data with more than 8 BitsPerSample.
					 * Most of the viewers interpret BitsPerSample%8 == 0 according to the  endianess of the image,
					 * but think other bit depth like 12 bits always as big endian. For now we only flip the
					 * endian for BitsPerSample%8 == 0 as needed and leave the other bit depth images as is.
					 */
					// Flip the byte sequence of the data
					ImageDecoder decoder = null;
					ImageEncoder encoder = null;
					// Original image data start from these offsets.
					TiffField<?> stripOffSets = currIFD.removeField(TiffTag.STRIP_OFFSETS);							
					if(stripOffSets == null)
						stripOffSets = currIFD.removeField(TiffTag.TILE_OFFSETS);									
					TiffField<?> stripByteCounts = currIFD.getField(TiffTag.STRIP_BYTE_COUNTS);							
					if(stripByteCounts == null)
						stripByteCounts = currIFD.getField(TiffTag.TILE_BYTE_COUNTS);
					/* 
					 * Make sure this will work in the case when neither STRIP_OFFSETS nor TILE_OFFSETS presents.
					 * Not sure if this will ever happen for TIFF. JPEG EXIF data do not contain these fields. 
					 */
					if(stripOffSets != null) { 
						int[] counts = stripByteCounts.getDataAsLong();		
						int[] off = stripOffSets.getDataAsLong();
						int[] temp = new int[off.length];
								
						int[] uncompressedStripByteCounts = getUncompressedStripByteCounts(currIFD, off.length);
								
						// We are going to write the image data first
						output.seek(offset);
								
						TiffField<?> tiffField = currIFD.getField(TiffTag.COMPRESSION);
						int tiffCompression = 1;
						if(tiffField != null)
							tiffCompression = tiffField.getDataAsLong()[0];
						TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(tiffCompression);
								
						int samplesPerPixel = 1;
						tiffField = currIFD.getField(TiffTag.SAMPLES_PER_PIXEL);								
						if(tiffField != null) samplesPerPixel = tiffField.getDataAsLong()[0];
								
						int planaryConfiguration = 1;
								
						tiffField = currIFD.getField(TiffTag.PLANAR_CONFIGURATTION);		
						if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];										
						
						int scanLineStride = getRowWidth(currIFD);
						if(planaryConfiguration == 1) scanLineStride *= samplesPerPixel;
								
						// Need to uncompress the data, reorder the byte sequence, and compress the data again
						switch(compression) { // Predictor seems to work for LZW, DEFLATE as is! Need more test though!
							case LZW: // Tested
								decoder = new LZWTreeDecoder(8, true);
								encoder = new LZWTreeEncoder(output, 8, 4096, null); // 4K buffer	
								break;
							case DEFLATE:
							case DEFLATE_ADOBE: // Tested
								decoder = new DeflateDecoder();
								encoder = new DeflateEncoder(output, 4096, 4, null); // 4K buffer	
								break;
							case PACKBITS:
								// Not tested
								for(int k = 0; k < off.length; k++) {
									toBeInserted.seek(off[k]);
									byte[] buf = new byte[counts[k]];
									toBeInserted.readFully(buf);
									byte[] buf2 = new byte[uncompressedStripByteCounts[k]];
									Packbits.unpackbits(buf, buf2);
									ArrayUtils.flipEndian(buf2, 0, buf2.length, bitsPerSample, scanLineStride, readEndian == IOUtils.BIG_ENDIAN);
									// Compress the data
									buf2 = new byte[buf.length + (buf.length + 127)/128];
									int bytesCompressed = Packbits.packbits(buf, buf2);
									output.write(buf2, 0, bytesCompressed);
									temp[k] = offset;
									offset += bytesCompressed; // DONE!
								}
								break;
							case NONE:										
								// In case we only have one strip/tile but StripByteCounts contains wrong value
								// If there is only one strip/samplesPerPixel strips for PlanaryConfiguration = 2
								if(planaryConfiguration == 1 && off.length == 1 || planaryConfiguration == 2 && off.length == samplesPerPixel)
								{
									int[] totalBytes2Read = getBytes2Read(currIFD);
										
									for(int k = 0; k < off.length; k++)
										counts[k] = totalBytes2Read[k];					
								}
								// Read the data, reorder the byte sequence and write back the data
								for(int k = 0; k < off.length; k++) {
									toBeInserted.seek(off[k]);
									byte[] buf = new byte[counts[k]];
									toBeInserted.readFully(buf);										
									buf = ArrayUtils.flipEndian(buf, 0, buf.length, bitsPerSample, scanLineStride, readEndian == IOUtils.BIG_ENDIAN);
									output.write(buf);
									temp[k] = offset;
									offset += buf.length;
								}										
								break;
							default: // Fall back to simple copy, at least won't break the whole output image
								for(int l = 0; l < off.length; l++) {
									toBeInserted.seek(off[l]);
									byte[] buf = new byte[counts[l]];
									toBeInserted.readFully(buf);
									output.write(buf);
									temp[l] = offset;
									offset += buf.length;
								}
								break;
						}
						if(decoder != null) {
							for(int k = 0; k < off.length; k++) {
								toBeInserted.seek(off[k]);
								byte[] buf = new byte[counts[k]];
								toBeInserted.readFully(buf);
								decoder.setInput(buf);
								int bytesDecompressed = 0;
								byte[] decompressed = new byte[uncompressedStripByteCounts[k]];
								try {
									bytesDecompressed = decoder.decode(decompressed, 0, uncompressedStripByteCounts[k]);
								} catch (Exception e) {
									e.printStackTrace();
								}
								buf = ArrayUtils.flipEndian(decompressed, 0, bytesDecompressed, bitsPerSample, scanLineStride, readEndian == IOUtils.BIG_ENDIAN);
								// Compress the data
								try {
									encoder.initialize();
									encoder.encode(buf, 0, buf.length);
									encoder.finish();
								} catch (Exception e) {
									e.printStackTrace();
								}
								temp[k] = offset;
								offset += encoder.getCompressedDataLen(); // DONE!
							}
						}
						if(currIFD.getField(TiffTag.STRIP_BYTE_COUNTS) != null)
							stripOffSets = new LongField(TiffTag.STRIP_OFFSETS.getValue(), temp);
						else
							stripOffSets = new LongField(TiffTag.TILE_OFFSETS.getValue(), temp);		
						currIFD.addField(stripOffSets);		
					} else { // Just copy since in this case TIFF viewers tend to think the data is always in TIFF LZW packing format
						offset = copyPageData(currIFD, offset, toBeInserted, output);
					}
					if(prevIFD != null) // Link this IFD with previous one if any
						prevIFD.setNextIFDOffset(output, offset);
					// Then write the IFD
					offset = currIFD.write(output, offset);							
					prevIFD = currIFD;
				}					
			}
		}
		if(pageNumber == 0) {
			ifds2.get(ifds2.size() - 1).setNextIFDOffset(output, ifds1.get(0).getStartOffset());
			newList.addAll(ifds2);
			newList.addAll(ifds1);
		} else {
			if(pageNumber == ifds1.size()) {
				ifds1.get(ifds1.size() - 1).setNextIFDOffset(output, ifds2.get(0).getStartOffset());
				newList.addAll(ifds1);
				newList.addAll(ifds2);
			} else {
				ifds1.get(pageNumber - 1).setNextIFDOffset(output, ifds2.get(0).getStartOffset());
				ifds2.get(ifds2.size() - 1).setNextIFDOffset(output, ifds1.get(pageNumber).getStartOffset());
				newList.addAll(ifds1.subList(0, pageNumber));
				newList.addAll(ifds2);
				newList.addAll(ifds1.subList(pageNumber, ifds1.size()));
			}
		}
		int maxPageNumber = newList.size();
		// Reset pageNumber and total pages
		for(int i = 0; i < maxPageNumber; i++) {
			offset = newList.get(i).getField(TiffTag.PAGE_NUMBER).getDataOffset();
			output.seek(offset);
			output.writeShort((short)i); // Update page number for this page
			output.writeShort((short)maxPageNumber); // Update total page number
		}			
		// Figure out the first IFD offset
		int firstIFDOffset = newList.get(0).getStartOffset();
		// And write the IFDs
		writeToStream(output, firstIFDOffset); // DONE!	
	}
	
	public static void insertXMP(byte[] xmp, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertXMP(xmp, 0, rin, rout);
	}
	
	/**
	 * Insert XMP data into TIFF image
	 * @param xmp byte array for the XMP data to be inserted
	 * @param pageNumber page offset where to insert XMP
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws IOException
	 */
	public static void insertXMP(byte[] xmp, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		workingPage.addField(new UndefinedField(TiffTag.XMP.getValue(), xmp));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertXMP(String xmp, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		Document doc = XMLUtils.createXML(xmp);
		XMLUtils.insertLeadingPI(doc, "xpacket", "begin='' id='W5M0MpCehiHzreSzNTczkc9d'");
		XMLUtils.insertTrailingPI(doc, "xpacket", "end='w'");
		byte[] xmpBytes = XMLUtils.serializeToByteArray(doc);
		insertXMP(xmpBytes, rin, rout);
	}
	
	/**
	 * Merges two TIFF images together
	 * <p>
	 * Note: this method works for all TIFF images with BitsPerSample <= 8.
	 * For BitsPerSample > 8, the result is unpredictable: if the image is
	 * the first one in the list to be merged, or the endianness of the image
	 * is the same as the merged image, it works fine, otherwise, the merged
	 * image with BitsPerSample > 8 will contain wrong image data.
	 * 
	 * @param image1 RandomAccessInputStream for the first TIFF image
	 * @param image2 RandomAccessInputStream for the second TIFF image
	 * @param merged RanomAccessOutputStream for the merged TIFF image
	 * @throws IOException
	 */
	public static void mergeTiffImages(RandomAccessInputStream image1, RandomAccessInputStream image2, RandomAccessOutputStream merged) throws IOException {
		int offset1 = copyHeader(image1, merged);
		int offset2 = readHeader(image2);
		// Read IFDs
		List<IFD> ifds1 = new ArrayList<IFD>();
		List<IFD> ifds2 = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds1, offset1, image1);
		readIFDs(null, null, TiffTag.class, ifds2, offset2, image2);
		int maxPageNumber = ifds1.size() + ifds2.size();
		// Reset pageNumber
		for(int i = 0; i < ifds1.size(); i++) {
			ifds1.get(i).removeField(TiffTag.PAGE_NUMBER);
			ifds1.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)maxPageNumber}));
		}
		for(int i = 0; i < ifds2.size(); i++) {
			ifds2.get(i).removeField(TiffTag.PAGE_NUMBER);
			ifds2.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)(i+ifds1.size()), (short)maxPageNumber}));
		}		
		int offset = copyPages(ifds1, FIRST_WRITE_OFFSET, image1, merged);
		offset = copyPages(ifds2, offset, image2, merged);
		// Link the two IFDs
		ifds1.get(ifds1.size() - 1).setNextIFDOffset(merged, ifds2.get(0).getStartOffset());
		// Figure out the first IFD offset
		int firstIFDOffset = ifds1.get(0).getStartOffset();
		// And write the IFDs
		writeToStream(merged, firstIFDOffset); // DONE!
	}
	
	/**
	 * Merges a list of TIFF images (single or multiple page) into one. 
	 * <p>
	 * Note: this method works for all TIFF images with BitsPerSample <= 8.
	 * For BitsPerSample > 8, the result is unpredictable: if the image is
	 * the first one in the list to be merged, or the endianness of the image
	 * is the same as the merged image, it works fine, otherwise, the merged
	 * image with BitsPerSample > 8 will contain wrong image data.  
	 *  
	 * @param merged RandomAccessOutputStream for the merged TIFF
	 * @param images input TIFF image files to be merged
	 * @throws IOException
	 */
	public static void mergeTiffImages(RandomAccessOutputStream merged, File... images) throws IOException {
		if(images != null && images.length > 1) {
			FileInputStream fis1 = new FileInputStream(images[0]);
			RandomAccessInputStream image1 = new FileCacheRandomAccessInputStream(fis1);
			List<IFD> ifds1 = new ArrayList<IFD>();
			int offset1 = copyHeader(image1, merged);
			// Read IFDs for the first image
			readIFDs(null, null, TiffTag.class, ifds1, offset1, image1);
			for(int i = 0; i < ifds1.size(); i++) {
				ifds1.get(i).removeField(TiffTag.PAGE_NUMBER);
				// Place holder, to be updated afterwards
				ifds1.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{0, 0}));
			}
			int offset = copyPages(ifds1, FIRST_WRITE_OFFSET, image1, merged);
			// Release resources
			image1.close();
			fis1.close();
			for(int i = 1; i < images.length; i++) {
				List<IFD> ifds2 = new ArrayList<IFD>();
				FileInputStream fis2 = new FileInputStream(images[i]);
				RandomAccessInputStream image2 = new FileCacheRandomAccessInputStream(fis2); 
				readIFDs(ifds2, image2);
				for(int j = 0; j < ifds2.size(); j++) {
					ifds2.get(j).removeField(TiffTag.PAGE_NUMBER);
					// Place holder, to be updated afterwards
					ifds2.get(j).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{0, 0})); 
				}
				offset = copyPages(ifds2, offset, image2, merged);
				// Link the two IFDs
				ifds1.get(ifds1.size() - 1).setNextIFDOffset(merged, ifds2.get(0).getStartOffset());
				ifds1.addAll(ifds2);
				// Release resources
				image2.close();
				fis2.close();
			}
			int maxPageNumber = ifds1.size();
			// Reset pageNumber and total pages
			for(int i = 0; i < ifds1.size(); i++) {
				offset = ifds1.get(i).getField(TiffTag.PAGE_NUMBER).getDataOffset();
				merged.seek(offset);
				merged.writeShort((short)i); // Update page number for this page
				merged.writeShort((short)maxPageNumber); // Update total page number
			}			
			// Figure out the first IFD offset
			int firstIFDOffset = ifds1.get(0).getStartOffset();
			// And write the IFDs
			writeToStream(merged, firstIFDOffset); // DONE!
		}
	}
	
	/**
	 * Merges a list of TIFF images into one regardless of the original bit depth
	 * 
	 * @param merged RandomAccessOutputStream for the merged TIFF
	 * @param images input TIFF image files to be merged
	 * @throws IOException
	 */
	public static void mergeTiffImagesEx(RandomAccessOutputStream merged, File... images) throws IOException {
		if(images != null && images.length > 1) {
			FileInputStream fis1 = new FileInputStream(images[0]);
			RandomAccessInputStream image1 = new FileCacheRandomAccessInputStream(fis1);
			List<IFD> ifds1 = new ArrayList<IFD>();
			int offset1 = copyHeader(image1, merged);
			// Read IFDs for the first image
			readIFDs(null, null, TiffTag.class, ifds1, offset1, image1);
			for(int i = 0; i < ifds1.size(); i++) {
				ifds1.get(i).removeField(TiffTag.PAGE_NUMBER);
				// Place holder, to be updated afterwards
				ifds1.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{0, 0}));
			}
			int offset = copyPages(ifds1, FIRST_WRITE_OFFSET, image1, merged);
			// Release resources
			image1.close();
			fis1.close();
			short writeEndian = merged.getEndian();
			for(int i = 1; i < images.length; i++) {
				List<IFD> ifds2 = new ArrayList<IFD>();
				FileInputStream fis2 = new FileInputStream(images[i]);
				RandomAccessInputStream image2 = new FileCacheRandomAccessInputStream(fis2); 
				readIFDs(ifds2, image2);
				for(int j = 0; j < ifds2.size(); j++) {
					ifds2.get(j).removeField(TiffTag.PAGE_NUMBER);
					// Place holder, to be updated afterwards
					ifds2.get(j).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{0, 0})); 
				}
				short readEndian = image2.getEndian();
				if(readEndian == writeEndian) // Copy as is
					offset = copyPages(ifds2, offset, image2, merged);
				else {
					// Need to check BitsPerSample to see if we are dealing with images with BitsPerSample > 8
					IFD prevIFD = null;
					for(int j = 0; j < ifds2.size(); j++) {
						IFD currIFD = ifds2.get(j);
						int bitsPerSample = 1; // Default
						TiffField<?> f_bitsPerSample = currIFD.getField(TiffTag.BITS_PER_SAMPLE);
						if(f_bitsPerSample != null) bitsPerSample = f_bitsPerSample.getDataAsLong()[0];
						if(bitsPerSample <= 8) { // Just copy data
							offset = copyPageData(currIFD, offset, image2, merged);							
						} else if(bitsPerSample%8 == 0) {
							/*
							 * TIFF viewers seem to have problem interpreting data with more than 8 BitsPerSample.
							 * Most of the viewers interpret BitsPerSample%8 == 0 according to the  endianess of the image,
							 * but think other bit depth like 12 bits always as big endian. For now we only flip the
							 * endian for BitsPerSample%8 == 0 as needed and leave the other bit depth images as is.
							 */
							// Flip the byte sequence of the data
							ImageDecoder decoder = null;
							ImageEncoder encoder = null;
							// Original image data start from these offsets.
							TiffField<?> stripOffSets = currIFD.removeField(TiffTag.STRIP_OFFSETS);							
							if(stripOffSets == null)
								stripOffSets = currIFD.removeField(TiffTag.TILE_OFFSETS);									
							TiffField<?> stripByteCounts = currIFD.getField(TiffTag.STRIP_BYTE_COUNTS);							
							if(stripByteCounts == null)
								stripByteCounts = currIFD.getField(TiffTag.TILE_BYTE_COUNTS);
							/* 
							 * Make sure this will work in the case when neither STRIP_OFFSETS nor TILE_OFFSETS presents.
							 * Not sure if this will ever happen for TIFF. JPEG EXIF data do not contain these fields. 
							 */
							if(stripOffSets != null) { 
								int[] counts = stripByteCounts.getDataAsLong();		
								int[] off = stripOffSets.getDataAsLong();
								int[] temp = new int[off.length];
								
								int[] uncompressedStripByteCounts = getUncompressedStripByteCounts(currIFD, off.length);
								
								// We are going to write the image data first
								merged.seek(offset);
								
								TiffField<?> tiffField = currIFD.getField(TiffTag.COMPRESSION);
								int tiffCompression = 1;
								if(tiffField != null)
									tiffCompression = tiffField.getDataAsLong()[0];
								TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(tiffCompression);
								
								int samplesPerPixel = 1;
								tiffField = currIFD.getField(TiffTag.SAMPLES_PER_PIXEL);								
								if(tiffField != null) samplesPerPixel = tiffField.getDataAsLong()[0];
								
								int planaryConfiguration = 1;
								
								tiffField = currIFD.getField(TiffTag.PLANAR_CONFIGURATTION);		
								if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];										
								
								int scanLineStride = getRowWidth(currIFD);
								if(planaryConfiguration == 1) scanLineStride *= samplesPerPixel;
								
								// Need to uncompress the data, reorder the byte sequence, and compress the data again
								switch(compression) { // Predictor seems to work for LZW, DEFLATE as is! Need more test though!
									case LZW: // Tested
										decoder = new LZWTreeDecoder(8, true);
										encoder = new LZWTreeEncoder(merged, 8, 4096, null); // 4K buffer	
										break;
									case DEFLATE:
									case DEFLATE_ADOBE: // Tested
										decoder = new DeflateDecoder();
										encoder = new DeflateEncoder(merged, 4096, 4, null); // 4K buffer	
										break;
									case PACKBITS:
										// Not tested
										for(int k = 0; k < off.length; k++) {
											image2.seek(off[k]);
											byte[] buf = new byte[counts[k]];
											image2.readFully(buf);
											byte[] buf2 = new byte[uncompressedStripByteCounts[k]];
											Packbits.unpackbits(buf, buf2);
											ArrayUtils.flipEndian(buf2, 0, buf2.length, bitsPerSample, scanLineStride, readEndian == IOUtils.BIG_ENDIAN);
											// Compress the data
											buf2 = new byte[buf.length + (buf.length + 127)/128];
											int bytesCompressed = Packbits.packbits(buf, buf2);
											merged.write(buf2, 0, bytesCompressed);
											temp[k] = offset;
											offset += bytesCompressed; // DONE!
										}
										break;
									case NONE:										
										// In case we only have one strip/tile but StripByteCounts contains wrong value
										// If there is only one strip/samplesPerPixel strips for PlanaryConfiguration = 2
										if(planaryConfiguration == 1 && off.length == 1 || planaryConfiguration == 2 && off.length == samplesPerPixel)
										{
											int[] totalBytes2Read = getBytes2Read(currIFD);
										
											for(int k = 0; k < off.length; k++)
												counts[k] = totalBytes2Read[k];					
										}
										// Read the data, reorder the byte sequence and write back the data
										for(int k = 0; k < off.length; k++) {
											image2.seek(off[k]);
											byte[] buf = new byte[counts[k]];
											image2.readFully(buf);										
											buf = ArrayUtils.flipEndian(buf, 0, buf.length, bitsPerSample, scanLineStride, readEndian == IOUtils.BIG_ENDIAN);
											merged.write(buf);
											temp[k] = offset;
											offset += buf.length;
										}										
										break;
									default: // Fall back to simple copy, at least won't break the whole merged image
										for(int l = 0; l < off.length; l++) {
											image2.seek(off[l]);
											byte[] buf = new byte[counts[l]];
											image2.readFully(buf);
											merged.write(buf);
											temp[l] = offset;
											offset += buf.length;
										}
										break;								
								}
								if(decoder != null) {
									for(int k = 0; k < off.length; k++) {
										image2.seek(off[k]);
										byte[] buf = new byte[counts[k]];
										image2.readFully(buf);
										decoder.setInput(buf);
										int bytesDecompressed = 0;
										byte[] decompressed = new byte[uncompressedStripByteCounts[k]];
										try {
											bytesDecompressed = decoder.decode(decompressed, 0, uncompressedStripByteCounts[k]);
										} catch (Exception e) {
											e.printStackTrace();
										}
										buf = ArrayUtils.flipEndian(decompressed, 0, bytesDecompressed, bitsPerSample, scanLineStride, readEndian == IOUtils.BIG_ENDIAN);
										// Compress the data
										try {
											encoder.initialize();
											encoder.encode(buf, 0, buf.length);
											encoder.finish();
										} catch (Exception e) {
											e.printStackTrace();
										}
										temp[k] = offset;
										offset += encoder.getCompressedDataLen(); // DONE!
									}
								}
								if(currIFD.getField(TiffTag.STRIP_BYTE_COUNTS) != null)
									stripOffSets = new LongField(TiffTag.STRIP_OFFSETS.getValue(), temp);
								else
									stripOffSets = new LongField(TiffTag.TILE_OFFSETS.getValue(), temp);		
								currIFD.addField(stripOffSets);		
							}
						} else { // Just copy since in this case TIFF viewers tend to think the data is always in TIFF LZW packing format
							offset = copyPageData(currIFD, offset, image2, merged);
						}
						if(prevIFD != null) // Link this IFD with previous one if any
							prevIFD.setNextIFDOffset(merged, offset);
						// Then write the IFD
						offset = currIFD.write(merged, offset);							
						prevIFD = currIFD;
					}					
				}
				// Link the two IFDs
				ifds1.get(ifds1.size() - 1).setNextIFDOffset(merged, ifds2.get(0).getStartOffset());
				ifds1.addAll(ifds2);
				// Release resources
				image2.close();
				fis2.close();
			}
			int maxPageNumber = ifds1.size();
			// Reset pageNumber and total pages
			for(int i = 0; i < ifds1.size(); i++) {
				offset = ifds1.get(i).getField(TiffTag.PAGE_NUMBER).getDataOffset();
				merged.seek(offset);
				merged.writeShort((short)i); // Update page number for this page
				merged.writeShort((short)maxPageNumber); // Update total page number
			}			
			// Figure out the first IFD offset
			int firstIFDOffset = ifds1.get(0).getStartOffset();
			// And write the IFDs
			writeToStream(merged, firstIFDOffset); // DONE!
		}
	}
	
	public static int prepareForInsert(RandomAccessInputStream rin, RandomAccessOutputStream rout, List<IFD> ifds) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		if(ifds.size() == 1) { // Make the original image one page of the new multiple page TIFF
			if(ifds.get(0).removeField(TiffTag.SUBFILE_TYPE) == null)
				ifds.get(0).removeField(TiffTag.NEW_SUBFILE_TYPE);
			ifds.get(0).addField(new ShortField(TiffTag.SUBFILE_TYPE.getValue(), new short[]{3}));
		}		
		for(int i = 0; i < ifds.size(); i++) {
			ifds.get(i).removeField(TiffTag.PAGE_NUMBER);
			// Place holder, to be updated later
			ifds.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{0, 0}));
		}
		int writeOffset = FIRST_WRITE_OFFSET;
		// Copy pages
		writeOffset = copyPages(ifds, writeOffset, rin, rout);
		
		return writeOffset;
	}
	
	public static void printIFDs(Collection<IFD> list, String indent) {
		int id = 0;
		System.out.print(indent);
		for(IFD currIFD : list) {
			System.out.println("IFD #" + id);
			printIFD(currIFD, TiffTag.class, indent);
			id++;
		}
	}
	
	public static void printIFD(IFD currIFD, Class<? extends Tag> tagClass, String indent) {
		// Use reflection to invoke fromShort(short) method
		Method method = null;
		try {
			method = tagClass.getDeclaredMethod("fromShort", short.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		Collection<TiffField<?>> fields = currIFD.getFields();
		int i = 0;
		for(TiffField<?> field : fields) {
			System.out.print(indent);
			System.out.println("Field #" + i);
			System.out.print(indent);
			short tag = field.getTag();
			Tag ftag = TiffTag.UNKNOWN;
			try {
				ftag = (Tag)method.invoke(null, tag);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			if (ftag == TiffTag.UNKNOWN) {
				System.out.println("Tag: " + ftag + " [Value: 0x"+ Integer.toHexString(tag&0xffff) + "]" + " (Unknown)");
			} else {
				System.out.println("Tag: " + ftag);
			}
			FieldType ftype = field.getType();				
			System.out.print(indent);
			System.out.println("Field type: " + ftype);
			int field_length = field.getLength();
			System.out.print(indent);
			System.out.println("Field length: " + field_length);
			System.out.print(indent);			
			
			String suffix = null;
			if(ftype == FieldType.SHORT || ftype == FieldType.SSHORT)
				suffix = ftag.getFieldAsString(field.getDataAsLong());
			else
				suffix = ftag.getFieldAsString(field.getData());			
			
			System.out.println("Field value: " + field.getDataAsString() + (StringUtils.isNullOrEmpty(suffix)?"":" => " + suffix));
			
			i++;
		}
		
		Map<Tag, IFD> children = currIFD.getChildren();
		
		if(children.get(TiffTag.EXIF_SUB_IFD) != null) {
			System.out.print(indent + "--------- ");
			System.out.println("<<Exif SubIFD starts>>");
			printIFD(children.get(TiffTag.EXIF_SUB_IFD), ExifTag.class, indent + "--------- ");
			System.out.print(indent + "--------- ");
			System.out.println("<<Exif SubIFD ends>>");
		}
		
		if(children.get(TiffTag.GPS_SUB_IFD) != null) {
			System.out.print(indent + "--------- ");
			System.out.println("<<GPS SubIFD starts>>");
			printIFD(children.get(TiffTag.GPS_SUB_IFD), GPSTag.class, indent + "--------- ");
			System.out.print(indent + "--------- ");
			System.out.println("<<GPS SubIFD ends>>");
		}		
	}
	
	private static int readHeader(RandomAccessInputStream rin) throws IOException {
		int offset = 0;
	    // First 2 bytes determine the byte order of the file
		rin.seek(STREAM_HEAD);
	    short endian = rin.readShort();
	    offset += 2;
	
		if (endian == IOUtils.BIG_ENDIAN) {
		    //Byte order: Motorola BIG_ENDIAN
		    rin.setReadStrategy(ReadStrategyMM.getInstance());
		} else if(endian == IOUtils.LITTLE_ENDIAN) {
		    // Byte order: Intel LITTLE_ENDIAN"
		    rin.setReadStrategy(ReadStrategyII.getInstance());
		} else {		
			rin.close();
			throw new RuntimeException("Invalid TIFF byte order");
	    }		
		// Read TIFF identifier
		rin.seek(offset);
		short tiff_id = rin.readShort();
		offset +=2;
		
		if(tiff_id!=0x2a) { //"*" 42 decimal
			rin.close();
			throw new RuntimeException("Invalid TIFF identifier");
		}
		
		rin.seek(offset);
		offset = rin.readInt();
			
		return offset;
	}
	
	private static int readIFD(IFD parent, Tag parentTag, Class<? extends Tag> tagClass, RandomAccessInputStream rin, List<IFD> list, int offset) throws IOException	{	
		// Use reflection to invoke fromShort(short) method
		Method method = null;
		try {
			method = tagClass.getDeclaredMethod("fromShort", short.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("The static method 'fromShort' doesn't exist");
		} catch (SecurityException e) {
			throw new RuntimeException("Current security doesn't allow this operation");
		}
		IFD tiffIFD = new IFD();
		rin.seek(offset);
		int no_of_fields = rin.readShort();
		offset += 2;
		
		for (int i = 0; i < no_of_fields; i++) {
			rin.seek(offset);
			short tag = rin.readShort();
			Tag ftag = TiffTag.UNKNOWN;
			try {
				ftag = (Tag)method.invoke(null, tag);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			offset += 2;
			rin.seek(offset);
			short type = rin.readShort();
			FieldType ftype = FieldType.fromShort(type);
			offset += 2;
			rin.seek(offset);
			int field_length = rin.readInt();
			offset += 4;
			////// Try to read actual data.
			switch (ftype) {
				case BYTE:
				case UNDEFINED:
					byte[] data = new byte[field_length];
					rin.seek(offset);
					if(field_length <= 4) {						
						rin.readFully(data, 0, field_length);					   
					} else {
						rin.seek(rin.readInt());
						rin.readFully(data, 0, field_length);
					}					
					TiffField<byte[]> byteField = null;
					if(ftype == FieldType.BYTE)
						byteField = new ByteField(tag, data);
					else
						byteField = new UndefinedField(tag, data);
					tiffIFD.addField(byteField);
					offset += 4;					
					break;
				case ASCII:
					data = new byte[field_length];
					if(field_length <= 4) {
						rin.seek(offset);
						rin.readFully(data, 0, field_length);
					}						
					else {
						rin.seek(offset);
						rin.seek(rin.readInt());
						rin.readFully(data, 0, field_length);
					}
					TiffField<String> ascIIField = new ASCIIField(tag, new String(data, 0, data.length, "UTF-8"));
					tiffIFD.addField(ascIIField);
					offset += 4;	
					break;
				case SHORT:
					short[] sdata = new short[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  sdata[0] = rin.readShort();
					  offset += 4;
					} else if (field_length == 2) {
						rin.seek(offset);
						sdata[0] = rin.readShort();
						offset += 2;
						rin.seek(offset);
						sdata[1] = rin.readShort();
						offset += 2;
					} else {
						rin.seek(offset);
						int toOffset = rin.readInt();
						offset += 4;
						for (int j = 0; j  <field_length; j++) {
							rin.seek(toOffset);
							sdata[j] = rin.readShort();
							toOffset += 2;
						}
					}
					TiffField<short[]> shortField = new ShortField(tag, sdata);
					tiffIFD.addField(shortField);
					break;
				case LONG:
					int[] ldata = new int[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  ldata[0] = rin.readInt();
					  offset += 4;
					} else {
						rin.seek(offset);
						int toOffset = rin.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							rin.seek(toOffset);
							ldata[j] = rin.readInt();
							toOffset += 4;
						}
					}
					TiffField<int[]> longField = new LongField(tag, ldata);
					tiffIFD.addField(longField);
					
					if ((ftag == TiffTag.EXIF_SUB_IFD) && (ldata[0]!= 0)) {
						try { // If something bad happens, we skip the sub IFD
							readIFD(tiffIFD, TiffTag.EXIF_SUB_IFD, ExifTag.class, rin, null, ldata[0]);
						} catch(Exception e) {
							tiffIFD.removeField(TiffTag.EXIF_SUB_IFD);
							e.printStackTrace();
						}
					} else if ((ftag == TiffTag.GPS_SUB_IFD) && (ldata[0] != 0)) {
						try {
							readIFD(tiffIFD, TiffTag.GPS_SUB_IFD, GPSTag.class, rin, null, ldata[0]);
						} catch(Exception e) {
							tiffIFD.removeField(TiffTag.GPS_SUB_IFD);
							e.printStackTrace();
						}
					} else if((ftag == ExifTag.EXIF_INTEROPERABILITY_OFFSET) && (ldata[0] != 0)) {
						try {
							readIFD(tiffIFD, ExifTag.EXIF_INTEROPERABILITY_OFFSET, InteropTag.class, rin, null, ldata[0]);
						} catch(Exception e) {
							tiffIFD.removeField(ExifTag.EXIF_INTEROPERABILITY_OFFSET);
							e.printStackTrace();
						}
					} else if (ftag == TiffTag.SUB_IFDS) {						
						for(int ifd = 0; ifd < ldata.length; ifd++) {
							try {
								readIFD(tiffIFD, TiffTag.SUB_IFDS, TiffTag.class, rin, null, ldata[0]);
							} catch(Exception e) {
								tiffIFD.removeField(TiffTag.SUB_IFDS);
								e.printStackTrace();
							}
						}
					}				
					break;
				case FLOAT:
					float[] fdata = new float[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  fdata[0] = rin.readFloat();
					  offset += 4;
					} else {
						rin.seek(offset);
						int toOffset = rin.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++) {
							rin.seek(toOffset);
							fdata[j] = rin.readFloat();
							toOffset += 4;
						}
					}
					TiffField<float[]> floatField = new FloatField(tag, fdata);
					tiffIFD.addField(floatField);
					
					break;
				case DOUBLE:
					double[] ddata = new double[field_length];
					rin.seek(offset);
					int toOffset = rin.readInt();
					offset += 4;
					for (int j=0;j<field_length; j++) {
						rin.seek(toOffset);
						ddata[j] = rin.readDouble();
						toOffset += 8;
					}
					TiffField<double[]> doubleField = new DoubleField(tag, ddata);
					tiffIFD.addField(doubleField);
					
					break;
				case RATIONAL:
				case SRATIONAL:
					int len = 2*field_length;
					ldata = new int[len];	
					rin.seek(offset);
					toOffset = rin.readInt();
					offset += 4;					
					for (int j=0;j<len; j+=2){
						rin.seek(toOffset);
						ldata[j] = rin.readInt();
						toOffset += 4;
						rin.seek(toOffset);
						ldata[j+1] = rin.readInt();
						toOffset += 4;
					}
					TiffField<int[]> rationalField = null;
					if(ftype == FieldType.SRATIONAL) {
						rationalField = new SRationalField(tag, ldata);
					} else {
						rationalField = new RationalField(tag, ldata);
					}
					tiffIFD.addField(rationalField);
					
					break;
				case IFD:
					ldata = new int[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  ldata[0] = rin.readInt();
					  offset += 4;
					} else {
						rin.seek(offset);
						toOffset = rin.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++) {
							rin.seek(toOffset);
							ldata[j] = rin.readInt();
							toOffset += 4;
						}
					}
					TiffField<int[]> ifdField = new IFDField(tag, ldata);
					tiffIFD.addField(ifdField);
					for(int ifd = 0; ifd < ldata.length; ifd++) {
						readIFD(tiffIFD, TiffTag.SUB_IFDS, TiffTag.class, rin, null, ldata[0]);
					}
								
					break;
				default:
					offset += 4;
					break;					
			}
		}
		// If this is a child IFD, add it to its parent
		if(parent != null)
			parent.addChild(parentTag, tiffIFD);
		else // Otherwise, add to the main IFD list
			list.add(tiffIFD);
		rin.seek(offset);
		
		return rin.readInt();
	}
	
	private static void readIFDs(IFD parent, Tag parentTag, Class<? extends Tag> tagClass, List<IFD> list, int offset, RandomAccessInputStream rin) throws IOException {
		// Read the IFDs into a list first	
		while (offset != 0) {
			offset = readIFD(parent, parentTag, tagClass, rin, list, offset);
		}
	}
	
	public static void readIFDs(List<IFD> list, RandomAccessInputStream rin) throws IOException {
		int offset = readHeader(rin);
		readIFDs(null, null, TiffTag.class, list, offset, rin);
	}
	
	public static Map<MetadataType, Metadata> readMetadata(RandomAccessInputStream rin) throws IOException {
		return readMetadata(rin, 0);
	}
	
	public static Map<MetadataType, Metadata> readMetadata(RandomAccessInputStream rin, int pageNumber) throws IOException	{
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		int offset = readHeader(rin);
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD currIFD = ifds.get(pageNumber);
		TiffField<?> field = currIFD.getField(TiffTag.ICC_PROFILE); 
		if(field != null) { // We have found ICC_Profile
			metadataMap.put(MetadataType.ICC_PROFILE, new ICCProfile((byte[])field.getData()));
		}
		field = currIFD.getField(TiffTag.XMP);
		if(field != null) { // We have found XMP
			metadataMap.put(MetadataType.XMP, new XMP((byte[])field.getData()));
		}
		field = currIFD.getField(TiffTag.PHOTOSHOP);
		if(field != null) { // We have found Photoshop IRB
			IRB irb = new IRB((byte[])field.getData());
			metadataMap.put(MetadataType.PHOTOSHOP, irb);
			_8BIM photoshop_8bim = irb.get8BIM(ImageResourceID.IPTC_NAA.getValue());
			if(photoshop_8bim != null) { // If we have IPTC data inside Photoshop, keep it
				IPTC iptc = new IPTC(photoshop_8bim.getData());
				metadataMap.put(MetadataType.IPTC, iptc);
			}
		}
		field = currIFD.getField(TiffTag.IPTC);
		if(field != null) { // We have found IPTC data
			// See if we already have IPTC data from IRB
			IPTC iptc = (IPTC)(metadataMap.get(MetadataType.IPTC));
			byte[] iptcData = null;
			FieldType type = field.getType();
			if(type == FieldType.LONG)
				iptcData = ArrayUtils.toByteArray(field.getDataAsLong(), rin.getEndian() == IOUtils.BIG_ENDIAN);		
			else
				iptcData = (byte[])field.getData();			
			if(iptc != null) // If we have IPTC data from IRB, consolidate it with the current data
				iptcData = ArrayUtils.concat(iptcData, iptc.getData());
			metadataMap.put(MetadataType.IPTC, new IPTC(iptcData));
		}
		field = currIFD.getField(TiffTag.EXIF_SUB_IFD);
		if(field != null) { // We have found EXIF SubIFD
			metadataMap.put(MetadataType.EXIF, new TiffExif(currIFD));
		}
		
		return metadataMap;
	}
	
	public static void removeMetadata(int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout, MetadataType ... metadataTypes) throws IOException {
		removeMetadata(new HashSet<MetadataType>(Arrays.asList(metadataTypes)), pageNumber, rin, rout);
	}
	
	public static void removeMetadata(RandomAccessInputStream rin, RandomAccessOutputStream rout, MetadataType ... metadataTypes) throws IOException {
		removeMetadata(0, rin, rout, metadataTypes);
	}
	
	/**
	 * Remove meta data from TIFF image
	 * 
	 * @param pageNumber working page from which to remove EXIF and GPS data
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws IOException
	 */
	public static void removeMetadata(Set<MetadataType> metadataTypes, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
	
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		
		TiffField<?> metadata = null;
		
		for(MetadataType metaType : metadataTypes) {
			switch(metaType) {
				case XMP:
					workingPage.removeField(TiffTag.XMP);
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove XMP and keep the other IRB data untouched.
						removeMetadataFromIRB(workingPage, data, ImageResourceID.XMP_METADATA);
					}
					break;
				case IPTC:
					workingPage.removeField(TiffTag.IPTC);
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove IPTC_NAA and keep the other IRB data untouched.
						removeMetadataFromIRB(workingPage, data, ImageResourceID.IPTC_NAA);
					}
					break;
				case ICC_PROFILE:
					workingPage.removeField(TiffTag.ICC_PROFILE);
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove ICC_PROFILE and keep the other IRB data untouched.
						removeMetadataFromIRB(workingPage, data, ImageResourceID.ICC_PROFILE);
					}
					break;
				case PHOTOSHOP:
					workingPage.removeField(TiffTag.PHOTOSHOP);
					break;
				case EXIF:
					workingPage.removeField(TiffTag.EXIF_SUB_IFD);
					workingPage.removeField(TiffTag.GPS_SUB_IFD);
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove EXIF and keep the other IRB data untouched.
						removeMetadataFromIRB(workingPage, data, ImageResourceID.EXIF_DATA1, ImageResourceID.EXIF_DATA3);
					}
					break;
				default:
			}
		}
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);		
	}
	
	public static void removeMetadata(Set<MetadataType> metadataTypes, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		removeMetadata(metadataTypes, 0, rin, rout);
	}
	
	private static void removeMetadataFromIRB(IFD workingPage, byte[] data, ImageResourceID ... ids) throws IOException {
		IRB irb = new IRB(data);
		// Shallow copy the map.
		Map<Short, _8BIM> bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());								
		// We only remove XMP and keep the other IRB data untouched.
		for(ImageResourceID id : ids)
			bimMap.remove(id.getValue());
		if(bimMap.size() > 0) {
		   	// Write back the IRB
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			for(_8BIM bim : bimMap.values())
				bim.write(bout);
			// Add new PHOTOSHOP field
			workingPage.addField(new ByteField(TiffTag.PHOTOSHOP.getValue(), bout.toByteArray()));
		}		
	}
		
	/**
	 * Remove a range of pages from a multiple page TIFF image
	 * 
	 * @param startPage start page number (inclusive) 
	 * @param endPage end page number (inclusive)
	 * @param is input image stream
	 * @param os output image stream
	 * @return number of pages removed
	 * @throws IOException
	 */
	public static int removePages(int startPage, int endPage, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		if(startPage < 0 || endPage < 0)
			throw new IllegalArgumentException("Negative start or end page");
		else if(startPage > endPage)
			throw new IllegalArgumentException("Start page is larger than end page");
		
		List<IFD> list = new ArrayList<IFD>();
	  
		int offset = copyHeader(rin, rout);
		
		// Step 1: read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, list, offset, rin);		
		// Step 2: remove pages from a multiple page TIFF
		int pagesRemoved = 0;
		if(startPage <= list.size() - 1)  {
			if(endPage > list.size() - 1) endPage = list.size() - 1;
			for(int i = endPage; i >= startPage; i--) {
				if(list.size() > 1) {
					pagesRemoved++;
					list.remove(i);
				}
			}
		}
		// Reset pageNumber for the existing pages
		for(int i = 0; i < list.size(); i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)(list.size() - 1)}));
		}
		// End of removing pages		
		// Step 3: copy the remaining pages
		// 0x08 is the first write offset
		int writeOffset = FIRST_WRITE_OFFSET;
		offset = copyPages(list, writeOffset, rin, rout);
		int firstIFDOffset = list.get(0).getStartOffset();

		writeToStream(rout, firstIFDOffset);
		
		return pagesRemoved;
	}
	
	/**
	 * Remove pages from a multiple page TIFF image
	 * 
	 * @param rin input image stream
	 * @param rout output image stream
	 * @param pages an array of page numbers to be removed
	 * @return number of pages removed
	 * @throws IOException
	 */
	public static int removePages(RandomAccessInputStream rin, RandomAccessOutputStream rout, int... pages) throws IOException {
		List<IFD> list = new ArrayList<IFD>();
				  
		int offset = copyHeader(rin, rout);
		
		// Step 1: read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, list, offset, rin);
		// Step 2: remove pages from a multiple page TIFF
		int pagesRemoved = 0;			
		pages = ArrayUtils.removeDuplicates(pages);
		for(int i = pages.length - 1; i >= 0; i--) {
			if(pages[i] < 0) break;
			// We have to keep at least one page to avoid corrupting the image
			if(list.size() > 1 && list.size() > pages[i]) {
				pagesRemoved++;
				list.remove(pages[i]);
			}
		}
		// End of removing pages
		// Reset pageNumber for the existing pages
		for(int i = 0; i < list.size(); i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)(list.size() - 1)}));
		}
		// Step 3: copy the remaining pages
		// 0x08 is the first write offset
		int writeOffset = FIRST_WRITE_OFFSET; 
		offset = copyPages(list, writeOffset, rin, rout);
		int firstIFDOffset = list.get(0).getStartOffset();

		writeToStream(rout, firstIFDOffset);
			
		return pagesRemoved;
	}
	
	public static int retainPages(int startPage, int endPage, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		if(startPage < 0 || endPage < 0)
			throw new IllegalArgumentException("Negative start or end page");
		else if(startPage > endPage)
			throw new IllegalArgumentException("Start page is larger than end page");
		
		List<IFD> list = new ArrayList<IFD>();
	  
		int offset = copyHeader(rin, rout);
		
		// Step 1: read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, list, offset, rin);		
		// Step 2: remove pages from a multiple page TIFF
		int pagesRetained = list.size();
		List<IFD> newList = new ArrayList<IFD>();
		if(startPage <= list.size() - 1)  {
			if(endPage > list.size() - 1) endPage = list.size() - 1;
			for(int i = endPage; i >= startPage; i--) {
				newList.add(list.get(i)); 
			}
		}
		if(newList.size() > 0) {
			pagesRetained = newList.size();
			list.retainAll(newList);
		}
		// Reset pageNumber for the existing pages
		for(int i = 0; i < list.size(); i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)(list.size() - 1)}));
		}
		// End of removing pages		
		// Step 3: copy the remaining pages
		// 0x08 is the first write offset
		int writeOffset = FIRST_WRITE_OFFSET;
		offset = copyPages(list, writeOffset, rin, rout);
		int firstIFDOffset = list.get(0).getStartOffset();
		
		writeToStream(rout, firstIFDOffset);
		
		return pagesRetained;
	}
	
	// Return number of pages retained
	public static int retainPages(RandomAccessInputStream rin, RandomAccessOutputStream rout, int... pages) throws IOException {
		List<IFD> list = new ArrayList<IFD>();
	  
		int offset = copyHeader(rin, rout);
		// Step 1: read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, list, offset, rin);		
		// Step 2: remove pages from a multiple page TIFF
		int pagesRetained = list.size();
		List<IFD> newList = new ArrayList<IFD>();
		Arrays.sort(pages);
		for(int i = pages.length - 1; i >= 0; i--) {
			if(pages[i] >= 0 && pages[i] < list.size())
				newList.add(list.get(pages[i])); 
		}
		if(newList.size() > 0) {
			pagesRetained = newList.size();
			list.retainAll(newList);
		}
		// End of removing pages
		// Reset pageNumber for the existing pages
		for(int i = 0; i < list.size(); i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)(list.size() - 1)}));
		}
		// Step 3: copy the remaining pages
		// 0x08 is the first write offset
		int writeOffset = FIRST_WRITE_OFFSET;
		offset = copyPages(list, writeOffset, rin, rout);
		int firstIFDOffset = list.get(0).getStartOffset();
		
		writeToStream(rout, firstIFDOffset);
		
		return pagesRetained;
	}
	
	/**
	 * Split a multiple page TIFF into single page TIFFs
	 * 
	 * @param rin input RandomAccessInputStream to read multiple page TIFF
	 * @param outputFilePrefix output file name prefix for the split TIFFs
	 * @throws IOException
	 */
	public static void splitPages(RandomAccessInputStream rin, String outputFilePrefix) throws IOException {
		List<IFD> list = new ArrayList<IFD>();
		short endian = rin.readShort();
		rin.seek(STREAM_HEAD);
		int offset = readHeader(rin);
		readIFDs(null, null, TiffTag.class, list, offset, rin);
		
		String fileNamePrefix = "page_#";
		if(!StringUtils.isNullOrEmpty(outputFilePrefix)) fileNamePrefix = outputFilePrefix + "_" + fileNamePrefix;
		
		for(int i = 0; i < list.size(); i++) {
			RandomAccessOutputStream rout = new FileCacheRandomAccessOutputStream(new FileOutputStream(fileNamePrefix + i + ".tif"));
			// Write TIFF header
			int writeOffset = writeHeader(endian, rout);
			// Write page data
			writeOffset = copyPageData(list.get(i), writeOffset, rin, rout);
			int firstIFDOffset = writeOffset;
			// Write IFD
			if(list.get(i).removeField(TiffTag.SUBFILE_TYPE) == null)
				list.get(i).removeField(TiffTag.NEW_SUBFILE_TYPE);
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.SUBFILE_TYPE.getValue(), new short[]{1}));
			writeOffset = list.get(i).write(rout, writeOffset);
			writeToStream(rout, firstIFDOffset);
			rout.close();		
		}
	}
	
	public static void write(TIFFImage tiffImage, RandomAccessOutputStream rout) throws IOException {
		RandomAccessInputStream rin = tiffImage.getInputStream();
		int offset = writeHeader(IOUtils.BIG_ENDIAN, rout);
		offset = copyPages(tiffImage.getIFDs(), offset, rin, rout);
		int firstIFDOffset = tiffImage.getIFDs().get(0).getStartOffset();	
	 
		writeToStream(rout, firstIFDOffset);
	}
	
	// Return stream offset where to write actual image data or IFD	
	private static int writeHeader(short endian, RandomAccessOutputStream rout) throws IOException {
		// Write byte order
		rout.writeShort(endian);
		// Set write strategy based on byte order
		if (endian == IOUtils.BIG_ENDIAN)
		    rout.setWriteStrategy(WriteStrategyMM.getInstance());
		else if(endian == IOUtils.LITTLE_ENDIAN)
		    rout.setWriteStrategy(WriteStrategyII.getInstance());
		else {
			throw new RuntimeException("Invalid TIFF byte order");
	    }		
		// Write TIFF identifier
		rout.writeShort(0x2a);
		
		return FIRST_WRITE_OFFSET;
	}
	
	public static void writeMultipageTIFF(RandomAccessOutputStream rout, BufferedImage ... images) throws IOException {
		writeMultipageTIFF(rout, null, images);
	}
	
	/**
	 * Write an array of ImageFrames as a multiple page TIFF.
	 *  
	 * @param rout RandomAccessOutputStream for the output image
	 * @param frames an array of ImageFrame
	 * 
	 * @throws IOException
	 */
	public static void writeMultipageTIFF(RandomAccessOutputStream rout, ImageFrame ... frames) throws IOException {
		// Write header first
		writeHeader(IOUtils.BIG_ENDIAN, rout);
		// Write pages
		int writeOffset = FIRST_WRITE_OFFSET;
		int pageNumber = 0;
		int maxPageNumber = frames.length;
		List<IFD> list = new ArrayList<IFD>(frames.length);
		TIFFWriter writer = new TIFFWriter();
		// Grab image pixels in ARGB format and write image
		for(int i = 0; i < frames.length; i++) {
			// Retrieve image dimension
			BufferedImage frame = frames[i].getFrame();
			ImageParam param = frames[i].getFrameParam();
			int imageWidth = frame.getWidth();
			int imageHeight = frame.getHeight();
			int[] pixels = IMGUtils.getRGB(frame);//frame.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
			
			try {
				writer.setImageParam(param);
				writeOffset = writer.writePage(pixels, pageNumber++, maxPageNumber, imageWidth, imageHeight, rout, writeOffset);
				list.add(writer.getIFD());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Link the IFDs
		for(int i = 0; i < frames.length - 1; i++)
			list.get(i).setNextIFDOffset(rout, list.get(i+1).getStartOffset());
				
		int firstIFDOffset = list.get(0).getStartOffset();
		
		writeToStream(rout, firstIFDOffset);
	}
	
	public static void writeMultipageTIFF(RandomAccessOutputStream rout, ImageParam[] imageParam, BufferedImage ... images) throws IOException {
		// Write header first
		writeHeader(IOUtils.BIG_ENDIAN, rout);
		// Write pages
		int writeOffset = FIRST_WRITE_OFFSET;
		int pageNumber = 0;
		int maxPageNumber = images.length;
		List<IFD> list = new ArrayList<IFD>(images.length);
		TIFFWriter writer = new TIFFWriter();
		ImageParam[] param = null;
		
		if(imageParam == null) {
			param = new ImageParam[images.length];
			Arrays.fill(param, ImageParam.DEFAULT_IMAGE_PARAM);
		} else if(images.length > imageParam.length && imageParam.length > 0) {
				param = new ImageParam[images.length];
				System.arraycopy(imageParam, 0, param, 0, imageParam.length);
				Arrays.fill(param, imageParam.length, images.length, imageParam[imageParam.length - 1]);
		} else {
			param = imageParam;
		}
		
		// Grab image pixels in ARGB format and write image
		for(int i = 0; i < images.length; i++) {
			// Retrieve image dimension
			int imageWidth = images[i].getWidth();
			int imageHeight = images[i].getHeight();
			int[] pixels = IMGUtils.getRGB(images[i]);//images[i].getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
			
			try {
				writer.setImageParam(param[i]);
				writeOffset = writer.writePage(pixels, pageNumber++, maxPageNumber, imageWidth, imageHeight, rout, writeOffset);
				list.add(writer.getIFD());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Link the IFDs
		for(int i = 0; i < images.length - 1; i++)
			list.get(i).setNextIFDOffset(rout, list.get(i+1).getStartOffset());
				
		int firstIFDOffset = list.get(0).getStartOffset();
		
		writeToStream(rout, firstIFDOffset);
	}
	
	private static void writeToStream(RandomAccessOutputStream rout, int firstIFDOffset) throws IOException {
		// Go to the place where we should write the first IFD offset
		// and write the first IFD offset
		rout.seek(OFFSET_TO_WRITE_FIRST_IFD_OFFSET);
		rout.writeInt(firstIFDOffset);
		// Dump the data to the real output stream
		rout.seek(STREAM_HEAD);
		rout.writeToStream(rout.getLength());
		//rout.flush();
	}
}