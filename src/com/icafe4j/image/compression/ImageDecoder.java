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

package com.icafe4j.image.compression;

public interface ImageDecoder {
	/**
	 * @param pix buffer to put decoded data
	 * @param offset offset to start put decoded data
	 * @param len the maximum number of uncompressed bytes
	 * @return number of pixels decoded
	 * @throws Exception
	 */
	public int decode(byte[] pix, int offset, int len) throws Exception;
	public void setInput(byte[] input);
	public void setInput(byte[] input, int offset, int len);
}