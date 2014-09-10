package cafe.test;

import java.io.FileInputStream;
import java.io.IOException;

import cafe.image.tiff.TIFFTweaker;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.RandomAccessInputStream;

public class TestTIFFSnoop {

	public static void main(String[] args) throws IOException {
		FileInputStream fin = new FileInputStream(args[0]);
		RandomAccessInputStream randomIS = new FileCacheRandomAccessInputStream(fin);
		TIFFTweaker.snoop(randomIS);
	}
}