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

package com.rgi.geopackage.features.geometry.z;

import java.util.Collection;

/**
 * A restricted form of GeometryCollection where each Geometry in the
 * collection must be of type Curve.
 *
 * @param <T> {@link WkbCurveZ} subclass
 * @author Luke Lambert
 * @see "http://www.geopackage.org/spec/#sfsql_intro"
 */
@SuppressWarnings("AbstractClassExtendsConcreteClass")
public abstract class WkbMultiCurveZ<T extends WkbCurveZ> extends WkbGeometryCollectionZ<T> {
    /**
     * Constructor
     *
     * @param curves Collection of curves
     */
    protected WkbMultiCurveZ(final Collection<T> curves) {
        super(curves);
    }
}
