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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * General purpose IO helper class
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/12/2012
 */
public class IOUtils {
	//
	public static final short LITTLE_ENDIAN = 0x4949;//II Intel
	public static final short BIG_ENDIAN = 0x4d4d;//MM Motorola	
	
	public static void close(InputStream is) throws IOException {
		is.close();
	}
	 
	public static void close(OutputStream os) throws IOException {
		os.close();
	}
	 
	public static byte[] inputStreamToByteArray(InputStream is) throws IOException {
		 
		ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
		byte[] buf = new byte[4096];
		int len = 0;
		
		while((len = is.read(buf)) != -1) {
			bout.write(buf, 0, len);
		}
		
		is.close();
		bout.close();
		 
		return bout.toByteArray();
	}
     
	public static int read(InputStream is) throws IOException {
		return is.read();
	}
   
	public static int read(InputStream is, byte[] bytes) throws IOException {
		return is.read(bytes);
	}     
     
	public static int read(InputStream is, byte[] bytes, int off, int len) throws IOException {
		return is.read(bytes, off, len);
	}
     
	public static double readDouble(InputStream is) throws IOException {
		return Double.longBitsToDouble(readLong(is));
	}
	 
	public static double readDoubleMM(InputStream is) throws IOException {
		return Double.longBitsToDouble(readLongMM(is));
	}	
	 
	public static float readFloat(InputStream is) throws IOException {
		return Float.intBitsToFloat(readInt(is));
	}

	public static float readFloatMM(InputStream is) throws IOException {
		return Float.intBitsToFloat(readIntMM(is));
	}
	 
	public static byte[] readFully(InputStream is, int bufLen) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(bufLen);
		byte[] buf = new byte[bufLen];
		int count = is.read(buf);         
		
		while (count > 0) {
			bout.write(buf, 0, count);
			count = is.read(buf);
		}
		
