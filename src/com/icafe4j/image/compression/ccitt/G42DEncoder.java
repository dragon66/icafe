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

package com.icafe4j.image.compression.ccitt;

import java.io.OutputStream;

import com.icafe4j.image.compression.ImageEncoder;
import com.icafe4j.util.Updatable;

/**
 * CCITT Group 4 two dimensional encoding
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/20/2013
 */
public class G42DEncoder extends G32DEncoder implements ImageEncoder {
	
	public G42DEncoder(OutputStream os, int scanLineWidth, int buf_length, Updatable<Integer> writer) {
		// The K value is not used by G4 encoding, we pass in Integer.MAX_VALUE as a place holder
		super(os, scanLineWidth, buf_length, Integer.MAX_VALUE, writer);		
	}
	
	/**
	 * This method assumes "len" is a multiplication of scan line length.
	 * 
	 * @param len the number of pixels to be encoded
	 */
	@Override public void encode(byte[] pixels, int start, int len) throws Exception {
		//
		int totalScanLines = len/scanLineWidth;
		
		for(int i = 0; i < totalScanLines; i++) {
			start = encode2DLine(pixels, start);
		}
		
		// Now send EOFB which replaces RTC of G3 - 2 consecutive EOLs to output
		send_code_to_buffer(T4Code.EOL, 12);
		send_code_to_buffer(T4Code.EOL, 12);
		
		// We need to flush the last buffer
		setExtraFlush(true);	
	}	
}