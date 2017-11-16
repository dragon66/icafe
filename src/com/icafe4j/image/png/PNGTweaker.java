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
 * PNGTweaker.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    30Mar2016  Changed removeAncillaryChunks() method signature
 * WY    30Mar2016  Added insertTextChunk()
 * WY    06Jul2015  Added insertXMP(InputSream, OutputStream, XMP)
 * WY    30Mar2015  Added insertICCProfile()
 * WY    27Mar2015  Revised insertXMP() to remove old XMP
 * WY    03Mar2015  Added insertXMP() to insert XMP to iTXT chunk
 * WY    11Feb2015  Added code to extract XMP from iTXT chunk
 * WY    20Jan2015  Revised to work with Metadata.showMetadata()
 * WY    13Jan2015  Split remove_ancillary_chunks() arguments
 * WY    22Dec2014  Added readICCProfleChunk() to read ICC_Profile
 * WY    22Dec2014  dumpTextChunks() now calls readTextChunks()
 */

package com.icafe4j.image.png;

import java.awt.color.ICC_Profile;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.icc.ICCProfile;
import com.icafe4j.image.meta.png.PngXMP;
import com.icafe4j.image.meta.png.TIMEChunk;
import com.icafe4j.image.meta.png.TextualChunks;
import com.icafe4j.image.meta.xmp.XMP;
import com.icafe4j.io.IOUtils;
import com.icafe4j.string.StringUtils;
import com.icafe4j.string.XMLUtils;
import com.icafe4j.util.ArrayUtils;

/**
 * PNG image tweaking tool
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/18/2012
 */
public class PNGTweaker {
	
	/** PNG signature constant */
    private static final long SIGNATURE = 0x89504E470D0A1A0AL;
	
	private static Set<ChunkType> REMOVABLE = EnumSet.range(ChunkType.TEXT, ChunkType.TIME);    
    
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(PNGTweaker.class);

   	public static void dumpTextChunks(Chunk[] chunks) throws IOException {
   		TextReader reader = new TextReader();
   		for (Chunk chunk : chunks) {
   			if ((chunk.getChunkType() == ChunkType.TEXT) || (chunk.getChunkType() == ChunkType.ITXT) || 
   					(chunk.getChunkType() == ChunkType.ZTXT)) {
   				reader.setInput(chunk);
	   			LOGGER.info("{}: {}", reader.getKeyword(), reader.getText());
   			}
   		}   	
   	}
	
	// Dump text chunks
   	public static void dumpTextChunks(InputStream is) throws IOException {
   		LOGGER.info("\n{}", readTextChunks(is));
    }

  	public static void insertChunk(Chunk chunk, InputStream is, OutputStream os) throws IOException {
  		insertChunks(is, os, chunk);
  	}
  	
  	public static void insertChunks(InputStream is, OutputStream os, Chunk... chunks) throws IOException {
  		List<Chunk> list = readChunks(is);  		
        Collections.addAll(list, chunks);
    	
  		IOUtils.writeLongMM(os, SIGNATURE);
	
        serializeChunks(list, os);
  	}
  	
  	public static void insertChunks(List<Chunk> chunks, InputStream is, OutputStream os) throws IOException	{
  		List<Chunk> list = readChunks(is);  		
        list.addAll(chunks);
    	
  		IOUtils.writeLongMM(os, SIGNATURE);
	
        serializeChunks(list, os);
  	}
  	
  	public static void insertComments(InputStream is, OutputStream os, List<String> comments) throws IOException {
  		// Build tEXt chunk
  		TextBuilder txtBuilder = new TextBuilder(ChunkType.TEXT);
  		int numOfComments = comments.size();
  		Chunk[] chunks = new Chunk[numOfComments];
  		for(int i = 0; i < numOfComments; i++) {
  			chunks[i] = txtBuilder.keyword("Comment").text(comments.get(i)).build();
  		}
  		insertChunks(is, os, chunks);
  	}
  	
