package cafe.test;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.image.meta.iptc.IPTCDataSet;
import cafe.image.meta.iptc.IPTCRecord;
import cafe.image.meta.iptc.IPTCApplicationTag;

public class TestMetadata {

	public static void main(String[] args) throws IOException {
		Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(args[0]);
		System.out.println("Start of metadata information:");
		System.out.println("Total number of metadata entries: " + metadataMap.size());
		int i = 0;
		for(Map.Entry<MetadataType, Metadata> entry : metadataMap.entrySet()) {
			System.out.println("Metadata entry " + i + " - " + entry.getKey());
			entry.getValue().showMetadata();
			i++;
			System.out.println("-----------------------------------------");
		}
		System.out.println("End of metadata information.");
		Metadata.extractThumbnails(args[0], "thumbnail");
		
		FileInputStream fin = new FileInputStream("images/iptc.tif");
		FileOutputStream fout = new FileOutputStream("iptc-iptc-inserted.tif");
			
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
	}
	
	private static List<IPTCDataSet> createIPTCDataSet() {
		List<IPTCDataSet> iptcs = new ArrayList<IPTCDataSet>();
		iptcs.add(new IPTCDataSet(IPTCRecord.APPLICATION, IPTCApplicationTag.COPYRIGHT_NOTICE.getTag(), "Copyright 2014-2015, yuwen_66@yahoo.com"));
		iptcs.add(new IPTCDataSet(IPTCApplicationTag.CATEGORY.getTag(), "ICAFE"));
		iptcs.add(new IPTCDataSet(IPTCApplicationTag.KEY_WORDS.getTag(), "Welcome 'icafe' user!"));
		
		return iptcs;
	}
	
	private static BufferedImage createThumbnail(String filePath) throws IOException {
		FileInputStream fin = null;
		BufferedImage original = null;
		try {
			fin = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		original = javax.imageio.ImageIO.read(fin);
		int imageWidth = original.getWidth();
		int imageHeight = original.getHeight();
		// Default thumbnail dimension
		int thumbnailWidth = 160;
		int thumbnailHeight = 120;
		if(imageWidth < imageHeight) {
			// Swap width and height to keep a relative aspect ratio
			int temp = thumbnailWidth;
			thumbnailWidth = thumbnailHeight;
			thumbnailHeight = temp;
		}			
		if(imageWidth < thumbnailWidth) thumbnailWidth = imageWidth;
		if(imageHeight < thumbnailHeight) thumbnailHeight = imageHeight;
		BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = thumbnail.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(original, 0, 0, thumbnailWidth, thumbnailHeight, null);
		
		fin.close();
		
		return thumbnail;
	}
}