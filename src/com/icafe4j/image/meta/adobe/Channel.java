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
 * Channel.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    27Jul2015  initial creation
 */

package com.icafe4j.image.meta.adobe;

public class Channel {
	private int id;
	private int dataLen;
	
	private static final int RED = 0;
	private static final int GREEN = 1;
	private static final int BLUE = 2;
	
	private static final int TRANSPARENCY_MASK = -1;
	private static final int USER_SUPPLIED_LAYER_MASK = -2;
	private static final int REAL_USER_SUPPLIED_LAYER_MASK = -3;
		
	public Channel(int id, int len) {
		this.id = id;
		this.dataLen = len;
	}
	
	public int getDataLen() {
		return dataLen;
	}
	
	public int getID() {
		return id;
	}
	
	public String getType() {
		switch(id) {
			case RED:
				return "Red channel";
			case GREEN:
				return "Green channel";
			case BLUE:
				return "Blue channel";
			case TRANSPARENCY_MASK:
				return "Transparency mask";
			case USER_SUPPLIED_LAYER_MASK:
				return "User supplied layer mask";
			case REAL_USER_SUPPLIED_LAYER_MASK:
				return "real user supplied layer mask (when both a user mask and a vector mask are present)";
			default:
				return "Unknown channel (value " + id + ")";
		}
	}
}
