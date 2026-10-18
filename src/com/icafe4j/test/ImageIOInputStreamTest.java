/*
 * Copyright (C) 2014-2019 WEN YU (YUWEN_66@YAHOO.COM)
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0.
 */
package com.icafe4j.test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.icafe4j.image.ImageIO;

/** Focused regression for ImageIO.read(InputStream) stream ownership. */
public final class ImageIOInputStreamTest {
    public static void main(String[] args) throws Exception {
        TrackingInputStream input = new TrackingInputStream(onePixelBmp());
        BufferedImage image = ImageIO.read(input);
        if (image == null || image.getWidth() != 1 || image.getHeight() != 1) {
            throw new AssertionError("BMP was not decoded as 1x1");
        }
        if (image.getRGB(0, 0) != 0xff112233) {
            throw new AssertionError("BMP pixel was decoded incorrectly");
        }
        if (input.closed) {
            throw new AssertionError("ImageIO.read closed the caller's stream");
        }
    }

    private static byte[] onePixelBmp() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(58);
        out.write('B'); out.write('M');
        littleEndian(out, 58, 4);
        littleEndian(out, 0, 4);
        littleEndian(out, 54, 4);
        littleEndian(out, 40, 4);
        littleEndian(out, 1, 4);
        littleEndian(out, 1, 4);
        littleEndian(out, 1, 2);
        littleEndian(out, 24, 2);
        littleEndian(out, 0, 4);
        littleEndian(out, 4, 4);
        littleEndian(out, 0, 4);
        littleEndian(out, 0, 4);
        littleEndian(out, 0, 4);
        littleEndian(out, 0, 4);
        out.write(0x33); out.write(0x22); out.write(0x11); out.write(0);
        return out.toByteArray();
    }

    private static void littleEndian(ByteArrayOutputStream out, int value, int bytes) {
        for (int i = 0; i < bytes; i++) out.write(value >>> (8 * i) & 0xff);
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;
        TrackingInputStream(byte[] bytes) { super(bytes); }
        @Override public void close() { closed = true; }
    }
}
