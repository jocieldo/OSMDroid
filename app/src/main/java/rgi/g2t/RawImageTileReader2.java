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

package rgi.g2t;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import other.MimeType;
import other.MimeTypeParseException;
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
import rgi.common.tile.scheme.ZoomTimesTwo;
import rgi.store.tiles.TileHandle;
import rgi.store.tiles.TileStoreException;
import rgi.store.tiles.TileStoreReader;
import utility.GdalUtility;

/**
 * TileStoreReader implementation for GDAL-readable image files
 *
 * @author Steven D. Lander
 * @author Luke D. Lambert
 */
public class RawImageTileReader2 implements TileStoreReader {
//    /**
//     * Constructor
//     *
//     * @param rawImage    A raster image
//     * @param tileSize    A {@link Dimensions} that describes what an individual tile
//     *                    looks like
//     * @param noDataColor The {@link Color} of the NODATA fields within the raster image
//     * @throws TileStoreException Thrown when GDAL could not get the correct
//     *                            {@link CoordinateReferenceSystem} of the input raster OR if
//     */
//    public RawImageTileReader(File rawImage, Dimensions<Integer> tileSize, Color noDataColor) throws TileStoreException {
//        this(rawImage, tileSize, noDataColor, null);
//    }

    private String cacheName;

    public RawImageTileReader2(String cacheName, Bitmap rawImage, Dimensions<Integer> tileSize, CoordinateReferenceSystem coordinateReferenceSystem, BoundingBox boundingBox) throws TileStoreException {
//        if (rawImage == null || !rawImage.canRead()) {
//            throw new IllegalArgumentException("Raw image may not be null, and must represent a valid file on disk.");
//        }
        this.cacheName = cacheName;
        if (tileSize == null) {
            throw new IllegalArgumentException("Tile size may not be null.");
        }

        this.rawImage = rawImage;
        this.tileSize = tileSize;

        // TODO check noDataColor for null when the feature is implemented
        // this.noDataColor = noDataColor;

//        this.dataset = dataset;

        //            if (this.dataset.GetRasterCount() == 0) {
//                throw new IllegalArgumentException("Input file has no raster bands");
//            }
//
//            if (this.dataset.GetRasterBand(1).GetColorTable() != null) {
//                System.out.println("expand this raster to RGB/RGBA"); // TODO: make a temporary vrt with gdal_translate to expand this to RGB/RGBA
//            }

        // We cannot tile an image with no geo referencing information
//            if (!GdalUtility.hasGeoReference(this.dataset)) {
//                throw new IllegalArgumentException("Input raster image has no georeference.");
//            }

//            this.coordinateReferenceSystem = GdalUtility.getCoordinateReferenceSystem(GdalUtility.getSpatialReference(this.dataset));
        this.coordinateReferenceSystem = coordinateReferenceSystem;

        if (this.coordinateReferenceSystem == null) {
            throw new IllegalArgumentException("Image file is not in a recognized coordinate reference system");
        }

        this.profile = CrsProfileFactory.create(this.coordinateReferenceSystem);

        this.tileScheme = new ZoomTimesTwo(0, MAX_ZOOM_LEVEL, 1, 1);    // Use absolute tile numbering

        final BoundingBox datasetBounds = boundingBox;

        this.tileRanges = GdalUtility.calculateTileRanges(this.tileScheme, datasetBounds, this.profile.getBounds(), this.profile, RawImageTileReader2.Origin);

        //final int minimumZoom = GdalUtility.getMinimalZoom(this.dataset, this.tileRanges, Origin, this.tileScheme, tileSize);
//        final int minimumZoom = GdalUtility.getMinimalZoom(this.tileRanges);
//            int maximumZoom = GdalUtility.getMaximalZoom(this.dataset, this.tileRanges, Origin, this.tileScheme, tileSize);
        int minimumZoom = 15;
        int maximumZoom = 17;

        // The bounds of the dataset is **almost never** the bounds of the
        // data.  The bounds of the dataset fit inside the bounds of the
        // data because the bounds of the data must align to the tile grid.
        // The minimum zoom level is selected such that the entire dataset
        // fits inside a single tile.  that single tile is the minimum data
        // bounds.

        final Coordinate<Integer> minimumTile = this.tileRanges.get(minimumZoom).getMinimum();  // The minimum and maximum for the range returned from tileRanges.get() should be identical (it's a single tile)

        this.dataBounds = this.getTileBoundingBox(minimumTile.getX(), minimumTile.getY(), this.tileScheme.dimensions(minimumZoom));

        this.zoomLevels = IntStream.rangeClosed(minimumZoom, maximumZoom).boxed().collect(Collectors.toSet());

        this.tileCount = IntStream.rangeClosed(minimumZoom, maximumZoom).map(zoomLevel -> {
            final Range<Coordinate<Integer>> range = this.tileRanges.get(zoomLevel);
            return (range.getMaximum().getX() - range.getMinimum().getX() + 1) * (range.getMinimum().getY() - range.getMaximum().getY() + 1);
        }).sum();

        this.cachedTiles = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void close() throws TileStoreException {
        // Remove temporary reprojected file
//        if (this.dataset != null) {
//            final Iterable<String> files = this.dataset.GetFileList();
//            this.dataset.delete();
//            for (final String file : files) {
//                if (file.startsWith((System.getProperty("java.io.tmpdir")))) {
//                    try {
//                        Files.delete(Paths.get(file));
//                    } catch (final IOException e) {
//                        throw new TileStoreException(e);
//                    }
//                }
//            }
//        }
        // Remove final temporary cached tile(s)
        try {
            this.cachedTiles.values().stream().forEach(tile -> {
                tile.delete();
//                    Files.delete(tile);
            });
        } catch (final RuntimeException e) {
            // Catch the exception when a tile cannot be deleted and throw a new TileStoreException
            throw new TileStoreException(e);
        }
    }

    @Override
    public BoundingBox getBounds() {
        return this.dataBounds;
    }

    @Override
    public long countTiles() {
        return this.tileCount;
    }

    @Override
    public long getByteSize() {
        return rawImage.getByteCount();
//        return this.rawImage.length();
    }

    @Override
    public Bitmap getTile(final int column, final int row, final int zoomLevel) throws TileStoreException {
        throw new TileStoreException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public Bitmap getTile(final CrsCoordinate coordinate, final int zoomLevel) throws TileStoreException {
        throw new TileStoreException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public Set<Integer> getZoomLevels() {
        // removes the possibility of the collection being modified during return
        return Collections.unmodifiableSet(this.zoomLevels);
    }

    @Override
    public Stream<TileHandle> stream() throws TileStoreException {
        final List<TileHandle> tileHandles = new ArrayList<>();

        final Range<Integer> zoomRange = new Range<>(this.zoomLevels, Integer::compare);

        // Should always start with the lowest-integer-zoom-level that has only one tile
        final Range<Coordinate<Integer>> zoomInfo = this.tileRanges.get(zoomRange.getMinimum());

        // Get the coordinate information
        final Coordinate<Integer> topLeftCoordinate = zoomInfo.getMinimum();
        final Coordinate<Integer> bottomRightCoordinate = zoomInfo.getMaximum();

        // Parse each coordinate into min/max tiles for X/Y
        final int zoomMinXTile = topLeftCoordinate.getX();
        final int zoomMaxXTile = bottomRightCoordinate.getX();
        final int zoomMinYTile = bottomRightCoordinate.getY();
        final int zoomMaxYTile = topLeftCoordinate.getY();

        //Make tiles for each tile at the min zoom level
        for (int x = zoomMinXTile; x <= zoomMaxXTile; x++) {
            for (int y = zoomMinYTile; y <= zoomMaxYTile; y++) {
                this.makeTiles(tileHandles, new RawImageTileReader2.RawImageTileHandle(zoomRange.getMinimum(), x, y), zoomRange.getMaximum());
            }
        }

        // Sort the tile handles so that they decrement.  This ensures all base level tiles
        // are generated before the overview tiles
        tileHandles.sort((o1, o2) -> Integer.compare(o2.getZoomLevel(), o1.getZoomLevel()));
        return tileHandles.stream();
    }

    private boolean tileIntersectsData(final TileHandle tile) {
        final int zoom = tile.getZoomLevel();
        final int column = tile.getColumn();
        final int row = tile.getRow();

        final Range<Coordinate<Integer>> zoomRange = this.tileRanges.get(zoom);

        return column >= zoomRange.getMinimum().getX() &&
                column <= zoomRange.getMaximum().getX() &&
                row >= zoomRange.getMaximum().getY() &&
                row <= zoomRange.getMinimum().getY();
    }

    private void makeTiles(final List<TileHandle> tileHandles, final TileHandle tile, final int maxZoom) throws TileStoreException {
        if (!this.tileIntersectsData(tile)) {
            // Do nothing if the tile does not intersect with the data bounding box
            return;
        }
        if (tile.getZoomLevel() == maxZoom) {
            // tell the RawImageTileHandle this is a special case: a gdalImage
            tileHandles.add(new RawImageTileReader2.RawImageTileHandle(tile.getZoomLevel(), tile.getColumn(), tile.getRow(), true));
        } else {
            // calculate all the tiles below this current one
            final int zoomBelow = tile.getZoomLevel() + 1;
            // Shift values instead of multiplying by 2 for possible performance improvement
            final int zoomColumnBelow = tile.getColumn() << 1;
            final int zoomRowBelow = tile.getRow() << 1;

            // create handles for below tiles
            final TileHandle belowOriginTile = new RawImageTileReader2.RawImageTileHandle(zoomBelow,
                    zoomColumnBelow,
                    zoomRowBelow);
            final TileHandle belowColumnShiftedTile = new RawImageTileReader2.RawImageTileHandle(zoomBelow,
                    zoomColumnBelow + 1,
                    zoomRowBelow);
            final TileHandle belowRowShiftedTile = new RawImageTileReader2.RawImageTileHandle(zoomBelow,
                    zoomColumnBelow,
                    zoomRowBelow + 1);
            final TileHandle belowBothShiftedTile = new RawImageTileReader2.RawImageTileHandle(zoomBelow,
                    zoomColumnBelow + 1,
                    zoomRowBelow + 1);

            // recurse
            this.makeTiles(tileHandles, belowOriginTile, maxZoom);
            this.makeTiles(tileHandles, belowColumnShiftedTile, maxZoom);
            this.makeTiles(tileHandles, belowRowShiftedTile, maxZoom);
            this.makeTiles(tileHandles, belowBothShiftedTile, maxZoom);

            // finally, add this current tile
            tileHandles.add(tile);
        }
    }

    @Override
    public Stream<TileHandle> stream(final int zoomLevel) throws TileStoreException {
        final Range<Coordinate<Integer>> zoomInfo = this.tileRanges.get(zoomLevel);

        // Get the coordinate information
        final Coordinate<Integer> topLeftCoordinate = zoomInfo.getMinimum();
        final Coordinate<Integer> bottomRightCoordinate = zoomInfo.getMaximum();

        // Parse each coordinate into min/max tiles for X/Y
        final int zoomMinXTile = topLeftCoordinate.getX();
        final int zoomMaxXTile = bottomRightCoordinate.getX();
        final int zoomMinYTile = bottomRightCoordinate.getY();
        final int zoomMaxYTile = topLeftCoordinate.getY();

        // Create a tile handle list that we can append to
        final Collection<TileHandle> tileHandles = new ArrayList<>();

        for (int tileY = zoomMaxYTile; tileY >= zoomMinYTile; --tileY) {
            for (int tileX = zoomMinXTile; tileX <= zoomMaxXTile; ++tileX) {
                tileHandles.add(new RawImageTileReader2.RawImageTileHandle(zoomLevel, tileX, tileY));
            }
        }
        // Return the entire tile handle list as a stream
        return tileHandles.stream();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return this.coordinateReferenceSystem;
    }

    @Override
    public String getName() {
        return cacheName;
//        return FileUtility.nameWithoutExtension(this.rawImage);
    }

    @Override
    public String getImageType() throws TileStoreException {
        try {
            MimeType mimeType = new MimeType("image/png");
//            final MimeType mimeType = new MimeType(Files.probeContentType(this.rawImage.toPath()));

            if (mimeType.getPrimaryType().toLowerCase().equals("image")) {
                return mimeType.getSubType();
            }

            return null;
        } catch (final MimeTypeParseException ex) {
            throw new TileStoreException(ex);
        }
    }

    @Override
    public Dimensions<Integer> getImageDimensions() throws TileStoreException {
        return this.tileSize;
    }

    @Override
    public TileScheme getTileScheme() throws TileStoreException {
        return this.tileScheme;
    }

    @Override
    public TileOrigin getTileOrigin() {
        return Origin;
    }

    private class RawImageTileHandle implements TileHandle {
        private final TileMatrixDimensions matrix;

        private boolean gotImage;
        private boolean gdalImage;
        private File cachedImageLocation;
        private Bitmap image;

        private final int zoomLevel;
        private final int column;
        private final int row;

        RawImageTileHandle(final int zoom, final int column, final int row) throws TileStoreException {
            this.zoomLevel = zoom;
            this.column = column;
            this.row = row;
            this.matrix = RawImageTileReader2.this.getTileScheme().dimensions(this.zoomLevel);
        }

        RawImageTileHandle(final int zoom, final int column, final int row, final File cachedImageLocation) throws TileStoreException {
            this.zoomLevel = zoom;
            this.column = column;
            this.row = row;
            this.matrix = RawImageTileReader2.this.getTileScheme().dimensions(this.zoomLevel);
            this.cachedImageLocation = cachedImageLocation;
        }

        RawImageTileHandle(final int zoom, final int column, final int row, final boolean gdalImage) throws TileStoreException {
            this.zoomLevel = zoom;
            this.column = column;
            this.row = row;
            this.matrix = RawImageTileReader2.this.getTileScheme().dimensions(this.zoomLevel);
            this.gdalImage = gdalImage;
        }

        @Override
        public int getZoomLevel() {
            return this.zoomLevel;
        }

        @Override
        public int getColumn() {
            return this.column;
        }

        @Override
        public int getRow() {
            return this.row;
        }

        public File getCachedImagePath() {
            return this.cachedImageLocation;
        }

        @Override
        public TileMatrixDimensions getMatrix() throws TileStoreException {
            return this.matrix;
        }

        @Override
        public CrsCoordinate getCrsCoordinate() throws TileStoreException {
            return RawImageTileReader2.this.tileToCrsCoordinate(this.column,
                    this.row,
                    this.matrix,
                    RawImageTileReader2.this.getTileOrigin());
        }

        @Override
        public CrsCoordinate getCrsCoordinate(final TileOrigin corner) throws TileStoreException {
            return RawImageTileReader2.this.tileToCrsCoordinate(this.column,
                    this.row,
                    this.matrix,
                    corner);
        }

        @Override
        public BoundingBox getBounds() throws TileStoreException {
            return RawImageTileReader2.this.getTileBoundingBox(this.column,
                    this.row,
                    this.matrix);
        }

        @Override
        public Bitmap getImage() throws TileStoreException {
            if (this.gotImage) {
                return this.image;
            }

//            if (this.gdalImage) {
//                // Build the parameters for GDAL read raster call
//                final GdalUtility.GdalRasterParameters params = GdalUtility.getGdalRasterParameters(RawImageTileReader.this.dataset.GetGeoTransform(), this.getBounds(), RawImageTileReader.this.tileSize, RawImageTileReader.this.dataset);
//                try {
//                    // Read image data directly from the raster
//                    final byte[] imageData = GdalUtility.readRaster(params, RawImageTileReader.this.dataset);
//
//                    // TODO: logic goes here in the case that the querysize == tile size (gdalconstConstants.GRA_NearestNeighbour) (write directly)
//                    final Dataset querySizeImageCanvas = GdalUtility.writeRaster(params, imageData, RawImageTileReader.this.dataset.GetRasterCount());
//
//                    try {
//                        // Scale each band of tileDataInMemory down to the tile size (down from the query size)
//                        final Dataset tileDataInMemory = GdalUtility.scaleQueryToTileSize(querySizeImageCanvas, RawImageTileReader.this.tileSize);
//
//                        this.image = GdalUtility.convert(tileDataInMemory);
//
//                        // Write this image to disk for later overview generation
//                        final Path baseTilePath = this.writeTempTile(this.image);
//
//                        // Add the image path to the reader map
//                        RawImageTileReader.this.cachedTiles.put(this.tileKey(this.zoomLevel, this.column, this.row), baseTilePath);
//
//                        // Clean up dataset
//                        tileDataInMemory.delete();
//                        this.gotImage = true;
//
//                        return this.image;
//                    } catch (final TilingException | IOException ex) {
//                        throw new TileStoreException(ex);
//                    } finally {
//                        querySizeImageCanvas.delete();
//                    }
//                } catch (final TilingException ex) {
//                    throw new TileStoreException(ex);
//                } catch (final IOException ignored) {
//                    this.gotImage = true;
//                    return this.createTransparentImage();
//                }
//            }
            return this.generateScaledTileFromChildren();
        }

        private File writeTempTile(final Bitmap tileImage) throws IOException {
            final File baseTileFile = File.createTempFile("baseTile" + this.zoomLevel + this.column + this.row, ".png");

            FileOutputStream fileOutputStream = new FileOutputStream(baseTileFile);
            tileImage.compress(Bitmap.CompressFormat.PNG, 96, fileOutputStream);
            return baseTileFile;
//            try (final ImageOutputStream fileOutputStream = ImageIO.createImageOutputStream(baseTileFile)) {
//                ImageIO.write(tileImage, "png", baseTilePath.toFile());
//            }
//            return baseTilePath;
        }

        private Bitmap createTransparentImage() {
            final int tileWidth = RawImageTileReader2.this.tileSize.getWidth();
            final int tileHeight = RawImageTileReader2.this.tileSize.getHeight();

            final Bitmap transparentImage = Bitmap.createBitmap(tileWidth, tileHeight, Bitmap.Config.ARGB_8888);

            // Return the transparent tile
//            return transparentImage;
            return getTransparentBitmap(transparentImage, 0);
        }

        public Bitmap getTransparentBitmap(Bitmap sourceImg, int number) {
            int[] argb = new int[sourceImg.getWidth() * sourceImg.getHeight()];

            sourceImg.getPixels(argb, 0, sourceImg.getWidth(), 0, 0, sourceImg.getWidth(), sourceImg.getHeight());// 获得图片的ARGB值

            number = number * 255 / 100;

            for (int i = 0; i < argb.length; i++) {
                argb[i] = (number << 24) | (argb[i] & 0x00FFFFFF);
            }

            sourceImg = Bitmap.createBitmap(argb, sourceImg.getWidth(), sourceImg.getHeight(), Bitmap.Config.ARGB_8888);
            return sourceImg;
        }

        private Bitmap generateScaledTileFromChildren() throws TileStoreException {
            final int tileWidth = RawImageTileReader2.this.tileSize.getWidth();
            final int tileHeight = RawImageTileReader2.this.tileSize.getHeight();

            // Create the full-sized buffered image from the child tiles
            final Bitmap fullCanvas = Bitmap.createBitmap(tileWidth * 2, tileHeight * 2, Bitmap.Config.ARGB_8888);

            // Create the full-sized graphics object
            Canvas fullCanvasGraphics = new Canvas(fullCanvas);
//            final Graphics2D fullCanvasGraphics = fullCanvas.createGraphics();

            // Get child handles
            final List<RawImageTileReader2.RawImageTileHandle> children = this.generateTileChildren();

            // Get the cached children of this tile
            final List<RawImageTileReader2.RawImageTileHandle> transformedChildren = children.stream().map(tileHandle -> {
                final Coordinate<Integer> resultCoordinate = RawImageTileReader2.Origin.transform(TileOrigin.UpperLeft, tileHandle.column, tileHandle.row, this.matrix);
                try {
                    return new RawImageTileHandle(tileHandle.zoomLevel, resultCoordinate.getX(), resultCoordinate.getY(), tileHandle.cachedImageLocation);
                } catch (TileStoreException e) {
                    throw new RuntimeException(e);
                }
            }).sorted((final TileHandle o1, final TileHandle o2) -> {
                final int columnCompare = Integer.compare(o1.getColumn(), o2.getColumn());
                final int rowCompare = Integer.compare(o1.getRow(), o2.getRow());

                // column values are the same
                if (columnCompare == 0) {
                    return rowCompare;
                }
                if (rowCompare == 0) {
                    return columnCompare;
                }
                return 0;
            }).collect(Collectors.toList());

            if (transformedChildren.size() == 4) {
                // Origin tile
                if (transformedChildren.get(2).cachedImageLocation != null) {
                    Bitmap originImage = BitmapFactory.decodeFile(transformedChildren.get(2).cachedImageLocation.getAbsolutePath());

//                        final Bitmap originImage = ImageIO.read(transformedChildren.get(2).cachedImageLocation.toFile());
//                        fullCanvasGraphics.drawImage(originImage, null, 0, 0);
                    fullCanvasGraphics.drawBitmap(originImage, 0, 0, null);
                }

                // Tile that is Y+1 in relation to the origin
                if (transformedChildren.get(0).cachedImageLocation != null) {
                    Bitmap rowShiftedImage = BitmapFactory.decodeFile(transformedChildren.get(0).cachedImageLocation.getAbsolutePath());
//                        final Bitmap rowShiftedImage = ImageIO.read(transformedChildren.get(0).cachedImageLocation.toFile());
//                        fullCanvasGraphics.drawImage(rowShiftedImage, null, 0, tileHeight);
                    fullCanvasGraphics.drawBitmap(rowShiftedImage, 0, tileHeight, null);
                }

                // Tile that is X+1 in relation to the origin
                if (transformedChildren.get(3).cachedImageLocation != null) {
//                        final Bitmap columnShiftedImage = ImageIO.read(transformedChildren.get(3).cachedImageLocation.toFile());
                    Bitmap columnShiftedImage = BitmapFactory.decodeFile(transformedChildren.get(0).cachedImageLocation.getAbsolutePath());
//                        fullCanvasGraphics.drawImage(columnShiftedImage, null, tileWidth, 0);
                    fullCanvasGraphics.drawBitmap(columnShiftedImage, tileWidth, 0, null);
                }

                // Tile that is both X+1 and Y+1 in relation to the origin
                if (transformedChildren.get(1).cachedImageLocation != null) {
//                        final Bitmap bothShiftedImage = ImageIO.read(transformedChildren.get(1).cachedImageLocation.toFile());
                    Bitmap bothShiftedImage = BitmapFactory.decodeFile(transformedChildren.get(0).cachedImageLocation.getAbsolutePath());
//                        fullCanvasGraphics.drawImage(bothShiftedImage, null, tileWidth, tileHeight);
                    fullCanvasGraphics.drawBitmap(bothShiftedImage, tileWidth, tileHeight, null);
                }
            } else {
                throw new RuntimeException("Cannot create tile from child tiles.");
            }

            // Write cached tile
            try {
                final Bitmap scaledTile = this.scaleToTileCanvas(fullCanvas);

                RawImageTileReader2.this.cachedTiles.put(this.tileKey(this.zoomLevel, this.column, this.row), this.writeTempTile(scaledTile));
                return scaledTile;
            } catch (final IOException ex) {
                throw new TileStoreException(ex);
            } finally {
                // Clean-up step
//                fullCanvasGraphics.dispose();
                children.stream().filter(Objects::nonNull).forEach(child -> {
                    if (child.cachedImageLocation != null) {
                        child.cachedImageLocation.delete();
                    }
                    RawImageTileReader2.this.cachedTiles.remove(this.tileKey(child.zoomLevel, child.column, child.row));
                });
            }
        }

        private Bitmap scaleToTileCanvas(final Bitmap fullCanvas) {
            final Bitmap tileCanvas = Bitmap.createBitmap(RawImageTileReader2.this.tileSize.getWidth(), RawImageTileReader2.this.tileSize.getHeight(), Bitmap.Config.ARGB_8888);

//            final AffineTransform affineTransform = new AffineTransform();
//            affineTransform.scale(AFFINE_SCALE, AFFINE_SCALE);
//            final BufferedImageOp scaleOp = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);
//
//            return scaleOp.filter(fullCanvas, tileCanvas);

            return imageAffineTransformOp(fullCanvas, tileCanvas, AFFINE_SCALE, AFFINE_SCALE);
        }

        private Bitmap imageAffineTransformOp(Bitmap src, Bitmap dst, double affineScale, double affineScale1) {
            if (src == null) {
                throw new NullPointerException("src image is null");
            }
            if (src == dst) {
                throw new IllegalArgumentException("src image cannot be the " + "same as the dst image");
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            // 获取这个图片的宽和高信息到options中, 此时返回bm为空
            options.inJustDecodeBounds = false;
            // 计算缩放比
            int sampleSize = sampleSize(options, (int) (src.getWidth() * affineScale), (int) (src.getHeight() * affineScale1));
            options.inSampleSize = sampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inPurgeable = true;
            options.inInputShareable = true;

            if (src != null && !src.isRecycled()) {
                src.recycle();
            }

            return BitmapFactory.decodeFile("", options);

//            bitmap = BitmapFactory.decodeFile(src, options);

//            return bitmap;
//            return null;
        }

        public int sampleSize(BitmapFactory.Options options, int maxWidth, int maxHeight) {
            // raw height and width of image
            int rawWidth = options.outWidth;
            int rawHeight = options.outHeight;

            // calculate best sample size
            int inSampleSize = 0;
            if (rawHeight > maxHeight || rawWidth > maxWidth) {
                float ratioWidth = (float) rawWidth / maxWidth;
                float ratioHeight = (float) rawHeight / maxHeight;
                inSampleSize = (int) Math.min(ratioHeight, ratioWidth);
            }
            inSampleSize = Math.max(1, inSampleSize);

            return inSampleSize;
        }

        private String tileKey(final int zoom, final int column, final int row) {
            return String.format("%d/%d/%d", zoom, column, row);
        }

        private List<RawImageTileReader2.RawImageTileHandle> generateTileChildren() throws TileStoreException {
            final int childZoom = this.zoomLevel + 1;
            final int childColumn = this.column * 2;
            final int childRow = this.row * 2;

            final File origin = RawImageTileReader2.this.cachedTiles.get(this.tileKey(childZoom, childColumn, childRow));
            final File columnShifted = RawImageTileReader2.this.cachedTiles.get(this.tileKey(childZoom, childColumn + 1, childRow));
            final File rowShifted = RawImageTileReader2.this.cachedTiles.get(this.tileKey(childZoom, childColumn, childRow + 1));
            final File bothShifted = RawImageTileReader2.this.cachedTiles.get(this.tileKey(childZoom, childColumn + 1, childRow + 1));

            final List<RawImageTileReader2.RawImageTileHandle> children = new ArrayList<>();

            children.add(new RawImageTileReader2.RawImageTileHandle(childZoom, childColumn, childRow, origin));
            children.add(new RawImageTileReader2.RawImageTileHandle(childZoom, childColumn + 1, childRow, columnShifted));
            children.add(new RawImageTileReader2.RawImageTileHandle(childZoom, childColumn, childRow + 1, rowShifted));
            children.add(new RawImageTileReader2.RawImageTileHandle(childZoom, childColumn + 1, childRow + 1, bothShifted));

            return children;
        }

        @Override
        public String toString() {
            return this.tileKey(this.zoomLevel, this.column, this.row);
        }
    }

    private CrsCoordinate tileToCrsCoordinate(final int column, final int row, final TileMatrixDimensions tileMatrixDimensions, final TileOrigin corner) {
        if (corner == null) {
            throw new IllegalArgumentException("Corner may not be null");
        }
        // RawImageTileReader uses absolute tiling, which covers the whole globe
        return this.profile.tileToCrsCoordinate(column + corner.getHorizontal(), row + corner.getVertical(), this.profile.getBounds(), tileMatrixDimensions, RawImageTileReader2.Origin);
    }

    private BoundingBox getTileBoundingBox(final int column, final int row, final TileMatrixDimensions tileMatrixDimensions) {
        return this.profile.getTileBounds(column, row, this.profile.getBounds(), tileMatrixDimensions, RawImageTileReader2.Origin);
    }

    private Bitmap rawImage;
    private CoordinateReferenceSystem coordinateReferenceSystem;
    private Dimensions<Integer> tileSize;
    //private final Color                                    noDataColor; // TODO implement no-data color handling
//    private final Dataset dataset;
    private BoundingBox dataBounds;
    private Set<Integer> zoomLevels;
    private ZoomTimesTwo tileScheme;
    private CrsProfile profile;
    private int tileCount;
    private Map<Integer, Range<Coordinate<Integer>>> tileRanges;
    private Map<String, File> cachedTiles;

    private static final int MAX_ZOOM_LEVEL = 31;
    private static final double AFFINE_SCALE = 0.5;
    private static final String NOT_SUPPORTED_MESSAGE = "Call to unsupported method.";

    private static final TileOrigin Origin = TileOrigin.LowerLeft;
}
