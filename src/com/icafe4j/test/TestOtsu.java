/**
 * COPYRIGHT (C) 2014-2025 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Any modifications to this file must keep this entire header intact.
 */

package com.icafe4j.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.util.Otsu;

/**
 * Test program for Otsu's method implementation.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/27/2025
 */
public class TestOtsu {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestOtsu.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            LOGGER.info("Usage: java com.icafe4j.test.TestOtsu <image-file>");
            LOGGER.info("Example: java com.icafe4j.test.TestOtsu images/test.jpg");
            System.exit(1);
        }

        try {
            // Read the input image
            File inputFile = new File(args[0]);
            LOGGER.info("Reading image: {}", inputFile.getAbsolutePath());

            BufferedImage image = ImageIO.read(new FileInputStream(inputFile));
            if (image == null) {
                LOGGER.error("Failed to read image. Unsupported format or file not found.");
                System.exit(1);
            }

            LOGGER.info("Image dimensions: {} x {}", image.getWidth(), image.getHeight());

            // Run Otsu's method
            long startTime = System.currentTimeMillis();
            Otsu otsu = new Otsu(image);
            long endTime = System.currentTimeMillis();

            int threshold = otsu.getThreshold();
            double elapsed = (endTime - startTime) / 1000.0;

            // Print results
            LOGGER.info("Optimal threshold: {}", threshold);
            LOGGER.info("Processing time: {} seconds", elapsed);

            // Apply threshold to create binary image
            BufferedImage thresholdedImage = otsu.applyThreshold(image);

            // Save the thresholded image
            String outputPath = getOutputPath(inputFile, "_otsu_thresholded");
            File outputFile = new File(outputPath);
            ImageIO.write(thresholdedImage, "png", outputFile);
            LOGGER.info("Thresholded image saved to: {}", outputFile.getAbsolutePath());

            // Save grayscale version for comparison
            String grayscalePath = getOutputPath(inputFile, "_grayscale");
            File grayscaleFile = new File(grayscalePath);
            BufferedImage grayscaleImage = convertToGrayscale(image);
            ImageIO.write(grayscaleImage, "png", grayscaleFile);
            LOGGER.info("Grayscale image saved to: {}", grayscaleFile.getAbsolutePath());

            LOGGER.info("Test completed successfully!");

        } catch (Exception e) {
            LOGGER.error("Test failed with exception", e);
            System.exit(1);
        }
    }

    /**
     * Generate output file path based on input file path and suffix.
     */
    private static String getOutputPath(File inputFile, String suffix) {
        String parent = inputFile.getParent();
        String name = inputFile.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = (dotIndex > 0) ? name.substring(0, dotIndex) : name;

        if (parent != null) {
            return parent + File.separator + baseName + suffix + ".png";
        } else {
            return baseName + suffix + ".png";
        }
    }

    /**
     * Convert a BufferedImage to grayscale.
     */
    private static BufferedImage convertToGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Convert to grayscale using luminosity method
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int grayRgb = (gray << 16) | (gray << 8) | gray;

                grayscale.setRGB(x, y, grayRgb);
            }
        }

        return grayscale;
    }
}