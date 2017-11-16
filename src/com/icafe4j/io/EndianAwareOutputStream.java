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

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Endian-aware OutputStream backed up by WriteStrategy
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 02/03/2014
 */
public class EndianAwareOutputStream extends OutputStream implements DataOutput {

	private OutputStream out; 
	private WriteStrategy strategy = WriteStrategyMM.getInstance();
	
	public EndianAwareOutputStream(OutputStream os) {
		out = os;
	}
	
	public void setWriteStrategy(WriteStrategy strategy) 
	{
		this.strategy = strategy;
	}
	
	public void write(int value) throws IOException {
	  out.write(value);
	}

	public void writeBoolean(boolean value) throws IOException {
		this.write(value ? 1 : 0);
	}

	public void writeByte(int value) throws IOException {
		this.write(value);
	}

	public void writeBytes(String value) throws IOException {
		new DataOutputStream(this).writeBytes(value);
	}

	public void writeChar(int value) throws IOException {
		this.writeShort(value);
	}

	public void writeChars(String value) throws IOException {
		int len = value.length();
		
		for (int i = 0 ; i < len ; i++) {
			int v = value.charAt(i);
		    this.writeShort(v);
		}
	}

	public void writeDouble(double value) throws IOException {
		 writeLong(Double.doubleToLongBits(value));
	}

	public void writeFloat(float value) throws IOException {
		 writeInt(Float.floatToIntBits(value));
	}

	public void writeInt(int value) throws IOException {
		byte[] buf = new byte[4];
		strategy.writeInt(buf, 0, value);
		this.write(buf, 0, 4);
	}

	public void writeLong(long value) throws IOException {
		byte[] buf = new byte[8];
		strategy.writeLong(buf, 0, value);
		this.write(buf, 0, 8);
	}
	
	public void writeS15Fixed16Number(float value) throws IOException {
		byte[] buf = new byte[4];
		strategy.writeS15Fixed16Number(buf, 0, value);
		this.write(buf, 0, 4);
	}

	public void writeShort(int value) throws IOException {
		byte[] buf = new byte[2];
		strategy.writeShort(buf, 0, value);
		this.write(buf, 0, 2);
	}
	
	public void writeU16Fixed16Number(float value) throws IOException {
		byte[] buf = new byte[4];
		strategy.writeU16Fixed16Number(buf, 0, value);
		this.write(buf, 0, 4);
	}

	public void writeU8Fixed8Number(float value) throws IOException {
		byte[] buf = new byte[2];
		strategy.writeU8Fixed8Number(buf, 0, value);
		this.write(buf, 0, 2);
	}
	
	public void writeUTF(String value) throws IOException {
		new DataOutputStream(this).writeUTF(value);
	}
	
	public void close() throws IOException {
		out.close();
	}
}