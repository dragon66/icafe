package cafe.test;

import java.io.FileInputStream;
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
		
		FileInputStream fin = new FileInputStream("images/f1.tif");
		FileOutputStream fout = new FileOutputStream("f1-iccp-inserted.tif");
			
		Metadata.insertIPTC(fin, fout, createIPTCDataSet());
		
		fin.close();
		fout.close();
	}
	
	private static List<IPTCDataSet> createIPTCDataSet() {
		List<IPTCDataSet> iptcs = new ArrayList<IPTCDataSet>();
		iptcs.add(new IPTCDataSet(IPTCRecord.APPLICATION, IPTCApplicationTag.COPYRIGHT_NOTICE.getTag(), "Copyright 2014-2015, yuwen_66@yahoo.com"));
		iptcs.add(new IPTCDataSet(IPTCApplicationTag.KEY_WORDS.getTag(), "Welcome 'icafe' user!"));
		
		return iptcs;
	}
}