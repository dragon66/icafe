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
 * JPEGWriter.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    08Nov2015  Write Adobe APP14 segment for RGB color space
 * WY    21Jun2015  Removed copyright notice from generated JPEG images
 * WY    13Aug2014  Added support for YCCK JPEG image
 * WY    06Aug2014  Added writeAdobeApp14 to support CMYK image
 * WY    04Jun2014  Added ICC_Profile support
 * WY    03Jun2014  Added CMYK image support
 * WY    25Mar2014  Added AAN DCT support
 * WY    22Mar2014  Added support for grayScale image
 * WY    12Mar2014  First ever working version
 */

package com.icafe4j.image.writer;

import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.*; 
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.icafe4j.image.ImageColorType;
import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.compression.huffman.HuffmanEncoder;
import com.icafe4j.image.jpeg.HTable;
import com.icafe4j.image.jpeg.JPEGConsts;
import com.icafe4j.image.jpeg.Marker;
import com.icafe4j.image.jpeg.QTable;
import com.icafe4j.image.jpeg.Segment;
import com.icafe4j.image.options.ImageOptions;
import com.icafe4j.image.options.JPEGOptions;
import com.icafe4j.image.util.DCT;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.io.IOUtils;

/**
 * JPEG image writer  
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/02/2013
 */
public class JPEGWriter extends ImageWriter { 
	// The image width and height after image expansion 
  	private int newHeight;
	private int newWidth; 
	
	private ImageParam imageParam;
    private JPEGOptions jpegOptions;
    
    private int numOfComponents = 3; // Default number of components (3): YCrCb
    private int numOfQTables = 2; // Default number of quantization tables
    private int numOfHTables = 2; // Default number of Huffman tables for each class
    private int[] qTableSelector = new int[] {0, 1, 1, 1}; // q table selectors for different components
    
    private int[][] quant_table = new int[2][];
    private byte[][][] huffman_bits = new byte[2][2][]; // AC, DC
    private byte[][][] huffman_values = new byte[2][2][]; // AC, DC
    
	private int quality = 100; // Default image quality
	private boolean includeTables = true;
	private boolean grayScale;
	private int colorSpace = JPEGOptions.COLOR_SPACE_YCbCr;
	
	private static final String pathToCMYKProfile = "/resources/CMYK Profiles/USWebCoatedSWOP.icc";
	private ICC_ColorSpace cmykColorSpace;
	private boolean writeICCProfile;
	private boolean isTiffFlavor;
		
    private static final String comment = "Created by ICAFE - https://github.com/dragon66/icafe";
		   
	public JPEGWriter() {}
	
	public JPEGWriter(ImageParam param) {
		super(param);
	}
   	
	// Expand array to make image width and height a multiple of 8
	private float[][] expandArray(float[][] component, int width, int height) {
		int xpadding = width%8;
		int ypadding = height%8;
		newWidth = width + ((xpadding == 0)?0:(8-xpadding));
		newHeight = height + ((ypadding == 0)?0:(8-ypadding));
		
		if(newWidth > width || newHeight > height) {
			float[][] temp = new float[newHeight][newWidth];			
			// Expand width first
			for(int i = 0; i < height; i++) {
				System.arraycopy(component[i], 0, temp[i], 0, width);
				Arrays.fill(temp[i], width, newWidth, component[i][width - 1]);
			}
			// Expand height
			for(int k = height; k < newHeight; k++) {
				System.arraycopy(temp[height - 1], 0, temp[k], 0, newWidth);
			}
					
			return temp;
		}
		
		return component;		
	}
	
	private float[][] getDCTBlock(float[][] component, int x, int y) {
		//
		float[][] block = new float[8][8];
		
		for(int i = x, index = 0; i < 8 + x; i++, index++) {
			System.arraycopy(component[i], y, block[index], 0, 8);
		}
		
		return block;
	}
	
	public byte[] getCMYK_ICC_Profile() {
		if(cmykColorSpace != null)
			return cmykColorSpace.getProfile().getData();
		return null;
	}
	
	@Override
	public ImageType getImageType() {
		return ImageType.JPG;
	}
	
