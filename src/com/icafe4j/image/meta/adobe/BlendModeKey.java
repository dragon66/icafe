/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * BlendModeKey.java - Adobe Photoshop layer blend mode keys
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    28Jul2015  Initial creation
 */

package com.icafe4j.image.meta.adobe;

import java.util.HashMap;
import java.util.Map;

public enum BlendModeKey {
	pass("pass through", 0x70617373),
	norm("normal", 0x6e6f726d),
	diss("dissolve", 0x64697373),
	dark("darken", 0x6461726b),
	mul("multiply", 0x6d756c20),
	idiv("color burn", 0x69646976),
	lbrn("linear burn", 0x6c62726e),
	dkCl("darker color", 0x646b436c),
	lite("lighten", 0x6c697465),
	scrn("screen", 0x7363726e),
	div("color dodge", 0x64697620),
	lddg("linear dodge", 0x6c646467),
	lgCl("lighter color", 0x6c67436c),
	over("overlay", 0x6f766572),
	sLit("soft light", 0x734c6974),
	hLit("hard light", 0x684c6974),
	vLit("vivid light", 0x764c6974),
	lLit("linear light", 0x6c4c6974),
	pLit("pin light", 0x704c6974),
	hMix("hard mix", 0x684d6978),
	diff("difference", 0x64696666),
	smud("exclusion", 0x736d7564),
	fsub("subtract", 0x66737562),
	fdiv("divide", 0x66646976),
	hue("hue", 0x68756520),
	sat("saturation", 0x73617420),
	colr("color", 0x636f6c72),
	lum("luminosity", 0x6c756d20),
	
	UNKNOWN("Unknown Blending Mode", 0xFFFFFFFF);
	
	private BlendModeKey(String description, int value) {
		this.description = description;
		this.value = value;
	}
	
	public String getDescription() {
		return description;
	}
	
	public int getValue() {
		return value;
	}
	
	public static BlendModeKey fromInt(int value) {
       	BlendModeKey key = keyMap.get(value);
    	if (key == null)
    		return UNKNOWN;
   		return key;
    }
	
	private static final Map<Integer, BlendModeKey> keyMap = new HashMap<Integer, BlendModeKey>();
    
	static
    {
      for(BlendModeKey key : values()) {
           keyMap.put(key.getValue(), key);
      }
    }
	
 	private final String description;
	private final int value;
}
