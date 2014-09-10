package cafe.test;

import cafe.util.FileUtils;

public class TestFileUtils {

	public static void main(String[] args) {
		FileUtils.list("C:/apache-tomcat-7.0.52/webapps/BIRTViewer", "rptdesign");
		FileUtils.delete("C:\\Tomcat 7.0\\webapps\\statusboard\\temp", "png");
	}
}