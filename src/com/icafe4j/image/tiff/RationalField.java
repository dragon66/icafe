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
 * TIFF Rational type field.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/06/2013
 */
public final class RationalField extends AbstractRationalField {

	public RationalField(short tag, int[] data) {
		super(tag, FieldType.RATIONAL, data);
	}
	
	public String getDataAsString() {
		return StringUtils.rationalArrayToString(data, true);
	}
}