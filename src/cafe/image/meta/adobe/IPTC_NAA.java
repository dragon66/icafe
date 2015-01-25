/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * IPTC_NAA.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    24Jan2015  initial creation
 */

package cafe.image.meta.adobe;

import cafe.image.meta.iptc.IPTCReader;

public class IPTC_NAA extends _8BIM {

	public IPTC_NAA(String name, int size, byte[] data) {
		super(ImageResourceID.IPTC_NAA.getValue(), name, size, data);
	}
	
	public void show() {
		super.show();
		
		/* Structure of an IPTC data set
		   [Record name]    [size]   [description]
		   ---------------------------------------
		   (Tag marker)     1 byte   this must be 0x1c
		   (Record number)  1 byte   always 2 for 2:xx datasets
		   (Dataset number) 1 byte   this is what we call a "tag"
		   (Size specifier) 2 bytes  data length (< 32768 bytes) or length of ...
		   (Size specifier)  ...     data length (> 32767 bytes only)
		   (Data)            ...     (its length is specified before)
		 */
		new IPTCReader(getData()).showMetadata();
	}
}
