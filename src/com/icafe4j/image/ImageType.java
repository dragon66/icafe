/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.image;

import java.util.Map;
import java.util.HashMap;

import com.icafe4j.image.reader.*;
import com.icafe4j.image.writer.*;

/**
 * Image types supported by ImageReader and ImageWriter.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/09/2012
 * @see com.icafe4j.image.reader.ImageReader
 * @see com.icafe4j.image.writer.ImageWriter
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
			return new JPGReader(); 
		}
		@Override
		public ImageWriter getWriter() { 
			return new JPEGWriter(); 
		}
	},
	
	JPG2000("Jpeg2000") {
		@Override
		public String getExtension() {
			return "jp2";
		}
		@Override
		public ImageReader getReader() { 
			throw new UnsupportedOperationException(
					"JPG2000 reader is not implemented."
				);
		}
		@Override
		public ImageWriter getWriter() { 
			throw new UnsupportedOperationException(
					"JPG2000 writer is not implemented."
				); 
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
	},
	
	UNKNOWN("Unknown") {
		@Override
		public String getExtension() {
			return null;
		}

		@Override
		public ImageReader getReader() { 
			throw new UnsupportedOperationException(
					"No reader for unknown image type."
				);
		}
		@Override
		public ImageWriter getWriter() { 
			throw new UnsupportedOperationException(
					"No writer for unknown image type."
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
