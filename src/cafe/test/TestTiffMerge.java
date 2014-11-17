package cafe.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import cafe.image.tiff.TIFFTweaker;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.RandomAccessOutputStream;
import cafe.util.FileUtils;

import java.io.FileOutputStream;

public class TestTiffMerge {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		long t1 = System.currentTimeMillis();
		FileOutputStream out = new FileOutputStream(args[2]);
		File[] files = FileUtils.listFilesMatching(new File(args[0]), args[1]);
		RandomAccessOutputStream dest = new FileCacheRandomAccessOutputStream(out);
		TIFFTweaker.mergeTiffImagesEx(dest, files);
		// Release resources
		dest.close();
		out.close();
		long t2 = System.currentTimeMillis();
		System.out.println("Merging time: " + (t2-t1) + " ms");
	}
}