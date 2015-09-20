/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.reader;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.icafe4j.io.IOUtils;
import com.icafe4j.string.StringUtils;
import com.icafe4j.util.ArrayUtils;

public class JPEGReader extends ImageReader {
	public static final int LUMINANCE = 1;
	public static final int CHROMINANCE = 2;   
	/**
	 * A segment is a stream of bytes with length <= 65535.The segment
	 * begins with a marker. A marker consists of 2 bytes beginning with
	 * 0xFF, and ending with a byte between 0 and 0xFF.
	 */
	public static final int SEGMENT_IDENTIFIER = 0xff;
	public static final int END_OF_STREAM = 256;
	/**
	 * Define JPEG markers. 
	 * A marker is prefixed by a SEGMENT_IDENTIFIER. 
	 * Most markers will have additional information following them. 
	 * When this is the case, the marker and its associated information
	 * is referred to as a "header." In a header the marker is immediately 
	 * followed by two bytes that indicate the length of the information, 
	 * in bytes, that the header contains. The two bytes that indicate the 
	 * length are always included in that count.
	 */
	public static final int TEM  = 0x01; // Usually causes a decoding error, may be ignored 
	public static final int SOF0 = 0xc0; // Baseline DCT process frame marker
	public static final int SOF1 = 0xc1; // Extended sequential DCT frame marker, Huffman coding
	public static final int SOF2 = 0xc2; // Progressive DCT frame marker, Huffman coding
	public static final int SOF3 = 0xc3; // Lossless process frame marker, Huffman coding
	public static final int DHT  = 0xc4; // Define Huffman table 
	public static final int SOF5 = 0xc5; // Differential sequential DCT frame marker, Huffman coding
	public static final int SOF6 = 0xc6; // Differential progressive DCT frame marker, Huffman coding
	public static final int SOF7 = 0xc7; // Differential lossless process frame marker, Huffman coding
	public static final int JPG  = 0xc8; // Undefined/reserved (causes decoding error) 
	public static final int SOF9 = 0xc9; // Sequential DCT frame marker, arithmetic coding
	public static final int SOF10= 0xca; // Progressive DCT frame marker, arithmetic coding
	public static final int SOF11= 0xcb; // Lossless process frame marker, arithmetic coding
	public static final int DAC  = 0xcc; // Define Arithmetic Table, usually unsupported 
	public static final int SOF13= 0xcd; // Differential sequential DCT frame marker, arithmetic coding
	public static final int SOF14= 0xce; // Differential progressive DCT frame marker, arithmetic coding
	public static final int SOF15= 0xcf; // Differential lossless process frame marker, arithmetic coding
	/**
	 * RSTn are used for resync, may be ignored, no length and other contents are
	 * associated with these markers. 
	 */
	public static final int RST0 = 0xd0;  
	public static final int RST1 = 0xd1;
	public static final int RST2 = 0xd2;
	public static final int RST3 = 0xd3;
	public static final int RST4 = 0xd4;
	public static final int RST5 = 0xd5;
	public static final int RST6 = 0xd6;
	public static final int RST7 = 0xd7;
	//End of RSTn definitions
	public static final int SOI  = 0xd8; // Start of image 
	public static final int EOI  = 0xd9; // End of image, the very last marker 
	public static final int SOS  = 0xda; // Start of scan
	public static final int DQT  = 0xdb; // Define Quantization table
	public static final int DNL  = 0xdc; // Define number of lines
	public static final int DRI  = 0xdd; // Define Restart Interval
	public static final int DHP  = 0xde; // Define hierarchical progression
	public static final int EXP  = 0xdf; // Expand reference components
	public static final int APP0 = 0xe0; // JFIF application segment
	public static final int APP1 = 0xe1; // EXIF application segment
	public static final int APP2 = 0xe2; // FPXR or ICC Profile data 
	public static final int APP3 = 0xe3; 
	public static final int APP4 = 0xe4; 
	public static final int APP5 = 0xe5; 
	public static final int APP6 = 0xe6; 
	public static final int APP7 = 0xe7; 
	public static final int APP8 = 0xe8; 
	public static final int APP9 = 0xe9; 
	public static final int APP10 =0xea; 
	public static final int APP11 =0xeb; 
	public static final int APP12 =0xec; 
	public static final int APP13 =0xed;// IPTC and Photoshop data 
	public static final int APP14 =0xee; 
	public static final int APP15= 0xef; 
	public static final int JPG0 = 0xf0; // Reserved
	// A lot more here      ...
	public static final int JPG13= 0xfd; // Reserved
	public static final int COM  = 0xfe; // Comment marker
	// End of JPEG marker definitions
	
