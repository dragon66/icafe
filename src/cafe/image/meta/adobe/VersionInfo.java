package cafe.image.meta.adobe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import cafe.io.IOUtils;
import cafe.string.StringUtils;
import cafe.util.ArrayUtils;

public class VersionInfo extends _8BIM {
	//
	private byte[] version;
	private boolean hasRealMergedData;
	private String writerName;
	private String readerName;
	private byte[] fileVersion;
	
	public VersionInfo() {
		this("VersionInfo");
	}
	
	public VersionInfo(String name) {
		super(ImageResourceID.VERSION_INFO, name, null);
	}

	public VersionInfo(String name, byte[] data) {
		super(ImageResourceID.VERSION_INFO, name, data);
		read();
	}
	
	public String getFileVersion() {
		return StringUtils.byteArrayToHexString(fileVersion);
	}
	
	public String getVersion() {
		return StringUtils.byteArrayToHexString(version);
	}
	
	public boolean hasRealMergedData() {
		return hasRealMergedData;
	}
	
	public String getReaderName() {
		return readerName;
	}
	
	public String getWriterName() {
		return writerName;
	}
	
	private void read() {
		int i = 0;
		version = ArrayUtils.subArray(data, i, 4);
		i += 4;
	    hasRealMergedData = ((data[i++]!=0)?true:false);
	    int writer_size = IOUtils.readIntMM(data, i);
	    i += 4;
	    try {
	    	writerName = new String(data, i, writer_size*2, "UTF-16BE");
	    } catch (UnsupportedEncodingException e) {
	    	e.printStackTrace();
	    }
	    i += writer_size*2;
	    int reader_size = IOUtils.readIntMM(data, i);
    	i += 4;
    	try {
			readerName = new String(data, i, reader_size*2, "UTF-16BE");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	i += reader_size*2;
	    fileVersion = ArrayUtils.subArray(data, i, 4);  
	}
	
	public void print() {
		super.print();
		System.out.println("Version: " + getVersion());
		System.out.println("Has Real Merged Data: " + hasRealMergedData);
        System.out.println("Writer name: " + writerName);
		System.out.println("Reader name: " + readerName);
		System.out.println("File Version: " + getFileVersion()); 
	}

	public void setHasRealMergedData(boolean hasRealMergedData) {
		this.hasRealMergedData = hasRealMergedData;
	}
	
	public void setFileVersion(byte[] fileVersion) {
		if(fileVersion.length != 4)
			throw new IllegalArgumentException("File version should be 4 bytes");
		this.fileVersion = fileVersion;
	}
	
	public void setVersion(byte[] version) {
		if(version.length != 4)
			throw new IllegalArgumentException("Version should be 4 bytes");
		this.version = version;
	}
	
	public void setWriterName(String writerName) {
		this.writerName = writerName;
	}
	
	public void setReaderName(String readerName) {
		this.readerName = readerName;
	}
	
	public void write(OutputStream os) throws IOException {
		if(data == null) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			bout.write(version);
			bout.write(hasRealMergedData?1:0);
			byte[] writerNameBytes = null;
			writerNameBytes = writerName.getBytes("UTF-16BE");
			IOUtils.writeIntMM(bout, writerName.length());
			bout.write(writerNameBytes);
			byte[] readerNameBytes = null;
			readerNameBytes = readerName.getBytes("UTF-16BE");
			IOUtils.writeIntMM(bout, readerName.length());
			bout.write(readerNameBytes);
			bout.write(fileVersion);
			data = bout.toByteArray();
			size = data.length;
		}
		super.write(os);
	}
}