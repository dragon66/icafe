package cafe.image.meta;

import java.awt.image.BufferedImage;

public class Thumbnail {
	// Internal data type for thumbnail
	// Represented by a BufferedImage
	public static final int DATA_TYPE_KRawRGB = 0; // This is from IRBThumbnail
	// Represented by a byte array of JPEG or TIFF image
	public static final int DATA_TYPE_KJpegRGB = 1; // This is from IRBThumbnail
	public static final int DATA_TYPE_TIFF = 2;
	
	private BufferedImage thumbnail;
	private byte[] compressedThumbnail;
	
	private int width;
	private int height;
	
	// Default data type
	private int dataType = Thumbnail.DATA_TYPE_KRawRGB;
	
	public Thumbnail() {}
	
	public Thumbnail(BufferedImage thumbnail) {
		setImage(thumbnail);
	}
	
	public Thumbnail(int width, int height, int dataType, byte[] compressedThumbnail) {
		setImage(width, height, dataType, compressedThumbnail);
	}
	
	public boolean containsImage() {
		return thumbnail != null || compressedThumbnail != null;
	}
	
	public byte[] getCompressedImage() {
		return compressedThumbnail;
	}
	
	public int getDataType() {
		return dataType;
	}
	
	public String getDataTypeAsString() {
		switch(dataType) {
			case 0:
				return "DATA_TYPE_KRawRGB";
			case 1:
				return "DATA_TYPE_KJpegRGB";
			case 2:
				return "DATA_TYPE_TIFF";
			default:
				return "DATA_TYPE_Unknown";
		}
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
		this.width = thumbnail.getWidth();
		this.height = thumbnail.getHeight();
		this.thumbnail = thumbnail;
		this.dataType = DATA_TYPE_KRawRGB;
	}
	
	public void setImage(int width, int height, int dataType, byte[] compressedThumbnail) {
		this.width = width;
		this.height = height;
		
		if(dataType == DATA_TYPE_KJpegRGB || dataType == DATA_TYPE_TIFF) {
			this.compressedThumbnail = compressedThumbnail;
			this.dataType = dataType;
		}
	}
}