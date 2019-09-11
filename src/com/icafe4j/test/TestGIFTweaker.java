package com.icafe4j.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

import com.icafe4j.image.ImageIO;
import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.gif.GIFFrame;
import com.icafe4j.image.gif.GIFTweaker;
import com.icafe4j.image.options.JPGOptions;
import com.icafe4j.image.options.PNGOptions;
import com.icafe4j.image.options.TIFFOptions;
import com.icafe4j.image.png.Filter;
import com.icafe4j.image.tiff.TiffFieldEnum.Compression;
import com.icafe4j.image.writer.GIFWriter;
import com.icafe4j.image.writer.ImageWriter;
import com.icafe4j.util.FileUtils;

public class TestGIFTweaker extends TestBase {

	public TestGIFTweaker() {}

	public static void main(String[] args) throws Exception {
		new TestGIFTweaker().test(args);
	}
	
	public void test(String ... args) throws Exception {
		FileOutputStream fout = new FileOutputStream("NEW.gif");
		
		File[] files = FileUtils.listFilesMatching(new File(args[1]), args[2]);
		
		GIFFrame[] images = new GIFFrame[files.length];
		
		for(int i = 0; i < files.length; i++) {
			FileInputStream fin = new FileInputStream(files[i]);
			BufferedImage image = javax.imageio.ImageIO.read(fin);
			images[i] = new GIFFrame(image, 100, GIFFrame.DISPOSAL_RESTORE_TO_BACKGROUND);
			fin.close();
		}
		
		ImageParam.ImageParamBuilder builder = ImageParam.getBuilder();
		
		long t1 = System.currentTimeMillis();
		// Uncomment the following line to write frames all at once
		// GIFTweaker.writeAnimatedGIF(images, 0, fout);
		// Comment out the following lines if writing frames all at once
		// Start writing animated GIF frame by frame to save memory
		GIFWriter animatedGIFWriter = new GIFWriter();
		animatedGIFWriter.setImageParam(builder.applyDither(true).build());
		// Set logical screen width and height to zero to use first frame width and height 
		GIFTweaker.prepareForWrite(animatedGIFWriter, fout, 0, 0);
		for(int i = 0; i < images.length; i++)
			GIFTweaker.writeFrame(animatedGIFWriter, fout, images[i]);
		// wrap it up
		GIFTweaker.finishWrite(fout);
		// End of writing animated GIF frame by frame
		long t2 = System.currentTimeMillis();
		logger.info("time used: {}ms", (t2-t1));
		
		ImageWriter writer = ImageIO.getWriter(ImageType.GIF);
		ImageType imageType = writer.getImageType();
			  
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
		  		JPGOptions jpegOptions = new JPGOptions();
		  		jpegOptions.setQuality(90);
		  		builder.imageOptions(jpegOptions);
		  		break;
		  	default:
		}
		  
		writer.setImageParam(builder.applyDither(true).hasAlpha(true).build());
		
		FileInputStream is = new FileInputStream(args[0]);
		
		GIFTweaker.splitAnimatedGIF(is, writer, "split");
		
		is.close();
		
		is = new FileInputStream("images/tmp-00.gif");
		fout = new FileOutputStream("tmp-00-comment-inserted.gif");
		GIFTweaker.insertComments(is, fout, Arrays.asList("I am a piggy!", "I can fly!"));
		
		is.close();
		fout.close();
	}
}
