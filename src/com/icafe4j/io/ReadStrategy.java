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

/**
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/27/2012
 */
public interface ReadStrategy {
	//
	public int readInt(byte[] buf, int start_idx);
	public int readInt(InputStream is) throws IOException;
	public long readLong(byte[] buf, int start_idx);
	public long readLong(InputStream is) throws IOException;
	public float readS15Fixed16Number(byte[] buf, int start_idx);
	public float readS15Fixed16Number(InputStream is) throws IOException;
	public short readShort(byte[] buf, int start_idx);
	public short readShort(InputStream is) throws IOException;
	public float readU16Fixed16Number(byte[] buf, int start_idx);
	public float readU16Fixed16Number(InputStream is) throws IOException;
    public float readU8Fixed8Number(byte[] buf, int start_idx);
	public float readU8Fixed8Number(InputStream is) throws IOException;
	public long readUnsignedInt(byte[] buf, int start_idx);
    public long readUnsignedInt(InputStream is) throws IOException;
    public int readUnsignedShort(byte[] buf, int start_idx);
    public int readUnsignedShort(InputStream is) throws IOException;
}