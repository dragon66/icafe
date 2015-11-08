/**
 * Copyright (c) 2014-2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * Metadata.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    26Sep2015  Added insertComment(InputStream, OutputStream, String)
 * WY    06Jul2015  Added insertXMP(InputSream, OutputStream, XMP)
 * WY    16Apr2015  Changed insertIRB() parameter List to Collection
 * WY    03Mar2015  Added insertXMP()
 * WY    03Feb2015  Added insertExif()
 * WY    03Feb2015  Added removeExif()
 * WY    03Feb2015  Added insertICCProfile()
 * WY    27Jan2015  Added insertIRB()
 * WY    26Jan2015  Added insertIPTC()
 * WY    25Jan2015  Added extractThumbnails()
 */

package com.icafe4j.image.meta;

import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.ImageIO;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.bmp.BMPTweaker;
import com.icafe4j.image.gif.GIFTweaker;
import com.icafe4j.image.jpeg.JPEGTweaker;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.MetadataType;
import com.icafe4j.image.meta.adobe.XMP;
import com.icafe4j.image.meta.adobe._8BIM;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.iptc.IPTCDataSet;
import com.icafe4j.image.png.PNGTweaker;
import com.icafe4j.image.tiff.TIFFTweaker;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.FileCacheRandomAccessOutputStream;
import com.icafe4j.io.PeekHeadInputStream;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.io.RandomAccessOutputStream;

/**
 * Base class for image metadata.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/12/2015
 */
