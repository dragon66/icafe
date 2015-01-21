package cafe.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import cafe.image.ImageIO;
import cafe.image.ImageParam;
import cafe.image.ImageType;
import cafe.image.gif.GIFFrame;
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
		
		GIFFrame[] images = new GIFFrame[files.length];
		
		for(int i = 0; i < files.length; i++) {
			FileInputStream fin = new FileInputStream(files[i]);
			BufferedImage image = javax.imageio.ImageIO.read(fin);
			images[i] = new GIFFrame(image, 100, GIFFrame.DISPOSAL_RESTORE_TO_BACKGROUND);
			fin.close();
		}
		
		long t1 = System.currentTimeMillis();
		GIFTweaker.writeAnimatedGIF(images, 0, fout);
		long t2 = System.currentTimeMillis();
		System.out.println("time used: " + (t2-t1) + "ms");
		
		ImageWriter writer = ImageIO.getWriter(ImageType.GIF);
		ImageType imageType = writer.getImageType();
		ImageParam.ImageParamBuilder builder = new ImageParam.ImageParamBuilder();
		  
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
		  
		writer.setImageParam(builder.applyDither(true).ditherThreshold(18).hasAlpha(true).build());
		
		FileInputStream is = new FileInputStream(args[0]);
		
		GIFTweaker.splitFramesEx2(is, writer, "split");
		
		is.close();
	}
}