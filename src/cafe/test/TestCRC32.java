package cafe.test;

import cafe.util.zip.CRC32;

public class TestCRC32 {
	public static void main(String[]  args) {
		byte[] buff = {0, 1, 2, 3, 4, 5};
		byte[] buff2 = {6, 7, 8, 9, 10};
		
		CRC32 crc32 = new CRC32();
		java.util.zip.CRC32 crc32j = new java.util.zip.CRC32();
		crc32.update(buff);
		crc32.update(buff2);
		crc32j.update(buff);
		crc32j.update(buff2);
		
		System.out.println("Home made CRC32: " + crc32.getValue());
		System.out.println("Java util CRC32: " + crc32j.getValue());		
		
		System.out.println("Are they giving equal value? " + (crc32.getValue()==crc32j.getValue()));
		
		crc32.reset();
		crc32j.reset();
		
		crc32.update(buff2);
		crc32.update(buff);
		
		crc32j.update(buff);
		crc32j.update(buff2);
		
		System.out.println("Home made CRC32: " + crc32.getValue());
		System.out.println("Java util CRC32: " + crc32j.getValue());	
		
		System.out.println("Are they giving equal value? " + (crc32.getValue()==crc32j.getValue()));		
	}
}