	public static final byte[] ADOBE_ID = {0x41, 0x64, 0x6f, 0x62, 0x65}; //"Adobe" no trailing NULL
	// For JFIF there are normally two quantization tables, but for
	// other format there can be up to 4 quantization tables!
	private short quant_tbl[][] = new short[4][64];
	private int transform[][] = new int[8][8];
	private int block[] = new int[64];
	private int zigzag_array[] = new int[64];
		
	boolean finished = false;
	boolean reset_mcu = false;
	
	private int image_height = 0;
	private int image_width = 0;
	
	private int max_v_samp_factor = 0;
	private int max_h_samp_factor = 0;
	
	private HuffmanTbl dc_hufftbl[] = new HuffmanTbl[4];
	private HuffmanTbl ac_hufftbl[] = new HuffmanTbl[4];
	
	private int component[][] = new int[4][6];
	
	private SOFReader sofReader;
	
	@SuppressWarnings("unused")
	private HUF_NODE[][] dc_node;
	@SuppressWarnings("unused")
	private HUF_NODE[][] ac_node;
	
	@SuppressWarnings("unused")
	private int total_nodes;
	@SuppressWarnings("unused")
	private int[] dc_total_nodes;
	@SuppressWarnings("unused")
	private int[] ac_total_nodes;
	
	Image img_thumb;
	private boolean containsThumbnail = false;
	
	/**
	 * Used to keep track of the tables when reading SOS segment.
	 * Quantization and Huffman tables are comparable and can be sorted after
	 * all the table segments are read. 
	 */   
	@SuppressWarnings("unused")
	private List<QTable> qTables = new ArrayList<QTable>(4);
	@SuppressWarnings("unused")
	private List<HTable> acTables = new ArrayList<HTable>(4);
	@SuppressWarnings("unused")
	private List<HTable> dcTables = new ArrayList<HTable>(4);
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(JPEGReader.class);
	
