package com.icafe4j.test;

import com.icafe4j.util.zip.CRC32;

public class TestCRC32 extends TestBase {
	
	public static void main(String[] args) {
		new TestCRC32().test();
	}
	
	public void test(String ... args) {
		byte[] buff = {0, 1, 2, 3, 4, 5};
		byte[] buff2 = {6, 7, 8, 9, 10};
		
		CRC32 crc32 = new CRC32();
		java.util.zip.CRC32 crc32j = new java.util.zip.CRC32();
		crc32.update(buff);
		crc32.update(buff2);
		crc32j.update(buff);
		crc32j.update(buff2);
		
		logger.info("Home made CRC32: {}", crc32.getValue());
		logger.info("Java util CRC32: {}", crc32j.getValue());		
		
		logger.info("Are they giving equal value? {}", (crc32.getValue()==crc32j.getValue()));
		
		crc32.reset();
		crc32j.reset();
		
		crc32.update(buff2);
		crc32.update(buff);
		
		crc32j.update(buff);
		crc32j.update(buff2);
		
		logger.info("Home made CRC32: {}", crc32.getValue());
		logger.info("Java util CRC32: {}", crc32j.getValue());	
		
		logger.info("Are they giving equal value? {}", (crc32.getValue()==crc32j.getValue()));		
	}
}
