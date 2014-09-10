/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image;

import cafe.image.core.ImageType;
import cafe.image.reader.ImageReader;
import cafe.image.writer.ImageWriter;

public final class ImageIO {
	
	/**
	 * ImageReader factory
	 * 
	 * @param imgType image type enum defined by {@link ImageType}
	 * @return a ImageReader for image type imgType or null if not found. 
	 */
	public static ImageReader getReader(ImageType imgType)
	{
		return imgType.getReader();
	}	
	
	/**
	 * ImageWriter factory
	 * 
	 * @param imgType image type enum defined by {@link ImageType}
	 * @return a ImageWriter for image type imageType or null if not found. 
	 */
	public static ImageWriter getWriter(ImageType imgType)
	{
		return imgType.getWriter();
	}
	
	private ImageIO() {}
}
