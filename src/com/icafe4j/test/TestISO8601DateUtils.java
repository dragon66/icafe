package com.icafe4j.test;

import com.icafe4j.date.*;

public class TestISO8601DateUtils extends TestBase {
	
	public TestISO8601DateUtils() {}
    
	public static void main(String[] args) throws Exception {
		new TestISO8601DateUtils().test();
		
	}
	
    public void test(String ... args) throws Exception {
       	logger.info(DateUtils.parseISO8601("2008-09-22T16:20:30.998-05:00").formatISO8601());
    }
}