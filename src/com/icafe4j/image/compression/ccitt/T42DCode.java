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

/**
 * T4 two dimensional codes
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/02/2014
 */
public enum T42DCode implements T4Code {
	// Table 4/T.4 - Two-dimensional code table
	P((short)0x1000, 4),
	H((short)0x2000, 3),
	V0((short)0x8000, 1),
	VR1((short)0x6000, 3),
	VR2((short)0x0c00, 6),
	VR3((short)0x0600, 7),
	VL1((short)0x4000, 3),
	VL2((short)0x0800, 6),
	VL3((short)0x0400, 7 ),
	EXTENSION2D((short)0x0200, 7),
	EXTENSION1D((short)0x0080, 9),
	// Unknown code
	UNKNOWN((short)0x0000, 12);
	
	private T42DCode(short code, int codeLen) {
		this.code = code;
		this.codeLen = codeLen;
	}
	
	public short getCode() {
		return code;
	}
	
	public int getCodeLen() {
		return codeLen;
	}
	
	public int getRunLen() {
		return 0;
	}
	
	private final short code;
	private final int codeLen;
}