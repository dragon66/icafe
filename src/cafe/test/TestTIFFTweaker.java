package cafe.test;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cafe.image.ColorType;
import cafe.image.ImageFrame;
import cafe.image.ImageMeta;
import cafe.image.meta.exif.Exif;
import cafe.image.meta.exif.ExifTag;
import cafe.image.options.TIFFOptions;
import cafe.image.tiff.ASCIIField;
import cafe.image.tiff.RationalField;
import cafe.image.tiff.ShortField;
import cafe.image.tiff.TIFFTweaker;
import cafe.image.tiff.TiffFieldEnum.Compression;
import cafe.image.tiff.IFD;
import cafe.image.tiff.TiffField;
import cafe.image.tiff.UndefinedField;
import cafe.image.writer.TIFFWriter;
import cafe.io.FileCacheRandomAccessInputStream;
import cafe.io.FileCacheRandomAccessOutputStream;
import cafe.io.RandomAccessInputStream;
import cafe.io.RandomAccessOutputStream;
import cafe.util.FileUtils;

public class TestTIFFTweaker {

	public static void main(String[] args) throws Exception {
		RandomAccessInputStream rin = new FileCacheRandomAccessInputStream(new FileInputStream(args[0]));
		RandomAccessOutputStream rout = null;
		
		if(args.length > 1) {			
			if(args[1].equalsIgnoreCase("copycat")) {
				rout = new FileCacheRandomAccessOutputStream(new FileOutputStream("NEW.tif"));
				TIFFTweaker.copyCat(rin, rout);
				rout.close();
			} else if(args[1].equalsIgnoreCase("snoop")) {
				TIFFTweaker.snoop(rin);
			} else if(args[1].equalsIgnoreCase("retainpage")) {
				int pageCount = TIFFTweaker.getPageCount(rin);
				rout = new FileCacheRandomAccessOutputStream(new FileOutputStream("NEW.tif"));
				if(pageCount > 1)
					TIFFTweaker.retainPages(rin, rout, pageCount - 1); // Last page
				else
					TIFFTweaker.copyCat(rin, rout);
				rout.close();
			} else if(args[1].equalsIgnoreCase("writemultipage") || args[1].equalsIgnoreCase("insertpage")) {
				File[] files = FileUtils.listFilesMatching(new File(args[2]), args[3]);
				ImageFrame[] frames = new ImageFrame[files.length];				
				for(int i = 0; i < files.length; i++) {
					FileInputStream fin = new FileInputStream(files[i]);
					BufferedImage image = javax.imageio.ImageIO.read(fin);
					frames[i] = new ImageFrame(image);
					fin.close();
				}				
				
				ImageMeta.ImageMetaBuilder builder = new ImageMeta.ImageMetaBuilder();
				  
				TIFFOptions tiffOptions = new TIFFOptions();
				tiffOptions.setTiffCompression(Compression.LZW);
				tiffOptions.setApplyPredictor(true);
				tiffOptions.setDeflateCompressionLevel(6);
				builder.imageOptions(tiffOptions);
				
				frames[0].setFrameMeta(builder.colorType(ColorType.GRAY_SCALE).applyDither(true).ditherThreshold(18).hasAlpha(true).build());
				
				tiffOptions = new TIFFOptions(tiffOptions); // Copy constructor		
				tiffOptions.setTiffCompression(Compression.DEFLATE);
								
				frames[1].setFrameMeta(builder.imageOptions(tiffOptions).build());
				
				tiffOptions = new TIFFOptions(tiffOptions);				
				tiffOptions.setTiffCompression(Compression.CCITTFAX4);
				
				ImageMeta meta = builder.colorType(ColorType.BILEVEL).ditherThreshold(50).imageOptions(tiffOptions).build();
				
				for(int i = 2; i < frames.length; i++)
					frames[i].setFrameMeta(meta);
				
				rout = new FileCacheRandomAccessOutputStream(new FileOutputStream("NEW.tif"));
				
				if(args[1].equalsIgnoreCase("writemultipage"))
					TIFFTweaker.writeMultipageTIFF(rout, frames);
				else {
					// This one line test one time insert using insertPages
					//TIFFTweaker.insertPages(rin, rout, 0, frames);
					// The following lines test insert pages each at a time
					long t1 = System.currentTimeMillis();
					List<IFD> list = new ArrayList<IFD>();
					int offset = TIFFTweaker.prepareForInsert(rin, rout, list);
					int index = 3;
					TIFFWriter writer = new TIFFWriter();
					writer.setImageMeta(frames[0].getFrameMeta());
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
					writer.setImageMeta(frames[2].getFrameMeta());
					TIFFTweaker.insertPage(im, 0, rout, list, offset, writer);
					TIFFTweaker.finishInsert(rout, list);
					long t2 = System.currentTimeMillis();
					System.out.println("time used: " + (t2-t1) + "ms");
				}
				rout.close();
			} else if(args[1].equalsIgnoreCase("splitpage")) {
				TIFFTweaker.splitPages(rin, FileUtils.getNameWithoutExtension(new File(args[0])));
			} else if(args[1].equalsIgnoreCase("insertexif")) {
				rout = new FileCacheRandomAccessOutputStream(new FileOutputStream("EXIF.tif"));
				TIFFTweaker.insertExif(rin, rout, populateExif());
				rout.close();
			} else if(args[1].equalsIgnoreCase("removepage")) {
				int pageCount = TIFFTweaker.getPageCount(rin);
				rout = new FileCacheRandomAccessOutputStream(new FileOutputStream("NEW.tif"));
				if(pageCount > 1)
					TIFFTweaker.removePages(rin, rout, 0, 1, 1, 1, 5, 5, 4, 100, -100);
				else
					TIFFTweaker.copyCat(rin, rout);
				rout.close();
			}
		}
		
		rin.close();
	}
	
	// This method is for testing only
	private static Exif populateExif() throws Exception {
		// Create an EXIF wrapper
		Exif exif = new Exif(Exif.EXIF_FLAVOR_TIFF);		
		// ExifSubIFD
		IFD exifSubIFD = new IFD();		
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		TiffField<?> tiffField = new RationalField(ExifTag.EXPOSURE_TIME.getValue(), new int[] {10, 600});
		exifSubIFD.addField(tiffField);
		tiffField = new RationalField(ExifTag.FNUMBER.getValue(), new int[] {49, 10});
		exifSubIFD.addField(tiffField);
		tiffField = new ShortField(ExifTag.ISO_SPEED_RATINGS.getValue(), new short[]{273});
		exifSubIFD.addField(tiffField);
		//All four bytes should be interpreted as ASCII values - represents [0220]
		tiffField = new UndefinedField(ExifTag.EXIF_VERSION.getValue(), new byte[]{48, 50, 50, 48});
		exifSubIFD.addField(tiffField);
		tiffField = new ASCIIField(ExifTag.DATE_TIME_ORIGINAL.getValue(), formatter.format(new Date()) + '\0');
		exifSubIFD.addField(tiffField);
		tiffField = new ASCIIField(ExifTag.DATE_TIME_DIGITIZED.getValue(), formatter.format(new Date()) + '\0');
		exifSubIFD.addField(tiffField);
		tiffField = new RationalField(ExifTag.FOCAL_LENGTH.getValue(), new int[] {240, 10});
		exifSubIFD.addField(tiffField);
		// Insert ExifSubIFD
		exif.addExif(exifSubIFD);
		// Insert ThumbNailIFD. We pass in null to tell TIFFTweaker to create a thumbnail from the input stream
		exif.addThumbnail(null);
		
		return exif;
	}
}