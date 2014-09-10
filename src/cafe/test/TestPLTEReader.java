package cafe.test;

import java.io.IOException;

import cafe.image.png.PLTEBuilder;
import cafe.image.png.PLTEReader;
import cafe.string.StringUtils;

public class TestPLTEReader {

	public TestPLTEReader() { }
	
	public static void main(String[] args) throws IOException {
		PLTEReader reader = new PLTEReader(
				new PLTEBuilder().redMap(new byte[] {1,4,7}).greenMap(new byte[] {2,5,8}).blueMap(new byte[] {3,6,9}).build());
		System.out.println(StringUtils.byteArrayToHexString(reader.getRedMap()));
		System.out.println(StringUtils.byteArrayToHexString(reader.getGreenMap()));
		System.out.println(StringUtils.byteArrayToHexString(reader.getBlueMap()));
	}
}