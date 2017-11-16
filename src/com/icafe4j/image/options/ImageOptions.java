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
 * Change History - most recent changes go on top of previous changes
 *
 * ImageOptions.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    03Jul2015  Removed generic type from ImageOptions
 * WY    03Jul2015  Removed setNativeMetadata() and getNativeMetadata()
 * WY    09Mar2015  Make ImageOptions generic
 * WY    09Mar2015  Added setNativeMetadata() and getNativeMetadata()
 */

package com.icafe4j.image.options;

import com.icafe4j.image.ImageType;

public abstract class ImageOptions {
	// The sole interface
	public abstract ImageType getImageType();	
}