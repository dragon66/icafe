package com.icafe4j.test;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.icafe4j.image.ImageColorType;
import com.icafe4j.image.ImageFrame;
import com.icafe4j.image.ImageParam;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.exif.ExifTag;
import com.icafe4j.image.meta.tiff.TiffExif;
import com.icafe4j.image.options.TIFFOptions;
import com.icafe4j.image.quant.DitherMethod;
import com.icafe4j.image.tiff.FieldType;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.TIFFTweaker;
import com.icafe4j.image.tiff.TiffFieldEnum.Compression;
import com.icafe4j.image.writer.TIFFWriter;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.FileCacheRandomAccessOutputStream;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.io.RandomAccessOutputStream;
import com.icafe4j.io.WriteStrategyII;
import com.icafe4j.util.FileUtils;

public class TestTIFFTweaker extends TestBase {
	public static void main(String[] args) throws Exception {
		new TestTIFFTweaker().test(args);
	}
	
	public void test(String ... args) throws Exception {
		FileInputStream fin = new FileInputStream(args[0]);
		RandomAccessInputStream rin = new FileCacheRandomAccessInputStream(fin);
		FileOutputStream fout = null;
		RandomAccessOutputStream rout = null;
		
		if(args.length > 1) {			
			if(args[1].equalsIgnoreCase("copycat")) {
				fout = new FileOutputStream("NEW.tif");
				rout = new FileCacheRandomAccessOutputStream(fout);
				TIFFTweaker.copyCat(rin, rout);
				rout.close();
				fout.close();
			} else if(args[1].equalsIgnoreCase("snoop")) {
				TIFFTweaker.readMetadata(rin);
			} else if(args[1].equalsIgnoreCase("extractThumbnail")){
				TIFFTweaker.extractThumbnail(rin, "thumbnail");					
			} else if(args[1].equalsIgnoreCase("extractICCProfile")) {
				byte[] icc_profile = TIFFTweaker.extractICCProfile(rin);
				if(icc_profile != null) {
					OutputStream iccOut = new FileOutputStream(new File("ICCProfile.icc"));
					iccOut.write(icc_profile);
					iccOut.close();
				}
			} else if(args[1].equalsIgnoreCase("retainpage")) {
				int pageCount = TIFFTweaker.getPageCount(rin);
				fout = new FileOutputStream("NEW.tif");
				rout = new FileCacheRandomAccessOutputStream(fout);
				if(pageCount > 1)
					TIFFTweaker.retainPages(rin, rout, pageCount - 1); // Last page
				else
					TIFFTweaker.copyCat(rin, rout);
				rout.close();
				fout.close();
			} else if(args[1].equalsIgnoreCase("writemultipage") || args[1].equalsIgnoreCase("insertpage")) {
				File[] files = FileUtils.listFilesMatching(new File(args[2]), args[3]);
				ImageFrame[] frames = new ImageFrame[files.length];				
				for(int i = 0; i < files.length; i++) {
					fin = new FileInputStream(files[i]);
					BufferedImage image = javax.imageio.ImageIO.read(fin);
					frames[i] = new ImageFrame(image);
					fin.close();
				}				
				
				ImageParam.ImageParamBuilder builder = ImageParam.getBuilder();
				
				TIFFOptions tiffOptions = new TIFFOptions();
				tiffOptions.setTiffCompression(Compression.LZW);
				tiffOptions.setApplyPredictor(true);
				tiffOptions.setDeflateCompressionLevel(6);
				builder.imageOptions(tiffOptions);
				
				frames[0].setFrameParam(builder.colorType(ImageColorType.GRAY_SCALE).hasAlpha(true).build());
				
				tiffOptions = new TIFFOptions(tiffOptions); // Copy constructor		
				tiffOptions.setTiffCompression(Compression.DEFLATE);
								
				frames[1].setFrameParam(builder.imageOptions(tiffOptions).build());
				
				tiffOptions = new TIFFOptions(tiffOptions);				
				tiffOptions.setTiffCompression(Compression.CCITTFAX4);
				
				ImageParam param = builder.colorType(ImageColorType.BILEVEL).applyDither(true).ditherMethod(DitherMethod.BAYER).imageOptions(tiffOptions).build();
				
				for(int i = 2; i < frames.length; i++)
					frames[i].setFrameParam(param);
				
				fout = new FileOutputStream("NEW.tif");
				rout = new FileCacheRandomAccessOutputStream(fout);
				rout.setWriteStrategy(WriteStrategyII.getInstance());
				
				if(args[1].equalsIgnoreCase("writemultipage")) {
					//TIFFTweaker.writeMultipageTIFF(rout, frames);
					TIFFWriter writer = new TIFFWriter();
					List<IFD> ifds = new ArrayList<IFD>();
					int writeOffset = TIFFTweaker.prepareForWrite(rout);
					for(int i = 0; i < frames.length; i++) {
						writeOffset = TIFFTweaker.writePage(frames[i], rout, ifds, writeOffset, writer);
					}
					TIFFTweaker.finishWrite(rout, ifds);
				} else {
					// This one line test one time insert using insertPages
					//TIFFTweaker.insertPages(rin, rout, 0, frames);
					// The following lines test insert pages each at a time
					long t1 = System.currentTimeMillis();
					List<IFD> list = new ArrayList<IFD>();
					int offset = TIFFTweaker.prepareForInsert(rin, rout, list);
					int index = 3;
					TIFFWriter writer = new TIFFWriter();
					writer.setImageParam(frames[0].getFrameParam());
					for(int i = 0; i < frames.length; i++) {
						offset = TIFFTweaker.insertPage(frames[i].getFrame(), index+=2, rout, list, offset, writer);
					}
					int nColors = 2;
					byte[] reds   = new byte[]{0,(byte)255};
					byte[] greens = new byte[]{0,(byte)255};
					byte[] blues  = new byte[]{0,(byte)255};
					int width = 400; // Dimensions of the image
					int height = 400;
					// Let's create a IndexColorModel for this image.
					IndexColorModel colorModel = new IndexColorModel(1, nColors, reds, greens, blues);
					// Let's create a BufferedImage for an indexed color image.
					BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY, colorModel);
					// We need its raster to set the pixels' values.
					WritableRaster raster = im.getRaster();
					// Put the pixels on the raster. Note that only values 0 and 1 are used for the pixels.
					for(int h = 0; h < height; h++)
						for(int w = 0; w < width; w++)
							if (((h/50)+(w/50)) % 2 == 0) raster.setSample(w, h, 0, 0); // checkerboard pattern.
							else raster.setSample(w, h, 0, 1);
					writer.setImageParam(frames[2].getFrameParam());
					TIFFTweaker.insertPage(im, 0, rout, list, offset, writer);
					TIFFTweaker.finishInsert(rout, list);
					long t2 = System.currentTimeMillis();
					logger.info("time used: {}ms", (t2-t1));
				}
				rout.close();
				fout.close();
			} else if(args[1].equalsIgnoreCase("splitpage")) {
				TIFFTweaker.splitPages(rin, FileUtils.getNameWithoutExtension(new File(args[0])));
			} else if(args[1].equalsIgnoreCase("splitpagebytes")) {
				TIFFTweaker.splitPages(rin, new ArrayList<byte[]>());
			}
			else if(args[1].equalsIgnoreCase("insertexif")) {
				fout = new FileOutputStream("EXIF.tif");
				rout = new FileCacheRandomAccessOutputStream(fout);
				TIFFTweaker.insertExif(rin, rout, populateExif(), true);
				rout.close();
				fout.close();
			} else if(args[1].equalsIgnoreCase("insertcomments")) {
				fout = new FileOutputStream("comments-inserted.tif");
				rout = new FileCacheRandomAccessOutputStream(fout);
				TIFFTweaker.insertComments(Arrays.asList("Comment1", "Comment2"), rin, rout);
				rout.close();
				fout.close();
			} else if(args[1].equalsIgnoreCase("removepage")) {
				int pageCount = TIFFTweaker.getPageCount(rin);
				fout = new FileOutputStream("NEW.tif");
				rout = new FileCacheRandomAccessOutputStream(fout);
				if(pageCount > 1)
					TIFFTweaker.removePages(rin, rout, 0, 1, 1, 1, 5, 5, 4, 100, -100);
				else
					TIFFTweaker.copyCat(rin, rout);
				rout.close();
				fout.close();
			}
		}
		
		rin.close();
	}
	
	// This method is for testing only
	private static Exif populateExif() throws Exception {
		// Create an EXIF wrapper
		Exif exif = new TiffExif();		
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		exif.addExifField(ExifTag.EXPOSURE_TIME, FieldType.RATIONAL, new int[] {10, 600});
		exif.addExifField(ExifTag.FNUMBER, FieldType.RATIONAL, new int[] {49, 10});
		exif.addExifField(ExifTag.ISO_SPEED_RATINGS, FieldType.SHORT, new short[]{273});
		//All four bytes should be interpreted as ASCII values - represents [0220]
		exif.addExifField(ExifTag.EXIF_VERSION, FieldType.UNDEFINED, new byte[]{48, 50, 50, 48});
		exif.addExifField(ExifTag.DATE_TIME_ORIGINAL, FieldType.ASCII, formatter.format(new Date()));
		exif.addExifField(ExifTag.DATE_TIME_DIGITIZED, FieldType.ASCII, formatter.format(new Date()));
		exif.addExifField(ExifTag.FOCAL_LENGTH, FieldType.RATIONAL, new int[] {240, 10});
		
		return exif;
	}
}