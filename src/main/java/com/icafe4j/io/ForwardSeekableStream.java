/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.io;

import java.io.InputStream;
import java.io.IOException;

/**
 * A subclass of <code>SeekableStream</code> that may be used
 * to wrap a regular <code>InputStream</code> efficiently.
 * Seeking backwards is not supported.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public class ForwardSeekableStream extends SeekableStream {

    /** The source <code>InputStream</code>. */
    private InputStream src;

    /** The current position. */
    long pointer = 0L;

    /** The marked position. */
    long markPos = -1L;

    /** 
     * Constructs a <code>InputStreamForwardSeekableStream</code> from a
     * regular <code>InputStream</code>.
     */
    public ForwardSeekableStream(InputStream src) {
        this.src = src;
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    public final int read() throws IOException {
        int result = src.read();
        if (result != -1) {
            ++pointer;
        }
        return result;
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    public final int read(byte[] b, int off, int len) throws IOException {
        int result = src.read(b, off, len);
        if (result != -1) {
            pointer += result;
        }
        return result;
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    public final long skip(long n) throws IOException {
        long skipped = src.skip(n);
        pointer += skipped;
        return skipped;
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    public final int available() throws IOException {
        return src.available();
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    public final void close() throws IOException {
        src.close();
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    public synchronized final void mark(int readLimit) {
        markPos = pointer;
        src.mark(readLimit);
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    public synchronized final void reset() throws IOException {
        if (markPos != -1) {
            pointer = markPos;
        }
        src.reset();
    }

    /** Forwards the request to the real <code>InputStream</code>. */
    public boolean markSupported() {
        return src.markSupported();
    }

    /** Returns <code>false</code> since seking backwards is not supported. */
    public final boolean canSeekBackwards() {
        return false;
    }

    /** Returns the current position in the stream (bytes read). */
    public final long getFilePointer() {
        return pointer;
    }

    /**
     * Seeks forward to the given position in the stream.
     * If <code>pos</code> is smaller than the current position
     * as returned by <code>getFilePointer()</code>, nothing
     * happens.
     */
    public final void seek(long pos) throws IOException {
        while (pos - pointer > 0) {
            pointer += src.skip(pos - pointer);
        }
    }
}