	public BufferedImage read1(InputStream is) throws Exception	{
		finished = false;
		int identifier = -1;
		int marker = 0;
		
		// Read segment identifier
		identifier = is.read();
	            
		if (identifier == -1) {
			LOGGER.error("END OF FILE!");
			return null;
		}
		
		// The very first marker should be the start_of_image marker!	
		if((identifier != SEGMENT_IDENTIFIER)||((marker = is.read()) != SOI)) {
			LOGGER.error("Invalid JPEG image!");
			return null;
		}
		
		LOGGER.info("SOI(0xd8) - Start of image");
		
		while (!finished)
		{
			identifier = is.read(); // Keep reading segment identifiers
	            
			if (identifier == -1) {
				LOGGER.info("END OF FILE!");
				finished = true;
			} else if(identifier != SEGMENT_IDENTIFIER)	{
				LOGGER.warn("Invalid SEGMENT_IDENTIFIER!");
				finished  = true;
			} else {
				marker = is.read(); // Read marker	
				while(marker == SEGMENT_IDENTIFIER) {
					marker = is.read();
				}
				if(marker==0x00) continue;// This is not a marker
				// RSTs, there is no length byte and other contents
				// associated with these markers, skip...
				if ((marker >= 0xd0) && (marker <= 0xd7)) { ; }                              
				else if ((marker >= APP0) && (marker <= APP15))
					read_APP_Segment(marker, is);
				else {
					switch (marker)	{
						case SEGMENT_IDENTIFIER: break;
						case DQT:
							read_DQT_Segment(is);
							break;
						case DHT:
							read_DHT_Segment(is);
							break;
						case SOF0:
						case SOF2:
							read_SOF_Segment(is, marker);
							break;				        	 
						case SOS:
							read_SOS_Segment(is);
							break;
						case DRI:
							read_DRI_Segment(is);
							break;
						case EOI:
						case  -1:
							read_EOI_Segment();
							break;
						case COM:
							read_COM_Segment(is);
							break;
						default:
							read_Unknown_Segment(is, marker, IOUtils.readUnsignedShortMM(is));
							break;
					}// End of switch
				}
			}
		}
		
		return null;
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
	   
	// Process JFIF APPn header segment
	private boolean read_APP_Segment(int APPn, InputStream is) throws IOException {
		// Length of the JFIF header
		int len = IOUtils.readUnsignedShortMM(is);
		String identifier = "";
		
		switch(APPn) { 
			case APP0:
				byte[] jfif = {0x4A, 0x46, 0x49, 0x46, 0x00}; // JFIF
				LOGGER.info("APP0(0xe0) - Application segment 0");
				byte[] buf = new byte[len - 2];
			    IOUtils.readFully(is, buf);
			    // JFIF segment
			    if(Arrays.equals(ArrayUtils.subArray(buf, 0, 5), jfif)) {
			    	LOGGER.info("{} - version {}.{}", new String(buf, 0, 4), (buf[5]&0xff), (buf[6]&0xff));
			    	
			    	switch(buf[7]&0xff) {
			    		case 0:
			    			LOGGER.info("Density unit: No units, aspect ratio only specified");
			    			break;
			    		case 1:
			    			LOGGER.info("Density unit: Dots per inch");
			    			break;
			    		case 2:
			    			LOGGER.info("Density unit: Dots per centimeter");
			    			break;
			    		default:
			    	}
			    	
			    	LOGGER.info("X density: {}", IOUtils.readUnsignedShortMM(buf, 8));
			    	LOGGER.info("Y density: {}", IOUtils.readUnsignedShortMM(buf, 10));
			    	int thumbnailWidth = buf[12]&0xff;
			    	int thumbnailHeight = buf[13]&0xff;
			    	LOGGER.info("Thumbnail dimension: {}X{}", thumbnailWidth, thumbnailHeight);
			    	
			    	if(thumbnailWidth != 0 && thumbnailHeight != 0)	{
			    		containsThumbnail = true;
			    		generate_thumbnail(thumbnailWidth, thumbnailHeight, is);
			    	}	   
			    }
				break;
			case APP1: // Could be Exif or Adobe XMP
				LOGGER.info("APP1(0xe1) - Application segment 1"); 
				/** 
				 * This format is specific to digital cameras
				 * to insert information about camera setups to 
				 * the JPEG images,the specification is made by 
				 * Japanese producer.
				 * sometimes, there are more than one app1 segments found,
				 * this will cause some confusion, but it won't stop the decoder!
				 */
				buf = new byte[len-2];
				IOUtils.readFully(is, buf, 0, len-2);
				int identifier_len = 0;
				while ((buf[identifier_len]&0xff) != 0)	{
					identifier_len++;
				}
				identifier = new String(buf, 0, identifier_len, "UTF-8"); 
				LOGGER.info("[{}]", identifier);
				break;
			case APP2:
				// FPXR or ICC Profile data
				buf = new byte[len-2];
				IOUtils.readFully(is, buf, 0, len-2);
				identifier_len = 0;
				while ((buf[identifier_len]&0xff) != 0) {
					identifier_len++;
				}
				identifier = new String(buf, 0, identifier_len, "UTF-8");
				LOGGER.info("APP2(0xe2[{}]", identifier);
				break;
			case APP12: // [Ducky]
				// Some digital cameras store useful text information in APP12 markers.
				buf = new byte[len-2];
				IOUtils.readFully(is, buf, 0, len-2);
				identifier_len = 0;
				while ((buf[identifier_len]&0xff) != 0) {
					identifier_len++;
				}
				identifier = new String(buf, 0, identifier_len, "UTF-8");
				LOGGER.info("APP12(0xec[{}]", identifier);
				break;
			case APP13:// [Photoshop 3.0]
			// IPTC and Photoshop data
				buf = new byte[len-2];
				IOUtils.readFully(is, buf, 0, len-2);
				identifier_len = 0;
				while ((buf[identifier_len]&0xff) != 0) {
					identifier_len++;
				}
				identifier = new String(buf,0,identifier_len,"UTF-8");
				LOGGER.info("APP13(0xed[{}]", identifier);
				break;				
			case APP14:// [Adobe]
				buf = new byte[len-2];
				IOUtils.readFully(is, buf, 0, len-2);
				identifier_len = 0;
				while ((buf[identifier_len]&0xff) != 0)	{
					identifier_len++;
				}
				identifier = new String(buf, 0, identifier_len, "UTF-8");
				LOGGER.info("APP14(0xee[{}]", identifier);
				break;
			case APP15:
				buf = new byte[len-2];
				IOUtils.readFully(is,buf,0,len-2);
				identifier_len = 0;
				while ((buf[identifier_len]&0xff) != 0)	{
					identifier_len++;
				}
				identifier = new String(buf, 0, identifier_len, "UTF-8");
				LOGGER.info("APP15(0x{}): [{}]", Integer.toHexString(APP15), identifier);
				break;
			default:
				read_Unknown_Segment(is, APPn, len);
		}// End of switch!	
		
		return true;
	}
	    
	public boolean containsThumbnail()
	{
		return containsThumbnail;
	}
	   
	// Generate thumbnail view
	private void generate_thumbnail(int thumb_width,int thumb_height,InputStream is) throws IOException
	{
		int pix_thumb[]=new int[thumb_width*thumb_height];
		int index=0;
		int nindex=0;
		byte brgb[]=new byte[3*thumb_width];
		for(int i=1;i<=thumb_height;i++)
		{
			IOUtils.readFully(is,brgb,0,3*thumb_width);
			for(int j=0;j<thumb_width;j++)
			{
				pix_thumb[index++]=((0xff<<24)|((brgb[nindex++]&0xff)<<16))|((brgb[nindex++]&0xff)<<8)|(brgb[nindex++]&0xff);
			}
		}
		img_thumb = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(thumb_width, thumb_height, pix_thumb, 0, thumb_width));
		LOGGER.info("Thumbnail view created!");
   	}
	   
