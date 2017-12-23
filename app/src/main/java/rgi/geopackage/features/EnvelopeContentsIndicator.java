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

package rgi.geopackage.features;

/**
 * Envelope contents indicator code (3-bit unsigned integer)
 * 0: no envelope (space saving slower indexing option), 0 bytes
 * 1: envelope is [minx, maxx, miny, maxy], 32 bytes
 * 2: envelope is [minx, maxx, miny, maxy, minz, maxz], 48 bytes
 * 3: envelope is [minx, maxx, miny, maxy, minm, maxm], 48 bytes
 * 4: envelope is [minx, maxx, miny, maxy, minz, maxz, minm, maxm], 64 bytes
 * 5-7: invalid
 *
 * @author Luke Lambert
 * @see <a href="http://www.geopackage.org/spec/#gpb_spec">http://www.geopackage.org/spec/#gpb_spec</a>
 */
public enum EnvelopeContentsIndicator {
    /**
     * 0: no envelope (space saving slower indexing option), 0 bytes
     */
    NoEnvelope(0, 0),

    /**
     * 1: envelope is [minx, maxx, miny, maxy], 32 bytes
     */
    Xy(1, 4),

    /**
     * 2: envelope is [minx, maxx, miny, maxy, minz, maxz], 48 bytes
     */
    Xyz(2, 6),

    /**
     * 3: envelope is [minx, maxx, miny, maxy, minm, maxm], 48 bytes
     */
    Xym(3, 6),

    /**
     * 4: envelope is [minx, maxx, miny, maxy, minz, maxz, minm, maxm], 64 bytes
     */
    Xyzm(4, 8);

    EnvelopeContentsIndicator(final int code,
                              final int arraySize) {
        this.code = code;
        this.arraySize = arraySize;
    }

    public int getCode() {
        return this.code;
    }

    public int getArraySize() {
        return this.arraySize;
    }

    /**
     * Converts a code to the appropriate indicator enum value
     *
     * @param code Numeric code. The only valid values are 0, 1, 2, 3, 4
     * @return The corresponding enum value
     */
    public static EnvelopeContentsIndicator fromCode(final int code) {
        switch (code) {
            case 0:
                return NoEnvelope;
            case 1:
                return Xy;
            case 2:
                return Xyz;
            case 3:
                return Xym;
            case 4:
                return Xyzm;

            default:
                throw new IllegalArgumentException("Invalid envelope contents indicator code");
        }
    }

    private final int code;
    private final int arraySize;
}
