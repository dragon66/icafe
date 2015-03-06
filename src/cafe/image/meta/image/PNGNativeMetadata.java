package cafe.image.meta.image;

import java.util.List;

import cafe.image.meta.NativeMetadata;
import cafe.image.png.Chunk;

/**
 * PNG native image metadata
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/06/2015
 */
public class PNGNativeMetadata extends NativeMetadata<Chunk> {
	
	public PNGNativeMetadata() {
		;
	}

	public PNGNativeMetadata(List<Chunk> chunks) {
		super(chunks);
	}
	
	@Override
	public String getMimeType() {
		return "image/png";
	}
}