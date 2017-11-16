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
 * FileCacheRandomAccessOutputStream.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    07Apr2015  Removed flush() along with super flush()
 * WY    06Apr2015  Added empty flush() to control flush timing
 */
 
package com.icafe4j.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class FileCacheRandomAccessOutputStream extends RandomAccessOutputStream {

	/** The cache File. */
    private File cacheFile;

    /** The cache as a RandomAcessFile. */
    private RandomAccessFile cache;
    
    /** The length of the read buffer. */
    private int bufLen = 4096;

    /** Number of bytes in the cache. */
    private long length = 0L;

    /** Next byte to be read. */
    private long pointer = 0L;
    
    private long flushPos = 0L;
    
    public FileCacheRandomAccessOutputStream(OutputStream dist) throws IOException {
    	super(dist);
        this.cacheFile = File.createTempFile("cafe-FCRAOS-", ".tmp");
        cacheFile.deleteOnExit();
        this.cache = new RandomAccessFile(cacheFile, "rw");
    }
    
    public FileCacheRandomAccessOutputStream(OutputStream dist, int bufLen) throws IOException {
    	super(dist);
    	this.bufLen = bufLen;
        this.cacheFile = File.createTempFile("cafe-FCRAOS-", ".tmp");
        cacheFile.deleteOnExit();
        this.cache = new RandomAccessFile(cacheFile, "rw");
    }
    
    /**
     * Closes this stream and releases any system resources
     * associated with the stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
    	if(closed) return;
        super.close();
        cache.close();
        cacheFile.delete();
        dist.close();
        dist = null;
        closed = true;
    }
    
    public void shallowClose() throws IOException {
    	if(closed) return;
        super.close();
        cache.close();
        cacheFile.delete();
        dist = null;
        closed = true;
    }
    
	@Override
	public void disposeBefore(long pos) { 
		throw new UnsupportedOperationException("This method is not implemented");
	}
	
	@Override
	public long getFlushPos() {
		return flushPos;
	}

	@Override
	public long getLength() {
		return length;
	}

	@Override
	public long getStreamPointer() {
		return pointer;
	}
	
	@Override
	public void reset() { }

	@Override
	 public void seek(long pos) throws IOException {
		ensureOpen();
        if (pos < 0) {
        	throw new IOException("Negtive seek position.");
        }
        pointer = pos;
    }

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ensureOpen();
		if (b == null) {
			throw new NullPointerException("b == null!");
		}
	       
		if ((off < 0) || (len < 0) || (pointer < 0) ||
				(off + len > b.length) || (off + len < 0)) {
			throw new IndexOutOfBoundsException();
		}
		
		long lastPos = pointer + len - 1;
		
		if (lastPos >= length) {
			length = lastPos + 1;
		}
		
		cache.seek(pointer);
		cache.write(b, off, len);
		pointer += len;
	}
	
	@Override
	public void write(int value) throws IOException {
		ensureOpen();
		if (pointer < 0)
	    	throw new IndexOutOfBoundsException("pointer < 0");
		if (pointer >= length) {
           length = pointer + 1;
        }
		cache.seek(pointer);
        cache.write(value);
    	pointer++;
    }

	@Override
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
        
        cache.seek(pointer);

        while (len > 0) {
    	   byte[] buf = new byte[bufLen];
           int nbytes = cache.read(buf);
           dist.write(buf, 0, nbytes);
           len -= nbytes;
           flushPos += nbytes;
        }
    }	
}