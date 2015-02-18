package cafe.image.meta.iptc;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import cafe.image.meta.Metadata;
import cafe.image.meta.MetadataType;
import cafe.io.IOUtils;

public class IPTC extends Metadata {
	private IPTCReader reader;
	
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
	
	public Map<String, List<IPTCDataSet>> getDataSet() {
		return reader.getDataSet();
	}
	
	public List<IPTCDataSet> getDataSet(String key) {
		return reader.getDataSet().get(key);
	}
	
	public IPTCReader getReader() {
		return reader;
	}
}