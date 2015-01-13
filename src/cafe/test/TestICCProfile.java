package cafe.test;

import java.io.FileInputStream;
import java.io.IOException;

import cafe.image.meta.icc.ICCProfile;

public class TestICCProfile {

	public static void main(String[] args) throws IOException {		
		FileInputStream fin = new FileInputStream(args[0]);
		ICCProfile.showProfile(fin);
		fin.close();
	}
}