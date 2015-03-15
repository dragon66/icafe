/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image;

import java.awt.image.BufferedImage;

/** 
 * Wrapper for an image frame
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 11/21/2014
 */
public class ImageFrame {
	// Frame parameters
	private BufferedImage frame;
	private ImageParam frameParam;
	
	public ImageFrame(BufferedImage frame) {
		this(frame, ImageParam.DEFAULT_IMAGE_PARAM);
	}
	
	public ImageFrame(BufferedImage frame, ImageParam frameParam) {
		this.frame = frame;
		this.frameParam = frameParam;
	}
	public BufferedImage getFrame() {
		return frame;
	}
	
	public ImageParam getFrameParam() {
		return frameParam;
	}
	
	public void setFrameMeta(ImageParam frameParam) {
		this.frameParam = frameParam;
	}
}