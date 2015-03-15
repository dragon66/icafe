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

public class HTable implements Comparable<HTable> {
	//
	public static final int DC_COMPONENT = 0;
	public static final int AC_COMPONENT = 1;
	
	private int component_class; // DC or AC
	private int destination_id; // Table #
	private byte[] bits;
	private byte[] values;
	
	public HTable(int component_class, int destination_id, byte[] bits, byte[] values) {
		this.component_class = component_class;
		this.destination_id = destination_id;
		this.bits = bits;
		this.values = values;
	}
	
	public int getComponentClass() {
		return component_class; 
	}
	
	public int getDestinationID() {
		return destination_id;
	}
	
	public byte[] getBits() {
		return bits;
	}
	
	public byte[] getValues() {
		return values;
	}

	@Override
	public int compareTo(HTable that) {
		return this.destination_id - that.destination_id;
	}
}