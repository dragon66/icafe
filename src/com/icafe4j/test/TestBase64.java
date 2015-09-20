package com.icafe4j.test;

import com.icafe4j.string.Base64;

public class TestBase64 extends TestBase {

	public static void main(String[] args) throws Exception {
		new TestBase64().test();		
	}
	
	@Override
	public void test(String ... args) throws Exception {
		String input = "Merry Christmas!";
		logger.info("Input string: {}", input);
		String base64Encoded = Base64.encode(input);
		logger.info("Base64 encoded as: {}", base64Encoded);
		String base64Decoded = Base64.decode(base64Encoded);
		logger.info("Base64 decoded as: {}", base64Decoded);		
	}
}
