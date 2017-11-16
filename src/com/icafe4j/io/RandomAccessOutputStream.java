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
 * RandomAccessOutputStream.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    07Apr2015  Removed flush(), move it's function to close()
 */

package com.icafe4j.io;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Based on javax.imageio.stream.MemoryCache.java.
 * * <p>
 * To make it flexible, this class and any of its sub-class doesn't close the underlying
 * stream. It's up to the underlying stream creator to close them. This ensures the actual
 * stream out-lives the random stream itself in case we need to write more content to the
 * underlying stream.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/29/2013
 */
public abstract class RandomAccessOutputStream extends OutputStream implements DataOutput {

	private WriteStrategy strategy = WriteStrategyMM.getInstance();
	
	/** The destination stream. */
	protected OutputStream dist;
	protected boolean closed;
	
	protected RandomAccessOutputStream(OutputStream dist) {
		this.dist = dist;
	}
	
	public void close() throws IOException {
		long flushPos = getFlushPos();
		long length = getLength();
		
		if(flushPos < length) {
			seek(flushPos);
			writeToStream(length - flushPos);
		}
	}
	
	/**
     * Closes the RandomAccessInputStream and it's underlying stream
     * @throws IOException
     */
    public abstract void shallowClose() throws IOException;
    
    /**
     * Check to make sure that this stream has not been closed
     */
    protected  void ensureOpen() throws IOException {
    	if (closed)
    		throw new IOException("Stream closed");
    }
	
	public abstract void disposeBefore(long pos) throws IOException;
	
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}
		
	public short getEndian() {
		return strategy instanceof WriteStrategyMM?IOUtils.BIG_ENDIAN:IOUtils.LITTLE_ENDIAN;
	}
	
	public abstract long getFlushPos();
	
	/**
	 * Returns the total length of data that has been cached,
	 * regardless of whether any early blocks have been disposed.
	 * This value will only ever increase. 
	 * @throws IOException 
	 */
	public abstract long getLength();
	
	/**
	 * @return the current stream position
	 * @throws IOException 
	 */
	public abstract long getStreamPointer();	
	
	/** Reset this stream to be used again */
	public abstract void reset();
	
	public abstract void seek(long pos) throws IOException;
	
	public void setWriteStrategy(WriteStrategy strategy) 
	{
		this.strategy = strategy;
	}
	
	public abstract void write(byte[] b, int off, int len) throws IOException;
	
	@Override
	public abstract void write(int value) throws IOException;

	public final void writeBoolean(boolean value) throws IOException {
		this.write(value ? 1 : 0);
	}

	public final void writeByte(int value) throws IOException {
		this.write(value);
	}

	public final void writeBytes(String value) throws IOException {
		new DataOutputStream(this).writeBytes(value);
	}

	public final void writeChar(int value) throws IOException {
		this.writeShort(value);
	}

	public final void writeChars(String value) throws IOException {
		int len = value.length();
		
		for (int i = 0 ; i < len ; i++) {
			int v = value.charAt(i);
		    this.writeShort(v);
		}
	}

	public final void writeDouble(double value) throws IOException {
		 writeLong(Double.doubleToLongBits(value));
	}

	public final void writeFloat(float value) throws IOException {
		 writeInt(Float.floatToIntBits(value));
	}

	public final void writeInt(int value) throws IOException {
		byte[] buf = new byte[4];
		strategy.writeInt(buf, 0, value);
		this.write(buf, 0, 4);
	}
	
	public final void writeLong(long value) throws IOException {
		byte[] buf = new byte[8];
		strategy.writeLong(buf, 0, value);
		this.write(buf, 0, 8);
	}

	public final void writeS15Fixed16Number(float value) throws IOException {
		byte[] buf = new byte[4];
		strategy.writeS15Fixed16Number(buf, 0, value);
		this.write(buf, 0, 4);
	}
	
	public final void writeShort(int value) throws IOException {
		byte[] buf = new byte[2];
		strategy.writeShort(buf, 0, value);
		this.write(buf, 0, 2);
	} 
		
	public abstract void writeToStream(long len) throws IOException;

	public final void writeU16Fixed16Number(float value) throws IOException {
		byte[] buf = new byte[4];
		strategy.writeU16Fixed16Number(buf, 0, value);
		this.write(buf, 0, 4);
	}
	
	public final void writeU8Fixed8Number(float value) throws IOException {
		byte[] buf = new byte[2];
		strategy.writeU8Fixed8Number(buf, 0, value);
		this.write(buf, 0, 2);
	}
	
	public final void writeUTF(String value) throws IOException {
		new DataOutputStream(this).writeUTF(value);
	}
}