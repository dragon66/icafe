package cafe.test;

import java.io.FileInputStream;
import cafe.image.meta.Snoopy;

public class TestSnoopy {

	public static void main(String[] args) throws Exception {
		FileInputStream is = new FileInputStream(args[0]);
		Snoopy.snoop(is);
		is.close();
	}
}
