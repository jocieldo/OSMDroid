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

package rgi.geopackage.features.geometry.z;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import rgi.geopackage.features.GeometryType;

/**
 * A restricted form of MultiSurface where each Surface in the collection must
 * be of type Polygon.
 *
 * @author Luke Lambert
 * @see "http://www.geopackage.org/spec/#sfsql_intro"
 */
public class WkbMultiPolygonZ extends WkbMultiSurfaceZ<WkbPolygonZ> {
    /**
     * Constructor
     *
     * @param polygons Array of polygons
     */
    public WkbMultiPolygonZ(final WkbPolygonZ... polygons) {
        this(Arrays.asList(polygons));
    }

    /**
     * Constructor
     *
     * @param polygons Collection of polygons
     */
    public WkbMultiPolygonZ(final Collection<WkbPolygonZ> polygons) {
        super(polygons);
    }

    @Override
    public long getTypeCode() {
        return WkbGeometryZ.GeometryTypeDimensionalityBase + GeometryType.MultiPolygon.getCode();
    }

    @Override
    public String getGeometryTypeName() {
        return GeometryType.MultiPolygon.toString();
    }

    public List<WkbPolygonZ> getPolygons() {
        return this.getGeometries();
    }

    /**
     * Assumes the ByteOutputStream's byte order has been properly set
     *
     * @param byteBuffer buffer to be read from
     * @return a new WkbMultiPolygonZ
     */
    public static WkbMultiPolygonZ readWellKnownBinary(final ByteBuffer byteBuffer) {
        readWellKnownBinaryHeader(byteBuffer, GeometryTypeDimensionalityBase + GeometryType.MultiPolygon.getCode());

        final long polygonCount = Integer.toUnsignedLong(byteBuffer.getInt());

        final Collection<WkbPolygonZ> polygons = new LinkedList<>();

        for (long polygonIndex = 0; polygonIndex < polygonCount; ++polygonIndex) {
            polygons.add(WkbPolygonZ.readWellKnownBinary(byteBuffer));
        }

        return new WkbMultiPolygonZ(polygons);
    }
}
