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

package com.icafe4j.image.jpeg;

public class HTable implements Comparable<HTable> {
	//
	public static final int DC_CLAZZ = 0;
	public static final int AC_CLAZZ = 1;
	
	private int clazz; // DC or AC
	private int id; // Destination ID (0...3)
	
	private byte[] bits;
	private byte[] values;
	
	public HTable(int clazz, int id, byte[] bits, byte[] values) {
		this.clazz = clazz;
		this.id = id;
		this.bits = bits;
		this.values = values;
	}
	
	public int getClazz() {
		return clazz; 
	}
	
	public int getID() {
		return id;
	}
	
	public byte[] getBits() {
		return bits;
	}
	
	public byte[] getValues() {
		return values;
	}

	public int compareTo(HTable that) {
		return this.id - that.id;
	}
}