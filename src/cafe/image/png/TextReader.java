/**
 * Copyright (c) 2014-2015 by Wen Yu.
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.InflaterInputStream;

import cafe.util.Reader;

/**
 * Reader for PNG textual chunks: iTXT, zTXT, and tEXT.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/01/2013
 */
public class TextReader implements Reader {

	private String keyword;
	private String text;
	private Chunk chunk;
		
	public TextReader(Chunk chunk) throws IOException {
		this.chunk = chunk;
		read();
	}
	
	public String getKeyword() {
		return this.keyword;
	}
	
	public String getText() {
		return text;
	}
	
	// Read text chunks to a String
   	public void read() throws IOException
    {       
   		StringBuilder sb = new StringBuilder(1024);
   		byte[] data = chunk.getData();
   		
        switch (chunk.getChunkType())
        {
		   case ZTXT:
		   {   					  
			   int keyword_len = 0;
			   while(data[keyword_len]!=0) keyword_len++;
			   this.keyword = new String(data,0,keyword_len,"UTF-8");
			
			   InflaterInputStream ii = new InflaterInputStream(new ByteArrayInputStream(data,keyword_len+2, data.length-keyword_len-2));
			   InputStreamReader ir = new InputStreamReader(ii,"UTF-8");
               BufferedReader br = new BufferedReader(ir);                       
			   String read = null;
               while((read=br.readLine()) != null) {
                  sb.append(read);
                  sb.append("\n");
               }                  
			   br.close();
	
               break;
           }

		   case TEXT:
		   {
			   int keyword_len = 0;			   
			   while(data[keyword_len]!=0) keyword_len++;
			   this.keyword = new String(data,0,keyword_len,"UTF-8");
			   sb.append(new String(data,keyword_len+1,data.length-keyword_len-1,"UTF-8"));
			
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
			   int keyword_len = 0;
			   int trans_keyword_len = 0;
			   int lang_flg_len = 0;
			   boolean compr = false;
			   while(data[keyword_len]!=0) keyword_len++;
			   sb.append(new String(data,0,keyword_len,"UTF-8"));
			   if(data[++keyword_len]==1) compr = true;
			   keyword_len++;//Skip the compression method byte.
               while(data[++keyword_len]!=0) lang_flg_len++;
			   //////////////////////
			   sb.append("(");
			   if(lang_flg_len>0)
				   sb.append(new String(data,keyword_len-lang_flg_len, lang_flg_len, "UTF-8"));
			   while(data[++keyword_len]!=0) trans_keyword_len++;
               if(trans_keyword_len>0) {
				   sb.append(" ");
            	   sb.append(new String(data,keyword_len-trans_keyword_len, trans_keyword_len, "UTF-8"));
               }
			   sb.append(")");
			   
			   this.keyword = sb.toString().replaceFirst("\\(\\)", "");
			   
			   sb.setLength(0); // Reset StringBuilder
			   /////////////////////// End of key.
			   if(compr) //Compressed text
			   {
				   InflaterInputStream ii = new InflaterInputStream(new ByteArrayInputStream(data,keyword_len+1, data.length-keyword_len-1));
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
				   sb.append(new String(data,keyword_len+1,data.length-keyword_len-1,"UTF-8"));
				   sb.append("\n");
			   }
			   
			   sb.deleteCharAt(sb.length() - 1);
			   
			   break;
		   }			   

           default:
           {
               throw new IllegalArgumentException("Not a valid textual chunk.");
           }           
        }
        this.text = sb.toString();
     }
}
