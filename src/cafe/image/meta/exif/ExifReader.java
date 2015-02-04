package cafe.image.meta.exif;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cafe.image.meta.MetadataReader;
import cafe.image.meta.Thumbnail;
import cafe.image.tiff.FieldType;
import cafe.image.tiff.IFD;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.tiff.Tag;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffTag;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.IOUtils;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;
import cafe.string.StringUtils;

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
	
	public ExifReader(IFD imageIFD) {
		ifds.add(0, imageIFD);
	}
	
	public IFD getExifIFD() {
		return ifds.get(0).getChild(TiffTag.EXIF_SUB_IFD);
	}
	
	public IFD getGPSIFD() {
		return ifds.get(0).getChild(TiffTag.GPS_SUB_IFD);
	}
	
	public IFD getImageIFD() {
		return ifds.get(0);
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
		if(data != null) {
			RandomAccessInputStream exifIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(data));
	    	TIFFTweaker.readIFDs(ifds, exifIn);		
		    // We have thumbnail IFD
		    if(ifds.size() >= 2) {
		    	containsThumbnail = true;
		    	IFD thumbnailIFD = ifds.get(1);
		    	int width = -1;
		    	int height = -1;
		    	TiffField<?> field = thumbnailIFD.getField(TiffTag.IMAGE_WIDTH.getValue());
		    	if(field != null) 
		    		width = field.getDataAsLong()[0];
		    	field = thumbnailIFD.getField(TiffTag.IMAGE_LENGTH.getValue());
		    	if(field != null)
		    		height = field.getDataAsLong()[0];
		    	field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue());
		    	if(field != null) { // JPEG format, save as JPEG
		    		int thumbnailOffset = field.getDataAsLong()[0];
		    		field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue());
		    		int thumbnailLen = field.getDataAsLong()[0];
		    		exifIn.seek(thumbnailOffset);
		    		byte[] thumbnailData = new byte[thumbnailLen];
		    		exifIn.readFully(thumbnailData);
		    		thumbnail = new ExifThumbnail(width, height, Thumbnail.DATA_TYPE_KJpegRGB, thumbnailData, thumbnailIFD);
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
		    			 thumbnail = new ExifThumbnail(width, height, Thumbnail.DATA_TYPE_TIFF, bout.toByteArray(), thumbnailIFD);
		    		}
		    	}
		    }
		    exifIn.close();
		}
	    loaded = true;
	}
	
	private static void showIFDs(Collection<IFD> list, String indent) {
		int id = 0;
		System.out.print(indent);
		for(IFD currIFD : list) {
			System.out.println("IFD #" + id);
			showIFD(currIFD, TiffTag.class, indent);
			id++;
		}
	}
	
	private static void showIFD(IFD currIFD, Class<? extends Tag> tagClass, String indent) {
		// Use reflection to invoke fromShort(short) method
		Method method = null;
		try {
			method = tagClass.getDeclaredMethod("fromShort", short.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		Collection<TiffField<?>> fields = currIFD.getFields();
		int i = 0;
		for(TiffField<?> field : fields) {
			System.out.print(indent);
			System.out.println("Field #" + i);
			System.out.print(indent);
			short tag = field.getTag();
			Tag ftag = TiffTag.UNKNOWN;
			try {
				ftag = (Tag)method.invoke(null, tag);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			if (ftag == TiffTag.UNKNOWN) {
				System.out.println("Tag: " + ftag + " [Value: 0x"+ Integer.toHexString(tag&0xffff) + "]" + " (Unknown)");
			} else {
				System.out.println("Tag: " + ftag);
			}
			FieldType ftype = field.getType();				
			System.out.print(indent);
			System.out.println("Field type: " + ftype);
			int field_length = field.getLength();
			System.out.print(indent);
			System.out.println("Field length: " + field_length);
			System.out.print(indent);			
			switch (ftype)
			{
				case BYTE:
				case UNDEFINED:
					byte[] data = (byte[])field.getData();
					if(ftag == ExifTag.EXIF_VERSION || ftag == ExifTag.FLASH_PIX_VERSION)
						System.out.println("Field value: " + new String(data));
					else
						System.out.println("Field value: " + StringUtils.byteArrayToHexString(data, 0, 10));
					break;
				case ASCII:
					System.out.println("Field value: " + (String)field.getData());
					break;
				case SHORT:
					short[] sdata = (short[])field.getData();
					System.out.println("Field value: " + StringUtils.shortArrayToString(sdata, 0, 10, true) + " " + ftag.getFieldDescription(sdata[0]&0xffff));
					break;
				case LONG:
					int[] ldata = (int[])field.getData();
					System.out.println("Field value: " + StringUtils.longArrayToString(ldata, 0, 10, true) + " " + ftag.getFieldDescription(ldata[0]&0xffff));
					break;
				case FLOAT:
					float[] fdata = (float[])field.getData();
					System.out.println("Field value: " + Arrays.toString(fdata));							
					break;
				case DOUBLE:
					double[] ddata = (double[])field.getData();
					System.out.println("Field value: " + Arrays.toString(ddata));
					break;
				case RATIONAL:
					ldata = (int[])field.getData();	
					System.out.println("Field value: " + StringUtils.rationalArrayToString(ldata, true));
					break;
				default:
					break;					
			}
			i++;
		}
		Map<Tag, IFD> children = currIFD.getChildren();
		
		if(children.get(TiffTag.EXIF_SUB_IFD) != null) {
			System.out.print(indent + ">>>>>>>>> ");
			System.out.println("Exif SubIFD:");
			showIFD(children.get(TiffTag.EXIF_SUB_IFD), ExifTag.class, indent + "--------- ");
		}
		
		if(children.get(TiffTag.GPS_SUB_IFD) != null) {
			System.out.print(indent + ">>>>>>>>> ");
			System.out.println("GPS SubIFD:");
			showIFD(children.get(TiffTag.GPS_SUB_IFD), GPSTag.class, indent + "--------- ");
		}		
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
		System.out.println("Exif reader output starts =>");
		showIFDs(ifds, "");
		if(containsThumbnail) {
			System.out.println("Exif thumbnail format: " + (thumbnail.getDataType() == 1? "DATA_TYPE_JPG":"DATA_TYPE_TIFF"));
			System.out.println("Exif thumbnail data length: " + thumbnail.getCompressedImage().length);
		}
		System.out.println("<= Exif reader output ends");
	}
}