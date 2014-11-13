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
 * TIFFReader.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    07Nov2014  Added support for 16 bit CMYK image
 * WY    06Nov2014  Planar support for stripped YCbCr image
 * WY    05Nov2014  Fixed bug for YCbCr image with wrong image width and height
 * WY    31Oct2014  Added basic support for uncompressed and LZW compressed YCbCr
 * WY    15Oct2014  Added basic support for 16 bits RGB image
 * WY    14Oct2014  Revised to show specification violation TIFF LZW compression
 * WY    14Oct2014  Revised to show RGB TIFF with extra sample tag
 */

package cafe.image.reader;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import cafe.image.colorspace.CMYKColorSpace;
import cafe.image.compression.ImageDecoder;
import cafe.image.compression.deflate.DeflateDecoder;
import cafe.image.compression.lzw.LZWTreeDecoder;
import cafe.image.compression.packbits.Packbits;
import cafe.image.tiff.ASCIIField;
import cafe.image.tiff.ByteField;
import cafe.image.tiff.IFD;
import cafe.image.tiff.LongField;
import cafe.image.tiff.RationalField;
import cafe.image.tiff.ShortField;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.tiff.Tag;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffFieldEnum;
import cafe.image.tiff.TiffTag;
import cafe.image.tiff.FieldType;
import cafe.image.tiff.UndefinedField;
import cafe.image.util.IMGUtils;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.io.ReadStrategyII;
import cafe.io.ReadStrategyMM;
import cafe.string.StringUtils;
import cafe.util.ArrayUtils;

/** 
 * Decodes and shows TIFF images. 
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/09/2012  
 */
public class TIFFReader extends ImageReader {
	private RandomAccessInputStream randIS = null;
	private List<IFD> list = new ArrayList<IFD>();
	private int endian = IOUtils.BIG_ENDIAN;
	private static final long[] redMask =   {0x00, 0x04, 0x30, 0x1c0, 0xf00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xfff000000L};
	private static final long[] greenMask = {0x00, 0x02, 0x0c, 0x038, 0x0f0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xfff000};
	private static final long[] blueMask =  {0x00, 0x01, 0x03, 0x007, 0x00f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xfff};
	 
	public BufferedImage read(InputStream is) throws Exception
	{
		randIS = new FileCacheRandomAccessInputStream(is);
		if(!readHeader(randIS)) return null;
		 
		int offset = randIS.readInt();
		
		int ifd = 0;
				
		while (offset != 0)
		{
			offset = readIFD(ifd++, offset);
		}
		BufferedImage bi =  decode(list.get(0));
		randIS.close();
		
		return bi;
	}
	 
