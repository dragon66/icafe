package cafe.test;

import cafe.string.Base64;

public class TestBase64 extends TestBase {

	public static void main(String[] args) throws Exception {
		new TestBase64().test();		
	}
	
	@Override
	public void test(String ... args) throws Exception {
		String input = "Merry Christmas!";
		log.info("Input string: {}", input);
		String base64Encoded = Base64.encode(input);
		log.info("Base64 encoded as: {}", base64Encoded);
		String base64Decoded = Base64.decode(base64Encoded);
		log.info("Base64 decoded as: {}", base64Decoded);		
	}
}
