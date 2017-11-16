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

public class QTable implements Comparable<QTable> {
	//
	private int precision;
	private int id;
	private int[] data;
	
	public QTable(int precision, int id, int[] data) {
		if(precision != 0 && precision != 1) 
			throw new IllegalArgumentException("Invalid precision value: " + precision);
		this.precision = precision;
		this.id = id;
		this.data = data;
	}
	
	public int getPrecision() {
		return precision; 
	}
	
	public int getID() {
		return id;
	}
	
	public int[] getData() {
		return data.clone();
	}

	public int compareTo(QTable that) {
		return this.id - that.id;
	}
}