	// TODO: may need more changes to work with CMYK or RGB, and perhaps YCCK color space
	private void processImageMeta() throws Exception {
		// Grab the ImageParam
		imageParam = getImageParam();
		grayScale = imageParam.getColorType() == ImageColorType.GRAY_SCALE;
		ImageOptions options = imageParam.getImageOptions();
		// Read and set options if any
		if(options instanceof JPEGOptions) {
			jpegOptions = (JPEGOptions)options;
			quality = jpegOptions.getQuality();
			includeTables = jpegOptions.includeTables();
			colorSpace = jpegOptions.getColorSpace();
			isTiffFlavor = jpegOptions.isTiffFlavor();
			writeICCProfile = jpegOptions.writeICCProfile();
		}
		if(colorSpace == JPEGOptions.COLOR_SPACE_CMYK || colorSpace == JPEGOptions.COLOR_SPACE_YCCK) {
			numOfComponents = 4;
			if(cmykColorSpace == null)
				cmykColorSpace = IMGUtils.getICCColorSpace(pathToCMYKProfile);
		}
		// See if we are dealing with grayscale images		
		if(grayScale) {
			numOfComponents = 1;
			numOfQTables = 1;
			numOfHTables = 1;
		}
		// Set tables (Could be custom ones)
		setDefaultTables(quality);	
	}
	
	// Convert RGB to separate R, G, B with level shift (minus 128)
	private void RGB2RGB(int[] rgb, float[][] r, float[][] g, float[][] b, int imageWidth, int imageHeight) throws Exception {
		// TODO: Add down-sampling
		int red,green,blue, index = 0;
	
		for(int i = 0; i < imageHeight; i++) {
			for(int j = 0; j < imageWidth; j++) {
				red = ((rgb[index] >> 16) & 0xff);
				green = ((rgb[index] >> 8) & 0xff);
				blue = (rgb[index++] & 0xff);
				r[i][j] = red - 128.0f;
				g[i][j] = green - 128.0f;
				b[i][j] = blue - 128.0f;
			}
		}
	}
	
	// TODO: find a way to use different tables if CMYK or RGB color space is used
	// May need to change HuffmanEncoder accordingly.
	private void setDefaultTables(int quality) {
		// Set default quantization and Huffman tables
		quant_table[0] = JPEGConsts.getDefaultLuminanceMatrix(quality);
		quant_table[1] = JPEGConsts.getDefaultChrominanceMatrix(quality);			
		huffman_bits[0][0] = JPEGConsts.getDCLuminanceBits();
		huffman_bits[0][1] = JPEGConsts.getDCChrominanceBits();
		huffman_bits[1][0] = JPEGConsts.getACLuminanceBits();
		huffman_bits[1][1] = JPEGConsts.getACChrominanceBits();	
		huffman_values[0][0] = JPEGConsts.getDCLuminanceValues();
		huffman_values[0][1] = JPEGConsts.getDCChrominanceValues();
		huffman_values[1][0] = JPEGConsts.getACLuminanceValues();
		huffman_values[1][1] = JPEGConsts.getACChrominanceValues();	
	}
	
	protected void write(int[] pixels, int imageWidth, int imageHeight, OutputStream os) throws Exception {	
		// Read ImageParam and set parameters
		processImageMeta();	
		// Start of image marker
		writeSOI(os);
		if(colorSpace == JPEGOptions.COLOR_SPACE_YCbCr)			
			writeJFIF(os);// JFIF segment
		else
			writeAdobeApp14(os);
		if(colorSpace == JPEGOptions.COLOR_SPACE_CMYK || colorSpace == JPEGOptions.COLOR_SPACE_YCCK) {
			if(writeICCProfile)
				writeICCProfile(os);// Write ICC_Profile as APP2
		}		
		// Comments
        writeComment(comment, os);
      	// Write creation time comment
     	writeComment("Created on " + new SimpleDateFormat("yyyy:MM:dd HH:mm:ss z").format(new Date()), os);	
     	// Write JPEG tables if needed (for TIFF Technote2, JPEG tables can be written separately)
        if(includeTables) {	     	   
			// Write DQT
			writeDQT(os);
			// Write DHT
			writeDHT(os);
        }
        // Write SOF0
        writeSOF0(os, imageWidth, imageHeight);	
        // Write SOS
        writeSOS(os);
        // Write actual image stream
       	if(grayScale)		
			writeGrayScale(IMGUtils.rgb2grayscale(pixels, imageWidth, imageHeight), os, imageWidth, imageHeight);
		else
			writeFullColor(pixels, os, imageWidth, imageHeight);   
       	// Write EOI marker
        writeEOI(os);        
    }
	
