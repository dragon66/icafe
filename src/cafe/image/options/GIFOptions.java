/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * GIFOptions.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    18Aug2015  Moved similar fields and methods to GIFFrame class
 * WY    21Dec2014  Added similar fields and methods to GIFFrame class  
 */

package cafe.image.options;

import cafe.image.ImageType;

public class GIFOptions extends ImageOptions {
	public ImageType getImageType() {
		return ImageType.GIF;
	}	
}