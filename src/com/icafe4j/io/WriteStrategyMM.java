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
import java.io.OutputStream;

/**
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/29/2013
 */
public class WriteStrategyMM implements WriteStrategy {

	private static final WriteStrategyMM instance = new WriteStrategyMM();	
	 
	public static WriteStrategyMM getInstance() 
	{
		return instance;
	}
	 
	private WriteStrategyMM(){}
	
	public void writeInt(byte[] buf, int start_idx, int value)
			throws IOException {
		
		byte[] tmp =  {(byte)(value >>> 24), (byte)(value >>> 16), (byte)(value >>> 8), (byte)value};
		
		System.arraycopy(tmp, 0, buf, start_idx, 4);
	}

	public void writeInt(OutputStream os, int value) throws IOException {
		os.write(new byte[] {
			 (byte)(value >>> 24),
		     (byte)(value >>> 16),
		     (byte)(value >>> 8),
		     (byte)value});
	}
	
	public void writeLong(byte[] buf, int start_idx, long value)
			throws IOException {
		
		byte[] tmp = new byte[] { (byte)(value>>>56), (byte)(value>>>48),
			     (byte)(value>>>40), (byte)(value>>>32), (byte)(value>>>24),
			     (byte)(value>>>16), (byte)(value>>>8), (byte)value};
		
		System.arraycopy(tmp, 0, buf, start_idx, 8);
	}

	public void writeLong(OutputStream os, long value) throws IOException {
		os.write(new byte[] {
	         (byte)(value>>>56), (byte)(value>>>48),
			 (byte)(value>>>40), (byte)(value>>>32),
			 (byte)(value>>>24), (byte)(value>>>16),
			 (byte)(value>>>8),  (byte)value});
	}
	
	public void writeS15Fixed16Number(byte[] buf, int start_idx, float value)
			throws IOException {
		// Check range
		if((value < -32768.0f)||(value >= (32767 + (65535/65536.0f)))||Float.isNaN(value)) {
			throw new IllegalArgumentException(value + " is not a valid S15Fixed16Number");
		}
		
		if(value == 0.0f) {
			writeInt(buf, start_idx, 0);
		}
		else if(value > 0.0f) {
			writeU16Fixed16Number(buf, start_idx, value);
		}
		else {
			int s15 = (int)Math.floor(value);
			int fixed16 = (int)((value - s15)*65536.0f);
			buf[start_idx++] = (byte)(s15>>>8);
			buf[start_idx++] = (byte)s15;
			buf[start_idx++] = (byte)(fixed16>>>8);
			buf[start_idx] = (byte)fixed16;
		}			
	}

	public void writeS15Fixed16Number(OutputStream os, float value) throws IOException {
		// Check range
		if((value < -32768.0f)||(value >= (32767 + (65535/65536.0f)))||Float.isNaN(value)) {
			throw new IllegalArgumentException(value + " is not a valid S15Fixed16Number");
		}
		
		if(value == 0.0f) {
			writeInt(os, 0);
		}
		else if(value > 0.0f) {
			writeU16Fixed16Number(os, value);
		}
		else {
			int s15 = (int)Math.floor(value);
			int fixed16 = (int)((value - s15)*65536.0f);
			
			os.write(new byte[] {
				  (byte)(s15 >>> 8),
				  (byte)s15,
				  (byte)(fixed16>>>8),
				  (byte)fixed16
				  });
		}
	}

	public void writeShort(byte[] buf, int start_idx, int value)
			throws IOException {
		buf[start_idx] = (byte)(value>>>8);
		buf[start_idx + 1] = (byte)value;
	}

	public void writeShort(OutputStream os, int value) throws IOException {
		os.write(new byte[] {
				 (byte)(value >>> 8),
			     (byte)value});
	}

	public void writeU16Fixed16Number(byte[] buf, int start_idx, float value)
			throws IOException {
		// Check range
		if((value < 0.0f)||(value >= (65535 + (65535/65536.0f)))||Float.isNaN(value)) {
			throw new IllegalArgumentException(value + " is not a valid U16Fixed16Number");
		}
		if(value == 0.0f) {
			writeInt(buf, start_idx, 0);
		}
		else {
			int s15 = (int)value;
			int fixed16 = (int)((value - s15)*65536.0f);
			buf[start_idx++] = (byte)(s15>>>8);
			buf[start_idx++] = (byte)s15;
			buf[start_idx++] = (byte)(fixed16>>>8);
			buf[start_idx] = (byte)fixed16;			
		}
	}

	public void writeU16Fixed16Number(OutputStream os, float value) throws IOException {
		// Check range
		if((value < 0.0f)||(value >= (65535 + (65535/65536.0f)))||Float.isNaN(value)) {
			throw new IllegalArgumentException(value + " is not a valid U16Fixed16Number");
		}
		
		if(value == 0.0f) {
			writeInt(os, 0);
		}
		else {
			int s15 = (int)value;
			int fixed16 = (int)((value - s15)*65536.0f);
			
			os.write(new byte[] {
				  (byte)(s15 >>> 8),
				  (byte)s15,
				  (byte)(fixed16>>>8),
				  (byte)fixed16				  
				  });
		}
	}

	public void writeU8Fixed8Number(byte[] buf, int start_idx, float value)
			throws IOException {
		// Check range
		if((value < 0.0f)||(value >= (255 + (255/256.0f)))||Float.isNaN(value)) {
			throw new IllegalArgumentException(value + " is not a valid U8ixed8Number");
		}
		if(value == 0.0f) {
			writeShort(buf, start_idx, 0);
		}
		else {
			int u8 = (int)value;
			int fixed8 = (int)((value - u8)*256.0f);
			buf[start_idx++] = (byte)u8;
			buf[start_idx] = (byte)fixed8;
		}
	}

	public void writeU8Fixed8Number(OutputStream os, float value) throws IOException {
		// Check range
		if((value < 0.0f)||(value >= (255 + (255/256.0f)))||Float.isNaN(value)) {
			throw new IllegalArgumentException(value + " is not a valid U8Fixed8Number");
		}
		
		if(value == 0.0f) {
			writeShort(os, 0);
		}
		else {
			int u8 = (int)value;
			int fixed8 = (int)((value - u8)*256.0f);
			
			os.write(new byte[] {
				  (byte)u8,
				  (byte)fixed8
				  });
		}
	}
}