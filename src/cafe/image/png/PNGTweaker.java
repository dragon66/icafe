/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.png;

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
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Set;
import java.util.zip.InflaterInputStream;

import cafe.io.IOUtils;
import cafe.util.ArrayUtils;
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
    
   	public static void dump_text_chunks(Chunk[] chunks) throws IOException
   	{
   		for (Chunk chunk : chunks) {
   			if ((chunk.getChunkType() == ChunkType.TEXT) || (chunk.getChunkType() == ChunkType.ITXT) || 
   					(chunk.getChunkType() == ChunkType.ZTXT)) {
   				TextReader reader = new TextReader(chunk);
	   			System.out.println("Keyword: " + reader.getKeyword());
	   			System.out.println("Text: " + reader.getText());
   			}
   		}   	
   	}
	
	// Dump text chunks
   	public static void dump_text_chunks(InputStream is) throws IOException
    {
   		//Local variables for reading chunks
   		int data_len = 0;
   		int chunk_value = 0;
   		byte[] buf = null;

   		long signature = IOUtils.readLongMM(is);

   		if (signature != SIGNATURE)
   		{
   			System.out.println("--- NOT A PNG IMAGE ---");
   			return;
   		}

   		/** Read header */
   		/** We are expecting IHDR */
   		if ((IOUtils.readIntMM(is)!=13)||(IOUtils.readIntMM(is) != ChunkType.IHDR.getValue()))
   		{
   			System.out.println("--- NOT A PNG IMAGE ---");
   			return;
   		}

   		buf = new byte[13+4];//13 plus 4 bytes CRC
   		IOUtils.read(is, buf, 0, 17);

   		while (true)
   		{
   			data_len = IOUtils.readIntMM(is);
   			chunk_value = IOUtils.readIntMM(is);
   			//System.out.println("chunk type: 0x"+Integer.toHexString(chunk_type));

   			if (chunk_value == ChunkType.IEND.getValue())
   			{
   				System.out.println("End of Image");
   				IOUtils.readIntMM(is);//CRC
   				break;
   			}
 			  
   			ChunkType chunk = ChunkType.fromInt(chunk_value);

   			switch (chunk)
   			{
   				case ZTXT:
   				{  
   					System.out.println("zTXt chunk:");
   					buf = new byte[data_len];
   					IOUtils.read(is, buf);
   					int keyword_len = 0;
   					while(buf[keyword_len]!=0) keyword_len++;
   					System.out.print(new String(buf,0,keyword_len,"UTF-8")+": ");
   					InflaterInputStream ii = new InflaterInputStream(new ByteArrayInputStream(buf,keyword_len+2, data_len-keyword_len-2));
   					InputStreamReader ir = new InputStreamReader(ii,"UTF-8");
   					BufferedReader br = new BufferedReader(ir);                       
   					String read = null;
   					while((read=br.readLine()) != null) {
   						System.out.println(read);
   					}
   					br.close();
   					System.out.println("**********************");
   					IOUtils.skipFully(is, 4);
   					break;
   				}

   				case TEXT:
   				{
   					System.out.println("tEXt chunk:");
   					buf = new byte[data_len];
   					IOUtils.read(is, buf);
   					int keyword_len = 0;
   					while(buf[keyword_len]!=0) keyword_len++;
   					System.out.print(new String(buf,0,keyword_len,"UTF-8")+": ");
   					System.out.println(new String(buf,keyword_len+1,data_len-keyword_len-1,"UTF-8"));
   					System.out.println("**********************");
   					IOUtils.skipFully(is, 4);
   					break;
   				}

   				case ITXT:
   				{
   					// System.setOut(new PrintStream(new File("TextChunk.txt"),"UTF-8"));
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
   					System.out.println("iTXt chunk:");
   					buf = new byte[data_len];
   					IOUtils.read(is, buf);
   					int keyword_len = 0;
   					int trans_keyword_len = 0;
   					int lang_flg_len = 0;
   					boolean compr = false;
   					while(buf[keyword_len]!=0) keyword_len++;
   					System.out.print(new String(buf,0,keyword_len,"UTF-8"));
   					if(buf[++keyword_len]==1) compr = true;
   					keyword_len++;//Skip the compression method byte.
   					while(buf[++keyword_len]!=0) lang_flg_len++;
   					//////////////////////
   					System.out.print("(");
   					if(lang_flg_len>0)
   						System.out.print(new String(buf,keyword_len-lang_flg_len, lang_flg_len, "UTF-8"));
   					while(buf[++keyword_len]!=0) trans_keyword_len++;
   					if(trans_keyword_len>0)
   						System.out.print(" "+new String(buf,keyword_len-trans_keyword_len, trans_keyword_len, "UTF-8"));
   					System.out.print("): ");
   					/////////////////////// End of keyword. 					   
   					if(compr) {//Compressed text
   						InflaterInputStream ii = new InflaterInputStream(new ByteArrayInputStream(buf,keyword_len+1, data_len-keyword_len-1));
   						InputStreamReader ir = new InputStreamReader(ii,"UTF-8");
   						BufferedReader br = new BufferedReader(ir);                       
   						String read = null; 						   
   						while((read=br.readLine()) != null) {
   							System.out.println(read);
   						} 						   
   						br.close();
   					} else {//Uncompressed text
   						System.out.println(new String(buf,keyword_len+1,data_len-keyword_len-1,"UTF-8"));						   
   					}
   					// End of text.
   					System.out.println("**********************"); 					   
   					IOUtils.skipFully(is, 4); 					   
   					break;
   				}			   
 	
   				default:
   				{
   					buf = new byte[data_len+4];
   					IOUtils.read(is, buf,0, data_len+4);
   					break;
   				}
   			}
   		}
    }

  	public static void insertChunk(Chunk customChunk, InputStream is, OutputStream os) throws IOException
  	{
  		insertChunks(new Chunk[]{customChunk}, is, os);
  	}
  	
  	public static void insertChunks(Chunk[] chunks, InputStream is, OutputStream os) throws IOException
  	{
  		List<Chunk> list = readChunks(is);  		
        Collections.addAll(list, chunks);
    	
  		IOUtils.writeLongMM(os, SIGNATURE);
	
        serializeChunks(list, os);
  	}
  	
  	public static void insertChunks(List<Chunk> chunks, InputStream is, OutputStream os) throws IOException
  	{
  		List<Chunk> list = readChunks(is);  		
        list.addAll(chunks);
    	
  		IOUtils.writeLongMM(os, SIGNATURE);
	
        serializeChunks(list, os);
  	}
  	
  	public static List<Chunk> mergeIDATChunks(List<Chunk> chunks) {
   		
   		Iterator<Chunk> iter = chunks.listIterator();
   		byte[] data = new byte[0];
   		
   		while(iter.hasNext()) {
   			Chunk chunk = iter.next();
   		
   			if (chunk.getChunkType() == ChunkType.IDAT)
   			{
   				data = ArrayUtils.concat(data, chunk.getData());
   				iter.remove();
   			}   			
   		}
   		
   		chunks.add(new Chunk(ChunkType.IDAT, data.length, data, Chunk.calculateCRC(ChunkType.IDAT.getValue(), data)));

   		return chunks;
   	}
  	
  	public static String read_text_chunks(File file) throws IOException {
  		return read_text_chunks(new FileInputStream(file));
  	}
  	
  	// Read text chunks to a String
   	public static String read_text_chunks(InputStream is) throws IOException {
   		//Local variables for reading chunks
        int data_len = 0;
        int chunk_value = 0;
        byte[] buf = null;
        StringBuilder sb = new StringBuilder(1024);

        long signature = IOUtils.readLongMM(is);

        if (signature != SIGNATURE)
        {
        	return "--- NOT A PNG IMAGE ---";
        }

        /** Read header */
        /** We are expecting IHDR */
        if ((IOUtils.readIntMM(is)!=13)||(IOUtils.readIntMM(is) != ChunkType.IHDR.getValue()))
        {
        	return "--- NOT A PNG IMAGE ---";
        }

        buf = new byte[13+4];//13 plus 4 bytes CRC
        IOUtils.read(is, buf, 0, 17);

        while (true)
        {
            data_len = IOUtils.readIntMM(is);
            chunk_value = IOUtils.readIntMM(is);
            //System.out.println("chunk type: 0x"+Integer.toHexString(chunk_type));

            if (chunk_value == ChunkType.IEND.getValue())
            {
            	sb.append("End of Image\n");
                IOUtils.readIntMM(is);//CRC
                break;
            }
              
 			ChunkType chunk = ChunkType.fromInt(chunk_value);
 			   			 
            switch (chunk)
            {
            	case ZTXT:
            	{  
            		sb.append("zTXt chunk:\n");
            		buf = new byte[data_len];
            		IOUtils.read(is, buf);
            		int keyword_len = 0;
            		while(buf[keyword_len]!=0) keyword_len++;
            		sb.append(new String(buf,0,keyword_len,"UTF-8"));
            		sb.append(": ");
            		InflaterInputStream ii = new InflaterInputStream(new ByteArrayInputStream(buf,keyword_len+2, data_len-keyword_len-2));
            		InputStreamReader ir = new InputStreamReader(ii,"UTF-8");
            		BufferedReader br = new BufferedReader(ir);                       
            		String read = null;
            		while((read=br.readLine()) != null) {
            			sb.append(read);
            			sb.append("\n");
                    }                  
            		sb.append("**********************\n");
            		br.close();
            		IOUtils.skipFully(is, 4);
            		break;
            	}

            	case TEXT:
            	{
            		sb.append("tEXt chunk:\n");
            		buf = new byte[data_len];
            		IOUtils.read(is, buf);
            		int keyword_len = 0;
            		while(buf[keyword_len]!=0) keyword_len++;
            		sb.append(new String(buf,0,keyword_len,"UTF-8"));
            		sb.append(": ");
            		sb.append(new String(buf,keyword_len+1,data_len-keyword_len-1,"UTF-8"));
            		sb.append("\n**********************\n");
            		IOUtils.skipFully(is, 4);
            		break;
            	}

            	case ITXT:
            	{
            		// System.setOut(new PrintStream(new File("TextChunk.txt"),"UTF-8"));
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
            		buf = new byte[data_len];
            		IOUtils.read(is, buf);
            		int keyword_len = 0;
            		int trans_keyword_len = 0;
            		int lang_flg_len = 0;
            		boolean compr = false;
            		while(buf[keyword_len]!=0) keyword_len++;
            		sb.append(new String(buf,0,keyword_len,"UTF-8"));
            		if(buf[++keyword_len]==1) compr = true;
            		keyword_len++;//Skip the compression method byte.
            		while(buf[++keyword_len]!=0) lang_flg_len++;
            		//////////////////////
            		sb.append("(");
            		if(lang_flg_len>0)
            			sb.append(new String(buf,keyword_len-lang_flg_len, lang_flg_len, "UTF-8"));
            		while(buf[++keyword_len]!=0) trans_keyword_len++;
            		if(trans_keyword_len>0) {
            			sb.append(" ");
            			sb.append(new String(buf,keyword_len-trans_keyword_len, trans_keyword_len, "UTF-8"));
            		}
            		sb.append("): ");
            		/////////////////////// End of key.
            		if(compr) //Compressed text
            		{
            			InflaterInputStream ii = new InflaterInputStream(new ByteArrayInputStream(buf,keyword_len+1, data_len-keyword_len-1));
            			InputStreamReader ir = new InputStreamReader(ii,"UTF-8");
            			BufferedReader br = new BufferedReader(ir);                       
            			String read = null;
            			while((read=br.readLine()) != null) {
            				sb.append(read);
            				sb.append("\n");
            			}	
            			br.close();
            		}
            		else //Uncompressed text
            		{
            			sb.append(new String(buf,keyword_len+1,data_len-keyword_len-1,"UTF-8"));
            			sb.append("\n");
            		}
            		sb.append("**********************\n");
            		IOUtils.skipFully(is, 4);
            		break;
            	}			   
 	
            	default:
            	{
            		buf = new byte[data_len+4];
            		IOUtils.read(is, buf,0, data_len+4);
            		break;
            	}
            }
        }
        
        return sb.toString();
    }
  	
  	public static String read_text_chunks(String fileName) throws IOException {
   		FileInputStream fi = new FileInputStream(fileName);
  		String text = read_text_chunks(fi);
  		
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

        if (signature != SIGNATURE)
        {
       	 	throw new RuntimeException("--- NOT A PNG IMAGE ---");
        }   

        /** Read header */
        /** We are expecting IHDR */
        if ((IOUtils.readIntMM(is)!=13)||(IOUtils.readIntMM(is) != ChunkType.IHDR.getValue()))
        {
            throw new RuntimeException("Not a valid IHDR chunk.");
        }     
        
        buf = new byte[13];
        IOUtils.read(is, buf, 0, 13);
  
        list.add(new Chunk(ChunkType.IHDR, 13, buf, IOUtils.readUnsignedIntMM(is)));         
      
        while (true)
        {
        	data_len = IOUtils.readIntMM(is);
	       	chunk_type = IOUtils.readIntMM(is);
	   
	       	if (chunk_type == ChunkType.IEND.getValue()) {
	        	 list.add(new Chunk(ChunkType.IEND, data_len, new byte[0], IOUtils.readUnsignedIntMM(is)));
	       		 break;
	       	} 
       		ChunkType chunkType = ChunkType.fromInt(chunk_type);
       		buf = new byte[data_len];
       		IOUtils.read(is, buf,0, data_len);
              
       		if (chunkType == ChunkType.UNKNOWN)
       			list.add(new UnknownChunk(data_len, chunk_type, buf, IOUtils.readUnsignedIntMM(is)));
       		else
       			list.add(new Chunk(chunkType, data_len, buf, IOUtils.readUnsignedIntMM(is)));
        }
        
        return list;
  	}
  	
  	/**
  	 * @param is  InputStream of the image
  	 * @param chunkNames  Names of the chunks to be removed. Effective names are those defined by Attribute 
  	 *                    with Attribute.ANCILLARY. Names are not case-sensitive.  
  	 * @see  cafe.image.png.ChunkType
  	 * @throws Exception  Any exception related to the IO operations.
  	 */
  	public static void remove_ancillary_chunks(InputStream is, String...chunkNames) throws IOException
    {
  		File dir = new File(".");

	    if(chunkNames.length>0)
		{	
   		    REMOVABLE = EnumSet.noneOf(ChunkType.class);
   		     
   		    String key = "";
			
   		    for (int i=0;i<chunkNames.length;i++)
   		    {
   		    	key = chunkNames[i];   		    	
				if(ChunkType.containsIgnoreCase(key) && ChunkType.fromString(key).getAttribute() == ChunkType.Attribute.ANCILLARY)
			    	  REMOVABLE.add(ChunkType.fromString(key));
			}   		     
 		 }
	      
		 String outFileName = "slim.png";
         remove_chunks(is, dir, outFileName);
		 System.out.println(">>"+outFileName);	
		 System.out.println("************************");
    }
  	
  	public static List<Chunk> remove_ancillary_chunks(List<Chunk> chunks) throws Exception {
  		return removeChunks(chunks, REMOVABLE);
  	}
  	
    /**
   	 * @param args  An array of String passed to the method. The first element is either a file name or
   	 *              a directory. The other elements are the names of the chunks to be removed.
   	 * @throws Exception  Any exception related to the IO operations.
   	 */  	
  	public static void remove_ancillary_chunks(String args[]) throws IOException
    {
  		if(args == null) throw new IllegalArgumentException("Input args is null");
       
  	    if(args.length>0)
	    {
  	    	File[] files = {new File(args[0])};
  	    	File dir = new File(".");

	        if(files[0].isDirectory())
		    {
			   dir = files[0];

		       files = files[0].listFiles(new FileFilter(){
				  public boolean accept(File file)
				  {
				     if(file.getName().toLowerCase().endsWith("png")){
                       return true;
				     }
				     
                     return false;
				  }
			   });
		    }			
		  
	        if(args.length>1)
		    {	
   		       REMOVABLE = EnumSet.noneOf(ChunkType.class);
   		     
	    	   String key = "";
			
   		       for (int i=1;i<args.length;i++)
   		       {
				  key = args[i];				  
				  if(ChunkType.containsIgnoreCase(key) && ChunkType.fromString(key).getAttribute() == ChunkType.Attribute.ANCILLARY)
			    	  REMOVABLE.add(ChunkType.fromString(key));
			   }
		    }
	      
		    FileInputStream fs = null;		
		  
		    for(int i= files.length-1;i>=0;i--)
		    {
			 	String outFileName = files[i].getName();
				outFileName = outFileName.substring(0,outFileName.lastIndexOf('.'))
					+"_slim.png";
				System.out.println("<<"+files[i].getName());
		  	    fs = new FileInputStream(files[i]);
                remove_chunks(fs, dir, outFileName);
				System.out.println(">>"+outFileName);	
				System.out.println("************************");
				fs.close();
		    }
		}
	}
   	
   	private static void remove_chunks(InputStream is, File outfileDir, String outfileName) throws IOException
    {
  		//Local variables for reading chunks
        int data_len = 0;
        int chunk_value = 0;
        byte[] buf = null;
      
        long signature = IOUtils.readLongMM(is);

        if (signature != SIGNATURE)
        {
            System.out.println("--- NOT A PNG IMAGE ---");
            return;
        }   

        /** Read header */
        /** We are expecting IHDR */
        if ((IOUtils.readIntMM(is)!=13)||(IOUtils.readIntMM(is) != ChunkType.IHDR.getValue()))
        {
            System.out.println("--- NOT A PNG IMAGE ---");
            return;
        }
            
        FileOutputStream fs = new FileOutputStream(new File(outfileDir,outfileName)); 
         
        IOUtils.writeLongMM(fs, SIGNATURE);
        IOUtils.writeIntMM(fs, 13);//We expect length to be 13 bytes
        IOUtils.writeIntMM(fs, ChunkType.IHDR.getValue());

        buf = new byte[13+4];//13 plus 4 bytes CRC
        IOUtils.read(is, buf, 0, 17);
        IOUtils.write(fs, buf);

        while (true)
        {
           data_len = IOUtils.readIntMM(is);
           chunk_value = IOUtils.readIntMM(is);
           //System.out.println("chunk type: 0x"+Integer.toHexString(chunk_type));

           if (chunk_value == ChunkType.IEND.getValue())
           {
              System.out.println("End of Image");
              IOUtils.writeIntMM(fs, data_len);
              IOUtils.writeIntMM(fs, ChunkType.IEND.getValue());
              int crc = IOUtils.readIntMM(is);
              IOUtils.writeIntMM(fs, crc);
              break;
           }
           if(REMOVABLE.contains(ChunkType.fromInt(chunk_value)))
           {
              System.out.println(ChunkType.fromInt(chunk_value)+" Chunk removed!");
              IOUtils.skipFully(is, data_len+4);
           }
           else
           {
              buf = new byte[data_len+4];
              IOUtils.read(is, buf,0, data_len+4);
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
   		
   			if (chunk.getChunkType() == chunkType)
   			{   				
   				iter.remove();
   			}   			
   		}
   		
   		return chunks;  		
  	}
   	
   	/**
   	 * Removes chunks which have the same ChunkType values from the chunkEnumSet.
   	 * 
   	 * @param chunks a list of chunks to be checked.
   	 * @param chunkEnumSet a set of ChunkType (better use a HashSet instead of EnumSet for performance).
   	 * @return a list of chunks with the specified chunks removed if any.
   	 */
   	
   	public static List<Chunk> removeChunks(List<Chunk> chunks, Set<ChunkType> chunkEnumSet) {
  		
  		Iterator<Chunk> iter = chunks.listIterator();
   	
   		while(iter.hasNext()) {
   			
   			Chunk chunk = iter.next();
   		
   			if (chunkEnumSet.contains(chunk.getChunkType()))
   			{   				
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
   					(chunk.getChunkType() == ChunkType.ITXT))
   			{   				
   				iter.remove();
   			}   			
   		}
   		
   		return chunks;  		
  	}
  	
  	public static void serializeChunks(List<Chunk> chunks, OutputStream os) throws IOException {
  		
  		Collections.sort(chunks);
  	    
  		for(Chunk chunk : chunks)
        {
        	chunk.write(os);
        }
  	}
  	
  	public static void snoop(InputStream is) throws IOException {
		
		List<Chunk> chunks = PNGTweaker.readChunks(is);
		Iterator<Chunk> iter = chunks.iterator();
		
		while (iter.hasNext()) {
			Chunk chunk = iter.next();
			System.out.print(chunk.getChunkType().getName() + " (" + chunk.getChunkType().getAttribute() + ")");
			System.out.print(" | " + chunk.getLength() + " bytes");
			System.out.println(" | " + "0x" + Long.toHexString(chunk.getCRC()) + " (CRC)");
		}
		
		is.close();
	}
  	
  	public static List<Chunk> splitIDATChunk(Chunk chunk, int size) {
   		
  		if (chunk.getChunkType() != ChunkType.IDAT)	{
   			throw new IllegalArgumentException("Not a valid IDAT chunk.");   				
   		} 
   		
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
