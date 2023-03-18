package com.icafe4j.image.tiff;

import java.io.IOException;

import com.icafe4j.image.meta.exif.ExifTag;
import com.icafe4j.io.RandomAccessOutputStream;
import com.icafe4j.string.StringUtils;

public class MakerNoteField extends TiffField<byte[]> {

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
		return StringUtils.byteArrayToHexString(data, 0,  MAX_STRING_REPR_LEN);
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
