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

package com.icafe4j.image.tiff;

import com.icafe4j.string.StringUtils;

/**
 * TIFF SShort type field.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 02/24/2013
 */
public final class SShortField extends AbstractShortField {

	public SShortField(short tag, short[] data) {
		super(tag, FieldType.SSHORT, data);	
	}
	
	public int[] getDataAsLong() {
		//
		int[] temp = new int[data.length];
		
		System.arraycopy(data, 0, temp, 0, data.length);
		
		return temp;
	}
	
	public String getDataAsString() {
		return StringUtils.shortArrayToString(data, 0, 10, false);
	}
}