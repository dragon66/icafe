package cafe.test;

import cafe.date.*;

public class TestISO8601DateUtils {

	public TestISO8601DateUtils() {}
    
    public static void main(String[] args) throws Exception {
       	System.out.println(ISO8601DateUtils.parse("2008-09-22T16:20:30.998-05:00").formatISO8601());
    }
}