/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.tiff;

import cafe.string.StringUtils;

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
		
		for(int i=0; i<data.length; i++) {
			temp[i] = data[i];
		}
		
		return temp;
	}
	
	public String getDataAsString() {
		return StringUtils.shortArrayToString(data, 0, 10, false);
	}
}