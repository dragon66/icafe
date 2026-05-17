/**
 * COPYRIGHT (C) 2014-2019 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History most recent changes go on top of previous changes
 *
 * TIFFReader.java
 *
 * Who Date Description
 * ==== ======= ===========
 * WY 03Jan2018 Fix issue with fillOrder 2
 * WY 07Dec2017 Added support for CCITTRLE compression
 * WY 28Nov2017 Added gray-scale alpha support
 * WY 23Nov2017 Added support for 16 bits gray-scale
 * WY 22Nov2017 Added support for BlackIsZero and WhiteIsZero
 * WY 09Nov2015 Fixed bug with stripped CMYK decoding
 * WY 13Sep2015 Extract unpackStrip() method
 * WY 08Jan2015 Better exception handling to resume from failed frame decoding
 * WY 06Jan2015 Enhancement to decode multipage TIFF
 * WY 10Dec2014 Fixed bug for bitsPerSample%8 != 0 case to assume BIG_ENDIAN
 * WY 09Dec2014 Added support for LSB2MSB FillOrder for RGB image
 * WY 07Dec2014 Added support for floating point sample data type
 * WY 03Dec2014 Code clean and test showing float type raster images
 * WY 01Dec2014 Added support for less than 8 bits planar stripped image
 * WY 28Nov2014 Added support for 32 bits tiled image
 * WY 14Nov2014 Added support for tiled Palette Image
 * WY 07Nov2014 Added support for 16 bit CMYK image
 * WY 06Nov2014 Planar support for stripped YCbCr image
 * WY 05Nov2014 Fixed bug for YCbCr image with wrong image width and height
 * WY 31Oct2014 Added basic support for uncompressed and LZW compressed YCbCr
 * WY 15Oct2014 Added basic support for 16 bits RGB image
 * WY 14Oct2014 Revised to show specification violation TIFF LZW compression
 * WY 14Oct2014 Revised to show RGB TIFF with extra sample tag
 */

package com.icafe4j.image.reader;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.color.CMYKColorSpace;
import com.icafe4j.image.color.Int32ComponentColorModel;
import com.icafe4j.image.compression.ImageDecoder;
import com.icafe4j.image.compression.UnsupportedCompressionException;
import com.icafe4j.image.compression.ccitt.CCITTDecoder;
import com.icafe4j.image.compression.ccitt.G31DDecoder;
import com.icafe4j.image.compression.ccitt.G32DDecoder;
import com.icafe4j.image.compression.ccitt.G42DDecoder;
import com.icafe4j.image.compression.deflate.DeflateDecoder;
import com.icafe4j.image.compression.lzw.LZWTreeDecoder;
import com.icafe4j.image.compression.packbits.Packbits;
import com.icafe4j.image.compression.thunderscan.ThunderScanDecoder;
import com.icafe4j.image.compression.sgilog.SGILogDecoder;
import com.icafe4j.image.tiff.ASCIIField;
import com.icafe4j.image.tiff.ByteField;
import com.icafe4j.image.tiff.SByteField;
import com.icafe4j.image.tiff.DoubleField;
import com.icafe4j.image.tiff.FieldType;
import com.icafe4j.image.tiff.FloatField;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.LongField;
import com.icafe4j.image.tiff.SLongField;
import com.icafe4j.image.tiff.RationalField;
import com.icafe4j.image.tiff.ShortField;
import com.icafe4j.image.tiff.SShortField;
import com.icafe4j.image.tiff.TIFFTweaker;
import com.icafe4j.image.tiff.Tag;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffFieldEnum;
import com.icafe4j.image.tiff.TiffFieldEnum.PhotoMetric;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.image.tiff.UndefinedField;
import com.icafe4j.image.util.IMGUtils;
import com.icafe4j.io.FileCacheRandomAccessInputStream;
import com.icafe4j.io.IOUtils;
import com.icafe4j.io.RandomAccessInputStream;
import com.icafe4j.io.ReadStrategyII;
import com.icafe4j.io.ReadStrategyMM;
import com.icafe4j.string.StringUtils;
import com.icafe4j.util.ArrayUtils;

/**
 * Decodes and shows TIFF images.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/09/2012
 */
public class TIFFReader extends ImageReader {
    private RandomAccessInputStream randIS = null;
    protected List<IFD> ifds;
    private List<BufferedImage> frames;
    private int endian = IOUtils.BIG_ENDIAN;

    private static final int[] redMask = {0x00, 0x04, 0x30, 0x1c0, 0xf00};
    private static final int[] greenMask = {0x00, 0x02, 0x0c, 0x038, 0x0f0};
    private static final int[] blueMask = {0x00, 0x01, 0x03, 0x007, 0x00f};

    private static final int[] BLACK_WHITE_PALETTE = {0xFF000000, 0xFFFFFFFF};
    private static final int[] BLACK_WHITE_PALETTE_WHITE_IS_ZERO = {0xFFFFFFFF, 0xFF000000};
    private static final int[] FOUR_COLOR_PALETTE = {0xFF000000, 0xFF404040, 0xFF808080, 0xFFFFFFFF};
    private static final int[] FOUR_COLOR_PALETTE_WHITE_IS_ZERO = {0xFFFFFFFF, 0xFF808080, 0xFF404040, 0xFF000000};
    private static final int[] SIXTEEN_COLOR_PALETTE = {
        0xFF000000, 0xFF111111, 0xFF222222, 0xFF333333,
        0xFF444444, 0xFF555555, 0xFF666666, 0xFF777777,
        0xFF888888, 0xFF999999, 0xFFAAAAAA, 0xFFBBBBBB,
        0xFFCCCCCC, 0xFFDDDDDD, 0xFFEEEEEE, 0xFFFFFFFF
    };
    private static final int[] SIXTEEN_COLOR_PALETTE_WHITE_IS_ZERO = {
        0xFFFFFFFF, 0xFFEEEEEE, 0xFFDDDDDD, 0xFFCCCCCC,
        0xFFBBBBBB, 0xFFAAAAAA, 0xFF999999, 0xFF888888,
        0xFF777777, 0xFF666666, 0xFF555555, 0xFF444444,
        0xFF333333, 0xFF222222, 0xFF111111, 0xFF000000
    };

    private static final int[] EIGHT_BIT_COLOR_PALETTE = new int[256];
    private static final int[] EIGHT_BIT_COLOR_PALETTE_WHITE_IS_ZERO = new int[256];

    private static int GROUP30PT_2DENCODING = 1; // Bit 0: 2D encoding (Modified READ)
    private static int GROUP30PT_UNCOMPRESSED = 2; // Bit 1: Uncompressed mode
    private static int GROUP30PT_FILLBITS = 4; // Bit 2: Fill bits before EOL

    static {
        for(int i = 0; i < 256; i++) {
            EIGHT_BIT_COLOR_PALETTE[i] = 0xFF000000 | (i << 16) | (i << 8) | (i & 0xff);
        }
    }

    static {
        for(int i = 0; i < 256; i++) {
            EIGHT_BIT_COLOR_PALETTE_WHITE_IS_ZERO[255 - i] = 0xFF000000 | (i << 16) | (i << 8) | (i & 0xff);
        }
    }

    private static final int bufLen = 40960; // 40K read buffer
    private static final Logger LOGGER = LoggerFactory.getLogger(TIFFReader.class);

