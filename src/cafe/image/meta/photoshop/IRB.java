package cafe.image.meta.photoshop;

import java.io.IOException;
import java.io.InputStream;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataReader;
import cafe.image.meta.MetadataType;
import cafe.io.IOUtils;

public class IRB extends Metadata {
	private MetadataReader reader;
	
	public static void showIRB(byte[] irb) {
		if(irb != null && irb.length > 0) {
			IRBReader reader = new IRBReader(irb);
			try {
				reader.read();
				reader.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void showIRB(InputStream is) {
		try {
			showIRB(IOUtils.inputStreamToByteArray(is));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public IRB(byte[] data) {
		super(MetadataType.PHOTOSHOP_IRB, data);
		reader = new IRBReader(data);
	}
	
	public MetadataReader getReader() {
		return reader;
	}
}