/* The MIT License (MIT)
 *
 * Copyright (c) 2015 Reinventing Geospatial, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rgi.common.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ImageWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Utility methods to convert back and forth between <code>byte[]</code>s and
 * {@link android.graphics.Bitmap}s
 *
 * @author Luke Lambert
 */
public class ImageUtility {
    /**
     * Converts a {@link Bitmap} into bytes using a specific image
     * writer, and image write parameters
     *
     * @param bufferedImage The {@link Bitmap} to be converted to bytes
     *                      //     * @param imageWriter         Writer responsible for the conversion
     *                      //     * @param imageWriteParameter Controls image writing parameters, e.g. transparency, compression, etc
     * @return The image as an array of bytes
     * @throws IOException Throws if image writing fails
     */
//    public static byte[] bufferedImageToBytes(final Bitmap bufferedImage, final ImageWriter imageWriter, final ImageWriteParam imageWriteParameter) throws IOException {
    public static byte[] bufferedImageToBytes(final Bitmap bufferedImage) throws IOException {
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Buffered image may not be null");
        }

//        if (imageWriter == null) {
//            throw new IllegalArgumentException("Image writer may not be null");
//        }
//
//        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//             final MemoryCacheImageOutputStream memoryCacheImage = new MemoryCacheImageOutputStream(outputStream)) {
//            imageWriter.setOutput(memoryCacheImage);
//
//            // Check for JPEG conversion.  The incoming buffered image needs to be re-written to an RGB
//            // colorspace prior to imageio performing the conversion.  This avoids a pink hue in the resulting
//            // jpeg tiles.
//            if (imageWriteParameter != null && bufferedImage.getType() == Bitmap.TYPE_4BYTE_ABGR) {
//                final Bitmap rgbImage = new Bitmap(bufferedImage.getWidth(), bufferedImage.getHeight(), Bitmap.TYPE_INT_RGB);
//                rgbImage.getGraphics().drawImage(bufferedImage, 0, 0, null);
//                imageWriter.write(null, new IIOImage(rgbImage, null, null), imageWriteParameter);
//            } else {
//                imageWriter.write(null, new IIOImage(bufferedImage, null, null), imageWriteParameter);
//            }
//
//            outputStream.flush();
//
//            return outputStream.toByteArray();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bufferedImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    /**
     * Converts a {@link Bitmap} into bytes using an image writer that
     * corresponds to the specified output format
     *
     * @param bufferedImage The {@link Bitmap} to be converted to bytes
     *                      //     * @param outputFormat  A string containing the informal name of the format.  See
     * @return The image as an array of bytes
     * @throws IOException Throws if image writing fails
     */
    public static byte[] bufferedImageToBytes(final Bitmap bufferedImage, final String outputFormat) throws IOException {
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Buffered image may not be null");
        }

        if (outputFormat == null) {
            throw new IllegalArgumentException("Output format may not be null");
        }

//        try (@SuppressWarnings("resource") final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
//            if (!ImageIO.write(bufferedImage, outputFormat, outputStream)) {
//                throw new IOException(String.format("No appropriate image writer found for format '%s'", outputFormat));
//            }
//
//            return outputStream.toByteArray();
//        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bufferedImage.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    /**
     * Converts an image as an array of bytes to a {@link Bitmap}
     *
     * @param imageData The image as an array of bytes
     * @return A {@link Bitmap}
     * @throws IOException If an error occurs in reading the image
     */
    public static Bitmap bytesToBufferedImage(final byte[] imageData) throws IOException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data may not be null");
        }

//        try (ByteArrayInputStream imageInputStream = new ByteArrayInputStream(imageData)) {
//            final Bitmap bufferedImage = ImageIO.read(imageInputStream);
//
//            if (bufferedImage == null) {
//                throw new IOException("Image data is corrupt or in an unknown format");
//            }
//
//            return bufferedImage;
//        }

        if (imageData.length != 0) {
            return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        } else {
            return null;
        }
    }

    /**
     * Writes text on a {@link Bitmap} with a red border around the image
     *
     * @param oldImage The image that is to be written on
     * @param text     The words that are to be written on the image
     * @return A {@link Bitmap} with text written on the image
     */
    public static Bitmap graffiti(final Bitmap oldImage, final String text) {
        final int width = oldImage.getWidth();
        final int height = oldImage.getHeight();

//        final Bitmap newImage = new Bitmap(width, height, oldImage.getType());
        Bitmap newImage = Bitmap.createBitmap(oldImage);
        Canvas canvas = new Canvas(newImage);

        final Paint brush = new Paint();
        brush.setColor(Color.RED);


//        brush.drawLine(0, 0, width - 1, 0);
        canvas.drawLine(0, 0, width - 1, 0, brush);
//        brush.drawLine(width - 1, 0, width - 1, height - 1);
        canvas.drawLine(width - 1, 0, width - 1, height - 1, brush);
//        brush.drawLine(width - 1, height - 1, 0, height - 1);
        canvas.drawLine(width - 1, height - 1, 0, height - 1, brush);
//        brush.drawLine(0, height - 1, 0, 0);
        canvas.drawLine(0, height - 1, 0, 0, brush);

        brush.setColor(Color.BLUE);
//        brush.setPaint(Color.BLUE);
        brush.setTextSize(20);
//        brush.setFont(new Font("Serif", Font.BOLD, 20));
        //brush.clearRect(0, 0, width, height);
        final Paint.FontMetrics fm = brush.getFontMetrics();

        final String[] parts = text.split("\n");
        for (int part = 0; part < parts.length; ++part) {
            final int x = 2;//bufferedImage.getWidth() - fm.stringWidth(text) - 5;
//            final int y = fm.getHeight();
            final int y = (int) Math.abs(fm.top - fm.bottom);
            canvas.drawText(parts[part], x, y * (part + 1), brush);
        }
        return newImage;
    }

}
