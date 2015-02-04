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
 * JpegExif.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    03Feb2015  Initial creation
 */

package cafe.image.meta.exif;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cafe.image.jpeg.Marker;
import cafe.image.tiff.ASCIIField;
import cafe.image.tiff.IFD;
import cafe.image.tiff.LongField;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffTag;
import cafe.io.IOUtils;
import cafe.io.MemoryCacheRandomAccessOutputStream;
import cafe.io.RandomAccessOutputStream;
import cafe.io.WriteStrategyMM;

public class JpegExif extends Exif {

	public JpegExif() {
	
	}
	
	public JpegExif(byte[] data) {
		super(data);
	}
	
	private void createImageIFD() {
		// Create Image IFD (IFD0)
		imageIFD = new IFD();
		TiffField<?> tiffField = new ASCIIField(TiffTag.IMAGE_DESCRIPTION.getValue(), "Exif created by JPEGTweaker\0");
		imageIFD.addField(tiffField);
		String softWare = "JPEGTweaker 1.0\0";
		tiffField = new ASCIIField(TiffTag.SOFTWARE.getValue(), softWare);
		imageIFD.addField(tiffField);
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		tiffField = new ASCIIField(TiffTag.DATETIME.getValue(), formatter.format(new Date()) + '\0');
		imageIFD.addField(tiffField);		
	}
	
	/** 
	 * Write the EXIF data to the OutputStream
	 * 
	 * @param os OutputStream
	 * @throws Exception 
	 */
	@Override
	public void write(OutputStream os) throws IOException {
		// Wraps output stream with a RandomAccessOutputStream
		RandomAccessOutputStream randOS = new MemoryCacheRandomAccessOutputStream(os);
		// Write JPEG the EXIF data
		// Writes APP1 marker
		IOUtils.writeShortMM(os, Marker.APP1.getValue());		
		// TIFF structure starts here
		short endian = IOUtils.BIG_ENDIAN;
		short tiffID = 0x2a; //'*'
		randOS.setWriteStrategy(WriteStrategyMM.getInstance());
		randOS.writeShort(endian);
		randOS.writeShort(tiffID);
		// First IFD offset relative to TIFF structure
		randOS.seek(0x04);
		randOS.writeInt(firstIFDOffset);
		// Writes IFDs
		randOS.seek(firstIFDOffset);
		if(imageIFD == null) createImageIFD();
		// Attach EXIIF and/or GPS SubIFD to main image IFD
		if(exifSubIFD != null) {
			imageIFD.addField(new LongField(TiffTag.EXIF_SUB_IFD.getValue(), new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.EXIF_SUB_IFD, exifSubIFD);			
		}
		if(gpsSubIFD != null) {
			imageIFD.addField(new LongField(TiffTag.GPS_SUB_IFD.getValue(), new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.GPS_SUB_IFD, gpsSubIFD);
		}
		int offset = imageIFD.write(randOS, firstIFDOffset);
		if(thumbnail != null) {
			imageIFD.setNextIFDOffset(randOS, offset);
			thumbnail.write(randOS, offset);
		}
		// Now it's time to update the segment length
		int length = (int)randOS.getLength();
		// Update segment length
		IOUtils.writeShortMM(os, length + 8);
		// Add EXIF identifier with trailing bytes [0x00,0x00].
		byte[] exif = {0x45, 0x78, 0x69, 0x66, 0x00, 0x00};
		IOUtils.write(os, exif);
		// Dump randOS to normal output stream and we are done!
		randOS.seek(0);
		randOS.writeToStream(length);
		randOS.close();
	}
}