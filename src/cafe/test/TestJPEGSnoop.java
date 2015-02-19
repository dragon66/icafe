package cafe.test;

import java.io.FileInputStream;
import java.io.IOException;

import cafe.image.jpeg.JPEGTweaker;
import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;

public class TestJPEGSnoop {

	public static void main(String[] args) throws IOException {
		FileInputStream fin = new FileInputStream(args[0]);
		Metadata meta = JPEGTweaker.readMetadata(fin).get(MetadataType.PHOTOSHOP);
		if(meta != null)
			meta.showMetadata();
		fin.close();
	}
}