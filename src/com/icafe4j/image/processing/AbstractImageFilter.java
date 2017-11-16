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

package com.icafe4j.image.processing;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
/**
 * Abstract image filter 
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 09/17/2012
 */
public abstract class AbstractImageFilter implements BufferedImageOp {

	public Rectangle2D getBounds2D(BufferedImage src) {
		// TODO Auto-generated method stub
		return null;
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src,
			ColorModel destCM) {
		// TODO Auto-generated method stub
		return null;
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		// TODO Auto-generated method stub
		return null;
	}

	public RenderingHints getRenderingHints() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public abstract BufferedImage filter(BufferedImage src, BufferedImage dest);

}
