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
 * WY    24Sep2014  Added write() to write Image
 */

package cafe.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

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
		return read(new FileInputStream(file));
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
		BufferedImage bi = null;		
		
		if(imageType != ImageType.UNKNOWN) {
			bi = getReader(imageType).read(pushBackStream);
		}
		
		pushBackStream.close();
		
		return bi;
	}
	
	/**
	 * @param path input image path
	 * @return BufferedImage
	 * @throws Exception
	 */	
	public static BufferedImage read(String path) throws Exception {
		return read(new File(path));
	}
	
	public static void write(BufferedImage img, OutputStream os, ImageType imageType) throws Exception {
		write(img, os, imageType, ImageMeta.DEFAULT_IMAGE_META);
	}
	
	public static void write(BufferedImage img, OutputStream os, ImageType imageType, ImageMeta imageMeta) throws Exception {
		ImageWriter imageWriter = getWriter(imageType);
		if(imageWriter != null) {
			imageWriter.setImageMeta(imageMeta);
			imageWriter.write(img, os);
		}		
	}
	
	private ImageIO() {}
}
