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

import com.rgi.geopackage.features.GeometryType;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A restricted form of MultiSurface where each Surface in the collection must
 * be of type Polygon.
 *
 * @author Luke Lambert
 * @see "http://www.geopackage.org/spec/#sfsql_intro"
 */
public class WkbMultiPolygon extends WkbMultiSurface<WkbPolygon> {
    /**
     * Constructor
     *
     * @param polygons Array of polygons
     */
    public WkbMultiPolygon(final WkbPolygon... polygons) {
        this(Arrays.asList(polygons));
    }

    /**
     * Constructor
     *
     * @param polygons Collection of polygons
     */
    public WkbMultiPolygon(final Collection<WkbPolygon> polygons) {
        super(polygons);
    }

    @Override
    public long getTypeCode() {
        return GeometryType.MultiPolygon.getCode();
    }

    @Override
    public String getGeometryTypeName() {
        return GeometryType.MultiPolygon.toString();
    }

    public List<WkbPolygon> getPolygons() {
        return this.getGeometries();
    }

    /**
     * Assumes the ByteOutputStream's byte order has been properly set
     *
     * @param byteBuffer buffer to be read from
     * @return a new WkbMultiPolygon
     */
    public static WkbMultiPolygon readWellKnownBinary(final ByteBuffer byteBuffer) {
        readWellKnownBinaryHeader(byteBuffer, GeometryType.MultiPolygon.getCode());

        final long polygonCount = Integer.toUnsignedLong(byteBuffer.getInt());

        final Collection<WkbPolygon> polygons = new LinkedList<>();

        for (long polygonIndex = 0; polygonIndex < polygonCount; ++polygonIndex) {
            polygons.add(WkbPolygon.readWellKnownBinary(byteBuffer));
        }

        return new WkbMultiPolygon(polygons);
    }
}
