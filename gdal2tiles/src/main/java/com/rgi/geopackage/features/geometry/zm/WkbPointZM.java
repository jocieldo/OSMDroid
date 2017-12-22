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

package com.rgi.geopackage.features.geometry.zm;

import com.rgi.geopackage.features.ByteOutputStream;
import com.rgi.geopackage.features.GeometryType;
import com.rgi.geopackage.features.geometry.xy.Envelope;

import java.nio.ByteBuffer;

/**
 * A single location in space. Each point has an X, Y and M coordinate.
 *
 * @author Luke Lambert
 * @see "http://www.geopackage.org/spec/#sfsql_intro"
 */
public class WkbPointZM extends WkbGeometryZM {
    /**
     * Constructor
     *
     * @param x x component
     * @param y y component
     * @param z z component
     * @param m m component
     */
    public WkbPointZM(final double x,
                      final double y,
                      final double z,
                      final double m) {
        this.coordinate = new CoordinateZM(x,
                y,
                z,
                m);
    }

    /**
     * Constructor
     *
     * @param coordinate coordinate to copy
     */
    public WkbPointZM(final CoordinateZM coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("Coordinate may not be null");
        }

        this.coordinate = new CoordinateZM(coordinate.getX(),
                coordinate.getY(),
                coordinate.getZ(),
                coordinate.getM());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        return this.coordinate.equals(((WkbPointZM) obj).coordinate);
    }

    @Override
    public int hashCode() {
        return this.coordinate.hashCode();
    }

    @Override
    public long getTypeCode() {
        return GeometryTypeDimensionalityBase + GeometryType.Point.getCode();
    }

    @Override
    public String getGeometryTypeName() {
        return GeometryType.Point.toString();
    }

    @Override
    public boolean isEmpty() {
        return this.coordinate.isEmpty();
    }

    @Override
    public Envelope createEnvelope() {
        return this.coordinate.createEnvelope();
    }

    @Override
    public EnvelopeZM createEnvelopeZM() {
        return this.coordinate.createEnvelope();
    }

    @Override
    public void writeWellKnownBinary(final ByteOutputStream byteOutputStream) {
        this.writeWellKnownBinaryHeader(byteOutputStream); // Checks byteOutputStream for null
        this.coordinate.writeWellKnownBinary(byteOutputStream);
    }

    /**
     * Assumes the ByteOutputStream's byte order has been properly set
     *
     * @param byteBuffer buffer to be read from
     * @return a new WkbPointZM
     */
    public static WkbPointZM readWellKnownBinary(final ByteBuffer byteBuffer) {
        readWellKnownBinaryHeader(byteBuffer, GeometryTypeDimensionalityBase + GeometryType.Point.getCode());

        return new WkbPointZM(byteBuffer.getDouble(),
                byteBuffer.getDouble(),
                byteBuffer.getDouble(),
                byteBuffer.getDouble());
    }

    public double getX() {
        return this.coordinate.getX();
    }

    public double getY() {
        return this.coordinate.getY();
    }

    public double getZ() {
        return this.coordinate.getZ();
    }

    public double getM() {
        return this.coordinate.getM();
    }

    private final CoordinateZM coordinate;
}