	private BufferedImage decode(IFD ifd) throws Exception {
		// Grab some of the TIFF fields we are interested in
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH.getValue());
		TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH.getValue());
		if(f_tileWidth != null && f_tileLength != null)
			return decodeTiledTiff(ifd);
		return decodeStrippedTiff(ifd);
	}
	
	private BufferedImage decodeStrippedTiff(IFD ifd) throws Exception {
		// Grab some of the TIFF fields we are interested in
		TiffField<?> field = ifd.getField(TiffTag.COMPRESSION.getValue());
		short[] data = new short[]{1}; // Default no compression
		if(field != null)
			data = (short[])field.getData();
		TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(data[0]&0xffff);
		System.out.println("Compression type: " + compression.getDescription());
		// Forget about tiled TIFF for now
		TiffField<?> f_stripOffsets = ifd.getField(TiffTag.STRIP_OFFSETS.getValue());
		TiffField<?> f_stripByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS.getValue());
		int[] stripOffsets = f_stripOffsets.getDataAsLong();
		int[] stripByteCounts = f_stripByteCounts.getDataAsLong();
		int imageWidth = ifd.getField(TiffTag.IMAGE_WIDTH.getValue()).getDataAsLong()[0];
		int imageHeight = ifd.getField(TiffTag.IMAGE_LENGTH.getValue()).getDataAsLong()[0];
		System.out.println("Image width: " + imageWidth);
		System.out.println("Image height: " + imageHeight);
		TiffField<?> f_rowsPerStrip = ifd.getField(TiffTag.ROWS_PER_STRIP.getValue());
		int rowsPerStrip = imageHeight;
		if(f_rowsPerStrip != null)
			rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];
		System.out.println("Rows per strip: " + rowsPerStrip);		
		TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue());
		int photoMetric = f_photoMetric.getDataAsLong()[0];
		TiffFieldEnum.PhotoMetric e_photoMetric = TiffFieldEnum.PhotoMetric.fromValue(photoMetric);
		System.out.println("PhotoMetric: " + e_photoMetric);
		TiffField<?> f_bitsPerSample = ifd.getField(TiffTag.BITS_PER_SAMPLE.getValue());
		int bitsPerSample = f_bitsPerSample.getDataAsLong()[0];
		System.out.println("Bits per sample: " + bitsPerSample);
		TiffField<?> f_samplesPerPixel = ifd.getField(TiffTag.SAMPLES_PER_PIXEL.getValue());
		int samplesPerPixel = f_samplesPerPixel.getDataAsLong()[0];
		System.out.println("Samples per pixel: " + samplesPerPixel);
		TiffField<?> f_predictor = ifd.getField(TiffTag.PREDICTOR.getValue());
		int predictor = 0;
		if(f_predictor != null) {
			predictor = f_predictor.getDataAsLong()[0];
			System.out.println("Predictor: " + predictor);
		}
		TiffField<?> f_planaryConfiguration = ifd.getField(TiffTag.PLANAR_CONFIGURATTION.getValue());
		int planaryConfiguration = 1;
		if(f_planaryConfiguration != null) planaryConfiguration = f_planaryConfiguration.getDataAsLong()[0];
		TiffFieldEnum.PlanarConfiguration e_planaryConfiguration = TiffFieldEnum.PlanarConfiguration.fromValue(planaryConfiguration);
		System.out.println("Planary configuration: " + e_planaryConfiguration);
		
		boolean transparent = false;
		boolean isAssociatedAlpha = false;
		int numOfBands = samplesPerPixel;
		int trans = Transparency.OPAQUE;
		
		TiffField<?> f_extraSamples = ifd.getField(TiffTag.EXTRA_SAMPLES.getValue());
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
				short[] colorMap = (short[])ifd.getField(TiffTag.COLORMAP.getValue()).getData();
				rgbColorPalette = new int[colorMap.length/3];
				int numOfColors = (1<<bitsPerSample);
				int numOfColors2 = (numOfColors<<1);
				for(int i = 0, index = 0; i < colorMap.length/3;i++) {
					rgbColorPalette[index++] = 0xff000000|((colorMap[i]&0xff00)<<8)|((colorMap[i+numOfColors]&0xff00))|((colorMap[i+numOfColors2]&0xff00)>>8) ;
				}
				int bytesPerScanLine = (imageWidth*bitsPerSample +7)/8;
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
					default:
				}
				if(decoder != null) {
					for(int i = 0; i < stripByteCounts.length; i++) {
						byte[] temp = new byte[stripByteCounts[i]];
						randIS.seek(stripOffsets[i]);
						randIS.readFully(temp);
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
						int bytes2Read = stripBytes[i];
						byte[] temp = new byte[stripByteCounts[i]];
						randIS.seek(stripOffsets[i]);
						randIS.readFully(temp);
						decoder.setInput(temp);
						int numOfBytes = decoder.decode(pixels, offset, bytes2Read);							
						offset += numOfBytes;
					}
				}
				// This also works with 4 samples per pixel data
				if(predictor == 2 && planaryConfiguration == 1)
					pixels = applyDePredictor(samplesPerPixel, pixels, imageWidth, imageHeight);
				//Create a BufferedImage
				db = new DataBufferByte(pixels, pixels.length);
				// Get ICC_Profile
				TiffField<?> f_colorProfile = ifd.getField(TiffTag.ICC_PROFILE.getValue());
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
					short[] spixels = ArrayUtils.byteArrayToShortArray(pixels, endian == IOUtils.BIG_ENDIAN);
					db = new DataBufferUShort(spixels, spixels.length);
					cm = new ComponentColorModel(CMYKColorSpace.getInstance(), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_USHORT);
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
						raster = IMGUtils.CMYK2RGB(raster, cm);
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
						raster = IMGUtils.CMYK2RGB(raster, cm);
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_BYTE);
					}				
				}
				
				return new BufferedImage(cm, raster, false, null);
			case YCbCr:
				int[] samplingFactor = {2, 2}; // Default value, Not [1, 1]
				TiffField<?> f_YCbCrSubSampling = ifd.getField(TiffTag.YCbCr_SUB_SAMPLING.getValue());
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
			    
			    TiffField<?> f_referenceBlackWhite = ifd.getField(TiffTag.REFERENCE_BLACK_WHITE.getValue());
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
				
				TiffField<?> f_YCbCrCoefficients = ifd.getField(TiffTag.YCbCr_COEFFICIENTS.getValue());
				
				if(f_YCbCrCoefficients != null) {
					int[] lumas = f_YCbCrCoefficients.getDataAsLong();
					lumaRed = 1.0f*lumas[0]/lumas[1];
					lumaGreen = 1.0f*lumas[2]/lumas[3];
					lumaBlue = 1.0f*lumas[4]/lumas[5];
				}
											
				int offsetX = 0;
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
								int len = stripByteCounts[i];
								temp = new byte[len];
								randIS.readFully(temp);
								
								offset = 0;
								offsetX = 0;
								
								int numOfDataUnit = len/bytesPerDataUnit;
						
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
							}							
							
							break;
						case LZW:
							for(int i = 0; i < stripByteCounts.length; i++) {
								randIS.seek(stripOffsets[i]);
								int len = stripByteCounts[i];
								temp = new byte[len];
								randIS.readFully(temp);
								temp2 = new byte[stripBytes[i]];
								decoder = new LZWTreeDecoder(8, true);
								decoder.setInput(temp);
								int numOfBytes = decoder.decode(temp2, 0, temp2.length);	
								
								offset = 0;
								offsetX = 0;
								
								int numOfDataUnit = numOfBytes/bytesPerDataUnit;
								
								for(int j = 1; j <= numOfDataUnit; j++) {
									int offsetCb = offset + bytesPerUnitY;
									int Cb = temp2[offsetCb]&0xff;
									int Cr = temp2[offsetCb + 1]&0xff;
									for(int k = 0; k < samplingFactor[1]; k++) {// Rows
										for(int l = 0; l < samplingFactor[0]; l++, offsetX++) { // Columns
											// Populate the pixels array
											int Y = temp2[offset++]&0xff;
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
											float R =  (fCr * (2 - 2 * lumaRed) + fY);
											float B = (fCb * (2 - 2 * lumaBlue) + fY);
											float G = ((fY - lumaBlue * B - lumaRed * R) / lumaGreen);
											// This is very important!!!
											if(R < 0) R = 0;
											if(R > 255) R = 255;
											if(G < 0) G = 0;
											if(G > 255) G = 255;
											if(B < 0) B = 0;
											if(B > 255) B = 255;
											
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
							}
							
							break;
						default:
					}				
				} else {
					// Planar configuration
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
									temp = new byte[len];
									randIS.readFully(temp);
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
						break;
					case PACKBITS:
						for(int i = 0; i < stripByteCounts.length; i++) {
							int bytes2Read = stripBytes[i];
							temp = new byte[stripByteCounts[i]];
							randIS.seek(stripOffsets[i]);
							randIS.readFully(temp);
							temp2 = new byte[bytes2Read];
							Packbits.unpackbits(temp, temp2);
							System.arraycopy(temp2, 0, pixels, offset, bytes2Read);			
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
						temp = new byte[stripByteCounts[i]];
						randIS.seek(stripOffsets[i]);
						randIS.readFully(temp);
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
				//band offset, we have 3 bands if no extra sample is specified, otherwise 4
				bandoff = new int[]{0, 1, 2};
				nBits = new int[]{bitsPerSample, bitsPerSample, bitsPerSample};
				transparent = false;
				numOfBands = samplesPerPixel;
				trans = Transparency.OPAQUE;
				if(samplesPerPixel == 4) {
					bandoff = new int[]{0, 1, 2, 3};
					nBits = new int[]{bitsPerSample, bitsPerSample, bitsPerSample, bitsPerSample};
					trans = Transparency.TRANSLUCENT;
					transparent = true;
				}				
				if(bitsPerSample == 16) {
					short[] spixels = ArrayUtils.byteArrayToShortArray(pixels, endian == IOUtils.BIG_ENDIAN);
					db = new DataBufferUShort(spixels, spixels.length);
				} else {
					//Create a BufferedImage
					db = new DataBufferByte(pixels, pixels.length);					
				}
				if(planaryConfiguration == 2) {
					bandoff = new int[]{0, count[0]*8/bitsPerSample, (count[0] + count[1])*8/bitsPerSample};
					int[] bankIndices = new int[]{0, 0, 0};
					if(samplesPerPixel == 4) {
						bandoff = new int[]{0, count[0]*8/bitsPerSample, (count[0] + count[1])*8/bitsPerSample, (count[0] + count[1] + count[2])*8/bitsPerSample};
						bankIndices = new int[]{0, 0, 0, 0};
					}
					if(bitsPerSample < 8) {						
						raster = Raster.createPackedRaster(db, imageWidth, imageHeight, samplesPerPixel*bitsPerSample, null);
					} else
						raster = Raster.createBandedRaster(db, imageWidth, imageHeight, bytesPerScanLine*8/bitsPerSample, bankIndices, bandoff, null);					
				} else {
					if(bitsPerSample < 8) {
						Object tempArray = ArrayUtils.toNBits(bitsPerSample*samplesPerPixel, pixels, imageWidth, true);
						cm = new DirectColorModel(bitsPerSample*samplesPerPixel, (int)redMask[bitsPerSample], (int)greenMask[bitsPerSample], (int)blueMask[bitsPerSample]);
						raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
						raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);
					} else if(bitsPerSample == 8) {
						raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth*numOfBands, numOfBands, bandoff, null);
					} else {
						Object tempArray = ArrayUtils.toNBits(bitsPerSample, pixels, samplesPerPixel*imageWidth, endian == IOUtils.BIG_ENDIAN);
						cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_USHORT);
						raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
						raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);
					}
				}
				if(bitsPerSample == 16)
					cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha,	trans, DataBuffer.TYPE_USHORT);
				else if(bitsPerSample == 8)
					cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha,	trans, DataBuffer.TYPE_BYTE);
			
				return new BufferedImage(cm, raster, false, null);
			default:
		 		break;
		}
		
		return null;
	}
	
	private BufferedImage decodeTiledTiff(IFD ifd) throws Exception {
		// Grab some of the TIFF fields we are interested in
		TiffField<?> field = ifd.getField(TiffTag.COMPRESSION.getValue());
		short[] data = (short[])field.getData();
		TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(data[0]&0xffff);
		System.out.println("Compression type: " + compression.getDescription());
		
		TiffField<?> f_tileOffsets = ifd.getField(TiffTag.TILE_OFFSETS.getValue());
		if(f_tileOffsets == null) f_tileOffsets = ifd.getField(TiffTag.STRIP_OFFSETS.getValue());
		
		TiffField<?> f_tileByteCounts = ifd.getField(TiffTag.TILE_BYTE_COUNTS.getValue());
		if(f_tileByteCounts == null) f_tileByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS.getValue());
		
		int[] tileOffsets = f_tileOffsets.getDataAsLong();
		int[] tileByteCounts = f_tileByteCounts.getDataAsLong();
		
		int imageWidth = ifd.getField(TiffTag.IMAGE_WIDTH.getValue()).getDataAsLong()[0];
		int imageHeight = ifd.getField(TiffTag.IMAGE_LENGTH.getValue()).getDataAsLong()[0];
		System.out.println("Image width: " + imageWidth);
		System.out.println("Image height: " + imageHeight);
		
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH.getValue());
		TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH.getValue());
		
		int tileWidth = imageWidth;
		if(f_tileWidth != null) tileWidth = f_tileWidth.getDataAsLong()[0];
		
		int tileLength = imageHeight;
		if(f_tileLength != null) tileLength = f_tileLength.getDataAsLong()[0];
		
		TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue());
		int photoMetric = f_photoMetric.getDataAsLong()[0];
		TiffFieldEnum.PhotoMetric e_photoMetric = TiffFieldEnum.PhotoMetric.fromValue(photoMetric);
		System.out.println("PhotoMetric: " + e_photoMetric);
		
		TiffField<?> f_bitsPerSample = ifd.getField(TiffTag.BITS_PER_SAMPLE.getValue());
		int bitsPerSample = f_bitsPerSample.getDataAsLong()[0];
		System.out.println("Bits per sample: " + bitsPerSample);
		
		TiffField<?> f_samplesPerPixel = ifd.getField(TiffTag.SAMPLES_PER_PIXEL.getValue());
		int samplesPerPixel = f_samplesPerPixel.getDataAsLong()[0];
		System.out.println("Samples per pixel: " + samplesPerPixel);
		
		TiffField<?> f_predictor = ifd.getField(TiffTag.PREDICTOR.getValue());
		int predictor = 0;
		if(f_predictor != null) {
			predictor = f_predictor.getDataAsLong()[0];
			System.out.println("Predictor: " + predictor);
		}
		
		TiffField<?> f_planaryConfiguration = ifd.getField(TiffTag.PLANAR_CONFIGURATTION.getValue());
		int planaryConfiguration = 1;
		if(f_planaryConfiguration != null) planaryConfiguration = f_planaryConfiguration.getDataAsLong()[0];
		TiffFieldEnum.PlanarConfiguration e_planaryConfiguration = TiffFieldEnum.PlanarConfiguration.fromValue(planaryConfiguration);
		System.out.println("Planary configuration: " + e_planaryConfiguration);
		
		int tilesAcross = (imageWidth + tileWidth - 1) / tileWidth;
		int tilesDown = (imageHeight + tileLength - 1) / tileLength;
		int tilesPerImage = tilesAcross * tilesDown;
		
		ImageDecoder decoder = null;
		byte[] pixels = null;
		
		int bytes2Read = ((tileWidth*bitsPerSample + 7)/8)*samplesPerPixel*tileLength;
		
		int[] stripBytes = TIFFTweaker.getUncompressedStripByteCounts(ifd, tileOffsets.length);
		
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
			case RGB:
				//Create a BufferedImage
				pixels = new byte[tileLength*((tileWidth*bitsPerSample +7)/8)*samplesPerPixel*tilesPerImage];
				short[] spixels = null;
				DataBuffer db = new DataBufferByte(pixels, pixels.length);
				//band offset, we have 3 bands if no extra sample is specified, otherwise 4
				int[] bandoff = new int[]{0, 1, 2}; 
				boolean transparent = false;
				int numOfBands = samplesPerPixel;
				int trans = Transparency.OPAQUE;
				int[] nBits = new int[]{bitsPerSample, bitsPerSample, bitsPerSample};
				// There is an extra sample (most probably alpha)
				if(samplesPerPixel == 4) {
					bandoff = new int[]{0, 1, 2, 3};
					nBits = new int[]{bitsPerSample, bitsPerSample, bitsPerSample, bitsPerSample};
					trans = Transparency.TRANSLUCENT;
					transparent = true;
				}
				ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, false,
			                trans, DataBuffer.TYPE_BYTE);
				if(bitsPerSample == 16) {
					spixels = new short[pixels.length];
					db = new DataBufferUShort(spixels, spixels.length);
					cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, false,
			                trans, DataBuffer.TYPE_USHORT);
				}
				WritableRaster raster = Raster.createInterleavedRaster(db, tileWidth*tilesAcross, tileLength*tilesDown, tileWidth*tilesAcross*numOfBands, numOfBands, bandoff, null);
				// Now we are going to read and set tiles to the raster
				int xoff = 0;
				int yoff = 0;
				int tileCounter = 0;
				switch(compression) {
					case NONE:
						if(planaryConfiguration == 1) {
							for(int i = 0; i < tileByteCounts.length; i++) {
								byte[] temp = new byte[tileByteCounts[i]];
								randIS.seek(tileOffsets[i]);
								randIS.readFully(temp);
								if(bitsPerSample == 16) {
									raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.byteArrayToShortArray(temp, endian == IOUtils.BIG_ENDIAN));
								} else
									raster.setDataElements(xoff, yoff, tileWidth, tileLength, temp);
								xoff += tileWidth;
								tileCounter++;
								if(tileCounter >= tilesAcross) {
									xoff = 0;
									yoff += tileLength;
									tileCounter = 0;
								}						
							}
						} else {							
							byte[][] rgb = new byte[samplesPerPixel][];
							Raster tileRaster = null;
							DataBuffer tileDatabuffer = null;
							int[] bankIndices = {0, 1, 2};
							bandoff = new int[]{0, 0, 0};
							raster = Raster.createBandedRaster(db, tileWidth*tilesAcross, tileLength*tilesDown, tileLength*tilesAcross, new int[]{0, 0, 0}, new int[]{0, count[0]*8/bitsPerSample, (count[0] + count[1])*8/bitsPerSample}, null);
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
								if(bitsPerSample == 16) {
									tileDatabuffer = new DataBufferUShort(new short[][]{ArrayUtils.byteArrayToShortArray(rgb[0], endian == IOUtils.BIG_ENDIAN), 
											ArrayUtils.byteArrayToShortArray(rgb[1], endian == IOUtils.BIG_ENDIAN), 
											ArrayUtils.byteArrayToShortArray(rgb[2], endian == IOUtils.BIG_ENDIAN)},
											tileWidth);
								} else {
									tileDatabuffer = new DataBufferByte(rgb, rgb[0].length);
								}
								tileRaster = Raster.createBandedRaster(tileDatabuffer, tileWidth, tileLength, tileWidth, bankIndices, bandoff, null);
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
								raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.byteArrayToShortArray(temp2, endian == IOUtils.BIG_ENDIAN));
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
							raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.byteArrayToShortArray(temp2, endian == IOUtils.BIG_ENDIAN));
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
	 
	private boolean readHeader(RandomAccessInputStream randIS) throws IOException {
		// First 2 bytes determine the byte order of the file
		endian = randIS.readShort();
		
		if(endian == IOUtils.BIG_ENDIAN)
		{
			System.out.println("Byte order: Motorola BIG_ENDIAN");
			this.randIS.setReadStrategy(ReadStrategyMM.getInstance());
		} else if(endian == IOUtils.LITTLE_ENDIAN) {
			System.out.println("Byte order: Intel LITTLE_ENDIAN");
			this.randIS.setReadStrategy(ReadStrategyII.getInstance());
		} else {
			System.out.println("Warning: invalid TIFF byte order!");
			return false;
		} 
		
		// Read TIFF identifier
		short tiff_id = randIS.readShort();
		  
		if(tiff_id!=0x2a)//"*" 42 decimal
		{
			System.out.println("Warning: invalid tiff identifier");
			return false;
		}
		  
		return true;
	}
	 
	private int readIFD(int id, int offset) throws IOException 
	{
		IFD tiffIFD = new IFD();
		System.out.println("IFD " + id + " offset: byte " + offset);
		randIS.seek(offset);
		int no_of_fields = randIS.readShort();
		System.out.println("Total number of fields for IFD " + id +": " + no_of_fields);
		offset += 2;
		
		for (int i=0;i<no_of_fields;i++)
		{
			System.out.println("TiffField "+i+" =>");
			randIS.seek(offset);
			short tag = randIS.readShort();
			Tag ftag = TiffTag.fromShort(tag);
			if (ftag == TiffTag.UNKNOWN)
				System.out.println("TiffTag: " + ftag + " [Value: 0x"+ Integer.toHexString(tag&0xffff) + "]" + " (Unknown)");
			else
				System.out.println("TiffTag: " + ftag);
			offset += 2;
			randIS.seek(offset);
			short type = randIS.readShort();
			FieldType ftype = FieldType.fromShort(type);
			System.out.println("Data type: " + ftype);
			offset += 2;
			randIS.seek(offset);
			int field_length = randIS.readInt();
			System.out.println("TiffField length: " + field_length);
			offset += 4;
			////// Try to read actual data.
			switch (ftype)
			{
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
					System.out.println("TiffField value: " + StringUtils.byteArrayToHexString(data, 0, 10));
					offset += 4;					
					tiffIFD.addField((ftype == FieldType.BYTE)?new ByteField(tag, data):
						new UndefinedField(tag, data));
					break;
				case ASCII:
					data = new byte[field_length];
					if(field_length <= 4) {
						randIS.seek(offset);
						randIS.readFully(data, 0, field_length);
					}						
					else {
						randIS.seek(offset);
						randIS.seek(randIS.readInt());
						randIS.readFully(data, 0, field_length);
					}
					if(data.length>0)
					  System.out.println("TiffField value: " + new String(data, 0, data.length-1).trim());
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
					System.out.println("TiffField value: " + StringUtils.shortArrayToString(sdata, true));
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
					System.out.println("TiffField value: " + StringUtils.longArrayToString(ldata, true));
					tiffIFD.addField(new LongField(tag, ldata));
					break;
				case RATIONAL:
					int len = 2*field_length;
					ldata = new int[len];	
					randIS.seek(offset);
					int toOffset = randIS.readInt();
					offset += 4;					
					for (int j=0;j<len; j+=2){
						randIS.seek(toOffset);
						ldata[j] = randIS.readInt();
						toOffset += 4;
						randIS.seek(toOffset);
						ldata[j+1] = randIS.readInt();
						toOffset += 4;
					}	
					tiffIFD.addField(new RationalField(tag, ldata));
					System.out.println("TiffField value: " + StringUtils.rationalArrayToString(ldata, true));
					break;
				default:
					offset += 4;
					break;					
			  }	
		}
		list.add(tiffIFD);
		System.out.println("********************************");
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
	
	// De-predictor for PLANARY_CONFIGURATION value 2
	private static byte[] applyDePredictor2(byte[] input, int offset, int imageWidth, int imageHeight) {
		for(int i = imageHeight - 1, inc = imageWidth, maxVal = offset + inc - 1, minVal = offset + 1; i >= 0; maxVal += inc, minVal += inc,  i--) {
			for (int j = minVal; j < maxVal; j++) {
				input[j] += input[j - 1];
			}
		}
		return input;
	}
}