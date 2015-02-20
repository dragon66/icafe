package cafe.image.meta.adobe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import cafe.image.meta.iptc.IPTC;
import cafe.image.meta.iptc.IPTCDataSet;

public class PhotoshopIPTC extends _8BIM {
	//
	private IPTC iptc;
		
	public PhotoshopIPTC() {
		this("IPTC_NAA");
	}
	
	public PhotoshopIPTC(String name) {
		super(ImageResourceID.IPTC_NAA, name, null);
		iptc = new IPTC();
	}

	public PhotoshopIPTC(String name, byte[] data) {
		super(ImageResourceID.IPTC_NAA, name, data);
		iptc = new IPTC(data);
		read();
	}
	
	public void addDataSet(IPTCDataSet dataSet) {
		iptc.addDataSet(dataSet);
	}
	
	/**
	 * Get all the IPTCDataSet as a map for this IPTC data
	 * 
	 * @return a map with the key for the IPTCDataSet name and a list of IPTCDataSet as the value
	 */
	public Map<String, List<IPTCDataSet>> getDataSet() {
		return iptc.getDataSet();			
	}
	
	/**
	 * Get a list of IPTCDataSet associated with a key
	 * 
	 * @param key name of the data set
	 * @return a list of IPTCDataSet associated with the key
	 */
	public List<IPTCDataSet> getDataSet(String key) {
		return iptc.getDataSet(key);
	}
	
	private void read() {
		try {
			iptc.getReader().read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void print() {
		super.print();
		// Print multiple entry IPTCDataSet
		for(List<IPTCDataSet> datasets : iptc.getDataSet().values())
			for(IPTCDataSet dataset : datasets)
				dataset.print();			
	}
	
	public void write(OutputStream os) throws IOException {
		if(data == null) {			
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			for(List<IPTCDataSet> datasets : iptc.getDataSet().values())
				for(IPTCDataSet dataset : datasets)
					dataset.write(bout);
			data = bout.toByteArray();
			size = data.length;
		}
		super.write(os);
	}
}