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

public interface ImageEncoder {
	/**
	 * The actual encoding implementation
	 * 
	 * @param pixels array of pixels (This has nothing to do with the actual bits per pixel since it could be pixel packed)
	 * @param start offset in the pixel array where the encoding starts (the actual position could be anywhere inside the
	 * 	   			offset byte which maybe kept track of by the implementation class through a parameter such as
	 * 				<em>currPos</em> if the encoder is pixel oriented).
	 * @param len the number of pixels to be encoded if the encoder is pixel oriented like CCITT or the number of bytes to
	 * 		  	  be encoded if the encoder is byte oriented like LZW etc.
	 */
	public void encode(byte[] pixels, int start, int len) throws Exception;
	// Wrap up
	public void finish() throws Exception;
	// Prepare
	public void initialize() throws Exception;
	
	public int getCompressedDataLen();
}