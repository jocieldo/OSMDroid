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

package rgi.store.tiles.geopackage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rgi.common.BoundingBox;
import rgi.common.Dimensions;
import rgi.common.Range;
import rgi.common.coordinate.Coordinate;
import rgi.common.coordinate.CoordinateReferenceSystem;
import rgi.common.coordinate.CrsCoordinate;
import rgi.common.coordinate.referencesystem.profile.CrsProfile;
import rgi.common.coordinate.referencesystem.profile.CrsProfileFactory;
import rgi.common.tile.TileOrigin;
import rgi.common.tile.scheme.TileMatrixDimensions;
import rgi.common.tile.scheme.TileScheme;
import rgi.common.util.ImageUtility;
import rgi.geopackage.GeoPackage;
import rgi.geopackage.core.SpatialReferenceSystem;
import rgi.geopackage.tiles.GeoPackageTiles;
import rgi.geopackage.tiles.Tile;
import rgi.geopackage.tiles.TileCoordinate;
import rgi.geopackage.tiles.TileMatrix;
import rgi.geopackage.tiles.TileMatrixSet;
import rgi.geopackage.tiles.TileSet;
import rgi.geopackage.verification.Severity;
import rgi.geopackage.verification.VerificationLevel;
import rgi.store.tiles.TileHandle;
import rgi.store.tiles.TileStoreException;
import rgi.store.tiles.TileStoreReader;

/**
 * @author Luke Lambert
 */
public class GeoPackageReader implements TileStoreReader {
    /**
     * @param geoPackageFile   Handle to a new or existing GeoPackage file
     * @param tileSetTableName Name for the new tile set's table in the GeoPackage database
     * @throws TileStoreException if there's an error in constructing the underlying tile store implementation
     */
    public GeoPackageReader(final File geoPackageFile, final String tileSetTableName) throws TileStoreException {
        this(geoPackageFile, tileSetTableName, VerificationLevel.Fast);
    }