	private void writeAdobeApp14(OutputStream os) throws Exception {
		int len = 14; // Expected length of this segment is 14.
		byte[] app14 = new byte[len + 2];
		// App14 marker
		app14[0] = (byte)0xff;
		app14[1] = (byte)0xee;
		app14[2] = (byte)((len>>8)&0xff);
		app14[3] = (byte)(len&0xff);
		// Adobe = {0x41, 0x64, 0x6f, 0x62, 0x65};
		app14[4] = 0x41;
		app14[5] = 0x64;
		app14[6] = 0x6f;
		app14[7] = 0x62;
		app14[8] = 0x65;
		
		app14[9]  = 0x00;
		app14[10] = 0x64;
		app14[11] = 0x00;
		app14[12] = 0x00;
		app14[13] = 0x00;
		app14[14] = 0x00;
		app14[15] = 0x00;
		
		if(colorSpace == JPEGOptions.COLOR_SPACE_YCbCr)
			app14[15] = 0x01;
		else if(colorSpace == JPEGOptions.COLOR_SPACE_YCCK)
			app14[15] = 0x02;
		
		os.write(app14);
	}
	
	private void writeComment(String comment, OutputStream os) throws Exception	{
		byte[] data = comment.getBytes();
		int len = data.length + 2;
		byte[] COM = new byte[len + 2];
		// Comment marker: 0xfffe
		COM[0] = (byte)0xff;
		COM[1] = (byte)0xfe;
		COM[2] = (byte)((len>>8)&0xff);
		COM[3] = (byte)(len&0xff);
		System.arraycopy(data, 0, COM, 4, len-2);
		os.write(COM, 0, len + 2);
	}
	
	private void writeDHT(HTable table, OutputStream os) throws Exception {
		// Write a single Huffman table
		int HT_class = table.getClazz();
		int HT_destination_id = table.getID();
		byte[] bits = table.getBits();
		byte[] values = table.getValues();
		
		int noOfCodes = 0;
		
		for(int i: bits) noOfCodes +=i;
		
		byte[] temp = new byte[1 + bits.length + noOfCodes];
		
		System.arraycopy(bits, 0, temp, 1, bits.length);
		System.arraycopy(values, 0, temp, bits.length + 1, noOfCodes);
		
		temp[0] = (byte)(((HT_class<<4)&0xf0)|(HT_destination_id&0x0f));
		
		new Segment(Marker.DHT, temp.length + 2, temp).write(os);
	}
	
	// Write DHT using the default Huffman tables
	private void writeDHT(OutputStream os) throws Exception {
		// We can write custom DHT tables or standard tables
		// Standard tables for DC and AC respectively
		for(int i = 0; i < numOfHTables; i++) {
				writeDHT(new HTable(0, i, huffman_bits[0][i], huffman_values[0][i]), os);
				writeDHT(new HTable(1, i, huffman_bits[1][i], huffman_values[1][i]), os);
		}
	}
	
	// Write DQT using the default quantization tables
	private void writeDQT(OutputStream os) throws Exception {
		// We can write custom DQT tables or standard tables
		// Standard tables for luminance and chrominance respectively
		for(int i = 0; i < numOfQTables; i++) {
			writeDQT(new QTable(0, i, quant_table[i]), os);
		}		
	}
	
	private void writeDQT(QTable table, OutputStream os) throws Exception {
		// Write a single quantization table
		int precision = table.getPrecision();
		int index     = table.getID();
		int[] data    = table.getData();
		int[] zigzagOrder = JPEGConsts.getZigzagMatrix(); 
		byte[] dqt;
		
		if(precision == 0) { // 8 bits
			dqt = new byte[1+data.length];
						
			for(int i = 1; i < dqt.length; i++) {
				dqt[i] = (byte)data[zigzagOrder[i-1]];
			}			
		} else {
			dqt = new byte[1+data.length*2];			
			
			for(int i = 1; i < data.length; i++) {
				dqt[i] = (byte)(data[zigzagOrder[i-1]]>>8);
				dqt[i+1] = (byte)(data[zigzagOrder[i-1]]);
			}
		}
		
		dqt[0] = (byte)((index&0x0f)|((precision<<4)&0xf0));
			
		new Segment(Marker.DQT, dqt.length + 2, dqt).write(os);
	}
	
	private void writeEOI(OutputStream os) throws Exception	{
		byte[] EOI = {(byte)0xff, (byte)0xd9};
		os.write(EOI);
	}
	