  	public static void insertICCProfile(String profile_name, byte[] icc_profile, InputStream is, OutputStream os) throws IOException {
  		ICCPBuilder builder = new ICCPBuilder();
  		builder.name(profile_name);
  		builder.data(icc_profile);
  		insertChunk(builder.build(), is, os);
  	}
  	
  	public static void insertICCProfile(String profile_name, ICC_Profile icc_profile, InputStream is, OutputStream os) throws IOException {
  		insertICCProfile(profile_name, icc_profile.getData(), is, os);
  	}
  	
  	public static void insertTextChunk(ChunkType type, String keyword, String text, InputStream is, OutputStream os) throws IOException {
  		if(type == null || keyword == null || text == null)
  			throw new IllegalArgumentException("Argument(s) are null");
  		
  		insertChunk(new TextBuilder(type).keyword(keyword).text(text).build(), is, os);
  	}
  	
  	public static void insertTextChunks(TextualChunks textualChunks, InputStream is, OutputStream os) throws IOException {
  		if(textualChunks == null) throw new IllegalArgumentException("Argument is null");
  		insertChunks(textualChunks.getChunks(), is, os);
  	}
  	
  	public static void insertXMP(InputStream is, OutputStream os, XMP xmp) throws IOException {
  		insert(is, os, XMLUtils.serializeToString(xmp.getMergedDocument()));
  	}
  	
  	// Add leading and trailing PI
  	public static void insertXMP(InputStream is, OutputStream os, String xmp) throws IOException {
  		Document doc = XMLUtils.createXML(xmp);
		XMLUtils.insertLeadingPI(doc, "xpacket", "begin='' id='W5M0MpCehiHzreSzNTczkc9d'");
		XMLUtils.insertTrailingPI(doc, "xpacket", "end='r'");
		String newXmp = XMLUtils.serializeToString(doc); // DONOT use XMLUtils.serializeToStringLS()
  		insert(is, os, newXmp);
    }
  	
  	private static void insert(InputStream is, OutputStream os, String xmp) throws IOException {
  		// Read all the chunks first
  		List<Chunk> chunks = readChunks(is);
	    ListIterator<Chunk> itr = chunks.listIterator();
	    
	    // Remove old XMP chunk
	    while(itr.hasNext()) {
	    	Chunk chunk = itr.next();
	    	if(chunk.getChunkType() == ChunkType.ITXT) {
	    		TextReader reader = new TextReader(chunk);
				if(reader.getKeyword().equals("XML:com.adobe.xmp")); // We found XMP data
					itr.remove();
	    	}
	    }
	    
	    // Create XMP textual chunk
		Chunk xmpChunk = new TextBuilder(ChunkType.ITXT).keyword("XML:com.adobe.xmp").text(xmp).build();
		// Insert XMP textual chunk into image
	    chunks.add(xmpChunk);
	    
	    IOUtils.writeLongMM(os, SIGNATURE);
	    
        serializeChunks(chunks, os);
    }
  	
  	public static List<Chunk> mergeIDATChunks(List<Chunk> chunks) {
   		
   		Iterator<Chunk> iter = chunks.listIterator();
   		byte[] data = new byte[0];
   		
   		while(iter.hasNext()) {
   			Chunk chunk = iter.next();
   		
   			if (chunk.getChunkType() == ChunkType.IDAT) {
   				data = ArrayUtils.concat(data, chunk.getData());
   				iter.remove();
   			}   			
   		}
   		
   		chunks.add(new Chunk(ChunkType.IDAT, data.length, data, Chunk.calculateCRC(ChunkType.IDAT.getValue(), data)));

   		return chunks;
   	}
  	
