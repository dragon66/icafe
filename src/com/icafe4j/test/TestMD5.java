package com.icafe4j.test;

import com.icafe4j.string.StringUtils;

public class TestMD5 extends TestBase {
	
	public static void main(String[] args) {
		new TestMD5().test();
	}
	
	public void test(String ... args) {
		byte[] message = "Hello World".getBytes();
		String MD5 = StringUtils.generateMD5(message);
		logger.info(MD5);
	}
}
