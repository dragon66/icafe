package cafe.test;

import cafe.util.LangUtils;

public class TestLangUtils {

	public TestLangUtils() { }

	public static void main(String[] args) {
		System.out.println(LangUtils.getClassName(Object.class));
		LangUtils.log("showing information about the current executing line.", System.out);
		System.out.println(LangUtils.getLoadedClassLocation(LangUtils.class));
		System.out.println(LangUtils.doubleToRational(0.5)[0] + "/" + LangUtils.doubleToRational(0.5)[1]);
	}
}