		return bout.toByteArray();
	}
	
	public static void readFully(InputStream is, byte b[]) throws IOException {
		readFully(is, b, 0, b.length);
	}
	 
	public static void readFully(InputStream is, byte[] b, int off, int len) throws IOException {
		if (len < 0)
			throw new IndexOutOfBoundsException();
		int n = 0;         
		while (n < len) {
			int count = is.read(b, off + n, len - n);
			if (count < 0)
				throw new EOFException();
			n += count;
		}
	}
	 
	public static int readInt(byte[] buf, int start_idx) { 
		return ((buf[start_idx++]&0xff)|((buf[start_idx++]&0xff)<<8)|
				((buf[start_idx++]&0xff)<<16)|((buf[start_idx++]&0xff)<<24));
	}

	public static int readInt(InputStream is) throws IOException {
		byte[] buf = new byte[4];
		readFully(is, buf);
		 
		return (((buf[3]&0xff)<<24)|((buf[2]&0xff)<<16)|((buf[1]&0xff)<<8)|(buf[0]&0xff));
	}
	 
	public static int readIntMM(byte[] buf, int start_idx) { 
		return (((buf[start_idx++]&0xff)<<24)|((buf[start_idx++]&0xff)<<16)|
				((buf[start_idx++]&0xff)<<8)|(buf[start_idx++]&0xff));
	}

	public static int readIntMM(InputStream is) throws IOException {
		byte[] buf = new byte[4];
		readFully(is, buf);
		 
		return (((buf[0]&0xff)<<24)|((buf[1]&0xff)<<16)|
				((buf[2]&0xff)<<8)|(buf[3]&0xff));
	}

	public static long readLong(byte[] buf, int start_idx) {    	 
		return ((buf[start_idx++]&0xffL)|(((buf[start_idx++]&0xffL)<<8)|((buf[start_idx++]&0xffL)<<16)|
				((buf[start_idx++]&0xffL)<<24)|((buf[start_idx++]&0xffL)<<32)|((buf[start_idx++]&0xffL)<<40)|
				((buf[start_idx++]&0xffL)<<48)|(buf[start_idx]&0xffL)<<56));
	}

	public static long readLong(InputStream is) throws IOException {
		byte[] buf = new byte[8];
		readFully(is, buf);
		 
		return (((buf[7]&0xffL)<<56)|((buf[6]&0xffL)<<48)|
				((buf[5]&0xffL)<<40)|((buf[4]&0xffL)<<32)|((buf[3]&0xffL)<<24)|
				((buf[2]&0xffL)<<16)|((buf[1]&0xffL)<<8)|(buf[0]&0xffL));
	}

	public static long readLongMM(byte[] buf, int start_idx) {		 
		return (((buf[start_idx++]&0xffL)<<56)|((buf[start_idx++]&0xffL)<<48)|
				((buf[start_idx++]&0xffL)<<40)|((buf[start_idx++]&0xffL)<<32)|((buf[start_idx++]&0xffL)<<24)|
				((buf[start_idx++]&0xffL)<<16)|((buf[start_idx++]&0xffL)<<8)|(buf[start_idx]&0xffL));
	}

	public static long readLongMM(InputStream is) throws IOException {
		byte[] buf = new byte[8];
		readFully(is, buf);
		 
		return (((buf[0]&0xffL)<<56)|((buf[1]&0xffL)<<48)|
				((buf[2]&0xffL)<<40)|((buf[3]&0xffL)<<32)|((buf[4]&0xffL)<<24)|
				((buf[5]&0xffL)<<16)|((buf[6]&0xffL)<<8)|(buf[7]&0xffL));
	}
	 
	public static float readS15Fixed16MMNumber(byte[] buf, int start_idx) { 
		short s15 = (short)(((buf[start_idx++]&0xff)<<8)|(buf[start_idx++]&0xff));
		int fixed16 = (((buf[start_idx++]&0xff)<<8)|(buf[start_idx]&0xff));
		 
		return s15 + fixed16/65536.0f;
	}
	 
	public static float readS15Fixed16MMNumber(InputStream is) throws IOException { 		
		byte[] buf = new byte[4];
		IOUtils.readFully(is, buf);
		 
		short s15 = (short)((buf[1]&0xff)|((buf[0]&0xff)<<8));
		int fixed16 = ((buf[3]&0xff)|((buf[2]&0xff)<<8));
		 
		return s15 + fixed16/65536.0f;	
	}
	
	public static float readS15Fixed16Number(byte[] buf, int start_idx) {
		short s15 = (short)((buf[start_idx++]&0xff)|((buf[start_idx++]&0xff)<<8));
		int fixed16 = ((buf[start_idx++]&0xff)|((buf[start_idx]&0xff)<<8));
		 
		return s15 + fixed16/65536.0f;
	}
	 
	public static float readS15Fixed16Number(InputStream is) throws IOException { 		
		byte[] buf = new byte[4];
		IOUtils.readFully(is, buf);
		 
		short s15 = (short)((buf[0]&0xff)|((buf[1]&0xff)<<8));
		int fixed16 = ((buf[2]&0xff)|((buf[3]&0xff)<<8));
		 
		return s15 + fixed16/65536.0f;	
	}
	
	public static short readShort(byte[] buf, int start_idx) { 
		return (short)((buf[start_idx++]&0xff)|((buf[start_idx]&0xff)<<8));
	}

	public static short readShort(InputStream is) throws IOException { 
		byte[] buf = new byte[2];
		readFully(is, buf);
		
		return (short)(((buf[1]&0xff)<<8)|(buf[0]&0xff));
	}
	
	public static short readShortMM(byte[] buf, int start_idx) { 
		return (short)(((buf[start_idx++]&0xff)<<8)|(buf[start_idx]&0xff));
	}

	public static short readShortMM(InputStream is) throws IOException { 
		byte[] buf = new byte[2];
		readFully(is, buf);
		
		return (short)(((buf[0]&0xff)<<8)|(buf[1]&0xff));
	}
	 
	public static long readUnsignedInt(byte[] buf, int start_idx) { 
		return ((buf[start_idx++]&0xff)|((buf[start_idx++]&0xff)<<8)|
				((buf[start_idx++]&0xff)<<16)|((buf[start_idx++]&0xff)<<24))& 0xffffffffL;
	}
	
	public static long readUnsignedInt(InputStream is) throws IOException {
		byte[] buf = new byte[4];
		readFully(is, buf);
		
		return (((buf[3]&0xff)<<24)|((buf[2]&0xff)<<16)|
				((buf[1]&0xff)<<8)|(buf[0]&0xff))& 0xffffffffL;
	}
	
	public static long readUnsignedIntMM(byte[] buf, int start_idx)	{ 
		return readIntMM(buf, start_idx) & 0xffffffffL;
	}
	
	public static long readUnsignedIntMM(InputStream is) throws IOException	{
		return readIntMM(is) & 0xffffffffL;
	}
	
	public static int readUnsignedShort(byte[] buf, int start_idx) { 
		return (buf[start_idx++]&0xff)|((buf[start_idx]&0xff)<<8);
	}
	
	public static int readUnsignedShort(InputStream is) throws IOException {
		byte[] buf = new byte[2];
		readFully(is, buf);
		
		return ((buf[1]&0xff)<<8)|(buf[0]&0xff);
	}
	
	public static int readUnsignedShortMM(byte[] buf, int start_idx) { 
		return ((buf[start_idx++]&0xff)<<8)|(buf[start_idx]&0xff);
	}
	
	public static int readUnsignedShortMM(InputStream is) throws IOException { 
		byte[] buf = new byte[2];
		readFully(is, buf);
		
		return ((buf[0]&0xff)<<8)|(buf[1]&0xff);
	}
	
	public static long skip(InputStream is, long len) throws IOException {
		return is.skip(len);
	}
	
	public static void skipFully(InputStream is, int n) throws IOException {
		readFully(is, new byte[n]);
	}	
	 
	public static void write(OutputStream os, byte[] bytes) throws IOException {
		os.write(bytes);
	}
	 
	public static void write(OutputStream os, byte[] bytes, int off, int len) throws IOException {
		os.write(bytes, off, len);
	}
	 
	public static void write(OutputStream os, int abyte) throws IOException {
		os.write(abyte);
	}
	
	// Write an int to the OutputStream with Intel format 
	public static void writeInt(OutputStream os, int value) throws IOException {
		os.write(new byte[] {
	        (byte)value,
	        (byte)(value>>>8),
	        (byte)(value>>>16),
	        (byte)(value>>>24)});
	}
    
	// Write an int to the OutputStream with Motorola format 
	public static void writeIntMM(OutputStream os, int value) throws IOException {
		os.write(new byte[] {
			(byte)(value >>> 24),
			(byte)(value >>> 16),
			(byte)(value >>> 8),
			(byte)value});
	}
	
	public static void writeLong(OutputStream os, long value) throws IOException {
		os.write(new byte[] {
	        (byte)value, (byte)(value>>>8),
	        (byte)(value>>>16), (byte)(value>>>24),
	        (byte)(value>>>32), (byte)(value>>>40),
		    (byte)(value>>>48), (byte)(value>>>56)});
	}
	 
	public static void writeLongMM(OutputStream os, long value) throws IOException {
		os.write(new byte[] {
			(byte)(value>>>56),
			(byte)(value>>>48),
			(byte)(value>>>40),
			(byte)(value>>>32),
			(byte)(value>>>24),
			(byte)(value>>>16),
			(byte)(value>>>8),
			(byte)value});
	}
	
	public static void writeShort(OutputStream os, int value) throws IOException {
		os.write(new byte[] {
		  (byte)value,
		  (byte)(value >>> 8)});
	}
	 
	public static void writeShortMM(OutputStream os, int value) throws IOException {
		os.write(new byte[] {
			(byte)(value >>> 8),
			(byte)value});
	}
	
	private IOUtils() {}
}