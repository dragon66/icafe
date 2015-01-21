package cafe.image.meta.image;

import java.awt.image.BufferedImage;

public class Thumbnail {
	// Internal data type for thumbnail
	// Represented by a BufferedImage
	public static final int DATA_TYPE_KRawRGB = 0; // This is from IRBThumbnail
	// Represented by a byte array of JPEG or TIFF stream
	public static final int DATA_TYPE_KJpegRGB = 1; // This is from IRBThumbnail
	public static final int DATA_TYPE_KTiffRGB = 2; // This one is fabricated
	
	protected BufferedImage thumbnail;
	protected byte[] compressedThumbnail;
	
	protected int width;
	protected int height;
	
	// Default data type
	protected int dataType = Thumbnail.DATA_TYPE_KRawRGB; 
	
	public boolean containsImage() {
		return thumbnail != null || compressedThumbnail != null;
	}
	
	public byte[] getCompressedImage() {
		return compressedThumbnail;
	}
	
	public int getDataType() {
		return dataType;
	}
	
	public int getHeight() {
		return height;
	}
	
	public BufferedImage getRawImage() {
		return thumbnail;
	}
	
	public int getWidth() {
		return width;
	}
	
	public void setImage(BufferedImage thumbnail) {
		this.thumbnail = thumbnail;
		this.dataType = DATA_TYPE_KRawRGB;
	}	
}