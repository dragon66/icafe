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

package com.icafe4j.image.meta.png;

import java.io.IOException;
import java.io.OutputStream;

import com.icafe4j.image.meta.xmp.XMP;

public class PngXMP extends XMP {

	public PngXMP(String xmp) {
		super(xmp);
		// TODO Auto-generated constructor stub
	}

	public void write(OutputStream os) throws IOException {
		// TODO: add code to write XMP to PNG image
		throw new UnsupportedOperationException("PngXMP.write() is not implemented.");
	}
}
