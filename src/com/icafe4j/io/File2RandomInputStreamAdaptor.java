package com.icafe4j.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class File2RandomInputStreamAdaptor extends RandomInputStreamAdaptor<File> {
	
	public File2RandomInputStreamAdaptor(File[] files) {
		super(files);
	}

	public RandomAccessInputStream next() {
		try {
			return new FileCacheRandomAccessInputStream(new FileInputStream(input[index++]));
		} catch (IOException e) {
			throw new RuntimeException("Failed to create RandomAccessInputStream from input file");
		}
	}
}
