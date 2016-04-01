package com.icafe4j.image.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;

import com.icafe4j.image.meta.xmp.XMP;

public class JpegXMP extends XMP {

	public JpegXMP(byte[] data) {
		super(data);
		// TODO Auto-generated constructor stub
	}

	public void write(OutputStream os) throws IOException {
		// TODO: add code to write XMP to JPEG image
	}
}
