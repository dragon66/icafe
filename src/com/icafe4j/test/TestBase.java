package com.icafe4j.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Temporary class to simplify logging until all the tests are migrated to JUnit
public abstract class TestBase {
	// Obtain a logger instance
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	public abstract void test(String ... args) throws Exception;
}
