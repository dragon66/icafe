package com.icafe4j.image.meta;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.icafe4j.image.tiff.ASCIIField;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffTag;

public class MetadataHelper {
	
	public static IFD createImageIFD() {
		// Create Image IFD (IFD0)
		IFD imageIFD = new IFD();
		TiffField<?> tiffField = new ASCIIField(TiffTag.IMAGE_DESCRIPTION.getValue(), "Exif created by JPEGTweaker");
		imageIFD.addField(tiffField);
		String softWare = "JPEGTweaker 1.0";
		tiffField = new ASCIIField(TiffTag.SOFTWARE.getValue(), softWare);
		imageIFD.addField(tiffField);
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		tiffField = new ASCIIField(TiffTag.DATETIME.getValue(), formatter.format(new Date()));
		imageIFD.addField(tiffField);
		
		return imageIFD;
	}
}
