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

package rgi.geopackage.tiles;

import rgi.geopackage.core.Content;

/**
 * @author Luke Lambert
 */
public class TileSet extends Content {
    /**
     * Constructor
     *
     * @param tableName                        The name of the tiles, feature, or extension specific content table
     * @param identifier                       A human-readable identifier (e.g. short name) for the tableName content
     * @param description                      A human-readable description for the tableName content
     * @param lastChange                       Date value in ISO 8601 format as defined by the strftime function %Y-%m-%dT%H:%M:%fZ format string applied to the current time
     * @param minimumX                         Bounding box minimum easting or longitude for all content
     * @param minimumY                         Bounding box maximum easting or longitude for all content
     * @param maximumX                         Bounding box minimum northing or latitude for all content
     * @param maximumY                         Bounding box maximum northing or latitude for all content
     * @param spatialReferenceSystemIdentifier
     */
    protected TileSet(final String tableName,
                      final String identifier,
                      final String description,
                      final String lastChange,
                      final Double minimumX,
                      final Double minimumY,
                      final Double maximumX,
                      final Double maximumY,
                      final int spatialReferenceSystemIdentifier) {
        super(tableName,
                TileSet.TileContentType,
                identifier,
                description,
                lastChange,
                minimumX,
                minimumY,
                maximumX,
                maximumY,
                spatialReferenceSystemIdentifier);
    }

    /**
     * According to the OGC specifications, the data type of all Tiles table is
     * "tiles" http://www.geopackage.org/spec/#tiles
     */
    public static final String TileContentType = "tiles";
}
