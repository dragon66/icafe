package cafe.test;

import cafe.string.Base64;

public class TestBase64 {

	public static void main(String[] args) throws Exception {
		
		String input = "Merry Christmas!";
		System.out.println("Input string: " + input);
		String base64Encoded = Base64.encode(input);
		System.out.println("Base64 encoded as: " + base64Encoded);
		String base64Decoded = Base64.decode(base64Encoded);
		System.out.println("Base64 decoded as: " + base64Decoded);
	}
}
