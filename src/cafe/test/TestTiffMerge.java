package cafe.test;

import java.io.File;
import cafe.image.tiff.TIFFTweaker;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.RandomAccessOutputStream;
import cafe.util.FileUtils;

import java.io.FileOutputStream;

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