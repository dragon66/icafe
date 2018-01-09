/**
 * COPYRIGHT (C) 2014-2017 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * TIFFWriter.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    06Dec2017  Remove unnecessary T4Options for G3/1D and G4/2D
 * WY    23Nov2017  Fix bug with gray-scale image byte packing
 * WY    22Oct2017  Added compression type check
 * WY    11Dec2016  Added byte order support to TiffOptions
 * WY    16Jun2016  Added code to set resolution
 * WY    05Dec2015  Changed writePage() signature
 * WY    20Sep2015  Added LZW and DEFLATE compression for BW images
 * WY    21Jun2015  Removed copyright notice from generated TIFF images
 * WY    11Jun2015  Fixed the regression bug with CMYK profile path
 * WY    11Jun2015  Updated to use generic Updatable<T> interface
 * WY    22Dec2014  Fixed regression bug when migrating to ImageColorType 
 * WY    05Jun2014  Added support for CMYK image and ICC_Profile
 * WY    24Mar2014  Added JPEG compression for GrayScale image
 * WY    19Mar2014  Added JPEG compression for RGB image
 */

package com.icafe4j.image.writer;

import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.EnumSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.ImageColorType;
import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.compression.ImageEncoder;
import com.icafe4j.image.compression.UnsupportedCompressionException;
import com.icafe4j.image.compression.ccitt.G31DEncoder;
import com.icafe4j.image.compression.ccitt.G32DEncoder;
import com.icafe4j.image.compression.ccitt.G42DEncoder;
import com.icafe4j.image.compression.deflate.DeflateEncoder;
import com.icafe4j.image.compression.lzw.LZWTreeEncoder;
import com.icafe4j.image.compression.packbits.Packbits;
import com.icafe4j.image.options.ImageOptions;
import com.icafe4j.image.options.JPEGOptions;
import com.icafe4j.image.options.TIFFOptions;
import com.icafe4j.image.quant.DitherMethod;
import com.icafe4j.image.tiff.ASCIIField;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.LongField;
import com.icafe4j.image.tiff.RationalField;
import com.icafe4j.image.tiff.ShortField;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.image.tiff.UndefinedField;
import com.icafe4j.image.tiff.TiffFieldEnum.*;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.io.ByteOrder;
import com.icafe4j.io.FileCacheRandomAccessOutputStream;
import com.icafe4j.io.IOUtils;
import com.icafe4j.io.RandomAccessOutputStream;
import com.icafe4j.io.WriteStrategyII;
import com.icafe4j.io.WriteStrategyMM;
import com.icafe4j.util.ArrayUtils;
import com.icafe4j.util.CollectionUtils;
import com.icafe4j.util.Updatable;

/**
 * TIFF image writer.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 11/16/2013
 */
public class TIFFWriter extends ImageWriter implements Updatable<Integer> {
	private static final String pathToCMYKProfile = "/resources/CMYK Profiles/USWebCoatedSWOP.icc";
	// Offset to write image data
	private int stripOffset;
	private IFD ifd;
	
	private TIFFOptions tiffOptions;
	private ICC_ColorSpace cmykColorSpace;
		
