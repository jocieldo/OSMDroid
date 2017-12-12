package com.zhjf.osmdroid.geopackage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

import com.zhjf.osmdroid.R;
import com.zhjf.osmdroid.common.DensityUtil;
import com.zhjf.osmdroid.common.FileUtil;
import com.zhjf.osmdroid.entity.AttributeEntity;
import com.zhjf.osmdroid.entity.layer.AttributeLayer;
import com.zhjf.osmdroid.overlay.TileLayer;
import com.zhjf.osmdroid.overlay.VectorLayer;

import org.osmdroid.gpkg.overlay.OsmMapShapeConverter;
import org.osmdroid.gpkg.overlay.features.MarkerOptions;
import org.osmdroid.gpkg.overlay.features.PolygonOptions;
import org.osmdroid.gpkg.overlay.features.PolylineOptions;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.attributes.AttributesColumn;
import mil.nga.geopackage.attributes.AttributesDao;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.geopackage.schema.TableColumnKey;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.Point;

public class CustomGeoPackageManager {
    private static GeoPackageManager geoPackageManager;
    private Context context;
    private MapView mapView;
    private static CustomGeoPackageManager customGeoPackageManager = null;
    private GeoPackage geoPackage;
    private boolean hasDb = false;
    private File vectorMapFilePath;

    private CustomGeoPackageManager(Context context) {
        this.context = context;
        geoPackageManager = GeoPackageFactory.getManager(context);
    }


    public GeoPackage getGeoPackage() {
        return geoPackage;
    }

    public void setMapView(MapView mapView) {
        this.mapView = mapView;
    }

    public static CustomGeoPackageManager getInstance(Context context) {
        if (customGeoPackageManager == null && context != null)
            synchronized (CustomGeoPackageManager.class) {
                if (customGeoPackageManager == null)
                    customGeoPackageManager = new CustomGeoPackageManager(context);
            }

        return customGeoPackageManager;
    }

    public boolean createEmptDatabase(File dbPath, String dbName) throws Exception {
        boolean created = false;
        if (dbPath == null || !dbPath.exists()) return created;
        geoPackageManager = GeoPackageFactory.getManager(context);
        if (!geoPackageManager.exists(dbName)) {
            if (geoPackageManager.createAtPath(dbName, dbPath)) {
                geoPackage = geoPackageManager.open(dbName);
                created = true;
            }
        }
        return created;
    }

