/**
 * Copyright (c) 2014 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package cafe.image;

import java.util.Map;
import java.util.HashMap;

import cafe.image.reader.*;
import cafe.image.writer.*;

/**
 * Image types supported by ImageReader and ImageWriter.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/09/2012
 * @see cafe.image.reader.ImageReader
 * @see cafe.image.writer.ImageWriter
 */
public enum ImageType {
	
	GIF("Gif") { 
		@Override
		public String getExtension() {
			return "gif";
		}
		@Override
		public ImageReader getReader() { 
			return new GIFReader(); 
		}
		@Override
		public ImageWriter getWriter() {
			return new GIFWriter(); 
		}
	},
	
    PNG("Png") { 
		@Override
		public String getExtension() {
			return "png";
		}
		@Override
		public ImageReader getReader() { 
			return new PNGReader(); 
		}
		@Override
		public ImageWriter getWriter() { 
			return new PNGWriter();
        }
	},
	
    JPG("Jpeg") { 
		@Override
		public String getExtension() {
			return "jpg";
		}
		@Override
		public ImageReader getReader() { 
			return new JPEGReader(); 
		}
		@Override
		public ImageWriter getWriter() { 
			return new JPEGWriter(); 
		}
	},
	
    BMP("Bitmap") { 
		@Override
		public String getExtension() {
			return "bmp";
		}
		@Override
		public ImageReader getReader() {
            return new BMPReader(); 
        }
		@Override
		public ImageWriter getWriter() { 
			return new BMPWriter(); 
		}
	},
	
    TGA("Targa") { 
		@Override
		public String getExtension() {
			return "tga";
		}
		@Override
		public ImageReader getReader() { 
			return new TGAReader();
		}
		@Override
		public ImageWriter getWriter() {
			throw new UnsupportedOperationException(
				"TGA writer is not implemented."
			); 
		}
	},
	
	TIFF("Tiff") { 
		@Override
		public String getExtension() {
			return "tif";
		}
		@Override
		public ImageReader getReader() { 
			return new TIFFReader(); 
		}
		@Override
		public ImageWriter getWriter() { 
			return new TIFFWriter(); 
		}		
	},
	
    PCX("Pcx") { 
		@Override
		public String getExtension() {
			return "pcx";
		}
		@Override
		public ImageReader getReader() { 
			return new PCXReader(); 
		}
		@Override
		public ImageWriter getWriter() { 
			throw new UnsupportedOperationException(
				"PCX writer is not implemented."
			);
		}
	};
    
    private static final Map<String, ImageType> stringMap = new HashMap<String, ImageType>();
   
    static
    {
      for(ImageType type : values())
          stringMap.put(type.toString(), type);
    }
   
    public static ImageType fromString(String name)
    {
      return stringMap.get(name);
    }
   
    private final String name;
   
    private ImageType(String name)
    {
      this.name = name;
    }
    
    public abstract String getExtension();
    public abstract ImageReader getReader();
    public abstract ImageWriter getWriter();
    
    @Override
    public String toString()
    {
      return name;
    }
}
