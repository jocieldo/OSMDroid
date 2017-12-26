package com.zhjf.osmdroid.tile.pdf;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.DisplayMetrics;

import com.tom_roush.pdfbox.cos.COSArray;
import com.tom_roush.pdfbox.cos.COSBase;
import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.cos.COSFloat;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.zhjf.osmdroid.geopackage.FilePathManage;

import rgi.common.coordinate.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mil.nga.wkb.geom.GeometryEnvelope;
import mil.nga.wkb.geom.Point;
import rgi.common.BoundingBox;
import rgi.common.Dimensions;
import rgi.g2t.RawImageTileReader;
import rgi.store.tiles.TileStoreException;

import static java.lang.Math.PI;
import static java.lang.Math.abs;

/**
 * Created by Administrator on 2017/11/27.
 */
public class PDFCacheUtils {
    private int tileSize = 256;
    /**
     * 标识Level 0 的时候只有一个Tile
     */
    private double resFact = 180.0 / tileSize;

    private final int MAXZOOMLEVEL = 32;
    private Map<Integer, int[]> tminmax;
    private float density;
    private int densityDPI;

    public PDFCacheUtils(Activity context) {
        DisplayMetrics metric = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metric);
        density = metric.density;  // 屏幕密度（0.75 / 1.0 / 1.5）
        densityDPI = metric.densityDpi;  // 屏幕密度DPI（120 / 160 / 240）
    }

    /**
     *
     */
    public PDFInfo getPDFInfo(File file) throws IOException {
        if (file.exists()) {
            PDFInfo pdfInfo = new PDFInfo();
            PDDocument pdDocument = PDDocument.load(file);
            // 获取页码
            int pages = pdDocument.getNumberOfPages();
            if (pages > 0) {
                PDPage pdPage = pdDocument.getPage(0);
                pdfInfo.bitmap = new PDFRenderer(pdDocument).renderImage(0, density, Bitmap.Config.ARGB_8888);
                GeometryEnvelope envelope = new GeometryEnvelope(false, false);
                COSDictionary dictionary = pdPage.getCOSObject();
                COSArray cosArray = (COSArray) dictionary.getDictionaryObject("VP");
                Iterator<COSBase> iterator = cosArray.iterator();
                while (iterator.hasNext()) {
                    COSBase cosBase = iterator.next();
                    if (cosBase instanceof COSDictionary) {
                        dictionary = (COSDictionary) cosBase;
                        cosBase = dictionary.getDictionaryObject("Measure");
                        if (cosBase instanceof COSDictionary) {
                            dictionary = (COSDictionary) cosBase;
                            cosBase = dictionary.getDictionaryObject("GPTS");
                            if (cosBase instanceof COSArray) {
                                cosArray = (COSArray) cosBase;
                                for (int i = 0; i < cosArray.size(); i++) {
                                    cosBase = cosArray.get(i);
                                    if (cosBase instanceof COSFloat) {
                                        COSFloat cosFloat = (COSFloat) cosBase;
                                        if (i == 0) {
                                            envelope.setMinX(cosFloat.floatValue());
                                        } else if (i == 1) {
                                            envelope.setMinY(cosFloat.floatValue());
                                        } else if (i == 2) {
                                            envelope.setMaxX(cosFloat.floatValue());
                                        } else if (i == 3) {
                                            envelope.setMinY(cosFloat.floatValue());
                                        } else if (i == 4) {
                                            envelope.setMaxX(cosFloat.floatValue());
                                        } else if (i == 5) {
                                            envelope.setMaxY(cosFloat.floatValue());
                                        } else if (i == 6) {
                                            envelope.setMinX(cosFloat.floatValue());
                                        } else if (i == 7) {
                                            envelope.setMaxY(cosFloat.floatValue());
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
                pdfInfo.envelope = envelope;
            }
            return pdfInfo;
        }
        return null;
    }

    public double getResolution(int zoom) {
        return resFact / Math.pow(2, (double) zoom);
    }

    public int[] lonlatToTile(double lon, double lat, int zoom) {
        double[] pxy = new double[]{0.0, 0.0};
        double res = resFact / Math.pow(2, (double) zoom);
        pxy[0] = (180.0 + lon) / res;
        pxy[1] = (90.0 + lat) / res;
        return new int[]{(int) (Math.ceil(pxy[0] / 256.0) - 1), (int) (Math.ceil(pxy[1] / 256.0) - 1)};
    }

    /**
     * @param envelope PDF地理范围
     * @param bitmap   PDF像素范围
     */
    public void createCache(String cacheName, GeometryEnvelope envelope, Bitmap bitmap) throws FileNotFoundException {
        //获得最大的缩放级别（在光栅分辨率上最接近的缩放级别）
//        double pixelSize = self.out_gt[1];
        int pixelSize = 256;
        bitmap.getDensity();
        int minZoom = zoomForPixelSize(pixelSize * max(bitmap.getWidth() * bitmap.getDensity() / 96, bitmap.getHeight() * bitmap.getDensity() / 96) / (float) (tileSize));
        int maxZoom = zoomForPixelSize(pixelSize);
        //生成所有Level中瓦片最大最小值对应的坐标
        tminmax = new ArrayMap<>();
        for (int tz = 15; tz <= 17; tz++) {
            // lon lat
            int[] leftTopPoint = lonlatToTile(envelope.getMinY(), envelope.getMinX(), tz);
            int[] rightBottomPoint = lonlatToTile(envelope.getMaxY(), envelope.getMaxY(), tz);
            leftTopPoint = new int[]{max(0, leftTopPoint[0]), max(0, leftTopPoint[1])};
            rightBottomPoint = new int[]{min((int) (Math.pow(2, tz + 1) - 1), rightBottomPoint[0]), min((int) Math.pow(2, tz - 1), rightBottomPoint[1])};
            tminmax.put(tz, new int[]{leftTopPoint[0], leftTopPoint[1], rightBottomPoint[0], rightBottomPoint[1]});
        }
        File file = new File(FilePathManage.getInstance().getCacheDirectory() + "/" + cacheName);
        if (!file.exists())
            file.mkdirs();
        for (Map.Entry<Integer, int[]> entry : tminmax.entrySet()) {
            int[] bound = entry.getValue();
            int tz = entry.getKey();
            int tcount = ((1 + abs(bound[2] - bound[0])) * (1 + abs(bound[3] - bound[1])));
            System.out.println(tcount);
            for (int ty = bound[3]; ty < bound[1] - 1; ty--) {
                for (int tx = bound[0]; tx < bound[2] + 1; tx++) {
                    String outputFileName = String.format("tile_%d_%d_%d.png", tx, ty, tz);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 96, new FileOutputStream(new File(file.getPath(), outputFileName)));
                    double[] tileBound = tileBounds(tx, ty, tz);
                    System.out.println(tileBound);
//                    geoQuery(bitmap, tileBound.tminx, tileBound.tmaxy, tileBound.tminy, tileBound.tmaxx);
                }
            }
        }
    }

    public void createCache2(String cacheName, GeometryEnvelope bound, Bitmap bitmap) {
        double dXMin = 0.0;
        double dYMin = 0.0;
        double dXMax = 0.0;
        double dYMax = 0.0;
        int iScalelevels = 3;
        double dDPI = 96;
        double dWidth = 256;
        double dHeight = 256;

        dXMin = bound.getMinX();
        dYMin = bound.getMinY();
        dXMax = bound.getMaxX();
        dYMax = bound.getMaxY();
        File sFolderDir = new File(FilePathManage.getInstance().getCacheDirectory() + "/" + cacheName);
        if (!sFolderDir.exists())
            sFolderDir.mkdirs();

        double dTileOriginX = 0.0;
        double dTileOriginY = 0.0;

        dTileOriginX = -400;
        dTileOriginY = 400;

        int iLNum = 0;
        for (int i = 15; i <= 17; i++) {
            String sLFolderName = sFolderDir.getPath() + "\\L" + String.valueOf(iLNum);
            new File(sLFolderName).mkdirs();

            double dMapScale = (96 * 2 * Math.PI * 6378137 * getResolution(i) / 360 / 0.0254);
            double dResolution = dMapScale * 0.0254 / dDPI;

            double dImageWidth = dWidth * dResolution;
            double dImageHeight = dHeight * dResolution;

            double detaY = 1;
            int iRNum = 0;
            while (dTileOriginY - dImageHeight * detaY > dYMin - dImageHeight) {
                String sRFolderName = sLFolderName + "\\R" + iRNum;
                new File(sRFolderName).mkdirs();

                double dTempYMax = dTileOriginY - dImageHeight * detaY;
                double dTempYMin = dTileOriginY - dImageHeight * (detaY - 1);

                double detaX = 1;
                int iCNum = 0;
                while (dTileOriginX + dImageWidth * detaX < dXMax + dImageWidth) {
                    double dTempXMin = dTileOriginX + dImageWidth * (detaX - 1);
                    double dTempXMax = dTileOriginX + dImageWidth * detaX;
                    GeometryEnvelope envelope = new GeometryEnvelope(false, false);
                    envelope.setMinX(dTempXMin);
                    envelope.setMinY(dTempYMin);
                    envelope.setMaxX(dTempXMax);
                    envelope.setMaxY(dTempYMax);
                    String destImageName = "\\C" + iCNum + ".png";
                    String sImagePath = sRFolderName + destImageName;

                    exportToImage(bitmap, sImagePath, envelope, (int) dWidth, (int) dHeight);
                    iCNum++;
                    detaX++;
                }
                iRNum++;
                detaY++;
            }
            iLNum++;
        }
    }

    //导出图片 dpi为固定96
    private void exportToImage(Bitmap bitmap, String sImagePath, GeometryEnvelope pEnvelope, int right, int bottom) {
        Rect grect = new Rect();
        //照片的大小
        GeometryEnvelope envelope = new GeometryEnvelope(false, false);
        grect.left = 0;
        grect.top = 0;
        grect.right = right;
        grect.bottom = bottom;
        envelope.setMinX((double) grect.left);
        envelope.setMinY((double) grect.bottom);
        envelope.setMaxX((double) grect.right);
        envelope.setMaxY((double) grect.top);

        // pActiveView.Output(export.StartExporting(), 96, ref grect, pEnvelope, cancel);
    }

    private Map<String, int[]> geoQuery(Bitmap bitmap, double ulx, double uly, double lrx, double lry, int querysize) {
        double[] geotran = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 1.0};
        int rx = (int) ((ulx - geotran[0]) / geotran[1] + 0.001);
        int ry = (int) ((uly - geotran[3]) / geotran[5] + 0.001);
        int rxsize = (int) ((lrx - ulx) / geotran[1] + 0.5);
        int rysize = (int) ((lry - uly) / geotran[5] + 0.5);

        int wxsize, wysize;
        if (querysize == 0) {
            wxsize = rxsize;
            wysize = rysize;
        } else {
            wxsize = querysize;
            wysize = querysize;
        }

        int wx = 0;
        if (rx < 0) {
            int rxshift = abs(rx);
            wx = (int) (wxsize * ((float) (rxshift) / rxsize));
            wxsize = wxsize - wx;
            rxsize = rxsize - (int) (rxsize * ((float) (rxshift) / rxsize));
            rx = 0;
        }

        if (rx + rxsize > bitmap.getWidth()) {
            wxsize = (int) (wxsize * ((float) (bitmap.getWidth() - rx) / rxsize));
            rxsize = bitmap.getWidth() - rx;
        }


        int wy = 0;
        if (ry < 0) {
            int ryshift = abs(ry);
            wy = (int) (wysize * ((float) (ryshift) / rysize));
            wysize = wysize - wy;
            rysize = rysize - (int) (rysize * ((float) (ryshift) / rysize));
            ry = 0;
        }
        if (ry + rysize > bitmap.getHeight()) {
            wysize = (int) (wysize * ((float) (bitmap.getHeight() - ry) / rysize));
            rysize = bitmap.getHeight() - ry;
        }

        Map<String, int[]> result = new HashMap<>();
        result.put("rb", new int[]{rx, ry, rxsize, rysize});
        result.put("wb", new int[]{wx, wy, wxsize, wysize});
        return result;
    }

    private double[] tileBounds(int tx, int ty, int zoom) {
        double res = resFact / Math.pow(2, (double) zoom);
        return new double[]{tx * tileSize * res - 180.0,
                ty * tileSize * res - 90.0,
                (tx + 1) * tileSize * res - 180.0,
                (ty + 1) * tileSize * res - 90.0};
    }

    /**
     * @param pixelSize 图像像素最大值
     * @return
     */
    private int zoomForPixelSize(double pixelSize) {
        for (int i = 0; i < MAXZOOMLEVEL; i++) {
            if (pixelSize > getResolution(i)) {
                if (i != 0)
                    return i - 1;
                else
                    return 0;
            }
        }
        return 0;
    }

    public int max(int x1, int x2) {
        return x1 > x2 ? x1 : x2;
    }

    public int min(int x1, int x2) {
        return x1 < x2 ? x1 : x2;
    }

    public void createCache3(String cacheName, GeometryEnvelope envelope, int minZoom, int maxZoom, Bitmap image) {
        File sFolderDir = new File(FilePathManage.getInstance().getCacheDirectory() + "/" + cacheName);
        if (!sFolderDir.exists())
            sFolderDir.mkdirs();
        // Verify arguments are valid
        // Tiling
        for (int currentZoom = minZoom; currentZoom <= maxZoom; currentZoom++) {
            final File zoomDirectory = new File(sFolderDir.getAbsolutePath() + "/" + currentZoom);
            if (!zoomDirectory.exists()) zoomDirectory.mkdirs();
            //计算这一层级的最大最小值
            int[] tminxy = lonlatToTile(envelope.getMinY(), envelope.getMinX(), currentZoom);
            int[] tmaxxy = lonlatToTile(envelope.getMaxY(), envelope.getMaxX(), currentZoom);
            tminxy[0] = max(0, tminxy[0]);
            tminxy[1] = max(0, tminxy[1]);

            tmaxxy[0] = min((int) (Math.pow(2, currentZoom + 1) - 1), tmaxxy[0]);
            tmaxxy[1] = min((int) (Math.pow(2, currentZoom) - 1), tmaxxy[1]);

            int tcount = (1 + abs(tmaxxy[0] - tminxy[0])) * (1 + abs(tmaxxy[1] - tminxy[1]));

            final int rowCount = (int) Math.sqrt(Math.pow(4, currentZoom));
            final int realTileLength = (image.getHeight() < image.getWidth() ? image.getHeight() : image.getWidth()) / rowCount;

            for (int currentColumn = 0; currentColumn < rowCount; currentColumn++) {
                for (int currentRow = 0; currentRow < rowCount; currentRow++) {
                    // Get sub image
                    final int x = currentRow * realTileLength;
                    final int y = currentColumn * realTileLength;
                    final int w = realTileLength;
                    final int h = realTileLength;
                    Bitmap subImageRaw = Bitmap.createBitmap(image, x, y, w, h);

                    if (subImageRaw.getWidth() < 256) {
                        subImageRaw = Bitmap.createScaledBitmap(subImageRaw, 256, subImageRaw.getHeight(), false);
                    }
                    if (subImageRaw.getHeight() < 256) {
                        subImageRaw = Bitmap.createScaledBitmap(subImageRaw, subImageRaw.getWidth(), 256, false);
                    }

                    // Size image for saving
                    Bitmap subImage = Bitmap.createBitmap(subImageRaw, 0, 0, 256, 256, null, false);
                    // Write image to file
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(new File(zoomDirectory, currentRow + "_" + currentColumn + "_" + currentZoom + "." + "png"));
                        subImage.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    } catch (final IOException ex) {
                        throw new RuntimeException("Failed to output file!");
                    }
                }
            }
        }
    }

    public void createCache4(String cacheName, GeometryEnvelope envelope, int minZoom, int maxZoom, Bitmap image) {
        File sFolderDir = new File(FilePathManage.getInstance().getCacheDirectory() + "/" + cacheName);
        if (!sFolderDir.exists())
            sFolderDir.mkdirs();

        Dimensions<Integer> tileSize = new Dimensions<>(256, 256);
        BoundingBox boundingBox = new BoundingBox(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
        try {
            RawImageTileReader rawImageTileReader = new RawImageTileReader(cacheName, image, tileSize, new CoordinateReferenceSystem(null, "EPSG", 4326), boundingBox);
            rawImageTileReader.stream().filter(tile -> tile.getZoomLevel() == maxZoom).forEach(tile -> {
                try {
                    Bitmap bitmap = tile.getImage();
                    final File zoomDirectory = new File(sFolderDir.getAbsolutePath() + "/" + tile.getZoomLevel());
                    if (!zoomDirectory.exists()) {
                        zoomDirectory.mkdir();
                    }
                    File file = new File(zoomDirectory, tile.getRow() + "_" + tile.getColumn() + "_" + tile.getZoomLevel() + ".png");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);

                    bitmap.compress(Bitmap.CompressFormat.PNG, 96, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (final TileStoreException exp) {
                    throw new RuntimeException(exp);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (TileStoreException e) {
            e.printStackTrace();
        }
    }

    private List<ScaleLevel> scaleLevels = new ArrayList<>();

    private void initScale() {
        scaleLevels.add(new ScaleLevel(19, 20, (20 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(18, 50, (50 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(17, 100, (100 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(16, 200, (200 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(15, 500, (500 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(14, 1000, (1000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(13, 2000, (2000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(12, 5000, (5000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(11, 10000, (10000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(10, 20000, (20000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(9, 25000, (25000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(8, 50000, (50000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(7, 100000, (100000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(6, 200000, (200000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(5, 500000, (500000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(4, 1000000, (1000000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(3, 2000000, (2000000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(2, 5000000, (5000000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
        scaleLevels.add(new ScaleLevel(1, 10000000, (10000000 * 360) / (0.0254 * 96 * 2 * PI * 6378137)));
    }

    public double originX = -400, originY = 400;

    /**
     * 得到瓦片行列号
     *
     * @param canvasWidth
     * @param canvasHeight
     * @param envelope
     * @param level
     */
    public void createCache4(int canvasWidth, int canvasHeight, GeometryEnvelope envelope, int level) {
        initScale();
        Point centerPoint = new Point(false, false, (envelope.getMinX() + envelope.getMaxX()) / 2, (envelope.getMinY() + envelope.getMaxY()) / 2);

        if (level < 0) {
            //根据参数中的几何范围得到理论上的瓦片大小
            double clipXLength = (envelope.getMaxX() - envelope.getMinX()) / (canvasWidth / tileSize);
            Map<Integer, Double> nearLength = new HashMap<>();
            for (int i = 0; i < scaleLevels.size(); i++) {
                //这里计算每个级别下的瓦片实际大小
                ScaleLevel scaleLevel = scaleLevels.get(i);
                //如果是投影坐标系
                //如果是经纬度坐标系
                double resolution = (scaleLevel.scale * 360) / (0.0254 * 96 * 2 * PI * 6378137);

                double levelClipXLength = resolution * tileSize;
                nearLength.put(19 - i, Math.abs(clipXLength - levelClipXLength));
            }
            List<Map.Entry<Integer, Double>> list = new ArrayList<>(nearLength.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
                @Override
                public int compare(Map.Entry<Integer, Double> item1, Map.Entry<Integer, Double> item2) {
                    return item1.getValue().compareTo(item2.getValue());
                }
            });
            level = list.get(0).getKey();
        }
        double resolution = (getScaleByLevel(level) * 360) / (0.0254 * 96 * 2 * PI * 6378137);

        //算出屏幕范围对应的地理范围
        GeometryEnvelope screenEnvelop = new GeometryEnvelope(false, false);
        screenEnvelop.setMinX(centerPoint.getX() - ((resolution * canvasWidth) / 2));
        screenEnvelop.setMinY(centerPoint.getY() - ((resolution * canvasHeight) / 2));
        screenEnvelop.setMaxX(centerPoint.getX() - ((resolution * canvasWidth) / 2));
        screenEnvelop.setMaxY(centerPoint.getY() - ((resolution * canvasHeight) / 2));
        //计算屏幕范围对应切片的起始行列号
        int fixedTileLeftTopNumX = (int) Math.floor((Math.abs(originX - screenEnvelop.getMinX())) / resolution * tileSize);
        int fixedTileLeftTopNumY = (int) Math.floor((Math.abs(originY - screenEnvelop.getMaxY())) / resolution * tileSize);
        //根据起始行列号纠正实际地理范围
        double realMinX = fixedTileLeftTopNumX * resolution * tileSize + originX;
        double realMaxY = originX - fixedTileLeftTopNumY * resolution * tileSize;
        //计算出偏移量
        double offSetX = ((realMinX - screenEnvelop.getMinX()) / resolution);
        double offSetY = ((screenEnvelop.getMaxY() - realMaxY) / resolution);
        //计算瓦片个数
        int mapXClipNum = (int) Math.ceil((canvasWidth + Math.abs(offSetX)) / tileSize);
        int mapYClipNum = (int) Math.ceil((canvasHeight + Math.abs(offSetY)) / tileSize);
        System.out.print("");
    }

    private int getScaleByLevel(int level) {
        for (int i = 0; i < scaleLevels.size(); i++) {
            ScaleLevel scaleLevel = scaleLevels.get(i);
            if (scaleLevel.getLevel() == level) {
                return scaleLevel.getScale();
            }
        }
        return 0;
    }

    public class PDFInfo {
        public GeometryEnvelope envelope;
        public PDRectangle rectangle;
        public Bitmap bitmap;
    }

    private class ScaleLevel {
        private int level;
        private int scale;
        private double resolution;

        public ScaleLevel() {

        }

        public ScaleLevel(int level, int scale, double resolution) {
            this.level = level;
            this.scale = scale;
            this.resolution = resolution;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getScale() {
            return scale;
        }

        public void setScale(int scale) {
            this.scale = scale;
        }

        public double getResolution() {
            return resolution;
        }

        public void setResolution(double resolution) {
            this.resolution = resolution;
        }
    }
}
