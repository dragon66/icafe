package cafe.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import cafe.image.ImageIO;
import cafe.image.core.ImageMeta;
import cafe.image.core.ImageType;
import cafe.image.gif.GIFTweaker;
import cafe.image.options.JPEGOptions;
import cafe.image.options.PNGOptions;
import cafe.image.options.TIFFOptions;
import cafe.image.png.Filter;
import cafe.image.tiff.TiffFieldEnum.Compression;
import cafe.image.writer.ImageWriter;
import cafe.util.FileUtils;

public class TestGIFTweaker {

	public TestGIFTweaker() {}

	public static void main(String[] args) throws Exception {
		FileOutputStream fout = new FileOutputStream("NEW.gif");
		
		File[] files = FileUtils.listFilesMatching(new File(args[1]), args[2]);
		
		BufferedImage[] images = new BufferedImage[files.length];
		int[] delays = new int[images.length];
		
		for(int i = 0; i < files.length; i++) {
			FileInputStream fin = new FileInputStream(files[i]);
			BufferedImage image = javax.imageio.ImageIO.read(fin);
			images[i] = image;
			delays[i] = 100;
			fin.close();
		}
		
		long t1 = System.currentTimeMillis();
		GIFTweaker.writeAnimatedGIF(images, delays, fout);
		long t2 = System.currentTimeMillis();
		System.out.println("time used: " + (t2-t1) + "ms");
		
		ImageWriter writer = ImageIO.getWriter(ImageType.GIF);
		ImageType imageType = writer.getImageType();
		ImageMeta.ImageMetaBuilder builder = new ImageMeta.ImageMetaBuilder();
		  
		switch(imageType) {
			case TIFF:// Set TIFF-specific options
		  		 TIFFOptions tiffOptions = new TIFFOptions();
		  		 tiffOptions.setApplyPredictor(true);
		  		 tiffOptions.setTiffCompression(Compression.LZW);
		  		 tiffOptions.setJPEGQuality(60);
		  		 tiffOptions.setDeflateCompressionLevel(6);
		  		 builder.imageOptions(tiffOptions);
		  		 break;
		  	case PNG:
		  		PNGOptions pngOptions = new PNGOptions();
		  		pngOptions.setApplyAdaptiveFilter(true);
		  		pngOptions.setCompressionLevel(6);
		  		pngOptions.setFilterType(Filter.NONE);
		  		builder.imageOptions(pngOptions);
		  		break;
		  	case JPG:
		  		JPEGOptions jpegOptions = new JPEGOptions();
		  		jpegOptions.setQuality(90);
		  		builder.imageOptions(jpegOptions);
		  		break;
		  	default:
		}
		  
		writer.setImageMeta(builder.indexedColor(false).grayscale(false).bilevel(false).applyDither(true).ditherThreshold(18).hasAlpha(true).build());
		
		FileInputStream is = new FileInputStream(args[0]);
		
		GIFTweaker.splitFramesEx(is, writer, "bufferfly");
		
		is.close();
	}
}