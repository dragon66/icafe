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

/**
 * JPEG COM segment builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/11/2013
 */

public class COMBuilder extends SegmentBuilder {

	private String comment;
	
	public COMBuilder() {
		super(Marker.COM);	
	}
	
	public COMBuilder comment(String comment) {
		this.comment = comment;
		return this;
	}
	
	@Override
	protected byte[] buildData() {
		return comment.getBytes();
	}
}
