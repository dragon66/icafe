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

package com.icafe4j.image.jpeg;

import com.icafe4j.util.Builder;

/**
 * Base builder for JPEG segments.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/11/2013
 */
public abstract class SegmentBuilder implements Builder<Segment> {
	//
	private final Marker marker;
	
	public SegmentBuilder(Marker marker) {
		this.marker = marker;
	}
		
	public final Segment build() {
		byte[] data = buildData();
		
		return new Segment(marker, data.length + 2, data);
	}
	
	protected abstract byte[] buildData();
}