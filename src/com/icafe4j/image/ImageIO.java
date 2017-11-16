/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
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
 * WY    22Jan2015  Revised read(InputStream) to leave the stream open
 * WY    08Jan2015  Added getReader(PushbackInputStream)
 * WY    22Sep2014  Added read() to detect image type and read image
 * WY    24Sep2014  Added write() to write Image
 */

package com.icafe4j.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.icafe4j.image.reader.ImageReader;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.image.writer.ImageWriter;
import com.icafe4j.io.PeekHeadInputStream;
import com.icafe4j.io.RandomAccessInputStream;

public final class ImageIO {
	// Image header magic number length
	// We may need to bump this to 8 later
	public static final int IMAGE_MAGIC_NUMBER_LEN = 4; 
	
	/**
	 * ImageReader factory
	 * 
	 * @param imgType image type enum defined by {@link ImageType}
	 * @return a ImageReader for image type imgType or null if not found. 
	 */
	public static ImageReader getReader(ImageType imgType) {
		return imgType.getReader();
	}
	
	/**
	 * Creates an ImageReader for the image specified by the PeekHeadInputStream.
	 * The PeekHeadInputStream will be used to figure out image type and create
	 * corresponding ImageReader. The same PeekHeadInputStream is supposed to be
	 * used by the ImageReader to read the image.
	 * <p>
	 * Note: The reason we are using a PeekHeadInputStream is that image type
	 * probing will eat some bytes of the input stream. After the image type
	 * probing, we will have to be able to access the bytes previous read. We could
	 * have used a RandomAccessInputStream interface, but not all image types
	 * require random access while reading. In those cases, using a RandomAccessInputStream
	 * will degrade performance as well as require more memory. This is especially
	 * true when file cache based RandomAccessInputStream implementation is used.  
	 *  
	 * @param peekHeadInputStream A PeekHeadInputStream wrapper for the image input stream
	 * @return An ImageReader instance for the input image or null if none exists
	 */	
	public static ImageReader getReader(PeekHeadInputStream peekHeadInputStream) {
		ImageType imageType = ImageType.UNKNOWN;
		
		try {
			imageType = IMGUtils.guessImageType(peekHeadInputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(imageType != ImageType.UNKNOWN)
			return getReader(imageType);
		
		return null;			
	}
	
	/**
	 * ImageWriter factory
	 * 
	 * @param imgType image type enum defined by {@link ImageType}
	 * @return a ImageWriter for image type imageType or null if not found. 
	 */
	public static ImageWriter getWriter(ImageType imgType) {
		return imgType.getWriter();
	}
	
	/**
	 * @param file input image File
	 * @return BufferedImage
	 * @throws Exception
	 */
	public static BufferedImage read(File file) throws Exception {
		FileInputStream fin = new FileInputStream(file);
		BufferedImage bi = read(fin);
		// Release resources
		fin.close();
		
		return bi;
	}
	
	/**
	 * Read the image or the first frame of the image as a BufferedImage
	 * from the InputStream for the image.
	 * 
	 * @param is InputStream for the image
	 * @return BufferedImage or null
	 * @throws Exception
	 */
	public static BufferedImage read(InputStream is) throws Exception {
		ImageType imageType = null;
		boolean closeStream = false;
		// 4 byte as image magic number
		if(is instanceof RandomAccessInputStream) {
			imageType = IMGUtils.guessImageType((RandomAccessInputStream)is);
		} else {
			is = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN); 
			imageType = IMGUtils.guessImageType((PeekHeadInputStream)is);
			closeStream = true;
		}		
		BufferedImage bi = null;		
		if(imageType != ImageType.UNKNOWN) {
			bi = getReader(imageType).read(is);
		}
		// Close the PeekHeadInputStream we created internally
		if(closeStream) ((PeekHeadInputStream)is).shallowClose();		
		
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
		write(img, os, imageType, ImageParam.DEFAULT_IMAGE_PARAM);
	}
	
	public static void write(BufferedImage img, OutputStream os, ImageType imageType, ImageParam imageParam) throws Exception {
		ImageWriter imageWriter = getWriter(imageType);
		if(imageWriter != null) {
			imageWriter.setImageParam(imageParam);
			imageWriter.write(img, os);
		}		
	}
	
	private ImageIO() {}
}
