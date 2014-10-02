package cafe.test;

import java.awt.color.ICC_Profile;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cafe.image.jpeg.JPEGTweaker;
import cafe.image.meta.exif.Exif;
import cafe.image.meta.exif.ExifTag;
import cafe.image.tiff.ASCIIField;
import cafe.image.tiff.IFD;
import cafe.image.tiff.RationalField;
import cafe.image.tiff.ShortField;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.UndefinedField;
import cafe.image.util.IMGUtils;

public class TestJPEGTweaker {
	public static void main(String[] args) throws Exception {
		FileInputStream fin = new FileInputStream(args[0]);
		JPEGTweaker.showICCProfile(fin);
		fin.close();
		fin = new FileInputStream(args[1]);
		FileOutputStream fout = new FileOutputStream("icc_profile_inserted.jpg");
		ICC_Profile icc_profile = IMGUtils.getICCProfile("/lib/CMYK Profiles/USWebCoatedSWOP.icc");
		JPEGTweaker.insertICCProfile(fin, fout, icc_profile);
		fin.close();
		fout.close();
		fin = new FileInputStream(args[2]);
		JPEGTweaker.extractThumbnail(fin, "thumbnail");
		fin.close();
		fin = new FileInputStream(args[2]);
		fout = new FileOutputStream("exif_removed.jpg");
		JPEGTweaker.removeExif(fin, fout);
		fin.close();
		fout.close();
	}
	
	// This method is for testing only
	@SuppressWarnings("unused")
	private static Exif populateExif() throws Exception {
		// Create an EXIF wrapper
		Exif exif = new Exif(Exif.EXIF_FLAVOR_JPG);		
		// ExifSubIFD
		IFD exifSubIFD = new IFD();		
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		TiffField<?> tiffField = new RationalField(ExifTag.EXPOSURE_TIME.getValue(), new int[] {10, 600});
		exifSubIFD.addField(tiffField);
		tiffField = new RationalField(ExifTag.FNUMBER.getValue(), new int[] {49, 10});
		exifSubIFD.addField(tiffField);
		tiffField = new ShortField(ExifTag.ISO_SPEED_RATINGS.getValue(), new short[]{273});
		exifSubIFD.addField(tiffField);
		//All four bytes should be interpreted as ASCII values - represents [0220]
		tiffField = new UndefinedField(ExifTag.EXIF_VERSION.getValue(), new byte[]{48, 50, 50, 48});
		exifSubIFD.addField(tiffField);
		tiffField = new ASCIIField(ExifTag.DATE_TIME_ORIGINAL.getValue(), formatter.format(new Date()) + '\0');
		exifSubIFD.addField(tiffField);
		tiffField = new ASCIIField(ExifTag.DATE_TIME_DIGITIZED.getValue(), formatter.format(new Date()) + '\0');
		exifSubIFD.addField(tiffField);
		tiffField = new RationalField(ExifTag.FOCAL_LENGTH.getValue(), new int[] {240, 10});
		exifSubIFD.addField(tiffField);
		// Insert ExifSubIFD
		exif.addExif(exifSubIFD);
		// Insert ThumbNailIFD
		// Since we don't provide thumbnail image, it will be created later from the input stream
		exif.addThumbnail(null); 
			
		return exif;
	}
}