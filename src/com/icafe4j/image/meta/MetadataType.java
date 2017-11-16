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

package com.icafe4j.image.meta;

/**
 * Image metadata type enum.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/12/2015
 */
public enum MetadataType {
	EXIF, // EXIF
	IPTC, // IPTC
	ICC_PROFILE, // ICC Profile
	XMP, // Adobe XMP
	PHOTOSHOP_IRB, // PHOTOSHOP Image Resource Block
	PHOTOSHOP_DDB, // PHOTOSHOP Document Data Block
	COMMENT, // General comment
	IMAGE, // Image specific information
	JPG_JFIF, // JPEG APP0 (JFIF)
	JPG_DUCKY, // JPEG APP12 (DUCKY)
	JPG_ADOBE, // JPEG APP14 (ADOBE)
	PNG_TEXTUAL, // PNG textual information
	PNG_TIME; // PNG tIME (last modified time) chunk
}