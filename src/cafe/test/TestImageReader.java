package cafe.test;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PushbackInputStream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import cafe.image.ColorType;
import cafe.image.ImageIO;
import cafe.image.ImageMeta;
import cafe.image.ImageType;
import cafe.image.options.JPEGOptions;
import cafe.image.options.PNGOptions;
import cafe.image.options.TIFFOptions;
import cafe.image.png.Filter;
import cafe.image.reader.ImageReader;
import cafe.image.tiff.TiffFieldEnum.PhotoMetric;
import cafe.image.tiff.TiffFieldEnum.Compression;

/**
 * Temporary class for testing image readers
 */
public class TestImageReader {

	 public static void main(String args[]) throws Exception
	 {
		 System.setProperty("debug", args[1]);
		 
		 long t1 = System.currentTimeMillis();
		 
		 FileInputStream fin = new FileInputStream(new File(args[0]));
		 PushbackInputStream pushBackStream = new PushbackInputStream(fin, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		 ImageReader reader = ImageIO.getReader(pushBackStream);
		 BufferedImage img = reader.read(pushBackStream);
		 
		 pushBackStream.close();
		 
		 System.out.println("Total frames read: " + reader.getFrameCount());
		
		 System.out.println(img.getColorModel());
		 System.out.println(img.getRaster());
		 System.out.println(img.getSampleModel());
		
		 long t2 = System.currentTimeMillis();
		 
		 System.out.println("decoding time "+(t2-t1)+"ms");
			
		 final JFrame jframe = new JFrame("Image Reader");

		 jframe.addWindowListener(new WindowAdapter(){
			 public void windowClosing(WindowEvent evt)
			 {
				 jframe.dispose();
				 System.exit(0);
			 }
		 });
		  
		 ImageType imageType = ImageType.JPG;
		  
		 FileOutputStream fo = new FileOutputStream("NEW." + imageType.getExtension());
				
		 ImageMeta.ImageMetaBuilder builder = new ImageMeta.ImageMetaBuilder();
		  
		 switch(imageType) {
		  	case TIFF:// Set TIFF-specific options
		  		 TIFFOptions tiffOptions = new TIFFOptions();
		  		 tiffOptions.setApplyPredictor(true);
		  		 tiffOptions.setTiffCompression(Compression.JPG);
		  		 tiffOptions.setJPEGQuality(60);
		  		 tiffOptions.setPhotoMetric(PhotoMetric.SEPARATED);
		  		 tiffOptions.setWriteICCProfile(true);
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
		  		jpegOptions.setQuality(60);
		  		jpegOptions.setColorSpace(JPEGOptions.COLOR_SPACE_YCCK);
		  		jpegOptions.setWriteICCProfile(true);
		  		builder.imageOptions(jpegOptions);
		  		break;
		  	default:
		 }
		  
		 t1 = System.currentTimeMillis();
		 ImageIO.write(img, fo, imageType, builder.colorType(ColorType.FULL_COLOR).applyDither(true).ditherThreshold(18).hasAlpha(true).build());			
		 t2 = System.currentTimeMillis();
		
		 fo.close();
		
		 System.out.println(imageType + " writer "+ "(encoding time "+(t2-t1)+"ms)");
		
		 JLabel theLabel = new JLabel(new ImageIcon(img));
		 jframe.getContentPane().add(new JScrollPane(theLabel));
		 jframe.setSize(400,400);
		 jframe.setVisible(true);
	 }
}