   	public static byte[] readICCProfileChunk(InputStream is) throws IOException {
  		//Local variables for reading chunks
        int data_len = 0;
        int chunk_value = 0;
        byte[] buf = null;
    
        long signature = IOUtils.readLongMM(is);

        if (signature != SIGNATURE) {
        	throw new RuntimeException("Invalid PNG signature");
        }

        /** Read header */
        /** We are expecting IHDR */
        if ((IOUtils.readIntMM(is) != 13)||(IOUtils.readIntMM(is) != ChunkType.IHDR.getValue())) {
        	throw new RuntimeException("Invalid PNG header");
        }

        buf = new byte[13 + 4];//13 plus 4 bytes CRC
        IOUtils.readFully(is, buf, 0, 17);

        while (true) {
            data_len = IOUtils.readIntMM(is);
            chunk_value = IOUtils.readIntMM(is);
      
            if (chunk_value == ChunkType.IEND.getValue()) {
                IOUtils.readIntMM(is);//CRC
                break;
            }
              
 			ChunkType chunk = ChunkType.fromInt(chunk_value);
 			   			 
            switch (chunk) {
            	case ICCP:
            		buf = new byte[data_len];
            		IOUtils.readFully(is, buf);            		 
            		IOUtils.skipFully(is, 4); // Skip CRC
            		return readICCProfile(buf);
            	default:
            		buf = new byte[data_len + 4];
            		IOUtils.readFully(is, buf, 0, data_len + 4);
            		break;
            }
        }
        
        return null;  		
  	}
  	
  	public static String readTextChunks(File file) throws IOException {
  		return readTextChunks(new FileInputStream(file));
  	}
  	
  	// Read text chunks to a String
   	public static String readTextChunks(InputStream is) throws IOException {
   		//Local variables for reading chunks
        int data_len = 0;
        int chunk_value = 0;
        byte[] buf = null;
        StringBuilder sb = new StringBuilder(1024);

        long signature = IOUtils.readLongMM(is);

        if (signature != SIGNATURE) {
        	throw new RuntimeException("ReadTextChunks: Invalid PNG signature");
        }

        /** Read header */
        /** We are expecting IHDR */
        if ((IOUtils.readIntMM(is) != 13)||(IOUtils.readIntMM(is) != ChunkType.IHDR.getValue())) {
        	throw new RuntimeException("ReadTextChunks: Invalid PNG header");
        }

        buf = new byte[13 + 4];//13 plus 4 bytes CRC
        IOUtils.readFully(is, buf, 0, 17);

        while (true) {
            data_len = IOUtils.readIntMM(is);
            chunk_value = IOUtils.readIntMM(is);
       
            if (chunk_value == ChunkType.IEND.getValue()) {
                IOUtils.readIntMM(is);//CRC
                break;
            }
              
 			ChunkType chunk = ChunkType.fromInt(chunk_value);
 			   			 
            switch (chunk) {
            	case ZTXT:
            		sb.append("zTXt chunk:\n");
              		sb.append("**********************\n");
            		buf = new byte[data_len];
            		IOUtils.readFully(is, buf);
            		int keyword_len = 0;
            		while(buf[keyword_len] != 0) keyword_len++;
            		sb.append(new String(buf, 0, keyword_len, "UTF-8"));
            		sb.append(": ");
            		InflaterInputStream ii = new InflaterInputStream(new ByteArrayInputStream(buf, keyword_len + 2, data_len - keyword_len - 2));
            		InputStreamReader ir = new InputStreamReader(ii, "UTF-8");
            		BufferedReader br = new BufferedReader(ir);                       
            		String read = null;
            		while((read = br.readLine()) != null) {
            			sb.append(read);
            			sb.append("\n");
                    }                  
            		sb.append("**********************\n");
            		br.close();
            		IOUtils.skipFully(is, 4);
            		break;
             	case TEXT:
            		sb.append("tEXt chunk:\n");
            		sb.append("**********************\n");
            		buf = new byte[data_len];
            		IOUtils.readFully(is, buf);
            		keyword_len = 0;
            		while(buf[keyword_len] != 0) keyword_len++;
            		sb.append(new String(buf, 0, keyword_len, "UTF-8"));
            		sb.append(": ");
            		sb.append(new String(buf, keyword_len + 1, data_len - keyword_len - 1, "UTF-8"));
            		sb.append("\n**********************\n");
            		IOUtils.skipFully(is, 4);
            		break;
            	case ITXT:
             		/**
            		 * Keyword:             1-79 bytes (character string)
            		 * Null separator:      1 byte
            		 * Compression flag:    1 byte
            		 * Compression method:  1 byte
            		 * Language tag:        0 or more bytes (character string)
            		 * Null separator:      1 byte
            		 * Translated keyword:  0 or more bytes
            		 * Null separator:      1 byte
            		 * Text:                0 or more bytes
            		 */
            		sb.append("iTXt chunk:\n");
              		sb.append("**********************\n");
            		buf = new byte[data_len];
            		IOUtils.readFully(is, buf);
            		keyword_len = 0;
            		int trans_keyword_len = 0;
            		int lang_flg_len = 0;
            		boolean compr = false;
            		while(buf[keyword_len] != 0) keyword_len++;
            		sb.append(new String(buf, 0, keyword_len, "UTF-8"));
            		if(buf[++keyword_len] == 1) compr = true;
            		keyword_len++;//Skip the compression method byte.
            		while(buf[++keyword_len] != 0) lang_flg_len++;
            		//////////////////////
            		sb.append("(");
            		if(lang_flg_len > 0)
            			sb.append(new String(buf, keyword_len - lang_flg_len, lang_flg_len, "UTF-8"));
            		while(buf[++keyword_len] != 0) trans_keyword_len++;
            		if(trans_keyword_len > 0) {
            			sb.append(" ");
            			sb.append(new String(buf, keyword_len - trans_keyword_len, trans_keyword_len, "UTF-8"));
            		}
            		sb.append("): ");
            		/////////////////////// End of key.
            		if(compr) { //Compressed text
            			ii = new InflaterInputStream(new ByteArrayInputStream(buf,keyword_len+1, data_len-keyword_len-1));
            			ir = new InputStreamReader(ii, "UTF-8");
            			br = new BufferedReader(ir);                       
            			read = null;
            			while((read = br.readLine()) != null) {
            				sb.append(read);
            				sb.append("\n");
            			}	
            			br.close();
            		} else {//Uncompressed text
            			sb.append(new String(buf, keyword_len + 1,data_len - keyword_len - 1, "UTF-8"));
            			sb.append("\n");
            		}
            		sb.append("**********************\n");
            		IOUtils.skipFully(is, 4);
            		break;
             	default:
            		buf = new byte[data_len + 4];
            		IOUtils.readFully(is, buf, 0, data_len + 4);
            		break;
             }
        }
        
        return sb.toString();
    }
  	