	// Lists to hold strip offset and strip bytes count
	private List<Integer> stripOffsets = new ArrayList<Integer>();	
	private List<Integer> stripByteCounts = new ArrayList<Integer>();	
	private RandomAccessOutputStream randomOS;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TIFFWriter.class);
	
	public TIFFWriter() {}
	
	public TIFFWriter(ImageParam param) {
		super(param);
	}
		
	// Predictor for PLANARY_CONFIGURATION value 1
	private static byte[] applyPredictor(int numOfSamples, byte[] input, int imageWidth, int imageHeight) {
		for(int i = imageHeight - 1, inc = numOfSamples*imageWidth, maxVal = inc - numOfSamples, minVal = numOfSamples; i >= 0; maxVal += inc, minVal += inc,  i--) {
			for (int j = maxVal; j >= minVal; j-=numOfSamples) {
				for(int k = 0; k < numOfSamples; k++) {
					input[j + k] -= input[j - numOfSamples + k];
				}
			}			
		}
		
		return input;
	}
	
	// RGB images seems to work better with predictor but not indexed images
	// Predictor for RGB and gray-scale PLANARY_CONFIGURATION value 2 and gray-scale PLANARY_CONFIGURATION value 1 without alpha
	private static byte[] applyPredictor2(byte[] input, int imageWidth, int imageHeight) {
		//
		for(int i = imageHeight - 1, inc = imageWidth, maxVal = inc - 1, minVal = 1; i >= 0; maxVal += inc, minVal += inc,  i--) {
			for (int j = maxVal; j >= minVal; j--) {
				input[j] -= input[j - 1];
			}
		}
		
		return input;
	}
	
	private void ccittCompress(byte[] input, int imageWidth, int imageHeight, ImageEncoder encoder) throws Exception {
		encoder.initialize();
		encoder.encode(input, 0, imageWidth*imageHeight);
		encoder.finish();
		
		TiffField<?> tiffField = new ShortField(TiffTag.ROWS_PER_STRIP.getValue(), new short[]{(short)imageHeight});
		ifd.addField(tiffField);
	}
		
	private void compressSample(byte[] samples, int imageWidth, int imageHeight, Compression compression, int bufferSize) throws Exception {
		// This will make the compression more flexible by allowing different ROWS_PER_STRIP for different compression methods
		int rowsPerStrip = imageHeight;
					
		switch(compression) {
			case LZW:
				// LZW encode the image data
				ImageEncoder encoder = new LZWTreeEncoder(randomOS, 8, bufferSize, this); // 1K buffer		
				encoder.initialize();		
				encoder.encode(samples, 0, samples.length);
				// This will call update
				encoder.finish();
				break;
			case DEFLATE:
			case DEFLATE_ADOBE:
				int compressionLevel = 4;
				if(tiffOptions != null) {
					compressionLevel = tiffOptions.getDeflateCompressionLevel();
				}		
				ImageEncoder deflateEncoder = new DeflateEncoder(randomOS, bufferSize, compressionLevel, this);
				deflateEncoder.initialize();
				deflateEncoder.encode(samples, 0, samples.length);
				// This will call update
				deflateEncoder.finish();
				break;			
			case PACKBITS:
			default:
				compression = Compression.PACKBITS;
				@SuppressWarnings("unused")
				int bytesOut = 0;
				int offset = 0;
				byte[] buffer = new byte[imageWidth + (imageWidth + 127)/128];
				for(int i = 0; i < imageHeight; i++) {
					int tempBytes = Packbits.packbits(ArrayUtils.subArray(samples, offset, imageWidth), buffer);
					offset += imageWidth;			
					randomOS.write(buffer, 0, tempBytes);
					update(tempBytes);
					//bytesOut += tempBytes;			
				}				
				//update(bytesOut);	
				rowsPerStrip = 1;
				break;
		}
		
		// Add ROWS_PER_STRIP field
		TiffField<?> tiffField = new ShortField(TiffTag.ROWS_PER_STRIP.getValue(), new short[]{(short)rowsPerStrip});
		ifd.addField(tiffField);
		
		// Add compression field to IFD
		tiffField = new ShortField(TiffTag.COMPRESSION.getValue(), new short[]{(short)compression.getValue()});
		ifd.addField(tiffField);
	}
	
	private void deflateCompress(int compressionLevel, byte[] inflated, int bitsPerPixel, int imageWidth, int imageHeight, byte[] buffer) throws Exception {
		// Starts first strip
		DeflateEncoder deflateEncoder = new DeflateEncoder(randomOS, buffer.length, compressionLevel, this);
		
		deflateEncoder.initialize();
		
		if(bitsPerPixel == 8) {
			deflateEncoder.encode(inflated, 0, imageWidth*(imageHeight/2+1));
		} else {
			byte[] temp = ArrayUtils.packByteArray(inflated, imageWidth, 0, bitsPerPixel, imageWidth*(imageHeight/2+1));
			deflateEncoder.encode(temp, 0, temp.length);
		}
		
		deflateEncoder.finish();
		
		// Resets to compress next strip
		deflateEncoder.initialize();

		// Starts another strip
		if(bitsPerPixel == 8) {
			deflateEncoder.encode(inflated, imageWidth*(imageHeight/2+1), inflated.length - imageWidth*(imageHeight/2+1));
		} else {
			byte[] temp = ArrayUtils.packByteArray(inflated, imageWidth, imageWidth*(imageHeight/2+1), bitsPerPixel, inflated.length - imageWidth*(imageHeight/2+1));
			deflateEncoder.encode(temp, 0, temp.length);
		}
		
		deflateEncoder.finish();
		
		TiffField<?> tiffField = new ShortField(TiffTag.ROWS_PER_STRIP.getValue(), new short[]{(short)(imageHeight/2 + 1)});
		ifd.addField(tiffField);
	}
	
	/**
	 * Copy the internal IFD
	 * <p>
	 * one of the use case is inserting pages into existing TIFFs.
	 * See for example {@link com.icafe4j.image.tiff.TIFFTweaker#insertPage}
	 * 
	 * @return a read-only version of the internal IFD
	 */
	public IFD getIFD() {
		// Copy IFD using IFD's copy constructor
		// Defensive copy, won't affect this image IFD
		return new IFD(ifd);
	}
	
	@Override
	public ImageType getImageType() {
		return ImageType.TIFF;
	}
		
	private void jpegCompress(int[] pixels, int imageWidth, int imageHeight, boolean grayscale) throws Exception {
		// This will make the compression more flexible by allowing different ROWS_PER_STRIP for different compression methods
		int rowsPerStrip = imageHeight/2 + 1; // Two strips
		int jpegQuality = 90;
		boolean writeICCProfile = false;
		PhotoMetric photoMetric = PhotoMetric.YCbCr;
		
		if(tiffOptions != null) {
			jpegQuality = tiffOptions.getJPEGQuality();
			if(tiffOptions.getPhotoMetric() != PhotoMetric.UNKNOWN) {
				photoMetric = tiffOptions.getPhotoMetric();
			}
			writeICCProfile = tiffOptions.writeICCProfile();
		}
		
		int numOfSamples = 0;
		
		if(grayscale) {
			photoMetric = PhotoMetric.BLACK_IS_ZERO;
			numOfSamples = 1;
		} else if(photoMetric == PhotoMetric.RGB || photoMetric == PhotoMetric.YCbCr) {
			numOfSamples = 3;
			ifd.addField(new RationalField(TiffTag.REFERENCE_BLACK_WHITE.getValue(), new int[]{0, 255, 128, 255, 128, 255}));
			if(photoMetric == PhotoMetric.YCbCr)
				ifd.addField(new ShortField(TiffTag.YCbCr_SUB_SAMPLING.getValue(), new short[]{1, 1}));
		} else if(photoMetric == PhotoMetric.SEPARATED) {
			numOfSamples = 4;
		} else {
			throw new UnsupportedOperationException("Unsupported PHOTOMETRIC_INTERPRETATION!");
		}
		
		short[] bitsPerSample = new short[numOfSamples];
		Arrays.fill(bitsPerSample, (short)8);		
		
		ifd.addField(new ShortField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue(), new short[]{(short)photoMetric.getValue()}));
		ifd.addField(new ShortField(TiffTag.SAMPLES_PER_PIXEL.getValue(), new short[]{(short)numOfSamples}));		
		ifd.addField(new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), bitsPerSample));		
		
		JPEGWriter jpgWriter = new JPEGWriter();
		
		ImageParam.ImageParamBuilder builder = ImageParam.getBuilder();
		
		if(grayscale)
			builder.colorType(ImageColorType.GRAY_SCALE);
		
		JPEGOptions jpegOptions = new JPEGOptions();
		
		jpegOptions.setQuality(jpegQuality);
		jpegOptions.setColorSpace(photoMetric.getValue());
		jpegOptions.setTiffFlavor(true);
		// Tell the JPEGWriter to skip tables (We are going to write them separately)
		jpegOptions.setIncludeTables(false); 
		
		builder.imageOptions(jpegOptions);
		
		jpgWriter.setImageParam(builder.build());
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream(); // No need to close the stream
		jpgWriter.writeDefaultJPEGTables(bout);
		// Add JPEG tables field
		ifd.addField(new UndefinedField(TiffTag.JPEG_TABLES.getValue(), bout.toByteArray()));
		
		// This is amazing. We can actually keep track of how many bytes have been
		// written to the underlying stream by JPEGWriter
		long startOffset = randomOS.getStreamPointer();
		jpgWriter.write(Arrays.copyOfRange(pixels, 0, imageWidth*(imageHeight/2 + 1)), imageWidth, imageHeight/2 + 1, randomOS);
		long finishOffset = randomOS.getStreamPointer();
		
		int totalOut = (int)(finishOffset - startOffset);
		// Update STRIP_OFFSETS and STRIP_BYTE_COUNTS
		update(totalOut);
		
		// Another strip
		startOffset = finishOffset;
		jpgWriter.write(Arrays.copyOfRange(pixels, imageWidth*(imageHeight/2 + 1), pixels.length), imageWidth, imageHeight - imageHeight/2 - 1, randomOS);
		finishOffset = randomOS.getStreamPointer();
		totalOut = (int)(finishOffset - startOffset);
		// Update STRIP_OFFSETS and STRIP_BYTE_COUNTS
		update(totalOut);		
		
		if(photoMetric == PhotoMetric.SEPARATED && writeICCProfile) {
			// Add ICC_Profile field
			byte[] icc_profile = jpgWriter.getCMYK_ICC_Profile();
			if(icc_profile != null) {
				ifd.addField(new UndefinedField(TiffTag.ICC_PROFILE.getValue(), icc_profile));
			}
		}
		
		// Add other fields
		ifd.addField(new ShortField(TiffTag.PLANAR_CONFIGURATTION.getValue(), new short[]{(short)PlanarConfiguration.CONTIGUOUS.getValue()}));
		// Add compression field to IFD
		ifd.addField(new ShortField(TiffTag.COMPRESSION.getValue(), new short[]{(short)Compression.JPG.getValue()}));
		// Add ROWS_PER_STRIP field
		ifd.addField(new ShortField(TiffTag.ROWS_PER_STRIP.getValue(), new short[]{(short)rowsPerStrip}));
	}
	
	private void lzwCompress(byte[] newPixels, int bitsPerPixel, int imageWidth, int imageHeight, int buffSize) throws Exception {
		// LZW encode the image data
		ImageEncoder encoder = new LZWTreeEncoder(randomOS, 8, buffSize, this); // 1K buffer
		
		encoder.initialize();
		
		if(bitsPerPixel == 8) {
			encoder.encode(newPixels, 0, imageWidth*(imageHeight/2+1));
		} else {
			byte[] temp = ArrayUtils.packByteArray(newPixels, imageWidth, 0, bitsPerPixel, imageWidth*(imageHeight/2+1));
			encoder.encode(temp, 0, temp.length);
		}
		
		// This will call update
		encoder.finish();
		
		// Another strip starts		
		encoder.initialize();		
		
		if(bitsPerPixel == 8) {
			encoder.encode(newPixels, imageWidth*(imageHeight/2+1), newPixels.length - imageWidth*(imageHeight/2+1));
		} else {
			byte[] temp = ArrayUtils.packByteArray(newPixels, imageWidth, imageWidth*(imageHeight/2+1), bitsPerPixel, newPixels.length - imageWidth*(imageHeight/2+1));
			encoder.encode(temp, 0, temp.length);
		}
		
		// This will call update when finish encoding
		encoder.finish();
	
		TiffField<?> tiffField = new ShortField(TiffTag.ROWS_PER_STRIP.getValue(), new short[]{(short)(imageHeight/2 + 1)});
		ifd.addField(tiffField);
	}
	
	private void packbitsCompress(byte[] input, int bitsPerPixel, int imageWidth, int imageHeight) throws Exception {
		///////////
		int offset = 0;
		int bytesOut = 0;
		
		int rowsPerStrip = (((imageHeight%2)==0)?(imageHeight/2):(imageHeight/2 + 1));
		byte[] buffer = new byte[imageWidth + (imageWidth + 127)/128];
		
		for(int i = 0; i < imageHeight; i++) {			
			byte[] temp = ArrayUtils.packByteArray(input, imageWidth, offset, bitsPerPixel, imageWidth);
			int tempBytes = Packbits.packbits(temp, buffer);
			offset += imageWidth;		
		
			randomOS.write(buffer, 0, tempBytes);
			bytesOut += tempBytes;
	
			if(i == (rowsPerStrip - 1) || (i == imageHeight - 1)) {
				update(bytesOut);		
				bytesOut = 0;
			}
		}
		
		TiffField<?> tiffField = new ShortField(TiffTag.ROWS_PER_STRIP.getValue(), new short[]{(short)rowsPerStrip});
		ifd.addField(tiffField);
	}
	
	// Reset the writer to start write new page
	private void reset(int offset) {
		stripOffset = offset;
		stripOffsets.clear();
		stripByteCounts.clear();
	}
	
	/**
	 * A call back method used by encoders to update the strip
	 * length for multiple strip TIFF images.
	 * 
	 * @param stripLen compressed strip length for the current strip
	 * @throws IOException
	 */
	public void update(Integer stripLen) {
		stripByteCounts.add(stripLen);
		stripOffsets.add(stripOffset);
		stripOffset += stripLen;
	}
	
	/**
	 * Write a self-contained single page TIFF image
	 */
	@Override
	protected void write(int[] pixels, int imageWidth, int imageHeight,
			OutputStream os) throws Exception {
		// Set image parameters
		ImageParam param = getImageParam();
		ImageOptions options = param.getImageOptions();
		
		if(options instanceof TIFFOptions) {
			tiffOptions = (TIFFOptions)options;
		}
		// Wrap OutputStream with a RandomAccessOutputStream	
		randomOS = new FileCacheRandomAccessOutputStream(os);
		
		ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
		
		if(tiffOptions != null) {
			byteOrder = tiffOptions.getByteOrder();
		}
		
		if(byteOrder == ByteOrder.BIG_ENDIAN) {
			randomOS.setWriteStrategy(WriteStrategyMM.getInstance());
			randomOS.writeShort(IOUtils.BIG_ENDIAN);
		} else {
			randomOS.setWriteStrategy(WriteStrategyII.getInstance());
			randomOS.writeShort(IOUtils.LITTLE_ENDIAN);
		}			
		
		randomOS.writeShort(0x2a); // TIFF identifier
		
		// Single IFD only
		ifd = new IFD();
		TiffField<?> tiffField = new LongField(TiffTag.NEW_SUBFILE_TYPE.getValue(), new int[]{0});
		ifd.addField(tiffField);
		tiffField = new LongField(TiffTag.IMAGE_WIDTH.getValue(), new int[]{imageWidth});
		ifd.addField(tiffField);
		tiffField = new LongField(TiffTag.IMAGE_LENGTH.getValue(), new int[]{imageHeight});
		ifd.addField(tiffField);	
		
		reset(FIRST_WRITE_OFFSET);
		
		randomOS.seek(stripOffset);
		// Write image data
		writePageData(param, pixels, imageWidth, imageHeight);		
		
		// We have done with the strips, now add a new STRIP_OFFSETS field.
		tiffField = new LongField(TiffTag.STRIP_OFFSETS.getValue(), CollectionUtils.integerListToIntArray(stripOffsets));
		ifd.addField(tiffField);
		// and a new STRIP_BYTE_COUNTS field as well
		tiffField = new LongField(TiffTag.STRIP_BYTE_COUNTS.getValue(), CollectionUtils.integerListToIntArray(stripByteCounts));
		ifd.addField(tiffField);
		// Add software field
		String softWare = "ICAFE - https://github.com/dragon66/icafe\0";
		tiffField = new ASCIIField(TiffTag.SOFTWARE.getValue(), softWare);
		ifd.addField(tiffField);
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		tiffField = new ASCIIField(TiffTag.DATETIME.getValue(), formatter.format(new Date()) + '\0');
		ifd.addField(tiffField);
	
		int xResolution = 72;
		int yResolution = 72;
		int resolutionUnit = ResolutionUnit.RESUNIT_INCH.getValue();
		if(tiffOptions != null) {
			xResolution = tiffOptions.getXResolution();
			yResolution = tiffOptions.getYResolution();
			resolutionUnit = tiffOptions.getResolutionUnit().getValue();
		}
		ifd.addField(new RationalField(TiffTag.X_RESOLUTION.getValue(), new int[]{xResolution, 1}));
		ifd.addField(new RationalField(TiffTag.Y_RESOLUTION.getValue(), new int[]{yResolution, 1}));
		ifd.addField(new ShortField(TiffTag.RESOLUTION_UNIT.getValue(), new short[]{(short)resolutionUnit}));		
				
		randomOS.seek(OFFSET_TO_WRITE_FIRST_IFD_OFFSET);
		// Write IFD offset
		randomOS.writeInt(stripOffset);
		// Write IFD
		ifd.write(randomOS, stripOffset);
		// Write to actual stream
		randomOS.seek(STREAM_HEAD);
		randomOS.writeToStream(randomOS.getLength());
		randomOS.close();
	}
	
	private void writeBilevel(byte[] pixels, int imageWidth, int imageHeight, Compression compression) throws Exception {
		// Check Compression type
		EnumSet<Compression> supportedCompressionTypes = Compression.forBilevel();
		
		if(!supportedCompressionTypes.contains(compression)) throw new UnsupportedCompressionException("Bilevel Image only supports the following compression types: " + supportedCompressionTypes);
	
		TiffField<?> tiffField = new ShortField(TiffTag.SAMPLES_PER_PIXEL.getValue(), new short[]{1});
		ifd.addField(tiffField);
		tiffField = new ShortField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue(), new short[]{(short)PhotoMetric.WHITE_IS_ZERO.getValue()});
		ifd.addField(tiffField);	
		tiffField = new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), new short[]{1});
		ifd.addField(tiffField);		
		tiffField = new ShortField(TiffTag.FILL_ORDER.getValue(), new short[] {1});
		ifd.addField(tiffField);
		
		switch(compression) {
			case CCITTRLE:
				ImageEncoder encoder = new G31DEncoder(randomOS, imageWidth, 1024, this);
				ccittCompress(ArrayUtils.packByteArray(pixels, 0, 1, pixels.length), imageWidth, imageHeight, encoder);
				break;
			case CCITTFAX3:
				encoder = new G32DEncoder(randomOS, imageWidth, 1024, 4, this);
				ccittCompress(ArrayUtils.packByteArray(pixels, 0, 1, pixels.length), imageWidth, imageHeight, encoder);
				tiffField = new LongField(TiffTag.T4_OPTIONS.getValue(), new int[] {1}); // 2D coding
				ifd.addField(tiffField);
				break;
			case CCITTFAX4:
				encoder = new G42DEncoder(randomOS, imageWidth, 1024, this);
				ccittCompress(ArrayUtils.packByteArray(pixels, 0, 1, pixels.length), imageWidth, imageHeight, encoder);
				break;
			case LZW:
			case DEFLATE:
				compressSample(ArrayUtils.packByteArray(pixels, imageWidth, 0, 1, pixels.length), imageWidth, imageHeight, compression, 1024);
				break;
			case PACKBITS:
			default:
				compression = Compression.PACKBITS;
				packbitsCompress(pixels, 1, imageWidth, imageHeight);
				break;
		}
		
		tiffField = new ShortField(TiffTag.COMPRESSION.getValue(), new short[]{(short)compression.getValue()});
		ifd.addField(tiffField);	
	}
	
	private void writeGrayScale(byte[] newPixels, int imageWidth, int imageHeight, Compression compression, boolean hasAlpha) throws Exception {		
		// Check compression type
		EnumSet<Compression> supportedCompressionTypes = Compression.forGrayScale();
		
		if(!supportedCompressionTypes.contains(compression)) throw new UnsupportedCompressionException("GrayScale Image only supports the following compression types: " + supportedCompressionTypes);
		
		// Assume 8 bits first
		int bitsPerPixel = 8;
		boolean applyPredictor = true;
	
		if(tiffOptions != null) {
			applyPredictor = tiffOptions.isApplyPredictor();
		}
		
		switch(compression) {
			case LZW:
			case DEFLATE:			
				break;
			case PACKBITS:
			default:
				// Most TIFF readers don't support PACKBITS with predictor and one or the other combination with 
				// PLANARY_CONFIGURATION, so we'd rather play it safe to disable predictor for PACKBITS compression 
				applyPredictor = false;
				break;		
		}
		
		boolean noAlpha = !hasAlpha;
		
		if(noAlpha) {
			// Get the actual bits needed to represent this gray-scale image
			bitsPerPixel = IMGUtils.getBitDepth(newPixels, false);;
			// TIFF only allows for 4 and 8 bits gray-scale image
			switch(bitsPerPixel) {
				case 1:
				case 2:
				case 3:
					bitsPerPixel = 4;
					break;
				case 5:
				case 6:
				case 7:
					bitsPerPixel = 8;
					break;
				default:
			}
		}		
		
		// Scale input if needed
		if(bitsPerPixel != 8) { // It must be 4 here
			for(int l = 0; l < newPixels.length; l++) {
				newPixels[l] = (byte)((newPixels[l]<<bitsPerPixel)>>8);
			}
			// Pack newPixels according to bitsPerPixel value
			newPixels = ArrayUtils.packByteArray(newPixels, imageWidth, 0, bitsPerPixel, imageWidth*imageHeight);
		}
		
		TiffField<?> tiffField = new ShortField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue(), new short[]{(short)PhotoMetric.BLACK_IS_ZERO.getValue()});
		ifd.addField(tiffField);
		
		int samplesPerPixel = (noAlpha?1:2);
		
		ifd.addField(new ShortField(TiffTag.SAMPLES_PER_PIXEL.getValue(), new short[]{(short)samplesPerPixel}));
			
		// Create data for the strip
		if(noAlpha) {			
			ifd.addField(new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), new short[]{(short)bitsPerPixel}));
		} else {
			ifd.addField(new ShortField(TiffTag.EXTRA_SAMPLES.getValue(), new short[]{2}));
			ifd.addField(new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), new short[]{(short)bitsPerPixel, (short)bitsPerPixel}));
		}
		
		if(bitsPerPixel == 8) {
			// Apply predictor if needed
			if(applyPredictor) {
				if(noAlpha) {
					applyPredictor2(newPixels, imageWidth, imageHeight);
				} else {
					for(int i = imageHeight - 1, inc = 2*imageWidth, maxVal = inc - 2, minVal = 2; i >= 0; maxVal += inc, minVal += inc,  i--) {
						for (int j = maxVal; j >= minVal; j-=2) {
							newPixels[j] -= newPixels[j - 2];
							newPixels[j+1] -= newPixels[j - 1];
						}						
					}
				}
				tiffField = new ShortField(TiffTag.PREDICTOR.getValue(), new short[]{2});
				ifd.addField(tiffField);
			}
		}
		
		// Now compress the data
		// We pass in samplesPerPixel*imageWidth instead of imageWidth to make PACKBITS work, LZW and DEFLATE don't
		// use imageWidth parameter, so it doesn't matter what value is passed in
		compressSample(newPixels, samplesPerPixel*imageWidth, imageHeight, compression, 1024);
	}
	
	private void writePageData(ImageParam param, int[] pixels, int imageWidth, int imageHeight) throws Exception {
		//
		Compression compression = Compression.PACKBITS;
		
		if(tiffOptions != null) {
			compression = tiffOptions.getTiffCompression();
		}
		// Start writing image data				
		if(param.getColorType() == ImageColorType.INDEXED) {
			writeIndexed(pixels, imageWidth, imageHeight, compression);
		} else if(param.getColorType() == ImageColorType.BILEVEL) {
			if(param.isApplyDither()) {
				byte[] bilevelPixels = null;
				if(param.getDitherMethod() == DitherMethod.FLOYD_STEINBERG)
					bilevelPixels = IMGUtils.rgb2bilevelDiffusionDither(pixels, imageWidth, imageHeight);
				else
					bilevelPixels = IMGUtils.rgb2bilevelOrderedDither(pixels, imageWidth, imageHeight, param.getDitherMatrix());
				writeBilevel(bilevelPixels, imageWidth, imageHeight, compression);
			} else 
				writeBilevel(IMGUtils.rgb2bilevel(pixels), imageWidth, imageHeight, compression);
		} else {
			// JPEG is a special case
			if(compression == Compression.JPG) {
				if(param.hasAlpha())
					LOGGER.warn("#Warning: JPEG compression does not support transparency (all transparency information will be lost!)");
				jpegCompress(pixels, imageWidth, imageHeight, param.getColorType() == ImageColorType.GRAY_SCALE);			
			} else {
				if(param.getColorType() == ImageColorType.GRAY_SCALE) {
					if(param.hasAlpha()) {
						writeGrayScale(IMGUtils.rgb2grayscaleA(pixels), imageWidth, imageHeight, compression, true);
					} else {
						writeGrayScale(IMGUtils.rgb2grayscale(pixels), imageWidth, imageHeight, compression, false);
					}
				} else {
					writeTrueColor(pixels, imageWidth, imageHeight, compression);
				}
			}
		}
	}
	
	private void writeIndexed(int[] pixels, int imageWidth, int imageHeight, Compression compression) throws Exception {		
		// Check compression type
		EnumSet<Compression> supportedCompressionTypes = Compression.forIndexed();
		
		if(!supportedCompressionTypes.contains(compression)) throw new UnsupportedCompressionException("Indexed Image only supports the following compression types: " + supportedCompressionTypes);
		
		// Create data for the strip
		byte[] newPixels = new byte[imageWidth*imageHeight];
		int[] colorPalette = new int[256];
		int[] colorInfo = IMGUtils.checkColorDepth(pixels, newPixels, colorPalette);
		int bitsPerPixel = colorInfo[0];
		ImageParam param = getImageParam();
		if(colorInfo[0]>0x08) {
			bitsPerPixel = 8;
			if(param.isApplyDither()) {
				if(param.getDitherMethod() == DitherMethod.FLOYD_STEINBERG)
					colorInfo = IMGUtils.reduceColorsDiffusionDither(param.getQuantMethod(), pixels, imageWidth, imageHeight, bitsPerPixel, newPixels, colorPalette);
				else
					colorInfo = IMGUtils.reduceColorsOrderedDither(param.getQuantMethod(), pixels, imageWidth, imageHeight, bitsPerPixel, newPixels, colorPalette, param.getDitherMatrix());				
			} else
	    		colorInfo = IMGUtils.reduceColors(param.getQuantMethod(), pixels, bitsPerPixel, newPixels, colorPalette);
		}
		
		switch(bitsPerPixel) {
			case 3:
				bitsPerPixel = 4;
				break;
			case 5:
			case 6:
			case 7:
				bitsPerPixel = 8;
				break;
			default:
		}
		
		// See if we are dealing with BW image
		/** Comment out to respect user's compression setting
		if(bitsPerPixel == 1) {
			
			int color0 = colorPalette[0]&0xffffff;
			int color1 = colorPalette[1]&0xffffff;
			
			if((color0^0xffffff)==color1) {//BW image
				if(color0 == 0) { // We are going to write TIFF BW image with WhiteIsZero
					IMGUtils.invertBits(newPixels);
				} 			
				writeBilevel(newPixels, imageWidth, imageHeight, Compression.CCITTFAX4);
				
				return;
			}
		}*/
		
		int numOfColors = (1<<bitsPerPixel);
		int numOfColors2 = (numOfColors<<1);
		// Create color map
		short[] map = new short[3*numOfColors];
				
		for(int i = 0; i < numOfColors; i++) { 
			map[i] = (short)(((colorPalette[i]>>16)&0xff)<<8); // Red elements	
			map[numOfColors+i] = (short)(((colorPalette[i]>>8)&0xff)<<8); // Green elements
			map[numOfColors2+i] = (short)(((colorPalette[i]>>0)&0xff)<<8); // Blue elements
		}
		
		TiffField<?> tiffField = new ShortField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue(), new short[]{(short)PhotoMetric.PALETTE_COLOR.getValue()});
		ifd.addField(tiffField);
		
		tiffField = new ShortField(TiffTag.COLORMAP.getValue(), map);
		ifd.addField(tiffField);
		
		tiffField = new ShortField(TiffTag.SAMPLES_PER_PIXEL.getValue(), new short[]{1});
		ifd.addField(tiffField);
		
		tiffField = new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), new short[]{(short)bitsPerPixel});
		ifd.addField(tiffField);
		
		// Now compress the data
		if(compression == Compression.LZW) {				
			lzwCompress(newPixels, bitsPerPixel, imageWidth, imageHeight, 4096);
		} else if(compression == Compression.DEFLATE_ADOBE || compression == Compression.DEFLATE) {
			byte buffer[] = new byte[4096];
			int compressionLevel = 4;
			if(tiffOptions != null) {
				compressionLevel = tiffOptions.getDeflateCompressionLevel();
			}	
			deflateCompress(compressionLevel, newPixels, bitsPerPixel, imageWidth, imageHeight, buffer);				
		} else {
			compression = Compression.PACKBITS;
			packbitsCompress(newPixels, bitsPerPixel, imageWidth, imageHeight);			
		}
		
		tiffField = new ShortField(TiffTag.COMPRESSION.getValue(), new short[]{(short)compression.getValue()});
		ifd.addField(tiffField);		
	}
	
	/**
	 * Write a single page to TIFF stream.
	 * <p>
	 * Instead of writing a self-contained TIFF image as {@link #write} does,
	 * this method writes a bare-bone single page as part of a multiple page TIFF.
	 * 
	 * @param frame input BufferedImage
	 * 
	 * @param randomOutStream RandomAccessOutputStream
	 * @param offset stream offset to write this page
	 * @return stream offset after writing this page
	 * @throws Exception
	 */
	public int writePage(BufferedImage frame, int pageNumber, int maxNumber,
			RandomAccessOutputStream randomOutStream, int offset) throws Exception {
		// Grab image pixels in ARGB format
		int imageWidth = frame.getWidth();
		int imageHeight = frame.getHeight();
		int[] pixels = IMGUtils.getRGB(frame);//image.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
		// One page of a multiple page TIFF
		ifd = new IFD();
		TiffField<?> tiffField = new LongField(TiffTag.NEW_SUBFILE_TYPE.getValue(), new int[]{2});
		ifd.addField(tiffField);
		tiffField = new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)pageNumber, (short)maxNumber});
		ifd.addField(tiffField);
		tiffField = new LongField(TiffTag.IMAGE_WIDTH.getValue(), new int[]{imageWidth});
		ifd.addField(tiffField);
		tiffField = new LongField(TiffTag.IMAGE_LENGTH.getValue(), new int[]{imageHeight});
		ifd.addField(tiffField);
		
		reset(offset);
		
		randomOS = randomOutStream;
		randomOS.seek(stripOffset);
		
		// Set image parameters
		ImageParam param = getImageParam();
		ImageOptions options = param.getImageOptions();
		
		if(options instanceof TIFFOptions) {
			tiffOptions = (TIFFOptions)options;
		}
		//
		// Write image data
		writePageData(param, pixels, imageWidth, imageHeight);
		 
		// We have done with the strips, now add a new STRIP_OFFSETS field.
		tiffField = new LongField(TiffTag.STRIP_OFFSETS.getValue(), CollectionUtils.integerListToIntArray(stripOffsets));
		ifd.addField(tiffField);
		// and a new STRIP_BYTE_COUNTS field as well
		tiffField = new LongField(TiffTag.STRIP_BYTE_COUNTS.getValue(), CollectionUtils.integerListToIntArray(stripByteCounts));
		ifd.addField(tiffField);
		// Add software field
		String softWare = "ICAFE - https://github.com/dragon66/icafe\0";
		tiffField = new ASCIIField(TiffTag.SOFTWARE.getValue(), softWare);
		ifd.addField(tiffField);
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss z");
		tiffField = new ASCIIField(TiffTag.DATETIME.getValue(), formatter.format(new Date()) + '\0');
		ifd.addField(tiffField);
		int xResolution = 72;
		int yResolution = 72;
		int resolutionUnit = ResolutionUnit.RESUNIT_INCH.getValue();
		if(tiffOptions != null) {
			xResolution = tiffOptions.getXResolution();
			yResolution = tiffOptions.getYResolution();
			resolutionUnit = tiffOptions.getResolutionUnit().getValue();
		}
		ifd.addField(new RationalField(TiffTag.X_RESOLUTION.getValue(), new int[]{xResolution, 1}));
		ifd.addField(new RationalField(TiffTag.Y_RESOLUTION.getValue(), new int[]{yResolution, 1}));
		ifd.addField(new ShortField(TiffTag.RESOLUTION_UNIT.getValue(), new short[]{(short)resolutionUnit}));
		
		// Write IFD
		return ifd.write(randomOS, stripOffset);
	}
	
	private void writeTrueColor(int[] pixels, int imageWidth, int imageHeight, Compression compression) throws Exception {
		// Check compression type
		EnumSet<Compression> supportedCompressionTypes = Compression.forTrueColor();
		
		if(!supportedCompressionTypes.contains(compression)) throw new UnsupportedCompressionException("TrueColor Image only supports the following compression types: " + supportedCompressionTypes);
		
		// Whether or not to include alpha channel
		boolean applyPredictor = true;
		PhotoMetric photoMetric = PhotoMetric.RGB;
		boolean writeICCProfile = false;
		int numOfSamples = 3; // Default for RGB, YCbCr etc
		// Set write parameters
		if(tiffOptions != null) {
			applyPredictor = tiffOptions.isApplyPredictor();
			if(tiffOptions.getPhotoMetric() != PhotoMetric.UNKNOWN)
				photoMetric = tiffOptions.getPhotoMetric();
			if(photoMetric == PhotoMetric.SEPARATED) {
				numOfSamples = 4; // Default for SEPARATED - CMYK
				writeICCProfile = tiffOptions.writeICCProfile();
			}
		}		
		switch(compression) {
			case LZW:
			case DEFLATE:
				break;
			case PACKBITS:
			default:
				// Most TIFF readers don't support PACKBITS with predictor and one or the other combination with 
				// PLANARY_CONFIGURATION, so we'd rather play it safe to disable predictor for PACKBITS compression 
				applyPredictor = false;
				break;		
		}

		boolean hasAlpha = getImageParam().hasAlpha();

		int samplesPerPixel = (hasAlpha?(numOfSamples+1):numOfSamples);
		ifd.addField(new ShortField(TiffTag.SAMPLES_PER_PIXEL.getValue(), new short[]{(short)samplesPerPixel}));
		short[] bitsPerSample = new short[samplesPerPixel];
		Arrays.fill(bitsPerSample, (short)8);
		ifd.addField(new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), bitsPerSample));
		
		if(hasAlpha)
			ifd.addField(new ShortField(TiffTag.EXTRA_SAMPLES.getValue(), new short[]{2}));
						
		byte[] samples = new byte[samplesPerPixel*pixels.length];
		
		if(photoMetric == PhotoMetric.RGB) {
			if(!hasAlpha) {
				for(int index = 0, i = 0; i < pixels.length; i++) {
					samples[index++] = (byte)((pixels[i]>>16)&0xff);
					samples[index++] = (byte)((pixels[i]>>8)&0xff);
					samples[index++] = (byte)(pixels[i]&0xff);
				}
			} else {
				for(int index = 0, i = 0; i < pixels.length; i++) {
					samples[index++] = (byte)((pixels[i]>>16)&0xff);
					samples[index++] = (byte)((pixels[i]>>8)&0xff);
					samples[index++] = (byte)(pixels[i]&0xff);
					samples[index++] = (byte)((pixels[i]>>24)&0xff);
				}
			}
		} else if (photoMetric == PhotoMetric.SEPARATED) {
			if(cmykColorSpace == null)
				cmykColorSpace = IMGUtils.getICCColorSpace(pathToCMYKProfile);
			samples = IMGUtils.RGB2CMYK(cmykColorSpace, pixels, imageWidth, imageHeight, hasAlpha);
			if(writeICCProfile) {
				// Add ICC_Profile field
				byte[] icc_profile = cmykColorSpace.getProfile().getData();
				if(icc_profile != null) {
					ifd.addField(new UndefinedField(TiffTag.ICC_PROFILE.getValue(), icc_profile));
				}
			}
		} else
			throw new UnsupportedOperationException("Unsupported TiffPhotoMetric: " + photoMetric);
				
		if(applyPredictor) {
			applyPredictor(samplesPerPixel, samples, imageWidth, imageHeight);
			ifd.addField(new ShortField(TiffTag.PREDICTOR.getValue(), new short[]{2}));
		}
		
		// We pass in samplesPerPixel*imageWidth instead of imageWidth to make PACKBITS work, LZW and DEFLATE don't
		// use imageWidth parameter, so it doesn't matter what value is passed in
		compressSample(samples, samplesPerPixel*imageWidth, imageHeight, compression, 1024);
		
		ifd.addField(new ShortField(TiffTag.PLANAR_CONFIGURATTION.getValue(), new short[]{(short)PlanarConfiguration.CONTIGUOUS.getValue()}));
		ifd.addField(new ShortField(TiffTag.PHOTOMETRIC_INTERPRETATION.getValue(), new short[]{(short)photoMetric.getValue()}));

		/*
		 * PLANAR_CONFIGURATION type 2 only
		 * 
		byte[] reds = new byte[pixels.length];
		byte[] greens = new byte[pixels.length];
		byte[] blues = new byte[pixels.length];
		
		for(int i = 0; i < pixels.length; i++) {
			reds[i] = (byte)((pixels[i]>>16)&0xff);
			greens[i] = (byte)((pixels[i]>>8)&0xff);
			blues[i] = (byte)(pixels[i]&0xff);
		}
		
		compressSample(applyPredictor2(reds, imageWidth, imageHeight), imageWidth, imageHeight, compression, 1024);
		compressSample(applyPredictor2(greens, imageWidth, imageHeight), imageWidth, imageHeight, compression, 1024);
		compressSample(applyPredictor2(blues, imageWidth, imageHeight), imageWidth, imageHeight, compression, 1024);
		
		tiffField = new ShortField(TiffTag.PLANAR_CONFIGURATTION.getValue(), new short[]{2});
		ifd.addField(tiffField);
		*/
	}
	
	// Offset where to write the value of the first IFD offset
	public static final int OFFSET_TO_WRITE_FIRST_IFD_OFFSET = 0x04;
	public static final int FIRST_WRITE_OFFSET = 0x08;
	public static final int STREAM_HEAD = 0x00;
}