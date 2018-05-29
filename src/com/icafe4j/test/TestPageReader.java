package com.icafe4j.test;

import java.io.FileInputStream;
import com.icafe4j.image.tiff.PageReader;

public class TestPageReader extends TestBase {

	public static void main(String[] args) throws Exception {
		new TestPageReader().test(args);
	}
	
	public void test(String ... args) throws Exception {
		FileInputStream fin = new FileInputStream(args[0]);
		PageReader reader = new PageReader();
		int pageCounts = 0;
		
		while(reader.getNextPage(fin) != null) {
			pageCounts++;
		}
		
		logger.info("Total pages read {}", pageCounts);
		fin.close();
	}
}