	// Process define Quantization table
	private void read_DQT_Segment(InputStream is) throws IOException {
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		DQTReader reader = new DQTReader(new Segment(Marker.DQT, len, buf));
		List<QTable> qTables = reader.getTables();
	
		int count = 0;
		
		StringBuilder qtTable = new StringBuilder();
		qtTable.append("DQT(0xdb) - Define quantization table segment\n");
		
		for(QTable table : qTables)	{
			int QT_precision = table.getPrecision();
			int QT_index = table.getIndex();
			short[] qTable = table.getTable();
			qtTable.append("precision of QT is " + QT_precision + "\n");
			qtTable.append("Quantization table #" + QT_index + ":\n");
			
			System.arraycopy(qTable, 0, quant_tbl[QT_index], 0, 64);
			
			if(QT_precision == 0) {
				for (int j = 0; j < 64; j++) {
					if (j != 0 && j%8 == 0) {
						qtTable.append("\n");
					}					
					qtTable.append((qTable[j]&0xff) + " ");			
			    }
			} else { // 16 bit big-endian								
				for (int j = 0; j < 64; j++) {
					if (j != 0 && j%8 == 0) {
						qtTable.append("\n");
					}					
					qtTable.append((qTable[j]&0xffff) + " ");	
				}				
			}
			qtTable.append("\n");
			
			count++;
		}
		qtTable.append("Total number of Quantation tables: " + count + "\n");
		qtTable.append("------------------------------------\n");
		LOGGER.info("\n", qtTable);		
	}
	   