	private void writeGrayScale(float[][] pixels, OutputStream os, int imageWidth, int imageHeight) throws Exception {
		// Expand image if needed
		pixels = expandArray(pixels, imageWidth, imageHeight);
		
		// DCT transform and Huffman encoding
		HuffmanEncoder encoder = new HuffmanEncoder(os, 4096);
		// If we are going to use custom encoder tables, call encoder.setEncodingTables() here before
		// calling encoder.initialize() which will skip the default encoding tables generation
		encoder.initialize();
		
		for(int i = 0; i < newHeight; i+=8) {
			for(int j = 0; j < newWidth; j+=8) {
			    float[][] block = getDCTBlock(pixels, i, j);
				// DCT transform
				block = DCT.forwardDCT(block);
				int[] unzigzagBlock = new int[64];
				// Natural order block and quantization
				for(int k = 0, index = 0; k < 8; k++) {
					for(int l = 0; l < 8; l++, index++) 
						unzigzagBlock[index] = (int)block[k][l]/quant_table[0][index];
				}				
				encoder.encode(unzigzagBlock, 0);								
			}
		}
		
		encoder.finish();
	}
	
	private void writeICCProfile(OutputStream os) throws Exception {
		ICC_Profile icc_profile = cmykColorSpace.getProfile();
		writeICCProfile(os, icc_profile.getData());
	}
	
