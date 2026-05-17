package com.icafe4j.image.reader;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import com.icafe4j.image.util.DCT;
import com.icafe4j.util.ArrayUtils;
import com.icafe4j.image.jpeg.Marker;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.jpeg.JpegExif;
import com.icafe4j.image.tiff.IFD;
import com.icafe4j.image.tiff.TiffField;
import com.icafe4j.image.tiff.TiffTag;
import com.icafe4j.image.jpeg.Component;
import com.icafe4j.image.jpeg.JPGConsts;
import com.icafe4j.image.compression.huffman.HuffmanTbl;

/**
 * Huffman JPEG Decoder
 *
 * This decoder handles both baseline (sequential) and progressive JPEG images.
 * It is particularly designed to properly decode progressive JPEGs which use
 * Huffman encoding with spectral selection and successive approximation.
 *
 * Features:
 * - Baseline DCT (SOF0), Extended Sequential DCT (SOF1), and Progressive DCT (SOF2) support
 * - 8-bit and 12-bit sample precision (12-bit images are scaled to 8-bit for display)
 * - Grayscale (1 component), YCbCr (3 components), and CMYK/YCCK (4 components)
 * - ICC Profile support for accurate CMYK color conversion
 * - Chroma subsampling (4:4:4, 4:2:2, 4:2:0, etc.)
 * - Restart intervals (DRI) for error resilience
 * - Proper handling of DC/AC spectral selection and successive approximation
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/05/2026
 */
public class HuffmanJPGReader extends ImageReader {
    // Decoder state
    private byte[] rawFile;
    private int fileHeader;
    private boolean scanFinished;
    private ScanMode scanMode; // "baseline_dct" or "progressive_dct"
    private int samplePrecision = 8; // Sample precision in bits (8 or 12)
    // width and height are inherited from ImageReader
    private Map<Integer, Component> colorComponents;
    private Map<Integer, Integer> componentIdToOrder; // Maps componentId to array index (0, 1, 2)
    private int[] sampleShape; // [width, height]

    // Huffman tables for proper JPEG decoding
    private Map<Integer, HuffmanDecoderCache> huffmanDecoderTables; // tableId -> cached decoder tables (ACTIVE for decoding)

    // Quantization tables
    private Map<Integer, int[][]> quantizationTables; // tableId -> 8x8 matrix (ACTIVE for decoding)
    private int restartInterval;
    private short[] imageArray; // 1D array: [y * arrayWidth * arrayDepth + x * arrayDepth + component]
    private int scanCount;
    private int scanAmount;
    private int mcuCountH;
    private int mcuCountV;
    private int mcuCount;
    private int mcuWidth;
    private int mcuHeight;
    private int arrayWidth;
    private int arrayHeight;
    private int arrayDepth;

    // DC predictors for progressive decoding - must persist across all scans
    private short[] previousDC = new short[4]; // Support up to 4 components (CMYK)

    // This will be used to handle EXIF orientation
    private int orientation = 1; // Default orientation

    // Zigzag scan pattern for undoing JPEG's zigzag ordering
    // Using pre-calculated arrays from JPGConsts for consistency
    private static final int[][] ZIGZAG_INVERSE = JPGConsts.getZigzagInverse2D();

