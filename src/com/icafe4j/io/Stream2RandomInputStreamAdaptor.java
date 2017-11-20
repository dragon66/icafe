package com.icafe4j.io;

import java.io.IOException;
import java.io.InputStream;

public class Stream2RandomInputStreamAdaptor extends RandomInputStreamAdaptor<InputStream> {

	public Stream2RandomInputStreamAdaptor(InputStream[] streams) {
		super(streams);
	}

	public RandomAccessInputStream next() {
		try {
			return new FileCacheRandomAccessInputStream(input[index++]);
		} catch (IOException e) {
			throw new RuntimeException("Cannot create RandomInputStream from input stream");
		}
	}
}
