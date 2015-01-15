package cafe.image.meta.iptc;

import java.io.IOException;
import java.io.InputStream;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataReader;
import cafe.image.meta.MetadataType;
import cafe.io.IOUtils;

public class IPTC extends Metadata {
	private MetadataReader reader;
	
	public static void showIPTC(byte[] iptc) {
		if(iptc != null && iptc.length > 0) {
			IPTCReader reader = new IPTCReader(iptc);
			try {
				reader.read();
				reader.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void showIPTC(InputStream is) {
		try {
			showIPTC(IOUtils.inputStreamToByteArray(is));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public IPTC(byte[] data) {
		super(MetadataType.IPTC, data);
		reader = new IPTCReader(data);
	}
	
	public MetadataReader getReader() {
		return reader;
	}
}