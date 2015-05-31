package cafe.test;

import cafe.util.FileUtils;

public class TestFileUtils extends TestBase {

	public static void main(String[] args) {
		new TestFileUtils().test();
	}

	@Override
	public void test(String ... args) {
		log.info(System.getProperty("user.dir"));
		FileUtils.list(System.getProperty("user.dir"), "java");
		FileUtils.delete(System.getProperty("user.dir"), "tmp");
	}
}