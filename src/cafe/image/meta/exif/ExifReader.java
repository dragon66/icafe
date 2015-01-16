package cafe.image.meta.exif;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cafe.image.meta.MetadataReader;
import cafe.image.tiff.IFD;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffTag;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;

public class ExifReader implements MetadataReader {
	private boolean loaded;
	private byte[] data;
	private ExifThumbnail thumbnail;
	private boolean containsThumbnail;
	private List<IFD> ifds = new ArrayList<IFD>(3);
	
	public ExifReader(byte[] exif) {
		this.data = exif;
	}
	
	public ExifReader(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public List<IFD> getIFDs() {
		return ifds;
	}
	
	public boolean containsThumbnail() {
		return containsThumbnail;
	}
	
	public ExifThumbnail getThumbnail() {
		return thumbnail;
	}
	
	public boolean isDataLoaded() {
		return loaded;
	}
	
	@Override
	public void read() throws IOException {
		RandomAccessInputStream exifIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(data));
	    TIFFTweaker.readIFDs(ifds, exifIn);
	    // We have thumbnail IFD
	    if(ifds.size() >= 2) {
	    	containsThumbnail = true;
	    	thumbnail = new ExifThumbnail();
	    	IFD thumbnailIFD = ifds.get(1);
	    	TiffField<?> field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue());
	    	if(field != null) { // JPEG format, save as JPEG
	    		int thumbnailOffset = field.getDataAsLong()[0];
	    		field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue());
	    		int thumbnailLen = field.getDataAsLong()[0];
	    		exifIn.seek(thumbnailOffset);
	    		byte[] data = new byte[thumbnailLen];
	    		exifIn.readFully(data);
	    		thumbnail.setImage(data, ExifThumbnail.DATA_TYPE_COMPRESSED_JPG, thumbnailIFD);
	    	} else { // Uncompressed, save as TIFF
	    		field = thumbnailIFD.getField(TiffTag.STRIP_OFFSETS.getValue());
	    		if(field == null) 
	    			field = thumbnailIFD.getField(TiffTag.TILE_OFFSETS.getValue());
	    		if(field != null) {
	    			 exifIn.seek(0);
	    			 ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    			 RandomAccessOutputStream tiffout = new FileCacheRandomAccessOutputStream(bout);
	    			 TIFFTweaker.retainPages(exifIn, tiffout, 1);
	    			 tiffout.close(); // Auto flush when closed
	    			 thumbnail.setImage(bout.toByteArray(), ExifThumbnail.DATA_TYPE_UNCOMPRESSED_TIFF, thumbnailIFD);
	    		}
	    	}
	    }
	    exifIn.close();
	    loaded = true;
	}

	@Override
	public void showMetadata() {
		if(!loaded) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(containsThumbnail) {
			System.out.println("Exif thumbnail format: " + (thumbnail.getDataType() == 1? "DATA_TYPE_COMPRESSED_JPG":"DATA_TYPE_COMPRESSED_TIFF"));
			System.out.println("Exif thumbnail data length: " + thumbnail.getCompressedImage().length);
		}
	}
}