    /**
     * @param geoPackageFile    Handle to a new or existing GeoPackage file
     * @param tileSetTableName  Name for the new tile set's table in the GeoPackage database
     * @param verificationLevel Controls the level of verification testing performed on this
     *                          GeoPackage.  If verificationLevel is not None
     *                          {@link GeoPackage#verify()} is called automatically and will throw if
     *                          there are any conformance violations with the severity
     *                          {@link Severity#Error}.  Throwing from this method means
     *                          that it won't be possible to instantiate a GeoPackage object
     *                          based on an SQLite "GeoPackage" file with severe errors.
     * @throws TileStoreException if there's an error in constructing the underlying tile store implementation
     */
    public GeoPackageReader(final File geoPackageFile, final String tileSetTableName, final VerificationLevel verificationLevel) throws TileStoreException {
        if (geoPackageFile == null) {
            throw new IllegalArgumentException("GeoPackage file may not be null");
        }

        if (tileSetTableName == null) {
            throw new IllegalArgumentException("Tile set may not be null or empty");
        }

        try {
            this.geoPackage = new GeoPackage(geoPackageFile, verificationLevel, GeoPackage.OpenMode.Open);
        } catch (final Exception ex) {
            throw new TileStoreException(ex);
        }

        try {
            this.tileSet = this.geoPackage.tiles().getTileSet(tileSetTableName);

            if (this.tileSet == null) {
                throw new IllegalArgumentException("Table name does not specify a valid GeoPackage tile set");
            }

            final SpatialReferenceSystem srs = this.geoPackage.core().getSpatialReferenceSystem(this.tileSet.getSpatialReferenceSystemIdentifier());

            if (srs == null) {
                throw new IllegalArgumentException("SRS may not be null");
            }

            this.crsProfile = CrsProfileFactory.create(srs.getOrganization(), srs.getOrganizationSrsId());

            this.zoomLevels = this.geoPackage.tiles().getTileZoomLevels(this.tileSet);

            this.tileMatrixSet = this.geoPackage.tiles().getTileMatrixSet(this.tileSet);

            this.tileMatrices = this.geoPackage.tiles()
                    .getTileMatrices(this.tileSet)
                    .stream()
                    .collect(Collectors.toMap(TileMatrix::getZoomLevel,
                            tileMatrix -> tileMatrix));

            this.tileScheme = new TileScheme() {
                @Override
                public TileMatrixDimensions dimensions(final int zoomLevel) {
                    if (GeoPackageReader.this.tileMatrices.containsKey(zoomLevel)) {
                        final TileMatrix tileMatrix = GeoPackageReader.this.tileMatrices.get(zoomLevel);
                        return new TileMatrixDimensions(tileMatrix.getMatrixWidth(), tileMatrix.getMatrixHeight());
                    }

                    throw new IllegalArgumentException(String.format("Zoom level must be in the range %s",
                            new Range<>(GeoPackageReader.this.tileMatrices.keySet(), Integer::compare)));
                }

                @Override
                public Collection<Integer> getZoomLevels() {
                    try {
                        return GeoPackageReader.this.geoPackage.tiles().getTileZoomLevels(GeoPackageReader.this.tileSet);
                    } catch (final SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };
        } catch (final Exception ex) {
            try {
                this.geoPackage.close();
            } catch (final SQLException ex1) {
                throw new TileStoreException(ex1);
            }
            throw new TileStoreException(ex);
        }
    }

    @Override
    public void close() throws SQLException {
        this.geoPackage.close();
    }

    @Override
    public BoundingBox getBounds() throws TileStoreException {
        return this.tileMatrixSet.getBoundingBox();
    }

    @Override
    public long countTiles() throws TileStoreException {
        // TODO lazy precalculation ?
        try {
            return this.geoPackage.core().getRowCount(this.tileSet);
        } catch (final SQLException ex) {
            throw new TileStoreException(ex);
        }
    }

    @Override
    public long getByteSize() throws TileStoreException {
        // TODO lazy precalculation ?
        return this.geoPackage.getFile().getTotalSpace();
    }

    @Override
    public Bitmap getTile(final int column, final int row, final int zoomLevel) throws TileStoreException {
        try {
            return getImage(this.geoPackage
                    .tiles()
                    .getTile(this.tileSet,
                            column,
                            row,
                            zoomLevel));
        } catch (final SQLException ex) {
            throw new TileStoreException(ex);
        }
    }

    @Override
    public Bitmap getTile(final CrsCoordinate coordinate, final int zoomLevel) throws TileStoreException {
        if (coordinate == null) {
            throw new IllegalArgumentException("Coordinate may not be null");
        }

        if (!coordinate.getCoordinateReferenceSystem().equals(this.getCoordinateReferenceSystem())) {
            throw new IllegalArgumentException("Coordinate's coordinate reference system does not match the tile store's coordinate reference system");
        }

        try {
            return getImage(this.geoPackage
                    .tiles()
                    .getTile(this.tileSet,
                            coordinate,
                            this.crsProfile.getPrecision(),
                            zoomLevel));
        } catch (final IllegalArgumentException ignored) // This is to catch an IAE if the crsCoordinate requested is outside the bounds of the GeoPackage tiles BoundingBox
        {
            return null;
        } catch (final SQLException ex) {
            throw new TileStoreException(ex);
        }
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return this.crsProfile.getCoordinateReferenceSystem();
    }

    @Override
    public Set<Integer> getZoomLevels() throws TileStoreException {
        return Collections.unmodifiableSet(this.zoomLevels);
    }

    @Override
    public Stream<TileHandle> stream() throws TileStoreException {
        try {
            return this.geoPackage
                    .tiles()
                    .getTiles(this.tileSet)
                    .map(tileCoordinate -> this.getTileHandle(tileCoordinate.getZoomLevel(),
                            tileCoordinate.getColumn(),
                            tileCoordinate.getRow()));
        } catch (final SQLException ex) {
            throw new TileStoreException(ex);
        }
    }

    @Override
    public Stream<TileHandle> stream(final int zoomLevel) throws TileStoreException {
        try {
            return this.geoPackage
                    .tiles()
                    .getTiles(this.tileSet, zoomLevel)
                    .map(tileCoordinate -> this.getTileHandle(zoomLevel,
                            tileCoordinate.getX(),
                            tileCoordinate.getY()));
        } catch (final SQLException ex) {
            throw new TileStoreException(ex);
        }
    }

    @Override
    public String getImageType() throws TileStoreException {
        try {
            final TileCoordinate coordinate = this.geoPackage.tiles().getTiles(this.tileSet).findFirst().orElse(null);

            if (coordinate != null) {
                final Tile tile = this.geoPackage.tiles().getTile(this.tileSet, coordinate.getColumn(), coordinate.getRow(), coordinate.getZoomLevel());
                if (tile != null) {
                    final byte[] imageData = tile.getImageData();

                    BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

//                    try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageData)) {
//                        try (final ImageInputStream imageInputStream = ImageIO.createImageInputStream(byteArrayInputStream)) {
//                            final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
//
//                            while (imageReaders.hasNext()) {
//                                final ImageReader imageReader = imageReaders.next();
//
//                                final String[] names = imageReader.getOriginatingProvider().getFormatNames();
//                                if (names != null && names.length > 0) {
//                                    return names[0];
//                                }
//                            }
//                        }
//                    }
                    return "png";
                }
            }

            return null;
        } catch (final SQLException ex) {
            throw new TileStoreException(ex);
        }


    }

    @Override
    public Dimensions<Integer> getImageDimensions() throws TileStoreException {
        final TileHandle tile = this.stream().findFirst().orElse(null);

        if (tile != null) {
            final Bitmap image = tile.getImage();
            return new Dimensions<>(image.getWidth(), image.getHeight());
        }

        return null;
    }

    @Override
    public String getName() {
        return String.format("%s-%s",
                this.geoPackage.getFile().getName(),
                this.tileSet.getIdentifier());
    }

    @Override
    public TileScheme getTileScheme() {
        return this.tileScheme;
    }

    @Override
    public TileOrigin getTileOrigin() {
        return GeoPackageTiles.Origin;
    }

    private static Bitmap getImage(final Tile tile) throws TileStoreException {
        if (tile == null) {
            return null;
        }

        try {
            return ImageUtility.bytesToBufferedImage(tile.getImageData());
        } catch (final IOException ex) {
            throw new TileStoreException(ex);
        }
    }

    private TileHandle getTileHandle(final int zoomLevel, final int column, final int row) {
        final TileMatrix tileMatrix = GeoPackageReader.this.tileMatrices.get(zoomLevel);
        final TileMatrixDimensions matrix = new TileMatrixDimensions(tileMatrix.getMatrixWidth(), tileMatrix.getMatrixHeight());

        return new TileHandle() {
            @Override
            public int getZoomLevel() {
                return zoomLevel;
            }

            @Override
            public int getColumn() {
                return column;
            }

            @Override
            public int getRow() {
                return row;
            }

            @Override
            public TileMatrixDimensions getMatrix() {
                return matrix;
            }

            @Override
            public CrsCoordinate getCrsCoordinate() throws TileStoreException {
                return GeoPackageReader.this
                        .crsProfile
                        .tileToCrsCoordinate(column,
                                row,
                                GeoPackageReader.this.getBounds(),
                                matrix,
                                GeoPackageTiles.Origin);
            }

            @Override
            public CrsCoordinate getCrsCoordinate(final TileOrigin corner) throws TileStoreException {
                return GeoPackageReader.this
                        .crsProfile
                        .tileToCrsCoordinate(column + corner.getHorizontal(),     // same as: column - (GeoPackageTiles.Origin.getVertical() - corner.getHorizontal()) because GeoPackageTiles.Origin.getVertical() is always 0
                                row + (1 - corner.getVertical()),
                                GeoPackageReader.this.getBounds(),
                                matrix,
                                GeoPackageTiles.Origin);
            }

            @Override
            public BoundingBox getBounds() throws TileStoreException {
                final Coordinate<Double> upperLeft = this.getCrsCoordinate(TileOrigin.UpperLeft);
                final Coordinate<Double> lowerRight = this.getCrsCoordinate(TileOrigin.LowerRight);

                return new BoundingBox(upperLeft.getX(),
                        lowerRight.getY(),
                        lowerRight.getX(),
                        upperLeft.getY());
            }

            @Override
            public Bitmap getImage() throws TileStoreException {
                return GeoPackageReader.this.getTile(column, row, zoomLevel);
            }
        };
    }

    private final GeoPackage geoPackage;
    private final TileSet tileSet;
    private final CrsProfile crsProfile;
    private final TileScheme tileScheme;
    private final Set<Integer> zoomLevels;
    private final Map<Integer, TileMatrix> tileMatrices;
    private final TileMatrixSet tileMatrixSet;

}
