package cafe.test;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import cafe.image.ImageIO;
import cafe.image.core.ImageMeta;
import cafe.image.core.ImageType;
import cafe.image.options.JPEGOptions;
import cafe.image.options.PNGOptions;
import cafe.image.options.TIFFOptions;
import cafe.image.png.Filter;
import cafe.image.tiff.TiffFieldEnum.PhotoMetric;
import cafe.image.tiff.TiffFieldEnum.Compression;

/**
 * Temporary class for testing image readers
 */
public class TestImageReader {

	 public static void main(String args[]) throws Exception
	 {
		  long t1 = System.currentTimeMillis();
		  BufferedImage img = ImageIO.read(args[0]);
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
		  ImageIO.write(img, fo, imageType, builder.indexedColor(false).grayscale(false).bilevel(false).applyDither(true).ditherThreshold(18).hasAlpha(true).build());			
		  t2 = System.currentTimeMillis();
		
		  fo.close();
		
		  System.out.println(imageType + " writer "+ "(encoding time "+(t2-t1)+"ms)");
		
		  JLabel theLabel = new JLabel(new ImageIcon(img));
		  jframe.getContentPane().add(new JScrollPane(theLabel));
		  jframe.setSize(400,400);
		  jframe.setVisible(true);
	 }
}