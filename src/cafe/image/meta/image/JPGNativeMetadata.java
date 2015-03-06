package cafe.image.meta.image;

import java.util.List;

import cafe.image.jpeg.Segment;
import cafe.image.meta.NativeMetadata;

/**
 * JPEG native image metadata
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/06/2015
 */
public class JPGNativeMetadata extends NativeMetadata<Segment> {
	
	public JPGNativeMetadata() {
		;
	}

	public JPGNativeMetadata(List<Segment> segments) {
		super(segments);
	}
	
	@Override
	public String getMimeType() {
		return "image/jpg";
	}
}