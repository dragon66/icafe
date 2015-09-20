package com.icafe4j.test;

import com.icafe4j.string.StringUtils;

public class TestStringUtils extends TestBase {

	public TestStringUtils() {}
	
	public static void main(String args[]) {
		new TestStringUtils().test();
	}
	
	public void test(String ... args) {
		logger.info("{}", StringUtils.isNullOrEmpty((String)null));
		logger.info("{}", StringUtils.isNullOrEmpty(""));
		logger.info("{}", StringUtils.isNullOrEmpty("   "));
		logger.info(StringUtils.capitalizeFully("Here comes ME!"));		
		logger.info(StringUtils.concat(new String[] {null, "a", "b", "c", null}, " and ") + "!");
		logger.info(StringUtils.concat(null,"hello"," world", null));
		logger.info(StringUtils.reverse("www.google.com", "."));
	}
}