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
		FileOutputStream out = new FileOutputStream(args[2]);
		File[] files = FileUtils.listFilesMatching(new File(args[0]), args[1]);
		RandomAccessOutputStream dest = new FileCacheRandomAccessOutputStream(out);
		TIFFTweaker.mergeTiffImagesEx(dest, files);
		// Release resources
		dest.close();
		out.close();
	}
}