package cafe.image.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata for native image formats
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/06/2015
 */
public abstract class NativeMetadata<T> {
	private List<T> metadataList;
	
	public NativeMetadata() {
		;
	}
	
	public NativeMetadata(List<T> meta) {
		metadataList = meta;
	}
	
	public List<T> getMetadataList() {
		return Collections.unmodifiableList(metadataList);
	}
	
	public abstract String getMimeType();
	
	public void addMeta(T meta) {
		if(metadataList == null)
			metadataList = new ArrayList<T>();
		metadataList.add(meta);
	}
}