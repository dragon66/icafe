package cafe.test;

import cafe.string.StringUtils;

public class TestStringUtils extends TestBase {

	public TestStringUtils() {}
	
	public static void main(String args[]) {
		new TestStringUtils().test();
	}
	
	public void test(String ... args) {
		log.info("{}", StringUtils.isNullOrEmpty((String)null));
		log.info("{}", StringUtils.isNullOrEmpty(""));
		log.info("{}", StringUtils.isNullOrEmpty("   "));
		log.info(StringUtils.capitalizeFully("Here comes ME!"));		
		log.info(StringUtils.concat(new String[] {null, "a", "b", "c", null}, " and ") + "!");
		log.info(StringUtils.concat(null,"hello"," world", null));
		log.info(StringUtils.reverse("www.google.com", "."));
	}
}