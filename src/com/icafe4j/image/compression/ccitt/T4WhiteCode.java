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

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration for T4 white codes
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/20/2013
 */
public enum T4WhiteCode implements T4Code {
	// Terminating codes
	CODE0(0, 8, (short)0x3500),
	CODE1(1, 6, (short)0x1c00),
	CODE2(2, 4, (short)0x7000),
	CODE3(3, 4, (short)0x8000),
	CODE4(4, 4, (short)0xb000),
	CODE5(5, 4, (short)0xc000),
	CODE6(6, 4, (short)0xe000),
	CODE7(7, 4, (short)0xf000),
	CODE8(8, 5, (short)0x9800),
	CODE9(9, 5, (short)0xa000),
	CODE10(10, 5, (short)0x3800),
	CODE11(11, 5, (short)0x4000),
	CODE12(12, 6, (short)0x2000),
	CODE13(13, 6, (short)0x0c00),
	CODE14(14, 6, (short)0xd000),
	CODE15(15, 6, (short)0xd400),
	CODE16(16, 6, (short)0xa800),
	CODE17(17, 6, (short)0xac00),
	CODE18(18, 7, (short)0x4e00),
	CODE19(19, 7, (short)0x1800),
	CODE20(20, 7, (short)0x1000),
	CODE21(21, 7, (short)0x2e00),
	CODE22(22, 7, (short)0x0600),   
	CODE23(23, 7, (short)0x0800),
	CODE24(24, 7, (short)0x5000),
	CODE25(25, 7, (short)0x5600),
	CODE26(26, 7, (short)0x2600),
	CODE27(27, 7, (short)0x4800),
	CODE28(28, 7, (short)0x3000),
	CODE29(29, 8, (short)0x0200),
	CODE30(30, 8, (short)0x0300),
	CODE31(31, 8, (short)0x1a00),
	CODE32(32, 8, (short)0x1b00),
	CODE33(33, 8, (short)0x1200),
	CODE34(34, 8, (short)0x1300),
	CODE35(35, 8, (short)0x1400),
	CODE36(36, 8, (short)0x1500),
	CODE37(37, 8, (short)0x1600),
	CODE38(38, 8, (short)0x1700),
	CODE39(39, 8, (short)0x2800),
	CODE40(40, 8, (short)0x2900),
	CODE41(41, 8, (short)0x2a00),
	CODE42(42, 8, (short)0x2b00),
	CODE43(43, 8, (short)0x2c00),
	CODE44(44, 8, (short)0x2d00),
	CODE45(45, 8, (short)0x0400),	
	CODE46(46, 8, (short)0x0500),
	CODE47(47, 8, (short)0x0a00),
	CODE48(48, 8, (short)0x0b00),
	CODE49(49, 8, (short)0x5200),
	CODE50(50, 8, (short)0x5300),
	CODE51(51, 8, (short)0x5400),
	CODE52(52, 8, (short)0x5500),
	CODE53(53, 8, (short)0x2400),
	CODE54(54, 8, (short)0x2500),
	CODE55(55, 8, (short)0x5800),
	CODE56(56, 8, (short)0x5900),
	CODE57(57, 8, (short)0x5a00),
	CODE58(58, 8, (short)0x5b00),
	CODE59(59, 8, (short)0x4a00),
	CODE60(60, 8, (short)0x4b00),
	CODE61(61, 8, (short)0x3200),
	CODE62(62, 8, (short)0x3300),
	CODE63(63, 8, (short)0x3400),
	// Makeup codes
	CODE64(64, 5, (short)0xd800),
	CODE128(128, 5, (short)0x9000),
	CODE192(192, 6, (short)0x5c00),
	CODE256(256, 7, (short)0x6e00),
	CODE320(320, 8, (short)0x3600),
	CODE384(384, 8, (short)0x3700),
	CODE448(448, 8, (short)0x6400),
	CODE512(512, 8, (short)0x6500),
	CODE576(576, 8, (short)0x6800),
	CODE640(640, 8, (short)0x6700),
	CODE704(704, 9, (short)0x6600),
	CODE768(768, 9, (short)0x6680),
	CODE832(832, 9, (short)0x6900),
	CODE896(896, 9, (short)0x6980),
	CODE960(960, 9, (short)0x6a00),
	CODE1024(1024, 9, (short)0x6a80),
	CODE1088(1088, 9, (short)0x6b00),
	CODE1152(1152, 9, (short)0x6b80),
	CODE1216(1216, 9, (short)0x6c00),
	CODE1280(1280, 9, (short)0x6c80),
	CODE1344(1344, 9, (short)0x6d00),
	CODE1408(1408, 9, (short)0x6d80),
	CODE1472(1472, 9, (short)0x4c00),
	CODE1536(1536, 9, (short)0x4c80),
	CODE1600(1600, 9, (short)0x4d00),
	CODE1664(1664, 6, (short)0x6000),
	CODE1728(1728, 9, (short)0x4d80),
	CODE1792(1792, 11, (short)0x0100),
	CODE1856(1856, 11, (short)0x0180),
	CODE1920(1920, 11, (short)0x01a0),
	CODE1984(1984, 12, (short)0x0120),
	CODE2048(2048, 12, (short)0x0130),
	CODE2112(2112, 12, (short)0x0140),
	CODE2176(2176, 12, (short)0x0150),
	CODE2240(2240, 12, (short)0x0160),
	CODE2304(2304, 12, (short)0x0170),
	CODE2368(2368, 12, (short)0x01c0),
	CODE2432(2432, 12, (short)0x01d0),
	CODE2496(2496, 12, (short)0x01e0),
	CODE2560(2560, 12, (short)0x01f0),
	// Unknown code
	UNKNOWN(9999, 12, (short)0x0000),
	// Special codes
	EOL(-1, 12, (short)0x0010),
	FILL_4_EOL(-2, 16, (short)0x0001),
	FILL_3_EOL(-3, 15, (short)0x0002),
	FILL_2_EOL(-4, 14, (short)0x0004),
	FILL_1_EOL(-5, 13, (short)0x0008);
 	
	private T4WhiteCode(int runLen, int codeLen, short code) {
		this.runLen = runLen;
		this.codeLen = codeLen;
		this.code = code;
	}
	
	public int getRunLen() {
		return runLen;
	}
	
	public int getCodeLen() {
		return codeLen;
	}
	
	public short getCode() {
		return code;
	}
	
	public static T4Code fromRunLen(int runLen) {
		T4Code t4Code = runLenMap.get(runLen);
		if (t4Code == null)
			return UNKNOWN;
		return t4Code;
	}
	
	public static T4Code fromCode(short code) {
		T4Code t4Code = codeMap.get(code);
		if (t4Code == null)
			return UNKNOWN;
		return t4Code;
	}
	   
	private static final Map<Integer, T4Code> runLenMap = new HashMap<Integer, T4Code>();
	private static final Map<Short, T4Code> codeMap = new HashMap<Short, T4Code>();
	    
	static
	{
		for(T4Code code : values()) {
			runLenMap.put(code.getRunLen(), code);
			codeMap.put(code.getCode(), code);
		}	
	}	
	
	private final int runLen;
	private final int codeLen;
	private final short code;
}