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
 * MemoryCacheRandomAccessOutputStream.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    07Apr2015  Removed flush() along with super flush()
 * WY    06Apr2015  Added empty flush() to control flush timing
 */

package com.icafe4j.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MemoryCacheRandomAccessOutputStream extends RandomAccessOutputStream {
	private static final int BUFFER_SHIFT = 12;
	private static final int BUFFER_SIZE = 1 << BUFFER_SHIFT;
	private static final int BUFFER_MASK = BUFFER_SIZE - 1;
	
	private long pointer = 0L;
	// The largest position ever written to the cache.
	private long length = 0L;
	private List<byte[]> cache;
	private long cacheStart = 0L;
	private long flushPos = 0L;
	
	public MemoryCacheRandomAccessOutputStream(OutputStream dist) {
		super(dist);
		cache = new ArrayList<byte[]>(10);
	}
	
	public void close() throws IOException {
		if(closed) return;
		super.close();
 		cache.clear();
 		dist.close();
 		dist = null;
 		closed = true;
    }
	
	public void shallowClose() throws IOException {
		if(closed) return;
		super.close();
 		cache.clear();
 		dist = null;
 		closed = true;
	}

	public void disposeBefore(long pos) throws IOException {
		ensureOpen();
	    long index = pos >> BUFFER_SHIFT;
	    
	    if (index < cacheStart) {
	         throw new IndexOutOfBoundsException("pos already disposed");
	    }
	    
	    long numBlocks = Math.min(index - cacheStart, cache.size());
	    
	    for (long i = 0; i < numBlocks; i++) {
	         cache.remove(0);
	    }
	    
	    this.cacheStart = index;
    }
	
	private void expandCache(long pos) throws IOException {
        long currIndex = cacheStart + cache.size() - 1;
        long toIndex = pos >> BUFFER_SHIFT;
        long numNewBuffers = toIndex - currIndex;
        // Fill the cache with blocks to the position required for writing.
        for (long i = 0; i < numNewBuffers; i++) {
            try {
                cache.add(new byte[BUFFER_SIZE]);
            } catch (OutOfMemoryError e) {
                throw new IOException("No memory left for cache!");
            }
        }
    }
	
	private byte[] getCacheBlock(long blockNum) throws IOException {
        long blockOffset = blockNum - cacheStart;
        if (blockOffset > Integer.MAX_VALUE) {
            throw new IOException("Cache addressing limit exceeded!");
        }
        return cache.get((int)blockOffset);
    }
	
	public long getFlushPos() {
		return flushPos;
	}
	
	/**
	 * Returns the total length of data that has been cached,
	 * regardless of whether any early blocks have been disposed.
	 * This value will only ever increase. 
	 */
	public long getLength() {
	    return length;
	}	
	
	public long getStreamPointer() {
	  	return pointer;
	}
	
	@Override
	public void reset() {
		throw new UnsupportedOperationException("This method is not implemented");
	}
	
	public void seek(long pos) throws IOException {
		ensureOpen();
        if (pos < 0L)
        	throw new IOException("Negative seek position.");
		
        pointer = pos;
    }
	
	public void write(byte[] b, int off, int len) throws IOException {
		ensureOpen();
        if (b == null) {
            throw new NullPointerException("b == null!");
        }
       
        if ((off < 0) || (len < 0) || (pointer < 0) ||
            (off + len > b.length) || (off + len < 0)) {
            throw new IndexOutOfBoundsException();
        }
        // Ensure there is space for the incoming data
        long lastPos = pointer + len - 1;
        if (lastPos >= length) {
            expandCache(lastPos);
            length = lastPos + 1;
        }
        // Copy the data into the cache, block by block
        int offset = (int)(pointer & BUFFER_MASK);
        while (len > 0) {
            byte[] buf = getCacheBlock(pointer >> BUFFER_SHIFT);
            int nbytes = Math.min(len, BUFFER_SIZE - offset);
            System.arraycopy(b, off, buf, offset, nbytes);

            pointer += nbytes;
            off += nbytes;
            len -= nbytes;
            offset = 0; // Always after the first time
        }
    }
	
	@Override
	public void write(int value) throws IOException {
		ensureOpen();
	    if (pointer < 0)
	    	throw new ArrayIndexOutOfBoundsException("pointer < 0");
		// Ensure there is space for the incoming data
        if (pointer >= length) {
            expandCache(pointer);
            length = pointer + 1;
        }
        // Insert the data.
        byte[] buf = getCacheBlock(pointer >> BUFFER_SHIFT);
        int offset = (int)(pointer++ & BUFFER_MASK);
        buf[offset] = (byte)value;
	}

	public void writeToStream(long len) throws IOException {
		ensureOpen();
		if (len == 0) {
            return;
        }
		
		if (pointer + len > length) {
            throw new IndexOutOfBoundsException("Argument out of cache");
        }
        
        if ((pointer < 0) || (len < 0)) {
            throw new IndexOutOfBoundsException("Negative pointer or len");
        }      

        long bufIndex = pointer >> BUFFER_SHIFT;

        if (bufIndex < cacheStart) {
            throw new IndexOutOfBoundsException("pointer already disposed");
        }
        
        int offset = (int)(pointer & BUFFER_MASK);
        byte[] buf = getCacheBlock(bufIndex++);
        	
        while (len > 0) {
            if (buf == null) {
                buf = getCacheBlock(bufIndex++);
                offset = 0;
            }
            int nbytes = (int)Math.min(len, (BUFFER_SIZE - offset));
            dist.write(buf, offset, nbytes);
            buf = null;
            len -= nbytes;
            flushPos += nbytes;
        }
    }
}