  	public static String readTextChunks(String fileName) throws IOException {
   		FileInputStream fi = new FileInputStream(fileName);
  		String text = readTextChunks(fi);
  		
  		fi.close();
  		
  		return text;  		
  	}
  	
  	public static List<Chunk> readChunks(InputStream is) throws IOException {  		
  		List<Chunk> list = new ArrayList<Chunk>();
 		 //Local variables for reading chunks
        int data_len = 0;
        int chunk_type = 0;
        byte[] buf = null;
     
        long signature = IOUtils.readLongMM(is);

        if (signature != SIGNATURE) {
       	 	throw new RuntimeException("Invalid PNG signature");
        }   

        /** Read header */
        /** We are expecting IHDR */
        if ((IOUtils.readIntMM(is) != 13)||(IOUtils.readIntMM(is) != ChunkType.IHDR.getValue())) {
            throw new RuntimeException("Invalid PNG header");
        }     
        
        buf = new byte[13];
        IOUtils.readFully(is, buf, 0, 13);
  
        list.add(new Chunk(ChunkType.IHDR, 13, buf, IOUtils.readUnsignedIntMM(is)));         
      
        while (true) {
        	data_len = IOUtils.readIntMM(is);
	       	chunk_type = IOUtils.readIntMM(is);
	   
	       	if (chunk_type == ChunkType.IEND.getValue()) {
	        	 list.add(new Chunk(ChunkType.IEND, data_len, new byte[0], IOUtils.readUnsignedIntMM(is)));
	       		 break;
	       	} 
       		ChunkType chunkType = ChunkType.fromInt(chunk_type);
       		buf = new byte[data_len];
       		IOUtils.readFully(is, buf, 0, data_len);
              
       		if (chunkType == ChunkType.UNKNOWN)
       			list.add(new UnknownChunk(data_len, chunk_type, buf, IOUtils.readUnsignedIntMM(is)));
       		else
       			list.add(new Chunk(chunkType, data_len, buf, IOUtils.readUnsignedIntMM(is)));
        }
        
        return list;
  	}
  	
