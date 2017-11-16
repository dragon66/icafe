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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Implements a file cached random access input stream to ease the 
 * decoding of some types of images such as TIFF which may need random
 * access to the underlying stream. 
 * <p>
 * Based on com.sun.media.jai.codec.FileCacheSeekableStream.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 02/09/2014 
 */ 
public class FileCacheRandomAccessInputStream extends RandomAccessInputStream {

	/** The cache File. */
    private File cacheFile;

    /** The cache as a RandomAcessFile. */
    private RandomAccessFile cache;

    /** The length of the read buffer. */
    private int bufLen;

    /** The read buffer. */
    private byte[] buf;

    /** Number of bytes in the cache. */
    private long length = 0;

    /** Next byte to be read. */
    private long pointer = 0;

    /** True if we've encountered the end of the source stream. */
    private boolean foundEOF = false;

    /**
     * Constructs a <code>MemoryCacheRandomAccessInputStream</code>
     * that takes its source data from a regular <code>InputStream</code>.
     * Seeking backwards is supported by means of an file cache.
     *
     * <p> An <code>IOException</code> will be thrown if the
     * attempt to create the cache file fails for any reason.
     */
    public FileCacheRandomAccessInputStream(InputStream stream) throws IOException {
       this(stream, 4096); // 4k default buffer length
    }
    
    public FileCacheRandomAccessInputStream(InputStream src, int bufLen) throws IOException {
    	super(src);
        this.bufLen = bufLen;
        buf = new byte[bufLen];
    	this.cacheFile = File.createTempFile("cafe-FCRAIS-", ".tmp");
        cacheFile.deleteOnExit();
        this.cache = new RandomAccessFile(cacheFile, "rw");
    }

    /**
     * Ensures that at least <code>pos</code> bytes are cached,
     * or the end of the source is reached.  The return value
     * is equal to the smaller of <code>pos</code> and the
     * length of the source file.
     */
    private long readUntil(long pos) throws IOException {
        // We've already got enough data cached
        if (pos < length) {
            return pos;
        }
        // pos >= length but length isn't getting any bigger, so return it
        if (foundEOF) {
            return length;
        }

        long len = pos - length;
        cache.seek(length);
        while (len > 0) {
            // Copy a buffer's worth of data from the source to the cache
            // bufLen will always fit into an int so this is safe
            int nbytes = src.read(buf, 0, (int)Math.min(len, bufLen));
            if (nbytes == -1) {
                foundEOF = true;
                return length;
            }

            cache.setLength(cache.length() + nbytes);
            cache.write(buf, 0, nbytes);
            len -= nbytes;
            length += nbytes;
        }

        return pos;
    }

    /**
     * Returns the current offset in this stream.
     *
     * @return  the offset from the beginning of the stream, in bytes,
     *          at which the next read occurs.
     */
    public long getStreamPointer() {
         return pointer;
    }

    /**
     * Sets the stream-pointer offset, measured from the beginning of this
     * file, at which the next read occurs.
     *
     * @param  pos the offset position, measured in bytes from the
     *             beginning of the stream, at which to set the stream
     *                   pointer.
     * @exception  IOException  if <code>pos</code> is less than
     *                          <code>0</code> or if an I/O error occurs.
     */
    public void seek(long pos) throws IOException {
    	ensureOpen();
        if (pos < 0) {
        	throw new IOException("Negative seek position.");
        }
        pointer = pos;
    }

    public int read() throws IOException {
    	ensureOpen();
        long next = pointer + 1;
        long pos = readUntil(next);
        if (pos >= next) {
            cache.seek(pointer++);
            return cache.read();
        }
        return -1;    
    }

    public int read(byte[] b, int off, int len) throws IOException {
    	ensureOpen();
        if (b == null) {
            throw new NullPointerException();
        }
        if ((off < 0) || (len < 0) || (off + len > b.length)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        long pos = readUntil(pointer + len);

        // len will always fit into an int so this is safe
        len = (int)Math.min(len, pos - pointer);
        if (len > 0) {
            cache.seek(pointer);
            cache.readFully(b, off, len);
            pointer += len;
            return len;
        }	        
        return -1;
    }

    /**
     * Closes this stream and releases any system resources
     * associated with the stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
    	if(closed) return;
        cache.close();
        cacheFile.delete();
        src.close();
        src = null;
        closed = true;
    }
    
    public void shallowClose() throws IOException {
    	if(closed) return;
        cache.close();
        cacheFile.delete();
        src = null;
        closed = true;
    }
}