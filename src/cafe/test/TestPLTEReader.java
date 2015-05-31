package cafe.test;

import cafe.image.png.PLTEBuilder;
import cafe.image.png.PLTEReader;
import cafe.string.StringUtils;

public class TestPLTEReader extends TestBase {

	public TestPLTEReader() { }
	
	public static void main(String[] args) throws Exception {
		new TestPLTEReader().test();
	}
	
	public void test(String ... args) throws Exception {
		PLTEReader reader = new PLTEReader(
				new PLTEBuilder().redMap(new byte[] {1,4,7}).greenMap(new byte[] {2,5,8}).blueMap(new byte[] {3,6,9}).build());
		log.info(StringUtils.byteArrayToHexString(reader.getRedMap()));
		log.info(StringUtils.byteArrayToHexString(reader.getGreenMap()));
		log.info(StringUtils.byteArrayToHexString(reader.getBlueMap()));
	}
}