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

import java.io.UnsupportedEncodingException;

import cafe.util.ArrayUtils;
import cafe.util.Builder;

/**
 * PNG iCCP chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/23/2014
 */
public class ICCPBuilder extends ChunkBuilder implements Builder<Chunk> {
	
	private String profileName;
	private byte[] profileData;

	public ICCPBuilder() {
		super(ChunkType.ICCP);
	}
	
	public ICCPBuilder data(byte[] data) {
		this.profileData = data;		
		return this;
	}
	
	public ICCPBuilder name(String name) {
		this.profileName = name.trim() + '\0';
		return this;		
	}

	@Override
	protected byte[] buildData() {
		byte[] nameBytes = null;
		
		try {
			nameBytes = profileName.getBytes("iso-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return ArrayUtils.concat(nameBytes, profileData);
	}	
}