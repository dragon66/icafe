package cafe.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cafe.image.tiff.ASCIIField;
import cafe.image.tiff.TIFFImage;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.TiffTag;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;

public class TestTIFFImage {
	// Test manipulate TIFF image
	public static void main(String[] args) throws IOException {
		FileInputStream fin = new FileInputStream(args[0]);
		RandomAccessInputStream rin = new FileCacheRandomAccessInputStream(fin);
		FileOutputStream fout = new FileOutputStream("NEW.tif");
		RandomAccessOutputStream rout = new FileCacheRandomAccessOutputStream(fout);
		TIFFImage tiffImage = new TIFFImage(rin, rout);
		int numOfPages = tiffImage.getNumOfPages();
		tiffImage.setWorkingPage(numOfPages - 1); // Add something to the last page
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss z");
		TiffField<?> tiffField = new ASCIIField(TiffTag.DATETIME.getValue(), formatter.format(new Date()) + '\0');
		tiffImage.addField(tiffField);
		tiffImage.write();
		rin.close(); // Release resources
		rout.close(); // Release resources
		fin.close(); // We need to close the stream explicitly since neither input 
		fout.close(); // nor output random stream closes the underlying stream
	}
}