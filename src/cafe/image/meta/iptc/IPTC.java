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

	/**
	 * Get a string representation of the IPTCDataSet associated with the key
	 *  
	 * @param key the name for the IPTCDataSet
	 * @return a String representation of the IPTCDataSet, separated by ";"
	 */
	public String getAsString(String key) {
		// Retrieve the IPTCDataSet list associated with this key
		// Most of the time the list will only contain one item
		List<IPTCDataSet> list = getDataSet(key);
		
		String value = "";
	
		if(list != null) {
			if(list.size() == 0) {
				value = list.get(0).getDataAsString();
			} else {
				for(int i = 0; i < list.size() - 1; i++)
					value += list.get(i).getDataAsString() + ";";
				value += list.get(list.size() - 1).getDataAsString();
			}
		}
			
		return value;
	}
	
	/**
	 * Get all the IPTCDataSet as a map for this IPTC data
	 * 
	 * @return a map with the key for the IPTCDataSet name and a list of IPTCDataSet as the value
	 */
	public Map<String, List<IPTCDataSet>> getDataSet() {
		return reader.getDataSet();
	}
	
	/**
	 * Get a list of IPTCDataSet associated with a key
	 * 
	 * @param key name of the data set
	 * @return a list of IPTCDataSet associated with the key
	 */
	public List<IPTCDataSet> getDataSet(String key) {
		return reader.getDataSet().get(key);
	}
	
	public IPTCReader getReader() {
		return reader;
	}
}