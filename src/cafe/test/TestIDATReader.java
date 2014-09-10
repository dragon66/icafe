package cafe.test;

import java.io.IOException;

import cafe.image.png.IDATBuilder;
import cafe.image.png.IDATReader;
import cafe.string.StringUtils;

public class TestIDATReader {

	public TestIDATReader() { }
	
	public static void main(String[] args) throws IOException {
		IDATReader reader = new IDATReader();
		reader.addChunk(new IDATBuilder().data(new byte[] {1,2,3}).data(new byte[] {4,5,6}).build());
		System.out.println(StringUtils.byteArrayToHexString(reader.getData()));
	}
}