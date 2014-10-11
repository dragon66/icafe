/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.reader;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cafe.image.compression.deflate.DeflateDecoder;
import cafe.image.compression.lzw.LZWTreeDecoder;
import cafe.image.compression.packbits.Packbits;
import cafe.image.tiff.ASCIIField;
import cafe.image.tiff.ByteField;
import cafe.image.tiff.IFD;
import cafe.image.tiff.LongField;
import cafe.image.tiff.RationalField;
import cafe.image.tiff.ShortField;
import cafe.image.tiff.Tag;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffFieldEnum;
import cafe.image.tiff.TiffTag;
import cafe.image.tiff.FieldType;
import cafe.image.tiff.UndefinedField;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.io.ReadStrategyII;
import cafe.io.ReadStrategyMM;
import cafe.string.StringUtils;

/** 
 * Decodes and shows TIFF images. 
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/09/2012  
 */
public class TIFFReader extends ImageReader {
	private RandomAccessInputStream randIS = null;
	private List<IFD> list = new ArrayList<IFD>();
	 
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
		TiffField<?> field = ifd.getField(TiffTag.COMPRESSION.getValue());
		short[] data = (short[])field.getData();
		TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(data[0]&0xffff);
		System.out.println("Compression type: " + compression.getDescription());
		// Forget about tiled TIFF for now
		TiffField<?> f_stripOffsets = ifd.getField(TiffTag.STRIP_OFFSETS.getValue());
		TiffField<?> f_stripByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS.getValue());
		TiffField<?> f_rowsPerStrip = ifd.getField(TiffTag.ROWS_PER_STRIP.getValue());
		int[] stripOffsets = f_stripOffsets.getDataAsLong();
		int[] stripByteCounts = f_stripByteCounts.getDataAsLong();
		System.out.println(Arrays.toString(stripOffsets));
		System.out.println(Arrays.toString(stripByteCounts));
		int rowsPerStrip = -1;
		if(f_rowsPerStrip != null)
			rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];
		System.out.println(rowsPerStrip);
		int imageWidth = ifd.getField(TiffTag.IMAGE_WIDTH.getValue()).getDataAsLong()[0];
		int imageHeight = ifd.getField(TiffTag.IMAGE_LENGTH.getValue()).getDataAsLong()[0];
		System.out.println("Image width: " + imageWidth);
		System.out.println("Image height: " + imageHeight);
		TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue());
		int photoMetric = f_photoMetric.getDataAsLong()[0];
		TiffFieldEnum.PhotoMetric e_photoMetric = TiffFieldEnum.PhotoMetric.fromValue(photoMetric);
		System.out.println("PhotoMetric: " + e_photoMetric);
		TiffField<?> f_bitsPerSample = ifd.getField(TiffTag.BITS_PER_SAMPLE.getValue());
		int bitsPerSample = f_bitsPerSample.getDataAsLong()[0];
		System.out.println("Bits per sample: " + bitsPerSample);
		
		if(rowsPerStrip < 0) rowsPerStrip = imageHeight;
		int rowsRemain = imageHeight;
		int offset = 0;
		
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
				byte[] pixels = new byte[bytesPerScanLine*imageHeight];				
				if(compression == TiffFieldEnum.Compression.LZW) {
					LZWTreeDecoder decoder = new LZWTreeDecoder(8, true);					
					for(int i = 0; i < stripByteCounts.length; i++) {
						byte[] temp = new byte[stripByteCounts[i]];
						randIS.seek(stripOffsets[i]);
						randIS.readFully(temp);
						decoder.setInput(temp);
						int numOfBytes = decoder.decode(pixels, offset, Math.min(rowsPerStrip,rowsRemain)*bytesPerScanLine);
						offset += numOfBytes;
						rowsRemain -= Math.min(rowsPerStrip, rowsRemain);
					}
				}  else if(compression == TiffFieldEnum.Compression.DEFLATE || compression == TiffFieldEnum.Compression.DEFLATE_ADOBE) {
					DeflateDecoder decoder = new DeflateDecoder();
					for(int i = 0; i < stripByteCounts.length; i++) {
						byte[] temp = new byte[stripByteCounts[i]];
						randIS.seek(stripOffsets[i]);
						randIS.readFully(temp);
						decoder.setInput(temp);
						int numOfBytes = decoder.decode(pixels, offset, Math.min(rowsPerStrip,rowsRemain)*bytesPerScanLine);
						offset += numOfBytes;
						rowsRemain -= Math.min(rowsPerStrip, rowsRemain);
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
			case RGB:
				pixels = new byte[imageWidth*imageHeight*3];				
				if(compression == TiffFieldEnum.Compression.NONE) {
					for(int i = 0; i < stripByteCounts.length; i++) {					
						randIS.seek(stripOffsets[i]);
						randIS.readFully(pixels, offset, Math.min(rowsPerStrip, rowsRemain)*imageWidth*3);
						offset += Math.min(rowsPerStrip, rowsRemain)*imageWidth*3;
						rowsRemain -= Math.min(rowsPerStrip, rowsRemain);					
					}
				} else if(compression == TiffFieldEnum.Compression.LZW) {
					LZWTreeDecoder decoder = new LZWTreeDecoder(8, true);
					for(int i = 0; i < stripByteCounts.length; i++) {
						byte[] temp = new byte[stripByteCounts[i]];
						randIS.seek(stripOffsets[i]);
						randIS.readFully(temp);
						decoder.setInput(temp);
						int bytes2Read = Math.min(rowsPerStrip, rowsRemain)*imageWidth*3;
						int numOfBytes = decoder.decode(pixels, offset, bytes2Read);							
						offset += numOfBytes;
						rowsRemain -= Math.min(rowsPerStrip, rowsRemain);					
					}					
				} else if(compression == TiffFieldEnum.Compression.DEFLATE || compression == TiffFieldEnum.Compression.DEFLATE_ADOBE) {
					DeflateDecoder decoder = new DeflateDecoder();
					for(int i = 0; i < stripByteCounts.length; i++) {
						byte[] temp = new byte[stripByteCounts[i]];
						randIS.seek(stripOffsets[i]);
						randIS.readFully(temp);
						decoder.setInput(temp);
						int bytes2Read = Math.min(rowsPerStrip, rowsRemain)*imageWidth*3;
						int numOfBytes = decoder.decode(pixels, offset, bytes2Read);							
						offset += numOfBytes;
						rowsRemain -= Math.min(rowsPerStrip, rowsRemain);					
					}					
				} else if(compression == TiffFieldEnum.Compression.PACKBITS) {
					for(int i = 0; i < stripByteCounts.length; i++) {
						byte[] temp = new byte[stripByteCounts[i]];
						randIS.seek(stripOffsets[i]);
						randIS.readFully(temp);
						int bytes2Read = Math.min(rowsPerStrip, rowsRemain)*imageWidth*3;
						byte[] temp2 = new byte[bytes2Read];
						Packbits.unpackbits(temp, temp2);
						System.arraycopy(temp2, 0, pixels, offset, bytes2Read);							
						offset += bytes2Read;
						rowsRemain -= Math.min(rowsPerStrip, rowsRemain);					
					}					
				}
				//Create a BufferedImage
				db = new DataBufferByte(pixels, pixels.length);
				int[] bandoff = {0, 1, 2}; //band offset, we have 3 bands
				int numOfBands = 3;
				int trans = Transparency.OPAQUE;
				int[] nBits = {8, 8, 8};						
				raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth*numOfBands, numOfBands, bandoff, null);
				cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, false, false,
			                trans, DataBuffer.TYPE_BYTE);
					
				return new BufferedImage(cm, raster, false, null);						
			default:
		 		break;
		}	
		
		return null;
	}
	 
	private boolean readHeader(RandomAccessInputStream randIS) throws IOException {
		// First 2 bytes determine the byte order of the file
		short endian = randIS.readShort();
		
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
					System.out.println("TiffField value: " + StringUtils.byteArrayToHexString(data));
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
}