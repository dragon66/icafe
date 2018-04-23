package com.icafe4j.test;

import java.util.List;
import java.util.Map;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.iptc.IPTC;
import com.icafe4j.image.meta.iptc.IPTCDataSet;

public class TestIPTC extends TestBase {

	public static void main(String[] args) throws Exception {
		new TestIPTC().test(args);
	}
	
	public void test(String ... args) throws Exception {
		Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(args[0]);
		// Show IPTC specific information
		IPTC iptc = (IPTC)metadataMap.get(MetadataType.IPTC);
		if(iptc != null) {
			// Retrieve a list of Keywords Dataset
			List<IPTCDataSet> keywords = iptc.getDataSet("Keywords");
			//List<IPTCDataset> keywords = iptc.getDataSet(IPTCEnvelopeTag.KEY_WORDS.getName());
			String value = "";
				
			for(IPTCDataSet item : keywords) {
				value += ";" + item.getDataAsString();
			}
				
			logger.info("Keywords: " + value.replaceFirst(";", ""));
		}
	}
}