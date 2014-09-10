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
 * ====  =======    =================================================
 * WY    29Aug2014  Changed removeAPP1() to more general removeAPPn()
 * WY    07Jun2014  Added extractExifThumbnail() to extract thumbnail
 * WY    07Jun2014  Added insertICCProfile() to insert ICC_Profile
 * WY    06Jun2014  Added extractICCProfile() to extract ICC_Profile
 * WY    03Apr2014  Added snoop() as a result of delete JPEGSnoop
 */

package cafe.image.jpeg;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import cafe.image.meta.ImageResourceID;
import cafe.image.meta.exif.Exif;
import cafe.image.meta.icc.ICCProfile;
import cafe.image.meta.iptc.IPTCDataSet;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JPEG image tweaking tool
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/25/2013
 */
public class JPEGTweaker {
	/** Copy a single SOS segment */	
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
	
	// Extract a thumbnail image from Exif APP1 segment if any
	public static void extractExifThumbnail(InputStream is, String pathToThumbnail) throws IOException {
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
						    	IFD thumbnailIFD = ifds.get(1);
						    	TiffField<?> field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue());
						    	if(field != null) { // Save as JPEG
						    		int thumbnailOffset = field.getDataAsLong()[0];
						    		field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue());
						    		int thumbnailLen = field.getDataAsLong()[0];
						    		tiffin.seek(thumbnailOffset);
						    		byte[] data = new byte[thumbnailLen];
						    		tiffin.readFully(data);
						    		OutputStream fout = new FileOutputStream(pathToThumbnail.replaceFirst("[.][^.]+$", "")+".jpg");
						    		fout.write(data);
						    		fout.close();
						    	} else { // Save as TIFF
						    		field = thumbnailIFD.getField(TiffTag.STRIP_OFFSETS.getValue());
						    		if(field == null) 
						    			field = thumbnailIFD.getField(TiffTag.TILE_OFFSETS.getValue());
						    		if(field != null) {
						    			 tiffin.seek(0);
						    			 OutputStream fout = new FileOutputStream(pathToThumbnail.replaceFirst("[.][^.]+$", "")+".tif");
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
				    default:
					    length = IOUtils.readUnsignedShortMM(is);					
					    byte[] buf = new byte[length - 2];
					    IOUtils.readFully(is, buf);
					    marker = IOUtils.readShortMM(is);
				}
			}
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
			OutputStream os = new FileOutputStream(pathToICCProfile.replaceFirst("[.][^.]+$", "")+".icc");
			os.write(icc_profile);
			os.close();
		}	
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
	
	public static void insertICCProfile(InputStream is, OutputStream os, ICC_Profile icc_profile) throws Exception {
		// Copy the original image and insert ICC_Profile data
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
				    	writeICCProfile(os, icc_profile);
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
				for (int j = 0; j<64; j++)
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
	
	private static void readSOF0(InputStream is, Map<String, Segment> segmentMap) throws IOException 
	{		
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		Segment segment = new Segment(Marker.SOF0, len, buf);		
		SOF0Reader reader = new SOF0Reader(segment);
		
		segmentMap.put(Marker.SOF0.name(), segment);		
		
		System.out.println("Data length: " + len);		
		System.out.println("Precision: " + reader.getPrecision());
		System.out.println("Image height: " + reader.getImageHeight());
		System.out.println("Image width: " + reader.getImageWidth());
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
	}
	
	private static short readSOS(InputStream is, Map<String, Segment> segmentMap) throws IOException 
	{
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		Segment sof = segmentMap.get(Marker.SOF0.name());
		
		if(sof == null) {
			System.out.println("<<No SOF0 found, skip remaining SOS!>>");
			return Marker.EOI.getValue(); 
		}
		
		Segment segment = new Segment(Marker.SOS, len, buf);	
		SOF0Reader sof0Reader = new SOF0Reader(sof);
		new SOSReader(segment, sof0Reader);
		
		Component[] components = sof0Reader.getComponents();
		
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
	
	private static void readAPP0(InputStream is) throws IOException
	{
		int length = IOUtils.readUnsignedShortMM(is);
	    IOUtils.skipFully(is, length-2);
	}
	
	private static void readAPP1(InputStream is) throws IOException 
	{
		// EXIF identifier with trailing bytes [0x00,0x00] or [0x00,0xff].
		byte[] exif = {0x45, 0x78, 0x69, 0x66, 0x00, 0x00};
		byte[] exif2 = {0x45, 0x78, 0x69, 0x66, 0x00, (byte)0xff};
		byte[] buf = new byte[6];
		int length = IOUtils.readUnsignedShortMM(is);
		
		IOUtils.readFully(is, buf);		
		// EXIF segment.
		if (Arrays.equals(buf, exif)||Arrays.equals(buf, exif2)) {
			buf = new byte[length-8];
		    IOUtils.readFully(is, buf);
			JPEGTweaker.readExif(new ByteArrayInputStream(buf));		
		} else {
			// Might be Adobe XMP segment.
			//IOUtils.skipFully(is, length-8);
			// TODO process XMP packet.
			byte[] tmp = new byte[length-2];
			IOUtils.readFully(is, tmp, 6, length-8);
			System.arraycopy(buf, 0, tmp, 0, 6);
			System.out.println(new String(tmp, "utf-8").trim());		
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
							int tagMarker = data[i];
							List<IPTCDataSet> list = new ArrayList<IPTCDataSet>();
							
							while (tagMarker == 0x1c) {
								i++;
								int recordNumber = data[i++];
								int tag = data[i++];
								int recordSize = IOUtils.readUnsignedShortMM(data, i);
								i += 2;
								list.add(new IPTCDataSet(recordNumber, tag, recordSize, data, i));
								i += recordSize;
								
								tagMarker = data[i];							
							}
							
							if(size%2 != 0) i++;
								
							for(IPTCDataSet iptc : list) {
								iptc.print();
							}
							
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
	
	private static void readExif(InputStream is) throws IOException {		
		RandomAccessInputStream randInputStream = new FileCacheRandomAccessInputStream(is);
		List<IFD> list = new ArrayList<IFD>();
	    TIFFTweaker.readIFDs(list, randInputStream);
	    if(list.size() >= 2) {
	    	IFD thumbnailIFD = list.get(1);
	    	TiffField<?> field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue());
	    	if(field != null) { // Save as JPEG
	    		int thumbnailOffset = field.getDataAsLong()[0];
	    		field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue());
	    		int thumbnailLen = field.getDataAsLong()[0];
	    		randInputStream.seek(thumbnailOffset);
	    		byte[] data = new byte[thumbnailLen];
	    		randInputStream.readFully(data);
	    		OutputStream fout = new FileOutputStream("thumbnail.jpg");
	    		fout.write(data);
	    		fout.close();
	    	} else { // Save as TIFF
	    		field = thumbnailIFD.getField(TiffTag.STRIP_OFFSETS.getValue());
	    		if(field == null) 
	    			field = thumbnailIFD.getField(TiffTag.TILE_OFFSETS.getValue());
	    		if(field != null) {
	    			 randInputStream.seek(0);
	    			 OutputStream fout = new FileOutputStream("thumbnail.tif");
	    			 RandomAccessOutputStream tiffout = new FileCacheRandomAccessOutputStream(fout);
	    			 TIFFTweaker.retainPages(randInputStream, tiffout, 1);
	    			 tiffout.close(); // Auto flush when closed
	    			 fout.close();
	    		}
	    	}
	    }
		randInputStream.close();
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
				    	marker = copySOS(is, os);
				    	//copyToEnd(is, os);
						//finished = true; 
						break;
				    case APP1:
				    	readAPP1(is);
						marker = IOUtils.readShortMM(is);
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
	
	public static void showICCProfile(byte[] icc_profile) throws IOException {
		// TODO
		ICCProfile profile = new ICCProfile(new ByteArrayInputStream(icc_profile));
		profile.showHeader();
		profile.showTagTable();
	}
	
	public static void showICCProfile(InputStream is) throws IOException {
		byte[] icc_profile = extractICCProfile(is);
		if(icc_profile != null && icc_profile.length > 0) showICCProfile(icc_profile);
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
		// Definitions
		List<QTable> m_qTables = new ArrayList<QTable>(4);
		List<HTable> m_acTables = new ArrayList<HTable>(4);	
		List<HTable> m_dcTables = new ArrayList<HTable>(4);
		
		Map<String, Segment> segmentMap = new HashMap<String, Segment>();
		
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
						readSOF0(is, segmentMap);
						marker = IOUtils.readShortMM(is);
						break;
					case SOS:						
						marker = readSOS(is, segmentMap);
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
		
		System.out.println("*** JPEG snooping ends ***");
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
	
	// Prevent from instantiation
	private JPEGTweaker() {}
}