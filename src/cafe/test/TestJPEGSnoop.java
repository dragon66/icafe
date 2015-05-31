package cafe.test;

import java.io.FileInputStream;
import cafe.image.jpeg.JPEGTweaker;
import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;

public class TestJPEGSnoop extends TestBase {

	public static void main(String[] args) throws Exception {
		new TestJPEGSnoop().test(args);
	}
	
	public void test(String ... args) throws Exception {
		FileInputStream fin = new FileInputStream(args[0]);
		Metadata meta = JPEGTweaker.readMetadata(fin).get(MetadataType.XMP);
		if(meta != null)
			meta.showMetadata();
		fin.close();
	}
}