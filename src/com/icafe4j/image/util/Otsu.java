/**
 * COPYRIGHT (C) 2014-2025 WEN YU (YUWEN_66@YAHOO.COM) ALL RIGHTS RESERVED.
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
 * Otsu.java
 *
 * Who    Date         Description
 * ====   ==========   ========================================================
 * WY     27Dec2025    Initial creation - Otsu's method for automatic image thresholding
 */

package com.icafe4j.image.util;

import java.awt.image.BufferedImage;

/**
 * Otsu's Method - Finds optimal threshold between foreground and background pixels.
 * <p>
 * Also called "Optimal Global Threshold Calculator".
 * This algorithm automatically determines the best threshold value for converting
 * a grayscale image to binary by maximizing inter-class variance.
 * <p>
 * Run time: O(N) where N is the number of pixels in the image.
 * <p>
 * Reference: Nobuyuki Otsu (1979). "A threshold selection method from gray-level histograms".
 * IEEE Trans. Sys., Man., Cyber. 9 (1): 62–66.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/27/2025
 */
public class Otsu {
    // Number of possible grayscale values (0-255)
    private static final int RADIX = 256;

    // The calculated optimal threshold value
    private int threshold;

    /**
     * Finds optimal global FG/BG threshold for a BufferedImage.
     * <p>All work is done in constructor.
     *
     * @param image the BufferedImage to threshold
     */
    public Otsu(BufferedImage image) {
        int[][] pixels = getGrayscalePixels(image);
        threshold(pixels);
    }

    /**
     * Finds optimal global FG/BG threshold for array of grayscale pixel intensities.
     * <p>All work is done in constructor.
     *
     * @param pixels 2D array of grayscale pixel intensities (0-255)
     */
    public Otsu(int[][] pixels) {
        threshold(pixels);
    }

    /**
     * Finds optimal global FG/BG threshold for a 1D array of grayscale pixel intensities.
     * <p>All work is done in constructor.
     *
     * @param pixels 1D array of grayscale pixel intensities (0-255)
     */
    public Otsu(int[] pixels) {
        threshold(pixels);
    }

    /**
     * Finds optimal global FG/BG threshold for a 1D array of grayscale pixel intensities.
     * <p>All work is done in constructor.
     *
     * @param pixels 1D array of grayscale pixel intensities (0-255)
     */
    public Otsu(byte[] pixels) {
        threshold(pixels);
    }

    /**
     * Gets the calculated threshold value.
     *
     * @return the optimal threshold value (0-255)
     */
    public int getThreshold() {
        return threshold;
    }

