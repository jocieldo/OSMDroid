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

package com.rgi.geopackage.features.geometry.xy;

import com.rgi.geopackage.features.ByteOutputStream;
import com.rgi.geopackage.features.GeometryType;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

/**
 * A Curve that connects two or more points in space.
 *
 * @author Luke Lambert
 * @see "http://www.geopackage.org/spec/#sfsql_intro"
 */
public class WkbLineString extends WkbCurve {
    /**
     * Constructor
     *
     * @param coordinates Array of coordinates
     */
    public WkbLineString(final Coordinate... coordinates) {
        this(new LinearRing(coordinates));
    }

    /**
     * Constructor
     *
     * @param coordinates Collection of coordinates
     */
    public WkbLineString(final Collection<Coordinate> coordinates) {
        this(new LinearRing(coordinates));
    }

    private WkbLineString(final LinearRing linearString) {
        this.linearString = linearString;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        return this.linearString.equals(((WkbLineString) obj).linearString);
    }

    @Override
    public int hashCode() {
        return this.linearString.hashCode();
    }

    @Override
    public long getTypeCode() {
        return GeometryType.LineString.getCode();
    }

    @Override
    public String getGeometryTypeName() {
        return GeometryType.LineString.toString();
    }

    @Override
    public boolean isEmpty() {
        return this.linearString.isEmpty();
    }

    @Override
    public Envelope createEnvelope() {
        return this.linearString.createEnvelope();
    }

    @Override
    public void writeWellKnownBinary(final ByteOutputStream byteOutputStream) {
        this.writeWellKnownBinaryHeader(byteOutputStream); // Checks byteOutputStream for null
        this.linearString.writeWellKnownBinary(byteOutputStream);
    }

    /**
     * @return a {@link List} of coordinates
     */
    public List<Coordinate> getCoordinates() {
        return this.linearString.getCoordinates();
    }

    /**
     * Assumes the ByteOutputStream's byte order has been properly set
     *
     * @param byteBuffer buffer to be read from
     * @return a new WkbLineString
     */
    public static WkbLineString readWellKnownBinary(final ByteBuffer byteBuffer) {
        readWellKnownBinaryHeader(byteBuffer, GeometryType.LineString.getCode());

        return new WkbLineString(LinearRing.readWellKnownBinary(byteBuffer));
    }

    private final LinearRing linearString;
}
