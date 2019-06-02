package com.icafe4j.test;

import java.io.FileInputStream;
import com.icafe4j.image.jpeg.JPGTweaker;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.xmp.XMP;

public class TestJPGSnoop extends TestBase {

	public static void main(String[] args) throws Exception {
		new TestJPGSnoop().test(args);
	}
	
	public void test(String ... args) throws Exception {
		FileInputStream fin = new FileInputStream(args[0]);
		Metadata meta = JPGTweaker.readMetadata(fin).get(MetadataType.XMP);
		if(meta != null) {
			XMP.showXMP((XMP)meta);
		}
		fin.close();
	}
}