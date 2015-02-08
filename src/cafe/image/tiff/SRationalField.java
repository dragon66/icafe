/**
 * Copyright (c) 2014 by Wen Yu.
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
 * TIFF SRational type field.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 02/24/2013
 */
public final class SRationalField extends AbstractRationalField {

	public SRationalField(short tag, int[] data) {
		super(tag, FieldType.SRATIONAL, data);
	}
	
	public String getDataAsString() {
		return StringUtils.rationalArrayToString(data, true);
	}
}