package cafe.test;

import java.io.FileInputStream;
import java.io.IOException;

import cafe.image.meta.icc.ICCProfile;

public class TestICCProfile {

	public static void main(String[] args) throws IOException {		
		FileInputStream fin = new FileInputStream(args[0]);
		ICCProfile icc_profile = new ICCProfile(fin);
		icc_profile.showHeader();
		icc_profile.showTagTable();
		fin.close();
	}
}