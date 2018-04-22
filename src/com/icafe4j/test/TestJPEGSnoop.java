package com.icafe4j.test;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.Iterator;

import com.icafe4j.image.jpeg.JPEGTweaker;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataEntry;
import com.icafe4j.image.meta.MetadataType;

public class TestJPEGSnoop extends TestBase {

	public static void main(String[] args) throws Exception {
		new TestJPEGSnoop().test(args);
	}
	
	public void test(String ... args) throws Exception {
		FileInputStream fin = new FileInputStream(args[0]);
		Metadata meta = JPEGTweaker.readMetadata(fin).get(MetadataType.XMP);
		if(meta != null) {
			Iterator<MetadataEntry> iterator = meta.iterator();
			while(iterator.hasNext()) {
				MetadataEntry item = iterator.next();
				logger.info(item.getKey() + ": " + item.getValue());
				if(item.isMetadataEntryGroup()) {
					String indent = "    ";
					Collection<MetadataEntry> entries = item.getMetadataEntries();
					for(MetadataEntry e : entries) {
						logger.info(indent + e.getKey() + ": " + e.getValue());
					}			
				}
			}
		}
		fin.close();
	}
}