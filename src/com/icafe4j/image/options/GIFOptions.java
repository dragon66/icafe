/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
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
 * WY    18Aug2015  Removed similar fields and methods as GIFFrame class
 * WY    21Dec2014  Added similar fields and methods as GIFFrame class  
 */

package com.icafe4j.image.options;

import com.icafe4j.image.ImageType;

public class GIFOptions extends ImageOptions {
	public ImageType getImageType() {
		return ImageType.GIF;
	}	
}