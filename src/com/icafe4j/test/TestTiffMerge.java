package com.icafe4j.test;

import java.io.File;
import java.io.FileOutputStream;

import com.icafe4j.image.tiff.TIFFTweaker;
import com.icafe4j.io.FileCacheRandomAccessOutputStream;
import com.icafe4j.io.RandomAccessOutputStream;
import com.icafe4j.util.FileUtils;

public class TestTiffMerge extends TestBase {
	
	public static void main(String[] args) throws Exception {
		new TestTiffMerge().test(args);
	}

	public void test(String ... args) throws Exception {
		long t1 = System.currentTimeMillis();
		FileOutputStream out = new FileOutputStream(args[2]);
		File[] files = FileUtils.listFilesMatching(new File(args[0]), args[1]);
		RandomAccessOutputStream dest = new FileCacheRandomAccessOutputStream(out);
		TIFFTweaker.mergeTiffImagesEx(dest, files);
		// Release resources
		dest.close();
		out.close();
		long t2 = System.currentTimeMillis();
		logger.info("Merging time: {}ms", (t2-t1));
	}
}