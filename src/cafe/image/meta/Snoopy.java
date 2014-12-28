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
 * Snoopy.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    27Dec2014  Initial creation to show image meta data
 */

package cafe.image.meta;

import java.io.InputStream;
import java.io.PushbackInputStream;

import cafe.image.ImageType;
import cafe.image.jpeg.JPEGTweaker;
import cafe.image.png.PNGTweaker;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.util.IMGUtils;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.RandomAccessInputStream;

/**
 * Utility class to show image meta data.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/27/2014
 */
public class Snoopy {
	public static void snoop(InputStream is) throws Exception {
		// 4 byte as image magic number
		PushbackInputStream pushBackStream = new PushbackInputStream(is, 4); 
		ImageType imageType = IMGUtils.guessImageType(pushBackStream);
		
		switch(imageType) {
			case JPG:
				JPEGTweaker.snoop(pushBackStream);
				break;
			case PNG:
				PNGTweaker.snoop(pushBackStream);
				break;
			case TIFF:
				RandomAccessInputStream rin = new FileCacheRandomAccessInputStream(pushBackStream);
				TIFFTweaker.snoop(rin);
				rin.close();
				break;
			case GIF:
			default:
				System.out.println("snooping is not implemented for image type: " + imageType);
		}
		
		pushBackStream.close();
	}
}
