package com.icafe4j.image.meta.tiff;

import java.io.IOException;
import java.io.OutputStream;

import com.icafe4j.image.meta.xmp.XMP;

public class TiffXMP extends XMP {

	public TiffXMP(byte[] data) {
		super(data);
		// TODO Auto-generated constructor stub
	}

	public void write(OutputStream os) throws IOException {
		// TODO: add code to write XMP to TIFF image		
	}
}
