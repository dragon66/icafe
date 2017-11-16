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

package com.icafe4j.image.png;

import com.icafe4j.util.Builder;

/**
 * PNG IEND chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/29/2013
 */
public class IENDBuilder extends ChunkBuilder implements Builder<Chunk> {

	public IENDBuilder() {
		super(ChunkType.IEND);	
	}

	@Override
	protected byte[] buildData() {
		return new byte[0];
	}
}