	private static byte[] readICCProfile(byte[] buf) throws IOException {
		if(buf == null) throw new IllegalArgumentException("Input array is null");
		int profileName_len = 0;
		while(buf[profileName_len] != 0) profileName_len++;
		String profileName = new String(buf, 0, profileName_len, "UTF-8");
		
		InflaterInputStream ii = new InflaterInputStream(new ByteArrayInputStream(buf, profileName_len + 2, buf.length - profileName_len - 2));
		LOGGER.info("ICCProfile name: {}", profileName);
		 
		byte[] icc_profile = IOUtils.readFully(ii, 4096);
		LOGGER.info("ICCProfile length: {}", icc_profile.length);
	 		 
		return icc_profile;
 	}
  	
	public static Map<MetadataType, Metadata> readMetadata(InputStream is) throws IOException {
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		List<Chunk> chunks = readChunks(is);
		Iterator<Chunk> iter = chunks.iterator();
		TextualChunks textualChunk = null;
		while (iter.hasNext()) {
			Chunk chunk = iter.next();
			ChunkType type = chunk.getChunkType();
			long length = chunk.getLength();
			if(type == ChunkType.ICCP)
				metadataMap.put(MetadataType.ICC_PROFILE, new ICCProfile(readICCProfile(chunk.getData())));
			else if(type == ChunkType.TEXT || type == ChunkType.ITXT || type == ChunkType.ZTXT) {
				if(textualChunk == null)
					textualChunk = new TextualChunks();
				textualChunk.addChunk(chunk);			
			} else if(type == ChunkType.TIME) {
				metadataMap.put(MetadataType.PNG_TIME, new TIMEChunk(chunk));
			}
			
			LOGGER.info("{} ({}) | {} bytes | 0x{} (CRC)", type.getName(), type.getAttribute(), length, Long.toHexString(chunk.getCRC()));
		}
		
		if(textualChunk != null) {
			metadataMap.put(MetadataType.PNG_TEXTUAL, textualChunk);
			
			// We may find XMP data inside iTXT
			Map<String, String> keyValMap = textualChunk.getKeyValMap();
			
			for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
				if(entry.getKey().equals("XML:com.adobe.xmp"))
					metadataMap.put(MetadataType.XMP, new PngXMP(entry.getValue()));
			}
		}
			
		is.close();
		