    public boolean createFeatureTable() {
        //不存在表才创建表
        if (geoPackage.isFeatureOrTileTable("testTable")) {
            return true;
        }
        try {
            //创建表
            double minLat = -90 + 1;
            double maxLat = 90 - 1;
            double minLon = -180 + 1;
            double maxLon = 180 - 1;

            BoundingBox boundingBox = new BoundingBox(minLon, maxLon, minLat, maxLat);

            GeometryColumns geometryColumns = new GeometryColumns();
            //设置id
            geometryColumns.setId(new TableColumnKey("testTable", "Shape"));
            geometryColumns.setGeometryType(GeometryType.POINT);
//            geometryColumns.setZ((byte) 1);

            /**
             * 首先构建表的列，然后再构建表对象
             */
            int index = 0;
            List<FeatureColumn> columns = new ArrayList<>();
            columns.add(FeatureColumn.createPrimaryKeyColumn(index++, "ID"));
            columns.add(FeatureColumn.createGeometryColumn(index++, geometryColumns.getColumnName(), geometryColumns.getGeometryType(), false, null));
            columns.add(FeatureColumn.createColumn(index++, "NAME", GeoPackageDataType.TEXT, true, ""));
            columns.add(FeatureColumn.createColumn(index++, "CODE", GeoPackageDataType.TEXT, true, ""));

            if (geoPackage != null) {
                GeometryColumns columns1 = geoPackage.createFeatureTableWithMetadata(geometryColumns, boundingBox, ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM, columns);
                if (columns1 != null) {
                    FeatureDao featureDao = geoPackage.getFeatureDao("testTable");
                    GeoPackageGeometryData geomData = new GeoPackageGeometryData(featureDao.getGeometryColumns().getSrsId());
                    geomData.setGeometry(new Point(25, 101));
                    FeatureRow row = featureDao.newRow();
                    row.setGeometry(geomData);
                    row.setValue("NAME", "JERFER");
                    row.setValue("CODE", "001");
                    long count = featureDao.insert(row);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void initMap() throws Exception {
        //第一步扫描出包含的内容
        String tileMapFile = FilePathManage.getInstance().getMap();
        if (!tileMapFile.isEmpty()) {
            initTileMap(new File(tileMapFile));
        } else {
            throw new Exception("no tile map");
        }

        String vectorMapFile = FilePathManage.getInstance().getVector();
        if (!vectorMapFile.isEmpty()) {
            initVectorMap(new File(vectorMapFile));
        } else {
            createEmptDatabase(new File(FilePathManage.getInstance().getRootDir()), "data");
        }
    }

    private void initVectorMap(File vectorMapFilePath) throws Exception {
        if (!vectorMapFilePath.exists()) {
            throw new Exception("文件不存在");
        }
        this.vectorMapFilePath = vectorMapFilePath;
        geoPackageManager.deleteAll();
        String dbName = vectorMapFilePath.getName().substring(0, vectorMapFilePath.getName().indexOf("."));
        if (geoPackageManager.importGeoPackage(dbName, vectorMapFilePath)) {
            geoPackage = geoPackageManager.open(dbName);
            if (geoPackage != null) {
                List<String> features = geoPackage.getFeatureTables();
                hasDb = true;
                for (String tableName : features) {
                    VectorLayer vectorLayer = new VectorLayer(mapView, context, tableName);
                    vectorLayer.setName(tableName);
                    FeatureDao featureDao = geoPackage.getFeatureDao(tableName);
                    vectorLayer.setColumns(featureDao.getTable().getColumns());
                    if (GeometryType.POINT == featureDao.getGeometryType()) {
                        vectorLayer.setGeometryType(GeometryType.POINT);
                        vectorLayer.setLabeledColumn(featureDao.getTable().getColumn("name"));
                        vectorLayer.buildOverlays();
                    } else if (GeometryType.LINESTRING == featureDao.getGeometryType()) {
                        vectorLayer.setGeometryType(GeometryType.LINESTRING);
                        vectorLayer.buildOverlays();
                    } else if (GeometryType.POLYGON == featureDao.getGeometryType()) {
                        vectorLayer.setGeometryType(GeometryType.POLYGON);
                        vectorLayer.buildOverlays();
                    }
                    mapView.getOverlays().add(vectorLayer);
                    vectorLayer.setEnabled(true);
                    vectorLayer.setEdit(false);
                }
            }
        }
    }

    private void initTileMap(File mapTileFilePath) throws Exception {
        if (mapTileFilePath.exists()) {
            TileLayer tileLayer = new TileLayer(mapView, context, new OfflineTileProvider(new SimpleRegisterReceiver(context), new File[]{mapTileFilePath}));
            tileLayer.setEnabled(true);
            tileLayer.setTilePath(mapTileFilePath);
            try {
                mapView.getOverlays().add(tileLayer);
            } catch (Exception ex) {
                throw ex;
            }
        }
    }

    public boolean isHasDb() {
        return hasDb;
    }

    public void setHasDb(boolean hasDb) {
        this.hasDb = hasDb;
    }

    public boolean createFeatureClass(VectorLayer layer, List<AttributeEntity> entities) {
        //不存在表才创建表
        if (geoPackage.isFeatureOrTileTable(layer.getName())) {
            return true;
        }
        try {
            //创建表
            mil.nga.geopackage.BoundingBox boundingBox = new mil.nga.geopackage.BoundingBox(-180 + 1, 180 - 1, -90 + 1, 90 - 1);

            GeometryColumns geometryColumns = new GeometryColumns();
            //设置id
            geometryColumns.setId(new TableColumnKey(layer.getName(), "Shape"));
            geometryColumns.setGeometryType(layer.getGeometryType());
            /**
             * 首先构建表的列，然后再构建表对象
             */
            int index = 0;
            List<FeatureColumn> columns = layer.getColumns();
            columns.add(FeatureColumn.createPrimaryKeyColumn(index++, "ID"));
            columns.add(FeatureColumn.createGeometryColumn(index++, geometryColumns.getColumnName(), geometryColumns.getGeometryType(), false, null));

            for (AttributeEntity entity : entities) {
                GeoPackageDataType type = GeoPackageDataType.TEXT;
                if (entity.getType() == String.class) {
                    type = GeoPackageDataType.TEXT;
                } else if (entity.getType() == Integer.class) {
                    type = GeoPackageDataType.INTEGER;
                } else if (entity.getType() == Float.class) {
                    type = GeoPackageDataType.FLOAT;
                }
                columns.add(FeatureColumn.createColumn(index++, entity.getName(), type, false, null));
            }

            if (geoPackage != null)
                geoPackage.createFeatureTableWithMetadata(geometryColumns, boundingBox, ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM, columns);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean createAttrClass(AttributeLayer layer) {
        //不存在表才创建表
        if (geoPackage.isTable(GeopackageConstants.LINE_TABLE)) {
            return true;
        }

        int index = 1;
        List<AttributesColumn> additionalColumns = layer.getColumns();
        additionalColumns.add(AttributesColumn.createColumn(index++, GeopackageConstants.POINT_NAME, GeoPackageDataType.TEXT, false, ""));
        additionalColumns.add(AttributesColumn.createColumn(index++, GeopackageConstants.CODE, GeoPackageDataType.TEXT, false, ""));
        //根据需要在继续添加,等等
        geoPackage.createAttributesTable(GeopackageConstants.LINE_TABLE, GeopackageConstants.FID, additionalColumns);
        return true;
    }

    public Overlay getOverlay(String id, String typeName) {
        if (mapView != null) {
            List<Overlay> overlays = mapView.getOverlays();
            for (Overlay overlay : overlays) {
                if (overlay instanceof VectorLayer && ((VectorLayer) overlay).getGeometryType().getName().equals(typeName)) {
                    VectorLayer vectorLayer = (VectorLayer) overlay;
                    List<Overlay> geomOverlays = vectorLayer.getItems();
                    for (Overlay geomOverlay : geomOverlays) {
                        if (geomOverlay instanceof Marker) {
                            if (((Marker) geomOverlay).getId().equals(id)) {
                                return geomOverlay;
                            }
                        } else if (geomOverlay instanceof Polyline) {
                            if (((Polyline) geomOverlay).getId().equals(id)) {
                                return geomOverlay;
                            }
                        } else if (geomOverlay instanceof Polygon) {
                            if (((Polygon) geomOverlay).getId().equals(id)) {
                                return geomOverlay;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取到图层列表
     *
     * @return
     */
    public List<Overlay> getLayers() {
        List<Overlay> result = new ArrayList<>();
        if (mapView != null) {
            List<Overlay> overlays = mapView.getOverlays();
            for (Overlay overlay : overlays) {
                if (overlay instanceof VectorLayer) {
                    if (!((VectorLayer) overlay).getName().equals("temp"))
                        result.add(overlay);
                } else if (overlay instanceof TileLayer) {
                    result.add(overlay);
                }
            }
        }
        return result;
    }


    public void exportGeopackage() {
        geoPackageManager.exportGeoPackage("data", new File(FilePathManage.getInstance().getMap()).getParentFile().getAbsoluteFile());
    }

    public VectorLayer getFeatureLayerByName(String name) {
        if (mapView != null) {
            List<Overlay> overlays = mapView.getOverlays();
            for (Overlay overlay : overlays) {
                if (overlay instanceof VectorLayer) {
                    if (!((VectorLayer) overlay).getName().equals(name))
                        return (VectorLayer) overlay;
                }
            }
        }
        return null;
    }
}
