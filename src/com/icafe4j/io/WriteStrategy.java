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
public interface WriteStrategy {
	//
	public void writeInt(byte[] buf, int start_idx, int value) throws IOException;
	public void writeInt(OutputStream os, int value) throws IOException;
	public void writeLong(byte[] buf, int start_idx, long value) throws IOException;
	public void writeLong(OutputStream os, long value) throws IOException;
	public void writeS15Fixed16Number(byte[] buf, int start_idx, float value) throws IOException;
	public void writeS15Fixed16Number(OutputStream os, float value) throws IOException;
	public void writeShort(byte[] buf, int start_idx, int value) throws IOException;
	public void writeShort(OutputStream os, int value) throws IOException;
	public void writeU16Fixed16Number(byte[] buf, int start_idx, float value) throws IOException;
	public void writeU16Fixed16Number(OutputStream os, float value) throws IOException;
	public void writeU8Fixed8Number(byte[] buf, int start_idx, float value) throws IOException;
    public void writeU8Fixed8Number(OutputStream is, float value) throws IOException;
}