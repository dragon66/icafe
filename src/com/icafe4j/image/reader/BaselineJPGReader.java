package com.icafe4j.image.reader;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ColorConvertOp;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ColorSpace;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.icafe4j.image.compression.huffman.HuffmanTbl;
import com.icafe4j.image.jpeg.*;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.jpeg.JpegExif;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.image.util.DCT;
import com.icafe4j.io.IOUtils;
import com.icafe4j.util.ArrayUtils;

/**
 * Simple Baseline JPEG Decoder
 *
 * Supports:
 * Baseline DCT (SOF0) and Extended Sequential DCT (SOF1)
 * 8-bit and 12-bit sample precision
 * Grayscale images (1 component)
 * RGB/YCbCr images (3 components)
 * CMYK/YCCK images (4 components)
 * Sequential encoding (non-progressive)
 * Subsampling (4:4:4, 4:2:2, 4:2:0)
 * Restart marker support for error recovery
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.2 01/03/2026
 */
public class BaselineJPGReader extends ImageReader {
    private int[][] qTables = new int[4][]; // Support up to 4 quantization tables
    private HuffmanTbl[] dcTables = new HuffmanTbl[4]; // Support up to 4 DC tables
    private HuffmanTbl[] acTables = new HuffmanTbl[4]; // Support up to 4 AC tables
    private int numComponents;
    private int[] previousDC = new int[4]; // DC predictors for each component
    private SOFReader sofReader;
    private int precision = 8; // Default to 8-bit, can be 12-bit for SOF1
    private int maxHSampling;
    private int maxVSampling;
    private int restartInterval = 0; // MCUs between restart markers (0 = no restart)
    private boolean isYCCK = false; // true if YCCK, false if CMYK (for 4-component images)
    private ICC_ColorSpace iccColorSpace = null; // Optional ICC profile for CMYK color correction
    private ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    private byte[] iccProfileData = null; // ICC profile extracted during parsing
    private byte[][] iccChunks = null; // For multi-chunk ICC profiles
    private boolean skipYCbCrConversion = false; // For TIFF-embedded RGB JPEG (data is already RGB, not YCbCr)
    private static final int[] ZIGZAG = JPGConsts.getZigzagMatrix(); // Zigzag scan order: maps zigzag position -> natural position
    private int orientation = 1; // Default orientation

    /**
     * Decode a baseline JPEG from input stream
     * ICC profile is automatically detected and applied if present in the JPEG
     */
    public BufferedImage read(InputStream is) throws IOException {
        return read(is, false);
    }

    /**
     * Decode a baseline JPEG from input stream with control over YCbCr conversion
     * @param is Input stream containing JPEG data
     * @param skipYCbCrConversion If true, treats 3-component data as RGB (no YCbCr-RGB conversion).
     * Use this for TIFF-embedded JPEG with Photometric Interpretation = RGB where JPEG data is already RGB, not YCbCr.
     * @return Decoded BufferedImage
     */
    public BufferedImage read(InputStream is, boolean skipYCbCrConversion) throws IOException {
        // Reset state for each decode
        iccProfileData = null;
        iccColorSpace = null;
        iccChunks = null;
        this.skipYCbCrConversion = skipYCbCrConversion;

        // Parse JPEG structure (will extract ICC profile if present)
        if (!parseJPEG(is)) {
            throw new IOException("Failed to parse JPEG structure");
        }

        // Create ICC color space if profile was found during parsing
        if (iccProfileData != null) {
            try {
                ICC_Profile profile = ICC_Profile.getInstance(iccProfileData);
                this.iccColorSpace = new ICC_ColorSpace(profile);

                // Skip ICC transformation for 3-component images if profile is sRGB (ICC transformation is very slow)
                if (iccColorSpace.getNumComponents() == 3 && iccColorSpace.getType() == ColorSpace.TYPE_RGB) {
                    // Check if this is essentially sRGB by comparing to standard
                    this.iccColorSpace = null; // Skip transformation for RGB images
                }
            } catch (Exception e) {
                // If ICC profile is invalid, continue without it
                this.iccColorSpace = null;
            }
        }

        BufferedImage image = null;
        if (numComponents == 1) {
            image = readGrayscale(is);
        } else if (numComponents == 3) {
            image = readColor(is);
        } else if (numComponents == 4) {
            image = read4Component(is);
        } else {
            throw new IOException("Unsupported number of components: " + numComponents);
        }

        // Check if rotation is needed based on EXIF orientation
        if (orientation != 1) {
            image = applyEXIFOrientation(image, orientation);
        }

        return image;
    }

