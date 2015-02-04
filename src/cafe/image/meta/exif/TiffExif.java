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
 * TiffExif.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    03Feb2015  Initial creation
 */

package cafe.image.meta.exif;

import java.io.IOException;
import java.io.OutputStream;

import cafe.image.tiff.IFD;

public class TiffExif extends Exif {

	public TiffExif() {
		
	}
	
	public TiffExif(IFD imageIFD) {
		super(imageIFD);
	}
	
	/** 
	 * Write the EXIF data to the OutputStream
	 * 
	 * @param os OutputStream
	 * @throws Exception 
	 */
	@Override
	public void write(OutputStream os) throws IOException {
		
	}
}