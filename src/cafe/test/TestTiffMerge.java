package cafe.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import cafe.image.tiff.TIFFTweaker;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;

import java.io.FileOutputStream;

public class TestTiffMerge {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		FileInputStream is1 = new FileInputStream(args[0]);
		FileInputStream is2 = new FileInputStream(args[1]);
		FileOutputStream out = new FileOutputStream(args[2]);
		RandomAccessInputStream src1 = new FileCacheRandomAccessInputStream(is1);
		RandomAccessInputStream src2 = new FileCacheRandomAccessInputStream(is2);
		RandomAccessOutputStream dest = new FileCacheRandomAccessOutputStream(out);
		TIFFTweaker.mergeTiffImages(src1, src2, dest);
		// Release resources
		src1.close();
		src2.close();
		dest.close();
		is1.close();
		is2.close();
		out.close();
	}
}
