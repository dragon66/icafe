package com.icafe4j.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.icafe4j.image.tiff.ASCIIField;
import com.icafe4j.image.tiff.LongField;
import com.icafe4j.image.tiff.ShortField;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffTag;

public class TestTIFFField extends TestBase {

	public TestTIFFField() {}
	
	public static void main(String args[]) {
		new TestTIFFField().test(args);
	}
	
    public void test(String ... args) {
		List<TiffField<?>> tiffFields = new ArrayList<TiffField<?>>();
		tiffFields.add(new ShortField(TiffTag.ROWS_PER_STRIP.getValue(), new short[]{2}));
		tiffFields.add(new LongField(TiffTag.SUBFILE_TYPE.getValue(), new int[]{0}));
		tiffFields.add(new ASCIIField(TiffTag.SOFTWARE.getValue(), "ImageMagick\0"));
		tiffFields.add(new LongField((short)0x002e, new int[]{19}));
		Collections.sort(tiffFields);
		logger.info("{}", tiffFields);
	}
}
