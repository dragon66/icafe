package com.icafe4j.image.tiff;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.icafe4j.image.meta.exif.ExifTag;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.IOUtils;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.io.RandomAccessOutputStream;
import com.icafe4j.string.StringUtils;

public class MakerNoteField extends TiffField<byte[]> {
	//
	private short preferredEndian = IOUtils.BIG_ENDIAN;
	private boolean isDataRead;
	private IFD ifd;

	public MakerNoteField(byte[] data) {
		this(null, data);
	}
	
	public MakerNoteField(IFD parent, byte[] data) {
		super(parent, ExifTag.MAKER_NOTE.getValue(), FieldType.EXIF_MAKERNOTE, data.length);
		this.data = data;
	}
	
	public byte[] getData() {
		return data.clone();
	}
	
	public String getDataAsString() {
		return StringUtils.byteArrayToHexString(data, 0, TiffField.MAX_STRING_REPR_LEN);
	}
	
	public short getPreferredEndian() {
		return preferredEndian;
	}
	
	public void ensureDataRead() {
		if(!isDataRead) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			RandomAccessInputStream is = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(data));
			List<IFD> ifds = new ArrayList<IFD>(1);
			TIFFTweaker.readIFD(is, ifds, ExifTag.class);
			
			preferredEndian = is.getEndian();
			
			if(ifds.size() > 0)
				ifd = ifds.get(0);
			
			is.close();		
			isDataRead = true;
		}
	}
	
	public boolean isDataRead() {
		return isDataRead;
	}
	
	public void setPreferredEndian(short preferredEndian) {
		if(preferredEndian != IOUtils.BIG_ENDIAN && preferredEndian != IOUtils.LITTLE_ENDIAN)
			throw new IllegalArgumentException("Invalid Exif endian!");
		this.preferredEndian = preferredEndian;
	}	

	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
		
		if (data.length <= 4) {
			dataOffset = (int)os.getStreamPointer();
			byte[] tmp = new byte[4];
			System.arraycopy(data, 0, tmp, 0, data.length);
			os.write(tmp);
		} else {
			dataOffset = toOffset;
			os.writeInt(toOffset);
			os.seek(toOffset);
			os.write(data);
			toOffset += data.length;
		}
		
		return toOffset;
	}
}