	private void writeICCProfile(OutputStream os, byte[] data) throws Exception {
		// ICC_Profile ID
		final String ICC_PROFILE_ID = "ICC_PROFILE\0";
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
	
	// Write actual image data
	private void writeFullColor(int[] pixels, OutputStream os, int imageWidth, int imageHeight) throws Exception {
		// Create arrays according to number of color components
		float[][][] c = new float[numOfComponents][imageHeight][imageWidth];		
		// Determine the color space to use
		if(colorSpace == JPEGOptions.COLOR_SPACE_YCbCr) {
			// RGB to YCbCr transform
			IMGUtils.RGB2YCbCr(pixels, c[0], c[1], c[2], imageWidth, imageHeight);
		} else if(colorSpace == JPEGOptions.COLOR_SPACE_RGB) {
			// RGB to separate R, G, B transform with level shifting
			RGB2RGB(pixels, c[0], c[1], c[2], imageWidth, imageHeight);
		} else if(colorSpace == JPEGOptions.COLOR_SPACE_CMYK) {
			if(!isTiffFlavor) // All the software tends to believe JPEG CMYK is inverted!
				IMGUtils.RGB2CMYK_Inverted(cmykColorSpace, pixels, c[0], c[1], c[2], c[3], imageWidth, imageHeight);
			else
				IMGUtils.RGB2CMYK(cmykColorSpace, pixels, c[0], c[1], c[2], c[3], imageWidth, imageHeight);
		} else if(colorSpace == JPEGOptions.COLOR_SPACE_YCCK) {
			if(!isTiffFlavor) // All the software tends to believe JPEG YCCK is inverted!
				IMGUtils.RGB2YCCK_Inverted(cmykColorSpace, pixels, c[0], c[1], c[2], c[3], imageWidth, imageHeight);
			else
				throw new UnsupportedOperationException("YCCK JPEG is not supported in TIFF!");
		} else {
			throw new IllegalArgumentException("Unsupported color space type: " + colorSpace);
		}		
		// Expand image if needed
		for(int i = 0; i < numOfComponents; i++)
			c[i] = expandArray(c[i], imageWidth, imageHeight);
		// DCT transform and Huffman encoding
		HuffmanEncoder encoder = new HuffmanEncoder(os, 4096);
		// If we are going to use custom encoder tables, call encoder.setEncodingTables() here before
		// calling encoder.prepare() which will skip the default encoding tables generation
		encoder.initialize();
		
		for(int i = 0; i < newHeight; i+=8) {
			for(int j = 0; j < newWidth; j+=8) {
			   for(int k = 0; k < numOfComponents; k++) {
				   int[] q = quant_table[qTableSelector[k]];
				   float[][] block = getDCTBlock(c[k], i, j);
				   // DCT transform
				   block = DCT.forwardDCT(block);
				   int[] unzigzagBlock = new int[64];
				   // Natural order block and quantization
				   for(int l = 0, index = 0; l < 8; l++) {
					   for(int m = 0; m < 8; m++, index++) 
						   unzigzagBlock[index] = (int)block[l][m]/q[index];
				   }				
				   encoder.encode(unzigzagBlock, k);
			   }		
			}
		}
		
		encoder.finish();
	}
	
	private void writeJFIF(OutputStream os) throws Exception {
		// App0 segment
		byte[] JFIF = new byte[18];
		// JFIF marker: 0xffe0
		JFIF[0] = (byte)0xff;
		JFIF[1] = (byte)0xe0;
		// Write length: 16
        JFIF[2] = 0x00;
		JFIF[3] = 0x10;
		// JFIF identifier: JFIF0 
		JFIF[4] = 'J';
		JFIF[5] = 'F';
		JFIF[6] = 'I';
		JFIF[7] = 'F';
		JFIF[8] = 0x00;
		// Revision number, 1.0
		JFIF[9] = 0x01;
        JFIF[10] = 0x00;
		/**
		 * Units for x/y densities
		 * 0 = no units, x/y-density specify the aspect ratio instead 
         * 1 = x/y-density are dots/inch 
         * 2 = x/y-density are dots/cm 
         */
        JFIF[11] = 0x00;
		// X/Y densities
        JFIF[12] = 0x00;
        JFIF[13] = 0x01;
        JFIF[14] = 0x00;
        JFIF[15] = 0x01;
        // Thumbnail size
		JFIF[16] = 0x00;// width
        JFIF[17] = 0x00;// height

		os.write(JFIF);
	}
	
	/**
	 * Write default JPEG tables for TIFF image only. The following are allowed markers.
	 * But the only useful markers are SOI, EOI, DQT, and DHT. 
	 * SOI
	 * DQT
	 * DHT
	 * DAC (not to appear unless arithmetic coding is used)
	 * DRI (no-op)
	 * APPn (shall be ignored by TIFF readers)
	 * COM	(shall be ignored by TIFF readers) 
	 * EOI
	 */
	public void writeDefaultJPEGTables(OutputStream os) throws Exception {
		if(imageParam == null)
			processImageMeta();
		writeSOI(os);
		writeDQT(os);
		writeDHT(os);
		writeEOI(os);		
	}
	
	private void writeSOF0(OutputStream os, int imageWidth, int imageHeight) throws Exception {
		// SOF0 segment
		int length = 8 + 3*numOfComponents;
		byte SOF[] = new byte[length + 2];
	    // Marker: 0xffc0
		SOF[0] = (byte) 0xFF;
		SOF[1] = (byte) 0xC0;
		// Write length
		SOF[2] = (byte) (length >> 8);
		SOF[3] = (byte) length;
		// Precision
		SOF[4] = 8;
		// Image height
		SOF[5] = (byte) ((imageHeight >> 8) & 0xFF);
		SOF[6] = (byte) ((imageHeight) & 0xFF);
		// Image width
		SOF[7] = (byte) ((imageWidth >> 8) & 0xFF);
		SOF[8] = (byte) ((imageWidth) & 0xFF);
		SOF[9] = (byte)numOfComponents;
		
		int offset = 10;
		
		for(int i = 0; i < numOfComponents; i++) {
			SOF[offset++] = (byte) (i+1);
			SOF[offset++] = (byte) ((1 << 4) + 1);
			SOF[offset++] = (byte) qTableSelector[i];
		}
		
		// Serialize SOF0
		os.write(SOF);
	}
	
	private void writeSOI(OutputStream os) throws Exception {
		byte[] SOI = {(byte)0xff, (byte)0xd8};
		os.write(SOI);
	}
	
	private void writeSOS(OutputStream os) throws Exception {
		//
		int length = 6 + 2*numOfComponents;		
		byte SOS[] = new byte[length + 2];
		
		SOS[0] = (byte) 0xFF;
		SOS[1] = (byte) 0xDA;
		SOS[2] = (byte) (length >> 8);
		SOS[3] = (byte) length;
		SOS[4] = (byte) numOfComponents;
		
		int offset = 5;
		
		for(int i = 0; i < numOfComponents; i++) {
			SOS[offset++] = (byte) (i+1);
			SOS[offset++] = (byte) ((qTableSelector[i] << 4) + qTableSelector[i]);
		}
		
        SOS[offset++] = (byte) 0;
        SOS[offset++] = (byte) 63;
        SOS[offset++] = (byte) 0;
        
        os.write(SOS);
	}
}