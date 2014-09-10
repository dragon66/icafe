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

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import cafe.image.tiff.ASCIIField;
import cafe.image.tiff.ByteField;
import cafe.image.tiff.IFD;
import cafe.image.tiff.LongField;
import cafe.image.tiff.RationalField;
import cafe.image.tiff.ShortField;
import cafe.image.tiff.Tag;
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
public class TIFFReader extends ImageReader
{  	
	 private RandomAccessInputStream TIFFIN = null;
	 private List<IFD> list = new ArrayList<IFD>();
	 
	 public BufferedImage read(InputStream is) throws Exception
     {		  
		  TIFFIN = new FileCacheRandomAccessInputStream(is);
		  int length = 0;
		  // First 2 bytes determine the byte order of the file
		  short endian = TIFFIN.readShort();
		  length += 2;
		
		  if (endian == IOUtils.BIG_ENDIAN)
		  {
			  System.out.println("Byte order: Motorola BIG_ENDIAN");
			  this.TIFFIN.setReadStrategy(ReadStrategyMM.getInstance());
		  }
		  else if(endian == IOUtils.LITTLE_ENDIAN)
		  {
			  System.out.println("Byte order: Intel LITTLE_ENDIAN");
			  this.TIFFIN.setReadStrategy(ReadStrategyII.getInstance());
		  }
		  else {
			  System.out.println("Warning: invalid TIFF byte order!");
			  return null;
		  } 
		
		  // Read TIFF identifier
		  TIFFIN.seek(length);
		  short tiff_id = TIFFIN.readShort();
		  length +=2;
		  
		  if(tiff_id!=0x2a)//"*" 42 decimal
		  {
			  System.out.println("Warning: invalid tiff identifier");
			  return null;
		  }
		 
		  TIFFIN.seek(length);
		  int offset = TIFFIN.readInt();
		  length += 4;
		 	
		  int ifd = 0;
				
		  while (offset != 0)
		  {
			  offset = readIFD(ifd++, offset);
		  }
		  
		  TIFFIN.close();
		  
		  return null;
     }
	 
	 private int readIFD(int id, int offset) throws IOException 
	 {
		  IFD tiffIFD = new IFD();
		  System.out.println("IFD " + id + " offset: byte " + offset);
		  TIFFIN.seek(offset);
		  int no_of_fields = TIFFIN.readShort();
		  System.out.println("Total number of fields for IFD " + id +": " + no_of_fields);
		  offset += 2;
		
		  for (int i=0;i<no_of_fields;i++)
		  {
			  System.out.println("TiffField "+i+" =>");
			  TIFFIN.seek(offset);
			  short tag = TIFFIN.readShort();
			  Tag ftag = TiffTag.fromShort(tag);
			  if (ftag == TiffTag.UNKNOWN)
				  System.out.println("TiffTag: " + ftag + " [Value: 0x"+ Integer.toHexString(tag&0xffff) + "]" + " (Unknown)");
			  else
				  System.out.println("TiffTag: " + ftag);
			  offset += 2;
			  TIFFIN.seek(offset);
			  short type = TIFFIN.readShort();
			  FieldType ftype = FieldType.fromShort(type);
			  System.out.println("Data type: " + ftype);
			  offset += 2;
			  TIFFIN.seek(offset);
			  int field_length = TIFFIN.readInt();
			  System.out.println("TiffField length: " + field_length);
			  offset += 4;
			  ////// Try to read actual data.
			  switch (ftype)
			  {
			  	case BYTE:
				case UNDEFINED:
					byte[] data = new byte[field_length];
					if(field_length <= 4) {
						TIFFIN.seek(offset);
						TIFFIN.readFully(data, 0, field_length);					   
					}
					else {
						TIFFIN.seek(offset);
						TIFFIN.seek(TIFFIN.readInt());
						TIFFIN.readFully(data, 0, field_length);
					}
					System.out.println("TiffField value: " + StringUtils.byteArrayToHexString(data));
					offset += 4;					
					tiffIFD.addField((ftype == FieldType.BYTE)?new ByteField(tag, data):
						new UndefinedField(tag, data));
					break;
				case ASCII:
					data = new byte[field_length];
					if(field_length <= 4) {
						TIFFIN.seek(offset);
						TIFFIN.readFully(data, 0, field_length);
					}						
					else {
						TIFFIN.seek(offset);
						TIFFIN.seek(TIFFIN.readInt());
						TIFFIN.readFully(data, 0, field_length);
					}
					if(data.length>0)
					  System.out.println("TiffField value: " + new String(data, 0, data.length-1).trim());
					offset += 4;	
					tiffIFD.addField(new ASCIIField(tag, new String(data, 0, data.length)));
			        break;
				case SHORT:
					short[] sdata = new short[field_length];
					if(field_length == 1) {
					  TIFFIN.seek(offset);
					  sdata[0] = TIFFIN.readShort();
					  offset += 4;
					}
					else if (field_length == 2)
					{
						TIFFIN.seek(offset);
						sdata[0] = TIFFIN.readShort();
						offset += 2;
						TIFFIN.seek(offset);
						sdata[1] = TIFFIN.readShort();
						offset += 2;
					}
					else {
						TIFFIN.seek(offset);
						int toOffset = TIFFIN.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							TIFFIN.seek(toOffset);
							sdata[j] = TIFFIN.readShort();
							toOffset += 2;
						}
					}	
					tiffIFD.addField(new ShortField(tag, sdata));
					System.out.println("TiffField value: " + StringUtils.shortArrayToString(sdata, true));
					break;
				case LONG:
					int[] ldata = new int[field_length];
					if(field_length == 1) {
					  TIFFIN.seek(offset);
					  ldata[0] = TIFFIN.readInt();
					  offset += 4;
					}
					else {
						TIFFIN.seek(offset);
						int toOffset = TIFFIN.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							TIFFIN.seek(toOffset);
							ldata[j] = TIFFIN.readInt();
							toOffset += 4;
						}
					}	
					System.out.println("TiffField value: " + StringUtils.longArrayToString(ldata, true));
					tiffIFD.addField(new LongField(tag, ldata));
					break;
				case RATIONAL:
					int len = 2*field_length;
					ldata = new int[len];	
					TIFFIN.seek(offset);
					int toOffset = TIFFIN.readInt();
					offset += 4;					
					for (int j=0;j<len; j+=2){
						TIFFIN.seek(toOffset);
						ldata[j] = TIFFIN.readInt();
						toOffset += 4;
						TIFFIN.seek(toOffset);
						ldata[j+1] = TIFFIN.readInt();
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
		  TIFFIN.seek(offset);
		  return TIFFIN.readInt();
	 }
}