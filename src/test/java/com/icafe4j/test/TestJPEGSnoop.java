package com.icafe4j.test;

import java.io.FileInputStream;

import com.icafe4j.image.jpeg.JPEGTweaker;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;

public class TestJPEGSnoop extends TestBase {

	public static void main(String[] args) throws Exception {
		new TestJPEGSnoop().test(args);
	}
	
	public void test(String ... args) throws Exception {
		FileInputStream fin = new FileInputStream(args[0]);
		Metadata meta = JPEGTweaker.readMetadata(fin).get(MetadataType.XMP);
		if(meta != null)
			meta.showMetadata();
		fin.close();
	}
}