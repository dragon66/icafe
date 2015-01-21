package cafe.test;

import java.io.FileInputStream;
import java.io.IOException;

import cafe.image.jpeg.JPEGTweaker;

public class TestJPEGSnoop {

	public static void main(String[] args) throws IOException {
		FileInputStream fin = new FileInputStream(args[0]);
		JPEGTweaker.readMetadata(fin);
		fin.close();
	}
}