    protected BufferedImage decode(IFD ifd) throws Exception {
        // Grab some of the TIFF fields we are interested in
        TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH);
        TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH);

        if(f_tileWidth != null && f_tileLength != null) {
            return decodeTiledTiff(ifd);
        }
        return decodeStrippedTiff(ifd);
    }

    private BufferedImage decodeStrippedTiff(IFD ifd) throws Exception {
        // Grab some of the TIFF fields we are interested in
        TiffField<?> f_compression = ifd.getField(TiffTag.COMPRESSION);
        short[] data = new short[] {1}; // Default no compression
        if(f_compression != null) {
            data = (short[])f_compression.getData();
        }
        TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(data[0] & 0xffff);
        LOGGER.info("Compression type: {}", compression.getDescription());

        // Special case: JPEG with JpegInterchangeFormat but no strip offsets
        // This format stores a single complete JPEG image at the JpegInterchangeFormat offset
        TiffField<?> f_jpegInterchangeFormat = ifd.getField(TiffTag.JPEG_INTERCHANGE_FORMAT);
        TiffField<?> f_stripOffsets = ifd.getField(TiffTag.STRIP_OFFSETS);

        if (f_stripOffsets == null && f_jpegInterchangeFormat != null &&
            (compression == TiffFieldEnum.Compression.OLD_JPG || compression == TiffFieldEnum.Compression.JPG)) {
            // Use JpegInterchangeFormat as the single strip offset
            int jpegOffset = f_jpegInterchangeFormat.getDataAsLong()[0];
            TiffField<?> f_jpegLength = ifd.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH);
            int jpegLength = (f_jpegLength != null) ? f_jpegLength.getDataAsLong()[0] : -1;

            int[] stripOffsets = new int[]{jpegOffset};
            int[] stripByteCounts = new int[]{jpegLength > 0 ? jpegLength : 0};
            if (compression == TiffFieldEnum.Compression.OLD_JPG) {
                return decodeOldStyleJPEG(ifd, stripOffsets, stripByteCounts);
            } else {
                return decodeNewStyleJPEG(ifd, stripOffsets, stripByteCounts, false);
            }
        }

        // Forget about tiled TIFF for now
        if(f_stripOffsets == null) {
            LOGGER.error("Missing required field strip0ffsets");
            return null;
        }

        TiffField<?> f_stripByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS);
        int[] stripOffsets = f_stripOffsets.getDataAsLong();
        int[] stripByteCounts = null;
        if(f_stripByteCounts == null) {
            if(stripOffsets.length == 1) {
                stripByteCounts = new int[]{0};
            } else {
                LOGGER.error("Missing required field stripByteCounts");
                return null;
            }
        } else {
            stripByteCounts = f_stripByteCounts.getDataAsLong();
        }

        int imageWidth = ifd.getField(TiffTag.IMAGE_WIDTH).getDataAsLong()[0];
        int imageHeight = ifd.getField(TiffTag.IMAGE_LENGTH).getDataAsLong()[0];
        LOGGER.info("Image width: {}", imageWidth);
        LOGGER.info("Image height: {}", imageHeight);
        TiffField<?> f_rowsPerStrip = ifd.getField(TiffTag.ROWS_PER_STRIP);
        int rowsPerStrip = imageHeight;
        if(f_rowsPerStrip != null) {
            rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];
        }
        LOGGER.info("Rows per strip: {}", rowsPerStrip);
        if(rowsPerStrip <= 0 || rowsPerStrip > imageHeight) {
            rowsPerStrip = imageHeight; // Invalid value, use image height as rows per strip
        }

        TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION);
        int photoMetric = PhotoMetric.WHITE_IS_ZERO.getValue();
        if(f_photoMetric != null) {
            photoMetric = (int)f_photoMetric.getDataAsLong()[0];
        }
        TiffFieldEnum.PhotoMetric e_photoMetric = TiffFieldEnum.PhotoMetric.fromValue(photoMetric);
        LOGGER.info("PhotoMetric: {}", e_photoMetric);

        TiffField<?> f_bitsPerSample = ifd.getField(TiffTag.BITS_PER_SAMPLE);
        int bitsPerSample = 1;
        if(f_bitsPerSample != null) {
            bitsPerSample = (int)f_bitsPerSample.getDataAsLong()[0];
        }
        // Detect actual BitsPerSample for uncompressed data (handles wrong tags)
        int correctedBitsPerSample = TIFFTweaker.detectActualBitsPerSample(ifd, compression);
        if (correctedBitsPerSample != bitsPerSample) {
            LOGGER.warn("BitsPerSample tag {} mismatches data; using {} based on StripByteCounts", bitsPerSample, correctedBitsPerSample);
            bitsPerSample = correctedBitsPerSample;
            ifd.addField(new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), new short[] {(short)bitsPerSample}));
        }
        LOGGER.info("Bits per sample: {}", bitsPerSample);

        TiffField<?> f_samplesPerPixel = ifd.getField(TiffTag.SAMPLES_PER_PIXEL);
        int samplesPerPixel = 1;
        if(f_samplesPerPixel != null) {
            samplesPerPixel = (int)f_samplesPerPixel.getDataAsLong()[0];
        }
        LOGGER.info("Samples per pixel: {}", samplesPerPixel);

        TiffField<?> f_predictor = ifd.getField(TiffTag.PREDICTOR);
        int predictor = 0;
        if(f_predictor != null) {
            predictor = (int)f_predictor.getDataAsLong()[0];
            LOGGER.info("Predictor: {}", predictor);
        }

        TiffField<?> f_planaryConfiguration = ifd.getField(TiffTag.PLANAR_CONFIGURATTION);
        int planaryConfiguration = 1;
        if(f_planaryConfiguration != null) {
            planaryConfiguration = (int)f_planaryConfiguration.getDataAsLong()[0];
        }
        TiffFieldEnum.PlanarConfiguration e_planaryConfiguration = TiffFieldEnum.PlanarConfiguration.fromValue(planaryConfiguration);
        LOGGER.info("Planary configuration: {}", e_planaryConfiguration);

        TiffField<?> f_sampleFormat = ifd.getField(TiffTag.SAMPLE_FORMAT);
        TiffField<?> f_sampleMaxValue = ifd.getField(TiffTag.S_MAX_SAMPLE_VALUE);
        TiffField<?> f_sampleMinValue = ifd.getField(TiffTag.S_MIN_SAMPLE_VALUE);

        int fillOrder = 1;
        TiffField<?> f_fillOrder = ifd.getField(TiffTag.FILL_ORDER);
        if(f_fillOrder != null) {
            fillOrder = (int)f_fillOrder.getDataAsLong()[0];
        }

        boolean floatSample = false;
        if(f_sampleFormat != null && f_sampleFormat.getDataAsLong()[0] == 3) { // Floating point sample data type
            floatSample = true;
            double maxValue = (bitsPerSample <= 32) ? Float.MAX_VALUE : Double.MAX_VALUE;
            double minValue = (bitsPerSample <= 32) ? Float.MIN_VALUE : Double.MIN_VALUE;
            if(bitsPerSample <= 32 && f_sampleMaxValue != null) {
                maxValue = ((float[])f_sampleMaxValue.getData())[0];
            } else if(bitsPerSample > 32 && f_sampleMaxValue != null) {
                maxValue = ((double[])f_sampleMaxValue.getData())[0];
            }
            if (bitsPerSample <= 32 && f_sampleMinValue != null) {
                minValue = ((float[])f_sampleMinValue.getData())[0];
            } else if (bitsPerSample > 32 && f_sampleMinValue != null) {
                minValue = ((double[])f_sampleMinValue.getData())[0];
            }
            LOGGER.info("Sample MAX value: {}", maxValue);
            LOGGER.info("Sample MIN vlaue: {}", minValue);
        }

        boolean hasAlpha = false;
        TiffField<?> f_extraSamples = ifd.getField(TiffTag.EXTRA_SAMPLES);
        boolean isAssociatedAlpha = false;
        int numOfBands = samplesPerPixel;
        int transparency = Transparency.OPAQUE;
        int extraSamplesType = 0;
        if(f_extraSamples != null) {
            extraSamplesType = (int)f_extraSamples.getDataAsLong()[0];
            isAssociatedAlpha = (extraSamplesType == 1);
        }

        int offset = 0;
        ImageDecoder decoder = null;
        byte[] pixels = null;
        int[] stripBytes = TIFFTweaker.getUncompressedStripByteCounts(ifd, stripOffsets.length);
        int[] count = new int[samplesPerPixel];
        if (planaryConfiguration == 2) {
            int inc = stripBytes.length / samplesPerPixel;
            int startOff = 0;
            int endOff = inc;
            for(int k = 0; k < samplesPerPixel; k++) {
                for(int j = startOff; j < endOff; j++) count[k] += stripBytes[j];
                startOff += inc;
                endOff += inc;
            }
        }

        int[] rgbColorPalette = null;
        switch(e_photoMetric) {
            case PALETTE_COLOR:
                short[] colorMap = (short[])ifd.getField(TiffTag.COLORMAP).getData();
                rgbColorPalette = new int[colorMap.length / 3];
                int numOfColors = (1 << bitsPerSample);
                int numOfColors2 = (numOfColors << 1);
                for(int i = 0, index = 0; i < colorMap.length / 3; i++) {
                    rgbColorPalette[index++] = 0xff000000 | ((colorMap[i] & 0xff00) << 8) |
                                               ((colorMap[i + numOfColors] & 0xff00)) | 
                                               ((colorMap[i + numOfColors2] & 0xff00) >> 8);
                }
                int bytesPerScanLine = (imageWidth * bitsPerSample + 7) / 8;
                pixels = new byte[bytesPerScanLine * imageHeight];
                switch(compression) {
                    case NONE:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripBytes[i];
                            randIS.seek(stripOffsets[i]);
                            randIS.readFully(pixels, offset, bytes2Read);
                            offset += bytes2Read;
                        }
                        break;
                    case LZW:
                        decoder = new LZWTreeDecoder(8, true);
                        break;
                    case DEFLATE:
                    case DEFLATE_ADOBE:
                        decoder = new DeflateDecoder();
                        break;
                    case PACKBITS:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripBytes[i];
                            unpackStrip(pixels, offset, stripBytes[i], stripOffsets[i], stripByteCounts[i]);
                            offset += bytes2Read;
                        }
                        break;
                    default:
                }
                if (decoder != null) {
                    for(int i = 0; i < stripByteCounts.length; i++) {
                        byte[] temp = null;
                        randIS.seek(stripOffsets[i]);
                        if(stripByteCounts[i] == 0) {
                            temp = IOUtils.readFully(randIS, 4096);
                        } else {
                            temp = new byte[stripByteCounts[i]];
                            randIS.readFully(temp);
                        }
                        decoder.setInput(temp);
                        int numOfBytes = decoder.decode(pixels, offset, stripBytes[i]);
                        offset += numOfBytes;
                    }
                }
                WritableRaster raster = null;
                DataBuffer db = new DataBufferByte(pixels, pixels.length);
                if (bitsPerSample != 8) {
                    raster = Raster.createPackedRaster(db, imageWidth, imageHeight, bitsPerSample, null);
                } else {
                    int[] off = {0}; // band offset, we have only one band start at 0
                    raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth, 1, off, null);
                }
                ColorModel cm = new IndexColorModel(bitsPerSample, rgbColorPalette.length, rgbColorPalette, 0, false, 1, DataBuffer.TYPE_BYTE);
                return new BufferedImage(cm, raster, false, null);

            case SEPARATED:
                // Hopefully CMYK
                bytesPerScanLine = samplesPerPixel * ((imageWidth * bitsPerSample + 7) / 8);
                int totalBytes = bytesPerScanLine * imageHeight;
                if(planaryConfiguration == 2) bytesPerScanLine = (imageWidth * bitsPerSample + 7) / 8;
                pixels = new byte[totalBytes];
                switch(compression) {
                    case NONE:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripBytes[i];
                            randIS.seek(stripOffsets[i]);
                            randIS.readFully(pixels, offset, bytes2Read);
                            offset += bytes2Read;
                        }
                        break;
                    case LZW:
                        decoder = new LZWTreeDecoder(8, true);
                        break;
                    case DEFLATE:
                    case DEFLATE_ADOBE:
                        decoder = new DeflateDecoder();
                        break;
                    case PACKBITS:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripBytes[i];
                            unpackStrip(pixels, offset, stripBytes[i], stripOffsets[i], stripByteCounts[i]);
                            offset += bytes2Read;
                        }
                        break;
                    default:
                }
                if (decoder != null) {
                    for(int i = 0; i < stripByteCounts.length; i++) {
                        randIS.seek(stripOffsets[i]);
                        byte[] temp = null;
                        if(stripByteCounts[i] == 0) {
                            temp = IOUtils.readFully(randIS, 4096);
                        } else {
                            temp = new byte[stripByteCounts[i]];
                            randIS.readFully(temp);
                        }
                        decoder.setInput(temp);
                        int numOfBytes = decoder.decode(pixels, offset, stripBytes[i]);
                        offset += numOfBytes;
                    }
                }
                if (predictor == 2 && planaryConfiguration == 1) {
                    pixels = applyDePredictor(samplesPerPixel, pixels, imageWidth, imageHeight);
                }
                db = new DataBufferByte(pixels, pixels.length);
                if (extraSamplesType == 1 || extraSamplesType == 2) {
                    hasAlpha = true;
                    transparency = Transparency.TRANSLUCENT;
                }
                ColorSpace colorSpace = CMYKColorSpace.getInstance(hasAlpha);
                // Get ICC_Profile
                TiffField<?> f_colorProfile = ifd.getField(TiffTag.ICC_PROFILE);
                ICC_Profile profile = null;
                if(f_colorProfile != null) {
                    profile = ICC_Profile.getInstance((byte[])f_colorProfile.getData());
                    colorSpace = new ICC_ColorSpace(profile);
                }
                int[] bandoff = {0, 1, 2, 3};
                int[] nBits = {bitsPerSample, bitsPerSample, bitsPerSample, bitsPerSample};
                if (samplesPerPixel >= 5) {
                    bandoff = new int[]{0, 1, 2, 3, 4};
                    nBits = new int[] {bitsPerSample, bitsPerSample, bitsPerSample, bitsPerSample, bitsPerSample};
                }
                if(bitsPerSample == 16) {
                    short[] spixels = ArrayUtils.toShortArray(pixels, endian == IOUtils.BIG_ENDIAN);
                    db = new DataBufferUShort(spixels, spixels.length);
                    cm = new ComponentColorModel(colorSpace, nBits, hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_USHORT);
                    if(planaryConfiguration == 2) {
                        int[] bankIndices = new int[]{0, 0, 0, 0};
                        bandoff = new int[] {0, count[0], count[0] + count[1], count[0] + count[1] + count[2]};
                        if(samplesPerPixel >= 5) {
                            bandoff = new int[] {0, count[0], count[0] + count[1], count[0] + count[1] + count[2], count[0] + count[1] + count[2] + count[3]};
                            bankIndices = new int[]{0, 0, 0, 0, 0};
                        }
                        raster = Raster.createBandedRaster(db, imageWidth, imageHeight, bytesPerScanLine * 8 / bitsPerSample, bankIndices, bandoff, null);
                    } else {
                        raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth * numOfBands, numOfBands, bandoff, null);
                    }
                } else {
                    cm = new ComponentColorModel(colorSpace, nBits, hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_BYTE);
                    if(planaryConfiguration == 2) {
                        int[] bankIndices = new int[]{0, 0, 0, 0};
                        bandoff = new int[] {0, count[0], count[0] + count[1], count[0] + count[1] + count[2]};
                        if(samplesPerPixel >= 5) {
                            bandoff = new int[] {0, count[0], count[0] + count[1], count[0] + count[1] + count[2], count[0] + count[1] + count[2] + count[3]};
                            bankIndices = new int[]{0, 0, 0, 0, 0};
                        }
                        raster = Raster.createBandedRaster(db, imageWidth, imageHeight, bytesPerScanLine, bankIndices, bandoff, null);
                    } else {
                        raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth * numOfBands, numOfBands, bandoff, null);
                    }
                }
                if(profile != null) {
                    IMGUtils.iccp2rgbRaster(raster, cm);
                    cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), null, hasAlpha, isAssociatedAlpha, transparency, raster.getTransferType());
                }
                return new BufferedImage(cm, raster, false, null);

            case YCbCr:
                int[] samplingFactor = {2, 2}; // Default value, Not [1, 1]
                TiffField<?> f_YCbCrSubSampling = ifd.getField(TiffTag.YCbCr_SUB_SAMPLING);
                if(f_YCbCrSubSampling != null) samplingFactor = f_YCbCrSubSampling.getDataAsLong();

                int expandedImageWidth = ((imageWidth + samplingFactor[0] - 1) / samplingFactor[0]) * samplingFactor[0];
                int expandedImageHeight = ((imageHeight + samplingFactor[1] - 1) / samplingFactor[1]) * samplingFactor[1];

                float referenceBlackY = 0.0f;
                float referenceWhiteY = 255.0f;
                float referenceBlackCb = 128.0f;
                float referenceWhiteCb = 255.0f;
                float referenceBlackCr = 128.0f;
                float referenceWhiteCr = 255.0f;
                float codingRangeY = 255.0f;
                float codingRangeCbCr = 127.0f;

                TiffField<?> f_referenceBlackWhite = ifd.getField(TiffTag.REFERENCE_BLACK_WHITE);
                if(f_referenceBlackWhite != null) {
                    int[] referenceBlackWhite = f_referenceBlackWhite.getDataAsLong();
                    // ReferenceBlackWhite should have 6 rational values (12 integers)
                    if (referenceBlackWhite.length >= 12) {
                        referenceBlackY = 1.0f * referenceBlackWhite[0] / referenceBlackWhite[1];
                        referenceWhiteY = 1.0f * referenceBlackWhite[2] / referenceBlackWhite[3];
                        referenceBlackCb = 1.0f * referenceBlackWhite[4] / referenceBlackWhite[5];
                        referenceWhiteCb = 1.0f * referenceBlackWhite[6] / referenceBlackWhite[7];
                        referenceBlackCr = 1.0f * referenceBlackWhite[8] / referenceBlackWhite[9];
                        referenceWhiteCr = 1.0f * referenceBlackWhite[10] / referenceBlackWhite[11];
                    } else if (referenceBlackWhite.length >= 6) {
                        // Some files store it as 6 integers directly: [Y_black, Y_white, Cb_black, Cb_white, Cr_black, Cr_white]
                        referenceBlackY = 1.0f * referenceBlackWhite[0];
                        referenceWhiteY = 1.0f * referenceBlackWhite[1];
                        referenceBlackCb = 1.0f * referenceBlackWhite[2];
                        referenceWhiteCb = 1.0f * referenceBlackWhite[3];
                        referenceBlackCr = 1.0f * referenceBlackWhite[4];
                        referenceWhiteCr = 1.0f * referenceBlackWhite[5];
                    }
                }

                float lumaRed = 0.299f;
                float lumaGreen = 0.587f;
                float lumaBlue = 0.114f;

                TiffField<?> f_YCbCrCoefficients = ifd.getField(TiffTag.YCbCr_COEFFICIENTS);
                if(f_YCbCrCoefficients != null) {
                    int[] lumas = f_YCbCrCoefficients.getDataAsLong();
                    lumaRed = 1.0f * lumas[0] / lumas[1];
                    lumaGreen = 1.0f * lumas[2] / lumas[3];
                    lumaBlue = 1.0f * lumas[4] / lumas[5];
                }

                int offsetY = 0;
                int bytesY = expandedImageWidth * expandedImageHeight;
                pixels = new byte[bytesY * 3];
                byte[] temp = null, temp2 = null;
                if(planaryConfiguration == 1) {
                    // Define variables related to data unit
                    int bytesPerUnity = samplingFactor[0] * samplingFactor[1];
                    int bytesPerDataUnit = bytesPerUnity + 2;
                    int dataUnitsPerWidth = expandedImageWidth / samplingFactor[0];

                    switch(compression) {
                        case NONE:
                            for(int i = 0; i < stripByteCounts.length; i++) {
                                randIS.seek(stripOffsets[i]);
                                if(stripByteCounts[i] == 0) {
                                    temp = IOUtils.readFully(randIS, 4096);
                                } else {
                                    temp = new byte[stripByteCounts[i]];
                                    randIS.readFully(temp);
                                }
                                int numOfDataUnit = temp.length / bytesPerDataUnit;
                                offsetY = upsampling(offsetY, numOfDataUnit, bytesPerUnity, samplingFactor, referenceBlackY, referenceWhiteY, referenceBlackCb,
                                                     referenceWhiteCb, referenceBlackCr, referenceWhiteCr, codingRangeY, codingRangeCbCr, lumaRed, lumaGreen, lumaBlue, temp,
                                                     pixels, expandedImageWidth, dataUnitsPerWidth);
                            }
                            break;
                        case OLD_JPG:
                            return decodeOldStyleJPEG(ifd, stripOffsets, stripByteCounts);
                        case JPG:
                            return decodeNewStyleJPEG(ifd, stripOffsets, stripByteCounts, false);
                        case LZW:
                            for(int i = 0; i < stripByteCounts.length; i++) {
                                randIS.seek(stripOffsets[i]);
                                if(stripByteCounts[i] == 0){
                                    temp = IOUtils.readFully(randIS, 4096);
                                } else {
                                    temp = new byte[stripByteCounts[i]];
                                    randIS.readFully(temp);
                                }
                                temp2 = new byte[stripBytes[i]];
                                decoder = new LZWTreeDecoder(8, true);
                                decoder.setInput(temp);
                                int numOfBytes = decoder.decode(temp2, 0, temp2.length);
                                int numOfDataUnit = numOfBytes / bytesPerDataUnit;
                                offsetY = upsampling(offsetY, numOfDataUnit, bytesPerUnity, samplingFactor, referenceBlackY, referenceWhiteY, referenceBlackCb,
                                                     referenceWhiteCb, referenceBlackCr, referenceWhiteCr, codingRangeY, codingRangeCbCr, lumaRed, lumaGreen, lumaBlue, temp2,
                                                     pixels, expandedImageWidth, dataUnitsPerWidth);
                            }
                            break;
                        case PACKBITS:
                            for(int i = 0; i < stripByteCounts.length; i++) {
                                randIS.seek(stripOffsets[i]);
                                if(stripByteCounts[i] == 0){
                                    temp = IOUtils.readFully(randIS, 4096);
                                } else {
                                    temp = new byte[stripByteCounts[i]];
                                    randIS.readFully(temp);
                                }
                                temp2 = new byte[stripBytes[i]];
                                Packbits.unpackbits(temp, temp2);
                                int numOfBytes = stripBytes[i];
                                int numOfDataUnit = numOfBytes / bytesPerDataUnit;
                                offsetY = upsampling(offsetY, numOfDataUnit, bytesPerUnity, samplingFactor, referenceBlackY, referenceWhiteY, referenceBlackCb,
                                                     referenceWhiteCb, referenceBlackCr, referenceWhiteCr, codingRangeY, codingRangeCbCr, lumaRed, lumaGreen, lumaBlue, temp2,
                                                     pixels, expandedImageWidth, dataUnitsPerWidth);
                            }
                            break;
                        default:
                    }
                } else {
                    // Planar configuration
                    if(stripByteCounts.length == 1 && samplesPerPixel != 1)
                        throw new RuntimeException("stripByteCounts length 1 is not consistent with samplesPerPixel " + samplesPerPixel);

                    bytesPerScanLine = (expandedImageWidth * bitsPerSample + 7) / 8;
                    switch(compression) {
                        case NONE:
                            int stripsPerSample = stripByteCounts.length / samplesPerPixel;
                            byte[][] buf = new byte[samplesPerPixel][];
                            ByteArrayOutputStream bout = new ByteArrayOutputStream();
                            for(int i = 0, index = 0; i < samplesPerPixel; i++) {
                                for(int j = 0; j < stripsPerSample; j++, index++) {
                                    randIS.seek(stripOffsets[index]);
                                    int len = stripByteCounts[index];
                                    if(len == 0) {
                                        temp = IOUtils.readFully(randIS, 4096);
                                    } else {
                                        temp = new byte[len];
                                        randIS.readFully(temp);
                                    }
                                    bout.write(temp);
                                }
                                buf[i] = bout.toByteArray();
                                bout.reset();
                            }
                            int yPos = 0, CbPos = 0, CrPos = 0, yoffset = 0;
                            int stride = samplingFactor[0] * samplingFactor[1];
                            int counter = 1;
                            for(int i = 0; i < expandedImageHeight; i++) {
                                for(int j = 0; j < expandedImageWidth; j++, yPos++, yoffset++, counter++) {
                                    int Y = buf[0][yPos] & 0xff;
                                    int Cb = buf[1][CbPos] & 0xff;
                                    int Cr = buf[2][CrPos] & 0xff;
                                    if(counter % stride == 0) {
                                        CbPos++;
                                        CrPos++;
                                    }
                                    float fY = (Y - referenceBlackY) * codingRangeY / (referenceWhiteY - referenceBlackY);
                                    float fCb = (Cb - referenceBlackCb) * codingRangeCbCr / (referenceWhiteCb - referenceBlackCb);
                                    float fCr = (Cr - referenceBlackCr) * codingRangeCbCr / (referenceWhiteCr - referenceBlackCr);
                                    float R = (fCr * (2 - 2 * lumaRed) + fY);
                                    float B = (fCb * (2 - 2 * lumaBlue) + fY);
                                    float G = ((fY - lumaBlue * B - lumaRed * R) / lumaGreen);
                                    if(R < 0) R = 0; if(R > 255) R = 255;
                                    if(G < 0) G = 0; if(G > 255) G = 255;
                                    if(B < 0) B = 0; if(B > 255) B = 255;
                                    int redPos = 3 * yoffset;
                                    pixels[redPos] = (byte)R;
                                    pixels[redPos+1] = (byte)G;
                                    pixels[redPos+2] = (byte)B;
                                }
                            }
                            break;
                        default:
                    }
                }
                db = new DataBufferByte(pixels, pixels.length);
                bandoff = new int[] {0, 1, 2};
                hasAlpha = false;
                numOfBands = samplesPerPixel;
                transparency = Transparency.OPAQUE;
                nBits = new int[] {8, 8, 8};
                raster = Raster.createInterleavedRaster(db, expandedImageWidth, expandedImageHeight, expandedImageWidth * numOfBands, numOfBands, bandoff, null);
                cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_BYTE);
                return new BufferedImage(cm, raster, false, null).getSubimage(0, 0, imageWidth, imageHeight);

            case RGB:
                bytesPerScanLine = samplesPerPixel * ((imageWidth * bitsPerSample + 7) / 8);
                int totalBytes2Read = imageHeight * bytesPerScanLine;
                if (planaryConfiguration == 2) bytesPerScanLine = (imageWidth * bitsPerSample + 7) / 8;
                pixels = new byte[totalBytes2Read];
                switch(compression) {
                    case NONE:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripBytes[i];
                            randIS.seek(stripOffsets[i]);
                            randIS.readFully(pixels, offset, bytes2Read);
                            offset += bytes2Read;
                        }
                        if(fillOrder == 2) ArrayUtils.reverseBits(pixels);
                        break;
                    case OLD_JPG:
                        return decodeOldStyleJPEG(ifd, stripOffsets, stripByteCounts);
                    case JPG:
                        return decodeNewStyleJPEG(ifd, stripOffsets, stripByteCounts, true);
                    case PACKBITS:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripBytes[i];
                            unpackStrip(pixels, offset, stripBytes[i], stripOffsets[i], stripByteCounts[i]);
                            offset += bytes2Read;
                        }
                        break;
                    case LZW:
                        decoder = new LZWTreeDecoder(8, true);
                        break;
                    case DEFLATE:
                    case DEFLATE_ADOBE:
                        decoder = new DeflateDecoder();
                        break;
                    default:
                }
                if (decoder != null) {
                    pixels = new byte[stripOffsets.length * stripBytes[0]];
                    for(int i = 0; i < stripByteCounts.length; i++) {
                        randIS.seek(stripOffsets[i]);
                        if(stripByteCounts[i] == 0) {
                            temp = IOUtils.readFully(randIS, 4096);
                        } else {
                            temp = new byte[stripByteCounts[i]];
                            randIS.readFully(temp);
                        }
                        if(fillOrder == 2) ArrayUtils.reverseBits(temp);
                        decoder.setInput(temp);
                        int numOfBytes = decoder.decode(pixels, offset, stripBytes[i]);
                        offset += numOfBytes;
                    }
                }
                if (predictor == 2) {
                    if (planaryConfiguration == 1) {
                        pixels = applyDePredictor(samplesPerPixel, pixels, imageWidth, imageHeight);
                    } else {
                        int dataOffset = 0;
                        for(int k = 0; k < samplesPerPixel; k++) {
                            applyDePredictor2(pixels, dataOffset, imageWidth, imageHeight);
                            dataOffset += count[k];
                        }
                    }
                }
                cm = null;
                raster = null;
                bandoff = new int[samplesPerPixel];
                nBits = new int[samplesPerPixel];
                numOfBands = samplesPerPixel;
                int[] bankIndices = new int[samplesPerPixel];
                Arrays.fill(nBits, bitsPerSample <= 32 ? bitsPerSample : 32);
                if(planaryConfiguration == 2) {
                    for(int i = 0; i < samplesPerPixel; i++) {
                        bandoff[i] = 0;
                        bankIndices[i] = i;
                    }
                } else {
                    for(int i = 0; i < samplesPerPixel; i++) {
                        bandoff[i] = i;
                        bankIndices[i] = 0;
                    }
                }
                hasAlpha = false;
                transparency = Transparency.OPAQUE;
                if(samplesPerPixel == 4) {
                    transparency = Transparency.TRANSLUCENT;
                    hasAlpha = true;
                }
                if(planaryConfiguration == 2) {
                    byte[][] rgb = new byte[samplesPerPixel][];
                    int off = 0;
                    int dataBufferType = DataBuffer.TYPE_BYTE;
                    for(int i = 0; i < samplesPerPixel; i++) {
                        rgb[i] = ArrayUtils.subArray(pixels, off, count[i]);
                        off += count[i];
                    }
                    if(floatSample) {
                        if(bitsPerSample == 16 || bitsPerSample == 24 || bitsPerSample == 32) {
                            float[][] floats = new float[samplesPerPixel][];
                            if(bitsPerSample == 16) {
                                for(int i = 0; i < samplesPerPixel; i++) floats[i] = ArrayUtils.to16BitFloatArray(rgb[i], endian == IOUtils.BIG_ENDIAN);
                            } else if(bitsPerSample == 24) {
                                for(int i = 0; i < samplesPerPixel; i++) floats[i] = ArrayUtils.to24BitFloatArray(rgb[i], endian == IOUtils.BIG_ENDIAN);
                            } else {
                                for(int i = 0; i < samplesPerPixel; i++) floats[i] = ArrayUtils.toFloatArray(rgb[i], endian == IOUtils.BIG_ENDIAN);
                            }
                            dataBufferType = DataBuffer.TYPE_FLOAT;
                            db = new DataBufferFloat(floats, floats[0].length);
                        } else if(bitsPerSample == 64) {
                            double[][] doubles = new double[samplesPerPixel][];
                            for(int i = 0; i < samplesPerPixel; i++) doubles[i] = ArrayUtils.toDoubleArray(rgb[i], endian == IOUtils.BIG_ENDIAN);
                            dataBufferType = DataBuffer.TYPE_DOUBLE;
                            db = new DataBufferDouble(doubles, doubles[0].length);
                        } else {
                            throw new UnsupportedOperationException("Unsupported floating point sample bit depth: " + bitsPerSample);
                        }
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, hasAlpha, isAssociatedAlpha, transparency, dataBufferType);
                        SampleModel sampleModel = new BandedSampleModel(dataBufferType, imageWidth, imageHeight, imageWidth, bankIndices, bandoff);
                        raster = Raster.createWritableRaster(sampleModel, db, null);
                    } else {
                        if(bitsPerSample == 16) {
                            short[][] shorts = new short[samplesPerPixel][];
                            for(int i = 0; i < samplesPerPixel; i++) shorts[i] = ArrayUtils.toShortArray(rgb[i], endian == IOUtils.BIG_ENDIAN);
                            db = new DataBufferUShort(shorts, shorts[0].length);
                            dataBufferType = DataBuffer.TYPE_USHORT;
                        } else if(bitsPerSample > 8 && bitsPerSample < 16) {
                            short[][] shorts = new short[samplesPerPixel][];
                            for(int i = 0; i < samplesPerPixel; i++) shorts[i] = (short[])(ArrayUtils.toNBits(bitsPerSample, rgb[i], imageWidth, true));
                            db = new DataBufferUShort(shorts, shorts[0].length);
                            dataBufferType = DataBuffer.TYPE_USHORT;
                        } else if(bitsPerSample > 16) {
                            int[][] ints = new int[samplesPerPixel][];
                            boolean bigEndian = (bitsPerSample % 8 == 0);
                            for(int i = 0; i < samplesPerPixel; i++) ints[i] = (int[])(ArrayUtils.toNBits(bitsPerSample, rgb[i], samplesPerPixel * imageWidth, bigEndian));
                            db = new DataBufferInt(ints, ints[0].length);
                            dataBufferType = DataBuffer.TYPE_INT;
                        } else if(bitsPerSample < 8) {
                            byte[][] bytes = new byte[samplesPerPixel][];
                            for(int i = 0; i < samplesPerPixel; i++) bytes[i] = (byte[])(ArrayUtils.toNBits(bitsPerSample, rgb[i], imageWidth, true));
                            db = new DataBufferByte(bytes, bytes[0].length);
                            dataBufferType = DataBuffer.TYPE_BYTE;
                        } else {
                            db = new DataBufferByte(rgb, rgb[0].length);
                            dataBufferType = DataBuffer.TYPE_BYTE;
                        }
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, hasAlpha, isAssociatedAlpha, transparency, dataBufferType);
                        raster = Raster.createBandedRaster(db, imageWidth, imageHeight, imageWidth, bankIndices, bandoff, null);
                    }
                } else {
                    if(floatSample) {
                        if(bitsPerSample >= 16 && bitsPerSample <= 32) {
                            float[] tempArray = null;
                            if(bitsPerSample == 16) tempArray = ArrayUtils.to16BitFloatArray(pixels, endian == IOUtils.BIG_ENDIAN);
                            else if(bitsPerSample == 24) tempArray = ArrayUtils.to24BitFloatArray(pixels, endian == IOUtils.BIG_ENDIAN);
                            else tempArray = ArrayUtils.toFloatArray(pixels, endian == IOUtils.BIG_ENDIAN);
                            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_FLOAT);
                            db = new DataBufferFloat(tempArray, tempArray.length);
                            SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, imageWidth, imageHeight, samplesPerPixel, imageWidth * samplesPerPixel, bandoff);
                            raster = Raster.createWritableRaster(sampleModel, db, null);
                        } else if(bitsPerSample == 64) {
                            double[] tempArray = ArrayUtils.toDoubleArray(pixels, endian == IOUtils.BIG_ENDIAN);
                            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_DOUBLE);
                            db = new DataBufferDouble(tempArray, tempArray.length);
                            SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_DOUBLE, imageWidth, imageHeight, samplesPerPixel, imageWidth * samplesPerPixel, bandoff);
                            raster = Raster.createWritableRaster(sampleModel, db, null);
                        } else throw new UnsupportedOperationException("Unsupported bit depth: " + bitsPerSample);
                    } else {
                        if (bitsPerSample < 8) {
                            Object tempArray = ArrayUtils.toNBits(bitsPerSample * samplesPerPixel, pixels, imageWidth, true);
                            cm = new DirectColorModel(bitsPerSample * samplesPerPixel, redMask[bitsPerSample], greenMask[bitsPerSample], blueMask[bitsPerSample]);
                            raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
                            raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);
                        } else if(bitsPerSample == 8) {
                            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_BYTE);
                            db = new DataBufferByte(pixels, pixels.length);
                            raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth * numOfBands, numOfBands, bandoff, null);
                        } else {
                            Object tempArray = ArrayUtils.toNBits(bitsPerSample, pixels, samplesPerPixel * imageWidth, (bitsPerSample % 8 == 0) ? endian == IOUtils.BIG_ENDIAN : true);
                            if(bitsPerSample <= 16) {
                                cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_USHORT);
                                raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
                                raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);
                            } else if(bitsPerSample == 32) {
                                cm = new Int32ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), hasAlpha);
                                raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
                                raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);
                            } else {
                                cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_INT);
                                raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
                                raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);
                            }
                        }
                    }
                }
                return new BufferedImage(cm, raster, false, null);

            case BLACK_IS_ZERO:
            case WHITE_IS_ZERO:
                bytesPerScanLine = samplesPerPixel * ((imageWidth * bitsPerSample + 7) / 8);
                totalBytes2Read = imageHeight * bytesPerScanLine;
                if(planaryConfiguration == 2) bytesPerScanLine = (imageWidth * bitsPerSample + 7) / 8;
                pixels = new byte[totalBytes2Read];
                switch(bitsPerSample) {
                    case 1:
                        rgbColorPalette = (e_photoMetric == PhotoMetric.BLACK_IS_ZERO) ? BLACK_WHITE_PALETTE : BLACK_WHITE_PALETTE_WHITE_IS_ZERO;
                        break;
                    case 2:
                        rgbColorPalette = (e_photoMetric == PhotoMetric.BLACK_IS_ZERO) ? FOUR_COLOR_PALETTE : FOUR_COLOR_PALETTE_WHITE_IS_ZERO;
                        break;
                    case 4:
                        rgbColorPalette = (e_photoMetric == PhotoMetric.BLACK_IS_ZERO) ? SIXTEEN_COLOR_PALETTE : SIXTEEN_COLOR_PALETTE_WHITE_IS_ZERO;
                        break;
                    case 8:
                        rgbColorPalette = (e_photoMetric == PhotoMetric.BLACK_IS_ZERO) ? EIGHT_BIT_COLOR_PALETTE : EIGHT_BIT_COLOR_PALETTE_WHITE_IS_ZERO;
                        break;
                }
                switch(compression) {
                    case NONE:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripBytes[i];
                            randIS.seek(stripOffsets[i]);
                            randIS.readFully(pixels, offset, bytes2Read);
                            offset += bytes2Read;
                        }
                        if(fillOrder == 2) ArrayUtils.reverseBits(pixels);
                        break;
                    case CCITTRLE:
                        decoder = new CCITTDecoder(imageWidth, rowsPerStrip);
                        break;
                    case CCITTFAX3:
                        TiffField<?> f_t4Options = ifd.getField(TiffTag.T4_OPTIONS);
                        int t4Options = 0;
                        if(f_t4Options != null) t4Options = (int)f_t4Options.getDataAsLong()[0];
                        if ((t4Options & GROUP30PT_UNCOMPRESSED) == GROUP30PT_UNCOMPRESSED) {
                            throw new UnsupportedCompressionException("Group 3 Uncompressed mode is not supported");
                        }
                        boolean fillBits = ((t4Options & GROUP30PT_FILLBITS) == GROUP30PT_FILLBITS);
                        if((t4Options & GROUP30PT_2DENCODING) == GROUP30PT_2DENCODING) {
                            decoder = new G32DDecoder(imageWidth, rowsPerStrip, fillBits);
                        } else {
                            decoder = new G31DDecoder(imageWidth, rowsPerStrip, fillBits);
                        }
                        break;
                    case CCITTFAX4:
                        decoder = new G42DDecoder(imageWidth, rowsPerStrip);
                        break;
                    case LZW:
                        decoder = new LZWTreeDecoder(8, true);
                        break;
                    case DEFLATE:
                    case DEFLATE_ADOBE:
                        decoder = new DeflateDecoder();
                        break;
                    case THUNDERSCAN:
                        decoder = new ThunderScanDecoder();
                        break;
                    case PACKBITS:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripBytes[i];
                            unpackStrip(pixels, offset, stripBytes[i], stripOffsets[i], stripByteCounts[i]);
                            offset += bytes2Read;
                        }
                        break;
                    case OLD_JPG:
                        return decodeOldStyleJPEG(ifd, stripOffsets, stripByteCounts);
                    case JPG:
                        return decodeNewStyleJPEG(ifd, stripOffsets, stripByteCounts, false);
                }
                if (decoder != null) {
                    for(int i = 0; i < stripByteCounts.length; i++) {
                        randIS.seek(stripOffsets[i]);
                        if(stripByteCounts[i] == 0) {
                            temp = IOUtils.readFully(randIS, 4096);
                        } else {
                            temp = new byte[stripByteCounts[i]];
                            randIS.readFully(temp);
                        }
                        if(fillOrder == 2) ArrayUtils.reverseBits(temp);
                        decoder.setInput(temp);
                        int numOfBytes = decoder.decode(pixels, offset, stripBytes[i]);
                        offset += numOfBytes;
                    }
                }
                if(bitsPerSample <= 8) {
                    if(predictor == 2 && planaryConfiguration == 1)
                        pixels = applyDePredictor(samplesPerPixel, pixels, imageWidth, imageHeight);
                    db = new DataBufferByte(pixels, pixels.length);
                    raster = Raster.createPackedRaster(db, imageWidth, imageHeight, bitsPerSample, null);
                    cm = new IndexColorModel(bitsPerSample, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
                    if(samplesPerPixel == 2) { // Deal with alpha transparency
                        if(e_photoMetric == PhotoMetric.WHITE_IS_ZERO) IMGUtils.invertBits(pixels, 2);
                        numOfBands = samplesPerPixel;
                        int[] bandOffsets = {0, 0, 0, 1};
                        hasAlpha = true;
                        transparency = Transparency.TRANSLUCENT;
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_BYTE);
                        raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth * numOfBands, numOfBands, bandOffsets, null);
                    }
                } else if(bitsPerSample <= 16) {
                    short[] tempArray = (short[])ArrayUtils.toNBits(bitsPerSample, pixels, samplesPerPixel * imageWidth, (bitsPerSample % 8 == 0) ? endian == IOUtils.BIG_ENDIAN : true);
                    if (predictor == 2 && planaryConfiguration == 1)
                        tempArray = applyDePredictor(samplesPerPixel, tempArray, imageWidth, imageHeight);
                    if(e_photoMetric == PhotoMetric.WHITE_IS_ZERO) IMGUtils.invertBits(tempArray, samplesPerPixel);
                    // Auto-scale 16-bit data to full range for display
                    int min = 65535, max = 0;
                    for (short v : tempArray) {
                        int val = v & 0xFFFF;
                        if (val < min) min = val;
                        if (val > max) max = val;
                    }
                    if (max > min) {
                        for (int i = 0; i < tempArray.length; i++) {
                            int val = tempArray[i] & 0xFFFF;
                            int scaled = (int)(((val - min) * 65535L) / (max - min));
                            tempArray[i] = (short)scaled;
                        }
                    }
                    if (samplesPerPixel == 2) {
                        hasAlpha = true;
                        transparency = Transparency.TRANSLUCENT;
                    }
                    cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_USHORT);
                    raster = cm.createCompatibleWritableRaster(imageWidth, imageHeight);
                    raster.setDataElements(0, 0, imageWidth, imageHeight, tempArray);
                } else if(bitsPerSample == 64) {
                    double[] tempArray = ArrayUtils.toDoubleArray(pixels, endian == IOUtils.BIG_ENDIAN);
                    cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), null, hasAlpha, isAssociatedAlpha, transparency, DataBuffer.TYPE_DOUBLE);
                    db = new DataBufferDouble(tempArray, tempArray.length);
                    SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_DOUBLE, imageWidth, imageHeight, samplesPerPixel, imageWidth * samplesPerPixel, new int[]{0, 0, 0});
                    raster = Raster.createWritableRaster(sampleModel, db, null);
                } else throw new UnsupportedOperationException("Unsupported bit depth: " + bitsPerSample);
                return new BufferedImage(cm, raster, false, null);

            case LOGL:
            case LOGLUV:
                bytesPerScanLine = samplesPerPixel * ((imageWidth * bitsPerSample + 7) / 8);
                if(e_photoMetric == PhotoMetric.LOGLUV && compression == TiffFieldEnum.Compression.SGILOG24) {
                    bytesPerScanLine = imageWidth * 3; // 24-bit packed format
                }
                totalBytes = bytesPerScanLine * imageHeight;
                byte[] inputData = new byte[totalBytes];
                switch(compression) {
                    case NONE:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripBytes[i];
                            randIS.seek(stripOffsets[i]);
                            randIS.readFully(inputData, offset, bytes2Read);
                            offset += bytes2Read;
                        }
                        break;
                    case SGILOG:
                    case SGILOG24:
                        for(int i = 0; i < stripByteCounts.length; i++) {
                            int bytes2Read = stripByteCounts[i];
                            randIS.seek(stripOffsets[i]);
                            randIS.readFully(inputData, offset, bytes2Read);
                            offset += bytes2Read;
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported compression for LogLuv: " + compression);
                }
                pixels = new byte[imageWidth * imageHeight * 3];
                SGILogDecoder sgiLogDecoder;
                if(compression == TiffFieldEnum.Compression.SGILOG24) {
                    sgiLogDecoder = new SGILogDecoder(imageWidth, imageHeight, true);
                } else {
                    sgiLogDecoder = new SGILogDecoder(imageWidth, imageHeight, samplesPerPixel);
                }
                sgiLogDecoder.setInput(inputData);
                sgiLogDecoder.decode(pixels, 0, pixels.length);
                db = new DataBufferByte(pixels, pixels.length);
                int[] rgbBandOffsets = {0, 1, 2};
                raster = Raster.createInterleavedRaster(db, imageWidth, imageHeight, imageWidth * 3, 3, rgbBandOffsets, null);
                cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                return new BufferedImage(cm, raster, false, null);

            default:
        }
        return null;
    }

    private BufferedImage decodeTiledTiff(IFD ifd) throws Exception {
        TiffField<?> field = ifd.getField(TiffTag.COMPRESSION);
        short[] data = (short[])field.getData();
        TiffFieldEnum.Compression compression = TiffFieldEnum.Compression.fromValue(data[0] & 0xffff);
        LOGGER.info("Compression type: {}", compression.getDescription());

        TiffField<?> f_tileOffsets = ifd.getField(TiffTag.TILE_OFFSETS);
        if(f_tileOffsets == null) f_tileOffsets = ifd.getField(TiffTag.STRIP_OFFSETS);
        if(f_tileOffsets == null) {
            LOGGER.error("Missing required field tileOffsets");
            return null;
        }
        int[] tileOffsets = f_tileOffsets.getDataAsLong();

        int[] tileByteCounts = null;
        TiffField<?> f_tileByteCounts = ifd.getField(TiffTag.TILE_BYTE_COUNTS);
        if(f_tileByteCounts == null) f_tileByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS);
        if(f_tileByteCounts == null) {
            if(tileOffsets.length == 1) tileByteCounts = new int[]{0};
            else {
                LOGGER.error("Missing required field tileByteCounts");
                return null;
            }
        } else {
            tileByteCounts = f_tileByteCounts.getDataAsLong();
        }

        int imageWidth = ifd.getField(TiffTag.IMAGE_WIDTH).getDataAsLong()[0];
        int imageHeight = ifd.getField(TiffTag.IMAGE_LENGTH).getDataAsLong()[0];
        LOGGER.info("Image width: {}", imageWidth);
        LOGGER.info("Image height: {}", imageHeight);

        TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH);
        TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH);
        int tileWidth = imageWidth;
        if(f_tileWidth != null) tileWidth = (int)f_tileWidth.getDataAsLong()[0];
        int tileLength = imageHeight;
        if(f_tileLength != null) tileLength = (int)f_tileLength.getDataAsLong()[0];

        TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION);
        int photoMetric = PhotoMetric.WHITE_IS_ZERO.getValue();
        if(f_photoMetric != null) photoMetric = (int)f_photoMetric.getDataAsLong()[0];
        TiffFieldEnum.PhotoMetric e_photoMetric = TiffFieldEnum.PhotoMetric.fromValue(photoMetric);
        LOGGER.info("PhotoMetric: {}", e_photoMetric);

        TiffField<?> f_bitsPerSample = ifd.getField(TiffTag.BITS_PER_SAMPLE);
        int bitsPerSample = 1;
        if(f_bitsPerSample != null) bitsPerSample = (int)f_bitsPerSample.getDataAsLong()[0];
        int correctedBitsPerSample = TIFFTweaker.detectActualBitsPerSample(ifd, compression);
        if (correctedBitsPerSample != bitsPerSample) {
            LOGGER.warn("BitsPerSample tag {} mismatches data; using {} based on TileByteCounts", bitsPerSample, correctedBitsPerSample);
            bitsPerSample = correctedBitsPerSample;
            ifd.addField(new ShortField(TiffTag.BITS_PER_SAMPLE.getValue(), new short[] {(short)bitsPerSample}));
        }
        LOGGER.info("Bits per sample: {}", bitsPerSample);

        TiffField<?> f_samplesPerPixel = ifd.getField(TiffTag.SAMPLES_PER_PIXEL);
        int samplesPerPixel = 1;
        if(f_samplesPerPixel != null) samplesPerPixel = (int)f_samplesPerPixel.getDataAsLong()[0];
        LOGGER.info("Samples per pixel: {}", samplesPerPixel);

        TiffField<?> f_predictor = ifd.getField(TiffTag.PREDICTOR);
        int predictor = 0;
        if(f_predictor != null) {
            predictor = (int)f_predictor.getDataAsLong()[0];
            LOGGER.info("Predictor: {}", predictor);
        }

        TiffField<?> f_planaryConfiguration = ifd.getField(TiffTag.PLANAR_CONFIGURATTION);
        int planaryConfiguration = 1;
        if(f_planaryConfiguration != null) planaryConfiguration = (int)f_planaryConfiguration.getDataAsLong()[0];
        TiffFieldEnum.PlanarConfiguration e_planaryConfiguration = TiffFieldEnum.PlanarConfiguration.fromValue(planaryConfiguration);
        LOGGER.info("Planary configuration: {}", e_planaryConfiguration);

        TiffField<?> f_sampleFormat = ifd.getField(TiffTag.SAMPLE_FORMAT);
        TiffField<?> f_sampleMaxValue = ifd.getField(TiffTag.S_MAX_SAMPLE_VALUE);
        TiffField<?> f_sampleMinValue = ifd.getField(TiffTag.S_MIN_SAMPLE_VALUE);

        boolean floatSample = false;
        boolean isAssociatedAlpha = false;
        if(f_sampleFormat != null && f_sampleFormat.getDataAsLong()[0] == 3) { // Floating point sample data type
            floatSample = true;
            double maxValue = (bitsPerSample <= 32) ? Float.MAX_VALUE : Double.MAX_VALUE;
            double minValue = (bitsPerSample <= 32) ? Float.MIN_VALUE : Double.MIN_VALUE;
            if(bitsPerSample <= 32 && f_sampleMaxValue != null) maxValue = ((float[])f_sampleMaxValue.getData())[0];
            else if(bitsPerSample > 32 && f_sampleMaxValue != null) maxValue = ((double[])f_sampleMaxValue.getData())[0];
            if(bitsPerSample <= 32 && f_sampleMinValue != null) minValue = ((float[])f_sampleMinValue.getData())[0];
            else if(bitsPerSample > 32 && f_sampleMinValue != null) minValue = ((double[])f_sampleMinValue.getData())[0];
            LOGGER.info("Sample MAX value: {}", maxValue);
            LOGGER.info("Sample MIN vlaue: {}", minValue);
        }

        int tilesAcross = (imageWidth + tileWidth - 1) / tileWidth;
        int tilesDown = (imageHeight + tileLength - 1) / tileLength;
        int tilesPerImage = tilesAcross * tilesDown;
        ImageDecoder decoder = null;
        byte[] pixels = null;
        int[] tileBytes = TIFFTweaker.getUncompressedStripByteCounts(ifd, tileOffsets.length);

        int xoff = 0, yoff = 0, tileCounter = 0;
        WritableRaster raster = null;
        DataBuffer db = null;
        ColorModel cm = null;

        switch(e_photoMetric) {
            case PALETTE_COLOR:
                short[] colorMap = (short[])ifd.getField(TiffTag.COLORMAP).getData();
                int[] rgbColorPalette = new int[colorMap.length / 3];
                int numOfColors = (1 << bitsPerSample);
                int numOfColors2 = (numOfColors << 1);
                for(int i = 0, index = 0; i < colorMap.length / 3; i++) {
                    rgbColorPalette[index++] = 0xff000000 | ((colorMap[i] & 0xff00) << 8) |
                                               ((colorMap[i + numOfColors] & 0xff00)) | 
                                               ((colorMap[i + numOfColors2] & 0xff00) >> 8);
                }
                int bytesPerScanLine = tilesAcross * (tileWidth * bitsPerSample + 7) / 8;
                pixels = new byte[bytesPerScanLine * tilesDown * tileLength];
                db = new DataBufferByte(pixels, pixels.length);
                cm = new IndexColorModel(bitsPerSample, rgbColorPalette.length, rgbColorPalette, 0, false, -1, DataBuffer.TYPE_BYTE);
                if(bitsPerSample < 8) {
                    raster = Raster.createPackedRaster(db, tileWidth * tilesAcross, tileLength * tilesDown, bitsPerSample, null);
                } else {
                    int[] off = {0}; // band offset, we have only one band start at 0
                    if(bitsPerSample > 8) {
                        short[] spixels = new short[pixels.length / 2];
                        db = new DataBufferUShort(spixels, spixels.length);
                        cm = new IndexColorModel(bitsPerSample, rgbColorPalette.length, rgbColorPalette, 0, false, 1, DataBuffer.TYPE_USHORT);
                    }
                    raster = Raster.createInterleavedRaster(db, tileWidth * tilesAcross, tileLength * tilesDown, tileWidth * tilesAcross, 1, off, null);
                }
                switch(compression) {
                    case NONE:
                        for(int i = 0; i < tileByteCounts.length; i++) {
                            byte[] temp = new byte[tileByteCounts[i]];
                            randIS.seek(tileOffsets[i]);
                            randIS.readFully(temp);
                            if(bitsPerSample == 16) {
                                raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.toShortArray(temp, endian == IOUtils.BIG_ENDIAN));
                            } else {
                                DataBuffer tileDataBuffer = new DataBufferByte(temp, temp.length);
                                WritableRaster tileRaster = Raster.createPackedRaster(tileDataBuffer, tileWidth, tileLength, bitsPerSample, null);
                                raster.setDataElements(xoff, yoff, tileRaster);
                            }
                            xoff += tileWidth;
                            tileCounter++;
                            if(tileCounter >= tilesAcross) {
                                xoff = 0;
                                yoff += tileLength;
                                tileCounter = 0;
                            }
                        }
                        break;
                    case LZW:
                        decoder = new LZWTreeDecoder(8, true);
                        break;
                    case DEFLATE:
                    case DEFLATE_ADOBE:
                        decoder = new DeflateDecoder();
                        break;
                }
                if (decoder != null) {
                    for(int i = 0; i < tileByteCounts.length; i++) {
                        byte[] temp = new byte[tileByteCounts[i]];
                        byte[] temp2 = new byte[tileBytes[i]];
                        randIS.seek(tileOffsets[i]);
                        randIS.readFully(temp);
                        decoder.setInput(temp);
                        decoder.decode(temp2, 0, tileBytes[i]);
                        if(bitsPerSample == 16) {
                            raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.toShortArray(temp2, endian == IOUtils.BIG_ENDIAN));
                        } else {
                            raster.setDataElements(xoff, yoff, tileWidth, tileLength, temp2);
                        }
                        xoff += tileWidth;
                        tileCounter++;
                        if(tileCounter >= tilesAcross) {
                            xoff = 0;
                            yoff += tileLength;
                            tileCounter = 0;
                        }
                    }
                }
                return new BufferedImage(cm, raster, false, null).getSubimage(0, 0, imageWidth, imageHeight);

            case RGB:
                int[] bandoff = new int[samplesPerPixel];
                int[] nBits = new int[samplesPerPixel];
                Arrays.fill(nBits, bitsPerSample);
                for(int i = 0; i < samplesPerPixel; i++) bandoff[i] = i;
                boolean transparent = false;
                int trans = Transparency.OPAQUE;
                if (samplesPerPixel >= 4) {
                    trans = Transparency.TRANSLUCENT;
                    transparent = true;
                }
                if(floatSample) {
                    if(bitsPerSample == 64) {
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, false, trans, DataBuffer.TYPE_DOUBLE);
                    } else if (bitsPerSample >= 16 && bitsPerSample <= 32) {
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, false, trans, DataBuffer.TYPE_FLOAT);
                    }
                } else {
                    if(bitsPerSample < 8) {
                        cm = new DirectColorModel(bitsPerSample * samplesPerPixel, redMask[bitsPerSample], greenMask[bitsPerSample], blueMask[bitsPerSample]);
                    } else if(bitsPerSample == 16) {
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_USHORT);
                    } else if(bitsPerSample == 24) {
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_INT);
                    } else if(bitsPerSample == 32 || bitsPerSample == 64) {
                        cm = new Int32ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), transparent);
                    } else {
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, transparent, isAssociatedAlpha, trans, DataBuffer.TYPE_BYTE);
                    }
                }
                raster = cm.createCompatibleWritableRaster(tileWidth * tilesAcross, tileLength * tilesDown);
                switch(compression) {
                    case NONE:
                        if(planaryConfiguration == 1) {
                            for(int i = 0; i < tileByteCounts.length; i++) {
                                byte[] temp = new byte[tileByteCounts[i]];
                                randIS.seek(tileOffsets[i]);
                                randIS.readFully(temp);
                                Object tempArray = null;
                                if(floatSample) {
                                    if(bitsPerSample == 64) tempArray = ArrayUtils.toDoubleArray(temp, endian == IOUtils.BIG_ENDIAN);
                                    else if (bitsPerSample == 32) tempArray = ArrayUtils.toFloatArray(temp, endian == IOUtils.BIG_ENDIAN);
                                    else if(bitsPerSample == 24) tempArray = ArrayUtils.to24BitFloatArray(temp, endian == IOUtils.BIG_ENDIAN);
                                    else if(bitsPerSample == 16) tempArray = ArrayUtils.to16BitFloatArray(temp, endian == IOUtils.BIG_ENDIAN);
                                } else {
                                    if(bitsPerSample < 8) tempArray = ArrayUtils.toNBits(bitsPerSample * samplesPerPixel, temp, tileWidth, true);
                                    else if(bitsPerSample == 8) tempArray = temp;
                                    else if(bitsPerSample % 8 == 0) tempArray = ArrayUtils.toNBits(bitsPerSample, temp, samplesPerPixel * tileWidth, endian == IOUtils.BIG_ENDIAN);
                                    else tempArray = ArrayUtils.toNBits(bitsPerSample, temp, samplesPerPixel * tileWidth, true);
                                }
                                raster.setDataElements(xoff, yoff, tileWidth, tileLength, tempArray);
                                xoff += tileWidth;
                                tileCounter++;
                                if (tileCounter >= tilesAcross) {
                                    xoff = 0;
                                    yoff += tileLength;
                                    tileCounter = 0;
                                }
                            }
                        } else {
                            byte[][] rgb = new byte[samplesPerPixel][];
                            Raster tileRaster = null;
                            DataBuffer tileDatabuffer = null;
                            int[] bankIndices = {0, 1, 2};
                            int[] bandOffsets = {0, 0, 0};
                            int dataBufferType = DataBuffer.TYPE_BYTE;
                            for(int i = 0; i < tilesPerImage; i++) {
                                rgb[0] = new byte[tileByteCounts[i]];
                                randIS.seek(tileOffsets[i]);
                                randIS.readFully(rgb[0]);
                                int greenOff = i + tilesPerImage;
                                rgb[1] = new byte[tileByteCounts[greenOff]];
                                randIS.seek(tileOffsets[greenOff]);
                                randIS.readFully(rgb[1]);
                                int blueOff = greenOff + tilesPerImage;
                                rgb[2] = new byte[tileByteCounts[blueOff]];
                                randIS.seek(tileOffsets[blueOff]);
                                randIS.readFully(rgb[2]);

                                if(floatSample) {
                                    if(bitsPerSample >= 16 && bitsPerSample <= 32) {
                                        float[][] floats = new float[samplesPerPixel][];
                                        if(bitsPerSample == 16) {
                                            for(int j = 0; j < samplesPerPixel; j++) floats[j] = ArrayUtils.to16BitFloatArray(rgb[j], endian == IOUtils.BIG_ENDIAN);
                                        } else if(bitsPerSample == 24) {
                                            for(int j = 0; j < samplesPerPixel; j++) floats[j] = ArrayUtils.to24BitFloatArray(rgb[j], endian == IOUtils.BIG_ENDIAN);
                                        } else if(bitsPerSample == 32) {
                                            for(int j = 0; j < samplesPerPixel; j++) floats[j] = ArrayUtils.toFloatArray(rgb[j], endian == IOUtils.BIG_ENDIAN);
                                        }
                                        tileDatabuffer = new DataBufferFloat(floats, floats[0].length);
                                        dataBufferType = DataBuffer.TYPE_FLOAT;
                                    } else if(bitsPerSample == 64) {
                                        double[][] doubles = new double[samplesPerPixel][];
                                        for(int j = 0; j < samplesPerPixel; j++) doubles[j] = ArrayUtils.toDoubleArray(rgb[j], endian == IOUtils.BIG_ENDIAN);
                                        tileDatabuffer = new DataBufferDouble(doubles, doubles[0].length);
                                        dataBufferType = DataBuffer.TYPE_DOUBLE;
                                    }
                                    SampleModel sampleModel = new BandedSampleModel(dataBufferType, tileWidth, tileLength, tileWidth, bankIndices, bandOffsets);
                                    tileRaster = Raster.createWritableRaster(sampleModel, tileDatabuffer, null);
                                } else {
                                    if(bitsPerSample == 16) {
                                        tileDatabuffer = new DataBufferUShort(new short[][]{ArrayUtils.toShortArray(rgb[0], endian == IOUtils.BIG_ENDIAN), ArrayUtils.toShortArray(rgb[1], endian == IOUtils.BIG_ENDIAN), ArrayUtils.toShortArray(rgb[2], endian == IOUtils.BIG_ENDIAN)}, tileWidth * tileLength);
                                    } else if(bitsPerSample > 16 && bitsPerSample <= 32) {
                                        boolean bigEndian = (bitsPerSample % 8 == 0) ? endian == IOUtils.BIG_ENDIAN : true;
                                        tileDatabuffer = new DataBufferInt(new int[][]{(int[])(ArrayUtils.toNBits(bitsPerSample, rgb[0], tileWidth, bigEndian)), (int[])(ArrayUtils.toNBits(bitsPerSample, rgb[1], tileWidth, bigEndian)), (int[])(ArrayUtils.toNBits(bitsPerSample, rgb[2], tileWidth, bigEndian))}, tileWidth * tileLength);
                                    } else if(bitsPerSample == 64) {
                                        tileDatabuffer = new DataBufferInt(new int[][]{ArrayUtils.to32BitsLongArray(rgb[0], endian == IOUtils.BIG_ENDIAN), ArrayUtils.to32BitsLongArray(rgb[1], endian == IOUtils.BIG_ENDIAN), ArrayUtils.to32BitsLongArray(rgb[2], endian == IOUtils.BIG_ENDIAN)}, tileWidth * tileLength);
                                    } else {
                                        tileDatabuffer = new DataBufferByte(rgb, rgb[0].length);
                                    }
                                    tileRaster = Raster.createBandedRaster(tileDatabuffer, tileWidth, tileLength, tileWidth, bankIndices, bandOffsets, null);
                                }
                                raster.setRect(xoff, yoff, tileRaster);
                                xoff += tileWidth;
                                tileCounter++;
                                if (tileCounter >= tilesAcross) {
                                    xoff = 0;
                                    yoff += tileLength;
                                    tileCounter = 0;
                                }
                            }
                        }
                        break;
                    case PACKBITS:
                        for(int i = 0; i < tileByteCounts.length; i++) {
                            byte[] temp = new byte[tileByteCounts[i]];
                            randIS.seek(tileOffsets[i]);
                            randIS.readFully(temp);
                            byte[] temp2 = new byte[tileBytes[i]];
                            Packbits.unpackbits(temp, temp2);
                            if(bitsPerSample == 16) {
                                raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.toShortArray(temp2, endian == IOUtils.BIG_ENDIAN));
                            } else {
                                raster.setDataElements(xoff, yoff, tileWidth, tileLength, temp2);
                            }
                            xoff += tileWidth;
                            tileCounter++;
                            if (tileCounter >= tilesAcross) {
                                xoff = 0;
                                yoff += tileLength;
                                tileCounter = 0;
                            }
                        }
                        break;
                    case OLD_JPG:
                    case JPG:
                        TiffField<?> jpegTablesFieldTiled = ifd.getField(TiffTag.JPEG_TABLES);
                        BufferedImage fullImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = fullImage.createGraphics();
                        for(int i = 0; i < tileByteCounts.length; i++) {
                            randIS.seek(tileOffsets[i]);
                            byte[] tileData = new byte[tileByteCounts[i]];
                            randIS.readFully(tileData);
                            try {
                                BufferedImage tileImage = null;
                                if (jpegTablesFieldTiled != null && compression == TiffFieldEnum.Compression.JPG) {
                                    byte[] jpegTables = (byte[])jpegTablesFieldTiled.getData();
                                    ByteArrayOutputStream completeJPEG = new ByteArrayOutputStream();
                                    int tablesLength = jpegTables.length;
                                    if (tablesLength >= 2 && jpegTables[tablesLength-2] == (byte)0xFF && jpegTables[tablesLength-1] == (byte)0xD9) {
                                        completeJPEG.write(jpegTables, 0, tablesLength - 2);
                                    } else completeJPEG.write(jpegTables);
                                    if (tileData.length >= 2 && tileData[0] == (byte)0xFF && tileData[1] == (byte)0xD8) {
                                        completeJPEG.write(tileData, 2, tileData.length - 2);
                                    } else completeJPEG.write(tileData);
                                    BaselineJPGReader jpegReader = new BaselineJPGReader();
                                    tileImage = jpegReader.read(new ByteArrayInputStream(completeJPEG.toByteArray()), true);
                                } else {
                                    BaselineJPGReader jpegReader = new BaselineJPGReader();
                                    tileImage = jpegReader.read(new ByteArrayInputStream(tileData), true);
                                }
                                if (tileImage != null) g2d.drawImage(tileImage, xoff, yoff, null);
                            } catch (Exception e) {
                                LOGGER.error("Failed to decode JPEG tile " + i + " at offset " + tileOffsets[i], e);
                            }
                            xoff += tileWidth;
                            tileCounter++;
                            if (tileCounter >= tilesAcross) {
                                xoff = 0;
                                yoff += tileLength;
                                tileCounter = 0;
                            }
                        }
                        g2d.dispose();
                        return fullImage.getSubimage(0, 0, imageWidth, imageHeight);
                    case LZW:
                        decoder = new LZWTreeDecoder(8, true);
                        break;
                    case DEFLATE:
                    case DEFLATE_ADOBE:
                        decoder = new DeflateDecoder();
                        break;
                }
                if (decoder != null) {
                    for(int i = 0; i < tileByteCounts.length; i++) {
                        byte[] temp = new byte[tileByteCounts[i]];
                        byte[] temp2 = new byte[tileBytes[i]];
                        randIS.seek(tileOffsets[i]);
                        randIS.readFully(temp);
                        decoder.setInput(temp);
                        decoder.decode(temp2, 0, tileBytes[i]);
                        if(bitsPerSample == 16) {
                            raster.setDataElements(xoff, yoff, tileWidth, tileLength, ArrayUtils.toShortArray(temp2, endian == IOUtils.BIG_ENDIAN));
                        } else {
                            raster.setDataElements(xoff, yoff, tileWidth, tileLength, temp2);
                        }
                        xoff += tileWidth;
                        tileCounter++;
                        if (tileCounter >= tilesAcross) {
                            xoff = 0;
                            yoff += tileLength;
                            tileCounter = 0;
                        }
                    }
                }
                return new BufferedImage(cm, raster, false, null).getSubimage(0, 0, imageWidth, imageHeight);

            case YCbCr:
                if (compression != TiffFieldEnum.Compression.OLD_JPG && compression != TiffFieldEnum.Compression.JPG) {
                    throw new UnsupportedOperationException("YCbCr tiled images only support JPEG compression (found: " + compression + ")");
                }
                TiffField<?> jpegTablesYCbCr = ifd.getField(TiffTag.JPEG_TABLES);
                BufferedImage ycbcrImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2dYCbCr = ycbcrImage.createGraphics();
                xoff = 0; yoff = 0; tileCounter = 0;
                for(int i = 0; i < tileByteCounts.length; i++) {
                    randIS.seek(tileOffsets[i]);
                    byte[] tileData = new byte[tileByteCounts[i]];
                    randIS.readFully(tileData);
                    try {
                        BufferedImage tileImage = null;
                        if (jpegTablesYCbCr != null && compression == TiffFieldEnum.Compression.JPG) {
                            byte[] jpegTables = (byte[])jpegTablesYCbCr.getData();
                            ByteArrayOutputStream completeJPEG = new ByteArrayOutputStream();
                            int tablesLength = jpegTables.length;
                            if (tablesLength >= 2 && jpegTables[tablesLength-2] == (byte)0xFF && jpegTables[tablesLength-1] == (byte)0xD9) {
                                completeJPEG.write(jpegTables, 0, tablesLength - 2);
                            } else completeJPEG.write(jpegTables);
                            if (tileData.length >= 2 && tileData[0] == (byte)0xFF && tileData[1] == (byte)0xD8) {
                                completeJPEG.write(tileData, 2, tileData.length - 2);
                            } else completeJPEG.write(tileData);
                            BaselineJPGReader jpegReader = new BaselineJPGReader();
                            tileImage = jpegReader.read(new ByteArrayInputStream(completeJPEG.toByteArray()), false);
                        } else if (compression == TiffFieldEnum.Compression.OLD_JPG) {
                            try {
                                tileImage = reconstructJPEGFromSeparateTables(ifd, tileData, tileWidth, tileLength, false);
                            } catch (IOException e) {
                                LOGGER.debug("Separate tables not available, trying tile data as complete JPEG", e);
                                BaselineJPGReader jpegReaderOld = new BaselineJPGReader();
                                tileImage = jpegReaderOld.read(new ByteArrayInputStream(tileData), false);
                            }
                        }
                        if (tileImage != null) g2dYCbCr.drawImage(tileImage, xoff, yoff, null);
                    } catch (Exception e) {
                        LOGGER.error("Failed to decode YCbCr JPEG tile " + i + " at offset " + tileOffsets[i], e);
                    }
                    xoff += tileWidth;
                    tileCounter++;
                    if(tileCounter >= tilesAcross) {
                        xoff = 0; yoff += tileLength; tileCounter = 0;
                    }
                }
                g2dYCbCr.dispose();
                return ycbcrImage.getSubimage(0, 0, imageWidth, imageHeight);

            case BLACK_IS_ZERO:
            case WHITE_IS_ZERO:
                if (compression == TiffFieldEnum.Compression.OLD_JPG || compression == TiffFieldEnum.Compression.JPG) {
                    if (compression == TiffFieldEnum.Compression.OLD_JPG) return decodeOldStyleJPEG(ifd, tileOffsets, tileByteCounts);
                    else return decodeNewStyleJPEG(ifd, tileOffsets, tileByteCounts, false);
                } else if (compression == TiffFieldEnum.Compression.NONE && samplesPerPixel == 1 && planaryConfiguration == 1 && (bitsPerSample == 8 || bitsPerSample == 1)) {
                    BufferedImage img;
                    if (bitsPerSample == 8) {
                        raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, imageWidth, imageHeight, 1, null);
                        byte[] out = ((DataBufferByte)raster.getDataBuffer()).getData();
                        int tileIndex = 0;
                        for (int ty = 0; ty < tilesDown; ty++) {
                            for (int tx = 0; tx < tilesAcross; tx++, tileIndex++) {
                                int x0 = tx * tileWidth, y0 = ty * tileLength;
                                int w = Math.min(tileWidth, imageWidth - x0), h = Math.min(tileLength, imageHeight - y0);
                                randIS.seek(tileOffsets[tileIndex]);
                                byte[] tileData = new byte[w * h];
                                randIS.readFully(tileData);
                                for (int row = 0; row < h; row++) System.arraycopy(tileData, row * w, out, (y0 + row) * imageWidth + x0, w);
                            }
                        }
                        if (e_photoMetric == PhotoMetric.WHITE_IS_ZERO) {
                            for (int i = 0; i < out.length; i++) out[i] = (byte)(255 - (out[i] & 0xFF));
                        }
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                        img = new BufferedImage(cm, raster, false, null);
                    } else { // bitsPerSample == 1
                        int scanlineBytes = (imageWidth + 7) / 8;
                        int[] bandMasks = {0x80};
                        raster = Raster.createPackedRaster(DataBuffer.TYPE_BYTE, imageWidth, imageHeight, bandMasks, null);
                        byte[] out = ((DataBufferByte)raster.getDataBuffer()).getData();
                        int tileIndex = 0;
                        for (int ty = 0; ty < tilesDown; ty++) {
                            for (int tx = 0; tx < tilesAcross; tx++, tileIndex++) {
                                int x0 = tx * tileWidth, y0 = ty * tileLength;
                                int w = Math.min(tileWidth, imageWidth - x0), h = Math.min(tileLength, imageHeight - y0);
                                int tileScanlineBytes = (w + 7) / 8;
                                randIS.seek(tileOffsets[tileIndex]);
                                byte[] tileData = new byte[tileScanlineBytes * h];
                                randIS.readFully(tileData);
                                for (int row = 0; row < h; row++) {
                                    int outRow = y0 + row, outByte = outRow * scanlineBytes + (x0 / 8), inByte = row * tileScanlineBytes;
                                    if (w == tileWidth && x0 % 8 == 0) System.arraycopy(tileData, inByte, out, outByte, tileScanlineBytes);
                                    else {
                                        for (int col = 0; col < w; col++) {
                                            int bit = 7 - (col % 8), val = (tileData[inByte + (col / 8)] >> bit) & 1;
                                            int outBit = 7 - ((x0 + col) % 8), outIdx = outByte + ((x0 + col) / 8) - (x0 / 8);
                                            if (val == 1) out[outIdx] |= (1 << outBit);
                                            else out[outIdx] &= ~(1 << outBit);
                                        }
                                    }
                                }
                            }
                        }
                        if (e_photoMetric == PhotoMetric.WHITE_IS_ZERO) {
                            for (int i = 0; i < out.length; i++) out[i] = (byte)~out[i];
                        }
                        cm = new IndexColorModel(1, 2, new int[]{0x000000, 0xFFFFFF}, 0, false, -1, DataBuffer.TYPE_BYTE);
                        img = new BufferedImage(cm, raster, false, null);
                    }
                    return img;
                }
                break;

            case LOGL:
            case LOGLUV:
                int[] nBitsLogLuv = {8, 8, 8};
                ColorModel cmLogLuv = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBitsLogLuv, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                WritableRaster logLuvRaster = cmLogLuv.createCompatibleWritableRaster(tileWidth * tilesAcross, tileLength * tilesDown);
                int maxTileBytes = 0;
                for(int count : tileByteCounts) if(count > maxTileBytes) maxTileBytes = count;
                byte[] tileData = new byte[maxTileBytes];
                byte[] tilePixels = new byte[tileWidth * tileLength * 3];
                SGILogDecoder sgiTiledDecoder;
                if(compression == TiffFieldEnum.Compression.SGILOG24) sgiTiledDecoder = new SGILogDecoder(tileWidth, tileLength, true);
                else sgiTiledDecoder = new SGILogDecoder(tileWidth, tileLength, samplesPerPixel);
                xoff = 0; yoff = 0; tileCounter = 0;
                for(int i = 0; i < tileByteCounts.length; i++) {
                    randIS.seek(tileOffsets[i]);
                    randIS.readFully(tileData, 0, tileByteCounts[i]);
                    try {
                        sgiTiledDecoder.setInput(tileData);
                        sgiTiledDecoder.decode(tilePixels, 0, tilePixels.length);
                        logLuvRaster.setDataElements(xoff, yoff, tileWidth, tileLength, tilePixels);
                    } catch (Exception e) {
                        LOGGER.error("Failed to decode LogLuv tile " + i + " at offset " + tileOffsets[i], e);
                    }
                    xoff += tileWidth;
                    tileCounter++;
                    if (tileCounter >= tilesAcross) {
                        xoff = 0; yoff += tileLength; tileCounter = 0;
                    }
                }
                return new BufferedImage(cmLogLuv, logLuvRaster, false, null).getSubimage(0, 0, imageWidth, imageHeight);

            default:
        }
        return null;
    }

    public int getFrameCount() {
        if(frames != null) return frames.size();
        return super.getFrameCount();
    }

    public BufferedImage getFrame(int i) {
        if(frames == null) return null;
        if(i >= 0 && i < frames.size()) return frames.get(i);
        else throw new IllegalArgumentException("Frame index " + i + " out of bounds");
    }

    public List<BufferedImage> getFrames() {
        if(frames != null) return Collections.unmodifiableList(frames);
        return Collections.emptyList();
    }

    public BufferedImage read(InputStream is) throws Exception {
        if(!readIFDs(is)) return null;
        frames = new ArrayList<BufferedImage>();
        for(IFD page: ifds) {
            try {
                BufferedImage frame = decode(page);
                if(frame != null) frames.add(frame);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        randIS.shallowClose();
        if(frames.size() > 0) return frames.get(0);
        return null;
    }

    private boolean readHeader(RandomAccessInputStream randIS) throws IOException {
        endian = randIS.readShort();
        if(endian == IOUtils.BIG_ENDIAN) {
            LOGGER.info("Byte order: Motorola BIG_ENDIAN");
            this.randIS.setReadStrategy(ReadStrategyMM.getInstance());
        } else if (endian == IOUtils.LITTLE_ENDIAN) {
            LOGGER.info("Byte order: Intel LITTLE_ENDIAN");
            this.randIS.setReadStrategy(ReadStrategyII.getInstance());
        } else {
            LOGGER.info("Warning: invalid TIFF byte order!");
            return false;
        }
        short tiff_id = randIS.readShort();
        if(tiff_id != 0x2a) { // "*" 42 decimal
            LOGGER.error("Error: invalid tiff identifier");
            return false;
        }
        return true;
    }

    private int readIFD(int id, int offset) throws IOException {
        IFD tiffIFD = new IFD();
        LOGGER.info("IFD {} offset: byte {}", id, offset);
        randIS.seek(offset);
        int no_of_fields = randIS.readShort();
        LOGGER.info("Total number of fields for IFD {}: {}", id, no_of_fields);
        offset += 2;
        for (int i = 0; i < no_of_fields; i++) {
            LOGGER.info("TiffField {}=>", i);
            randIS.seek(offset);
            short tag = randIS.readShort();
            Tag ftag = TiffTag.fromShort(tag);
            if (ftag == TiffTag.UNKNOWN) {
                LOGGER.info("TiffTag: {} [Value: 0x{}) (Unknown)", ftag, Integer.toHexString(tag & 0xffff));
            } else {
                LOGGER.info("TiffTag: {}", ftag);
            }
            offset += 2;
            randIS.seek(offset);
            short type = randIS.readShort();
            FieldType ftype = FieldType.fromShort(type);
            LOGGER.info("Data type: {}", ftype);
            offset += 2;
            randIS.seek(offset);
            int field_length = randIS.readInt();
            LOGGER.info("TiffField length: {}", field_length);
            offset += 4;
            try {
                switch (ftype) {
                    case BYTE:
                    case SBYTE:
                    case UNDEFINED:
                        byte[] data = new byte[field_length];
                        if(field_length <= 4) {
                            randIS.seek(offset);
                            randIS.readFully(data, 0, field_length);
                        } else {
                            randIS.seek(offset);
                            randIS.seek(randIS.readInt());
                            randIS.readFully(data, 0, field_length);
                        }
                        LOGGER.info("TiffField value: {}", StringUtils.byteArrayToHexString(data, 0, 10));
                        tiffIFD.addField((ftype == FieldType.BYTE) ? new ByteField(tag, data) : (ftype == FieldType.SBYTE) ? new SByteField(tag, data) : new UndefinedField(tag, data));
                        offset += 4;
                        break;
                    case ASCII:
                        data = new byte[field_length];
                        if(field_length <= 4) {
                            randIS.seek(offset);
                            randIS.readFully(data, 0, field_length);
                        } else {
                            randIS.seek(offset);
                            randIS.seek(randIS.readInt());
                            randIS.readFully(data, 0, field_length);
                        }
                        if (data.length > 0) LOGGER.info("TiffField value: {}", new String(data, 0, Math.min(10, data.length - 1)).trim());
                        tiffIFD.addField(new ASCIIField(tag, new String(data, 0, data.length)));
                        offset += 4;
                        break;
                    case SHORT:
                    case SSHORT:
                        short[] sdata = new short[field_length];
                        if(field_length == 1) {
                            randIS.seek(offset);
                            sdata[0] = randIS.readShort();
                            offset += 4;
                        } else if (field_length == 2) {
                            randIS.seek(offset);
                            sdata[0] = randIS.readShort();
                            offset += 2;
                            randIS.seek(offset);
                            sdata[1] = randIS.readShort();
                            offset += 2;
                        } else {
                            randIS.seek(offset);
                            int tooffset = randIS.readInt();
                            randIS.seek(tooffset);
                            offset += 4;
                            data = new byte[2 * field_length];
                            randIS.readFully(data);
                            sdata = ArrayUtils.toShortArray(data, endian == IOUtils.BIG_ENDIAN);
                        }
                        tiffIFD.addField((ftype == FieldType.SHORT) ? new ShortField(tag, sdata) : new SShortField(tag, sdata));
                        LOGGER.info("TiffField value: {}", StringUtils.shortArrayToString(sdata, 0, 10, true));
                        break;
                    case LONG:
                    case SLONG:
                        int[] ldata = new int[field_length];
                        if(field_length == 1) {
                            randIS.seek(offset);
                            ldata[0] = randIS.readInt();
                            offset += 4;
                        } else {
                            randIS.seek(offset);
                            int tooffset = randIS.readInt();
                            randIS.seek(tooffset);
                            offset += 4;
                            data = new byte[4 * field_length];
                            randIS.readFully(data);
                            ldata = ArrayUtils.toIntArray(data, endian == IOUtils.BIG_ENDIAN);
                        }
                        LOGGER.info("TiffField value: {}", StringUtils.longArrayToString(ldata, 0, 10, true));
                        tiffIFD.addField((ftype == FieldType.LONG) ? new LongField(tag, ldata) : new SLongField(tag, ldata));
                        break;
                    case RATIONAL:
                        ldata = new int[2 * field_length];
                        randIS.seek(offset);
                        int tooffset = randIS.readInt();
                        randIS.seek(tooffset);
                        offset += 4;
                        data = new byte[8 * field_length];
                        randIS.readFully(data);
                        ldata = ArrayUtils.toIntArray(data, endian == IOUtils.BIG_ENDIAN);
                        tiffIFD.addField(new RationalField(tag, ldata));
                        LOGGER.info("TiffField value: {}", StringUtils.rationalArrayToString(ldata, true));
                        break;
                    case FLOAT:
                        float[] fdata = new float[field_length];
                        if(field_length == 1) {
                            randIS.seek(offset);
                            fdata[0] = randIS.readFloat();
                            offset += 4;
                        } else {
                            randIS.seek(offset);
                            int toffset = randIS.readInt();
                            offset += 4;
                            data = new byte[4 * field_length];
                            randIS.seek(toffset);
                            randIS.readFully(data);
                            fdata = ArrayUtils.toFloatArray(data, endian == IOUtils.BIG_ENDIAN);
                        }
                        tiffIFD.addField(new FloatField(tag, fdata));
                        LOGGER.info("TiffField value: {}", Arrays.toString(fdata));
                        break;
                    case DOUBLE:
                        double[] ddata = new double[field_length];
                        randIS.seek(offset);
                        toffset = randIS.readInt();
                        offset += 4;
                        data = new byte[8 * field_length];
                        randIS.seek(toffset);
                        randIS.readFully(data);
                        ddata = ArrayUtils.toDoubleArray(data, endian == IOUtils.BIG_ENDIAN);
                        tiffIFD.addField(new DoubleField(tag, ddata));
                        LOGGER.info("Field value: {}", Arrays.toString(ddata));
                        break;
                    default:
                        offset += 4;
                        break;
                }
            } catch (Exception e) {
                LOGGER.error("Error reading TIFF field with tag {} at offset {}", ftag, offset, e);
                offset += 4;
                continue;
            }
        }
        ifds.add(tiffIFD);
        LOGGER.info("********<************");
        randIS.seek(offset);
        return randIS.readInt();
    }

    protected boolean readIFDs(InputStream is) throws Exception {
        randIS = new FileCacheRandomAccessInputStream(is, bufLen);
        if(!readHeader(randIS)) return false;
        ifds = new ArrayList<IFD>();
        int offset = randIS.readInt();
        int ifdCount = 0;
        while (offset != 0) {
            try {
                offset = readIFD(ifdCount++, offset);
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            }
        }
        return true;
    }

    private static byte[] applyDePredictor(int numOfSamples, byte[] input, int imageWidth, int imageHeight) {
        for(int i = 0, inc = numOfSamples * imageWidth, maxVal = inc - numOfSamples, minVal = numOfSamples; i <= imageHeight - 1; maxVal += inc, minVal += inc, i++) {
            for (int j = minVal; j <= maxVal; j += numOfSamples) {
                for(int k = 0; k < numOfSamples; k++) {
                    input[j + k] += input[j - numOfSamples + k];
                }
            }
        }
        return input;
    }

    private static short[] applyDePredictor(int numOfSamples, short[] input, int imageWidth, int imageHeight) {
        for(int i = 0, inc = numOfSamples * imageWidth, maxVal = inc - numOfSamples, minVal = numOfSamples; i <= imageHeight - 1; maxVal += inc, minVal += inc, i++) {
            for (int j = minVal; j <= maxVal; j += numOfSamples) {
                for(int k = 0; k < numOfSamples; k++) {
                    input[j + k] += input[j - numOfSamples + k];
                }
            }
        }
        return input;
    }

    private static byte[] applyDePredictor2(byte[] input, int offset, int imageWidth, int imageHeight) {
        for(int i = imageHeight - 1, inc = imageWidth, maxVal = offset + inc - 1, minVal = offset + 1; i >= 0; maxVal += inc, minVal += inc, i--) {
            for (int j = minVal; j < maxVal; j++) {
                input[j] += input[j - 1];
            }
        }
        return input;
    }

    private void unpackStrip(byte[] pixels, int offset, int bytes2Read, int stripOffset, int stripByteCount) throws IOException {
        randIS.seek(stripOffset);
        byte[] temp = null;
        if(stripByteCount == 0) {
            temp = IOUtils.readFully(randIS, 4096);
        } else {
            temp = new byte[stripByteCount];
            randIS.readFully(temp);
        }
        byte[] temp2 = new byte[bytes2Read];
        Packbits.unpackbits(temp, temp2);
        System.arraycopy(temp2, 0, pixels, offset, bytes2Read);
    }

    private int upsampling(int offsety, int numOfDataUnit, int bytesPerUnity, int[] samplingFactor, float referenceBlacky, float referenceWhitey,
                           float referenceBlackCb, float referenceWhiteCb, float referenceBlackCr, float referenceWhiteCr, float codingRangeY,
                           float codingRangeCbCr, float lumaRed, float lumaGreen, float lumaBlue, byte[] temp, byte[] pixels,
                           int expandedImageWidth, int dataUnitsPerWidth) {
        int offset = 0, offsetX = 0;
        for(int j = 1; j <= numOfDataUnit; j++) {
            int offsetCb = offset + bytesPerUnity;
            int Cb = temp[offsetCb] & 0xff, Cr = temp[offsetCb + 1] & 0xff;
            for(int k = 0; k < samplingFactor[1]; k++) {
                for(int l = 0; l < samplingFactor[0]; l++, offsetX++) {
                    int Y = temp[offset++] & 0xff;
                    float fY = (Y - referenceBlacky) * codingRangeY / (referenceWhitey - referenceBlacky);
                    float fCb = (Cb - referenceBlackCb) * codingRangeCbCr / (referenceWhiteCb - referenceBlackCb);
                    float fCr = (Cr - referenceBlackCr) * codingRangeCbCr / (referenceWhiteCr - referenceBlackCr);
                    float R = (fCr * (2 - 2 * lumaRed) + fY), B = (fCb * (2 - 2 * lumaBlue) + fY);
                    float G = ((fY - lumaBlue * B - lumaRed * R) / lumaGreen);
                    if(R < 0) R = 0; if(R > 255) R = 255;
                    if(G < 0) G = 0; if(G > 255) G = 255;
                    if(B < 0) B = 0; if(B > 255) B = 255;
                    int yPos = offsetX + offsety * expandedImageWidth, redPos = 3 * yPos;
                    pixels[redPos] = (byte)R; pixels[redPos+1] = (byte)G; pixels[redPos+2] = (byte)B;
                }
                offsetX -= samplingFactor[0];
                offsety += 1;
            }
            offsety -= samplingFactor[1];
            offset += 2;
            offsetX += samplingFactor[0];
            if(j % dataUnitsPerWidth == 0) {
                offsety += samplingFactor[1];
                offsetX = 0;
            }
        }
        return offsety;
    }

    /**
     * Decode new-style JPEG compressed TIFF strips (compression 7)
     * New-style JPEG stores JPEG_TABLES tag with common tables for all strips,
     * or each strip contains a complete standalone JPEG image
     */
    private BufferedImage decodeNewStyleJPEG(IFD ifd, int[] stripOffsets, int[] stripByteCounts, boolean isRGBPhotometric) throws IOException {
        int imageWidth = ifd.getField(TiffTag.IMAGE_WIDTH).getDataAsLong()[0];
        int imageHeight = ifd.getField(TiffTag.IMAGE_LENGTH).getDataAsLong()[0];
        TiffField<?> jpegTablesField = ifd.getField(TiffTag.JPEG_TABLES);
        // Case 1: Each strip is a complete JPEG image
        if (jpegTablesField == null) {
            LOGGER.info("New-style JPEG without JPEG_TABLES each strip should be complete JPEG");
            // If single strip, just decode it
            if (stripOffsets.length == 1) {
                randIS.seek(stripOffsets[0]);
                byte[] jpegData = new byte[stripByteCounts[0]];
                randIS.readFully(jpegData);
                try {
                    BaselineJPGReader jpegReader = new BaselineJPGReader();
                    BufferedImage result = jpegReader.read(new ByteArrayInputStream(jpegData), isRGBPhotometric);
                    if (result != null) {
                        LOGGER.info("Successfully decoded new-style JPEG: " + result.getWidth() + "x" + result.getHeight());
                        return result;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to decode new-style JPEG strip", e);
                }
            } else {
                LOGGER.warn("New-style JPEG with multiple strips but no JPEG_TABLES limited support");
            }
        } else {
            // Case 2: JPEG_TABLES contains common tables (SOI+DQT+DHT+EOI)
            byte[] jpegTables = (byte[])jpegTablesField.getData();
            LOGGER.info("New-style JPEG with JPEG_TABLES (" + jpegTables.length + " bytes)");
            if (stripOffsets.length == 1) {
                randIS.seek(stripOffsets[0]);
                byte[] stripData = new byte[stripByteCounts[0]];
                randIS.readFully(stripData);
                ByteArrayOutputStream completeJPEG = new ByteArrayOutputStream();
                int tablesLength = jpegTables.length;
                if (tablesLength >= 2 && jpegTables[tablesLength-2] == (byte)0xFF && jpegTables[tablesLength-1] == (byte)0xD9) {
                    completeJPEG.write(jpegTables, 0, tablesLength - 2); // Skip EOI
                } else completeJPEG.write(jpegTables);
                if (stripData.length >= 2 && stripData[0] == (byte)0xFF && stripData[1] == (byte)0xD8) {
                    completeJPEG.write(stripData, 2, stripData.length - 2); // Skip SOI
                } else completeJPEG.write(stripData);
                try {
                    BaselineJPGReader jpegReader = new BaselineJPGReader();
                    BufferedImage result = jpegReader.read(new ByteArrayInputStream(completeJPEG.toByteArray()), isRGBPhotometric);
                    if (result != null) {
                        LOGGER.info("Successfully decoded new-style JPEG with tables: " + result.getWidth() + "x" + result.getHeight());
                        return result;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to decode new-style JPEG with tables", e);
                }
            } else {
                LOGGER.info("New-style JPEG with JPEG_TABLES and " + stripOffsets.length + " strips");
                TiffField<?> rowsPerStripField = ifd.getField(TiffTag.ROWS_PER_STRIP);
                int rowsPerStrip = (rowsPerStripField != null) ? (int)rowsPerStripField.getDataAsLong()[0] : imageHeight;
                BufferedImage result = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = result.createGraphics();
                int currentY = 0;
                for (int i = 0; i < stripOffsets.length; i++) {
                    randIS.seek(stripOffsets[i]);
                    byte[] stripData = new byte[stripByteCounts[i]];
                    randIS.readFully(stripData);
                    ByteArrayOutputStream completeJPEG = new ByteArrayOutputStream();
                    int tablesLength = jpegTables.length;
                    if (tablesLength >= 2 && jpegTables[tablesLength-2] == (byte)0xFF && jpegTables[tablesLength-1] == (byte)0xD9) {
                        completeJPEG.write(jpegTables, 0, tablesLength - 2);
                    } else completeJPEG.write(jpegTables);
                    if (stripData.length >= 2 && stripData[0] == (byte)0xFF && stripData[1] == (byte)0xD8) {
                        completeJPEG.write(stripData, 2, stripData.length - 2);
                    } else completeJPEG.write(stripData);
                    try {
                        BaselineJPGReader jpegReader = new BaselineJPGReader();
                        BufferedImage stripImage = jpegReader.read(new ByteArrayInputStream(completeJPEG.toByteArray()), isRGBPhotometric);
                        if (stripImage != null) {
                            g.drawImage(stripImage, 0, currentY, null);
                            currentY += stripImage.getHeight();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to decode strip " + i, e);
                    }
                }
                g.dispose();
                LOGGER.info("Successfully decoded " + stripOffsets.length + " strips into " + imageWidth + "x" + imageHeight + " image");
                return result;
            }
        }
        throw new UnsupportedCompressionException("New-style JPEG configuration not supported");
    }

    /**
     * Decode old-style JPEG compressed TIFF strips
     * Old-style JPEG (compression 6) stores JPEG tables separately in TIFF tags
     * and strips contain abbreviated JPEG data without tables
     */
    private BufferedImage decodeOldStyleJPEG(IFD ifd, int[] stripOffsets, int[] stripByteCounts) throws IOException {
        int imageWidth = ifd.getField(TiffTag.IMAGE_WIDTH).getDataAsLong()[0];
        int imageHeight = ifd.getField(TiffTag.IMAGE_LENGTH).getDataAsLong()[0];
        TiffField<?> jpegInterchangeFormat = ifd.getField(TiffTag.JPEG_INTERCHANGE_FORMAT);
        TiffField<?> jpegInterchangeLength = ifd.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH);

        if (jpegInterchangeFormat != null) {
            int headerOffset = jpegInterchangeFormat.getDataAsLong()[0];
            int headerLength = (jpegInterchangeLength != null) ? (int)jpegInterchangeLength.getDataAsLong()[0] : -1;
            if (headerLength < 0) {
                LOGGER.info("JpegInterchangeFormat without length attempting to read as complete JPEG");
                try {
                    randIS.seek(headerOffset);
                    byte[] jpegData = new byte[stripByteCounts[0]];
                    randIS.readFully(jpegData);
                    TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION);
                    int photoMetric = (f_photoMetric != null) ? (int)f_photoMetric.getDataAsLong()[0] : PhotoMetric.WHITE_IS_ZERO.getValue();
                    boolean isGrayscale = (photoMetric == PhotoMetric.BLACK_IS_ZERO.getValue() || photoMetric == PhotoMetric.WHITE_IS_ZERO.getValue());
                    BaselineJPGReader jpegReader = new BaselineJPGReader();
                    BufferedImage result = jpegReader.read(new ByteArrayInputStream(jpegData), isGrayscale);
                    if (result != null) {
                        LOGGER.info("Successfully decoded complete JPEG from JpegInterchangeFormat: " + result.getWidth() + "x" + result.getHeight());
                        return result;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to read as complete JPEG: " + e.getMessage());
                }
            }
            randIS.seek(headerOffset);
            byte[] jpegHeaders = new byte[headerLength];
            randIS.readFully(jpegHeaders);
            if (stripOffsets.length > 1) LOGGER.warn("Old-style JPEG with " + stripOffsets.length + " strips this format has limited support");
            randIS.seek(stripOffsets[0]);
            byte[] firstTwoBytes = new byte[2];
            randIS.readFully(firstTwoBytes);
            if (firstTwoBytes[0] == (byte)0xFF && firstTwoBytes[1] == (byte)0xD8) {
                LOGGER.info("Strip contains complete JPEG, using strip directly");
                randIS.seek(stripOffsets[0]);
                byte[] jpegData = new byte[stripByteCounts[0]];
                randIS.readFully(jpegData);
                try {
                    TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION);
                    int photoMetric = (f_photoMetric != null) ? (int)f_photoMetric.getDataAsLong()[0] : PhotoMetric.WHITE_IS_ZERO.getValue();
                    boolean isGrayscale = (photoMetric == PhotoMetric.BLACK_IS_ZERO.getValue() || photoMetric == PhotoMetric.WHITE_IS_ZERO.getValue());
                    BaselineJPGReader jpegReader = new BaselineJPGReader();
                    BufferedImage result = jpegReader.read(new ByteArrayInputStream(jpegData), isGrayscale);
                    if (result != null) {
                        LOGGER.info("Successfully decoded old-style JPEG from strip: " + result.getWidth() + "x" + result.getHeight());
                        return result;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to decode strip as complete JPEG, trying header+strip construction", e);
                }
            }
            ByteArrayOutputStream stripData = new ByteArrayOutputStream();
            for (int i = 0; i < stripOffsets.length; i++) {
                randIS.seek(stripOffsets[i]);
                byte[] strip = new byte[stripByteCounts[i]];
                randIS.readFully(strip);
                stripData.write(strip);
            }
            ByteArrayOutputStream completeJPEG = new ByteArrayOutputStream();
            completeJPEG.write(0xFF); completeJPEG.write(0xD8); // SOI
            if (jpegHeaders.length >= 2 && jpegHeaders[0] == (byte)0xFF && jpegHeaders[1] == (byte)0xD8) {
                completeJPEG.write(jpegHeaders, 2, jpegHeaders.length - 2);
            } else completeJPEG.write(jpegHeaders);
            completeJPEG.write(stripData.toByteArray());
            byte[] jpegBytes = completeJPEG.toByteArray();
            if (jpegBytes.length < 2 || jpegBytes[jpegBytes.length-2] != (byte)0xFF || jpegBytes[jpegBytes.length-1] != (byte)0xD9) {
                completeJPEG.write(0xFF); completeJPEG.write(0xD9); // EOI
            }
            try {
                TiffField<?> f_photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION);
                int photoMetric = (f_photoMetric != null) ? (int)f_photoMetric.getDataAsLong()[0] : PhotoMetric.WHITE_IS_ZERO.getValue();
                boolean isGrayscale = (photoMetric == PhotoMetric.BLACK_IS_ZERO.getValue() || photoMetric == PhotoMetric.WHITE_IS_ZERO.getValue());
                BaselineJPGReader jpegReader = new BaselineJPGReader();
                BufferedImage finalResult = jpegReader.read(new ByteArrayInputStream(completeJPEG.toByteArray()), isGrayscale);
                if (finalResult == null) {
                    LOGGER.error("BaselineJPGReader.read() returned null for old-style JPEG");
                    return null;
                }
                LOGGER.info("Successfully decoded old-style JPEG: " + finalResult.getWidth() + "x" + finalResult.getHeight());
                return finalResult;
            } catch (Exception e) {
                LOGGER.error("Failed to decode old-style JPEG", e);
            }
        }

        LOGGER.info("No JpegInterchangeFormat found, trying separate JPEG tables");
        TiffField<?> qTablesField = ifd.getField(TiffTag.JPEG_Q_TABLES);
        TiffField<?> dcTablesField = ifd.getField(TiffTag.JPEG_DC_TABLES);
        TiffField<?> acTablesField = ifd.getField(TiffTag.JPEG_AC_TABLES);
        if (qTablesField != null && dcTablesField != null && acTablesField != null) {
            LOGGER.info("Found separate JPEG tables, decoding " + stripOffsets.length + " strips");
            TiffField<?> rowsPerStripField = ifd.getField(TiffTag.ROWS_PER_STRIP);
            int rowsPerStrip = (rowsPerStripField != null) ? (int)rowsPerStripField.getDataAsLong()[0] : imageHeight;
            BufferedImage result = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = result.createGraphics();
            int currentY = 0;
            for (int i = 0; i < stripOffsets.length; i++) {
                randIS.seek(stripOffsets[i]);
                byte[] stripData = new byte[stripByteCounts[i]];
                randIS.readFully(stripData);
                try {
                    int stripHeight = Math.min(rowsPerStrip, imageHeight - currentY);
                    BufferedImage stripImage = reconstructJPEGFromSeparateTables(ifd, stripData, imageWidth, stripHeight, false);
                    if (stripImage != null) {
                        g.drawImage(stripImage, 0, currentY, null);
                        currentY += stripHeight;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to decode strip " + i + " with separate tables", e);
                }
            }
            g.dispose();
            LOGGER.info("Successfully decoded old-style JPEG from " + stripOffsets.length + " strips with separate tables: " + imageWidth + "x" + imageHeight);
            return result;
        }
        throw new UnsupportedCompressionException("Old-style JPEG: no JpegInterchangeFormat and no separate table tags found");
    }

    /**
     * Reconstruct a complete JPEG stream from separate TIFF JPEG table tags
     * Used for old-style JPEG (compression 6) with JPEG_Q_TABLES, JPEG_DC_TABLES, JPEG_AC_TABLES
     */
    private BufferedImage reconstructJPEGFromSeparateTables(IFD ifd, byte[] tileData, int width, int height, boolean skipYCbCrConversion) throws IOException {
        TiffField<?> qTablesField = ifd.getField(TiffTag.JPEG_Q_TABLES);
        TiffField<?> dcTablesField = ifd.getField(TiffTag.JPEG_DC_TABLES);
        TiffField<?> acTablesField = ifd.getField(TiffTag.JPEG_AC_TABLES);
        if (qTablesField == null || dcTablesField == null || acTablesField == null) {
            throw new IOException("Missing JPEG table tags for old-style JPEG");
        }
        int[] qTableOffsets = qTablesField.getDataAsLong();
        int[] dcTableOffsets = dcTablesField.getDataAsLong();
        int[] acTableOffsets = acTablesField.getDataAsLong();
        int bitsPerSample = (int)ifd.getField(TiffTag.BITS_PER_SAMPLE).getDataAsLong()[0];
        int samplesPerPixel = (int)ifd.getField(TiffTag.SAMPLES_PER_PIXEL).getDataAsLong()[0];

        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        jpeg.write(0xFF); jpeg.write(0xD8); // SOI
        for (int i = 0; i < qTableOffsets.length; i++) {
            randIS.seek(qTableOffsets[i]);
            byte[] qTable = new byte[64];
            randIS.readFully(qTable);
            jpeg.write(0xFF); jpeg.write(0xDB); // DQT
            jpeg.write(0x00); jpeg.write(67);
            jpeg.write(i);
            jpeg.write(qTable);
        }
        for (int i = 0; i < dcTableOffsets.length; i++) {
            randIS.seek(dcTableOffsets[i]);
            byte[] counts = new byte[16];
            randIS.readFully(counts);
            int numSymbols = 0;
            for (byte count : counts) numSymbols += (count & 0xFF);
            byte[] symbols = new byte[numSymbols];
            randIS.readFully(symbols);
            jpeg.write(0xFF); jpeg.write(0xC4); // DHT
            int length = 2 + 1 + 16 + numSymbols;
            jpeg.write((length >> 8) & 0xFF); jpeg.write(length & 0xFF);
            jpeg.write(i); // Table class 0=DC
            jpeg.write(counts); jpeg.write(symbols);
        }
        for (int i = 0; i < acTableOffsets.length; i++) {
            randIS.seek(acTableOffsets[i]);
            byte[] counts = new byte[16];
            randIS.readFully(counts);
            int numSymbols = 0;
            for (byte count : counts) numSymbols += (count & 0xFF);
            byte[] symbols = new byte[numSymbols];
            randIS.readFully(symbols);
            jpeg.write(0xFF); jpeg.write(0xC4); // DHT
            int length = 2 + 1 + 16 + numSymbols;
            jpeg.write((length >> 8) & 0xFF); jpeg.write(length & 0xFF);
            jpeg.write(0x10 | i); // Table class 1=AC
            jpeg.write(counts); jpeg.write(symbols);
        }
        jpeg.write(0xFF); jpeg.write(0xC0); // SOF0
        int sofLength = 8 + 3 * samplesPerPixel;
        jpeg.write((sofLength >> 8) & 0xFF); jpeg.write(sofLength & 0xFF);
        jpeg.write(bitsPerSample);
        jpeg.write((height >> 8) & 0xFF); jpeg.write(height & 0xFF);
        jpeg.write((width >> 8) & 0xFF); jpeg.write(width & 0xFF);
        jpeg.write(samplesPerPixel);
        if (samplesPerPixel == 3) {
            jpeg.write(1); jpeg.write(0x22); jpeg.write(0);
            jpeg.write(2); jpeg.write(0x11); jpeg.write(1);
            jpeg.write(3); jpeg.write(0x11); jpeg.write(1);
        }
        int sosLength = 6 + 2 * samplesPerPixel;
        jpeg.write(0xFF); jpeg.write(0xDA); // SOS
        jpeg.write((sosLength >> 8) & 0xFF); jpeg.write(sosLength & 0xFF);
        jpeg.write(samplesPerPixel);
        if (samplesPerPixel == 3) {
            jpeg.write(1); jpeg.write(0x00);
            jpeg.write(2); jpeg.write(0x11);
            jpeg.write(3); jpeg.write(0x11);
        }
        jpeg.write(0x00); jpeg.write(0x3F); jpeg.write(0x00);
        int dataStart = 0;
        if (tileData.length >= 2 && tileData[0] == (byte)0xFF && tileData[1] == (byte)0xD8) dataStart = 2;
        boolean foundScanData = false;
        for (int i = dataStart; i < tileData.length - 1; i++) {
            if (tileData[i] == (byte)0xFF && tileData[i+1] == (byte)0xDA) {
                i += 2;
                int sosLen = ((tileData[i] & 0xFF) << 8) | (tileData[i+1] & 0xFF);
                i += sosLen;
                dataStart = i;
                foundScanData = true;
                break;
            }
        }
        if (foundScanData) {
            jpeg.write(tileData, dataStart, tileData.length - dataStart);
        } else {
            jpeg.write(tileData, dataStart, tileData.length - dataStart);
        }
        byte[] jpegBytes = jpeg.toByteArray();
        if (jpegBytes.length < 2 || jpegBytes[jpegBytes.length-2] != (byte)0xFF || jpegBytes[jpegBytes.length-1] != (byte)0xD9) {
            jpeg.write(0xFF); jpeg.write(0xD9); // EOI
        }
        try {
            BaselineJPGReader jpegReader = new BaselineJPGReader();
            return jpegReader.read(new ByteArrayInputStream(jpeg.toByteArray()), skipYCbCrConversion);
        } catch (Exception e) {
            LOGGER.error("Failed to decode reconstructed JPEG", e);
            throw new IOException("Failed to decode reconstructed JPEG: " + e.getMessage(), e);
        }
    }
}