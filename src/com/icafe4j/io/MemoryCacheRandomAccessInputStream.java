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

package com.icafe4j.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a memory cached random access input stream to ease the 
 * decoding of some types of images such as TIFF which may need random
 * access to the underlying stream. 
 * <p>
 * Based on com.sun.media.jai.codec.MemoryCacheSeekableStream.
 * <p>
 * This implementation has a major drawback: It has no knowledge 
 * of the length of the stream, it is supposed to move forward
 * even though it is possible to put the pointer at anywhere
 * before the end of the stream. 
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 02/09/2014 
 */ 
public class MemoryCacheRandomAccessInputStream extends RandomAccessInputStream {
	//
	private static final int BUFFER_SHIFT = 12;
    private static final int BUFFER_SIZE = 1 << BUFFER_SHIFT;
    private static final int BUFFER_MASK = BUFFER_SIZE - 1;

    private long pointer;
    private List<byte[]> cache;
    private int length;
    private boolean foundEOS;
	    
	public MemoryCacheRandomAccessInputStream(InputStream src) {
		super(src);
		pointer = 0L;
		cache = new ArrayList<byte[]>(10);
		length = 0;
		foundEOS = false;
	}
		
	public void close() throws IOException {
		if(closed) return;
		cache.clear();
		cache = null;
		src.close();
		src = null;
		closed = true;
	}
	
	public void shallowClose() {
		if(closed) return;
		cache.clear();
		cache = null;
		src = null;
		closed = true;
	}
		
	public long getStreamPointer() {
		return pointer;
	}
		
	public int read() throws IOException {
		ensureOpen();
		long l = pointer + 1L;
		long pos = readUntil(l);
		if(pos >= l) {
			byte[] buf = cache.get((int)(pointer>>BUFFER_SHIFT));
			return buf[(int)(pointer++ & BUFFER_MASK)] & 0xff;
		}
	        
		return -1;
	}

	public int read(byte[] bytes, int off, int len) throws IOException {
		ensureOpen();
		if(bytes == null)
			throw new NullPointerException();
		if(off<0 || len<0 || off+len>bytes.length)
			throw new IndexOutOfBoundsException();
		if(len == 0)
			return 0;
		long l = readUntil(pointer+len);
		if (l <= pointer)
			return -1;
	        
		byte[] buf = cache.get((int)(pointer >> BUFFER_SHIFT));
		int k = Math.min(len, BUFFER_SIZE - (int)(pointer & BUFFER_MASK));
		System.arraycopy(buf, (int)(pointer & BUFFER_MASK), bytes, off, k);
	        
		pointer += k;
	        
		return k;
	}

	private long readUntil(long pos) throws IOException {		
		if(pos < length)
			return pos;
		if(foundEOS)
			return length;
		int slot = (int)(pos >> BUFFER_SHIFT);
		int startSlot = length >> BUFFER_SHIFT;
	        
		for(int k = startSlot; k <= slot; k++) 
		{
			byte[] buf = new byte[BUFFER_SIZE];
			cache.add(buf);
			int len = BUFFER_SIZE;
			int off = 0;
	            
			while(len > 0) {
				int nbytes = src.read(buf, off, len);
				if(nbytes == -1) {
					foundEOS = true;
					return length;
				}
				off += nbytes;
				len -= nbytes;
				length += nbytes;
			}
		}
		return length;
	}

	public void seek(long loc) throws IOException {
		ensureOpen();
		if (loc<0L)
			throw new IOException("Negative seek position.");
			
		pointer = loc;
	}
}