public abstract class Metadata implements MetadataReader {
	public static final int IMAGE_MAGIC_NUMBER_LEN = 4;
	// Fields
	private MetadataType type;
	protected byte[] data;
	protected boolean isDataRead;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Metadata.class);		
	
	public static void  extractThumbnails(File image, String pathToThumbnail) throws IOException {
		FileInputStream fin = new FileInputStream(image);
		extractThumbnails(fin, pathToThumbnail);
		fin.close();
	}
	
	public static void extractThumbnails(InputStream is, String pathToThumbnail) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate thumbnail extracting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.extractThumbnails(peekHeadInputStream, pathToThumbnail);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				TIFFTweaker.extractThumbnail(randIS, pathToThumbnail);
				randIS.shallowClose();
				break;
			case PNG:
				LOGGER.info("PNG image format does not contain any thumbnail");
				break;
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not contain any thumbnails", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("Thumbnail extracting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void extractThumbnails(String image, String pathToThumbnail) throws IOException {
		extractThumbnails(new File(image), pathToThumbnail);
	}
	
	public static void insertComment(InputStream is, OutputStream os, String comment) throws IOException {
		insertComments(is, os, Arrays.asList(comment));
	}
	
	public static void insertComments(InputStream is, OutputStream os, List<String> comments) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate IPTC inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.insertComments(peekHeadInputStream, os, comments);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFTweaker.insertComments(comments, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
				PNGTweaker.insertComments(peekHeadInputStream, os, comments);
				break;
			case GIF:
				GIFTweaker.insertComments(peekHeadInputStream, os, comments);
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support comment data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("comment data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertExif(InputStream is, OutputStream os, Exif exif) throws IOException {
		insertExif(is, os, exif, false);
	}
	
	/**
	 * @param is input image stream 
	 * @param os output image stream
	 * @param exif Exif instance
	 * @param update True to keep the original data, otherwise false
	 * @throws IOException 
	 */
	public static void insertExif(InputStream is, OutputStream os, Exif exif, boolean update) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate EXIF inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.insertExif(peekHeadInputStream, os, exif, update);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFTweaker.insertExif(randIS, randOS, exif, update);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case GIF:
			case PCX:
			case TGA:
			case BMP:
			case PNG:
				LOGGER.info("{} image format does not support EXIF data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("EXIF data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertICCProfile(InputStream is, OutputStream out, ICC_Profile icc_profile) throws IOException {
		insertICCProfile(is, out, icc_profile.getData());
	}
	
	public static void insertICCProfile(InputStream is, OutputStream out, byte[] icc_profile) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate ICCP inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.insertICCProfile(peekHeadInputStream, out, icc_profile);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFTweaker.insertICCProfile(icc_profile, 0, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support ICCProfile data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("ICCProfile data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}

	public static void insertIPTC(InputStream is, OutputStream out, Collection<IPTCDataSet> iptcs) throws IOException {
		insertIPTC(is, out, iptcs, false);
	}
	
	public static void insertIPTC(InputStream is, OutputStream out, Collection<IPTCDataSet> iptcs, boolean update) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate IPTC inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.insertIPTC(peekHeadInputStream, out, iptcs, update);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFTweaker.insertIPTC(randIS, randOS, iptcs, update);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support IPTC data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("IPTC data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertIRB(InputStream is, OutputStream out, Collection<_8BIM> bims) throws IOException {
		insertIRB(is, out, bims, false);
	}
	
	public static void insertIRB(InputStream is, OutputStream out, Collection<_8BIM> bims, boolean update) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate IRB inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.insertIRB(peekHeadInputStream, out, bims, update);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFTweaker.insertIRB(randIS, randOS, bims, update);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support IRB data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("IRB data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertIRBThumbnail(InputStream is, OutputStream out, BufferedImage thumbnail) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate IRB thumbnail inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.insertIRBThumbnail(peekHeadInputStream, out, thumbnail);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFTweaker.insertThumbnail(randIS, randOS, thumbnail);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support IRB thumbnail", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("IRB thumbnail inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertXMP(InputStream is, OutputStream out, XMP xmp) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate XMP inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.insertXMP(peekHeadInputStream, out, xmp); // No ExtendedXMP
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFTweaker.insertXMP(xmp, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
				PNGTweaker.insertXMP(peekHeadInputStream, out, xmp);
				break;
			case GIF:
				GIFTweaker.insertXMPApplicationBlock(peekHeadInputStream, out, xmp);
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support XMP data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("XMP inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertXMP(InputStream is, OutputStream out, String xmp) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate XMP inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.insertXMP(peekHeadInputStream, out, xmp, null); // No ExtendedXMP
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFTweaker.insertXMP(xmp, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
				PNGTweaker.insertXMP(peekHeadInputStream, out, xmp);
				break;
			case GIF:
				GIFTweaker.insertXMPApplicationBlock(peekHeadInputStream, out, xmp);
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support XMP data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("XMP inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static Map<MetadataType, Metadata> readMetadata(File image) throws IOException {
		FileInputStream fin = new FileInputStream(image);
		Map<MetadataType, Metadata> metadataMap = readMetadata(fin);
		fin.close();
		
		return metadataMap; 
	}
	
	/**
	 * Reads all metadata associated with the input image
	 *
	 * @param is InputStream for the image
	 * @return a list of Metadata for the input stream
	 * @throws IOException
	 */
	public static Map<MetadataType, Metadata> readMetadata(InputStream is) throws IOException {
		// Metadata map for all the Metadata read
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate metadata reading to corresponding image tweakers.
		switch(imageType) {
			case JPG:
				metadataMap = JPEGTweaker.readMetadata(peekHeadInputStream);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				metadataMap = TIFFTweaker.readMetadata(randIS);
				randIS.shallowClose();
				break;
			case PNG:
				metadataMap = PNGTweaker.readMetadata(peekHeadInputStream);
				break;
			case GIF:
				metadataMap = GIFTweaker.readMetadata(peekHeadInputStream);
				break;
			case BMP:
				metadataMap = BMPTweaker.readMetadata(peekHeadInputStream);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("Metadata reading is not supported for " + imageType + " image");
				
		}	
		peekHeadInputStream.shallowClose();
		
		return metadataMap;
	}
	
	public static Map<MetadataType, Metadata> readMetadata(String image) throws IOException {
		return readMetadata(new File(image));
	}
	
	/**
	 * Remove meta data from image
	 * 
	 * @param is InputStream for the input image
	 * @param os OutputStream for the output image
	 * @throws IOException
	 */
	public static void removeMetadata(InputStream is, OutputStream os, MetadataType ...metadataTypes) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, ImageIO.IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = IMGUtils.guessImageType(peekHeadInputStream);		
		// Delegate meta data removing to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPEGTweaker.removeMetadata(peekHeadInputStream, os, metadataTypes);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFTweaker.removeMetadata(randIS, randOS, metadataTypes);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support meta data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("Metadata removing is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public Metadata(MetadataType type, byte[] data) {
		this.type = type;
		this.data = data;
	}
	
	protected void ensureDataRead() {
		if(!isDataRead) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public byte[] getData() {
		if(data != null)
			return data.clone();
		
		return null;
	}
	
	public MetadataType getType() {
		return type;
	}
	
	public boolean isDataRead() {
		return isDataRead;
	}
	
	public abstract void showMetadata();
	
	/**
	 * Writes the metadata out to the output stream
	 * 
	 * @param out OutputStream to write the metadata to
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		byte[] data = getData();
		if(data != null)
			out.write(data);
	}	
}