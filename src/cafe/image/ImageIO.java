/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * ImageIO.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    22Sep2014  Added read() to detect image type and read image
 */

package cafe.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PushbackInputStream;

import cafe.image.core.ImageType;
import cafe.image.reader.ImageReader;
import cafe.image.writer.ImageWriter;
import cafe.image.util.IMGUtils;

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
	
	/**
	 * @param file input image File
	 * @return BufferedImage
	 * @throws Exception
	 */
	public static BufferedImage read(File file) throws Exception {
		FileInputStream fi = new FileInputStream(file);
		BufferedImage bi = read(fi);
		fi.close();
		
		return bi;
	}
	
	/**
	 * @param is InputStream for the image
	 * @return BufferedImage or null
	 * @throws Exception
	 */
	public static BufferedImage read(InputStream is) throws Exception {
		// 4 byte as image magic number
		PushbackInputStream pushBackStream = new PushbackInputStream(is, 4); 
		ImageType imageType = IMGUtils.guessImageType(pushBackStream);
		
		if(imageType != null) {
			return getReader(imageType).read(pushBackStream);
		}
		
		return null;
	}
	
	/**
	 * @param path input image path
	 * @return BufferedImage
	 * @throws Exception
	 */	
	public static BufferedImage read(String path) throws Exception {
		FileInputStream is = new FileInputStream(path);
		BufferedImage bi =  read(is);
		is.close();
		
		return bi;
	}
	
	private ImageIO() {}
}
