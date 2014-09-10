package cafe.test;

import cafe.string.StringUtils;

public class TestStringUtils {

	public TestStringUtils() {}
	
	public static void main(String args[]) {
		System.out.println(StringUtils.isNullOrEmpty((String)null));
		System.out.println(StringUtils.isNullOrEmpty(""));
		System.out.println(StringUtils.isNullOrEmpty("   "));
		System.out.println(StringUtils.capitalizeFully("Here comes ME!"));		
		System.out.println(StringUtils.concat(new String[] {null, "a", "b", "c", null}, " and ") + "!");
		System.out.println(StringUtils.concat(null,"hello"," world", null));
		System.out.println(StringUtils.reverse("www.google.com", "."));
	}
}