/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.png;

import cafe.util.Builder;

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
