/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.jpeg;

public class QTable implements Comparable<QTable> {
	//
	private int precision;
	private int index;
	private short[] table;
	
	public QTable(int precision, int index, short[] table) {
		if(precision != 0 && precision != 1) 
			throw new IllegalArgumentException("Invalid precision value: " + precision);
		this.precision = precision;
		this.index = index;
		this.table = table;
	}
	
	public int getPrecision() {
		return precision; 
	}
	
	public int getIndex() {
		return index;
	}
	
	public short[] getTable() {
		return table;
	}

	@Override
	public int compareTo(QTable that) {
		return this.index - that.index;
	}
}
