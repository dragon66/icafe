package cafe.test;

import java.io.IOException;
import java.util.Map;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;

public class TestMetadata {

	public static void main(String[] args) throws IOException {
		Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(args[0]);
		System.out.println("Start of metadata information:");
		System.out.println("Total number of metadata entries: " + metadataMap.size());
		int i = 0;
		for(Map.Entry<MetadataType, Metadata> entry : metadataMap.entrySet()) {
			System.out.println("Metadata entry " + i + " - " + entry.getKey());
			entry.getValue().showMetadata();
			i++;
			System.out.println("-----------------------------------------");
		}
		System.out.println("End of metadata information.");
		Metadata.extractThumbnails(args[0], "thumbnail");		
	}
}