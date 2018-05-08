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
 * TIFFReader.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    03Jan2018  Fix issue with fillOrder 2
 * WY    07Dec2017  Added support for CCITTRLE compression
 * WY    28Nov2017  Added gray-scale alpha support
 * WY    23Nov2017  Added support for 16 bits gray-scale
 * WY    22Nov2017  Added support for BlackIsZero and WhiteIsZero
 * WY    09Nov2015  Fixed bug with stripped CMYK decoding
 * WY    13Sep2015  Extract unpackStrip() method
 * WY    08Jan2015  Better exception handling to resume from failed frame decoding 
 * WY    06Jan2015  Enhancement to decode multipage TIFF 
 * WY    10Dec2014  Fixed bug for bitsPerSample%8 != 0 case to assume BIG_ENDIAN  
 * WY    09Dec2014  Added support for LSB2MSB FillOrder for RGB image
 * WY    07Dec2014  Added support for floating point sample data type
 * WY    03Dec2014  Code clean and test showing float type raster images
 * WY    01Dec2014  Added support for less than 8 bits planar stripped image
 * WY    28Nov2014  Added support for 32 bits tiled image
 * WY    14Nov2014  Added support for tiled Palette Image
 * WY    07Nov2014  Added support for 16 bit CMYK image
 * WY    06Nov2014  Planar support for stripped YCbCr image
 * WY    05Nov2014  Fixed bug for YCbCr image with wrong image width and height
 * WY    31Oct2014  Added basic support for uncompressed and LZW compressed YCbCr
 * WY    15Oct2014  Added basic support for 16 bits RGB image
 * WY    14Oct2014  Revised to show specification violation TIFF LZW compression
 * WY    14Oct2014  Revised to show RGB TIFF with extra sample tag
 */

package com.icafe4j.image.reader;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.color.CMYKColorSpace;
import com.icafe4j.image.color.Int32ComponentColorModel;
import com.icafe4j.image.compression.ImageDecoder;
import com.icafe4j.image.compression.UnsupportedCompressionException;
import com.icafe4j.image.compression.ccitt.G31DDecoder;
import com.icafe4j.image.compression.ccitt.G32DDecoder;
import com.icafe4j.image.compression.deflate.DeflateDecoder;
import com.icafe4j.image.compression.lzw.LZWTreeDecoder;
import com.icafe4j.image.compression.packbits.Packbits;
import com.icafe4j.image.tiff.ASCIIField;
import com.icafe4j.image.tiff.ByteField;
import com.icafe4j.image.tiff.DoubleField;
import com.icafe4j.image.tiff.FieldType;
import com.icafe4j.image.tiff.FloatField;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.LongField;
import com.icafe4j.image.tiff.RationalField;
import com.icafe4j.image.tiff.ShortField;
import com.icafe4j.image.tiff.TIFFTweaker;
import com.icafe4j.image.tiff.Tag;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffFieldEnum;
import com.icafe4j.image.tiff.TiffFieldEnum.PhotoMetric;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.image.tiff.UndefinedField;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.IOUtils;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.io.ReadStrategyII;
import com.icafe4j.io.ReadStrategyMM;
import com.icafe4j.string.StringUtils;
import com.icafe4j.util.ArrayUtils;

/** 
 * Decodes and shows TIFF images. 
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/09/2012  
 */
public class TIFFReader extends ImageReader {
	private RandomAccessInputStream randIS = null;
	private List<IFD> list = new ArrayList<IFD>();
	private List<BufferedImage> frames;
	private int endian = IOUtils.BIG_ENDIAN;
	private static final int[] redMask =   {0x00, 0x04, 0x30, 0x1c0, 0xf00};
	private static final int[] greenMask = {0x00, 0x02, 0x0c, 0x038, 0x0f0};
	private static final int[] blueMask =  {0x00, 0x01, 0x03, 0x007, 0x00f};	
	 
	private static final int[] BLACK_WHITE_PALETTE = {0xFF000000, 0xFFFFFFFF};
	private static final int[] BLACK_WHITE_PALETTE_WHITE_IS_ZERO = {0xFFFFFFFF, 0xFF000000};
	private static final int[] FOUR_COLOR_PALETTE = {0xFF000000, 0xFF404040, 0xFF808080, 0xFFFFFFFF};
	private static final int[] FOUR_COLOR_PALETTE_WHITE_IS_ZERO = {0xFFFFFFFF, 0xFF808080, 0xFF404040, 0xFF000000};
	private static final int[] SIXTEEN_COLOR_PALETTE = {0xFF000000, 0xFF111111, 0xFF222222, 0xFF333333,
		 											   0xFF444444, 0xFF555555, 0xFF666666, 0xFF777777,
		 											   0xFF888888, 0xFF999999, 0xFFAAAAAA, 0xFFBBBBBB,
		 											   0xFFCCCCCC, 0xFFDDDDDD, 0xFFEEEEEE, 0xFFFFFFFF};
	private static final int[] SIXTEEN_COLOR_PALETTE_WHITE_IS_ZERO = {0xFFFFFFFF, 0xFFEEEEEE, 0xFFDDDDDD, 0xFFCCCCCC, 
													   0xFFBBBBBB, 0xFFAAAAAA, 0xFF999999, 0xFF888888, 0xFF777777,
													   0xFF666666, 0xFF555555, 0xFF444444, 0xFF333333, 0xFF222222,
													   0xFF111111, 0xFF000000};
	private static final int[] EIGHT_BIT_COLOR_PALETTE = new int[256];
	private static final int[] EIGHT_BIT_COLOR_PALETTE_WHITE_IS_ZERO = new int[256];
	
	private static int GROUP3OPT_2DENCODING = 1;
	private static int GROUP3OPT_UNCOMPRESSED = 2;

	static {
		for(int i = 0; i < 256; i++)
			EIGHT_BIT_COLOR_PALETTE[i] = 0xFF000000|(i<<16)|(i<<8)|(i&0xff);
	}
	
	static {
		for(int i = 0; i < 256; i++)
			EIGHT_BIT_COLOR_PALETTE_WHITE_IS_ZERO[255 - i] = 0xFF000000|(i<<16)|(i<<8)|(i&0xff);
	}
	
