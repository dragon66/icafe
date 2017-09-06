package com.icafe4j.test;

import com.icafe4j.util.FileUtils;

public class TestFileUtils extends TestBase {

	public static void main(String[] args) {
		new TestFileUtils().test();
	}

	@Override
	public void test(String ... args) {
		logger.info(System.getProperty("user.dir"));
		FileUtils.list(System.getProperty("user.dir"), "java");
		FileUtils.delete(System.getProperty("user.dir"), "tmp");
	}
}