    // CMYK/YCCK support
    private boolean isYCCK = false; // true if YCCK, false if CMYK (for 4-component images)
    private byte[] iccProfileData = null; // ICC profile for CMYK color correction
    private ICC_ColorSpace iccColorSpace = null; // ICC color space for accurate CMYK-RGB conversion
    private byte[][] iccChunks = null; // For multi-chunk ICC profiles
    private ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB); // Standard sRGB color space

    /**
     * HuffmanDecoderCache caches frequently-accessed arrays from HuffmanTbl
     * to avoid repeated method calls during decoding (called millions of times)
     */
    private static class HuffmanDecoderCache {
        final int[] minCode;
        final int[] maxCode;
        final int[] valPtr;
        final byte[] huffVal;

        HuffmanDecoderCache(HuffmanTbl huffmanTbl) {
            this.minCode = huffmanTbl.getMinCodeTable();
            this.maxCode = huffmanTbl.getMaxCodeTable();
            this.valPtr = huffmanTbl.getValPTRTable();
            this.huffVal = huffmanTbl.getValueTable();
        }
    }

    private static enum ScanMode {
        BASELINE_DCT, // SOF0, 0xffc0
        EXTENDED_DCT, // SOF1, 0xffc1
        PROGRESSIVE_DCT, // SOF2, 0xffc2
        LOSSLESS_HUFFMAN, // SOF3, 0xffc3
        SEQUENTIAL_DIFFERENTIAL, // SOF5, 0xffc5
        PROGRESSIVE_DIFFERENTIAL, // SOF6, 0xffc6
        LOSSLESS_DIFFERENTIAL, // SOF7, 0xffc7
        SEQUENTIAL_ARITHMETIC, // SOF9, 0xffc9
        PROGRESSIVE_ARITHMETIC, // SOF10, 0xffca
        LOSSLESS_ARITHMETIC, // SOF11, 0xffcb
        SEQUENTIAL_DIFFERENTIAL_ARITHMETIC, // SOF13, 0xffcd
        PROGRESSIVE_DIFFERENTIAL_ARITHMETIC, // SOF14, 0xffce
        LOSSLESS_DIFFERENTIAL_ARITHMETIC, // SOF15, 0xffcf
        UNKNOWN
    }

    /**
     * Read and decode a JPEG file from an InputStream
     */
    public BufferedImage read(InputStream is) throws Exception {
        // Read entire file into memory (matches Python implementation)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        rawFile = baos.toByteArray();

        // Check if file is JPEG (starts with 0xFFD8)
        if (rawFile.length < 2 || (rawFile[0] & 0xFF) != 0xFF || (rawFile[1] & 0xFF) != 0xD8) {
            throw new IOException("File is not a JPEG image");
        }

        // Initialize decoder state
        fileHeader = 2; // Start after SOI marker (0xFFD8)
        scanFinished = false;
        scanMode = ScanMode.UNKNOWN;
        width = 0;
        height = 0;
        colorComponents = new HashMap<>();
        componentIdToOrder = new HashMap<>();
        huffmanDecoderTables = new HashMap<>();
        quantizationTables = new HashMap<>();
        restartInterval = 0;
        imageArray = null;
        scanCount = 0;
        Arrays.fill(previousDC, (short) 0); // Reset DC predictors for new image

        // Main parsing loop
        while (!scanFinished && fileHeader < rawFile.length - 1) {
            // Expect marker (0xFF + marker code)
            if ((rawFile[fileHeader] & 0xFF) != 0xFF) {
                // Not a marker skip to next byte
                fileHeader++;
                continue;
            }

            int marker = ((rawFile[fileHeader] & 0xFF) << 8) | (rawFile[fileHeader + 1] & 0xFF);
            fileHeader += 2;

            // EOI marker has no length field
            int eoi = Marker.EOI.getValue() & 0xFFFF;
            if (marker == eoi) {
                endOfImage();
                break;
            }

            // Check if we can read the segment length
            if (fileHeader + 1 >= rawFile.length) {
                break;
            }

            // Get segment length
            int segmentLength = ((rawFile[fileHeader] & 0xFF) << 8) | (rawFile[fileHeader + 1] & 0xFF);
            segmentLength -= 2; // Length includes the 2 length bytes
            fileHeader += 2;

            // Check if we have enough bytes for the segment data
            if (fileHeader + segmentLength > rawFile.length) {
                break;
            }

            // Process segment based on marker
            byte[] segmentData = new byte[segmentLength];
            System.arraycopy(rawFile, fileHeader, segmentData, 0, segmentLength);

            // JPEG Markers
            switch (Marker.fromShort((short) marker)) {
                case SOF0:
                case SOF1: // Extended sequential DCT (12-bit)
                case SOF2:
                case SOF3:
                case SOF9:
                case SOF10:
                    if (colorComponents.isEmpty()) {
                        startOfFrame(segmentData, marker);
                    } else {
                        // Skip duplicate SOF marker
                        fileHeader += segmentLength;
                    }
                    break;
                case DHT:
                    defineHuffmanTable(segmentData);
                    break;
                case DQT:
                    defineQuantizationTable(segmentData);
                    break;
                case DRI:
                    defineRestartInterval(segmentData);
                    break;
                case APP1: // APP1 (EXIF)
                    parseAPP1(segmentData);
                    break;
                case APP2: // APP2 (ICC Profile)
                    parseAPP2(segmentData);
                    break;
                case APP14: // APP14 (Adobe marker)
                    parseAPP14(segmentData);
                    break;
                case SOS:
                    startOfScan(segmentData);
                    break;
                default:
                    // Skip unknown segment
                    fileHeader += segmentLength;
            }
        }

        // Create ICC color space if profile was found during parsing
        if (iccProfileData != null) {
            try {
                ICC_Profile profile = ICC_Profile.getInstance(iccProfileData);
                this.iccColorSpace = new ICC_ColorSpace(profile);
            } catch (Exception e) {
                System.err.println("Warning: Failed to load ICC profile: " + e.getMessage());
                this.iccColorSpace = null;
            }
        }

        // Convert image array to BufferedImage
        return createBufferedImage();
    }

    /**
     * Parse Start of Frame segment
     */
    private void startOfFrame(byte[] data, int marker) {
        int dataHeader = 0;
        // Determine scan mode using Marker enum
        switch (Marker.fromShort((short) marker)) {
            case SOF0:
                scanMode = ScanMode.BASELINE_DCT;
                break;
            case SOF1:
                scanMode = ScanMode.EXTENDED_DCT; // 12-bit extended sequential
                break;
            case SOF2:
                scanMode = ScanMode.PROGRESSIVE_DCT;
                break;
            case SOF3:
                scanMode = ScanMode.LOSSLESS_HUFFMAN;
                throw new RuntimeException(scanMode + " JPEG is not supported.");
            case SOF9:
                scanMode = ScanMode.SEQUENTIAL_ARITHMETIC;
                throw new RuntimeException(scanMode + " JPEG is not supported.");
            case SOF10:
                scanMode = ScanMode.PROGRESSIVE_ARITHMETIC;
                throw new RuntimeException(scanMode + " JPEG is not supported.");
            default:
                throw new RuntimeException("Unsupported SOF marker: " + String.format("0x%04X", marker));
        }

        // Check sample precision
        int precision = data[dataHeader++] & 0xFF;
        if (precision != 8 && precision != 12) {
            throw new RuntimeException("Unsupported color depth: " + precision + "-bit. Only 8-bit and 12-bit are supported.");
        }
        this.samplePrecision = precision;

        // Get image dimensions
        height = ((data[dataHeader] & 0xFF) << 8) | (data[dataHeader + 1] & 0xFF);
        dataHeader += 2;
        width = ((data[dataHeader] & 0xFF) << 8) | (data[dataHeader + 1] & 0xFF);
        dataHeader += 2;

        if (width == 0) {
            throw new RuntimeException("Image width cannot be zero");
        }

        // Get number of components
        int componentsAmount = data[dataHeader++] & 0xFF;
        if (componentsAmount != 1 && componentsAmount != 3 && componentsAmount != 4)
            throw new RuntimeException("Unsupported number of components: " + componentsAmount);

        // Parse color components
        for (int i = 0; i < componentsAmount; i++) {
            byte componentId = data[dataHeader++];
            int sampling = data[dataHeader++] & 0xFF;
            byte hSampling = (byte) ((sampling >> 4) & 0x0F);
            byte vSampling = (byte) (sampling & 0x0F);
            byte qtId = data[dataHeader++];
            Component component = new Component(componentId, hSampling, vSampling, qtId);
            colorComponents.put((int) componentId, component);
            componentIdToOrder.put((int) componentId, i); // Store order: 0=Y, 1=Cb, 2=Cr
        }

        // Calculate sample shape (for upsampling subsampled components)
        int maxWidth = 0, maxHeight = 0;
        for (Component comp : colorComponents.values()) {
            int shapeW = 8 * comp.getHSampleFactor();
            int shapeH = 8 * comp.getVSampleFactor();
            maxWidth = Math.max(maxWidth, shapeW);
            maxHeight = Math.max(maxHeight, shapeH);
        }
        sampleShape = new int[] {maxWidth, maxHeight};
        fileHeader += data.length;
    }

    /**
     * Parse Define Huffman Table segment
     * Uses HuffmanTbl from repo for proper JPEG Huffman decoding
     */
    private void defineHuffmanTable(byte[] data) {
        int dataHeader = 0;
        while (dataHeader < data.length) {
            int tableInfo = data[dataHeader++] & 0xFF;
            // Count codes of each length (1-16 bits)
            byte[] bits = new byte[16];
            int totalValues = 0;
            for (int i = 0; i < 16; i++) {
                bits[i] = data[dataHeader];
                totalValues += (data[dataHeader++] & 0xFF);
            }
            // Get Huffman values
            byte[] values = new byte[totalValues];
            for (int i = 0; i < totalValues; i++) {
                values[i] = data[dataHeader++];
            }
            // Create HuffmanTbl with decoder tables and cache frequently-accessed arrays
            HuffmanTbl huffmanTbl = new HuffmanTbl(bits, values);
            huffmanTbl.generateDecoderTables();
            HuffmanDecoderCache cache = new HuffmanDecoderCache(huffmanTbl);
            huffmanDecoderTables.put(tableInfo, cache);
        }
        fileHeader += data.length;
    }

    /**
     * Parse Define Quantization Table segment
     * Enhanced to use QTable from repo
     */
    private void defineQuantizationTable(byte[] data) {
        int dataHeader = 0;
        while (dataHeader < data.length) {
            int tableInfo = data[dataHeader++] & 0xFF;
            int precision = (tableInfo >> 4) & 0x0F; // 0=8-bit, 1=16-bit
            int tableId = tableInfo & 0x0F;
            // Get 64 values in zigzag order
            int[] qtValues = new int[64];
            if (precision == 0) {
                // 8-bit precision
                for (int i = 0; i < 64; i++) {
                    qtValues[i] = data[dataHeader++] & 0xFF;
                }
            } else {
                // 16-bit precision
                for (int i = 0; i < 64; i++) {
                    qtValues[i] = ((data[dataHeader] & 0xFF) << 8) | (data[dataHeader + 1] & 0xFF);
                    dataHeader += 2;
                }
            }
            // Undo zigzag to create 8x8 matrix for current decoder logic
            int[][] quantizationTable = undoZigzag(qtValues);
            quantizationTables.put(tableId, quantizationTable);
        }
        fileHeader += data.length;
    }

    /**
     * Parse Define Restart Interval segment
     */
    private void defineRestartInterval(byte[] data) {
        restartInterval = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        fileHeader += data.length;
    }

    /**
     * Parse APP1 marker to extract exif data
     * For image decoding, we are interested in exif orientation only
     */
    private void parseAPP1(byte[] data) {
        // Check if this is an Exif (starts with "Exif\0\0")
        if (data.length > 6 && data[0] == 'E' && data[1] == 'x' && data[2] == 'i' &&
            data[3] == 'f' && data[4] == 0 && data[5] == 0) {
            Exif exif = new JpegExif(ArrayUtils.subArray(data, 6, data.length - 6));
            IFD imageIFD = exif.getImageIFD();
            if (imageIFD != null) {
                TiffField<?> f_orientation = imageIFD.getField(TiffTag.ORIENTATION);
                orientation = (f_orientation != null) ? (int)f_orientation.getDataAsLong()[0] : 1;
            }
        }
        fileHeader += data.length;
    }

    /**
     * Parse APP2 marker to extract ICC Profile
     */
    private void parseAPP2(byte[] data) {
        // Check if this is an ICC profile (starts with "ICC_PROFILE\0")
        if (data.length > 14 && data[0] == 'I' && data[1] == 'C' && data[2] == 'C' &&
            data[3] == '_' && data[4] == 'P' && data[5] == 'R' && data[6] == 'O' &&
            data[7] == 'F' && data[8] == 'I' && data[9] == 'L' && data[10] == 'E' && data[11] == 0) {
            
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
        fileHeader += data.length;
    }

    /**
     * Parse APP14 (Adobe) marker to determine color transform
     * Transform value: 0=CMYK, 1=YCbCr, 2=YCCK
     */
    private void parseAPP14(byte[] data) {
        // Check for Adobe marker
        if (data.length >= 12 && data[0] == 'A' && data[1] == 'd' &&
            data[2] == 'o' && data[3] == 'b' && data[4] == 'e') {
            // Transform is at byte 11
            int transform = data[11] & 0xFF;
            // Transform 0=CMYK, 1=YCbCr, 2=YCCK
            isYCCK = (transform == 2);
        }
        fileHeader += data.length;
    }

    /**
     * Parse Start of Scan segment and begin decoding
     */
    private void startOfScan(byte[] data) {
        int dataHeader = 0;
        // Number of components in this scan
        int componentsAmount = data[dataHeader++] & 0xFF;
        // Get component parameters and set their Huffman table assignments
        Map<Integer, Component> myColorComponents = new HashMap<>();
        for (int i = 0; i < componentsAmount; i++) {
            int componentId = data[dataHeader++] & 0xFF;
            int huffTableSelector = data[dataHeader++] & 0xFF;
            int dcTable = (huffTableSelector >> 4) & 0x0F;
            int acTable = (huffTableSelector & 0x0F) | 0x10;
            Component component = colorComponents.get(componentId);
            if (component == null) {
                throw new RuntimeException("Component " + componentId + " not found in SOS. Available: " + colorComponents.keySet());
            }
            component.setDCTableNumber((byte) dcTable);
            component.setACTableNumber((byte) acTable);
            myColorComponents.put(componentId, component);
        }

        // Progressive scan parameters
        int spectralSelectionStart = 0;
        int spectralSelectionEnd = 63;
        int bitPositionHigh = 0;
        int bitPositionLow = 0;
        if (scanMode == ScanMode.PROGRESSIVE_DCT) {
            spectralSelectionStart = data[dataHeader++] & 0xFF;
            spectralSelectionEnd = data[dataHeader++] & 0xFF;
            int approximation = data[dataHeader++] & 0xFF;
            bitPositionHigh = (approximation >> 4) & 0x0F;
            bitPositionLow = approximation & 0x0F;
        }
        fileHeader += data.length;

        // Define number of lines if height was 0
        if (height == 0) {
            // Search for DNL marker
            for (int i = fileHeader; i < rawFile.length - 5; i++) {
                if ((rawFile[i] & 0xFF) == 0xFF && (rawFile[i+1] & 0xFF) == 0xDC) {
                    height = ((rawFile[i+4] & 0xFF) << 8) | (rawFile[i+5] & 0xFF);
                    break;
                }
            }
            if (height == 0) {
                throw new RuntimeException("Image height cannot be zero");
            }
        }

        // Calculate MCU dimensions
        if (componentsAmount > 1) {
            int maxH = 0, maxV = 0;
            for (Component comp : colorComponents.values()) {
                maxH = Math.max(maxH, comp.getHSampleFactor());
                maxV = Math.max(maxV, comp.getVSampleFactor());
            }
            mcuWidth = 8 * maxH;
            mcuHeight = 8 * maxV;
        } else {
            mcuWidth = 8;
            mcuHeight = 8;
        }

        // Calculate MCU counts
        double layerWidth, layerHeight;
        if (componentsAmount > 1) {
            mcuCountH = (width + mcuWidth - 1) / mcuWidth;
            mcuCountV = (height + mcuHeight - 1) / mcuHeight;
            layerWidth = width;
            layerHeight = height;
        } else {
            Component comp = myColorComponents.values().iterator().next();
            int shapeW = 8 * comp.getHSampleFactor();
            int shapeH = 8 * comp.getVSampleFactor();
            double sampleRatioH = (double) sampleShape[0] / shapeW;
            double sampleRatioV = (double) sampleShape[1] / shapeH;
            layerWidth = width / sampleRatioH;
            layerHeight = height / sampleRatioV;
            mcuCountH = (int) Math.ceil(layerWidth / mcuWidth);
            mcuCountV = (int) Math.ceil(layerHeight / mcuHeight);
        }
        mcuCount = mcuCountH * mcuCountV;

        // Create image array if not already created
        if (imageArray == null) {
            int countH = (width + sampleShape[0] - 1) / sampleShape[0];
            int countV = (height + sampleShape[1] - 1) / sampleShape[1];
            arrayWidth = sampleShape[0] * countH;
            arrayHeight = sampleShape[1] * countV;
            arrayDepth = colorComponents.size();
            // Use 1D array for memory efficiency: row-major order [y][x][component]
            imageArray = new short[arrayWidth * arrayHeight * arrayDepth];
        }

        // Setup scan counter
        if (scanCount == 0) {
            // Count SOS markers in remaining file
            scanAmount = 1;
            for (int i = fileHeader; i < rawFile.length - 1; i++) {
                if ((rawFile[i] & 0xFF) == 0xFF && (rawFile[i+1] & 0xFF) == 0xDA) {
                    scanAmount++;
                }
            }
        }

        // Begin scan based on mode
        if (scanMode == ScanMode.BASELINE_DCT || scanMode == ScanMode.EXTENDED_DCT) {
            baselineDCTScan(myColorComponents);
        } else if (scanMode == ScanMode.PROGRESSIVE_DCT) {
            progressiveDCTScan(myColorComponents, spectralSelectionStart, spectralSelectionEnd, bitPositionHigh, bitPositionLow);
        }
    }

    /**
     * Fill a range of MCUs [startMCU, endMCU) with neutral grey value (128 for all components)
     * For single MCU fill, pass endMCU = startMCU + 1
     */
    private void fillMCUsWithGrey(int startMCU, int endMCU, List<Map.Entry<Integer, Component>> sortedComponents) {
        for (int mcuIndex = startMCU; mcuIndex < endMCU && mcuIndex < mcuCount; mcuIndex++) {
            int mcuY = mcuIndex / mcuCountH;
            int mcuX = mcuIndex % mcuCountH;
            for (Map.Entry<Integer, Component> entry : sortedComponents) {
                int order = componentIdToOrder.get(entry.getKey());
                short neutralValue = 128; // Neutral grey
                int x = mcuWidth * mcuX;
                int y = mcuHeight * mcuY;
                for (int dy = 0; dy < mcuHeight && (y + dy) < arrayHeight; dy++) {
                    for (int dx = 0; dx < mcuWidth && (x + dx) < arrayWidth; dx++) {
                        int idx = (y + dy) * arrayWidth * arrayDepth + (x + dx) * arrayDepth + order;
                        imageArray[idx] = neutralValue;
                    }
                }
            }
        }
    }

    /**
     * Fill DC coefficients for a range of MCUs [startMCU, endMCU) with neutral grey (128)
     * Used during progressive DC scan error recovery. Only fills DC coefficients (one value per 8x8 block),
     * not entire blocks. For single MCU fill, pass endMCU = startMCU + 1
     */
    private void fillProgressiveDCMCUsWithGrey(int startMCU, int endMCU, List<Map.Entry<Integer, Component>> sortedComponents, int componentsAmount, boolean refining) {
        for (int fillMCU = startMCU; fillMCU < endMCU && fillMCU < mcuCount; fillMCU++) {
            for (Map.Entry<Integer, Component> entry : sortedComponents) {
                int order = componentIdToOrder.get(entry.getKey());
                Component component = entry.getValue();
                int hSampling = component.getHSampleFactor();
                int vSampling = component.getVSampleFactor();
                int shapeW = 8 * hSampling;
                int shapeH = 8 * vSampling;
                int x = (fillMCU % mcuCountH) * shapeW;
                int y = (fillMCU / mcuCountH) * shapeH;
                int repeat = (componentsAmount > 1) ? (hSampling * vSampling) : 1;

                for (int blockCount = 0; blockCount < repeat; blockCount++) {
                    int blockY = (blockCount / hSampling) * 8;
                    int blockX = (blockCount % hSampling) * 8;
                    int deltaX = blockX;
                    int deltaY = blockY;
                    if (!refining && (x + deltaX) < arrayWidth && (y + deltaY) < arrayHeight) {
                        int idx = (y + deltaY) * arrayWidth * arrayDepth + (x + deltaX) * arrayDepth + order;
                        imageArray[idx] = 128;
                    }
                }
            }
        }
    }

    /**
     * Baseline DCT scan (sequential encoding)
     *
     * Error Recovery Strategy:
     * ====
     * When decoding fails for an MCU:
     * 1. Fill corrupted MCU with grey (128)
     * 2. If restart markers are enabled (restartInterval > 0):
     * a) Search for next restart marker in bitstream
     * b) If found:
     * Fill all MCUs between current and next restart boundary with grey
     * Reset DC predictors
     * Jump to next restart boundary and continue decoding
     * c) If NOT found:
     * Fill all remaining MCUs with grey
     * Stop decoding (unrecoverable)
     * 3. If no restart markers (restartInterval == 0):
     * Fill all remaining MCUs with grey
     * Stop decoding (cannot resynchronize)
     *
     * Control Flow:
     * ====
     * Happy path: decode MCU -> currentMCU++
     * Error + found: fill MCUs -> reset predictors -> jump to restart boundary -> continue
     * Error + lost: fill all remaining -> break (exit loop)
     */
    private void baselineDCTScan(Map<Integer, Component> myColorComponents) {
        // Initialize bit stream reader
        BitStreamReader bitStream = new BitStreamReader();
        int componentsAmount = myColorComponents.size();
        short[] previousDC = new short[componentsAmount];
        Arrays.fill(previousDC, (short) 0);

        // Sort components by their declared order for consistent processing
        List<Map.Entry<Integer, Component>> sortedComponents = new ArrayList<>(myColorComponents.entrySet());
        sortedComponents.sort(Comparator.comparingInt(e -> componentIdToOrder.get(e.getKey())));

        int currentMCU = 0;
        int errorCount = 0;

        while (currentMCU < mcuCount) {
            int mcuY = currentMCU / mcuCountH;
            int mcuX = currentMCU % mcuCountH;

            try {
                for (Map.Entry<Integer, Component> entry : sortedComponents) {
                    int componentId = entry.getKey();
                    Component component = entry.getValue();
                    int order = componentIdToOrder.get(componentId); // Get proper array index
                    int[][] quantizationTable = quantizationTables.get((int) component.getQTableNumber());

                    // Allocate MCU for this component
                    int hSampling = component.getHSampleFactor();
                    int vSampling = component.getVSampleFactor();
                    int repeat = (componentsAmount > 1) ? (hSampling * vSampling) : 1;
                    int mcuW = (componentsAmount > 1) ? (8 * hSampling) : 8;
                    int mcuH = (componentsAmount > 1) ? (8 * vSampling) : 8;
                    short[][] myMCU = new short[mcuW][mcuH];

                    for (int blockCount = 0; blockCount < repeat; blockCount++) {
                        // Decode 8x8 block
                        short[] block = new short[64];

                        // Decode DC value using cached decoder
                        HuffmanDecoderCache dcTable = huffmanDecoderTables.get((int) component.getDCTableNumber());
                        int huffmanValue = decodeHuffmanValue(bitStream, dcTable);
                        short dcValue = (short) (decodeTwosComplement(bitStream, huffmanValue) + previousDC[order]);
                        previousDC[order] = dcValue;
                        block[0] = dcValue;

                        // Decode AC values using cached decoder
                        HuffmanDecoderCache acTable = huffmanDecoderTables.get((int) component.getACTableNumber());
                        int index = 1;
                        while (index < 64) {
                            huffmanValue = decodeHuffmanValue(bitStream, acTable);
                            if (huffmanValue == 0x00) break; // End of block

                            int zeroRunLength = (huffmanValue >> 4) & 0x0F;
                            index += zeroRunLength;
                            if (index >= 64) break;

                            int acBitLength = huffmanValue & 0x0F;
                            if (acBitLength > 0) {
                                short acValue = (short) decodeTwosComplement(bitStream, acBitLength);
                                block[index] = acValue;
                            }
                            index++;
                        }

                        // Undo zigzag, dequantize, and convert to float in one operation
                        float[][] floatBlock = undoZigzagAndDequantize(block, quantizationTable);
                        // Apply IDCT
                        float[][] spatial = DCT.inverseDCT(floatBlock);
                        // Copy to MCU (with level shift: +128 for 8-bit, +2048 for 12-bit)
                        int levelShift = (samplePrecision == 12) ? 2048 : 128;
                        int blockY = (blockCount / hSampling) * 8;
                        int blockX = (blockCount % hSampling) * 8;
                        for (int y = 0; y < 8; y++) {
                            for (int x = 0; x < 8; x++) {
                                myMCU[blockX + x][blockY + y] = (short) Math.round(spatial[x][y] + levelShift);
                            }
                        }
                    }

                    // Upsample if necessary
                    int shapeW = 8 * hSampling;
                    int shapeH = 8 * vSampling;
                    if (shapeW != sampleShape[0] || shapeH != sampleShape[1]) {
                        myMCU = upsample(myMCU, sampleShape);
                    }

                    // Copy MCU to image array
                    int x = mcuWidth * mcuX;
                    int y = mcuHeight * mcuY;
                    for (int dy = 0; dy < myMCU[0].length && (y + dy) < arrayHeight; dy++) {
                        for (int dx = 0; dx < myMCU.length && (x + dx) < arrayWidth; dx++) {
                            int idx = (y + dy) * arrayWidth * arrayDepth + (x + dx) * arrayDepth + order;
                            imageArray[idx] = myMCU[dx][dy];
                        }
                    }
                }
                // Success: Move to next MCU
                currentMCU++;

            } catch (RuntimeException e) {
                // Error decoding current MCU attempt recovery
                errorCount++;
                if (errorCount == 1) {
                    System.err.println("Warning: JPEG decoding errors encountered");
                }

                // Fill the CURRENT corrupted MCU with neutral grey (Y=128, Cb=128, Cr=128)
                fillMCUsWithGrey(currentMCU, currentMCU + 1, sortedComponents);

                // Determine recovery strategy based on restart marker availability
                if (restartInterval > 0) {
                    // Strategy: Try to resynchronize at next restart marker
                    boolean found = bitStream.searchForRestartMarker();
                    if (found) {
                        // Successfully found restart marker can continue decoding
                        Arrays.fill(previousDC, (short) 0);
                        // Calculate next restart boundary and fill all skipped MCUs with grey
                        int nextRestartBoundary = ((currentMCU / restartInterval) + 1) * restartInterval;
                        int filledMCUs = Math.min(nextRestartBoundary, mcuCount) - (currentMCU + 1);
                        if (filledMCUs > 0) {
                            fillMCUsWithGrey(currentMCU + 1, nextRestartBoundary, sortedComponents);
                            errorCount += filledMCUs;
                        }
                        // Jump to next restart interval and continue
                        currentMCU = nextRestartBoundary;
                        continue;
                    } else {
                        // No more restart markers found cannot recover
                        int filledMCUs = mcuCount - (currentMCU + 1);
                        System.err.println("No more restart markers found after error at MCU " + currentMCU);
                        fillMCUsWithGrey(currentMCU + 1, mcuCount, sortedComponents);
                        errorCount += filledMCUs;
                        break; // Exit loop
                    }
                } else {
                    // No restart markers defined cannot resynchronize reliably
                    int filledMCUs = mcuCount - (currentMCU + 1);
                    System.err.println("No restart markers available, cannot recover from error at MCU " + currentMCU);
                    fillMCUsWithGrey(currentMCU + 1, mcuCount, sortedComponents);
                    errorCount += filledMCUs;
                    break; // Exit loop
                }
            }

            // Normal restart interval check
            if (restartInterval > 0 && currentMCU % restartInterval == 0 && currentMCU != mcuCount) {
                bitStream.reset();
                Arrays.fill(previousDC, (short) 0);
            }
        }

        if (errorCount > 0) {
            double percentDecoded = ((mcuCount - errorCount) * 100.0 / mcuCount);
            System.err.println("Completed with " + errorCount + " MCU errors (" + String.format("%.1f", percentDecoded) + "% successfully decoded)");
        }
        scanCount++;
    }

    /**
     * Progressive DCT scan (with spectral selection and successive approximation)
     * This is the complex part that handles progressive JPEG decoding
     */
    private void progressiveDCTScan(Map<Integer, Component> myColorComponents, int spectralSelectionStart, int spectralSelectionEnd, int bitPositionHigh, int bitPositionLow) {
        // Determine if this scan contains DC or AC values
        String values;
        if (spectralSelectionStart == 0 && spectralSelectionEnd == 0) {
            values = "dc";
        } else if (spectralSelectionStart > 0 && spectralSelectionEnd >= spectralSelectionStart) {
            values = "ac";
        } else {
            throw new RuntimeException("Progressive JPEG cannot contain both DC and AC in same scan");
        }

        // Determine if this is a refining scan
        boolean refining;
        if (bitPositionHigh == 0) {
            refining = false;
        } else if (bitPositionHigh == bitPositionLow + 1) {
            refining = true;
        } else {
            throw new RuntimeException("Progressive JPEG cannot have more than 1 bit per refining scan");
        }

        BitStreamReader bitStream = new BitStreamReader();
        int componentsAmount = myColorComponents.size();
        if ("ac".equals(values) && componentsAmount > 1) {
            throw new RuntimeException("AC progressive scan can only have single component");
        }

        int currentMCU = 0;
        try {
            // DC values scan
            if ("dc".equals(values)) {
                // Note: previousDC is now an instance variable that persists across all scans
                // Only reset at restart markers, not at start of each scan
                List<Map.Entry<Integer, Component>> sortedComponents = new ArrayList<>(myColorComponents.entrySet());
                sortedComponents.sort(Comparator.comparingInt(e -> componentIdToOrder.get(e.getKey())));

                while (currentMCU < mcuCount) {
                    for (Map.Entry<Integer, Component> entry : sortedComponents) {
                        int componentId = entry.getKey();
                        Component component = entry.getValue();
                        int order = componentIdToOrder.get(componentId);

                        int hSampling = component.getHSampleFactor();
                        int vSampling = component.getVSampleFactor();
                        int blockW = 8;
                        int blockH = 8;
                        int x, y, repeat;

                        if (componentsAmount > 1) {
                            int shapeW = 8 * hSampling;
                            int shapeH = 8 * vSampling;
                            x = (currentMCU % mcuCountH) * shapeW;
                            y = (currentMCU / mcuCountH) * shapeH;
                            repeat = hSampling * vSampling;
                        } else {
                            x = (currentMCU % mcuCountH) * blockW;
                            y = (currentMCU / mcuCountH) * blockH;
                            repeat = 1;
                        }

                        for (int blockCount = 0; blockCount < repeat; blockCount++) {
                            int deltaX = (componentsAmount > 1) ? (blockCount % hSampling) * 8 : 0;
                            int deltaY = (componentsAmount > 1) ? (blockCount / hSampling) * 8 : 0;

                            if (!refining) {
                                // First scan: decode partial DC value
                                HuffmanDecoderCache dcTable = huffmanDecoderTables.get((int) component.getDCTableNumber());
                                int huffmanValue = decodeHuffmanValue(bitStream, dcTable);
                                short dcValue = (short) (decodeTwosComplement(bitStream, huffmanValue) + previousDC[order]);
                                previousDC[order] = dcValue;

                                if (x + deltaX < arrayWidth && y + deltaY < arrayHeight) {
                                    int idx = (y + deltaY) * arrayWidth * arrayDepth + (x + deltaX) * arrayDepth + order;
                                    imageArray[idx] = (short) (dcValue << bitPositionLow);
                                }
                            } else {
                                // Refining scan: add next bit
                                int newBit = bitStream.readBits(1);
                                if (x + deltaX < arrayWidth && y + deltaY < arrayHeight) {
                                    int idx = (y + deltaY) * arrayWidth * arrayDepth + (x + deltaX) * arrayDepth + order;
                                    imageArray[idx] |= (newBit << bitPositionLow);
                                }
                            }
                        }
                    }
                    currentMCU++;

                    // Check restart interval
                    if (restartInterval > 0 && currentMCU % restartInterval == 0 && currentMCU != mcuCount) {
                        bitStream.reset();
                        Arrays.fill(previousDC, (short) 0);
                    }
                }
            } 
            // AC values scan
            else if ("ac".equals(values)) {
                Map.Entry<Integer, Component> entry = myColorComponents.entrySet().iterator().next();
                int componentId = entry.getKey();
                Component component = entry.getValue();
                int componentOrder = componentIdToOrder.get(componentId);

                HuffmanDecoderCache acTable = huffmanDecoderTables.get((int) component.getACTableNumber());
                int eobRun = 0;
                int zeroRun = 0;
                List<int[]> toRefine = new ArrayList<>();

                while (currentMCU < mcuCount) {
                    int x = (currentMCU % mcuCountH) * 8;
                    int y = (currentMCU / mcuCountH) * 8;
                    int index = spectralSelectionStart;

                    if (eobRun > 0) {
                        // Process existing EOB run
                        while (index <= spectralSelectionEnd) {
                            if (refining) {
                                int[] pos = ZIGZAG_INVERSE[index];
                                int idx = (y + pos[1]) * arrayWidth * arrayDepth + (x + pos[0]) * arrayDepth + componentOrder;
                                if (imageArray[idx] != 0) {
                                    toRefine.add(new int[]{x + pos[0], y + pos[1]});
                                }
                            }
                            index++;
                        }
                        eobRun--;
                    } else {
                        while (index <= spectralSelectionEnd) {
                            int huffmanValue = decodeHuffmanValue(bitStream, acTable);
                            int runMagnitude = (huffmanValue >> 4) & 0x0F;
                            int acBitsLength = huffmanValue & 0x0F;

                            if (acBitsLength == 0) {
                                if (runMagnitude < 15) {
                                    // EOB run
                                    String eobBits = bitStream.readBitsString(runMagnitude);
                                    eobRun = (1 << runMagnitude) + Integer.parseInt(eobBits.isEmpty() ? "0" : eobBits, 2);
                                    // Process refine bits for remaining coefficients in this block
                                    while (index <= spectralSelectionEnd) {
                                        if (refining) {
                                            int[] pos = ZIGZAG_INVERSE[index];
                                            int idx = (y + pos[1]) * arrayWidth * arrayDepth + (x + pos[0]) * arrayDepth + componentOrder;
                                            if (imageArray[idx] != 0) toRefine.add(new int[]{x + pos[0], y + pos[1]});
                                        }
                                        index++;
                                    }
                                    eobRun--; // Decrement because we are in an MCU
                                    break;
                                } else {
                                    // ZRL run
                                    zeroRun = 16;
                                }
                            } else {
                                zeroRun = runMagnitude;
                                short acValue = (short) decodeTwosComplement(bitStream, acBitsLength);
                                
                                // Perform zero run + find coefficient position
                                while (zeroRun > 0 || (refining && index <= spectralSelectionEnd)) {
                                    int[] pos = ZIGZAG_INVERSE[index];
                                    int idx = (y + pos[1]) * arrayWidth * arrayDepth + (x + pos[0]) * arrayDepth + componentOrder;
                                    if (imageArray[idx] == 0) {
                                        if (zeroRun == 0) break; // Found position for current AC
                                        zeroRun--;
                                    } else if (refining) {
                                        toRefine.add(new int[]{x + pos[0], y + pos[1]});
                                    }
                                    index++;
                                }
                                
                                int[] pos = ZIGZAG_INVERSE[index];
                                int idx = (y + pos[1]) * arrayWidth * arrayDepth + (x + pos[0]) * arrayDepth + componentOrder;
                                if (refining) {
                                    imageArray[idx] |= (short) (acValue << bitPositionLow);
                                } else {
                                    imageArray[idx] = (short) (acValue << bitPositionLow);
                                }
                                index++;
                            }
                        }
                    }

                    if (refining) {
                        refineAC(bitStream, toRefine, bitPositionLow, componentOrder);
                        toRefine.clear();
                    }

                    currentMCU++;
                    if (restartInterval > 0 && currentMCU % restartInterval == 0 && currentMCU < mcuCount) {
                        bitStream.reset();
                    }
                }
            }
        } catch (RuntimeException e) {
            System.err.println("Warning: Progressive scan error at MCU " + currentMCU + "/" + mcuCount + ":" + e.getMessage());
            if (!refining) {
                fillProgressiveDCMCUsWithGrey(currentMCU, mcuCount, new ArrayList<>(myColorComponents.entrySet()), componentsAmount, refining);
            }
        }

        scanCount++;
        if (scanCount == scanAmount) {
            performProgressiveIDCT();
        }
    }

    private void refineAC(BitStreamReader bitStream, List<int[]> toRefine, int bitPositionLow, int componentOrder) {
        if (toRefine.isEmpty()) return;
        String refineBits = bitStream.readBitsString(toRefine.size());
        for (int i = 0; i < toRefine.size(); i++) {
            int[] coords = toRefine.get(i);
            int newBit = refineBits.charAt(i) - '0';
            int idx = coords[1] * arrayWidth * arrayDepth + coords[0] * arrayDepth + componentOrder;
            if (newBit != 0) {
                if (imageArray[idx] > 0) imageArray[idx] += (1 << bitPositionLow);
                else imageArray[idx] -= (1 << bitPositionLow);
            }
        }
    }

    private void performProgressiveIDCT() {
        short[] dctArray = new short[imageArray.length];
        System.arraycopy(imageArray, 0, dctArray, 0, imageArray.length);

        for (Map.Entry<Integer, Component> entry : colorComponents.entrySet()) {
            int componentId = entry.getKey();
            Component component = entry.getValue();
            int order = componentIdToOrder.get(componentId);
            int[][] quantizationTable = quantizationTables.get((int) component.getQTableNumber());

            int hSampling = component.getHSampleFactor();
            int vSampling = component.getVSampleFactor();
            int shapeW = 8 * hSampling;
            int shapeH = 8 * vSampling;
            int ratioH = sampleShape[0] / shapeW;
            int ratioV = sampleShape[1] / shapeH;

            int componentWidth = arrayWidth / ratioH;
            int componentHeight = arrayHeight / ratioV;
            int compMCUH = componentWidth / 8;
            int compMCUV = componentHeight / 8;

            for (int mcu = 0; mcu < compMCUH * compMCUV; mcu++) {
                int x1 = (mcu % compMCUH) * 8;
                int y1 = (mcu / compMCUH) * 8;
                short[][] block = new short[8][8];

                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int idx = (y1 + y) * arrayWidth * arrayDepth + (x1 + x) * arrayDepth + order;
                        block[x][y] = dctArray[idx];
                    }
                }

                float[][] floatBlock = new float[8][8];
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        floatBlock[x][y] = block[x][y] * quantizationTable[x][y];
                    }
                }

                float[][] spatial = DCT.inverseDCT(floatBlock);
                int levelShift = (samplePrecision == 12) ? 2048 : 128;
                int maxValue = (samplePrecision == 12) ? 4095 : 255;

                short[][] idctBlock = new short[8][8];
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int val = (int) Math.round(spatial[x][y] + levelShift);
                        idctBlock[x][y] = (short) Math.max(0, Math.min(maxValue, val));
                    }
                }

                if (shapeW != sampleShape[0] || shapeH != sampleShape[1]) {
                    short[][] upsampled = upsample(idctBlock, sampleShape);
                    int x2 = x1 * ratioH;
                    int y2 = y1 * ratioV;
                    for (int y = 0; y < upsampled[0].length && (y2 + y) < arrayHeight; y++) {
                        for (int x = 0; x < upsampled.length && (x2 + x) < arrayWidth; x++) {
                            int idx = (y2 + y) * arrayWidth * arrayDepth + (x2 + x) * arrayDepth + order;
                            imageArray[idx] = upsampled[x][y];
                        }
                    }
                } else {
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            int idx = (y1 + y) * arrayWidth * arrayDepth + (x1 + x) * arrayDepth + order;
                            imageArray[idx] = idctBlock[x][y];
                        }
                    }
                }
            }
        }
    }

    private void endOfImage() {
        scanFinished = true;
    }

    private BufferedImage createBufferedImage() {
        BufferedImage image;
        if (arrayDepth == 4) {
            byte[] cmykPixels = new byte[width * height * 4];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float c, m, yVal, k;
                    int idx = y * arrayWidth * arrayDepth + x * arrayDepth;
                    if (isYCCK) {
                        float Y_stored = imageArray[idx];
                        float Cb_stored = imageArray[idx + 1];
                        float Cr_stored = imageArray[idx + 2];
                        k = 255.0f - imageArray[idx + 3];
                        
                        float luma = 255.0f - Y_stored;
                        float Cb_std = 128.0f - Cb_stored;
                        float Cr_std = 128.0f - Cr_stored;
                        
                        float cInv = luma + 1.402f * Cr_std;
                        float mInv = luma - 0.344136f * Cb_std - 0.714136f * Cr_std;
                        float yInv = luma + 1.772f * Cb_std;
                        
                        c = 255.0f - cInv;
                        m = 255.0f - mInv;
                        yVal = 255.0f - yInv;
                    } else {
                        c = 255.0f - imageArray[idx];
                        m = 255.0f - imageArray[idx + 1];
                        yVal = 255.0f - imageArray[idx + 2];
                        k = 255.0f - imageArray[idx + 3];
                    }
                    int pixelIndex = (y * width + x) * 4;
                    cmykPixels[pixelIndex] = (byte) Math.max(0, Math.min(255, Math.round(c)));
                    cmykPixels[pixelIndex + 1] = (byte) Math.max(0, Math.min(255, Math.round(m)));
                    cmykPixels[pixelIndex + 2] = (byte) Math.max(0, Math.min(255, Math.round(yVal)));
                    cmykPixels[pixelIndex + 3] = (byte) Math.max(0, Math.min(255, Math.round(k)));
                }
            }
            if (iccColorSpace != null) {
                image = cmykToRGBWithICC(cmykPixels);
            } else {
                image = cmykToRGBStandard(cmykPixels);
            }
        } else if (arrayDepth == 3) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = y * arrayWidth * arrayDepth + x * arrayDepth;
                    double Y, Cb, Cr;
                    if (samplePrecision == 12) {
                        Y = imageArray[idx] * 255.0 / 4095.0;
                        Cb = imageArray[idx + 1] * 255.0 / 4095.0;
                        Cr = imageArray[idx + 2] * 255.0 / 4095.0;
                    } else {
                        Y = imageArray[idx];
                        Cb = imageArray[idx + 1];
                        Cr = imageArray[idx + 2];
                    }
                    int R = (int) Math.round(Y + 1.402 * (Cr - 128.0));
                    int G = (int) Math.round(Y - 0.34414 * (Cb - 128.0) - 0.71414 * (Cr - 128.0));
                    int B = (int) Math.round(Y + 1.772 * (Cb - 128.0));
                    R = Math.max(0, Math.min(255, R));
                    G = Math.max(0, Math.min(255, G));
                    B = Math.max(0, Math.min(255, B));
                    image.setRGB(x, y, (R << 16) | (G << 8) | B);
                }
            }
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            double scaleFactor = (samplePrecision == 12) ? (255.0 / 4095.0) : 1.0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = y * arrayWidth * arrayDepth + x * arrayDepth;
                    int val = (int) Math.round(imageArray[idx] * scaleFactor);
                    pixels[y * width + x] = (byte) Math.max(0, Math.min(255, val));
                }
            }
        }
        if (orientation != 1) {
            image = applyEXIFOrientation(image, orientation);
        }
        return image;
    }

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

    private int[][] undoZigzag(int[] block) {
        int[][] result = new int[8][8];
        for (int i = 0; i < 64; i++) {
            result[ZIGZAG_INVERSE[i][0]][ZIGZAG_INVERSE[i][1]] = block[i];
        }
        return result;
    }

    private float[][] undoZigzagAndDequantize(short[] block, int[][] quantizationTable) {
        float[][] result = new float[8][8];
        for (int i = 0; i < 64; i++) {
            int x = ZIGZAG_INVERSE[i][0];
            int y = ZIGZAG_INVERSE[i][1];
            result[x][y] = block[i] * quantizationTable[x][y];
        }
        return result;
    }

    private short[][] upsample(short[][] block, int[] newShape) {
        int oldW = block.length;
        int oldH = block[0].length;
        int newW = newShape[0];
        int newH = newShape[1];
        short[][] result = new short[newW][newH];
        for (int y = 0; y < newH; y++) {
            for (int x = 0; x < newW; x++) {
                result[x][y] = block[x * oldW / newW][y * oldH / newH];
            }
        }
        return result;
    }

    private class BitStreamReader {
        private StringBuilder bitQueue = new StringBuilder();

        void reset() {
            bitQueue.setLength(0);
            fileHeader += 2; // Skip restart marker (0xFFDx)
        }

        boolean searchForRestartMarker() {
            bitQueue.setLength(0); // Clear bit queue
            while (fileHeader < rawFile.length - 1) {
                if ((rawFile[fileHeader] & 0xFF) == 0xFF) {
                    int marker = rawFile[fileHeader + 1] & 0xFF;
                    if ((marker >= 0xD0 && marker <= 0xD7) || marker == 0xD9) {
                        fileHeader += 2; // Skip past the marker
                        return true;
                    }
                }
                fileHeader++;
            }
            return false;
        }

        int readBits(int amount) {
            fillQueue(amount);
            if (bitQueue.length() < amount) {
                throw new RuntimeException("Not enough data in stream (requested " + amount + " bits, only " + bitQueue.length() + " available)");
            }
            String bits = bitQueue.substring(0, amount);
            bitQueue.delete(0, amount);
            return Integer.parseInt(bits, 2);
        }

        String readBitsString(int amount) {
            if (amount == 0) return "";
            fillQueue(amount);
            if (bitQueue.length() < amount) {
                throw new RuntimeException("Not enough data in stream (requested " + amount + " bits, only " + bitQueue.length() + " available)");
            }
            String bits = bitQueue.substring(0, amount);
            bitQueue.delete(0, amount);
            return bits;
        }

        private void fillQueue(int amount) {
            while (bitQueue.length() < amount && fileHeader < rawFile.length) {
                int nextByte = rawFile[fileHeader++] & 0xFF;
                if (nextByte == 0xFF && fileHeader < rawFile.length) {
                    int stuffedByte = rawFile[fileHeader] & 0xFF;
                    if (stuffedByte == 0x00) {
                        fileHeader++; // Skip stuffed byte
                    }
                }
                for (int i = 7; i >= 0; i--) {
                    bitQueue.append((nextByte >> i) & 1);
                }
            }
        }
    }

    private int decodeHuffmanValue(BitStreamReader bitStream, HuffmanDecoderCache cache) {
        int[] minCode = cache.minCode;
        int[] maxCode = cache.maxCode;
        int[] valPtr = cache.valPtr;
        byte[] huffVal = cache.huffVal;

        int code = bitStream.readBits(1);
        int i = 1; // Bit length (1-16)
        while (i <= 16) {
            int idx = i - 1;
            if (code <= maxCode[idx] && code >= minCode[idx]) {
                int j = valPtr[idx] + code - minCode[idx];
                if (j < 0 || j >= huffVal.length) {
                    throw new RuntimeException("Invalid Huffman decode: j=" + j + ", i=" + i + ", code=" + code);
                }
                return huffVal[j] & 0xFF;
            }
            i++;
            if (i <= 16) {
                code = (code << 1) + bitStream.readBits(1);
            }
        }
        throw new RuntimeException("Failed to decode Huffman value (codeword too long or invalid)");
    }

    private short decodeTwosComplement(BitStreamReader bitStream, int bitLength) {
        if (bitLength == 0) return 0;
        String bits = bitStream.readBitsString(bitLength);
        if (bits.isEmpty()) return 0;
        int value = Integer.parseInt(bits, 2);
        if (bits.charAt(0) == '1') {
            return (short) value;
        } else {
            return (short) (value - (1 << bitLength) + 1);
        }
    }

    private BufferedImage cmykToRGBWithICC(byte[] cmykPixels) {
        WritableRaster cmykRaster = Raster.createInterleavedRaster(
            new DataBufferByte(cmykPixels, cmykPixels.length),
            width, height, width * 4, 4, new int[]{0, 1, 2, 3}, null
        );
        ColorModel cmykColorModel = new ComponentColorModel(iccColorSpace, false, false, java.awt.Transparency.OPAQUE, java.awt.image.DataBuffer.TYPE_BYTE);
        BufferedImage cmykImage = new BufferedImage(cmykColorModel, cmykRaster, false, null);
        
        java.awt.RenderingHints hints = new java.awt.RenderingHints(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        hints.put(java.awt.RenderingHints.KEY_COLOR_RENDERING, java.awt.RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        hints.put(java.awt.RenderingHints.KEY_DITHERING, java.awt.RenderingHints.VALUE_DITHER_DISABLE);

        ColorConvertOp cco = new ColorConvertOp(iccColorSpace, sRGB, hints);
        BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        cco.filter(cmykImage, rgbImage);
        return rgbImage;
    }

    private BufferedImage cmykToRGBStandard(byte[] cmykPixels) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelIndex = (y * width + x) * 4;
                float c = (cmykPixels[pixelIndex] & 0xFF) / 255.0f;
                float m = (cmykPixels[pixelIndex + 1] & 0xFF) / 255.0f;
                float yVal = (cmykPixels[pixelIndex + 2] & 0xFF) / 255.0f;
                float k = (cmykPixels[pixelIndex + 3] & 0xFF) / 255.0f;

                int R = Math.round(255.0f * (1.0f - c) * (1.0f - k));
                int G = Math.round(255.0f * (1.0f - m) * (1.0f - k));
                int B = Math.round(255.0f * (1.0f - yVal) * (1.0f - k));

                R = Math.max(0, Math.min(255, R));
                G = Math.max(0, Math.min(255, G));
                B = Math.max(0, Math.min(255, B));
                image.setRGB(x, y, (R << 16) | (G << 8) | B);
            }
        }
        return image;
    }
}