    /**
     * Decode grayscale JPEG
     */
    private BufferedImage readGrayscale(InputStream is) throws IOException {
        // Calculate MCU dimensions
        int mcuWidth = (width + 7) / 8;
        int mcuHeight = (height + 7) / 8;

        // Allocate image buffer
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        // Decode image data
        BitInputStream bitStream = new BitInputStream(is);
        Arrays.fill(previousDC, 0);

        Component comp = sofReader.getComponents()[0];
        int mcuCount = 0;
        int totalMCUs = mcuWidth * mcuHeight;
        int errorCount = 0;

        while (mcuCount < totalMCUs) {
            int mcuY = mcuCount / mcuWidth;
            int mcuX = mcuCount % mcuWidth;

            try {
                // Reset DC predictors at restart intervals
                if (restartInterval > 0 && mcuCount > 0 && mcuCount % restartInterval == 0) {
                    Arrays.fill(previousDC, 0);
                    bitStream.reset();
                }

                // Decode one 8x8 block
                float[][] block = decodeBlock(bitStream, comp, 0);
                // Apply inverse DCT
                float[][] spatial = DCT.inverseDCT(block);

                // Normalize 12-bit to 8-bit range if needed
                if (precision == 12) {
                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 8; j++) {
                            spatial[i][j] /= 16.0f;
                        }
                    }
                }

                // Copy to image buffer (level shift by +128)
                copyBlockToImage(spatial, pixels, mcuX * 8, mcuY * 8, width, height);

            } catch (Exception e) {
                // Error in this MCU try to resynchronize
                errorCount++;
                if (errorCount == 1) {
                    System.err.println("Warning: JPEG decoding errors encountered");
                }
                // Fill the current MCU with neutral grey (128)
                fillBlockWithGrey(pixels, mcuX * 8, mcuY * 8, 8, 8, width, height, (byte) 128);

                // Try to find next restart marker to resynchronize
                if (restartInterval > 0) {
                    try {
                        int foundMarker = bitStream.searchForRestartMarker();
                        if (foundMarker >= 0 && foundMarker <= 7) {
                            // Found a restart marker jump to next boundary
                            // NOTE: We ignore the marker number like HuffmanJPGReader does
                            int nextRestartMCU = ((mcuCount / restartInterval) + 1) * restartInterval;
                            Arrays.fill(previousDC, 0);

                            for (int fillMCU = mcuCount + 1; fillMCU < nextRestartMCU && fillMCU < totalMCUs; fillMCU++) {
                                int filly = fillMCU / mcuWidth;
                                int fillx = fillMCU % mcuWidth;
                                fillBlockWithGrey(pixels, fillx * 8, filly * 8, 8, 8, width, height, (byte) 128);
                            }
                            mcuCount = nextRestartMCU; // continue skips mcuCount++ at loop end
                            continue;
                        } else {
                            // No more restart markers fill all remaining MCUs
                            for (int fillMCU = mcuCount; fillMCU < totalMCUs; fillMCU++) {
                                int filly = fillMCU / mcuWidth;
                                int fillX = fillMCU % mcuWidth;
                                fillBlockWithGrey(pixels, fillX * 8, filly * 8, 8, 8, width, height, (byte) 128);
                            }
                            System.err.println("No more restart markers found after error, filled remaining MCUs with grey");
                            break; // Exit loop
                        }
                    } catch (IOException ex) {
                        // Fill all remaining MCUs on I/O error
                        for (int fillMCU = mcuCount; fillMCU < totalMCUs; fillMCU++) {
                            int filly = fillMCU / mcuWidth;
                            int fillX = fillMCU % mcuWidth;
                            fillBlockWithGrey(pixels, fillX * 8, filly * 8, 8, 8, width, height, (byte) 128);
                        }
                        break;
                    }
                } else {
                    // No restart markers fill remaining MCUs with grey
                    int filledMCUs = totalMCUs - mcuCount;
                    for (int fillMCU = mcuCount; fillMCU < totalMCUs; fillMCU++) {
                        int filly = fillMCU / mcuWidth;
                        int fillX = fillMCU % mcuWidth;
                        fillBlockWithGrey(pixels, fillX * 8, filly * 8, 8, 8, width, height, (byte) 128);
                    }
                    errorCount += filledMCUs;
                    System.err.println("No restart markers available, filled remaining MCUs with grey");
                    break;
                }
            }
            mcuCount++;
        }

        if (errorCount > 0) {
            double percentDecoded = ((totalMCUs - errorCount) * 100.0 / totalMCUs);
            System.err.println("Completed with " + errorCount + " MCU errors (" + String.format("%.1f", percentDecoded) + "% successfully decoded)");
        }
        return image;
    }

    /**
     * Decode color JPEG (3 components)
     */
    private BufferedImage readColor(InputStream is) throws IOException {
        // Calculate MCU dimensions based on maximum sampling factors
        int mcuWidth = (width + maxHSampling * 8 - 1) / (maxHSampling * 8);
        int mcuHeight = (height + maxVSampling * 8 - 1) / (maxVSampling * 8);

        // Allocate image buffer
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[width * height];

        // Decode image data
        BitInputStream bitStream = new BitInputStream(is);
        Arrays.fill(previousDC, 0);
        Component[] components = sofReader.getComponents();
        int mcuCount = 0;
        int totalMCUs = mcuWidth * mcuHeight;
        int errorCount = 0;

        while (mcuCount < totalMCUs) {
            int mcuY = mcuCount / mcuWidth;
            int mcuX = mcuCount % mcuWidth;

            try {
                // Reset DC predictors at restart intervals
                if (restartInterval > 0 && mcuCount > 0 && mcuCount % restartInterval == 0) {
                    Arrays.fill(previousDC, 0);
                    bitStream.reset();
                }

                // Decode all components in this MCU
                float[][][] mcuBlocks = new float[3][][];
                for (int c = 0; c < 3; c++) {
                    Component comp = components[c];
                    int hSampling = comp.getHSampleFactor();
                    int vSampling = comp.getVSampleFactor();
                    // Decode blocks for this component
                    float[][][][] blocks = new float[vSampling][hSampling][][];
                    for (int v = 0; v < vSampling; v++) {
                        for (int h = 0; h < hSampling; h++) {
                            float[][] block = decodeBlock(bitStream, comp, c);
                            float[][] spatial = DCT.inverseDCT(block);
                            // Normalize 12-bit to 8-bit range if needed
                            if (precision == 12) {
                                for (int i = 0; i < 8; i++) {
                                    for (int j = 0; j < 8; j++) {
                                        spatial[i][j] /= 16.0f;
                                    }
                                }
                            }
                            blocks[v][h] = spatial;
                        }
                    }
                    // Assemble blocks into component MCU
                    mcuBlocks[c] = assembleBlocks(blocks, hSampling, vSampling);
                }

                // Upsample and convert to RGB
                convertMCUToRGB(mcuBlocks, components, pixels, mcuX, mcuY);

            } catch (Exception e) {
                // Error in this MCU try to resynchronize
                errorCount++;
                if (errorCount == 1) {
                    System.err.println("Warning: JPEG decoding errors encountered");
                }
                System.err.println("ERROR at MCU " + mcuCount + " (" + mcuX + "," + mcuY + "): " + e.getMessage());

                // Fill the current MCU with neutral grey (RGB: 128, 128, 128)
                fillMCUWithGreyRGB(pixels, mcuX, mcuY, maxHSampling * 8, maxVSampling * 8, width, height);

                // Try to find next restart marker to resynchronize
                if (restartInterval > 0) {
                    try {
                        System.err.println("Searching for restart marker after error at MCU " + mcuCount);
                        int foundMarker = bitStream.searchForRestartMarker();
                        if (foundMarker >= 0 && foundMarker <= 7) {
                            // Found a restart marker jump to next boundary
                            // NOTE: We ignore the marker number like HuffmanJPGReader does
                            // because corrupted streams may have marker number mismatches
                            int nextRestartMCU = ((mcuCount / restartInterval) + 1) * restartInterval;
                            System.err.println("Found restart marker! Filling MCUs " + (mcuCount + 1) + " to " + nextRestartMCU);
                            Arrays.fill(previousDC, 0);

                            // Fill gap MCUs
                            for (int fillMCU = mcuCount + 1; fillMCU < nextRestartMCU && fillMCU < totalMCUs; fillMCU++) {
                                int filly = fillMCU / mcuWidth;
                                int fillX = fillMCU % mcuWidth;
                                fillMCUWithGreyRGB(pixels, fillX, filly, maxHSampling * 8, maxVSampling * 8, width, height);
                            }
                            mcuCount = nextRestartMCU; // continue skips mcuCount++ at loop end
                            continue;
                        } else {
                            // No more restart markers fill all remaining MCUs
                            for (int fillMCU = mcuCount; fillMCU < totalMCUs; fillMCU++) {
                                int filly = fillMCU / mcuWidth;
                                int fillX = fillMCU % mcuWidth;
                                fillMCUWithGreyRGB(pixels, fillX, filly, maxHSampling * 8, maxVSampling * 8, width, height);
                            }
                            System.err.println("No more restart markers found after error, filled remaining MCUs with grey");
                            break;
                        }
                    } catch (IOException ex) {
                        // Fill all remaining MCUs on I/O error
                        for (int fillMCU = mcuCount; fillMCU < totalMCUs; fillMCU++) {
                            int filly = fillMCU / mcuWidth;
                            int fillX = fillMCU % mcuWidth;
                            fillMCUWithGreyRGB(pixels, fillX, filly, maxHSampling * 8, maxVSampling * 8, width, height);
                        }
                        break;
                    }
                } else {
                    // No restart markers fill remaining MCUs with grey
                    int filledMCUs = totalMCUs - mcuCount;
                    for (int fillMCU = mcuCount; fillMCU < totalMCUs; fillMCU++) {
                        int filly = fillMCU / mcuWidth;
                        int fillX = fillMCU % mcuWidth;
                        fillMCUWithGreyRGB(pixels, fillX, filly, maxHSampling * 8, maxVSampling * 8, width, height);
                    }
                    errorCount += filledMCUs;
                    System.err.println("No restart markers available, filled remaining MCUs with grey");
                    break;
                }
            }
            mcuCount++;
        }

        if (errorCount > 0) {
            double percentDecoded = ((totalMCUs - errorCount) * 100.0 / totalMCUs);
            System.err.println("Completed with " + errorCount + " MCU errors (" + String.format("%.1f", percentDecoded) + "% successfully decoded)");
        }

        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    /**
     * Decode 4-component JPEG (CMYK or YCCK)
     */
    private BufferedImage read4Component(InputStream is) throws IOException {
        // Calculate MCU dimensions based on maximum sampling factors
        int mcuWidth = (width + maxHSampling * 8 - 1) / (maxHSampling * 8);
        int mcuHeight = (height + maxVSampling * 8 - 1) / (maxVSampling * 8);

        // Allocate CMYK buffer we'll decode to CMYK first, then batch-convert to RGB
        byte[] cmykPixels = new byte[width * height * 4];

        // Decode image data
        BitInputStream bitStream = new BitInputStream(is);
        Arrays.fill(previousDC, 0);
        Component[] components = sofReader.getComponents();
        int mcuCount = 0;
        int totalMCUs = mcuWidth * mcuHeight;
        int errorCount = 0;

        while (mcuCount < totalMCUs) {
            int mcuY = mcuCount / mcuWidth;
            int mcuX = mcuCount % mcuWidth;

            try {
                // Reset DC predictors at restart intervals
                if (restartInterval > 0 && mcuCount > 0 && mcuCount % restartInterval == 0) {
                    Arrays.fill(previousDC, 0);
                    bitStream.reset();
                }

                // Decode all 4 components in this MCU
                float[][][] mcuBlocks = new float[4][][];
                for (int c = 0; c < 4; c++) {
                    Component comp = components[c];
                    int hSampling = comp.getHSampleFactor();
                    int vSampling = comp.getVSampleFactor();
                    // Decode blocks for this component
                    float[][][][] blocks = new float[vSampling][hSampling][][];
                    for (int v = 0; v < vSampling; v++) {
                        for (int h = 0; h < hSampling; h++) {
                            float[][] block = decodeBlock(bitStream, comp, c);
                            float[][] spatial = DCT.inverseDCT(block);

                            // Normalize 12-bit to 8-bit range if needed
                            if (precision == 12) {
                                for (int i = 0; i < 8; i++) {
                                    for (int j = 0; j < 8; j++) {
                                        spatial[i][j] /= 16.0f;
                                    }
                                }
                            }
                            blocks[v][h] = spatial;
                        }
                    }
                    // Assemble blocks into component MCU
                    mcuBlocks[c] = assembleBlocks(blocks, hSampling, vSampling);
                }
                // Convert YCCK/CMYK to CMYK (without ICC transformation)
                convertMCUToCMYK(mcuBlocks, components, cmykPixels, mcuX, mcuY);

            } catch (Exception e) {
                // Error in this MCU try to resynchronize
                errorCount++;
                if (errorCount == 1) {
                    System.err.println("Warning: JPEG decoding errors encountered");
                }

                // Fill the current MCU with neutral grey (CMYK: 0, 0, 0, 0 = white)
                fillMCUWithGreyCMYK(cmykPixels, mcuX, mcuY, maxHSampling * 8, maxVSampling * 8, width, height);

                // Try to find next restart marker to resynchronize
                if (restartInterval > 0) {
                    try {
                        int foundMarker = bitStream.searchForRestartMarker();
                        if (foundMarker >= 0 && foundMarker <= 7) {
                            // Found a restart marker jump to next boundary
                            // NOTE: We ignore the marker number like HuffmanJPGReader does
                            int nextRestartMCU = ((mcuCount / restartInterval) + 1) * restartInterval;
                            Arrays.fill(previousDC, 0);

                            for (int fillMCU = mcuCount + 1; fillMCU < nextRestartMCU && fillMCU < totalMCUs; fillMCU++) {
                                int filly = fillMCU / mcuWidth;
                                int fillX = fillMCU % mcuWidth;
                                fillMCUWithGreyCMYK(cmykPixels, fillX, filly, maxHSampling * 8, maxVSampling * 8, width, height);
                            }
                            mcuCount = nextRestartMCU; // continue skips mcuCount++ at loop end
                            continue;
                        } else {
                            // No more restart markers fill all remaining MCUs
                            for (int fillMCU = mcuCount; fillMCU < totalMCUs; fillMCU++) {
                                int filly = fillMCU / mcuWidth;
                                int fillX = fillMCU % mcuWidth;
                                fillMCUWithGreyCMYK(cmykPixels, fillX, filly, maxHSampling * 8, maxVSampling * 8, width, height);
                            }
                            System.err.println("No more restart markers found after error, filled remaining MCUs with grey");
                            break;
                        }
                    } catch (IOException ex) {
                        // Fill all remaining MCUs on I/O error
                        for (int fillMCU = mcuCount; fillMCU < totalMCUs; fillMCU++) {
                            int filly = fillMCU / mcuWidth;
                            int fillX = fillMCU % mcuWidth;
                            fillMCUWithGreyCMYK(cmykPixels, fillX, filly, maxHSampling * 8, maxVSampling * 8, width, height);
                        }
                        break;
                    }
                } else {
                    // No restart markers fill remaining MCUs with grey
                    int filledMCUs = totalMCUs - mcuCount;
                    for (int fillMCU = mcuCount; fillMCU < totalMCUs; fillMCU++) {
                        int filly = fillMCU / mcuWidth;
                        int fillX = fillMCU % mcuWidth;
                        fillMCUWithGreyCMYK(cmykPixels, fillX, filly, maxHSampling * 8, maxVSampling * 8, width, height);
                    }
                    errorCount += filledMCUs;
                    System.err.println("No restart markers available, filled remaining MCUs with grey");
                    break;
                }
            }
            mcuCount++;
        }

        if (errorCount > 0) {
            double percentDecoded = ((totalMCUs - errorCount) * 100.0 / totalMCUs);
            System.err.println("Completed with " + errorCount + " MCU errors (" + String.format("%.1f", percentDecoded) + "% successfully decoded)");
        }

        // Apply ICC profile if present, else convert CMYK to RGB using standard formula
        if (iccColorSpace != null) {
            return cmykToRGBWithICC(cmykPixels);
        } else {
            return cmykToRGBStandard(cmykPixels);
        }
    }

    /**
     * Parse JPEG segments to extract quantization tables, Huffman tables, and image dimensions
     */
    private boolean parseJPEG(InputStream is) throws IOException {
        // Read SOI marker
        int marker = IOUtils.readShortMM(is) & 0xFFFF; // Mask to unsigned
        if (marker != (Marker.SOI.getValue() & 0xFFFF)) {
            return false; // Not a JPEG
        }

        marker = IOUtils.readShortMM(is) & 0xFFFF; // Mask to unsigned
        while (marker != (Marker.EOI.getValue() & 0xFFFF)) {
            // Skip padding bytes
            while ((marker & 0xFF00) != 0xFF00) {
                marker = ((marker << 8) | IOUtils.read(is));
            }

            // Handle known markers
            if (marker == (Marker.DQT.getValue() & 0xFFFF)) {
                parseDQT(is);
            } else if (marker == (Marker.DHT.getValue() & 0xFFFF)) {
                parseDHT(is);
            } else if (marker == (Marker.SOF0.getValue() & 0xFFFF) || marker == (Marker.SOF1.getValue() & 0xFFFF)) {
                parseSOF(is, marker);
            } else if (marker == 0xFFDD) { // DRI
                parseDRI(is);
            } else if (marker == 0xFFE1) { // APP1 (Exif)
                parseAPP1(is);
            } else if (marker == 0xFFE2) { // APP2 (ICC Profile)
                parseAPP2(is);
            } else if (marker == 0xFFEE) { // APP14 (Adobe marker)
                parseAPP14(is);
            } else if (marker == (Marker.SOS.getValue() & 0xFFFF)) {
                parseSOS(is);
                return true; // Image data follows
            } else {
                // Skip unknown/unsupported segments (APP0, APP1, etc.) and padding 0xFF00 bytes
                if (marker != 0xFF00) {
                    int length = IOUtils.readUnsignedShortMM(is);
                    IOUtils.skipFully(is, length - 2);
                }
            }
            marker = IOUtils.readShortMM(is) & 0xFFFF; // Mask to unsigned
        }
        return false;
    }

    /**
     * Parse Define Quantization Table segment
     * NOTE: NOT using DQTReader because it incorrectly de-zigzags the table.
     * The table in the JPEG file is already in natural (row-major) order.
     */
    private void parseDQT(InputStream is) throws IOException {
        int length = IOUtils.readUnsignedShortMM(is);
        byte[] data = new byte[length - 2];
        IOUtils.readFully(is, data);
        // Use DQTReader which de-zigzags to natural order
        Segment segment = new Segment(Marker.DQT, length, data);
        DQTReader reader = new DQTReader(segment);
        List<QTable> tables = reader.getTables();
        // Store all quantization tables by their ID
        for (QTable qtable : tables) {
            int tableId = qtable.getID();
            qTables[tableId] = qtable.getData();
        }
    }

    /**
     * Parse Define Huffman Table segment using existing DHTReader
     */
    private void parseDHT(InputStream is) throws IOException {
        int length = IOUtils.readUnsignedShortMM(is);
        byte[] data = new byte[length - 2];
        IOUtils.readFully(is, data);
        Segment segment = new Segment(Marker.DHT, length, data);
        DHTReader reader = new DHTReader(segment);

        // Store DC tables by their ID
        List<HTable> dcTableList = reader.getDCTables();
        for (HTable htable : dcTableList) {
            int tableId = htable.getID();
            dcTables[tableId] = new HuffmanTbl(htable.getBits(), htable.getValues());
            dcTables[tableId].generateDecoderTables();
        }

        // Store AC tables by their ID
        List<HTable> acTableList = reader.getACTables();
        for (HTable htable : acTableList) {
            int tableId = htable.getID();
            acTables[tableId] = new HuffmanTbl(htable.getBits(), htable.getValues());
            acTables[tableId].generateDecoderTables();
        }
    }

    /**
     * Parse Start Of Frame segment using existing SOFReader
     */
    private void parseSOF(InputStream is, int marker) throws IOException {
        int length = IOUtils.readUnsignedShortMM(is);
        byte[] data = new byte[length - 2];
        IOUtils.readFully(is, data);
        Marker markerType = Marker.fromShort((short) marker);
        Segment segment = new Segment(markerType, length, data);
        sofReader = new SOFReader(segment);

        width = sofReader.getFrameWidth();
        height = sofReader.getFrameHeight();
        numComponents = sofReader.getNumOfComponents();
        precision = sofReader.getPrecision();

        if (numComponents != 1 && numComponents != 3 && numComponents != 4) {
            throw new IOException("Only 1, 3, or 4 component JPEG is supported, found " + numComponents);
        }

        if (precision != 8 && precision != 12) {
            throw new IOException("Only 8-bit and 12-bit precision supported, found " + precision + "-bit");
        }

        // Calculate maximum sampling factors
        maxHSampling = 1;
        maxVSampling = 1;
        Component[] components = sofReader.getComponents();
        if (components != null) {
            for (Component comp : components) {
                if (comp.getHSampleFactor() > maxHSampling) {
                    maxHSampling = comp.getHSampleFactor();
                }
                if (comp.getVSampleFactor() > maxVSampling) {
                    maxVSampling = comp.getVSampleFactor();
                }
            }
        }
    }

    /**
     * Parse Start Of Scan segment using existing SOSReader
     */
    private void parseSOS(InputStream is) throws IOException {
        int length = IOUtils.readUnsignedShortMM(is);
        byte[] data = new byte[length - 2];
        IOUtils.readFully(is, data);
        Segment segment = new Segment(Marker.SOS, length, data);
        // SOSReader constructor parses SOS and assigns DC/AC table numbers to sofReader's components
        new SOSReader(segment, sofReader);
        // Image data follows after SOS segment
        // Table assignments are retrieved per-component during decode
    }

    /**
     * Parse Define Restart Interval segment
     */
    private void parseDRI(InputStream is) throws IOException {
        IOUtils.readUnsignedShortMM(is); // length (always 4)
        restartInterval = IOUtils.readUnsignedShortMM(is);
    }

    /**
     * Parse APP1 marker to extract exif data
     * For image decoding, we are interested in exif orientation only
     */
    private void parseAPP1(InputStream is) throws IOException {
        // Check if this is an Exif (starts with "Exif\0\0")
        int length = IOUtils.readUnsignedShortMM(is);
        byte[] data = new byte[length - 2];
        IOUtils.readFully(is, data);
        if (data.length > 6 && data[0] == 'E' && data[1] == 'x' && data[2] == 'i' && data[3] == 'f' && data[4] == 0 && data[5] == 0) {
            Exif exif = new JpegExif(ArrayUtils.subArray(data, 6, data.length - 6));
            IFD imageIFD = exif.getImageIFD();
            if (imageIFD != null) {
                TiffField<?> f_orientation = imageIFD.getField(TiffTag.ORIENTATION);
                orientation = (f_orientation != null) ? (int) f_orientation.getDataAsLong()[0] : 1;
            }
        }
    }

    /**
     * Parse APP2 marker to extract ICC Profile
     */
    private void parseAPP2(InputStream is) throws IOException {
        int length = IOUtils.readUnsignedShortMM(is);
        byte[] data = new byte[length - 2];
        IOUtils.readFully(is, data);
        // Check if this is an ICC profile (starts with "ICC_PROFILE\0")
        if (data.length > 14 && data[0] == 'I' && data[1] == 'C' && data[2] == 'C' && data[3] == '_' && data[4] == 'P' && data[5] == 'R' && data[6] == 'O' && data[7] == 'F' && data[8] == 'I' && data[9] == 'L' && data[10] == 'E' && data[11] == 0) {
            // ICC profiles can be split across multiple APP2 segments
            // Format: "ICC_PROFILE\0" + chunk_number(1 byte) + total_chunks (1 byte) + profile_data
            int chunkNum = data[12] & 0xFF;
            int totalChunks = data[13] & 0xFF;

            // Initialize chunk array on first chunk
            if (iccChunks == null) {
                iccChunks = new byte[totalChunks][];
            }

            // Store this chunk (chunk numbers are 1-based)
            if (chunkNum >= 1 && chunkNum <= totalChunks) {
                byte[] chunkData = new byte[data.length - 14];
                System.arraycopy(data, 14, chunkData, 0, chunkData.length);
                iccChunks[chunkNum - 1] = chunkData;

                // Check if all chunks received
                boolean allChunksReceived = true;
                int totalSize = 0;
                for (int i = 0; i < totalChunks; i++) {
                    if (iccChunks[i] == null) {
                        allChunksReceived = false;
                        break;
                    }
                    totalSize += iccChunks[i].length;
                }

                // Reassemble complete ICC profile
                if (allChunksReceived) {
                    iccProfileData = new byte[totalSize];
                    int offset = 0;
                    for (int i = 0; i < totalChunks; i++) {
                        System.arraycopy(iccChunks[i], 0, iccProfileData, offset, iccChunks[i].length);
                        offset += iccChunks[i].length;
                    }
                }
            }
        }
    }

    /**
     * Parse APP14 (Adobe) marker to determine color transform
     */
    private void parseAPP14(InputStream is) throws IOException {
        int length = IOUtils.readUnsignedShortMM(is);
        byte[] data = new byte[length - 2];
        IOUtils.readFully(is, data);
        // Check for Adobe marker
        if (length >= 12 && data[0] == 'A' && data[1] == 'd' && data[2] == 'o' && data[3] == 'b' && data[4] == 'e') {
            // Transform is at byte 11: 0 CMYK, 1 YCbCr, 2 YCCK
            int transform = data[11] & 0xFF;
            isYCCK = (transform == 2);
        }
    }

    /**
     * Decode one 8x8 block using Huffman decoding
     */
    private float[][] decodeBlock(BitInputStream bitStream, Component comp, int componentIndex) throws IOException {
        int[] coeffs = new int[64];
        Arrays.fill(coeffs, 0);

        // Get tables for this component
        HuffmanTbl dcTable = dcTables[comp.getDCTableNumber()];
        HuffmanTbl acTable = acTables[comp.getACTableNumber()];
        int[] qTable = qTables[comp.getQTableNumber()];

        // Decode DC coefficient
        int dcLength = decodeSymbol(bitStream, dcTable);
        if (dcLength > 0) {
            int dcDiff = bitStream.readBits(dcLength);
            dcDiff = extendSign(dcDiff, dcLength);
            previousDC[componentIndex] += dcDiff;
        }
        coeffs[0] = previousDC[componentIndex];

        // Decode AC coefficients
        int k = 1;
        while (k < 64) {
            int symbol = decodeSymbol(bitStream, acTable);
            if (symbol == 0) { // EOB
                break;
            }
            if (symbol == 0xF0) { // ZRL (16 zeros)
                k += 16;
                continue;
            }
            int runLength = (symbol >> 4) & 0x0F;
            int acLength = symbol & 0x0F;
            k += runLength;
            if (k >= 64) break;
            if (acLength > 0) {
                int acValue = bitStream.readBits(acLength);
                coeffs[k] = extendSign(acValue, acLength);
            }
            k++;
        }

        // De-zigzag coefficients and dequantize
        // coeffs[i] is at zigzag position i
        // ZIGZAG[i] gives the natural position for zigzag position i
        // qTable is in natural order from DQTReader
        float[][] block = new float[8][8];
        for (int i = 0; i < 64; i++) {
            int naturalPos = ZIGZAG[i];
            int row = naturalPos / 8;
            int col = naturalPos % 8;
            // Dequantize: coeff at zigzag pos i * qTable at natural pos ZIGZAG[i]
            block[row][col] = coeffs[i] * (float) qTable[ZIGZAG[i]];
        }
        return block;
    }

    /**
     * Decode one Huffman symbol
     */
    private int decodeSymbol(BitInputStream bitStream, HuffmanTbl table) throws IOException {
        int code = 0;
        int[] minCode = table.getMinCodeTable();
        int[] maxCode = table.getMaxCodeTable();
        int[] valPtr = table.getValPTRTable();
        byte[] huffVal = table.getValueTable();

        for (int i = 0; i < 16; i++) {
            code = (code << 1) | bitStream.readBit();
            if (code >= minCode[i] && code <= maxCode[i]) {
                int j = valPtr[i] + code - minCode[i];
                return huffVal[j] & 0xFF;
            }
        }
        throw new IOException("Invalid Huffman code");
    }

    /**
     * Extend sign for variable length integer
     */
    private int extendSign(int value, int length) {
        int vt = 1 << (length - 1);
        if (value < vt) {
            vt = (-1 << length) + 1;
            value += vt;
        }
        return value;
    }

    /**
     * Copy decoded block to image buffer with level shift
     */
    private void copyBlockToImage(float[][] block, byte[] pixels, int x, int y, int width, int height) {
        for (int row = 0; row < 8; row++) {
            int imageY = y + row;
            if (imageY >= height) break;
            for (int col = 0; col < 8; col++) {
                int imageX = x + col;
                if (imageX >= width) break;
                // Level shift: add 128 and clamp to 0-255
                int value = Math.round(block[row][col] + 128.0f);
                value = clamp(value);
                pixels[imageY * width + imageX] = (byte) value;
            }
        }
    }

    /**
     * Fill a grayscale block with grey value (for error recovery)
     */
    private void fillBlockWithGrey(byte[] pixels, int x, int y, int w, int h, int width, int height, byte greyValue) {
        for (int row = 0; row < h; row++) {
            int imageY = y + row;
            if (imageY >= height) break;
            for (int col = 0; col < w; col++) {
                int imageX = x + col;
                if (imageX >= width) break;
                pixels[imageY * width + imageX] = greyValue;
            }
        }
    }

    /**
     * Fill an RGB MCU with neutral grey (for error recovery)
     */
    private void fillMCUWithGreyRGB(int[] pixels, int mcuX, int mcuY, int mcuWidth, int mcuHeight, int width, int height) {
        int greyRGB = (128 << 16) | (128 << 8) | 128; // RGB: 128, 128, 128
        for (int row = 0; row < mcuHeight; row++) {
            int imageY = mcuY * mcuHeight + row;
            if (imageY >= height) break;
            for (int col = 0; col < mcuWidth; col++) {
                int imageX = mcuX * mcuWidth + col;
                if (imageX >= width) break;
                pixels[imageY * width + imageX] = greyRGB;
            }
        }
    }

    /**
     * Fill a CMYK MCU with neutral grey (for error recovery)
     */
    private void fillMCUWithGreyCMYK(byte[] cmykPixels, int mcuX, int mcuY, int mcuWidth, int mcuHeight, int width, int height) {
        // Neutral grey in CMYK: C=0, M=0, Y=0, K=128 (roughly mid-grey)
        byte c = 0, m = 0, y = 0, k = (byte) 128;
        for (int row = 0; row < mcuHeight; row++) {
            int imageY = mcuY * mcuHeight + row;
            if (imageY >= height) break;
            for (int col = 0; col < mcuWidth; col++) {
                int imageX = mcuX * mcuWidth + col;
                if (imageX >= width) break;
                int pixelIndex = (imageY * width + imageX) * 4;
                cmykPixels[pixelIndex] = c;
                cmykPixels[pixelIndex + 1] = m;
                cmykPixels[pixelIndex + 2] = y;
                cmykPixels[pixelIndex + 3] = k;
            }
        }
    }

    /**
     * Assemble decoded blocks into a single component MCU
     */
    private float[][] assembleBlocks(float[][][][] blocks, int hSampling, int vSampling) {
        int mcuWidth = hSampling * 8;
        int mcuHeight = vSampling * 8;
        float[][] mcu = new float[mcuHeight][mcuWidth];
        for (int v = 0; v < vSampling; v++) {
            for (int h = 0; h < hSampling; h++) {
                float[][] block = blocks[v][h];
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < 8; col++) {
                        mcu[v * 8 + row][h * 8 + col] = block[row][col];
                    }
                }
            }
        }
        return mcu;
    }

    /**
     * Convert MCU blocks from YCbCr to RGB and write to image buffer
     * Or directly copy RGB components if skipYCbCrConversion is true (for TIFF-embedded RGB JPEG)
     */
    private void convertMCUToRGB(float[][][] mcuBlocks, Component[] components, int[] pixels, int mcuX, int mcuY) {
        int cbHSampling = components[1].getHSampleFactor();
        int cbVSampling = components[1].getVSampleFactor();
        int crHSampling = components[2].getHSampleFactor();
        int crVSampling = components[2].getVSampleFactor();

        float[][] yBlock = mcuBlocks[0]; // Y component (or R if skipYCbCrConversion)
        float[][] cbBlock = mcuBlocks[1]; // Cb component (or G if skipYCbCrConversion)
        float[][] crBlock = mcuBlocks[2]; // Cr component (or B if skipYCbCrConversion)

        int mcuPixelWidth = maxHSampling * 8;
        int mcuPixelHeight = maxVSampling * 8;

        for (int row = 0; row < mcuPixelHeight; row++) {
            int imageY = mcuY * mcuPixelHeight + row;
            if (imageY >= height) break;
            for (int col = 0; col < mcuPixelWidth; col++) {
                int imageX = mcuX * mcuPixelWidth + col;
                if (imageX >= width) break;
                int r, g, b;
                if (skipYCbCrConversion) {
                    // Data is already RGB (TIFF-embedded with PhotometricInterpretation=RGB)
                    float rVal = yBlock[row][col];
                    int gRow = (row * cbVSampling) / maxVSampling;
                    int gCol = (col * cbHSampling) / maxHSampling;
                    int bRow = (row * crVSampling) / maxVSampling;
                    int bCol = (col * crHSampling) / maxHSampling;
                    float gVal = cbBlock[gRow][gCol];
                    float bVal = crBlock[bRow][bCol];
                    // Level shift: add 128
                    r = clamp(Math.round(rVal + 128.0f));
                    g = clamp(Math.round(gVal + 128.0f));
                    b = clamp(Math.round(bVal + 128.0f));
                } else {
                    // Data is YCbCr, convert to RGB
                    float y = yBlock[row][col];
                    int cbRow = (row * cbVSampling) / maxVSampling;
                    int cbCol = (col * cbHSampling) / maxHSampling;
                    int crRow = (row * crVSampling) / maxVSampling;
                    int crCol = (col * crHSampling) / maxHSampling;
                    float cb = cbBlock[cbRow][cbCol];
                    float cr = crBlock[crRow][crCol];
                    // Convert YCbCr to RGB
                    r = clamp(Math.round(y + 128.0f + 1.402f * cr));
                    g = clamp(Math.round(y + 128.0f - 0.344136f * cb - 0.714136f * cr));
                    b = clamp(Math.round(y + 128.0f + 1.772f * cb));
                }

                // Apply ICC profile transformation if available (e.g., Adobe RGB -> sRGB)
                if (iccColorSpace != null && iccColorSpace.getNumComponents() == 3) {
                    float[] sourceRGB = new float[]{r / 255.0f, g / 255.0f, b / 255.0f};
                    float[] xyz = iccColorSpace.toCIEXYZ(sourceRGB);
                    float[] sRGBValues = sRGB.fromCIEXYZ(xyz);
                    r = clamp(Math.round(sRGBValues[0] * 255.0f));
                    g = clamp(Math.round(sRGBValues[1] * 255.0f));
                    b = clamp(Math.round(sRGBValues[2] * 255.0f));
                }
                pixels[imageY * width + imageX] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }
    }

    /**
     * Convert 4-component MCU from YCCK/CMYK to CMYK and write to buffer
     */
    private void convertMCUToCMYK(float[][][] mcuBlocks, Component[] components, byte[] cmykPixels, int mcuX, int mcuY) {
        int[] hSampling = new int[4];
        int[] vSampling = new int[4];
        for (int i = 0; i < 4; i++) {
            hSampling[i] = components[i].getHSampleFactor();
            vSampling[i] = components[i].getVSampleFactor();
        }

        float[][] comp0 = mcuBlocks[0];
        float[][] comp1 = mcuBlocks[1];
        float[][] comp2 = mcuBlocks[2];
        float[][] comp3 = mcuBlocks[3];

        int mcuPixelWidth = maxHSampling * 8;
        int mcuPixelHeight = maxVSampling * 8;

        for (int row = 0; row < mcuPixelHeight; row++) {
            int imageY = mcuY * mcuPixelHeight + row;
            if (imageY >= height) break;
            for (int col = 0; col < mcuPixelWidth; col++) {
                int imageX = mcuX * mcuPixelWidth + col;
                if (imageX >= width) break;

                float c0 = comp0[row][col];
                int c1Row = (row * vSampling[1]) / maxVSampling;
                int c1Col = (col * hSampling[1]) / maxHSampling;
                float c1 = comp1[c1Row][c1Col];
                int c2Row = (row * vSampling[2]) / maxVSampling;
                int c2Col = (col * hSampling[2]) / maxHSampling;
                float c2 = comp2[c2Row][c2Col];
                int c3Row = (row * vSampling[3]) / maxVSampling;
                int c3Col = (col * hSampling[3]) / maxHSampling;
                float c3 = comp3[c3Row][c3Col];

                float c, m, y, k;
                if (isYCCK) {
                    // YCCK reverse transform
                    float Y_stored = c0;
                    float Cb_stored = c1;
                    float Cr_stored = c2;
                    k = 128.0f - c3;
                    float luma = 128.0f - Y_stored;
                    float Cb_std = -Cb_stored;
                    float Cr_std = -Cr_stored;
                    float cInv = luma + 1.402f * Cr_std;
                    float mInv = luma - 0.344136f * Cb_std - 0.714136f * Cr_std;
                    float yInv = luma + 1.772f * Cb_std;
                    c = 255.0f - cInv;
                    m = 255.0f - mInv;
                    y = 255.0f - yInv;
                } else {
                    // Direct CMYK
                    c = 128.0f - c0;
                    m = 128.0f - c1;
                    y = 128.0f - c2;
                    k = 128.0f - c3;
                }

                int pixelIndex = (imageY * width + imageX) * 4;
                cmykPixels[pixelIndex] = (byte) clamp(Math.round(c));
                cmykPixels[pixelIndex + 1] = (byte) clamp(Math.round(m));
                cmykPixels[pixelIndex + 2] = (byte) clamp(Math.round(y));
                cmykPixels[pixelIndex + 3] = (byte) clamp(Math.round(k));
            }
        }
    }

    /**
     * Convert CMYK buffer to RGB using ICC profile with ColorConvertOp (fast batch conversion)
     */
    private BufferedImage cmykToRGBWithICC(byte[] cmykPixels) {
        WritableRaster cmykRaster = Raster.createInterleavedRaster(
            new DataBufferByte(cmykPixels, cmykPixels.length),
            width, height, width * 4, 4, new int[]{0, 1, 2, 3}, null
        );
        ColorModel cmykColorModel = new ComponentColorModel(iccColorSpace, false, false, java.awt.Transparency.OPAQUE, java.awt.image.DataBuffer.TYPE_BYTE);
        BufferedImage cmykImage = new BufferedImage(cmykColorModel, cmykRaster, false, null);

        RenderingHints hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);

        ColorConvertOp cco = new ColorConvertOp(iccColorSpace, sRGB, hints);
        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        cco.filter(cmykImage, rgbImage);
        return rgbImage;
    }

    /**
     * Convert CMYK buffer to RGB using standard formula (no ICC profile)
     */
    private BufferedImage cmykToRGBStandard(byte[] cmykPixels) {
        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[width * height];
        for (int i = 0; i < width * height; i++) {
            int pixelIndex = i * 4;
            int c = cmykPixels[pixelIndex] & 0xFF;
            int m = cmykPixels[pixelIndex + 1] & 0xFF;
            int y = cmykPixels[pixelIndex + 2] & 0xFF;
            int k = cmykPixels[pixelIndex + 3] & 0xFF;

            int r = ((255 - c) * (255 - k)) / 255;
            int g = ((255 - m) * (255 - k)) / 255;
            int b = ((255 - y) * (255 - k)) / 255;

            pixels[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;
        }
        rgbImage.setRGB(0, 0, width, height, pixels, 0, width);
        return rgbImage;
    }

    /**
     * Apply EXIF orientation to the image
     */
    private BufferedImage applyEXIFOrientation(BufferedImage image, int orientation) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage transformedImage;

        switch (orientation) {
            case 2: // Flip X
                transformedImage = new BufferedImage(width, height, image.getType());
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        transformedImage.setRGB(width - 1 - x, y, image.getRGB(x, y));
                break;
            case 3: // Rotate 180
                transformedImage = new BufferedImage(width, height, image.getType());
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        transformedImage.setRGB(width - 1 - x, height - 1 - y, image.getRGB(x, y));
                break;
            case 4: // Flip Y
                transformedImage = new BufferedImage(width, height, image.getType());
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        transformedImage.setRGB(x, height - 1 - y, image.getRGB(x, y));
                break;
            case 5: // Transpose
                transformedImage = new BufferedImage(height, width, image.getType());
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        transformedImage.setRGB(y, x, image.getRGB(x, y));
                break;
            case 6: // Rotate 90
                transformedImage = new BufferedImage(height, width, image.getType());
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        transformedImage.setRGB(height - 1 - y, x, image.getRGB(x, y));
                break;
            case 7: // Transverse
                transformedImage = new BufferedImage(height, width, image.getType());
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        transformedImage.setRGB(height - 1 - y, width - 1 - x, image.getRGB(x, y));
                break;
            case 8: // Rotate 270
                transformedImage = new BufferedImage(height, width, image.getType());
                for (int y = 0; y < height; y++)
                    for (int x = 0; x < width; x++)
                        transformedImage.setRGB(y, width - 1 - x, image.getRGB(x, y));
                break;
            default:
                return image;
        }
        return transformedImage;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Helper class for reading bits from input stream
     */
    private static class BitInputStream {
        private InputStream is;
        private int currentByte;
        private int bitsLeft;

        public BitInputStream(InputStream is) {
            this.is = is;
            this.bitsLeft = 0;
        }

        public int readBit() throws IOException {
            if (bitsLeft == 0) {
                currentByte = readNextByte();
                bitsLeft = 8;
            }
            int bit = (currentByte >> 7) & 1;
            currentByte <<= 1;
            bitsLeft--;
            return bit;
        }

        private int readNextByte() throws IOException {
            int b = is.read();
            if (b == -1) {
                throw new IOException("Unexpected end of stream");
            }
            // Handle byte stuffing (0xFF 0x00) and restart markers
            if (b == 0xFF) {
                b = handleFFByte();
            }
            return b;
        }

        private int handleFFByte() throws IOException {
            int nextByte = is.read();
            if (nextByte == 0x00) {
                return 0xFF;
            } else if (nextByte >= 0xD0 && nextByte <= 0xD7) {
                // Restart marker (RST0-RST7)
                bitsLeft = 0;
                return readNextByte();
            } else if (nextByte == -1) {
                throw new IOException("Unexpected end of stream after 0xFF");
            } else {
                throw new IOException("Unexpected marker 0xFF" + String.format("%02X", nextByte) + " in bitstream");
            }
        }

        public int readBits(int count) throws IOException {
            int result = 0;
            for (int i = 0; i < count; i++) {
                result = (result << 1) | readBit();
            }
            return result;
        }

        public void reset() {
            bitsLeft = 0;
        }

        /**
         * Search for the next restart marker (RST0-RST7) or EOI
         */
        public int searchForRestartMarker() throws IOException {
            bitsLeft = 0;
            try {
                while (true) {
                    int b = is.read();
                    if (b == -1) return -1;
                    if (b == 0xFF) {
                        int marker = is.read();
                        if (marker == -1) return -1;
                        if (marker >= 0xD0 && marker <= 0xD7) {
                            return marker - 0xD0;
                        } else if (marker == 0xD9) {
                            return 9; // EOI
                        }
                    }
                }
            } catch (IOException e) {
                return -1;
            }
        }
    }
}