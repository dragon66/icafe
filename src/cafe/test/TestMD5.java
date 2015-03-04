package cafe.test;

import cafe.string.StringUtils;

public class TestMD5 {

	public static void main(String[] args) {
		byte[] message = "Hello World".getBytes();
		String MD5 = StringUtils.generateMD5(message);
		System.out.println(MD5);
	}
}
