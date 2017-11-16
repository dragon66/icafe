/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image.png;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.zip.DeflaterOutputStream;
import java.io.ByteArrayOutputStream;

import com.icafe4j.util.Builder;

/**
 * Builder for PNG textual chunks: iTXT, zTXT, and tEXT.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/31/2012
 */
public class TextBuilder extends ChunkBuilder implements Builder<Chunk> {
	// Whether or not iTXT should be compressed. iTXT is uncompressed by default
	private boolean compressed;
	private String keyword;
	private String text;
	
	public TextBuilder(ChunkType chunkType) {
		super(chunkType);
		
		if((chunkType != ChunkType.TEXT) && (chunkType != ChunkType.ITXT) 
				&& (chunkType != ChunkType.ZTXT))
			throw new IllegalArgumentException("Expect Textual chunk!");
	}
	
	protected byte[] buildData() {	
		byte[] data = null;
		ChunkType chunkType = getChunkType();
		
		StringBuilder sb = new StringBuilder(this.keyword);
		sb.append('\0');
		
		switch (chunkType) {
			case TEXT:
				sb.append(this.text);
				try {
					data = sb.toString().getBytes("iso-8859-1");
				} catch (Exception ex) {
					ex.printStackTrace();
				}					
				break;
			case ZTXT:
				try {
					ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);
					sb.append('\0');
					bo.write(sb.toString().getBytes("iso-8859-1"));
					DeflaterOutputStream ds = new DeflaterOutputStream(bo);
					OutputStreamWriter or = new OutputStreamWriter(ds, "iso-8859-1");
	                BufferedWriter br = new BufferedWriter(or);                       
					br.write(this.text);
					br.flush();
					br.close();
					data = bo.toByteArray();					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
	            break;
			case ITXT:
				try {
					ByteArrayOutputStream bo = new ByteArrayOutputStream(1024);
					bo.write(sb.toString().getBytes("iso-8859-1"));
					OutputStreamWriter or = null;
					if(compressed) {
						bo.write(new byte[]{1, 0, 0, 0});
						or = new OutputStreamWriter(new DeflaterOutputStream(bo), "UTF-8");
					} else {
						bo.write(new byte[]{0, 0, 0, 0});
						or = new OutputStreamWriter(bo, "UTF-8");
					}
					BufferedWriter br = new BufferedWriter(or);
					br.write(this.text);
					br.flush();
					br.close();
					data = bo.toByteArray();					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				break;
			default: // It will never come this far!				
		}
		
	    return data;
	}
	
	/**
	 * The keyword must be at least one character and less than 80 characters long.
	 * <p>
	 * Keywords are always interpreted according to the ISO/IEC 8859-1 (Latin-1) 
	 * character set [ISO/IEC-8859-1]. 
	 * <p>
	 * They must contain only printable Latin-1 characters and spaces; that is, 
	 * only character codes 32-126 and 161-255 decimal are allowed. 
	 * <p>
	 * To reduce the chances for human misreading of a keyword, leading and 
	 * trailing spaces are forbidden, as are consecutive spaces.
	 * <p>
	 * Note also that the non-breaking space (code 160) is not permitted in keywords,
	 * since it is visually indistinguishable from an ordinary space.
	 */
	public TextBuilder keyword(String keyword) {
		this.keyword = keyword.trim().replaceAll("\\s+", " ");
		return this;
	}
	
	public void setCompressed(boolean compressed) {
		this.compressed = compressed;
	}

	/**
	 * The tExt chunk is interpreted according to the ISO/IEC 8859-1 (Latin-1) character 
	 * set [ISO/IEC-8859-1].
	 * 
	 * The text string can contain any Latin-1 character. Newlines in the text string 
	 * should be represented by a single line feed character (decimal 10); use of other
	 * control characters in the text is discouraged.
	 * <p>
	 * The zTXt chunk contains textual data, just as tEXt does; however, zTXt takes 
	 * advantage of compression. The zTXt and tEXt chunks are semantically equivalent,
	 * but zTXt is recommended for storing large blocks of text.
     * A zTXt chunk contains:
     *   Keyword:            1-79 bytes (character string)
     *   Null separator:     1 byte
     *   Compression method: 1 byte
     *   Compressed text:    n bytes
	 * <p>
	 * iTXt International textual data
     * This chunk is semantically equivalent to the tEXt and zTXt chunks, but the textual
     * data is in the UTF-8 encoding of the Unicode character set instead of Latin-1. 
     * This chunk contains:
     *    Keyword:             1-79 bytes (character string)
     *    Null separator:      1 byte
     *    Compression flag:    1 byte
     *    Compression method:  1 byte
     *    Language tag:        0 or more bytes (character string)
     *    Null separator:      1 byte
     *    Translated keyword:  0 or more bytes
     *    Null separator:      1 byte
     *    Text:                0 or more bytes
	 */
	public TextBuilder text(String text) {
		this.text = text;
		return this;
	}
}