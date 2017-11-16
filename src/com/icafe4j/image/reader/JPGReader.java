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
 * JPGReader.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    12Jan2016  Cleaned up stale code
 */
/** 
  * Decodes and shows images in JPEG format.
  *
  * Current version is a baseline JFIF compatible one. It supports Adobe
  * APP14 color transform - YCCK, CMYK, YCCK inverted. Progressive DCT
  * is not supported!
  *
  * @author Wen Yu, yuwen_66@yahoo.com
  * @version 1.0 04/23/2007
  */
package com.icafe4j.image.reader;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.compression.huffman.HuffmanTbl;
import com.icafe4j.image.jpeg.DHTReader;
import com.icafe4j.image.jpeg.DQTReader;
import com.icafe4j.image.jpeg.HTable;
import com.icafe4j.image.jpeg.Marker;
import com.icafe4j.image.jpeg.QTable;
import com.icafe4j.image.jpeg.SOFReader;
import com.icafe4j.image.jpeg.SOSReader;
import com.icafe4j.image.jpeg.Segment;
import com.icafe4j.image.jpeg.Component;
import com.icafe4j.io.IOUtils;
import com.icafe4j.string.StringUtils;
import com.icafe4j.util.ArrayUtils;

public class JPGReader extends ImageReader {
	//"Adobe" no trailing NULL
	public static final byte[] ADOBE_ID = {0x41, 0x64, 0x6f, 0x62, 0x65};
	public static final String ICC_PROFILE_ID = "ICC_PROFILE\0";

	private SOFReader sofReader;
	
	// Tables definition
	// For JFIF there are normally two quantization tables, but for
	// other format there can be up to 4 quantization tables!
	private int quant_tbl[][] = new int[4][];
	private HuffmanTbl dc_hufftbl[] = new HuffmanTbl[4];
	private HuffmanTbl ac_hufftbl[] = new HuffmanTbl[4];
	@SuppressWarnings("unused")
	private Map<Integer, Component> components = new HashMap<Integer, Component>(4);
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(JPGReader.class);
	
	public BufferedImage read1(InputStream is) throws Exception	{
		boolean finished = false;
		int length = 0;
		short marker;
		Marker emarker;
		
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
				
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(is)) != Marker.SOI)
			throw new IllegalArgumentException("Invalid JPEG image, expected SOI marker not found!");
		
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
				    case DQT:
				    	read_DQT(is);
				    	marker = IOUtils.readShortMM(is);
						break;
				    case DHT:
				    	read_DHT(is);
				    	marker = IOUtils.readShortMM(is);
						break;
				    case SOS:
				    	readSOS(is, sofReader);
						marker = IOUtils.readShortMM(is);
						break;
				    case SOF0:
	                case SOF1:
	                case SOF2:
	                	readSOF(is, emarker);
	                	// Read actual image data
				    case SOF3:
	                case SOF5:
	                case SOF6:
	                case SOF7:
	                    throw new Exception("Lossless Jpeg is not supported yet");
	                case SOF9:
	                case SOF10:
	                case SOF11:
	                    throw new Exception("Arithmetic encoded Jpeg is not supported yet");
				    case APP2:
				    	// Read ICC_Profile data
				    	readAPP2(is, bo);
						marker = IOUtils.readShortMM(is);
						break;
				    case APP14:
				    	// Read APP14 data
				    	readAPP14(is);
				    default:
				    	length = IOUtils.readUnsignedShortMM(is);					
				    	byte[] buf = new byte[length - 2];					   
				    	IOUtils.readFully(is, buf);				
				    	marker = IOUtils.readShortMM(is);
				}
			}				
		}
		
		return null;
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
	
	private static void readAPP14(InputStream is) throws IOException {	
		String[] app14Info = {"DCTEncodeVersion: ", "APP14Flags0: ", "APP14Flags1: ", "ColorTransform: "};		
		int expectedLen = 14; // Expected length of this segment is 14.
		int length = IOUtils.readUnsignedShortMM(is);
		if (length >= expectedLen) { 
			byte[] data = new byte[length - 2];
			IOUtils.readFully(is, data, 0, length - 2);
			byte[] buf = ArrayUtils.subArray(data, 0, 5);
			
			if(Arrays.equals(buf, ADOBE_ID)) {
				for (int i = 0, j = 5; i < 3; i++, j += 2) {
					LOGGER.info("{}{}", app14Info[i], StringUtils.shortToHexStringMM(IOUtils.readShortMM(data, j)));
				}
				LOGGER.info("{}{}", app14Info[3], (((data[11]&0xff) == 0)? "Unknown (RGB or CMYK)":
					((data[11]&0xff) == 1)? "YCbCr":"YCCK" ));
			}
		}
	}
	   
	private void read_DQT(InputStream is)throws IOException {
		// Define quantization table segment
		int len = IOUtils.readUnsignedShortMM(is);
        byte buf[] = new byte[len - 2];
        IOUtils.readFully(is, buf);
        
    	DQTReader reader = new DQTReader(new Segment(Marker.DQT, len, buf));    	
    	List<QTable> qTables = reader.getTables();
    	
    	for(QTable table : qTables) {
			int destination_id = table.getID();
			quant_tbl[destination_id] = table.getData();
		}
    	
    	LOGGER.debug("\n{}", qTablesToString(qTables));
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
		
	// Process define Huffman table
	private void read_DHT(InputStream is) throws IOException {
		// Define Huffman table segment
		int len = IOUtils.readUnsignedShortMM(is);
        byte buf[] = new byte[len - 2];
        IOUtils.readFully(is, buf);
        
    	DHTReader reader = new DHTReader(new Segment(Marker.DHT, len, buf));
		
		List<HTable> dcTables = reader.getDCTables();
		List<HTable> acTables = reader.getACTables();
		
		for(HTable table : dcTables)
			dc_hufftbl[table.getID()] = new HuffmanTbl(table.getBits(), table.getValues());
			
		for(HTable table : acTables)
			ac_hufftbl[table.getID()] = new HuffmanTbl(table.getBits(), table.getValues());
		
		LOGGER.debug("\n{}", hTablesToString(dcTables));
		LOGGER.debug("\n{}", hTablesToString(acTables));
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
	
	private SOFReader readSOF(InputStream is, Marker marker) throws IOException {		
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		Segment segment = new Segment(marker, len, buf);		
		SOFReader reader = new SOFReader(segment);
		
		LOGGER.debug("\n", sofToString(reader));
		
		return reader;
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
	
	private short readSOS(InputStream is, SOFReader sofReader) throws IOException {
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
	   
	@Override
	public BufferedImage read(InputStream is) throws Exception {
		return javax.imageio.ImageIO.read(is);
		//return read1(is);
	}
}