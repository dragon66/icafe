package com.icafe4j.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import com.icafe4j.image.ImageIO;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.jpeg.JPEGTweaker;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.Thumbnail;
import com.icafe4j.image.meta.adobe.IPTC_NAA;
import com.icafe4j.image.meta.adobe._8BIM;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.exif.ExifTag;
import com.icafe4j.image.meta.image.Comments;
import com.icafe4j.image.meta.image.ImageMetadata;
import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.image.meta.iptc.IPTCApplicationTag;
import com.icafe4j.image.meta.iptc.IPTCDataSet;
import com.icafe4j.image.meta.jpeg.JFIF;
import com.icafe4j.image.meta.jpeg.JpegExif;
import com.icafe4j.image.meta.jpeg.JpegXMP;
import com.icafe4j.image.meta.tiff.TiffExif;
import com.icafe4j.image.meta.xmp.XMP;
import com.icafe4j.image.tiff.FieldType;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.image.writer.ImageWriter;
import com.icafe4j.string.StringUtils;
import com.icafe4j.string.XMLUtils;
import com.icafe4j.util.FileUtils;

public class TestMetadata extends TestBase {

	public static void main(String[] args) throws Exception {
		new TestMetadata().test(args);
	}
	
	public void test(String ... args) throws Exception {
		Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(args[0]);
		logger.info("Start of metadata information:");
		logger.info("Total number of metadata entries: {}", metadataMap.size());
		int i = 0;
		for(Map.Entry<MetadataType, Metadata> entry : metadataMap.entrySet()) {
			//
			logger.info("Metadata entry {} - {}", i, entry.getKey());
			
			Iterator<MetadataEntry> iterator = entry.getValue().iterator();
			
			while(iterator.hasNext()) {
				MetadataEntry item = iterator.next();
				printMetadata(item, "", "     ");
			}
			
			i++;
			logger.info("-----------------------------------------");
		}
		logger.info("End of metadata information.");
	
		FileInputStream fin = null;
		FileOutputStream fout = null;
		
		if(metadataMap.get(MetadataType.XMP) != null) {
			XMP xmp = (XMP)metadataMap.get(MetadataType.XMP);
			fin = new FileInputStream("images/1.jpg");
			fout = new FileOutputStream("1-xmp-inserted.jpg");
			XMP jpegXmp = null;
			Document xmpDoc = xmp.getMergedDocument();
			jpegXmp = new JpegXMP(XMLUtils.serializeToByteArray(xmpDoc));
			Metadata.insertXMP(fin, fout, jpegXmp);			
			fin.close();
			fout.close();
		}
		
		if(metadataMap.get(MetadataType.IMAGE) != null) {
			ImageMetadata imageMeta = (ImageMetadata)metadataMap.get(MetadataType.IMAGE);
			if(imageMeta.containsThumbnail()) {
				Map<String, Thumbnail> thumbnails = imageMeta.getThumbnails();
				Iterator<Entry<String, Thumbnail>> iter = thumbnails.entrySet().iterator();
				ImageWriter writer = ImageIO.getWriter(ImageType.JPG);
				String outpath = FileUtils.getNameWithoutExtension(new File(args[0]));
				while(iter.hasNext()) {
					Entry<String, Thumbnail> entry = iter.next();
					BufferedImage image = entry.getValue().getAsBufferedImage();
					fout = new FileOutputStream(outpath + "_" + entry.getKey() + "_thumbnail" + ".jpg");
					try {
						writer.write(image, fout);
					} catch (Exception e) {
						throw new IOException("Writing thumbnail failed!");
					} finally {fout.close();}
				}
			}
		}
		
		Metadata.extractThumbnails("images/iptc-envelope.tif", "iptc-envelope");
	
		fin = new FileInputStream("images/iptc-envelope.tif");
		fout = new FileOutputStream("iptc-envelope-iptc-inserted.tif");
			
		Metadata.insertIPTC(fin, fout, createIPTCDataSet(), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/wizard.jpg");
		fout = new FileOutputStream("wizard-iptc-inserted.jpg");
		
		Metadata.insertIPTC(fin, fout, createIPTCDataSet(), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/1.jpg");
		fout = new FileOutputStream("1-irbthumbnail-inserted.jpg");
		
		Metadata.insertIRBThumbnail(fin, fout, createThumbnail("images/1.jpg"));
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/f1.tif");
		fout = new FileOutputStream("f1-irbthumbnail-inserted.tif");
		
		Metadata.insertIRBThumbnail(fin, fout, createThumbnail("images/f1.tif"));
		
		fin.close();
		fout.close();		

		fin = new FileInputStream("images/exif.tif");
		fout = new FileOutputStream("exif-exif-inserted.tif");
		
		Metadata.insertExif(fin, fout, populateExif(TiffExif.class), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/exif.tif");
		fout = new FileOutputStream("exif-exif-iptc-comment-inserted.tif");

		List<Metadata> metaList = new ArrayList<Metadata>();
		metaList.add(populateExif(TiffExif.class));
		metaList.add(createIPTC());
		metaList.add(new Comments(Arrays.asList("Comment1", "Comment2")));
		
		Metadata.insertMetadata(metaList, fin, fout);
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-exif-inserted.jpg");

		Metadata.insertExif(fin, fout, populateExif(JpegExif.class), true);
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-exif-iptc-inserted.jpg");
		
		metaList.clear();
		metaList.add(populateExif(JpegExif.class));
		metaList.add(createIPTC());
		metaList.add(new JFIF(new byte[]{}));
		
		Metadata.insertMetadata(metaList, fin, fout);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-metadata-removed.jpg");
		
		Metadata.removeMetadata(fin, fout, MetadataType.JPG_JFIF, MetadataType.JPG_ADOBE, MetadataType.IPTC, MetadataType.ICC_PROFILE, MetadataType.XMP, MetadataType.EXIF);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-photoshop-iptc-inserted.jpg");
		
		Metadata.insertIRB(fin, fout, createPhotoshopIPTC(), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/table.jpg");
		JPEGTweaker.extractDepthMap(fin, "table");		
		
		fin.close();
		
		fin = new FileInputStream("images/butterfly.png");
		fout = new FileOutputStream("comment-inserted.png");
		
		Metadata.insertComments(fin, fout, Arrays.asList("Comment1", "Comment2"));
		
		fin.close();
		fout.close();
	}
	
	private static List<IPTCDataSet> createIPTCDataSet() {
		List<IPTCDataSet> iptcs = new ArrayList<IPTCDataSet>();
		iptcs.add(new IPTCDataSet(IPTCApplicationTag.COPYRIGHT_NOTICE, "Copyright 2014-2016, yuwen_66@yahoo.com"));
		iptcs.add(new IPTCDataSet(IPTCApplicationTag.CATEGORY, "ICAFE"));
		iptcs.add(new IPTCDataSet(IPTCApplicationTag.KEY_WORDS, "Welcome 'icafe' user!"));
		
		return iptcs;
	}
	
	private static IPTC createIPTC() {
		IPTC iptc = new IPTC();
		iptc.addDataSets(createIPTCDataSet());
		return iptc;
	}
	
	private static List<_8BIM> createPhotoshopIPTC() {
		IPTC_NAA iptc = new IPTC_NAA();
		iptc.addDataSet(new IPTCDataSet(IPTCApplicationTag.COPYRIGHT_NOTICE, "Copyright 2014-2016, yuwen_66@yahoo.com"));
		iptc.addDataSet(new IPTCDataSet(IPTCApplicationTag.KEY_WORDS, "Welcome 'icafe' user!"));
		iptc.addDataSet(new IPTCDataSet(IPTCApplicationTag.CATEGORY, "ICAFE"));
		
		return new ArrayList<_8BIM>(Arrays.asList(iptc));
	}
	
	private static BufferedImage createThumbnail(String filePath) throws IOException {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		BufferedImage thumbnail = IMGUtils.createThumbnail(fin);
		
		fin.close();
		
		return thumbnail;
	}
	
	// This method is for testing only
	private static Exif populateExif(Class<?> exifClass) throws IOException {
		// Create an EXIF wrapper
		Exif exif = exifClass == (TiffExif.class)?new TiffExif() : new JpegExif();
		exif.addImageField(TiffTag.WINDOWS_XP_AUTHOR, FieldType.WINDOWSXP, "Author");
		exif.addImageField(TiffTag.WINDOWS_XP_KEYWORDS, FieldType.WINDOWSXP, "Copyright;Author");
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		exif.addExifField(ExifTag.EXPOSURE_TIME, FieldType.RATIONAL, new int[] {10, 600});
		exif.addExifField(ExifTag.FNUMBER, FieldType.RATIONAL, new int[] {49, 10});
		exif.addExifField(ExifTag.ISO_SPEED_RATINGS, FieldType.SHORT, new short[]{273});
		//All four bytes should be interpreted as ASCII values - represents [0220] - new byte[]{48, 50, 50, 48}
		exif.addExifField(ExifTag.EXIF_VERSION, FieldType.UNDEFINED, "0220".getBytes());
		exif.addExifField(ExifTag.DATE_TIME_ORIGINAL, FieldType.ASCII, formatter.format(new Date()));
		exif.addExifField(ExifTag.DATE_TIME_DIGITIZED, FieldType.ASCII, formatter.format(new Date()));
		exif.addExifField(ExifTag.FOCAL_LENGTH, FieldType.RATIONAL, new int[] {240, 10});		
		// Insert ThumbNailIFD
		// Since we don't provide thumbnail image, it will be created later from the input stream
		exif.setThumbnailRequired(true);
		
		return exif;
	}
	
	private void printMetadata(MetadataEntry entry, String indent, String increment) {
		logger.info(indent + entry.getKey() + (StringUtils.isNullOrEmpty(entry.getValue())? "" : ": " + entry.getValue()));
		if(entry.isMetadataEntryGroup()) {
			indent += increment;
			Collection<MetadataEntry> entries = entry.getMetadataEntries();
			for(MetadataEntry e : entries) {
				printMetadata(e, indent, increment);
			}			
		}
	}
}