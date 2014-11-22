/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image.tiff;

import java.awt.image.BufferedImage;
import cafe.image.core.ImageMeta;

/** 
 * Wrapper for TIFF frame
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 11/21/2014
 */
public class TIFFFrame {
	// Frame parameters
	private BufferedImage frame;
	private ImageMeta frameMeta;
	
	public TIFFFrame(BufferedImage frame) {
		this(frame, ImageMeta.DEFAULT_IMAGE_META);
	}
	
	public TIFFFrame(BufferedImage frame, ImageMeta frameMeta) {
		this.frame = frame;
		this.frameMeta = frameMeta;
	}
	public BufferedImage getFrame() {
		return frame;
	}
	
	public ImageMeta getFrameMeta() {
		return frameMeta;
	}
	
	public void setFrameMeta(ImageMeta frameMeta) {
		this.frameMeta = frameMeta;
	}
}