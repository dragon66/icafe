package cafe.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.image.tiff.TIFFTweaker;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.RandomAccessInputStream;

public class TestTIFFSnoop extends TestBase {
	public static void main(String[] args) throws IOException {
		new TestTIFFSnoop().test(args);
	}
	
	public void test(String ... args) throws IOException {
		FileInputStream fin = new FileInputStream(args[0]);
		RandomAccessInputStream randomIS = new FileCacheRandomAccessInputStream(fin);
		Map<MetadataType, Metadata> metadataMap = TIFFTweaker.readMetadata(randomIS);
		logger.info("Start of metadata information:");
		logger.info("Total number of metadata entries: {}", metadataMap.size());
		int i = 0;
		for(Map.Entry<MetadataType, Metadata> entry : metadataMap.entrySet()) {
			logger.info("Metadata entry {} - {}", i, entry.getKey());
			entry.getValue().showMetadata();
			i++;
			logger.info("-----------------------------------------");
		}
		logger.info("End of metadata information.");
	}
}