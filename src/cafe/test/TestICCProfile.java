package cafe.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cafe.image.meta.Metadata;
import cafe.image.meta.icc.ICCProfile;

public class TestICCProfile {

	public static void main(String[] args) throws IOException {		
		FileInputStream fin = new FileInputStream(args[0]);		
		Metadata icc_profile = new ICCProfile(fin);
		icc_profile.getReader().showMetadata();
		FileOutputStream fout = new FileOutputStream(new File("ICCProfile.icc"));
		icc_profile.write(fout);
		fin.close();
		fout.close();
	}
}