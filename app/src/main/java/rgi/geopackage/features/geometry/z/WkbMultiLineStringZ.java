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
 * A restricted form of MultiCurve where each Curve in the collection must be
 * of type LineString.
 *
 * @author Luke Lambert
 * @see "http://www.geopackage.org/spec/#sfsql_intro"
 */
public class WkbMultiLineStringZ extends WkbMultiCurveZ<WkbLineStringZ> {
    /**
     * Constructor
     *
     * @param lineStrings Array of line strings
     */
    public WkbMultiLineStringZ(final WkbLineStringZ... lineStrings) {
        this(Arrays.asList(lineStrings));
    }

    /**
     * Constructor
     *
     * @param lineStrings Collection of line strings
     */
    public WkbMultiLineStringZ(final Collection<WkbLineStringZ> lineStrings) {
        super(lineStrings);
    }

    @Override
    public long getTypeCode() {
        return WkbGeometryZ.GeometryTypeDimensionalityBase + GeometryType.MultiLineString.getCode();
    }

    @Override
    public String getGeometryTypeName() {
        return GeometryType.MultiLineString.toString();
    }

    public List<WkbLineStringZ> getLineStrings() {
        return this.getGeometries();
    }

    /**
     * Assumes the ByteOutputStream's byte order has been properly set
     *
     * @param byteBuffer buffer to be read from
     * @return a new WkbMultiLineStringZ
     */
    public static WkbMultiLineStringZ readWellKnownBinary(final ByteBuffer byteBuffer) {
        readWellKnownBinaryHeader(byteBuffer, GeometryTypeDimensionalityBase + GeometryType.MultiLineString.getCode());

        final long lineStringCount = Integer.toUnsignedLong(byteBuffer.getInt());

        final Collection<WkbLineStringZ> lineStrings = new LinkedList<>();

        for (long lineStringIndex = 0; lineStringIndex < lineStringCount; ++lineStringIndex) {
            lineStrings.add(WkbLineStringZ.readWellKnownBinary(byteBuffer));
        }

        return new WkbMultiLineStringZ(lineStrings);
    }
}
