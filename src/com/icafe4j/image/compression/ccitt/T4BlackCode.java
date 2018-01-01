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
 * Enumeration for T4 black codes
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/20/2013
 */
public enum T4BlackCode implements T4Code {
	// Terminating codes
	CODE0(0, 10, (short)0x0dc0),
	CODE1(1, 3, (short)0x4000),
	CODE2(2, 2, (short)0xc000),
	CODE3(3, 2, (short)0x8000),
	CODE4(4, 3, (short)0x6000),
	CODE5(5, 4, (short)0x3000),
	CODE6(6, 4, (short)0x2000),
	CODE7(7, 5, (short)0x1800),
	CODE8(8, 6, (short)0x1400),
	CODE9(9, 6, (short)0x1000),
	CODE10(10, 7, (short)0x0800),
	CODE11(11, 7, (short)0x0a00),
	CODE12(12, 7, (short)0x0e00),
	CODE13(13, 8, (short)0x0400),
	CODE14(14, 8, (short)0x0700),
	CODE15(15, 9, (short)0x0c00),
	CODE16(16, 10, (short)0x05c0),
	CODE17(17, 10, (short)0x0600),
	CODE18(18, 10, (short)0x0200),
	CODE19(19, 11, (short)0x0ce0),
	CODE20(20, 11, (short)0x0d00),
	CODE21(21, 11, (short)0x0d80),
	CODE22(22, 11, (short)0x06e0),
	CODE23(23, 11, (short)0x0500),	   	   
	CODE24(24, 11, (short)0x02e0),
	CODE25(25, 11, (short)0x0300),
	CODE26(26, 12, (short)0x0ca0),
	CODE27(27, 12, (short)0x0cb0),
	CODE28(28, 12, (short)0x0cc0),
	CODE29(29, 12, (short)0x0cd0),
	CODE30(30, 12, (short)0x0680),
	CODE31(31, 12, (short)0x0690),
	CODE32(32, 12, (short)0x06a0),
	CODE33(33, 12, (short)0x06b0),
	CODE34(34, 12, (short)0x0d20),
	CODE35(35, 12, (short)0x0d30),
	CODE36(36, 12, (short)0x0d40),
	CODE37(37, 12, (short)0x0d50),
	CODE38(38, 12, (short)0x0d60),
	CODE39(39, 12, (short)0x0d70),
	CODE40(40, 12, (short)0x06c0),
	CODE41(41, 12, (short)0x06d0),
	CODE42(42, 12, (short)0x0da0),
	CODE43(43, 12, (short)0x0db0),
	CODE44(44, 12, (short)0x0540),
	CODE45(45, 12, (short)0x0550),
	CODE46(46, 12, (short)0x0560),
	CODE47(47, 12, (short)0x0570),
	CODE48(48, 12, (short)0x0640),
	CODE49(49, 12, (short)0x0650),		   
	CODE50(50, 12, (short)0x0520),
	CODE51(51, 12, (short)0x0530),
	CODE52(52, 12, (short)0x0240),
	CODE53(53, 12, (short)0x0370),
	CODE54(54, 12, (short)0x0380),
	CODE55(55, 12, (short)0x0270),
	CODE56(56, 12, (short)0x0280),
	CODE57(57, 12, (short)0x0580),
	CODE58(58, 12, (short)0x0590),
	CODE59(59, 12, (short)0x02b0),
	CODE60(60, 12, (short)0x02c0),
	CODE61(61, 12, (short)0x05a0),
	CODE62(62, 12, (short)0x0660),
	CODE63(63, 12, (short)0x0670),
	// Makeup codes
	CODE64(64, 10, (short)0x03c0),
	CODE128(128, 12, (short)0x0c80),
	CODE192(192, 12, (short)0x0c90),
	CODE256(256, 12, (short)0x05b0),
	CODE320(320, 12, (short)0x0330),
	CODE384(384, 12, (short)0x0340),
	CODE448(448, 12, (short)0x0350),
	CODE512(512, 13, (short)0x0360),
	CODE576(576, 13, (short)0x0368),
	CODE640(640, 13, (short)0x0250),
	CODE704(704, 13, (short)0x0258),
	CODE768(768, 13, (short)0x0260),
	CODE832(832, 13, (short)0x0268),
	CODE896(896, 13, (short)0x0390),
	CODE960(960, 13, (short)0x0398),
	CODE1024(1024, 13, (short)0x03a0),
	CODE1088(1088, 13, (short)0x03a8),
	CODE1152(1152, 13, (short)0x03b0),
	CODE1216(1216, 13, (short)0x03b8),
	CODE1280(1280, 13, (short)0x0290),
	CODE1344(1344, 13, (short)0x0298),
	CODE1408(1408, 13, (short)0x02a0),
	CODE1472(1472, 13, (short)0x02a8),
	CODE1536(1536, 13, (short)0x02d0),
	CODE1600(1600, 13, (short)0x02d8),
	CODE1664(1664, 13, (short)0x0320),
	CODE1728(1728, 13, (short)0x0328),
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
	 
	private T4BlackCode(int runLen, int codeLen, short code) {
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