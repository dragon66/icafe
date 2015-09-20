package com.icafe4j.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.icafe4j.image.tiff.ASCIIField;
import com.icafe4j.image.tiff.TIFFImage;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.FileCacheRandomAccessOutputStream;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.io.RandomAccessOutputStream;

public class TestTIFFImage extends TestBase {
	// Test manipulate TIFF image
	public static void main(String[] args) throws Exception {
		new TestTIFFImage().test(args);
	}
	
	public void test(String ... args) throws Exception {
		FileInputStream fin = new FileInputStream(args[0]);
		RandomAccessInputStream rin = new FileCacheRandomAccessInputStream(fin);
		FileOutputStream fout = new FileOutputStream("NEW.tif");
		RandomAccessOutputStream rout = new FileCacheRandomAccessOutputStream(fout);
		TIFFImage tiffImage = new TIFFImage(rin);
		int numOfPages = tiffImage.getNumOfPages();
		tiffImage.setWorkingPage(numOfPages - 1); // Add something to the last page
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss z");
		TiffField<?> tiffField = new ASCIIField(TiffTag.DATETIME.getValue(), formatter.format(new Date()) + '\0');
		tiffImage.addField(tiffField);
		// Remove pages
		while(numOfPages > 1) {
			tiffImage.removePage(0);
			numOfPages--;
		}		
		tiffImage.write(rout);
		rin.close(); // Release resources
		rout.close(); // Release resources
		fin.close(); // We need to close the stream explicitly since neither input 
		fout.close(); // nor output random stream closes the underlying stream
	}
}