		return metadataMap;
	}
  	
  	/**
  	 * @param is  InputStream of the image
  	 * @param chunkTypes a set of ChunkType with Attribute.ANCILLARY.
  	 * 		  Ancillary chunks with ChunkType in the set will be removed. 
  	 * @see  com.icafe4j.image.png.ChunkType
  	 * @throws Exception  Any exception related to the IO operations.
  	 */
  	public static void removeAncillaryChunks(InputStream is, Set<ChunkType> chunkTypes) throws IOException {
  		File dir = new File(".");

	    if(chunkTypes != null)	{	
   		    REMOVABLE = EnumSet.noneOf(ChunkType.class);
   		     
   		    for (ChunkType type : chunkTypes) {
   				if(type.getAttribute() == ChunkType.Attribute.ANCILLARY)
			    	  REMOVABLE.add(type);
			}   		     
 		 }
	      
		 String outFileName = "slim.png";
         removeChunks(is, dir, outFileName);
		 LOGGER.info(">>{}", outFileName);	
		 LOGGER.info("************************");
    }
  	
  	public static List<Chunk> removeAncillaryChunks(List<Chunk> chunks) throws Exception {
  		return removeChunks(chunks, REMOVABLE);
  	}
  	
    /**
     * Removes ancillary chunks either specified by "args" or predefined by REMOVABLE EnumSet.
     * 
     * @param fileOrDirectoryName file or directory name for the input PNG image(s).
     * @param chunkTypes a set of ChunkType with Attribute.ANCILLARY.
  	 * 		  Ancillary chunks with ChunkType in the set will be removed.
  	 * @throws IOException
   	 */  	
  	public static void removeAncillaryChunks(String fileOrDirectoryName, Set<ChunkType> chunkTypes) throws IOException {
  		File dir = new File(".");
  		File[] files = null;

  	    if(!StringUtils.isNullOrEmpty(fileOrDirectoryName)) {
  	    	files = new File[] {new File(fileOrDirectoryName)};
  	    
  	    	if(files[0].isDirectory()) {
			   dir = files[0];
			   files = null;
  	    	}
    	}
  	    
  	    if(files == null) {
  	    	files = dir.listFiles(new FileFilter() {
				  public boolean accept(File file) {
				     if(file.getName().toLowerCase().endsWith("png")) {
				    	 return true;
				     }
				     return false;
				  }
  	    	});
  	    }
	   
  	    if(chunkTypes != null) {	
 		    REMOVABLE = EnumSet.noneOf(ChunkType.class);
 		     
 		    for (ChunkType type : chunkTypes) {
 				if(type.getAttribute() == ChunkType.Attribute.ANCILLARY)
			    	  REMOVABLE.add(type);
			}   		     
  	    }
	      
	    FileInputStream fs = null;		
		  
	    for(int i = files.length - 1; i >= 0; i--) {
		 	String outFileName = files[i].getName();
		 	outFileName = outFileName.substring(0,outFileName.lastIndexOf('.'))
					+"_slim.png";
		 	LOGGER.info("<<{}", files[i].getName());
	 		fs = new FileInputStream(files[i]);
	 		removeChunks(fs, dir, outFileName);
 			LOGGER.info(">>{}", outFileName);	
 			LOGGER.info("************************");
 			fs.close();
	    }
    }
  	
   	private static void removeChunks(InputStream is, File outfileDir, String outfileName) throws IOException {
  		//Local variables for reading chunks
        int data_len = 0;
        int chunk_value = 0;
        byte[] buf = null;
      
        long signature = IOUtils.readLongMM(is);

        if (signature != SIGNATURE) {
            throw new RuntimeException("Invalid PNG signature");
        }   

        /** Read header */
        /** We are expecting IHDR */
        if ((IOUtils.readIntMM(is) != 13)||(IOUtils.readIntMM(is) != ChunkType.IHDR.getValue())) {
            throw new RuntimeException("Invalid PNG header");
        }
            
        FileOutputStream fs = new FileOutputStream(new File(outfileDir, outfileName)); 
         
        IOUtils.writeLongMM(fs, SIGNATURE);
        IOUtils.writeIntMM(fs, 13);//We expect length to be 13 bytes
        IOUtils.writeIntMM(fs, ChunkType.IHDR.getValue());

        buf = new byte[13 + 4];//13 plus 4 bytes CRC
        IOUtils.readFully(is, buf, 0, 17);
        IOUtils.write(fs, buf);

        while (true) {
           data_len = IOUtils.readIntMM(is);
           chunk_value = IOUtils.readIntMM(is);
 
           if (chunk_value == ChunkType.IEND.getValue()) {
              IOUtils.writeIntMM(fs, data_len);
              IOUtils.writeIntMM(fs, ChunkType.IEND.getValue());
              int crc = IOUtils.readIntMM(is);
              IOUtils.writeIntMM(fs, crc);
              break;
           }
           if(REMOVABLE.contains(ChunkType.fromInt(chunk_value))) {
              LOGGER.info("{} Chunk removed!", ChunkType.fromInt(chunk_value));
              IOUtils.skipFully(is, data_len + 4);
           } else {
              buf = new byte[data_len + 4];
              IOUtils.readFully(is, buf, 0, data_len + 4);
              IOUtils.writeIntMM(fs, data_len);
              IOUtils.writeIntMM(fs, chunk_value);
              IOUtils.write(fs, buf);
           }
        }
        
        fs.close();
    }
   	
   	public static List<Chunk> removeChunks(List<Chunk> chunks, ChunkType chunkType) {
  		
  		Iterator<Chunk> iter = chunks.listIterator();
   	
   		while(iter.hasNext()) {
   			
   			Chunk chunk = iter.next();
   		
   			if (chunk.getChunkType() == chunkType) {   				
   				iter.remove();
   			}   			
   		}
   		
   		return chunks;  		
  	}
   	
   	/**
   	 * Removes chunks which have the same ChunkType values from the chunkEnumSet.
   	 * 
   	 * @param chunks a list of chunks to be checked.
   	 * @param chunkTypeSet a set of ChunkType (better use a HashSet instead of EnumSet for performance).
   	 * @return a list of chunks with the specified chunks removed if any.
   	 */
   	
   	public static List<Chunk> removeChunks(List<Chunk> chunks, Set<ChunkType> chunkTypeSet) {
  		
  		Iterator<Chunk> iter = chunks.listIterator();
   	
   		while(iter.hasNext()) {
   			
   			Chunk chunk = iter.next();
   		
   			if (chunkTypeSet.contains(chunk.getChunkType())) {   				
   				iter.remove();
   			}   			
   		}
   		
   		return chunks;  		
  	}
  	
  	public static List<Chunk> removeTextChunks(List<Chunk> chunks) {
  		
  		Iterator<Chunk> iter = chunks.listIterator();
   	
   		while(iter.hasNext()) {
   			
   			Chunk chunk = iter.next();
   		
   			if ((chunk.getChunkType() == ChunkType.TEXT) || (chunk.getChunkType() == ChunkType.ZTXT) ||
   					(chunk.getChunkType() == ChunkType.ITXT)) {   				
   				iter.remove();
   			}   			
   		}
   		
   		return chunks;  		
  	}
  	
  	public static void serializeChunks(List<Chunk> chunks, OutputStream os) throws IOException {
  		
  		Collections.sort(chunks);
  	    
  		for(Chunk chunk : chunks) {
        	chunk.write(os);
        }
  	}
  	
  	public static void showICCProfile(InputStream is) throws IOException {
  		byte[] icc_profile = readICCProfileChunk(is);
  		if(icc_profile != null)
  			ICCProfile.showProfile(icc_profile);
  	}
  	
   	public static List<Chunk> splitIDATChunk(Chunk chunk, int size) {
   		
  		if (chunk.getChunkType() != ChunkType.IDAT)	{
   			throw new IllegalArgumentException("Not a valid IDAT chunk.");   				
   		} 
   		
  		if(size <= 0) throw new IllegalArgumentException("Invalid IDAT chunk size: " + size);
  		
  		List<Chunk> chunks = new ArrayList<Chunk>();
   		byte[] data = chunk.getData();
   		
   		int dataLen = data.length;
   		int mod = dataLen % size;
   		int nSplits = dataLen / size;
   		
   		byte[] buffer = new byte[size];
   		byte[] leftOver = new byte[mod];
   		
   		for(int i = 0; i < nSplits; i++) {
   			buffer = ArrayUtils.subArray(data, i*size, size);
   			chunks.add(new Chunk(ChunkType.IDAT, size, buffer, Chunk.calculateCRC(ChunkType.IDAT.getValue(), buffer)));
   		}
   		
   		if (mod != 0) {
   			leftOver = ArrayUtils.subArray(data, dataLen - mod, mod);
   			chunks.add(new Chunk(ChunkType.IDAT, mod, leftOver, Chunk.calculateCRC(ChunkType.IDAT.getValue(), leftOver)));
   		}
   
   		return chunks;
   	}
  	
  	public static List<Chunk> splitIDATChunks(List<Chunk> chunks, int size) {
   		
  		List<Chunk> listIDAT = new ArrayList<Chunk>();
  		ListIterator<Chunk> iter = chunks.listIterator();
  		
        while(iter.hasNext()) {
        	Chunk chunk = iter.next();
         	if(chunk.getChunkType() == ChunkType.IDAT) {
         		listIDAT.addAll(splitIDATChunk(chunk, size));
         		iter.remove();
         	}        	
        }
        
        chunks.addAll(listIDAT);
        
        return chunks;
   	}
  	
  	private PNGTweaker() {}
}