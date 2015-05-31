package cafe.test;

import cafe.util.LangUtils;

public class TestLangUtils extends TestBase {

	public TestLangUtils() { }

	public static void main(String[] args) {
		new TestLangUtils().test();
	}
	
	public void test(String ... args) {
		log.info(LangUtils.getClassName(Object.class));
		LangUtils.log("showing information about the current executing line.", System.out);
		log.info("{}", LangUtils.getLoadedClassLocation(LangUtils.class));
		log.info("{}", LangUtils.getLoadedClassURL("cafe.util.LangUtils"));
		log.info("{}/{}", LangUtils.doubleToRational(0.5)[0], LangUtils.doubleToRational(0.5)[1]);
	}
}