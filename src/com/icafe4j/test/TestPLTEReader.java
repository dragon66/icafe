package com.icafe4j.test;

import com.icafe4j.image.png.PLTEBuilder;
import com.icafe4j.image.png.PLTEReader;
import com.icafe4j.string.StringUtils;

public class TestPLTEReader extends TestBase {

	public TestPLTEReader() { }
	
	public static void main(String[] args) throws Exception {
		new TestPLTEReader().test();
	}
	
	public void test(String ... args) throws Exception {
		PLTEReader reader = new PLTEReader(
				new PLTEBuilder().redMap(new byte[] {1,4,7}).greenMap(new byte[] {2,5,8}).blueMap(new byte[] {3,6,9}).build());
		logger.info(StringUtils.byteArrayToHexString(reader.getRedMap()));
		logger.info(StringUtils.byteArrayToHexString(reader.getGreenMap()));
		logger.info(StringUtils.byteArrayToHexString(reader.getBlueMap()));
	}
}