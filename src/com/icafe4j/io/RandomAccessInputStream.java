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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implements a random access input stream
 * <p>
 * Based on com.sun.media.jai.codec.SeekableStream.
 * <p>
 * To make it flexible, this class and any of its sub-class doesn't close the underlying
 * stream. It's up to the underlying stream creator to close them. This ensures the actual
 * stream out-lives the random stream itself in case we need to read more content from the
 * underlying stream.
 * <p>
 * NOTE:  for MemoryCacheRandomAccessInputStream, there is the risk of "over read" in which
 * more bytes are cached in the buffer than actually needed. In this case, the underlying
 * stream might not be usable anymore afterwards. 
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/24/2013 
 */ 
public abstract class RandomAccessInputStream extends InputStream implements DataInput {	
    
    private ReadStrategy strategy = ReadStrategyMM.getInstance();

	 /** The source stream. */
    protected InputStream src;
    protected boolean closed;
    
    protected RandomAccessInputStream(InputStream src) {
    	this.src = src;
    }
    
    /**
     * Closes the RandomAccessInputStream and but keeps it's underlying stream open
     * @throws IOException
     */
    public abstract void shallowClose() throws IOException;
   
    /**
     * Check to make sure that this stream has not been closed
     */
    protected void ensureOpen() throws IOException {
    	if (closed)
    		throw new IOException("Stream closed");
    }
    
    protected void finalize() throws Throwable {
		super.finalize();
		close();
	}
    
	public short getEndian() {
    	return strategy instanceof ReadStrategyMM?IOUtils.BIG_ENDIAN:IOUtils.LITTLE_ENDIAN;
    }
	
	public abstract long getStreamPointer();
	
	public abstract int read() throws IOException;
	
	public abstract int read(byte[] b, int off, int len) throws IOException;

	public final boolean readBoolean() throws IOException {
		int ch = this.read();
		if (ch < 0)
		    throw new EOFException();
		return (ch != 0);
	}
	
	public final byte readByte() throws IOException {
	    int ch = this.read();
		if (ch < 0)
		   throw new EOFException();
		return (byte)ch;
	}
	
	public final char readChar() throws IOException {
		return (char)(readShort()&0xffff);
	}

	public final double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}
	
	public final float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}	
	
    public final void readFully(byte[] b) throws IOException {
		readFully(b, 0, b.length);
	}

    public final void readFully(byte[] b, int off, int len) throws IOException {
		int n = 0;
		do {
			int count = this.read(b, off + n, len - n);
		    if (count < 0)
		        throw new EOFException();
		    n += count;
		} while (n < len);
	}

	public final int readInt() throws IOException {
		byte[] buf = new byte[4];
        readFully(buf);
    	return strategy.readInt(buf, 0);
	}

	@Deprecated
	public final String readLine() throws IOException {
		throw new UnsupportedOperationException(
			"readLine is not supported by RandomAccessInputStream."
		);
	}

	public final long readLong() throws IOException {
		byte[] buf = new byte[8];
        readFully(buf);
    	return strategy.readLong(buf, 0);
	}

	public final float readS15Fixed16Number() throws IOException {
		byte[] buf = new byte[4];
        readFully(buf);
		return strategy.readS15Fixed16Number(buf, 0);
	}

	public final short readShort() throws IOException {
		byte[] buf = new byte[2];
        readFully(buf);
    	return strategy.readShort(buf, 0);
	}

	public final float readU16Fixed16Number() throws IOException {
		byte[] buf = new byte[4];
        readFully(buf);
		return strategy.readU16Fixed16Number(buf, 0);
	}

	public final float readU8Fixed8Number() throws IOException {
		byte[] buf = new byte[2];
        readFully(buf);
		return strategy.readU8Fixed8Number(buf, 0);
	}
	
	public final int readUnsignedByte() throws IOException {
		int ch = this.read();
		if (ch < 0)
		   throw new EOFException();
	    return ch;
	}
	
	public final long readUnsignedInt() throws IOException {
		return readInt()&0xffffffffL;
	}

	public final int readUnsignedShort() throws IOException {
		return readShort()&0xffff;
	}

	/**
	 *  Due to the current implementation, writeUTF and readUTF are the
	 *  only methods which are machine or byte sequence independent as
	 *  they are actually both Motorola byte sequence under the hood.
	 *  
	 *  Whereas the following static method is byte sequence dependent
	 *  as it calls readUnsignedShort of RandomAccessInputStream.
	 *  
	 *  <code>DataInputStream.readUTF(this)</code>;
	 */
	public final String readUTF() throws IOException {
		return new DataInputStream(this).readUTF();	
	} 
	
	public abstract void seek(long loc) throws IOException;
	
	public void setReadStrategy(ReadStrategy strategy) {
		this.strategy = strategy;
	}	
	
	public int skipBytes(int n) throws IOException {
		if (n <= 0) {
			return 0;
		}
		return (int)skip(n);
	}
}