	// Process define Huffman table
	private void read_DHT_Segment(InputStream is) throws IOException {
		// Define Huffman table segment
		final String[] HT_class_table = {"DC Component", "AC Component"};
		int len = IOUtils.readUnsignedShortMM(is);
        byte buf[] = new byte[len - 2];
        IOUtils.readFully(is, buf);
        
    	StringBuilder hufTable = new StringBuilder();
    	hufTable.append("DHT(0xc4) - Define Huffman table\n");
    	hufTable.append("DHT segment length: " + len + "\n");       	
        
		DHTReader reader = new DHTReader(new Segment(Marker.DHT, len, buf));
		
		List<HTable> dcTables = reader.getDCTables();
		List<HTable> acTables = reader.getACTables();
		
		for(HTable table : dcTables) {
			int destination_id = table.getDestinationID();
			dc_hufftbl[destination_id] = new HuffmanTbl(table.getBits(), table.getValues());
		}
		
		for(HTable table : acTables) {
			int destination_id = table.getDestinationID();
			ac_hufftbl[destination_id] = new HuffmanTbl(table.getBits(), table.getValues());
		}
		
		List<HTable> tables = new ArrayList<HTable>(dcTables);
		tables.addAll(acTables);
			
		for(HTable table : tables )
		{
			hufTable.append("Class: " + table.getComponentClass() + " (" + HT_class_table[table.getComponentClass()] + ")\n");
			hufTable.append("Destination ID: " + table.getDestinationID() + "\n");
				
			byte[] bits = table.getBits();
			byte[] values = table.getValues();
				
			int count = 0;
				
			for (int i = 0; i < bits.length; i++)
			{
				count += (bits[i]&0xff);
			}
				
			hufTable.append("Number of codes: " + count + "\n");
				
			if (count > 256)
			{
				LOGGER.error("invalid huffman code count!");			
				return;
			}
		        
			int j = 0;
	            
			for (int i = 0; i < 16; i++) {
				
				hufTable.append("Codes of length " + (i+1) + " (" + (bits[i]&0xff) +  " total): [ ");
					
				for (int k = 0; k < (bits[i]&0xff); k++) {
					hufTable.append((values[j++]&0xff) + " ");
				}
					
				hufTable.append("]\n");
			}
				
			hufTable.append("**********************************\n");
		}
		
		LOGGER.info("\n{}", hufTable);
	}
	
	private SOFReader readSOF(InputStream is, Marker marker) throws IOException {		
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		Segment segment = new Segment(marker, len, buf);		
		SOFReader reader = new SOFReader(segment);
		
		return reader;
	}	
	   
	// Process SOF segment
	private void read_SOF_Segment(InputStream is, int SOFn) throws IOException {
		LOGGER.info("SOF(0x{}{}) - Start of frame", Integer.toHexString(SOFn));
		int samp_factor = 0;
		int len = IOUtils.readUnsignedShortMM(is);
		// This is in bits/sample, usually 8, (12 and 16 not supported by most software). 
		int precision = is.read();// Usually 8, for baseline JPEG
		LOGGER.info("Data precision (bits/sample): {}", precision);
		if (precision != 8) {
			LOGGER.error("only 8 bit precision is surported!");
			finished = true;
			return;
		}
		// Image width and height
		image_height = IOUtils.readUnsignedShortMM(is);
		image_width = IOUtils.readUnsignedShortMM(is);
		LOGGER.info("Image size: {}X{}", image_width, image_height);
		// Number of components
		// Usually 1 = grey scaled, 3 = color YCbCr or YIQ, 4 = color CMYK 
		int num_of_components = is.read();
		LOGGER.info("number of components: {}", num_of_components);

		// JFIF uses either 1 component (Y, greyscaled) or 3 components (YCbCr, sometimes called YUV, color).
		for (int i = 0; i < num_of_components; i++)	{
			// Component ID(1 byte)(1 = Y, 2 = Cb, 3 = Cr, 4 = I, 5 = Q)
			int component_id = is.read();
			int index = component_id - 1;
			component[index][0] = component_id; 
			// Sampling factors (1byte) (bit 0-3 vertical, 4-7 horizontal).
			samp_factor = is.read();
			component[index][1] = (samp_factor>>4)&0x0f;//Horizontal			
			component[index][2] = (samp_factor&0x0f);//Vertical
			if (component[index][1] > max_h_samp_factor) {
				max_h_samp_factor = component[i][1];
			}
			if (component[index][2] > max_v_samp_factor) {
				max_v_samp_factor = component[i][2];
			}
			component[index][3] = is.read();// Quantization table number
			LOGGER.info("Component {}:", i);
			LOGGER.info("Component id: {}", component[index][0]);
			LOGGER.info("Component horizontal sampling factor: {}", component[index][1]);
			LOGGER.info("Component vertical sampling factor: {}", component[index][2]);		
			LOGGER.info("Component quantization table number: {}", component[index][3]);
		}
		
		IOUtils.skipFully(is, len-8-3*num_of_components);
		
		LOGGER.info("------------------------------------");

		if(SOFn == SOF2) {
			LOGGER.error("progressive DCT, Huffman coding not supported!");
			finished = true;
			return;		
		}
	}
	
