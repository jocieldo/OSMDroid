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

package rgi.geopackage.features.geometry.m;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import rgi.geopackage.features.GeometryType;

/**
 * A restricted form of GeometryCollection where each Geometry in the
 * collection must be of type Point.
 *
 * @author Luke Lambert
 * @see "http://www.geopackage.org/spec/#sfsql_intro"
 */
public class WkbMultiPointM extends WkbGeometryCollectionM<WkbPointM> {
    /**
     * Constructor
     *
     * @param points Array of points
     */
    public WkbMultiPointM(final WkbPointM... points) {
        this(Arrays.asList(points));
    }

    /**
     * Constructor
     *
     * @param points Collection of points
     */
    public WkbMultiPointM(final Collection<WkbPointM> points) {
        super(points);
    }

    @Override
    public long getTypeCode() {
        return GeometryTypeDimensionalityBase + GeometryType.MultiPoint.getCode();
    }

    @Override
    public String getGeometryTypeName() {
        return GeometryType.MultiPoint.toString();
    }

    public List<WkbPointM> getPoints() {
        return this.getGeometries();
    }

    /**
     * Assumes the ByteOutputStream's byte order has been properly set
     *
     * @param byteBuffer buffer to be read from
     * @return a new WkbMultiPointM
     */
    public static WkbMultiPointM readWellKnownBinary(final ByteBuffer byteBuffer) {
        readWellKnownBinaryHeader(byteBuffer, GeometryTypeDimensionalityBase + GeometryType.MultiPoint.getCode());

        final long pointCount = Integer.toUnsignedLong(byteBuffer.getInt());

        final Collection<WkbPointM> points = new LinkedList<>();

        for (long pointIndex = 0; pointIndex < pointCount; ++pointIndex) {
            points.add(WkbPointM.readWellKnownBinary(byteBuffer));
        }

        return new WkbMultiPointM(points);
    }
}
