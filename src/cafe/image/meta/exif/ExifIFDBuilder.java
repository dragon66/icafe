/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.meta.exif;

import cafe.image.tiff.IFD;
import cafe.util.Builder;

// Builder to create an EXIF SubIFD
public class ExifIFDBuilder implements Builder<IFD> {
	IFD exifIFD;
		
	public ExifIFDBuilder() {
		this.exifIFD = new IFD();
	}
	
	@Override
	public IFD build() {
		return exifIFD;
	}
}