package com.zhjf.osmdroid.geopackage;


import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.geopackage.schema.TableColumnKey;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.Point;

/**
 * 点的操作类
 */
public class PointGeoOperation extends GeoOperationBase {

    private FeatureDao mPointTableDao;

    public PointGeoOperation(GeoPackage geoPackage) {
        super(geoPackage);
    }

    public void init() {
        if (mGeoPackage != null)
            mPointTableDao = mGeoPackage.getFeatureDao(GeopackageConstants.POINT_TABLE);
    }

    public FeatureDao getSurveyDao() {
        return mPointTableDao;
    }

    public boolean createTable() {
        //不存在表才创建表
        if (mGeoPackage.isFeatureOrTileTable(GeopackageConstants.POINT_TABLE)) {
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
            geometryColumns.setId(new TableColumnKey(GeopackageConstants.POINT_TABLE, "geom"));
            //todo GeometryType 可以选择 点 线 面 多边形等等
            geometryColumns.setGeometryType(GeometryType.POINT);
            geometryColumns.setZ((byte) 1);

            /**
             * 首先构建表的列，然后再构建表对象
             */
            int index = 0;
            List<FeatureColumn> columns = new ArrayList<FeatureColumn>();
            columns.add(FeatureColumn.createPrimaryKeyColumn(index++, GeopackageConstants.FID));
            columns.add(FeatureColumn.createGeometryColumn(index++, geometryColumns.getColumnName(), geometryColumns.getGeometryType(), false, null));
            columns.add(FeatureColumn.createColumn(index++, GeopackageConstants.POINT_NAME, GeoPackageDataType.TEXT, true, ""));
            columns.add(FeatureColumn.createColumn(index++, GeopackageConstants.CODE, GeoPackageDataType.TEXT, true, ""));

            if (mGeoPackage != null)
                mGeoPackage.createFeatureTableWithMetadata(geometryColumns, boundingBox, ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM, columns);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean deleteTable() {
        if (mGeoPackage != null)
            mGeoPackage.deleteTable(GeopackageConstants.POINT_TABLE);
        return true;
    }

    public boolean insertData(Geometry geometry, ContentValues values) {
        try {
            GeoPackageGeometryData geomData = new GeoPackageGeometryData(mPointTableDao.getGeometryColumns().getSrsId());
            geomData.setGeometry(geometry);
            FeatureRow row = mPointTableDao.newRow();
            row.setGeometry(geomData);
            row.setValue(GeopackageConstants.POINT_NAME, values.getAsString(GeopackageConstants.POINT_NAME));
            row.setValue(GeopackageConstants.CODE, values.getAsString(GeopackageConstants.CODE));
            mPointTableDao.insert(row);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean deleteData(int ID) {
        String queSQL = String.format(Locale.ENGLISH, GeopackageConstants.FID + "= %d", ID);
        return mPointTableDao.delete(queSQL, null) > 0;
    }

    private List<ContentValues> getAllSurveyPoint() {
        List<ContentValues> arrayslist = new ArrayList<ContentValues>();
        if (mPointTableDao != null) {
            FeatureCursor cursor = mPointTableDao.queryForAll();
            while (cursor.moveToNext()) {
                ContentValues contentValues = cursor.getRow().toContentValues();
                arrayslist.add(0, contentValues);
            }
        }
        return arrayslist;
    }

    public List<FeatureRow> getAllPoint() {
        List<FeatureRow> arrayslist = new ArrayList<FeatureRow>();
        if (mPointTableDao != null) {
            FeatureCursor cursor = mPointTableDao.queryForAll();
            while (cursor.moveToNext()) {
                FeatureRow row = cursor.getRow();
                Point point = (Point) row.getGeometry().getGeometry();
                arrayslist.add(row);
            }
        }
        return arrayslist;
    }

    public void updateWithID(int ID, ContentValues values) {
        String queSQL = String.format(Locale.ENGLISH, GeopackageConstants.FID + "= %d", ID);
        if (mPointTableDao != null)
            mPointTableDao.update(values, queSQL, null);
    }

    public boolean updateGeomtry(FeatureRow row) {
        return mPointTableDao.update(row) > 0;
    }
}
