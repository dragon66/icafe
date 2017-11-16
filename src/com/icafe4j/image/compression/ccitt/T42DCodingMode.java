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

public enum T42DCodingMode {
	//
	PASS(T42DCode.P),
	HHORIZONTAL(T42DCode.H),
	VERTICAL0(T42DCode.V0),
	VERTICAL_RIGHT1(T42DCode.VR1),
	VERTICAL_RIGHT2(T42DCode.VR2),
	VERTICAL_RIGHT3(T42DCode.VR3),
	VERTICAL_LEFT1(T42DCode.VL1),
	VERTICAL_LEFT2(T42DCode.VL2),
	VERTICAL_LEFT3(T42DCode.VL3);
	
	private T42DCodingMode(T42DCode code) {
		this.code = code;
	}
	
	public T42DCode getCode() {
		return code;
	}
	
	private final T42DCode code;
}