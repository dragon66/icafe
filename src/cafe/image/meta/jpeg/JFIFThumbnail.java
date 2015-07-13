/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * JFIFThumbnail.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    12Jul2015  Initial creation
 */

package cafe.image.meta.jpeg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import cafe.image.meta.Thumbnail;

public class JFIFThumbnail extends Thumbnail {

	public JFIFThumbnail(BufferedImage thumbnail) {
		super(thumbnail);
	}

	@Override
	public void write(OutputStream os) throws IOException {
		BufferedImage thumbnail = getRawImage();
		if(thumbnail == null) throw new IllegalArgumentException("Expected raw data thumbnail does not exist!");
		int[] rgbs = thumbnail.getRGB(0, 0, thumbnail.getWidth(), thumbnail.getHeight(), null, 0, thumbnail.getWidth());
		for(int rgb : rgbs) {
			os.write(rgb >> 24); // Red
			os.write(rgb >> 8); // Green
			os.write(rgb); // Blue
		}
	}
}