	private static final int bufLen = 40960; // 40K read buffer
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TIFFReader.class);		
		 
	public BufferedImage read(InputStream is) throws Exception {
		randIS = new FileCacheRandomAccessInputStream(is, bufLen);
		if(!readHeader(randIS)) return null;
		
		frames = new ArrayList<BufferedImage>();
		 
		int offset = randIS.readInt();
		
		int ifd = 0;
				
		while (offset != 0)	{
			try {
				offset = readIFD(ifd++, offset);
			} catch(Exception ex) {
				ex.printStackTrace();
				break;
			}
		}
		
		BufferedImage frame = null;
		
		for(IFD page : list) {
			try {
				frame = decode(page);
			} catch(Exception ex) {
				ex.printStackTrace();
				continue;
			}
			if(frame != null)
				frames.add(frame);
		}		
		
		randIS.shallowClose();
		if(frames.size() > 0)
			return frames.get(0);
		return null;
	}
	 
	private BufferedImage decode(IFD ifd) throws Exception {
		// Grab some of the TIFF fields we are interested in
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH);
		TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH);
		if(f_tileWidth != null && f_tileLength != null)
			return decodeTiledTiff(ifd);
		return decodeStrippedTiff(ifd);
	}
	
	private BufferedImage decodeStrippedTiff(IFD ifd) throws Exception {
		// Grab some of the TIFF fields we are interested in
		TiffField<?> f_compression = ifd.getField(TiffTag.COMPRESSION);
		short[] data = new short[]{1}; // Default no compression
		if(f_compression != null)
			data = (short[])f_compression.getData();
		TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(data[0]&0xffff);
		LOGGER.info("Compression type: {}", compression.getDescription());
		// Forget about tiled TIFF for now
		TiffField<?> f_stripOffsets = ifd.getField(TiffTag.STRIP_OFFSETS);
		TiffField<?> f_stripByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS);
		if(f_stripOffsets == null) throw new RuntimeException("Missing required field stripOffsets");
		int[] stripOffsets = f_stripOffsets.getDataAsLong();
		int[] stripByteCounts = null;
		if(f_stripByteCounts == null) {
			if(stripOffsets.length == 1) {
				stripByteCounts = new int[]{0};
			} else throw new RuntimeException("Missing required field stripByteCounts");
		} else
			stripByteCounts = f_stripByteCounts.getDataAsLong();
		int imageWidth = ifd.getField(TiffTag.IMAGE_WIDTH).getDataAsLong()[0];
		int imageHeight = ifd.getField(TiffTag.IMAGE_LENGTH).getDataAsLong()[0];
		LOGGER.info("Image width: {}", imageWidth);
		LOGGER.info("Image height: {}", imageHeight);
		TiffField<?> f_rowsPerStrip = ifd.getField(TiffTag.ROWS_PER_STRIP);
		int rowsPerStrip = imageHeight;
		if(f_rowsPerStrip != null)
			rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];
		LOGGER.info("Rows per strip: {}", rowsPerStrip);		
		TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION);
		int photoMetric = PhotoMetric.WHITE_IS_ZERO.getValue();
		if(f_photoMetric != null)
			photoMetric = f_photoMetric.getDataAsLong()[0];
		TiffFieldEnum.PhotoMetric e_photoMetric = TiffFieldEnum.PhotoMetric.fromValue(photoMetric);
		LOGGER.info("PhotoMetric: {}", e_photoMetric);
		TiffField<?> f_bitsPerSample = ifd.getField(TiffTag.BITS_PER_SAMPLE);
		int bitsPerSample = 1;
		if(f_bitsPerSample != null)
			bitsPerSample = f_bitsPerSample.getDataAsLong()[0];
		LOGGER.info("Bits per sample: {}", bitsPerSample);
		TiffField<?> f_samplesPerPixel = ifd.getField(TiffTag.SAMPLES_PER_PIXEL);
		int samplesPerPixel = 1;
		if(f_samplesPerPixel != null)
			samplesPerPixel = f_samplesPerPixel.getDataAsLong()[0];
		LOGGER.info("Samples per pixel: {}", samplesPerPixel);
		TiffField<?> f_predictor = ifd.getField(TiffTag.PREDICTOR);
		int predictor = 0;
		if(f_predictor != null) {
			predictor = f_predictor.getDataAsLong()[0];
			LOGGER.info("Predictor: {}", predictor);
		}
		TiffField<?> f_planaryConfiguration = ifd.getField(TiffTag.PLANAR_CONFIGURATTION);
		int planaryConfiguration = 1;
		if(f_planaryConfiguration != null) planaryConfiguration = f_planaryConfiguration.getDataAsLong()[0];
		TiffFieldEnum.PlanarConfiguration e_planaryConfiguration = TiffFieldEnum.PlanarConfiguration.fromValue(planaryConfiguration);
		LOGGER.info("Planary configuration: {}", e_planaryConfiguration);
		
		TiffField<?> f_sampleFormat = ifd.getField(TiffTag.SAMPLE_FORMAT);
		TiffField<?> f_sampleMaxValue = ifd.getField(TiffTag.S_MAX_SAMPLE_VALUE);
		TiffField<?> f_sampleMinValue = ifd.getField(TiffTag.S_MIN_SAMPLE_VALUE);
		
		int fillOrder = 1;
		TiffField<?> f_fillOrder = ifd.getField(TiffTag.FILL_ORDER);
		if(f_fillOrder != null) fillOrder = f_fillOrder.getDataAsLong()[0];
		
		boolean floatSample = false;
		if(f_sampleFormat != null && f_sampleFormat.getDataAsLong()[0] == 3) { // Floating point sample data type
			floatSample = true;
			double maxValue = (bitsPerSample <= 32)? Float.MAX_VALUE:Double.MAX_VALUE;
			double minValue = (bitsPerSample <= 32)? Float.MIN_VALUE:Double.MIN_VALUE;
			if(bitsPerSample <= 32 && f_sampleMaxValue != null) {
				maxValue = ((float[])f_sampleMaxValue.getData())[0];
			} else if(bitsPerSample > 32 && f_sampleMaxValue != null) {
				maxValue = ((double[])f_sampleMaxValue.getData())[0];
			}	
			if(bitsPerSample <= 32 && f_sampleMinValue != null) {
				minValue = ((float[])f_sampleMinValue.getData())[0];
			} else if(bitsPerSample > 32 && f_sampleMinValue != null) {
				minValue = ((double[])f_sampleMinValue.getData())[0];
			}				
			LOGGER.info("Sample MAX value: {}", maxValue);
			LOGGER.info("Sample MIN vlaue: {}", minValue);
		}
		boolean transparent = false;
		boolean isAssociatedAlpha = false;
		int numOfBands = samplesPerPixel;
		int trans = Transparency.OPAQUE;
		
		TiffField<?> f_extraSamples = ifd.getField(TiffTag.EXTRA_SAMPLES);
		if(f_extraSamples != null) isAssociatedAlpha = (f_extraSamples.getDataAsLong()[0] == 1)? true:false;
		
		int offset = 0;
		
		ImageDecoder decoder = null;
		byte[] pixels = null;
		int[] stripBytes = TIFFTweaker.getUncompressedStripByteCounts(ifd, stripOffsets.length);
		
		int[] count = new int[samplesPerPixel];
	
		if(planaryConfiguration == 2) {
			int inc = stripBytes.length/samplesPerPixel;
			int startOff = 0;
			int endOff = inc;
			for(int k = 0; k < samplesPerPixel; k++) {
				for(int j = startOff; j < endOff; j++) count[k] += stripBytes[j];
				startOff += inc;
				endOff += inc ;
			}
		}
		
		switch(e_photoMetric) {
			case PALETTE_COLOR:
				short[] colorMap = (short[])ifd.getField(TiffTag.COLORMAP).getData();
				rgbColorPalette = new int[colorMap.length/3];
				int numOfColors = (1<<bitsPerSample);
				int numOfColors2 = (numOfColors<<1);
				for(int i = 0, index = 0; i < colorMap.length/3;i++) {
					rgbColorPalette[index++] = 0xff000000|((colorMap[i]&0xff00)<<8)|((colorMap[i+numOfColors]&0xff00))|((colorMap[i+numOfColors2]&0xff00)>>8) ;
				}
				int bytesPerScanLine = (imageWidth*bitsPerSample + 7)/8;
				pixels = new byte[bytesPerScanLine*imageHeight];				
				switch(compression) {
					case NONE:
						for(int i = 0; i < stripByteCounts.length; i++) {
							int bytes2Read = stripBytes[i];
							randIS.seek(stripOffsets[i]);
							randIS.readFully(pixels, offset, bytes2Read);
							offset += bytes2Read;
						}
						break;
					case LZW:
						decoder = new LZWTreeDecoder(8, true);
						break;
					case DEFLATE:
					case DEFLATE_ADOBE:
						decoder = new DeflateDecoder();
						break;
					case PACKBITS:
						for(int i = 0; i < stripByteCounts.length; i++) {
							int bytes2Read = stripBytes[i];
							unpackStrip(pixels, offset, stripBytes[i], stripOffsets[i], stripByteCounts[i]);
							offset += bytes2Read;
						}
						break;
					default:
				}
				if(decoder != null) {
					for(int i = 0; i < stripByteCounts.length; i++) {
						byte[] temp = null;
						randIS.seek(stripOffsets[i]);
						if(stripByteCounts[i] == 0){
							temp = IOUtils.readFully(randIS, 4096);
						} else {
							temp = new byte[stripByteCounts[i]];
							randIS.readFully(temp);
						}
						decoder.setInput(temp);
						int numOfBytes = decoder.decode(pixels, offset, stripBytes[i]);
						offset += numOfBytes;
					}
				}
				//Create a BufferedImage
				DataBuffer db = new DataBufferByte(pixels, pixels.length);
				WritableRaster raster = null;
				if(bitsPerSample != 8) {
					raster = Raster.createPackedRaster(db, imageWidth, imageHeight, bitsPerSample, null);
				} else {
					int[] off = {0};//band offset, we have only one band start at 0
					raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth, 1, off, null);
				}
				ColorModel cm = new IndexColorModel(bitsPerSample, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
				   
				return new BufferedImage(cm, raster, false, null);
			case SEPARATED:
				// Hopefully CMYK
				bytesPerScanLine = samplesPerPixel*((imageWidth*bitsPerSample + 7)/8);
				int totalBytes = bytesPerScanLine*imageHeight;
				if(planaryConfiguration == 2) bytesPerScanLine = (imageWidth*bitsPerSample + 7)/8;
				pixels = new byte[totalBytes];
				switch(compression) {
					case NONE:
						for(int i = 0; i < stripByteCounts.length; i++) {
							int bytes2Read = stripBytes[i];
							randIS.seek(stripOffsets[i]);
							randIS.readFully(pixels, offset, bytes2Read);
							offset += bytes2Read;
						}	
						break;
					case PACKBITS:
						for(int i = 0; i < stripByteCounts.length; i++) {
							int bytes2Read = stripBytes[i];
							unpackStrip(pixels, offset, stripBytes[i], stripOffsets[i], stripByteCounts[i]);
							offset += bytes2Read;
						}
						break;
					case LZW:
						decoder = new LZWTreeDecoder(8, true);
						break;
					case DEFLATE:
					case DEFLATE_ADOBE:
						decoder = new DeflateDecoder();
						break;
					default:
						break;
				}
				if(decoder != null) {
					for(int i = 0; i < stripByteCounts.length; i++) {
						randIS.seek(stripOffsets[i]);
						byte[] temp = null;
						if(stripByteCounts[i] == 0){
							temp = IOUtils.readFully(randIS, 4096);
						} else {
							temp = new byte[stripByteCounts[i]];
							randIS.readFully(temp);
						}
						decoder.setInput(temp);
						int numOfBytes = decoder.decode(pixels, offset, stripBytes[i]);							
						offset += numOfBytes;
					}
				}
				// This also works with 4 samples per pixel data
				if(predictor == 2 && planaryConfiguration == 1)
					pixels = applyDePredictor(samplesPerPixel, pixels, imageWidth, imageHeight);
				//Create a BufferedImage
				db = new DataBufferByte(pixels, pixels.length);
				// Get ICC_Profile
				TiffField<?> f_colorProfile = ifd.getField(TiffTag.ICC_PROFILE);
				ICC_Profile profile = null;
				if(f_colorProfile != null) profile = ICC_Profile.getInstance((byte[])f_colorProfile.getData());
				ColorSpace colorSpace  = CMYKColorSpace.getInstance();
				if(profile != null) colorSpace = new ICC_ColorSpace(profile);			
				//band offset, we have 4 bands if no extra sample is specified, otherwise 5
				int[] bandoff = {0, 1, 2, 3}; 
				int[] nBits = {bitsPerSample, bitsPerSample, bitsPerSample, bitsPerSample};
				// There is an extra sample (most probably alpha)
				if(samplesPerPixel >= 5) {
					bandoff = new int[]{0, 1, 2, 3, 4};
					nBits = new int[]{bitsPerSample, bitsPerSample, bitsPerSample, bitsPerSample, bitsPerSample};
					trans = Transparency.TRANSLUCENT;
					transparent = true;
				}				
				if(bitsPerSample == 16) {
					short[] spixels = ArrayUtils.toShortArray(pixels, endian == IOUtils.BIG_ENDIAN);
					db = new DataBufferUShort(spixels, spixels.length);
					cm = new ComponentColorModel(colorSpace, nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_USHORT);
					if(planaryConfiguration == 2) {
						bandoff = new int[]{0, count[0]*8/bitsPerSample, (count[0] + count[1])*8/bitsPerSample, (count[0] + count[1] + count[2])*8/bitsPerSample};
						int[] bankIndices = new int[]{0, 0, 0, 0};
						if(samplesPerPixel >= 5) {
							bandoff = new int[]{0, count[0], count[0] + count[1], count[0] + count[1] + count[2], count[0] + count[1] + count[2] + count[3]};
							bankIndices = new int[]{0, 0, 0, 0, 0};
						}		
						raster = Raster.createBandedRaster(db, imageWidth, imageHeight, bytesPerScanLine*8/bitsPerSample, bankIndices, bandoff, null);
					} else {						
						raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth*numOfBands, numOfBands, bandoff, null);
					}
					if(profile != null) {
						raster = IMGUtils.iccp2rgbRaster(raster, cm);
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, raster.getTransferType());
					}
				} else {
					cm = new ComponentColorModel(colorSpace, nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_BYTE);
					if(planaryConfiguration == 2) {
						bandoff = new int[]{0, count[0], count[0] + count[1], count[0] + count[1] + count[2]};
						int[] bankIndices = new int[]{0, 0, 0, 0};
						if(samplesPerPixel >= 5) {
							bandoff = new int[]{0, count[0], count[0] + count[1], count[0] + count[1] + count[2], count[0] + count[1] + count[2] + count[3]};
							bankIndices = new int[]{0, 0, 0, 0, 0};
						}		
						raster = Raster.createBandedRaster(db, imageWidth, imageHeight, bytesPerScanLine, bankIndices, bandoff, null);
					} else {
						raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth*numOfBands, numOfBands, bandoff, null);
					}
					if(profile != null) {
						raster = IMGUtils.iccp2rgbRaster(raster, cm);
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_BYTE);
					}				
				}
				
				return new BufferedImage(cm, raster, false, null);
			case YCbCr:
				int[] samplingFactor = {2, 2}; // Default value, Not [1, 1]
				TiffField<?> f_YCbCrSubSampling = ifd.getField(TiffTag.YCbCr_SUB_SAMPLING);
				if(f_YCbCrSubSampling != null) samplingFactor = f_YCbCrSubSampling.getDataAsLong();
				
				int expandedImageWidth = ((imageWidth + samplingFactor[0] - 1)/samplingFactor[0])*samplingFactor[0];
				int expandedImageHeight = ((imageHeight + samplingFactor[1] - 1)/samplingFactor[1])*samplingFactor[1];				
				
				float referenceBlackY = 0.0f;
				float referenceWhiteY = 255.0f;
			    float referenceBlackCb = 128.0f;
			    float referenceWhiteCb = 255.0f;
		        float referenceBlackCr = 128.0f;
		        float referenceWhiteCr = 255.0f;
			    float codingRangeY = 255.0f;
			    float codingRangeCbCr = 127.0f;
			    
			    TiffField<?> f_referenceBlackWhite = ifd.getField(TiffTag.REFERENCE_BLACK_WHITE);
			    if(f_referenceBlackWhite != null) {
			    	int[] referenceBlackWhite = f_referenceBlackWhite.getDataAsLong();
			    	referenceBlackY = 1.0f*referenceBlackWhite[0]/referenceBlackWhite[1];
			    	referenceWhiteY = 1.0f*referenceBlackWhite[2]/referenceBlackWhite[3];
			    	referenceBlackCb = 1.0f*referenceBlackWhite[4]/referenceBlackWhite[5];
			    	referenceWhiteCb = 1.0f*referenceBlackWhite[6]/referenceBlackWhite[7];
			    	referenceBlackCr = 1.0f*referenceBlackWhite[8]/referenceBlackWhite[9];
			    	referenceWhiteCr = 1.0f*referenceBlackWhite[10]/referenceBlackWhite[11];
			    }
				/*
				 * R, G, and B may be computed from YCbCr as follows:
				 * R = Cr * ( 2 - 2 * LumaRed ) + Y
				 * G = ( Y - LumaBlue * B - LumaRed * R ) / LumaGreen
				 * B = Cb * ( 2 - 2 * LumaBlue ) + Y 
				 * Default values: lumaRed = 299/1000, lumaGreen = 587/1000 and lumaBlue = 114/1000
				 */
				float lumaRed = 0.299f;
				float lumaGreen = 0.587f;
				float lumaBlue = 0.114f;
				
				TiffField<?> f_YCbCrCoefficients = ifd.getField(TiffTag.YCbCr_COEFFICIENTS);
				
				if(f_YCbCrCoefficients != null) {
					int[] lumas = f_YCbCrCoefficients.getDataAsLong();
					lumaRed = 1.0f*lumas[0]/lumas[1];
					lumaGreen = 1.0f*lumas[2]/lumas[3];
					lumaBlue = 1.0f*lumas[4]/lumas[5];
				}
											
				int offsetY = 0;
				
				int bytesY = expandedImageWidth*expandedImageHeight;
				pixels = new byte[bytesY*3];
				
				byte[] temp = null, temp2 = null;
				
				if(planaryConfiguration == 1) {
					// Define variables related to data unit
					int bytesPerUnitY = samplingFactor[0]*samplingFactor[1];
					int bytesPerUnitCb = 1;
					int bytesPerUnitCr = 1;
					int bytesPerDataUnit = bytesPerUnitY + bytesPerUnitCb + bytesPerUnitCr;
					int dataUnitsPerWidth = expandedImageWidth/samplingFactor[0];					
					
					switch(compression) {
						case NONE:
							for(int i = 0; i < stripByteCounts.length; i++) {
								randIS.seek(stripOffsets[i]);
								if(stripByteCounts[i] == 0){
									temp = IOUtils.readFully(randIS, 4096);
								} else {
									temp = new byte[stripByteCounts[i]];
									randIS.readFully(temp);
								}
								int numOfDataUnit = temp.length/bytesPerDataUnit;
								
								offsetY = upsampling(offsetY, numOfDataUnit, bytesPerUnitY, samplingFactor, referenceBlackY, referenceWhiteY, referenceBlackCb,
										referenceWhiteCb, referenceBlackCr, referenceWhiteCr, codingRangeY, codingRangeCbCr, lumaRed, lumaGreen, lumaBlue, temp,
										pixels, expandedImageWidth, dataUnitsPerWidth);
							}
							
							break;
						case LZW:
							for(int i = 0; i < stripByteCounts.length; i++) {
								randIS.seek(stripOffsets[i]);
								if(stripByteCounts[i] == 0){
									temp = IOUtils.readFully(randIS, 4096);
								} else {
									temp = new byte[stripByteCounts[i]];
									randIS.readFully(temp);
								}
								temp2 = new byte[stripBytes[i]];
								decoder = new LZWTreeDecoder(8, true);
								decoder.setInput(temp);
								int numOfBytes = decoder.decode(temp2, 0, temp2.length);	
								
								int numOfDataUnit = numOfBytes/bytesPerDataUnit;
								
								offsetY = upsampling(offsetY, numOfDataUnit, bytesPerUnitY, samplingFactor, referenceBlackY, referenceWhiteY, referenceBlackCb,
										referenceWhiteCb, referenceBlackCr, referenceWhiteCr, codingRangeY, codingRangeCbCr, lumaRed, lumaGreen, lumaBlue, temp2,
										pixels, expandedImageWidth, dataUnitsPerWidth);
							}
							
							break;
						case PACKBITS:
							for(int i = 0; i < stripByteCounts.length; i++) {
								randIS.seek(stripOffsets[i]);
								if(stripByteCounts[i] == 0){
									temp = IOUtils.readFully(randIS, 4096);
								} else {
									temp = new byte[stripByteCounts[i]];
									randIS.readFully(temp);
								}
								temp2 = new byte[stripBytes[i]];
								Packbits.unpackbits(temp, temp2);
								
								int numOfBytes = stripBytes[i];	
								
								int numOfDataUnit = numOfBytes/bytesPerDataUnit;
								
								offsetY = upsampling(offsetY, numOfDataUnit, bytesPerUnitY, samplingFactor, referenceBlackY, referenceWhiteY, referenceBlackCb,
										referenceWhiteCb, referenceBlackCr, referenceWhiteCr, codingRangeY, codingRangeCbCr, lumaRed, lumaGreen, lumaBlue, temp2,
										pixels, expandedImageWidth, dataUnitsPerWidth);
							}
							
							break;
						default:
					}				
				} else {
					// Planar configuration
					if(stripByteCounts.length == 1 && samplesPerPixel != 1)
							throw new RuntimeException("stripByteCounts length 1 is not consistent with samplesPerPixel " + samplesPerPixel);
											
					bytesPerScanLine = (expandedImageWidth*bitsPerSample + 7)/8;
								
					switch(compression) {
						case NONE:
							int stripsPerSample = stripByteCounts.length/samplesPerPixel;
							byte[][] buf = new byte[samplesPerPixel][];
							ByteArrayOutputStream bout = new ByteArrayOutputStream();
							for(int i = 0, index = 0; i < samplesPerPixel; i++) {
								for(int j = 0; j < stripsPerSample; j++, index++) {
									randIS.seek(stripOffsets[index]);
									int len = stripByteCounts[index];
									if(len == 0) {
										temp = IOUtils.readFully(randIS, 4096);
									} else {
										temp = new byte[len];
										randIS.readFully(temp);
									}
									bout.write(temp);
								}
								buf[i] = bout.toByteArray();
								bout.reset();
							}
							
							int yPos = 0;
							int CbPos = 0;
							int CrPos = 0;
							int yOffset = 0;
						
							int stride = samplingFactor[0]*samplingFactor[1];
							int counter = 1;
							
							for(int i = 0; i < expandedImageHeight; i++) {
								for(int j = 0; j < expandedImageWidth; j++, yPos++, yOffset++, counter++) {
									// Populate the pixels array
									int Y = buf[0][yPos]&0xff;
									int Cb = buf[1][CbPos]&0xff;
									int Cr = buf[2][CrPos]&0xff;
									if(counter%stride == 0) {
										CbPos++;
										CrPos++;
									}
									// Convert YCbCr code to full-range YCbCr.
							        float fY = (Y - referenceBlackY)*codingRangeY/(referenceWhiteY - referenceBlackY);
							        float fCb = (Cb - referenceBlackCb)*codingRangeCbCr/(referenceWhiteCb - referenceBlackCb);
							        float fCr = (Cr - referenceBlackCr)*codingRangeCbCr/(referenceWhiteCr - referenceBlackCr);
							        /*
							         * R, G, and B may be computed from YCbCr as follows:
							         * R = Cr * ( 2 - 2 * LumaRed ) + Y
							         * G = ( Y - LumaBlue * B - LumaRed * R ) / LumaGreen
							         * B = Cb * ( 2 - 2 * LumaBlue ) + Y
							        */ 
							        float R = (fCr * (2 - 2 * lumaRed) + fY);
							        float B = (fCb * (2 - 2 * lumaBlue) + fY);
									float G = ((fY - lumaBlue * B - lumaRed * R) / lumaGreen);
									// This is very important!!!
									if(R < 0) R = 0;
									if(R > 255) R = 255;
									if(G < 0) G = 0;
									if(G > 255) G = 255;
									if(B < 0) B = 0;
									if(B > 255) B = 255;
									//								
									int redPos = 3*yOffset;
									
									pixels[redPos] = (byte)R;
									pixels[redPos+1] = (byte)G;
									pixels[redPos+2] = (byte)B;			
								}
							}							
						default:
					}					
				}
				//Create a BufferedImage
				db = new DataBufferByte(pixels, pixels.length);
				//band offset, we have 3 bands if no extra sample is specified, otherwise 4
				bandoff = new int[]{0, 1, 2}; 
				transparent = false;
				numOfBands = samplesPerPixel;
				trans = Transparency.OPAQUE;
				nBits = new int[]{8, 8, 8};
				raster = Raster.createInterleavedRaster(db, expandedImageWidth, expandedImageHeight, expandedImageWidth*numOfBands, numOfBands, bandoff, null);
				cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha,
			                trans, DataBuffer.TYPE_BYTE);
				
				return new BufferedImage(cm, raster, false, null).getSubimage(0, 0, imageWidth, imageHeight);
			case RGB:
				bytesPerScanLine = samplesPerPixel*((imageWidth*bitsPerSample + 7)/8);
				int totalBytes2Read = imageHeight*bytesPerScanLine;
				if(planaryConfiguration == 2) bytesPerScanLine = (imageWidth*bitsPerSample + 7)/8;
				pixels = new byte[totalBytes2Read];
				switch(compression) {
					case NONE:
						for(int i = 0; i < stripByteCounts.length; i++) {
							int bytes2Read = stripBytes[i];
							randIS.seek(stripOffsets[i]);
							randIS.readFully(pixels, offset, bytes2Read);
							offset += bytes2Read;
						}
						// Deals with LSB2MSB fill order (rare and erroneous)
						if(fillOrder == 2) ArrayUtils.reverseBits(pixels);
						break;
					case PACKBITS:
						for(int i = 0; i < stripByteCounts.length; i++) {
							int bytes2Read = stripBytes[i];
							unpackStrip(pixels, offset, stripBytes[i], stripOffsets[i], stripByteCounts[i]);
							offset += bytes2Read;			
						}
						break;
					case LZW:
						decoder = new LZWTreeDecoder(8, true);
						break;
					case DEFLATE:
					case DEFLATE_ADOBE:
						decoder = new DeflateDecoder();
						break;
					default:
				}				
				if(decoder != null) {					
					pixels = new byte[stripOffsets.length*stripBytes[0]];
					for(int i = 0; i < stripByteCounts.length; i++) {
						randIS.seek(stripOffsets[i]);
						if(stripByteCounts[i] == 0){
							temp = IOUtils.readFully(randIS, 4096);
						} else {
							temp = new byte[stripByteCounts[i]];
							randIS.readFully(temp);
						}
						// Deals with LSB2MSB fill order (rare and erroneous)
						if(fillOrder == 2) ArrayUtils.reverseBits(temp);
						decoder.setInput(temp);
						int numOfBytes = decoder.decode(pixels, offset, stripBytes[i]);							
						offset += numOfBytes;
					}
				}
				// This also works with 4 samples per pixel data				
				if(predictor == 2) {
					if(planaryConfiguration == 1)
						pixels = applyDePredictor(samplesPerPixel, pixels, imageWidth, imageHeight);
					else {					
						int dataOffset = 0;
						for(int k = 0; k < samplesPerPixel; k++) {							
							applyDePredictor2(pixels, dataOffset, imageWidth, imageHeight);
							dataOffset += count[k];
						}						
					}						
				}
				
				cm = null;
				raster  = null;
				
				//band offset, we have 3 bands if no extra sample is specified, otherwise 4
				bandoff = new int[samplesPerPixel];
				nBits = new int[samplesPerPixel];
				numOfBands = samplesPerPixel;
				int[] bankIndices = new int[samplesPerPixel];
								
				Arrays.fill(nBits, bitsPerSample <= 32 ? bitsPerSample : 32);
				
				if(planaryConfiguration == 2)
					for(int i = 0; i < samplesPerPixel; i++) {
						bandoff[i] = 0;
						bankIndices[i] = i;						
					}
				else
					for(int i = 0; i < samplesPerPixel; i++) {
						bandoff[i] = i;
						bankIndices[i] = 0; 
					}
				
				transparent = false;			
				trans = Transparency.OPAQUE;
				
				if(samplesPerPixel == 4) {
					trans = Transparency.TRANSLUCENT;
					transparent = true;
				}			
				
				if(planaryConfiguration == 2) {				
					byte[][] rgb = new byte[samplesPerPixel][];
					
					int off = 0;
					int dataBufferType = DataBuffer.TYPE_BYTE;
					
					for(int i = 0; i < samplesPerPixel; i++) {
						rgb[i] = ArrayUtils.subArray(pixels, off, count[i]);
						off += count[i];
					}
					
					if(floatSample) { // Floating point sample data type
						if(bitsPerSample == 16 || bitsPerSample == 32) {
							float[][] floats = new float[samplesPerPixel][];
							if(bitsPerSample == 16) {
								for(int i = 0; i < samplesPerPixel; i++)
									floats[i] = ArrayUtils.to16BitFloatArray(rgb[i], endian == IOUtils.BIG_ENDIAN);
							} else {
								for(int i = 0; i < samplesPerPixel; i++)
									floats[i] = ArrayUtils.toFloatArray(rgb[i], endian == IOUtils.BIG_ENDIAN);				
							}
							dataBufferType = DataBuffer.TYPE_FLOAT;
							db = new DataBufferFloat(floats, floats[0].length);
						} else if(bitsPerSample == 64) {
							double[][] doubles = new double[samplesPerPixel][];
							for(int i = 0; i < samplesPerPixel; i++)
								doubles[i] = ArrayUtils.toDoubleArray(rgb[i], endian == IOUtils.BIG_ENDIAN);
							dataBufferType = DataBuffer.TYPE_DOUBLE;
							db = new DataBufferDouble(doubles, doubles[0].length);
						} else {
							throw new UnsupportedOperationException("Unsupported floating point sample bit depth: " + bitsPerSample);
						}
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, dataBufferType);						
						// Create a TYPE_FLOAT sample model (specifying how the pixels are stored)
				        SampleModel sampleModel = new BandedSampleModel(dataBufferType, imageWidth, imageHeight, imageWidth, bankIndices, bandoff);
				        raster = Raster.createWritableRaster(sampleModel, db, null);							
					} else { // Assume integral sample data type
						if(bitsPerSample == 16) {
							short[][] shorts = new short[samplesPerPixel][];
							for(int i = 0; i < samplesPerPixel; i++)
								shorts[i] = ArrayUtils.toShortArray(rgb[i], endian == IOUtils.BIG_ENDIAN);
							db = new DataBufferUShort(shorts, shorts[0].length);
							dataBufferType = DataBuffer.TYPE_USHORT;
						} else if(bitsPerSample > 8 && bitsPerSample < 16) {
							short[][] shorts = new short[samplesPerPixel][];
							for(int i = 0; i < samplesPerPixel; i++)
								shorts[i] = (short[])(ArrayUtils.toNBits(bitsPerSample, rgb[i], imageWidth, true));
							db = new DataBufferUShort(shorts, shorts[0].length);
							dataBufferType = DataBuffer.TYPE_USHORT;
						} else if(bitsPerSample > 16) {
							int[][] ints = new int[samplesPerPixel][];
							boolean bigEndian = false;
							if(bitsPerSample%8 == 0) bigEndian = true;
							for(int i = 0; i < samplesPerPixel; i++)
								ints[i] = (int[])(ArrayUtils.toNBits(bitsPerSample, rgb[i], samplesPerPixel*imageWidth, bigEndian));
							db = new DataBufferInt(ints, ints[0].length);
							dataBufferType = DataBuffer.TYPE_INT;
						} else if(bitsPerSample < 8) {
							byte[][] bytes = new byte[samplesPerPixel][];
							for(int i = 0; i < samplesPerPixel; i++)
								bytes[i] = (byte[])(ArrayUtils.toNBits(bitsPerSample, rgb[i], imageWidth, true));
							db = new DataBufferByte(bytes, bytes[0].length);
							dataBufferType = DataBuffer.TYPE_BYTE;
						} else {
							db = new DataBufferByte(rgb, rgb[0].length);
							dataBufferType = DataBuffer.TYPE_BYTE;
						}
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, dataBufferType);						
						raster = Raster.createBandedRaster(db, imageWidth, imageHeight, imageWidth, bankIndices, bandoff, null);
					}
				} else {
					if(floatSample) {
						if(bitsPerSample >= 16 && bitsPerSample <= 32) {
							if(bitsPerSample % 8 == 0) {
								float[] tempArray = null;
								if(bitsPerSample == 16)
									tempArray = ArrayUtils.to16BitFloatArray(pixels, endian == IOUtils.BIG_ENDIAN);
								else if(bitsPerSample == 24)
									tempArray = ArrayUtils.to24BitFloatArray(pixels, endian == IOUtils.BIG_ENDIAN);
								else
									tempArray = ArrayUtils.toFloatArray(pixels, endian == IOUtils.BIG_ENDIAN);
								cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_FLOAT);
								db = new DataBufferFloat(tempArray, tempArray.length);
								// Create a TYPE_FLOAT sample model (specifying how the pixels are stored)
								SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, imageWidth, imageHeight, samplesPerPixel, imageWidth*samplesPerPixel, bandoff);
								raster = Raster.createWritableRaster(sampleModel, db, null);							
							} else
								throw new UnsupportedOperationException("Unsupported bit depth: " + bitsPerSample);
						} else if(bitsPerSample == 64) {
							double[] tempArray = ArrayUtils.toDoubleArray(pixels, endian == IOUtils.BIG_ENDIAN);
							cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_DOUBLE);
							db = new DataBufferDouble(tempArray, tempArray.length);
							// Create a TYPE_FLOAT sample model (specifying how the pixels are stored)
							SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_DOUBLE, imageWidth, imageHeight, samplesPerPixel, imageWidth*samplesPerPixel, bandoff);
							raster = Raster.createWritableRaster(sampleModel, db, null);
						} else
							throw new UnsupportedOperationException("Unsupported bit depth: " + bitsPerSample);
					} else {
						if(bitsPerSample < 8) {
							Object tempArray = ArrayUtils.toNBits(bitsPerSample*samplesPerPixel, pixels, imageWidth, true);
							cm = new DirectColorModel(bitsPerSample*samplesPerPixel, redMask[bitsPerSample], greenMask[bitsPerSample], blueMask[bitsPerSample]);
							raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
							raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);
						} else if(bitsPerSample == 8) {
							cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha,	trans, DataBuffer.TYPE_BYTE);
							db = new DataBufferByte(pixels, pixels.length);						
							raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth*numOfBands, numOfBands, bandoff, null);
						} else {
							Object tempArray = ArrayUtils.toNBits(bitsPerSample, pixels, samplesPerPixel*imageWidth, (bitsPerSample%8 == 0)?endian == IOUtils.BIG_ENDIAN:true);
							if(bitsPerSample <= 16) {
								cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_USHORT);
							} else if(bitsPerSample == 32) { // A workaround for Java's ComponentColorModel bug with 32 bit sample
								cm = new Int32ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), transparent);
							} else
								cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_INT);
							raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
							raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);
						}
					}
				}
			
				return new BufferedImage(cm, raster, false, null);
			case BLACK_IS_ZERO:
			case WHITE_IS_ZERO:
				bytesPerScanLine = samplesPerPixel*((imageWidth*bitsPerSample + 7)/8);
				totalBytes2Read = imageHeight*bytesPerScanLine;
				if(planaryConfiguration == 2) bytesPerScanLine = (imageWidth*bitsPerSample + 7)/8;
				pixels = new byte[totalBytes2Read];
				switch(bitsPerSample) {
					case 1: 
						rgbColorPalette = (e_photoMetric == PhotoMetric.BLACK_IS_ZERO)? BLACK_WHITE_PALETTE:BLACK_WHITE_PALETTE_WHITE_IS_ZERO;
						break;
					case 2: 
						rgbColorPalette = (e_photoMetric == PhotoMetric.BLACK_IS_ZERO)? FOUR_COLOR_PALETTE:FOUR_COLOR_PALETTE_WHITE_IS_ZERO;
						break;
					case 4: 
						rgbColorPalette = (e_photoMetric == PhotoMetric.BLACK_IS_ZERO)? SIXTEEN_COLOR_PALETTE:SIXTEEN_COLOR_PALETTE_WHITE_IS_ZERO;
						break;
					case 8:
						rgbColorPalette = (e_photoMetric == PhotoMetric.BLACK_IS_ZERO)? EIGHT_BIT_COLOR_PALETTE:EIGHT_BIT_COLOR_PALETTE_WHITE_IS_ZERO;
						break;
					default:
				}
				switch(compression) {
					case NONE:
						for(int i = 0; i < stripByteCounts.length; i++) {
							int bytes2Read = stripBytes[i];
							randIS.seek(stripOffsets[i]);
							randIS.readFully(pixels, offset, bytes2Read);
							offset += bytes2Read;
						}
						// Deals with LSB2MSB fill order (rare and erroneous)
						if(fillOrder == 2) ArrayUtils.reverseBits(pixels);
						break;
					case CCITTRLE:
						decoder = new G31DDecoder(imageWidth, rowsPerStrip);
						break;
					case CCITTFAX3:
						TiffField<?> f_t4Options = ifd.getField(TiffTag.T4_OPTIONS);						
						int t4Options = 0;
						if(f_t4Options != null) t4Options = f_t4Options.getDataAsLong()[0];
						if ((t4Options & GROUP3OPT_UNCOMPRESSED) == GROUP3OPT_UNCOMPRESSED) {
							throw new UnsupportedCompressionException("Group 3 Uncompressed mode is not supported");
						} else if((t4Options & GROUP3OPT_2DENCODING) == GROUP3OPT_2DENCODING) {
							decoder = new G32DDecoder(imageWidth, rowsPerStrip); // 2D encoding, need to take care of fill bit
						} else {
							decoder = new G32DDecoder(imageWidth, rowsPerStrip, true); // 1D encoding, need to take care of fill bit							
						}
						break;
					case LZW:
						decoder = new LZWTreeDecoder(8, true);
						break;
					case DEFLATE:
					case DEFLATE_ADOBE:
						decoder = new DeflateDecoder();
						break;
					case PACKBITS:
						for(int i = 0; i < stripByteCounts.length; i++) {
							int bytes2Read = stripBytes[i];
							unpackStrip(pixels, offset, stripBytes[i], stripOffsets[i], stripByteCounts[i]);
							offset += bytes2Read;
						}
						break;
					default:
				}
				if(decoder != null) {
					for(int i = 0; i < stripByteCounts.length; i++) {
						randIS.seek(stripOffsets[i]);
						if(stripByteCounts[i] == 0){
							temp = IOUtils.readFully(randIS, 4096);
						} else {
							temp = new byte[stripByteCounts[i]];
							randIS.readFully(temp);
						}
						// Deals with LSB2MSB fill order (rare and erroneous)
						if(fillOrder == 2) ArrayUtils.reverseBits(temp);
						decoder.setInput(temp);
						int numOfBytes = decoder.decode(pixels, offset, stripBytes[i]);
						offset += numOfBytes;
					}
				}
				
				//Create a BufferedImage
				if(bitsPerSample <= 8) {
					if(predictor == 2 && planaryConfiguration == 1)
						pixels = applyDePredictor(samplesPerPixel, pixels, imageWidth, imageHeight);								
					db = new DataBufferByte(pixels, pixels.length);
					cm = new IndexColorModel(bitsPerSample, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
					raster = Raster.createPackedRaster(db, imageWidth, imageHeight, bitsPerSample, null);
					if(samplesPerPixel == 2) { // Deal with alpha transparency
						if(e_photoMetric == PhotoMetric.WHITE_IS_ZERO)
							IMGUtils.invertBits(pixels, 2); // Unlike samplesPerPixel == 1 case, we have to invert bits here as we are using ComponentColorModel
						numOfBands = samplesPerPixel;
						int[] bandOffsets = {0, 0, 0, 1};
						transparent = true;
						trans = Transparency.TRANSLUCENT;
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_BYTE);
						raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth*numOfBands, numOfBands, bandOffsets, null);
					}
				} else if(bitsPerSample <= 16) { // Assume bitsPerSample <= 16
					short[] tempArray = (short[])ArrayUtils.toNBits(bitsPerSample, pixels, samplesPerPixel*imageWidth, (bitsPerSample%8 == 0)?endian == IOUtils.BIG_ENDIAN:true);
					if(predictor == 2 && planaryConfiguration == 1)
						tempArray = applyDePredictor(samplesPerPixel, tempArray, imageWidth, imageHeight);								
					if(e_photoMetric == PhotoMetric.WHITE_IS_ZERO)
						IMGUtils.invertBits(tempArray, samplesPerPixel);
					if(samplesPerPixel == 2) { // Deal with alpha transparency
						transparent = true;
						trans = Transparency.TRANSLUCENT;
					}
					cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_USHORT);
					raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
					raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);					
				} else if(bitsPerSample == 64) { // We can't use ColorSpace.CS_GRAY for some reason. Use CS_sRGB instead
					double[] tempArray = ArrayUtils.toDoubleArray(pixels, endian == IOUtils.BIG_ENDIAN);
					cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), null, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_DOUBLE);
					db = new DataBufferDouble(tempArray, tempArray.length);
					SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_DOUBLE, imageWidth, imageHeight, samplesPerPixel, imageWidth*samplesPerPixel, new int[]{0, 0, 0});
					raster = Raster.createWritableRaster(sampleModel, db, null);
				} else
					throw new UnsupportedOperationException("Unsupported bit depth: " + bitsPerSample);
				return new BufferedImage(cm, raster, false, null);
			default:
		 		break;
		}
		
		return null;
	}
	
	private BufferedImage decodeTiledTiff(IFD ifd) throws Exception {
		// Grab some of the TIFF fields we are interested in
		TiffField<?> field = ifd.getField(TiffTag.COMPRESSION);
		short[] data = (short[])field.getData();
		TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(data[0]&0xffff);
		LOGGER.info("Compression type: {}", compression.getDescription());
		
		TiffField<?> f_tileOffsets = ifd.getField(TiffTag.TILE_OFFSETS);
		if(f_tileOffsets == null) f_tileOffsets = ifd.getField(TiffTag.STRIP_OFFSETS);
		if(f_tileOffsets == null) throw new RuntimeException("Missing required field tileOffsets");
		int[] tileOffsets = f_tileOffsets.getDataAsLong();
		TiffField<?> f_tileByteCounts = ifd.getField(TiffTag.TILE_BYTE_COUNTS);
		if(f_tileByteCounts == null) f_tileByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS);
		int[] tileByteCounts = null;
		if(f_tileByteCounts == null) {
			if(tileOffsets.length == 1) {
				tileByteCounts = new int[]{0};
			} else throw new RuntimeException("Missing required field tileByteCounts");
		} else
			tileByteCounts = f_tileByteCounts.getDataAsLong();
		int imageWidth = ifd.getField(TiffTag.IMAGE_WIDTH).getDataAsLong()[0];
		int imageHeight = ifd.getField(TiffTag.IMAGE_LENGTH).getDataAsLong()[0];
		LOGGER.info("Image width: {}", imageWidth);
		LOGGER.info("Image height: {}", imageHeight);
		
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH);
		TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH);
		
		int tileWidth = imageWidth;
		if(f_tileWidth != null) tileWidth = f_tileWidth.getDataAsLong()[0];
		
		int tileLength = imageHeight;
		if(f_tileLength != null) tileLength = f_tileLength.getDataAsLong()[0];
		
		TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION);
		
		int photoMetric = PhotoMetric.WHITE_IS_ZERO.getValue();
		
		if(f_photoMetric != null)
			photoMetric = f_photoMetric.getDataAsLong()[0];		
		TiffFieldEnum.PhotoMetric e_photoMetric = TiffFieldEnum.PhotoMetric.fromValue(photoMetric);		
		LOGGER.info("PhotoMetric: {}", e_photoMetric);
		
		TiffField<?> f_bitsPerSample = ifd.getField(TiffTag.BITS_PER_SAMPLE);
		
		int bitsPerSample = 1;
		
		if(f_bitsPerSample != null)
			bitsPerSample = f_bitsPerSample.getDataAsLong()[0];
		LOGGER.info("Bits per sample: {}", bitsPerSample);
		
		TiffField<?> f_samplesPerPixel = ifd.getField(TiffTag.SAMPLES_PER_PIXEL);
		
		int samplesPerPixel = 1;
		
		if(f_samplesPerPixel != null)
			samplesPerPixel = f_samplesPerPixel.getDataAsLong()[0];
		LOGGER.info("Samples per pixel: {}", samplesPerPixel);
		
		TiffField<?> f_predictor = ifd.getField(TiffTag.PREDICTOR);
		int predictor = 0;
		if(f_predictor != null) {
			predictor = f_predictor.getDataAsLong()[0];
			LOGGER.info("Predictor: {}", predictor);
		}
		
		TiffField<?> f_planaryConfiguration = ifd.getField(TiffTag.PLANAR_CONFIGURATTION);
		int planaryConfiguration = 1;
		if(f_planaryConfiguration != null) planaryConfiguration = f_planaryConfiguration.getDataAsLong()[0];
		TiffFieldEnum.PlanarConfiguration e_planaryConfiguration = TiffFieldEnum.PlanarConfiguration.fromValue(planaryConfiguration);
		LOGGER.info("Planary configuration: {}", e_planaryConfiguration);
		
		TiffField<?> f_sampleFormat = ifd.getField(TiffTag.SAMPLE_FORMAT);
		TiffField<?> f_sampleMaxValue = ifd.getField(TiffTag.S_MAX_SAMPLE_VALUE);
		TiffField<?> f_sampleMinValue = ifd.getField(TiffTag.S_MIN_SAMPLE_VALUE);
		
		boolean floatSample = false;
		boolean isAssociatedAlpha = false;
		
		if(f_sampleFormat != null && f_sampleFormat.getDataAsLong()[0] == 3) { // Floating point sample data type
			floatSample = true;
			double maxValue = (bitsPerSample <= 32)? Float.MAX_VALUE:Double.MAX_VALUE;
			double minValue = (bitsPerSample <= 32)? Float.MIN_VALUE:Double.MIN_VALUE;
			if(bitsPerSample <= 32 && f_sampleMaxValue != null) {
				maxValue = ((float[])f_sampleMaxValue.getData())[0];
			} else if(bitsPerSample > 32 && f_sampleMaxValue != null) {
				maxValue = ((double[])f_sampleMaxValue.getData())[0];
			}	
			if(bitsPerSample <= 32 && f_sampleMinValue != null) {
				minValue = ((float[])f_sampleMinValue.getData())[0];
			} else if(bitsPerSample > 32 && f_sampleMinValue != null) {
				minValue = ((double[])f_sampleMinValue.getData())[0];
			}				
			LOGGER.info("Sample MAX value: {}", maxValue);
			LOGGER.info("Sample MIN vlaue: {}", minValue);
		}

		int tilesAcross = (imageWidth + tileWidth - 1) / tileWidth;
		int tilesDown = (imageHeight + tileLength - 1) / tileLength;
		int tilesPerImage = tilesAcross * tilesDown;
		
		ImageDecoder decoder = null;
		byte[] pixels = null;
		
		int bytes2Read = ((tileWidth*bitsPerSample + 7)/8)*samplesPerPixel*tileLength;
		
		int xoff = 0;
		int yoff = 0;
		int tileCounter = 0;
		
		WritableRaster raster = null;
		DataBuffer db = null;
		ColorModel cm = null;
		
		switch(e_photoMetric) {
			case PALETTE_COLOR:
				short[] colorMap = (short[])ifd.getField(TiffTag.COLORMAP).getData();
				rgbColorPalette = new int[colorMap.length/3];
				int numOfColors = (1<<bitsPerSample);
				int numOfColors2 = (numOfColors<<1);
				for(int i = 0, index = 0; i < colorMap.length/3;i++) {
					rgbColorPalette[index++] = 0xff000000|((colorMap[i]&0xff00)<<8)|((colorMap[i+numOfColors]&0xff00))|((colorMap[i+numOfColors2]&0xff00)>>8) ;
				}
				int bytesPerScanLine = tilesAcross*(tileWidth*bitsPerSample +7)/8;
				pixels = new byte[bytesPerScanLine*tilesDown*tileLength];
				short[] spixels = null;
				db = new DataBufferByte(pixels, pixels.length);
				cm = new IndexColorModel(bitsPerSample, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
				if(bitsPerSample < 8) {
					raster = Raster.createPackedRaster(db, tileWidth*tilesAcross, tileLength*tilesDown, bitsPerSample, null);
				} else {
					int[] off = {0}; //band offset, we have only one band start at 0
					if(bitsPerSample > 8) {
						spixels = new short[pixels.length/2];
						db = new DataBufferUShort(spixels, spixels.length);
						cm = new IndexColorModel(bitsPerSample, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_USHORT);
					}
					raster = Raster.createInterleavedRaster(db, tileWidth*tilesAcross, tileLength*tilesDown, tileWidth*tilesAcross, 1, off, null);
				}
				
				switch(compression) {
					case NONE:
						for(int i = 0; i < tileByteCounts.length; i++) {
							byte[] temp = new byte[tileByteCounts[i]];
							randIS.seek(tileOffsets[i]);
							randIS.readFully(temp);
							if(bitsPerSample == 16) {
								raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.toShortArray(temp, endian == IOUtils.BIG_ENDIAN));
							} else {
								DataBuffer tileDataBuffer = new DataBufferByte(temp, temp.length);
								WritableRaster tileRaster = Raster.createPackedRaster(tileDataBuffer, tileWidth, tileLength, bitsPerSample, null);
								raster.setDataElements(xoff, yoff, tileRaster);
							}
							xoff += tileWidth;
							tileCounter++;
							if(tileCounter >= tilesAcross) {
								xoff = 0;
								yoff += tileLength;
								tileCounter = 0;
							}						
						}
						break;
					case LZW:
						decoder = new LZWTreeDecoder(8, true);
						break;
					case DEFLATE:
					case DEFLATE_ADOBE:
						decoder = new DeflateDecoder();
						break;
					default:
				}
				if(decoder != null) {
					for(int i = 0; i < tileByteCounts.length; i++) {
						byte[] temp = new byte[tileByteCounts[i]];
						byte[] temp2 = new byte[bytes2Read];
						randIS.seek(tileOffsets[i]);
						randIS.readFully(temp);
						decoder.setInput(temp);
						decoder.decode(temp2, 0, bytes2Read);
						if(bitsPerSample == 16) {
							raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.toShortArray(temp2, endian == IOUtils.BIG_ENDIAN));
						} else
							raster.setDataElements(xoff, yoff, tileWidth, tileLength, temp2);
						xoff += tileWidth;
						tileCounter++;
						if(tileCounter >= tilesAcross) {
							xoff = 0;
							yoff += tileLength;
							tileCounter = 0;
						}					
					}
				}
							   
				return new BufferedImage(cm, raster, false, null).getSubimage(0, 0, imageWidth, imageHeight);
			case RGB:
				//Create a BufferedImage
				//band offset, we have 3 bands if no extra sample is specified, otherwise 4
				int[] bandoff = new int[samplesPerPixel];
				int[] nBits = new int[samplesPerPixel];
				Arrays.fill(nBits, bitsPerSample);
				for(int i = 0; i < samplesPerPixel; i++) {
					bandoff[i] = i; 
				}
				boolean transparent = false;
				int trans = Transparency.OPAQUE;				
				// There is an extra sample (most probably alpha)
				if(samplesPerPixel >= 4) {
					trans = Transparency.TRANSLUCENT;
					transparent = true;
				}
				
				if(floatSample) {
					if(bitsPerSample == 64) {
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, false,
								trans, DataBuffer.TYPE_DOUBLE);
					} else if(bitsPerSample >= 16 && bitsPerSample <= 32) {
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, false,
								trans, DataBuffer.TYPE_FLOAT);			
					}			
				} else {
					if(bitsPerSample < 8) {
						cm = new DirectColorModel(bitsPerSample*samplesPerPixel, redMask[bitsPerSample], greenMask[bitsPerSample], blueMask[bitsPerSample]);
					} else if(bitsPerSample == 16) {
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha,
				                trans, DataBuffer.TYPE_USHORT);
					} else if(bitsPerSample == 24) {
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha,
				                trans, DataBuffer.TYPE_INT);
					} else	if(bitsPerSample == 32) {
						cm = new Int32ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), transparent);
					} else if(bitsPerSample == 64) {
						cm = new Int32ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), transparent);
					} else
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha,
				                trans, DataBuffer.TYPE_BYTE);
				}
				
				raster = cm.createCompatibleWritableRaster(tileWidth*tilesAcross, tileLength*tilesDown);
			
				// Now we are going to read and set tiles to the raster
				switch(compression) {
					case NONE:
						if(planaryConfiguration == 1) {
							for(int i = 0; i < tileByteCounts.length; i++) {
								byte[] temp = new byte[tileByteCounts[i]];
								randIS.seek(tileOffsets[i]);
								randIS.readFully(temp);
								if(floatSample) {
									Object tempArray = null;
									if(bitsPerSample == 64)
										tempArray = ArrayUtils.toDoubleArray(temp, endian == IOUtils.BIG_ENDIAN);
									else if(bitsPerSample == 32) {
										tempArray = ArrayUtils.toFloatArray(temp, endian == IOUtils.BIG_ENDIAN);
									} else if(bitsPerSample == 24) {
									    tempArray =	ArrayUtils.to24BitFloatArray(temp, endian == IOUtils.BIG_ENDIAN);
									} else if(bitsPerSample == 16)
										tempArray = ArrayUtils.to16BitFloatArray(temp, endian == IOUtils.BIG_ENDIAN);
									else
										throw new UnsupportedOperationException("Unsupported bit depth: " + bitsPerSample);
									raster.setDataElements(xoff, yoff, tileWidth, tileLength, tempArray);	
								} else {
									if(bitsPerSample < 8) {
										raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.toNBits(bitsPerSample*samplesPerPixel, temp, tileWidth, true));
									} else if(bitsPerSample == 8) {
										raster.setDataElements(xoff, yoff, tileWidth, tileLength, temp);									
									} else if(bitsPerSample % 8 == 0) {
										Object tempArray = ArrayUtils.toNBits(bitsPerSample, temp, samplesPerPixel*tileWidth, endian == IOUtils.BIG_ENDIAN);
										raster.setDataElements(xoff, yoff, tileWidth, tileLength, tempArray);									
									} else {
										Object tempArray = ArrayUtils.toNBits(bitsPerSample, temp, samplesPerPixel*tileWidth, true);
										raster.setDataElements(xoff, yoff, tileWidth, tileLength, tempArray);
									}
								}
								xoff += tileWidth;
								tileCounter++;
								if(tileCounter >= tilesAcross) {
									xoff = 0;
									yoff += tileLength;
									tileCounter = 0;
								}						
							}
						} else { // TODO change according to Planar == 1					
							byte[][] rgb = new byte[samplesPerPixel][];
							Raster tileRaster = null;
							DataBuffer tileDatabuffer = null;
							int[] bankIndices = {0, 1, 2};
							bandoff = new int[]{0, 0, 0};
							int dataBufferType = DataBuffer.TYPE_BYTE;
							for(int i = 0; i < tilesPerImage; i++) {
								rgb[0] = new byte[tileByteCounts[i]];
								randIS.seek(tileOffsets[i]);
								randIS.readFully(rgb[0]);
								int greenOff = i + tilesPerImage;
								rgb[1] = new byte[tileByteCounts[greenOff]];
								randIS.seek(tileOffsets[greenOff]);
								randIS.readFully(rgb[1]);
								int blueOff = greenOff + tilesPerImage;
								rgb[2] = new byte[tileByteCounts[blueOff]];
								randIS.seek(tileOffsets[blueOff]);
								randIS.readFully(rgb[2]);
								
								if(floatSample) { // Floating point sample data type
									if(bitsPerSample >= 16 && bitsPerSample <= 32) {
										float[][] floats = new float[samplesPerPixel][];
										if(bitsPerSample == 16) {
											for(int j = 0; j < samplesPerPixel; j++)
												floats[j] = ArrayUtils.to16BitFloatArray(rgb[j], endian == IOUtils.BIG_ENDIAN);
										} else if(bitsPerSample == 24) {
											for(int j = 0; j < samplesPerPixel; j++)
												floats[j] = ArrayUtils.to24BitFloatArray(rgb[j], endian == IOUtils.BIG_ENDIAN);
										} else if(bitsPerSample == 32) {
											for(int j = 0; j < samplesPerPixel; j++)
												floats[j] = ArrayUtils.toFloatArray(rgb[j], endian == IOUtils.BIG_ENDIAN);				
										}										
										tileDatabuffer = new DataBufferFloat(floats, floats[0].length);
										dataBufferType = DataBuffer.TYPE_FLOAT;
									} else if(bitsPerSample == 64) {
										double[][] doubles = new double[samplesPerPixel][];
										for(int j = 0; j < samplesPerPixel; j++)
											doubles[j] = ArrayUtils.toDoubleArray(rgb[j], endian == IOUtils.BIG_ENDIAN);
										tileDatabuffer = new DataBufferDouble(doubles, doubles[0].length);
										dataBufferType = DataBuffer.TYPE_DOUBLE;										
									} else {
										throw new UnsupportedOperationException("Unsupported floating point sample bit depth: " + bitsPerSample);
									}
									// Create a TYPE_FLOAT sample model (specifying how the pixels are stored)
									SampleModel sampleModel = new BandedSampleModel(dataBufferType, tileWidth, tileLength, tileWidth, bankIndices, bandoff);
									tileRaster = Raster.createWritableRaster(sampleModel, tileDatabuffer, null);
								} else {
									if(bitsPerSample == 16) {
										tileDatabuffer = new DataBufferUShort(new short[][]{ArrayUtils.toShortArray(rgb[0], endian == IOUtils.BIG_ENDIAN), 
												ArrayUtils.toShortArray(rgb[1], endian == IOUtils.BIG_ENDIAN), 
												ArrayUtils.toShortArray(rgb[2], endian == IOUtils.BIG_ENDIAN)},
												tileWidth*tileLength);
									} else if(bitsPerSample > 16 && bitsPerSample <= 32) {
										boolean bigEndian = (bitsPerSample%8 == 0)?endian == IOUtils.BIG_ENDIAN:true;
										tileDatabuffer = new DataBufferInt(new int[][]{(int[])(ArrayUtils.toNBits(bitsPerSample, rgb[0], tileWidth, bigEndian)), 
												(int[])(ArrayUtils.toNBits(bitsPerSample, rgb[1], tileWidth, bigEndian)), 
												(int[])(ArrayUtils.toNBits(bitsPerSample, rgb[2], tileWidth, bigEndian))},
												tileWidth*tileLength);
									} else if(bitsPerSample == 64) { // Have to convert to 32 bit since there is no DataBuffer.TYPE_LONG
										tileDatabuffer = new DataBufferInt(new int[][]{ArrayUtils.to32BitsLongArray(rgb[0], endian == IOUtils.BIG_ENDIAN), 
												ArrayUtils.to32BitsLongArray(rgb[1], endian == IOUtils.BIG_ENDIAN), 
												ArrayUtils.to32BitsLongArray(rgb[2], endian == IOUtils.BIG_ENDIAN)},
												tileWidth*tileLength);
									} else {
										tileDatabuffer = new DataBufferByte(rgb, rgb[0].length);
									}
									
									tileRaster = Raster.createBandedRaster(tileDatabuffer, tileWidth, tileLength, tileWidth, bankIndices, bandoff, null);
								}
								raster.setRect(xoff, yoff, tileRaster);
								xoff += tileWidth;
								tileCounter++;
								if(tileCounter >= tilesAcross) {
									xoff = 0;
									yoff += tileLength;
									tileCounter = 0;
								}								
							}
						}
						break;
					case PACKBITS:
						for(int i = 0; i < tileByteCounts.length; i++) {
							byte[] temp = new byte[tileByteCounts[i]];
							randIS.seek(tileOffsets[i]);
							randIS.readFully(temp);
							byte[] temp2 = new byte[bytes2Read];
							Packbits.unpackbits(temp, temp2);
							if(bitsPerSample == 16) {
								raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.toShortArray(temp2, endian == IOUtils.BIG_ENDIAN));
							} else
								raster.setDataElements(xoff, yoff, tileWidth, tileLength, temp2);
							xoff += tileWidth;
							tileCounter++;
							if(tileCounter >= tilesAcross) {
								xoff = 0;
								yoff += tileLength;
								tileCounter = 0;
							}		
						}
						break;
					case LZW:
						decoder = new LZWTreeDecoder(8, true);
						break;
					case DEFLATE:
					case DEFLATE_ADOBE:
						decoder = new DeflateDecoder();
						break;
					default:
				}
				if(decoder != null) {					
					for(int i = 0; i < tileByteCounts.length; i++) {
						byte[] temp = new byte[tileByteCounts[i]];
						byte[] temp2 = new byte[bytes2Read];
						randIS.seek(tileOffsets[i]);
						randIS.readFully(temp);
						decoder.setInput(temp);
						decoder.decode(temp2, 0, bytes2Read);
						if(bitsPerSample == 16) {
							raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.toShortArray(temp2, endian == IOUtils.BIG_ENDIAN));
						} else
							raster.setDataElements(xoff, yoff, tileWidth, tileLength, temp2);
						xoff += tileWidth;
						tileCounter++;
						if(tileCounter >= tilesAcross) {
							xoff = 0;
							yoff += tileLength;
							tileCounter = 0;
						}
					}
				}
				// This also works with 4 samples per pixel data
				if(predictor == 2 && planaryConfiguration == 1 && bitsPerPixel == 8)
					pixels = applyDePredictor(samplesPerPixel, pixels, imageWidth, imageHeight);
			
				return new BufferedImage(cm, raster, false, null).getSubimage(0, 0, imageWidth, imageHeight);
			default:
		 		break;
		}	
		
		return null;
	}
	 
	public int getFrameCount() {
		if(frames != null) // We have already read the image
			return frames.size();
		return super.getFrameCount(); // We haven't read the image yet
	}
	
	public BufferedImage getFrame(int i) {
		if(frames == null) return null;
		if(i >= 0 && i < frames.size()) {
			return frames.get(i);
		} else 
			throw new IllegalArgumentException("Frame index " + i + " out of bounds");
	}
    
    public List<BufferedImage> getFrames() {
		if(frames != null)
			return Collections.unmodifiableList(frames);
		return Collections.emptyList();
    }
    
	private boolean readHeader(RandomAccessInputStream randIS) throws IOException {
		// First 2 bytes determine the byte order of the file
		endian = randIS.readShort();
		
		if(endian == IOUtils.BIG_ENDIAN)
		{
			LOGGER.info("Byte order: Motorola BIG_ENDIAN");
			this.randIS.setReadStrategy(ReadStrategyMM.getInstance());
		} else if(endian == IOUtils.LITTLE_ENDIAN) {
			LOGGER.info("Byte order: Intel LITTLE_ENDIAN");
			this.randIS.setReadStrategy(ReadStrategyII.getInstance());
		} else {
			LOGGER.info("Warning: invalid TIFF byte order!");
			return false;
		} 
		
		// Read TIFF identifier
		short tiff_id = randIS.readShort();
		  
		if(tiff_id!=0x2a) { //"*" 42 decimal
			LOGGER.error("Error: invalid tiff identifier");
			return false;
		}
		  
		return true;
	}
	 
	private int readIFD(int id, int offset) throws IOException {
		IFD tiffIFD = new IFD();
		LOGGER.info("IFD {} offset: byte {}", id, offset);
		randIS.seek(offset);
		int no_of_fields = randIS.readShort();
		LOGGER.info("Total number of fields for IFD {}: {}", id, no_of_fields);
		offset += 2;
		
		for (int i = 0;i < no_of_fields; i++) {
			LOGGER.info("TiffField {} =>", i);
			randIS.seek(offset);
			short tag = randIS.readShort();
			Tag ftag = TiffTag.fromShort(tag);
			if (ftag == TiffTag.UNKNOWN)
				LOGGER.info("TiffTag: {} [Value: 0x{}] (Unknown)", ftag, Integer.toHexString(tag&0xffff));
			else
				LOGGER.info("TiffTag: {}", ftag);
			offset += 2;
			randIS.seek(offset);
			short type = randIS.readShort();
			FieldType ftype = FieldType.fromShort(type);
			LOGGER.info("Data type: {}", ftype);
			offset += 2;
			randIS.seek(offset);
			int field_length = randIS.readInt();
			LOGGER.info("TiffField length: {}", field_length);
			offset += 4;
			////// Try to read actual data.
			switch (ftype) {
				case BYTE:
				case UNDEFINED:
					byte[] data = new byte[field_length];
					if(field_length <= 4) {
						randIS.seek(offset);
						randIS.readFully(data, 0, field_length);					   
					} else {
						randIS.seek(offset);
						randIS.seek(randIS.readInt());
						randIS.readFully(data, 0, field_length);
					}
					LOGGER.info("TiffField value: {}", StringUtils.byteArrayToHexString(data, 0, 10));
					offset += 4;					
					tiffIFD.addField((ftype == FieldType.BYTE)?new ByteField(tag, data):
						new UndefinedField(tag, data));
					break;
				case ASCII:
					data = new byte[field_length];
					if(field_length <= 4) {
						randIS.seek(offset);
						randIS.readFully(data, 0, field_length);
					} else {
						randIS.seek(offset);
						randIS.seek(randIS.readInt());
						randIS.readFully(data, 0, field_length);
					}
					if(data.length>0)
					  LOGGER.info("TiffField value: {}", new String(data, 0, data.length-1).trim());
					offset += 4;	
					tiffIFD.addField(new ASCIIField(tag, new String(data, 0, data.length)));
			        break;
				case SHORT:
					short[] sdata = new short[field_length];
					if(field_length == 1) {
					  randIS.seek(offset);
					  sdata[0] = randIS.readShort();
					  offset += 4;
					} else if (field_length == 2) {
						randIS.seek(offset);
						sdata[0] = randIS.readShort();
						offset += 2;
						randIS.seek(offset);
						sdata[1] = randIS.readShort();
						offset += 2;
					} else {
						randIS.seek(offset);
						int toOffset = randIS.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							randIS.seek(toOffset);
							sdata[j] = randIS.readShort();
							toOffset += 2;
						}
					}	
					tiffIFD.addField(new ShortField(tag, sdata));
					LOGGER.info("TiffField value: {}", StringUtils.shortArrayToString(sdata, 0, 10, true));
					break;
				case LONG:
					int[] ldata = new int[field_length];
					if(field_length == 1) {
						randIS.seek(offset);
						ldata[0] = randIS.readInt();
						offset += 4;
					} else {
						randIS.seek(offset);
						int toOffset = randIS.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							randIS.seek(toOffset);
							ldata[j] = randIS.readInt();
							toOffset += 4;
						}
					}	
					LOGGER.info("TiffField value: {}", StringUtils.longArrayToString(ldata, 0, 10, true));
					tiffIFD.addField(new LongField(tag, ldata));
					break;
				case RATIONAL:
					int len = 2*field_length;
					ldata = new int[len];	
					randIS.seek(offset);
					int toOffset = randIS.readInt();
					offset += 4;					
					for (int j=0;j<len; j+=2) {
						randIS.seek(toOffset);
						ldata[j] = randIS.readInt();
						toOffset += 4;
						randIS.seek(toOffset);
						ldata[j+1] = randIS.readInt();
						toOffset += 4;
					}	
					tiffIFD.addField(new RationalField(tag, ldata));
					LOGGER.info("TiffField value: {}", StringUtils.rationalArrayToString(ldata, true));
					break;
				case FLOAT:
					float[] fdata = new float[field_length];
					if(field_length == 1) {
						randIS.seek(offset);
						fdata[0] = randIS.readFloat();
						offset += 4;
					} else {
						randIS.seek(offset);
						toOffset = randIS.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++) {
							randIS.seek(toOffset);
							fdata[j] = randIS.readFloat();
							toOffset += 4;
						}
					}
					tiffIFD.addField(new FloatField(tag, fdata));
					LOGGER.info("TiffField value: {}", Arrays.toString(fdata));			
					break;
				case DOUBLE:
					double[] ddata = new double[field_length];
					randIS.seek(offset);
					toOffset = randIS.readInt();
					offset += 4;
					for (int j=0;j<field_length; j++) {
						randIS.seek(toOffset);
						ddata[j] = randIS.readDouble();
						toOffset += 8;
					}
					tiffIFD.addField(new DoubleField(tag, ddata));
					LOGGER.info("Field value: {}", Arrays.toString(ddata));						
					break;
				default:
					offset += 4;
					break;					
			  }	
		}
		list.add(tiffIFD);
		LOGGER.info("********************************");
		randIS.seek(offset);
		return randIS.readInt();
	}
	
	// De-predictor for PLANARY_CONFIGURATION value 1
	private static byte[] applyDePredictor(int numOfSamples, byte[] input, int imageWidth, int imageHeight) {
		for(int i = 0, inc = numOfSamples*imageWidth, maxVal = inc - numOfSamples, minVal = numOfSamples; i <= imageHeight - 1; maxVal += inc, minVal += inc, i++) {
			for (int j = minVal; j <= maxVal; j+=numOfSamples) {
				for(int k = 0; k < numOfSamples; k++) {
					input[j + k] += input[j - numOfSamples + k];
				}
			}
		}
		return input;
	}
	
	// De-predictor for PLANARY_CONFIGURATION value 1 and DataBuffer.TYPE_USHORT	
	private static short[] applyDePredictor(int numOfSamples, short[] input, int imageWidth, int imageHeight) {
		for(int i = 0, inc = numOfSamples*imageWidth, maxVal = inc - numOfSamples, minVal = numOfSamples; i <= imageHeight - 1; maxVal += inc, minVal += inc, i++) {
			for (int j = minVal; j <= maxVal; j+=numOfSamples) {
				for(int k = 0; k < numOfSamples; k++) {
					input[j + k] += input[j - numOfSamples + k];
				}
			}
		}
		return input;
	}
	
	// De-predictor for PLANARY_CONFIGURATION value 2
	private static byte[] applyDePredictor2(byte[] input, int offset, int imageWidth, int imageHeight) {
		for(int i = imageHeight - 1, inc = imageWidth, maxVal = offset + inc - 1, minVal = offset + 1; i >= 0; maxVal += inc, minVal += inc,  i--) {
			for (int j = minVal; j < maxVal; j++) {
				input[j] += input[j - 1];
			}
		}
		return input;
	}
	
	// Unpack PACKBITS encoded strips
	private void unpackStrip(byte[] pixels, int offset, int bytes2Read, int stripOffset, int stripByteCount) throws IOException {
		randIS.seek(stripOffset);
		byte[] temp = null;
		if(stripByteCount == 0) {
			temp = IOUtils.readFully(randIS, 4096);
		} else {
			temp = new byte[stripByteCount];
			randIS.readFully(temp);
		}		
		byte[] temp2 = new byte[bytes2Read];
		Packbits.unpackbits(temp, temp2);
		System.arraycopy(temp2, 0, pixels, offset, bytes2Read);			
	}
	
	private int upsampling(int offsetY, int numOfDataUnit, int bytesPerUnitY, int[] samplingFactor, float referenceBlackY, float referenceWhiteY, 
			float referenceBlackCb, float referenceWhiteCb, float referenceBlackCr, float referenceWhiteCr,  float codingRangeY,
			float codingRangeCbCr, 	float lumaRed, float lumaGreen,	float lumaBlue, byte[] temp, byte[] pixels,
			int expandedImageWidth, int dataUnitsPerWidth) {
		// Reset
		int offset = 0;
		int offsetX = 0;
		
		for(int j = 1; j <= numOfDataUnit; j++) {
			int offsetCb = offset + bytesPerUnitY;
			int Cb = temp[offsetCb]&0xff;
			int Cr = temp[offsetCb + 1]&0xff;
			for(int k = 0; k < samplingFactor[1]; k++) {// Rows
				for(int l = 0; l < samplingFactor[0]; l++, offsetX++) { // Columns
					// Populate the pixels array
					int Y = temp[offset++]&0xff;
					// Convert YCbCr code to full-range YCbCr.
			        float fY = (Y - referenceBlackY)*codingRangeY/(referenceWhiteY - referenceBlackY);
			        float fCb = (Cb - referenceBlackCb)*codingRangeCbCr/(referenceWhiteCb - referenceBlackCb);
			        float fCr = (Cr - referenceBlackCr)*codingRangeCbCr/(referenceWhiteCr - referenceBlackCr);
			        /*
			         * R, G, and B may be computed from YCbCr as follows:
			         * R = Cr * ( 2 - 2 * LumaRed ) + Y
			         * G = ( Y - LumaBlue * B - LumaRed * R ) / LumaGreen
			         * B = Cb * ( 2 - 2 * LumaBlue ) + Y
			        */ 
			        float R = (fCr * (2 - 2 * lumaRed) + fY);
			        float B = (fCb * (2 - 2 * lumaBlue) + fY);
					float G = ((fY - lumaBlue * B - lumaRed * R) / lumaGreen);
					// This is very important!!!
					if(R < 0) R = 0;
					if(R > 255) R = 255;
					if(G < 0) G = 0;
					if(G > 255) G = 255;
					if(B < 0) B = 0;
					if(B > 255) B = 255;
					//
					int yPos = offsetX + offsetY*expandedImageWidth;
					int redPos = 3*yPos;
					
					pixels[redPos] = (byte)R;
					pixels[redPos+1] = (byte)G;
					pixels[redPos+2] = (byte)B;											
				}
				offsetX -= samplingFactor[0];
				offsetY += 1;
			}
			offsetY -= samplingFactor[1];
			offset += 2;
			offsetX += samplingFactor[0];
			if(j%dataUnitsPerWidth == 0) {
				offsetY += samplingFactor[1];
				offsetX = 0;
			}
		}
		
		return offsetY;
	}	
}