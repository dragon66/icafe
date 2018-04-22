package com.icafe4j.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.tiff.TIFFTweaker;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.RandomAccessInputStream;

public class TestTIFFSnoop extends TestBase {
	public static void main(String[] args) throws IOException {
		new TestTIFFSnoop().test(args);
	}
	
	public void test(String ... args) throws IOException {
		FileInputStream fin = new FileInputStream(args[0]);
		RandomAccessInputStream randomIS = new FileCacheRandomAccessInputStream(fin);
		Map<MetadataType, Metadata> metadataMap = TIFFTweaker.readMetadata(randomIS);
		logger.info("Start of metadata information:");
		logger.info("Total number of metadata entries: {}", metadataMap.size());
		int i = 0;
		for(Map.Entry<MetadataType, Metadata> entry : metadataMap.entrySet()) {
			logger.info("Metadata entry {} - {}", i, entry.getKey());
			Iterator<MetadataEntry> itr = entry.getValue().iterator();
			while(itr.hasNext()) {
				MetadataEntry item = itr.next();
				logger.info(item.getKey() + ": " + item.getValue());
				if(item.isMetadataEntryGroup()) {
					String indent = "    ";
					Collection<MetadataEntry> entries = item.getMetadataEntries();
					for(MetadataEntry e : entries) {
						logger.info(indent + e.getKey() + ": " + e.getValue());
					}			
				}					
			}
			i++;
			logger.info("-----------------------------------------");
		}
		logger.info("End of metadata information.");
	}
}