	private void readSOS(InputStream is, SOFReader sofReader) throws IOException {
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(is, buf);
		
		Segment segment = new Segment(Marker.SOS, len, buf);
		new SOSReader(segment, sofReader);
	}
	   
	// Process start of scan 
	private void read_SOS_Segment(InputStream is) throws IOException {
		// Start of scan segment
		LOGGER.info("SOS(0xda");
			
		int tbl_no = 0;
		int Ss, Se, Ah_Al, Ah, Al;
		
		int len = IOUtils.readUnsignedShortMM(is);
		LOGGER.info("length is: {}", len);
		
		// The number of components in this scan
		int num_of_components = is.read();
		LOGGER.info("number of components in this scan: {}", num_of_components);
		len -= 3;
		
		for (int i = 0; i < num_of_components; i++)	{
			int component_id = is.read();// Component id
			tbl_no = is.read();
			component[component_id - 1][4] = (tbl_no>>4)&0x0f;// DC table number
			component[component_id - 1][5] = tbl_no&0x0f;// AC table number
			LOGGER.info("Component {}:", i);
			LOGGER.info("Component ID: {}", component_id);
			LOGGER.info("DC table number: {}", component[component_id - 1][4] );
			LOGGER.info("AC table number: {}", component[component_id - 1][5] );
		
			len -= 2;
		}
		Ss = is.read();// Start of spectral or predictor selection
		Se = is.read();// End of spectral selection		
		//LOGGER.info("Ss and Se are: {}&{}", Ss, Se);
		Ah_Al = is.read();
		Ah = (Ah_Al>>4)&0x0f;// Successive approximation bit position high
		Al = Ah_Al&0x0f;// Successive approximation bit position low or point transform
		IOUtils.skipFully(is, len-3);
		LOGGER.info("Ss: {}", Ss);
		LOGGER.info("Se: {}", Se);
		LOGGER.info("Ah: {}", Ah);
		LOGGER.info("Al: {}", Al);
		LOGGER.info("length remains: {}", (len-3));
		
		// Begin of the image data
		decode_image_data(is);
	}	
	   
	// Process define restart interval
	private void read_DRI_Segment(InputStream is) throws IOException {
		LOGGER.info("DRI(0xdd");
		IOUtils.skipFully(is, IOUtils.readUnsignedShortMM(is)-2);
	}	
	
	// Process unknown marker
	private void read_Unknown_Segment(InputStream is, int marker, int len) throws IOException {
		byte[] buf = new byte[len-2];
		IOUtils.readFully(is, buf, 0, len-2);
		int identifier_len = 0;
		while ((buf[identifier_len]&0xff) != 0)	{
			identifier_len++;
		}	
		String identifier = new String(buf,0,identifier_len,"UTF-8");	  	   
		if ((marker >= 0xe0) && (marker <= 0xef)) {
			LOGGER.info("Application type marker: 0x{}", Integer.toHexString(marker));
		} else {
			LOGGER.warn("Unrecognized marker: 0x{}", Integer.toHexString(marker));
		}
		LOGGER.info("with identifier: [{}]", identifier);
	}
	   
