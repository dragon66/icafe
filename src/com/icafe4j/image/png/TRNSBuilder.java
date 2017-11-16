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
 * PNG tRNS chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/03/2013
 */
public class TRNSBuilder extends ChunkBuilder implements Builder<Chunk> {

	private int colorType = 0;
	private byte[] alpha;
	
	public TRNSBuilder(int colorType) {
		super(ChunkType.TRNS);
		this.colorType = colorType;
	}
	
	public TRNSBuilder alpha(byte[] alpha) {
		this.alpha = alpha;
		return this;
	}

	@Override
	protected byte[] buildData() {
		switch(colorType)
		{			
			case 0:	
			case 2:				
			case 3:			
				break;
			case 4:
			case 6:		
			default:
				throw new IllegalArgumentException("Invalid color type: " + colorType);
		}
		
		return alpha;
	}
}
