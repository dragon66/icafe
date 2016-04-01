package com.icafe4j.test;

import java.awt.color.ICC_Profile;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.icafe4j.image.jpeg.JPEGTweaker;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.exif.ExifTag;
import com.icafe4j.image.meta.icc.ICCProfile;
import com.icafe4j.image.meta.jpeg.JpegExif;
import com.icafe4j.image.tiff.FieldType;
import com.icafe4j.image.util.IMGUtils;

public class TestJPEGTweaker extends TestBase {
	public static void main(String[] args) throws Exception {
		new TestJPEGTweaker().test(args);
	}
	
	public void test(String ... args) throws Exception {
		FileInputStream fin = new FileInputStream(args[0]);
		byte[] iccpBytes = JPEGTweaker.extractICCProfile(fin);
		ICCProfile.showProfile(iccpBytes);
		fin.close();
		fin = new FileInputStream(args[1]);
		FileOutputStream fout = new FileOutputStream("icc_profile_inserted.jpg");
		ICC_Profile icc_profile = IMGUtils.getICCProfile("/resources/CMYK Profiles/USWebCoatedSWOP.icc");
		JPEGTweaker.insertICCProfile(fin, fout, icc_profile);
		fin.close();
		fout.close();
		fin = new FileInputStream(args[2]);
		JPEGTweaker.extractThumbnails(fin, "thumbnail");
		fin.close();
		fin = new FileInputStream(args[2]);
		fout = new FileOutputStream("metadata_removed.jpg");
		JPEGTweaker.removeMetadata(fin, fout, MetadataType.XMP, MetadataType.EXIF, MetadataType.IPTC, MetadataType.ICC_PROFILE);
		fin.close();
		fout.close();
		fin = new FileInputStream(args[0]);
		fout = new FileOutputStream("exif_inserted.jpg");
		JPEGTweaker.insertExif(fin, fout, populateExif(), true);
		fin.close();
		fout.close();
	}
	
	// This method is for testing only
	private static Exif populateExif() throws Exception {
		// Create an EXIF wrapper
		Exif exif = new JpegExif();		
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		exif.addExifField(ExifTag.EXPOSURE_TIME, FieldType.RATIONAL, new int[] {10, 600});
		exif.addExifField(ExifTag.FNUMBER, FieldType.RATIONAL, new int[] {49, 10});
		exif.addExifField(ExifTag.ISO_SPEED_RATINGS, FieldType.SHORT, new short[]{273});
		//All four bytes should be interpreted as ASCII values - represents [0220]
		exif.addExifField(ExifTag.EXIF_VERSION, FieldType.UNDEFINED, new byte[]{48, 50, 50, 48});
		exif.addExifField(ExifTag.DATE_TIME_ORIGINAL, FieldType.ASCII, formatter.format(new Date()));
		exif.addExifField(ExifTag.DATE_TIME_DIGITIZED, FieldType.ASCII, formatter.format(new Date()));
		exif.addExifField(ExifTag.FOCAL_LENGTH, FieldType.RATIONAL, new int[] {240, 10});
		// Insert ThumbNailIFD
		// Since we don't provide thumbnail image, it will be created later from the input stream
		exif.setThumbnail(null);
			
		return exif;
	}
}