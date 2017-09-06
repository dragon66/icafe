package com.icafe4j.test;

import com.icafe4j.util.LangUtils;

public class TestLangUtils extends TestBase {

	public TestLangUtils() { }

	public static void main(String[] args) {
		new TestLangUtils().test();
	}
	
	public void test(String ... args) {
		logger.info(LangUtils.getClassName(Object.class));
		LangUtils.log("showing information about the current executing line.", System.out);
		logger.info("{}", LangUtils.getLoadedClassLocation(LangUtils.class));
		logger.info("{}", LangUtils.getLoadedClassURL("com.icafe4j.util.LangUtils"));
		logger.info("{}/{}", LangUtils.doubleToRational(0.5)[0], LangUtils.doubleToRational(0.5)[1]);
	}
}