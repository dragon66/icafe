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
 * PNG PLTE chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 04/26/2013
 */
public class PLTEBuilder extends ChunkBuilder implements Builder<Chunk> {

	private byte[] redMap;
	private byte[] greenMap;
	private byte[] blueMap;
	
	public PLTEBuilder() {
		super(ChunkType.PLTE);		
	}

	public PLTEBuilder redMap(byte[] redMap) {
		this.redMap = redMap;		
		return this;
	}
	
	public PLTEBuilder greenMap(byte[] greenMap) {
		this.greenMap = greenMap;
		return this;
	}
	
	public PLTEBuilder blueMap(byte[] blueMap) {
		this.blueMap = blueMap;
		return this;
	}
	
	@Override
	protected byte[] buildData() {
		// Converts to PNG RGB PLET format
		int mapLen = redMap.length;
		byte[] colorMap = new byte[3*mapLen];
		
		for (int i = mapLen - 1, j = colorMap.length - 1; i >= 0; i--) {			
			colorMap[j--] = blueMap[i];
			colorMap[j--] = greenMap[i];
			colorMap[j--] = redMap[i];
		}

		return colorMap;
	}
}
