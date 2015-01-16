package cafe.image.meta;

import cafe.util.Reader;

public interface MetadataReader extends Reader {
	public void showMetadata();
	public boolean isDataLoaded();
}