	// Process comment marker
	private void read_COM_Segment(InputStream is) throws IOException	{
		int len = IOUtils.readUnsignedShortMM(is);
		byte buf[] = new byte[len-2];
		IOUtils.readFully(is, buf, 0, len-2);
		LOGGER.info("Comment: {}", new String(buf,"UTF-8"));
	}	
	   
	// Process end of image marker
	private void read_EOI_Segment()	{
		LOGGER.info("END OF IMAGE!");
		finished = true;
	}
	
	/**
	 * @param is InputStream of the image
	 */
	private void decode_image_data(InputStream is) {
		LOGGER.info("Start Processing Image Data...");
		
	}
	
	@SuppressWarnings("unused")
	private byte getbit()
	{
		return 0;
	}
	
	@SuppressWarnings("unused")
	private void build_transform()
	{
		int x,y,k=0;
		for (y=0;y<8;y++)
			for (x=0;x<8;x++)
			{
				transform[x][y]=block[k];
				k++;		    
			}
	}
	   
	// Shift a signed 8*8 block to unsigned one 
	@SuppressWarnings("unused")
	private void shift_block(int k,int l,int m)
	{
		int x,y;
	
		for (x=0;x<8;x++)
			for (y=0;y<8;y++)
				;// component[l][m][k][x][y]+=128;
	}
	
	@SuppressWarnings("unused")
	private void idct(int k,int l,int m)
	{
		int x,y;
	
		for (x=0;x<8;x++)
			for (y=0;y<8;y++)
				;// component[l][m][k][x][y]=f(x,y);
	}
	
	@SuppressWarnings("unused")
	private int f(int x,int y)
	{
		int u,v;
		float sum=0;
	
		for (u=0;u<8;u++)
			for (v=0;v<8;v++)
				sum+=(C(u)*C(v))*transform[u][v]*(Math.cos(((2*x+1)*u*Math.PI)/16))*Math.cos(((2*y+1)*v*Math.PI)/16);
		return (int)((1.0/4.0)*sum);
	}
	
	private float C(int u)
	{
		if (u==0) return (float)(1.0/Math.sqrt(2));
		return 1.0f;
	}
	
	@SuppressWarnings("unused")
	private int YCbCr_to_RGB(int Y,int Cb,int Cr)
	{
		float red,green,blue;
	
		red = Y + 1.402f*(Cr-128);
		if (red<0) red = 0;
		if (red>255) red = 255;
		   
		green = Y - 0.34414f*(Cb-128)-0.71414f*(Cr-128);
		if (green<0) green = 0;
		if (green>255) green = 255;
		
		blue= Y + 1.772f*(Cb-128);
		if (blue<0) blue = 0;
		if (blue>255) blue = 255; 
		
		return ((0xff<<24)|((int)red<<16)|((int)green<<8)|(int)blue);
	}
	
	@SuppressWarnings("unused")
	private void setup_zigzag_array()
	{
		int i=0,j=0,x=0,y=0,k=0;
		   
		for (j=0;j<8;j++)
		{
			for (i=0;i<=j;i++)
			{
				if (j%2==0)
				{
					x=i;
					y=j-1;
				}
				else
				{
					x=j-i;
					y=i;
				}
				zigzag_array[k]=(y<<3)+x;
				k++;
			}
		}
	
		for (j=1;j<8;j++)
		{
			for (i=0;i<8-1-j;i++)
			{
				if (j%2==0)
				{
					x=8-1-i;
					y=j+i;
				}
				else
				{
					x=j+i;
					y=8-1-i; 
				}
				zigzag_array[k]=(y<<3)+x;
				k++;
			}
		}
	}
		
	class HUF_NODE
	{
		int code;
		int size;
		int value;
		int tree_index;
	}	
	
	@Override
	public BufferedImage read(InputStream is) throws Exception {
		return javax.imageio.ImageIO.read(is);
		//return read1(is);
	}
}