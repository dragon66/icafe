package cafe.image.reader;

import java.awt.image.BufferedImage;
import java.io.InputStream;

public class JPEGReader extends ImageReader {

	@Override
	public BufferedImage read(InputStream is) throws Exception {
		return javax.imageio.ImageIO.read(is);
	}
}