    /**
     * Converts a BufferedImage to grayscale pixel array.
     *
     * @param image the BufferedImage to convert
     * @return 2D array of grayscale pixel intensities
     */
    private int[][] getGrayscalePixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] pixels = new int[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = image.getRGB(j, i);
                // Convert RGB to grayscale using luminosity method
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                pixels[i][j] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
            }
        }

        return pixels;
    }

    /**
     * Runs Otsu's method on a 2D pixel array.
     *
     * @param pixels 2D array of grayscale pixel intensities
     */
    private void threshold(int[][] pixels) {
        int totalPixels = pixels.length * pixels[0].length;
        int[] histogram = buildHistogram(pixels);
        threshold = findThreshold(histogram, totalPixels);
    }

    /**
     * Runs Otsu's method on a 1D pixel array.
     *
     * @param pixels 1D array of grayscale pixel intensities
     */
    private void threshold(int[] pixels) {
        int totalPixels = pixels.length;
        int[] histogram = buildHistogram(pixels);
        threshold = findThreshold(histogram, totalPixels);
    }

    /**
     * Runs Otsu's method on a 1D pixel array.
     *
     * @param pixels 1D array of grayscale pixel intensities
     */
    private void threshold(byte[] pixels) {
        int totalPixels = pixels.length;
        int[] histogram = buildHistogram(pixels);
        threshold = findThreshold(histogram, totalPixels);
    }

    /**
     * Builds a histogram from 2D pixel array.
     * <p>Run-time: O(N) where N is the number of pixels.
     *
     * @param pixels 2D array of grayscale pixel intensities
     * @return histogram array where index is intensity and value is frequency
     */
    private int[] buildHistogram(int[][] pixels) {
        int[] histogram = new int[RADIX];

        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[0].length; col++) {
                histogram[pixels[row][col]]++;
            }
        }

        return histogram;
    }

    /**
     * Builds a histogram from 1D pixel array.
     * <p>Run-time: O(N) where N is the number of pixels.
     *
     * @param pixels 1D array of grayscale pixel intensities
     * @return histogram array where index is intensity and value is frequency
     */
    private int[] buildHistogram(int[] pixels) {
        int[] histogram = new int[RADIX];

        for (int i = 0; i < pixels.length; i++) {
            histogram[pixels[i]]++;
        }

        return histogram;
    }

    /**
     * Builds a histogram from 1D pixel array.
     * <p>Run-time: O(N) where N is the number of pixels.
     *
     * @param pixels 1D array of grayscale pixel intensities
     * @return histogram array where index is intensity and value is frequency
     */
    private int[] buildHistogram(byte[] pixels) {
        int[] histogram = new int[RADIX];

        for (int i = 0; i < pixels.length; i++) {
            histogram[pixels[i] & 0xFF]++;
        }

        return histogram;
    }

    /**
     * Finds the optimal threshold value using Otsu's method.
     * <p>Run-time: O(L) where L is the number of intensity levels (256 for 8-bit images)
     * <p>
     * This implementation maximizes the between-class variance using weighted statistics.
     * It works directly with histogram counts (not probabilities) and skips empty bins
     * for optimal performance. The threshold that produces the maximum separation
     * between the two classes (foreground and background) is selected as optimal.
     *
     * @param histogram frequency distribution of pixel intensities
     * @param totalPixels total number of pixels in the image
     * @return the optimal threshold value (0-255)
     */
    private int findThreshold(int[] histogram, int totalPixels) {
        // Compute sum of all pixel intensities (for global mean calculation)
        int sumIntensities = 0;
        for (int level = 0; level < RADIX; level++) {
            sumIntensities += level * histogram[level];
        }

        // Track the best threshold and its between-class variance
        int optimalThreshold = 0;
        double maxBetweenClassVariance = Double.NEGATIVE_INFINITY;

        // Initialize background and foreground statistics
        double backgroundMean = 0.0;
        double backgroundWeight = 0.0;
        double foregroundMean = (double) sumIntensities / totalPixels;
        double foregroundWeight = totalPixels;

        // Evaluate all possible thresholds
        int t = 0;
        while (t < RADIX) {
            // Calculate between-class variance with current split
            double meanDifference = foregroundMean - backgroundMean;
            double betweenClassVariance = backgroundWeight * foregroundWeight * meanDifference * meanDifference;

            // Update optimal threshold if this one is better
            if (betweenClassVariance > maxBetweenClassVariance) {
                maxBetweenClassVariance = betweenClassVariance;
                optimalThreshold = t;
            }

            // Skip empty histogram bins for efficiency
            while (t < RADIX && histogram[t] == 0) {
                t++;
            }

            // Update statistics for next iteration
            if (t < RADIX) {
                backgroundMean = (backgroundMean * backgroundWeight + histogram[t] * t) / (backgroundWeight + histogram[t]);
                foregroundMean = (foregroundMean * foregroundWeight - histogram[t] * t) / (foregroundWeight - histogram[t]);
                backgroundWeight += histogram[t];
                foregroundWeight -= histogram[t];
            }

            t++;
        }

        return optimalThreshold;
    }

    /**
     * Applies the threshold to a grayscale pixel array to create a binary image.
     * Pixels with intensity greater than threshold become white (255),
     * others become black (0).
     *
     * @param pixels the grayscale pixel array
     * @param threshold the threshold value
     * @return BufferedImage representing the thresholded binary image
     */
    public static BufferedImage applyThreshold(int[][] pixels, int threshold) {
        int height = pixels.length;
        int width = pixels[0].length;

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int value = pixels[i][j] > threshold ? 0xFFFFFFFF : 0xFF000000;
                result.setRGB(j, i, value);
            }
        }

        return result;
    }

    /**
     * Applies the threshold to a grayscale pixel array to create a binary image.
     * Pixels with intensity greater than threshold become white (255),
     * others become black (0).
     *
     * @param pixels the grayscale pixel array (1D)
     * @param width the width of the image
     * @param height the height of the image
     * @param threshold the threshold value
     * @return BufferedImage representing the thresholded binary image
     */
    public static BufferedImage applyThreshold(int[] pixels, int width, int height, int threshold) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int value = pixels[i * width + j] > threshold ? 0xFFFFFFFF : 0xFF000000;
                result.setRGB(j, i, value);
            }
        }

        return result;
    }

    /**
     * Applies the calculated threshold to the original image.
     *
     * @param image the original image
     * @return BufferedImage representing the thresholded binary image
     */
    public BufferedImage applyThreshold(BufferedImage image) {
        int[][] pixels = getGrayscalePixels(image);
        return applyThreshold(pixels, threshold);
    }
}
