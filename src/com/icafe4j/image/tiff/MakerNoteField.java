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
import com.icafe4j.io.WriteStrategyII;
import com.icafe4j.io.WriteStrategyMM;
import com.icafe4j.string.StringUtils;

public class MakerNoteField extends TiffField<byte[]> {
	//
	private int startOffSet; // offset relative to the EXIF
	private byte[] header = new byte[]{}; // Some maker notes start with a header
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
	
	private void ensureDataRead() {
		if(!isDataRead) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	private void read() throws IOException {
		if(!isDataRead) {
			// TODO Read header to figure out preferred endian
			RandomAccessInputStream is = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(data));
			is.setReadStrategy(null);
			List<IFD> ifds = new ArrayList<IFD>(1);
			TIFFTweaker.readIFD(is, ifds, ExifTag.class);
			
			preferredEndian = is.getEndian();
			
			if(ifds.size() > 0)
				ifd = ifds.get(0);
			
			is.close();		
			isDataRead = true;
		}
	}
	
	public void setPreferredEndian(short preferredEndian) {
		if(preferredEndian != IOUtils.BIG_ENDIAN && preferredEndian != IOUtils.LITTLE_ENDIAN)
			throw new IllegalArgumentException("Invalid IO stream endian!");
		this.preferredEndian = preferredEndian;
	}	

	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
		ensureDataRead();
		//Remember old endian
		short oldEndian = os.getEndian();
		// Set preferred endian
		if(preferredEndian != oldEndian)
			os.setWriteStrategy(preferredEndian == IOUtils.BIG_ENDIAN? WriteStrategyMM.getInstance() : WriteStrategyII.getInstance());
		// Write header first if any
		if(header.length > 0)
			os.write(header, 0, header.length);
		// Write the ifd
		if (ifd != null) 
			toOffset = ifd.write(os, toOffset + header.length);
		// Set old endian back
		os.setWriteStrategy(oldEndian == IOUtils.BIG_ENDIAN? WriteStrategyMM.getInstance() : WriteStrategyII.getInstance());

		return toOffset;
	}
}
