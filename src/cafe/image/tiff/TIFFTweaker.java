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
 * TIFFTeaker.java
 *
 * Who   Date       Description
 * ====  =========  ===================================================================
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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cafe.image.compression.ImageDecoder;
import cafe.image.compression.ImageEncoder;
import cafe.image.compression.deflate.DeflateDecoder;
import cafe.image.compression.deflate.DeflateEncoder;
import cafe.image.compression.lzw.LZWTreeDecoder;
import cafe.image.compression.lzw.LZWTreeEncoder;
import cafe.image.compression.packbits.Packbits;
import cafe.image.core.ImageMeta;
import cafe.image.jpeg.Marker;
import cafe.image.meta.exif.Exif;
import cafe.image.meta.exif.ExifTag;
import cafe.image.meta.exif.GPSTag;
import cafe.image.meta.exif.InteropTag;
import cafe.image.util.IMGUtils;
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
import cafe.util.ArrayUtils;
import static cafe.image.writer.TIFFWriter.*;

/**
 * TIFF image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/28/2014
 */
public class TIFFTweaker {
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
		    System.out.println("Byte order: Motorola BIG_ENDIAN");
		    rin.setReadStrategy(ReadStrategyMM.getInstance());
		    rout.setWriteStrategy(WriteStrategyMM.getInstance());
		} else if(endian == IOUtils.LITTLE_ENDIAN) {
		    System.out.println("Byte order: Intel LITTLE_ENDIAN");
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
	
	private static TiffField<?> copyJPEGHufTable(RandomAccessInputStream rin, RandomAccessOutputStream rout, TiffField<?> field, int curPos) throws IOException
	{
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
	
	private static void copyJPEGIFByteCount(RandomAccessInputStream rin, RandomAccessOutputStream rout, int offset, int outOffset) throws IOException 
	{		
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
		
		rin.seek(offset);
		rout.seek(outOffset);
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(rin)) != Marker.SOI)
		{
			System.out.println("Invalid JPEG image, expected SOI marker not found!");
			return;
		}
		
		System.out.println(Marker.SOI);
		IOUtils.writeShortMM(rout, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(rin);
			
		while (!finished)
	    {	        
			if (Marker.fromShort(marker) == Marker.EOI)
			{
				System.out.println(Marker.EOI);
				IOUtils.writeShortMM(rout, marker);
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
	
	private static TiffField<?> copyJPEGQTable(RandomAccessInputStream rin, RandomAccessOutputStream rout, TiffField<?> field, int curPos) throws IOException
	{
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
	
	private static short copyJPEGSOS(RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException 
	{
		int len = IOUtils.readUnsignedShortMM(rin);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(rin, buf);
		IOUtils.writeShortMM(rout, Marker.SOS.getValue());
		IOUtils.writeShortMM(rout, len);
		rout.write(buf);		
		// Actual image data follow.
		int nextByte = 0;
		short marker = 0;	
		
		while((nextByte = IOUtils.read(rin)) != -1)
		{
			rout.write(nextByte);
			
			if(nextByte == 0xff)
			{
				nextByte = IOUtils.read(rin);
			    rout.write(nextByte);
			    
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
	
	/**
	 * @param offset offset to write page image data
	 * 
	 * @return the position where to write the IFD for the current image page
	 */
	private static int copyPageData(IFD ifd, int offset, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		// Original image data start from these offsets.
		TiffField<?> stripOffSets = ifd.removeField(TiffTag.STRIP_OFFSETS.getValue());
		
		if(stripOffSets == null)
			stripOffSets = ifd.removeField(TiffTag.TILE_OFFSETS.getValue());
				
		TiffField<?> stripByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS.getValue());
		
		if(stripByteCounts == null)
			stripByteCounts = ifd.getField(TiffTag.TILE_BYTE_COUNTS.getValue());		
		/* 
		 * Make sure this will work in the case when neither STRIP_OFFSETS nor TILE_OFFSETS presents.
		 * Not sure if this will ever happen for TIFF. JPEG EXIF data do not contain these fields. 
		 */
		if(stripOffSets != null) { 
			int[] counts = stripByteCounts.getDataAsLong();		
			int[] off = stripOffSets.getDataAsLong();
			int[] temp = new int[off.length];
			
			TiffField<?> tiffField = ifd.getField(TiffTag.COMPRESSION.getValue());
			
			// Uncompressed image with one strip or tile (may contain wrong StripByteCounts value)
			// Bug fix for uncompressed image with one strip and wrong StripByteCounts value
			if(tiffField != null && tiffField.getDataAsLong()[0] == 1) { // Uncompressed data
				int planaryConfiguration = 1;
				
				tiffField = ifd.getField(TiffTag.PLANAR_CONFIGURATTION.getValue());		
				if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
				
				tiffField = ifd.getField(TiffTag.SAMPLES_PER_PIXEL.getValue());
				
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
						
			if(ifd.getField(TiffTag.STRIP_BYTE_COUNTS.getValue()) != null)
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
		TiffField<?> jpegIFOffset = ifd.removeField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue());
		if(jpegIFOffset != null) {
			TiffField<?> jpegIFByteCount = ifd.removeField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue());			
			if(jpegIFByteCount != null) {
				rin.seek(jpegIFOffset.getDataAsLong()[0]);
				byte[] bytes2Read = new byte[jpegIFByteCount.getDataAsLong()[0]];
				rin.readFully(bytes2Read);
				rout.seek(offset);
				rout.write(bytes2Read);
			} else {		
				copyJPEGIFByteCount(rin, rout, jpegIFOffset.getDataAsLong()[0], offset);
			}
			jpegIFOffset = new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue(), new int[]{offset});
			ifd.addField(jpegIFOffset);
		}		
		/* Another flavor of the old style JPEG compression type 6 contains separate tables */
		TiffField<?> jpegTable = ifd.removeField(TiffTag.JPEG_DC_TABLES.getValue());
		if(jpegTable != null) {
			ifd.addField(copyJPEGHufTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
		}
		
		jpegTable = ifd.removeField(TiffTag.JPEG_AC_TABLES.getValue());
		if(jpegTable != null) {
			ifd.addField(copyJPEGHufTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
		}
	
		jpegTable = ifd.removeField(TiffTag.JPEG_Q_TABLES.getValue());
		if(jpegTable != null) {
			ifd.addField(copyJPEGQTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
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
	
	public static void finishInsert(RandomAccessOutputStream rout, List<IFD> list) throws IOException {
		// Reset pageNumber and total pages
		for(int i = 0; i < list.size(); i++) {
			int offset = list.get(i).getField(TiffTag.PAGE_NUMBER.getValue()).getDataOffset();
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
		TiffField<?> tiffField = ifd.getField(TiffTag.IMAGE_WIDTH.getValue());
		int imageWidth = tiffField.getDataAsLong()[0];
		tiffField = ifd.getField(TiffTag.IMAGE_LENGTH.getValue());
		int imageHeight = tiffField.getDataAsLong()[0];				
		
		int samplesPerPixel = 1;
		
		tiffField = ifd.getField(TiffTag.SAMPLES_PER_PIXEL.getValue());
		if(tiffField != null) {
			samplesPerPixel = tiffField.getDataAsLong()[0];
		}				
		
		int bitsPerSample = 1;
		
		tiffField = ifd.getField(TiffTag.BITS_PER_SAMPLE.getValue());
		if(tiffField != null) {
			bitsPerSample = tiffField.getDataAsLong()[0];
		}
		
		int tileWidth = -1;
		int tileLength = -1;			
		
		TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH.getValue());
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH.getValue());
		
		if(f_tileWidth != null) {
			tileWidth = f_tileWidth.getDataAsLong()[0];
			tileLength = f_tileLength.getDataAsLong()[0];
		}
		
		int rowsPerStrip = imageHeight;
		int rowWidth = imageWidth;
		
		TiffField<?> f_rowsPerStrip = ifd.getField(TiffTag.ROWS_PER_STRIP.getValue());
		if(f_rowsPerStrip != null) rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];					
		
		if(rowsPerStrip > imageHeight) rowsPerStrip = imageHeight;
		
		if(tileWidth > 0) {
			rowsPerStrip = tileLength;
			rowWidth = tileWidth;
		}
	
		int planaryConfiguration = 1;
		
		tiffField = ifd.getField(TiffTag.PLANAR_CONFIGURATTION.getValue());
		if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
		
		int[] totalBytes2Read = new int[samplesPerPixel];		
		
		if(planaryConfiguration == 1)
			totalBytes2Read[0] = ((rowWidth*bitsPerSample + 7)/8)*samplesPerPixel*rowsPerStrip;
		else
			totalBytes2Read[0] = totalBytes2Read[1] = totalBytes2Read[2] = ((rowWidth*bitsPerSample + 7)/8)*rowsPerStrip;
		
		int photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue()).getDataAsLong()[0];
		
		if(photoMetric == TiffFieldEnum.PhotoMetric.YCbCr.getValue()) {
			// Deal with down sampling
			int horizontalSampleFactor = 2; // Default 2X2
			int verticalSampleFactor = 2; // Not 1X1
			
			TiffField<?> f_YCbCrSubSampling = ifd.getField(TiffTag.YCbCr_SUB_SAMPLING.getValue());
			
			if(f_YCbCrSubSampling != null) {
				int[] sampleFactors = f_YCbCrSubSampling.getDataAsLong();
				horizontalSampleFactor = sampleFactors[0];
				verticalSampleFactor = sampleFactors[1];
			}
			
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
	
	public static int getPageCount(RandomAccessInputStream rin) throws IOException {
		List<IFD> list = new ArrayList<IFD>();
		readIFDs(list, rin);
		rin.seek(STREAM_HEAD); // Reset pointer to the stream head
		
		return list.size();
	}
	
	// Calculate the expected StripByteCounts values for uncompressed image
	public static int[] getUncompressedStripByteCounts(IFD ifd, int strips) {
		// Get image dimension first
		TiffField<?> tiffField = ifd.getField(TiffTag.IMAGE_WIDTH.getValue());
		int imageWidth = tiffField.getDataAsLong()[0];
		tiffField = ifd.getField(TiffTag.IMAGE_LENGTH.getValue());
		int imageHeight = tiffField.getDataAsLong()[0];				
		
		int samplesPerPixel = 1;
		
		tiffField = ifd.getField(TiffTag.SAMPLES_PER_PIXEL.getValue());
		if(tiffField != null) {
			samplesPerPixel = tiffField.getDataAsLong()[0];
		}				
		
		int bitsPerSample = 1;
		
		tiffField = ifd.getField(TiffTag.BITS_PER_SAMPLE.getValue());
		if(tiffField != null) {
			bitsPerSample = tiffField.getDataAsLong()[0];
		}
		
		int tileWidth = -1;
		int tileLength = -1;			
		
		TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH.getValue());
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH.getValue());
		
		if(f_tileWidth != null) {
			tileWidth = f_tileWidth.getDataAsLong()[0];
			tileLength = f_tileLength.getDataAsLong()[0];
		}
		
		int rowsPerStrip = imageHeight;
		int rowWidth = imageWidth;
		
		TiffField<?> f_rowsPerStrip = ifd.getField(TiffTag.ROWS_PER_STRIP.getValue());
		if(f_rowsPerStrip != null) rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];					
		
		if(rowsPerStrip > imageHeight) rowsPerStrip = imageHeight;
		
		if(tileWidth > 0) {
			rowsPerStrip = tileLength;
			rowWidth = tileWidth;
		}
		
		int bytesPerRow = ((bitsPerSample*rowWidth + 7)/8)*samplesPerPixel;
	
		int planaryConfiguration = 1;
		
		tiffField = ifd.getField(TiffTag.PLANAR_CONFIGURATTION.getValue());		
		if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
		
		if(planaryConfiguration == 2) {
			bytesPerRow = (bitsPerSample*rowWidth + 7)/8;
		}
		
		int bytesPerStrip = bytesPerRow*rowsPerStrip;
		
		int[] counts = new int[strips];
		
		int photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue()).getDataAsLong()[0];
		
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
		} else { // Deal with down sampling
			int horizontalSampleFactor = 2; // Default 2X2
			int verticalSampleFactor = 2; // Not 1X1
			
			TiffField<?> f_YCbCrSubSampling = ifd.getField(TiffTag.YCbCr_SUB_SAMPLING.getValue());
			
			if(f_YCbCrSubSampling != null) {
				int[] sampleFactors = f_YCbCrSubSampling.getDataAsLong();
				horizontalSampleFactor = sampleFactors[0];
				verticalSampleFactor = sampleFactors[1];
			}
			
			if(samplesPerPixel != 3) samplesPerPixel = 3;
			
			int[] sampleBytesPerRow = new int[samplesPerPixel];
			sampleBytesPerRow[0] = (bitsPerSample*rowWidth + 7)/8;
			sampleBytesPerRow[1] = (bitsPerSample*rowWidth/horizontalSampleFactor + 7)/8;
			sampleBytesPerRow[2] = sampleBytesPerRow[1];
			
			int[] sampleRowsPerStrip = new int[samplesPerPixel];
			sampleRowsPerStrip[0] = rowsPerStrip;
			sampleRowsPerStrip[1] = rowsPerStrip/verticalSampleFactor;
			sampleRowsPerStrip[2]= sampleRowsPerStrip[1];
						
			if(planaryConfiguration == 1) {
				bytesPerStrip = sampleBytesPerRow[0]*sampleRowsPerStrip[0] + sampleBytesPerRow[1]*sampleRowsPerStrip[1] + sampleBytesPerRow[2]*sampleRowsPerStrip[2];
				Arrays.fill(counts, bytesPerStrip);
				if(tileWidth < 0) { // Stripped structure, last strip may be smaller
					int lastStripBytes = (sampleBytesPerRow[0] + sampleBytesPerRow[1] + sampleBytesPerRow[2])*imageHeight - bytesPerStrip*(strips - 1);
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
					lastStripBytes[0] = sampleBytesPerRow[0]*imageHeight - sampleBytesPerStrip[0]*(stripsPerSample - 1);
					lastStripBytes[1] = sampleBytesPerRow[1]*imageHeight - sampleBytesPerStrip[1]*(stripsPerSample - 1);
					lastStripBytes[2] = lastStripBytes[1];
					startOffset = stripsPerSample - 1;
					for(int i = 0; i < samplesPerPixel; i++) {
						counts[startOffset] = lastStripBytes[i];
						startOffset = stripsPerSample;
					}
				}
			}			
		}
			
		return counts;
	}
	
	/**
	 * Insert EXIF data with optional thumbnail IFD
	 * 
	 * @param rin input image stream
	 * @param rout output image stream
	 * @param exif EXIF wrapper instance
	 * @throws Exception
	 */
	public static void insertExif(RandomAccessInputStream rin, RandomAccessOutputStream rout, Exif exif) throws Exception {
		// If no thumbnail image is provided in EXIF wrapper, one will be created from the input stream
		if(exif.isThumbnailRequired() && !exif.hasThumbnail()) {
			BufferedImage original = javax.imageio.ImageIO.read(rin);
			int imageWidth = original.getWidth();
			int imageHeight = original.getHeight();
			int thumbnailWidth = 160;
			int thumbnailHeight = 120;
			if(imageWidth < imageHeight) { // Swap thumbnail width and height to keep a relative aspect ratio
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
			// Insert the thumbnail into EXIF wrapper
			exif.setThumbnail(thumbnail);
			// Reset the stream pointer
			rin.seek(0);
		}
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		IFD imageIFD = ifds.get(0);
		if(exif.getIFD(TiffTag.EXIF_SUB_IFD) != null) {
			imageIFD.addField(new LongField(TiffTag.EXIF_SUB_IFD.getValue(), new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.EXIF_SUB_IFD, exif.getIFD(TiffTag.EXIF_SUB_IFD));
		}
		if(exif.getIFD(TiffTag.GPS_SUB_IFD) != null) {
			imageIFD.addField(new LongField(TiffTag.GPS_SUB_IFD.getValue(), new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.GPS_SUB_IFD, exif.getIFD(TiffTag.GPS_SUB_IFD));
		}
		int writeOffset = FIRST_WRITE_OFFSET;
		// Copy pages
		writeOffset = copyPages(ifds.subList(0, 1), writeOffset, rin, rout);
		if(exif.isThumbnailRequired() && exif.hasThumbnail())
			imageIFD.setNextIFDOffset(rout, writeOffset);
		// This line is very important!!!
		rout.seek(writeOffset);
		exif.write(rout);
		int firstIFDOffset = imageIFD.getStartOffset();

		writeToStream(rout, firstIFDOffset);
	}
	
	/**
	 * Insert a single page into a TIFF image
	 * 
	 * @param image a BufferedImage to insert
	 * @param index index (relative to the existing pages) to insert the page
	 * @param rout RandomAccessOutputStream to write new image
	 * @param ifds a list of IFDs
	 * @param writeOffset stream offset to insert this page
	 * @param writer TIFFWriter instance
	 * @throws IOException
	 * 
	 * @return stream offset after inserting this page
	 */
	public static int insertPage(BufferedImage image, int index, RandomAccessOutputStream rout, List<IFD> ifds, int writeOffset, TIFFWriter writer) throws IOException {
		// Sanity check
		if(index < 0) index = 0;
		else if(index > ifds.size()) index = ifds.size();		
		
		// Grab image pixels in ARGB format
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		int[] pixels = IMGUtils.getRGB(image);//image.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
		
		try {
			writeOffset = writer.writePage(pixels, index, ifds.size(), imageWidth, imageHeight, rout, writeOffset);
			ifds.add(index, writer.getIFD());
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		return writeOffset;
	}
	
	public static void insertPages(RandomAccessInputStream rin, RandomAccessOutputStream rout, TIFFWriter writer, int pageNumber, BufferedImage... images) throws IOException {
		insertPages(rin, rout, writer, pageNumber, null, images);
	}
	
	/**
	 * Insert pages into a TIFF image
	 * 
	 * @param images a number of BufferedImage to insert
	 * @param pageNumber first page number
	 * @param rin RandomAccessInputStream to read old image
	 * @param rout RandomAccessOutputStream to write new image
	 * @param writer TIFFWriter instance
	 * @throws IOException
	 */
	public static void insertPages(RandomAccessInputStream rin, RandomAccessOutputStream rout, TIFFWriter writer, int pageNumber, ImageMeta[] imageMeta, BufferedImage... images) throws IOException {
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
		
		ImageMeta[] meta = null;
		
		if(imageMeta == null) {
			meta = new ImageMeta[images.length];
			Arrays.fill(meta, writer.getImageMeta());
		} else if(images.length > imageMeta.length && imageMeta.length > 0) {
				meta = new ImageMeta[images.length];
				System.arraycopy(imageMeta, 0, meta, 0, imageMeta.length);
				Arrays.fill(meta, imageMeta.length, images.length, imageMeta[imageMeta.length - 1]);
		} else {
			meta = imageMeta;
		}
	
		for(int i = 0; i < images.length; i++) {
			// Retrieve image dimension
			int imageWidth = images[i].getWidth();
			int imageHeight = images[i].getHeight();
			// Grab image pixels in ARGB format and write image
			int[] pixels = IMGUtils.getRGB(images[i]);//images[i].getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
			
			try {
				writer.setImageMeta(meta[i]);
				writeOffset = writer.writePage(pixels, pageNumber++, maxPageNumber, imageWidth, imageHeight, rout, writeOffset);
				insertedList.add(writer.getIFD());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Reset pageNumber for the existing pages
		for(int i = 0; i < minPageNumber; i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)maxPageNumber}));
		}
		
		for(int i = minPageNumber; i < list.size(); i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)(i + images.length), (short)maxPageNumber}));
		}
		
		if(list.size() == 1) { // Make the original image one page of the new multiple page TIFF
			if(list.get(0).removeField(TiffTag.SUBFILE_TYPE.getValue()) == null)
				list.get(0).removeField(TiffTag.NEW_SUBFILE_TYPE.getValue());
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
			ifds1.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
			ifds1.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)maxPageNumber}));
		}
		for(int i = 0; i < ifds2.size(); i++) {
			ifds2.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
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
				ifds1.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
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
					ifds2.get(j).removeField(TiffTag.PAGE_NUMBER.getValue());
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
				offset = ifds1.get(i).getField(TiffTag.PAGE_NUMBER.getValue()).getDataOffset();
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
				ifds1.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
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
					ifds2.get(j).removeField(TiffTag.PAGE_NUMBER.getValue());
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
						TiffField<?> f_bitsPerSample = currIFD.getField(TiffTag.BITS_PER_SAMPLE.getValue());
						if(f_bitsPerSample != null) bitsPerSample = f_bitsPerSample.getDataAsLong()[0];
						if(bitsPerSample <= 8) { // Just copy data
							offset = copyPageData(currIFD, offset, image2, merged);							
						} else if(bitsPerSample == 16) {
							/*
							 * TIFF viewers seem to have problem interpreting data with more than 8 BitsPerSample.
							 * Most of the viewers interpret 16 BitsPerSample according to the  endianess of the image,
							 * but think other bit depth like 12 bits always as big endian. For now we only flip the
							 * endian of 16 BitsPerSample as needed and leave the other bit depth images as is.
							 */
							// We assume BitsPerSample is 16, flip the byte sequence of the data
							ImageDecoder decoder = null;
							ImageEncoder encoder = null;
							// Original image data start from these offsets.
							TiffField<?> stripOffSets = currIFD.removeField(TiffTag.STRIP_OFFSETS.getValue());							
							if(stripOffSets == null)
								stripOffSets = currIFD.removeField(TiffTag.TILE_OFFSETS.getValue());									
							TiffField<?> stripByteCounts = currIFD.getField(TiffTag.STRIP_BYTE_COUNTS.getValue());							
							if(stripByteCounts == null)
								stripByteCounts = currIFD.getField(TiffTag.TILE_BYTE_COUNTS.getValue());
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
								
								TiffField<?> tiffField = currIFD.getField(TiffTag.COMPRESSION.getValue());
								TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(tiffField.getDataAsLong()[0]);
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
											short[] sbuf = ArrayUtils.byteArrayToShortArray(buf2, 0, buf2.length, readEndian == IOUtils.BIG_ENDIAN);
											buf = ArrayUtils.shortArrayToByteArray(sbuf, writeEndian == IOUtils.BIG_ENDIAN);
											// Compress the data
											buf2 = new byte[buf.length + (buf.length + 127)/128];
											int bytesCompressed = Packbits.packbits(buf, buf2);
											merged.write(buf2, 0, bytesCompressed);
											temp[k] = offset;
											offset += bytesCompressed; // DONE!
										}
										break;
									case NONE:										
										int planaryConfiguration = 1;
										
										tiffField = currIFD.getField(TiffTag.PLANAR_CONFIGURATTION.getValue());		
										if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
										
										tiffField = currIFD.getField(TiffTag.SAMPLES_PER_PIXEL.getValue());
										
										int samplesPerPixel = 1;
										if(tiffField != null) samplesPerPixel = tiffField.getDataAsLong()[0];
										
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
											buf = ArrayUtils.flipEndian(buf, 0, bitsPerSample, buf.length, readEndian == IOUtils.BIG_ENDIAN);
											//short[] sbuf = ArrayUtils.byteArrayToShortArray(buf, readEndian == IOUtils.BIG_ENDIAN);
											//buf = ArrayUtils.shortArrayToByteArray(sbuf, writeEndian == IOUtils.BIG_ENDIAN);
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
										short[] sbuf = ArrayUtils.byteArrayToShortArray(decompressed, 0, bytesDecompressed, readEndian == IOUtils.BIG_ENDIAN);
										buf = ArrayUtils.shortArrayToByteArray(sbuf, writeEndian == IOUtils.BIG_ENDIAN);
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
								if(currIFD.getField(TiffTag.STRIP_BYTE_COUNTS.getValue()) != null)
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
				offset = ifds1.get(i).getField(TiffTag.PAGE_NUMBER.getValue()).getDataOffset();
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
			if(ifds.get(0).removeField(TiffTag.SUBFILE_TYPE.getValue()) == null)
				ifds.get(0).removeField(TiffTag.NEW_SUBFILE_TYPE.getValue());
			ifds.get(0).addField(new ShortField(TiffTag.SUBFILE_TYPE.getValue(), new short[]{3}));
		}		
		for(int i = 0; i < ifds.size(); i++) {
			ifds.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
			// Place holder, to be updated later
			ifds.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{0, 0}));
		}
		int writeOffset = FIRST_WRITE_OFFSET;
		// Copy pages
		writeOffset = copyPages(ifds, writeOffset, rin, rout);
		
		return writeOffset;
	}
	
	private static int readHeader(RandomAccessInputStream rin) throws IOException {
		int offset = 0;
	    // First 2 bytes determine the byte order of the file
		rin.seek(STREAM_HEAD);
	    short endian = rin.readShort();
	    offset += 2;
	
		if (endian == IOUtils.BIG_ENDIAN)
		{
		    System.out.println("Byte order: Motorola BIG_ENDIAN");
		    rin.setReadStrategy(ReadStrategyMM.getInstance());
		}
		else if(endian == IOUtils.LITTLE_ENDIAN)
		{
		    System.out.println("Byte order: Intel LITTLE_ENDIAN");
		    rin.setReadStrategy(ReadStrategyII.getInstance());
		}
		else {		
			rin.close();
			throw new RuntimeException("Invalid TIFF byte order");
	    }
		
		// Read TIFF identifier
		rin.seek(offset);
		short tiff_id = rin.readShort();
		offset +=2;
		if(tiff_id!=0x2a)//"*" 42 decimal
		{
			rin.close();
			throw new RuntimeException("Invalid TIFF identifier");
		}
		
		rin.seek(offset);
		offset = rin.readInt();
			
		return offset;
	}
	
	private static int readIFD(IFD parent, Tag parentTag, Class<?> tagClass, RandomAccessInputStream rin, List<IFD> list, int offset, String indent) throws IOException 
	{	
		// Use reflection to invoke fromShort(short) method
		Method method = null;
		try {
			method = tagClass.getDeclaredMethod("fromShort", short.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		String indent2 = indent + "-----"; // Increment indentation
		IFD tiffIFD = new IFD();
		rin.seek(offset);
		int no_of_fields = rin.readShort();
		System.out.print(indent);
		System.out.println("Total number of fields: " + no_of_fields);
		offset += 2;
		
		for (int i = 0; i < no_of_fields; i++)
		{
			System.out.print(indent);
			System.out.println("Field "+i+" =>");
			rin.seek(offset);
			short tag = rin.readShort();
			Tag ftag = null;
			try {
				ftag = (Tag)method.invoke(null, tag);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			System.out.print(indent);
			if (ftag == TiffTag.UNKNOWN) {
				System.out.println("Tag: " + ftag + " [Value: 0x"+ Integer.toHexString(tag&0xffff) + "]" + " (Unknown)");
			} else {
				System.out.println("Tag: " + ftag);
			}
			offset += 2;
			rin.seek(offset);
			short type = rin.readShort();
			FieldType ftype = FieldType.fromShort(type);
			System.out.print(indent);
			System.out.println("Data type: " + ftype);
			offset += 2;
			rin.seek(offset);
			int field_length = rin.readInt();
			System.out.print(indent);
			System.out.println("Field length: " + field_length);
			offset += 4;
			////// Try to read actual data.
			switch (ftype)
			{
				case BYTE:
				case UNDEFINED:
					byte[] data = new byte[field_length];
					if(field_length <= 4) {
						rin.seek(offset);
						rin.readFully(data, 0, field_length);					   
					}
					else {
						rin.seek(offset);
						rin.seek(rin.readInt());
						rin.readFully(data, 0, field_length);
					}
					System.out.print(indent);
					if(ftag == ExifTag.EXIF_VERSION || ftag == ExifTag.FLASH_PIX_VERSION)
						System.out.println("Field value: " + new String(data));
					else
						System.out.println("Field value: " + StringUtils.byteArrayToHexString(data, 0, 10));
					offset += 4;					
					tiffIFD.addField((ftype == FieldType.BYTE)?new ByteField(tag, data):new UndefinedField(tag, data));
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
					if(data.length>0) {
						System.out.print(indent);
						System.out.println("Field value: " + new String(data, 0, data.length).trim());
					}
					offset += 4;	
					tiffIFD.addField(new ASCIIField(tag, new String(data, 0, data.length)));
			        break;
				case SHORT:
					short[] sdata = new short[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  sdata[0] = rin.readShort();
					  offset += 4;
					}
					else if (field_length == 2)
					{
						rin.seek(offset);
						sdata[0] = rin.readShort();
						offset += 2;
						rin.seek(offset);
						sdata[1] = rin.readShort();
						offset += 2;
					}
					else {
						rin.seek(offset);
						int toOffset = rin.readInt();
						offset += 4;
						for (int j = 0; j  <field_length; j++){
							rin.seek(toOffset);
							sdata[j] = rin.readShort();
							toOffset += 2;
						}
					}	
					tiffIFD.addField(new ShortField(tag, sdata));
					System.out.print(indent);
					System.out.println("Field value: " + StringUtils.shortArrayToString(sdata, true) + " " + ftag.getFieldDescription(sdata[0]&0xffff));
					break;
				case LONG:
					int[] ldata = new int[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  ldata[0] = rin.readInt();
					  offset += 4;
					}
					else {
						rin.seek(offset);
						int toOffset = rin.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							rin.seek(toOffset);
							ldata[j] = rin.readInt();
							toOffset += 4;
						}
					}
					
					tiffIFD.addField(new LongField(tag, ldata));
					
					System.out.print(indent);
					System.out.println("Field value: " + StringUtils.longArrayToString(ldata, true) + " " + ftag.getFieldDescription(ldata[0]&0xffff));
					
					if ((ftag == TiffTag.EXIF_SUB_IFD) && (ldata[0]!= 0)) {
						System.out.print(indent);
						System.out.println("<<ExifSubIFD: offset byte " + offset + ">>");
						try { // If something bad happens, we skip the sub IFD
							readIFD(tiffIFD, TiffTag.EXIF_SUB_IFD, ExifTag.class, rin, null, ldata[0], indent2);
						} catch(Exception e) {
							tiffIFD.removeField(TiffTag.EXIF_SUB_IFD.getValue());
							e.printStackTrace();
						}
					} else if ((ftag == TiffTag.GPS_SUB_IFD) && (ldata[0] != 0)) {
						System.out.print(indent);
						System.out.println("<<GPSSubIFD: offset byte " + offset + ">>");
						try {
							readIFD(tiffIFD, TiffTag.GPS_SUB_IFD, GPSTag.class, rin, null, ldata[0], indent2);
						} catch(Exception e) {
							tiffIFD.removeField(TiffTag.GPS_SUB_IFD.getValue());
							e.printStackTrace();
						}
					} else if((ftag == ExifTag.EXIF_INTEROPERABILITY_OFFSET) && (ldata[0] != 0)) {
						System.out.print(indent);
						System.out.println("<<ExifInteropSubIFD: offset byte " + offset + ">>");
						try {
							readIFD(tiffIFD, ExifTag.EXIF_INTEROPERABILITY_OFFSET, InteropTag.class, rin, null, ldata[0], indent2);
						} catch(Exception e) {
							tiffIFD.removeField(ExifTag.EXIF_INTEROPERABILITY_OFFSET.getValue());
							e.printStackTrace();
						}
					} else if (ftag == TiffTag.SUB_IFDS) {						
						for(int ifd = 0; ifd < ldata.length; ifd++) {
							System.out.print(indent);
							System.out.println("******* SubIFD " + ifd + " *******");
							try {
								readIFD(tiffIFD, TiffTag.SUB_IFDS, TiffTag.class, rin, null, ldata[0], indent2);
							} catch(Exception e) {
								tiffIFD.removeField(TiffTag.SUB_IFDS.getValue());
								e.printStackTrace();
							}
							System.out.println("******* End of SubIFD " + ifd + " *******");
						}
					}				
					break;
				case RATIONAL:
					int len = 2*field_length;
					ldata = new int[len];	
					rin.seek(offset);
					int toOffset = rin.readInt();
					offset += 4;					
					for (int j=0;j<len; j+=2){
						rin.seek(toOffset);
						ldata[j] = rin.readInt();
						toOffset += 4;
						rin.seek(toOffset);
						ldata[j+1] = rin.readInt();
						toOffset += 4;
					}	
					tiffIFD.addField(new RationalField(tag, ldata));
					System.out.print(indent);
					System.out.println("Field value: " + StringUtils.rationalArrayToString(ldata, true));
					break;
				case IFD:
					ldata = new int[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  ldata[0] = rin.readInt();
					  offset += 4;
					}
					else {
						rin.seek(offset);
						toOffset = rin.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							rin.seek(toOffset);
							ldata[j] = rin.readInt();
							toOffset += 4;
						}
					}
					System.out.print(indent);
					System.out.println("Field value: " + StringUtils.longArrayToString(ldata, true) + " " + ftag.getFieldDescription(ldata[0]&0xffff));
					for(int ifd = 0; ifd < ldata.length; ifd++) {
						System.out.print(indent);
						System.out.println("******* SubIFD " + ifd + " *******");
						readIFD(tiffIFD, TiffTag.SUB_IFDS, TiffTag.class, rin, null, ldata[0], indent2);
						System.out.println("******* End of SubIFD " + ifd + " *******");
					}
					tiffIFD.addField(new IFDField(tag, ldata));			
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
	
	private static void readIFDs(IFD parent, Tag parentTag, Class<?> tagClass, List<IFD> list, int offset, RandomAccessInputStream rin) throws IOException {
		int ifd = 0;
		// Read the IFDs into a list first	
		while (offset != 0)
		{
			System.out.println("************************************************");
			System.out.println("IFD " + ifd++ + " => offset byte " + offset);
			offset = readIFD(parent, parentTag, tagClass, rin, list, offset, "");
		}
	}
	
	public static void readIFDs(List<IFD> list, RandomAccessInputStream rin) throws IOException {
		int offset = readHeader(rin);
		readIFDs(null, null, TiffTag.class, list, offset, rin);
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
			list.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
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
		Arrays.sort(pages);
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
			list.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
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
			list.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
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
			list.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
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
	
	public static void snoop(RandomAccessInputStream rin) throws IOException	{	
		System.out.println("*** TIFF snooping starts ***");
		int offset = readHeader(rin);
		List<IFD> list = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, list, offset, rin);	
		System.out.println("*** TIFF snooping ends ***");
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
			if(list.get(i).removeField(TiffTag.SUBFILE_TYPE.getValue()) == null)
				list.get(i).removeField(TiffTag.NEW_SUBFILE_TYPE.getValue());
			list.get(i).removeField(TiffTag.PAGE_NUMBER.getValue());
			list.get(i).addField(new ShortField(TiffTag.SUBFILE_TYPE.getValue(), new short[]{1}));
			writeOffset = list.get(i).write(rout, writeOffset);
			writeToStream(rout, firstIFDOffset);
			rout.close();		
		}
	}
	
	public static void write(TIFFImage tiffImage) throws IOException {
		RandomAccessInputStream rin = tiffImage.getInputStream();
		RandomAccessOutputStream rout = tiffImage.getOutputStream();
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
	
	public static void writeMultipageTIFF(RandomAccessOutputStream rout, TIFFWriter writer, BufferedImage[] images) throws IOException {
		writeMultipageTIFF(rout, writer, null, images);
	}
	
	public static void writeMultipageTIFF(RandomAccessOutputStream rout, TIFFWriter writer, ImageMeta[] imageMeta,  BufferedImage[] images) throws IOException {
		// Write header first
		writeHeader(IOUtils.BIG_ENDIAN, rout);
		// Write pages
		int writeOffset = FIRST_WRITE_OFFSET;
		int pageNumber = 0;
		int maxPageNumber = images.length;
		List<IFD> list = new ArrayList<IFD>(images.length);
		
		ImageMeta[] meta = null;
		
		if(imageMeta == null) {
			meta = new ImageMeta[images.length];
			Arrays.fill(meta, writer.getImageMeta());
		} else if(images.length > imageMeta.length && imageMeta.length > 0) {
				meta = new ImageMeta[images.length];
				System.arraycopy(imageMeta, 0, meta, 0, imageMeta.length);
				Arrays.fill(meta, imageMeta.length, images.length, imageMeta[imageMeta.length - 1]);
		} else {
			meta = imageMeta;
		}
		
		// Grab image pixels in ARGB format and write image
		for(int i = 0; i < images.length; i++) {
			// Retrieve image dimension
			int imageWidth = images[i].getWidth();
			int imageHeight = images[i].getHeight();
			int[] pixels = IMGUtils.getRGB(images[i]);//images[i].getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
			
			try {
				writer.setImageMeta(meta[i]);
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