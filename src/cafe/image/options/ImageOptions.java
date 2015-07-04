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
 * ImageOptions.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    09Mar2015  Make ImageOptions generic
 * WY    09Mar2015  Added setNativeMetadata() and getNativeMetadata()
 */

package cafe.image.options;

import cafe.image.ImageType;

public abstract class ImageOptions {
	// The sole interface
	public abstract ImageType getImageType();	
}