package com.zhjf.osmdroid.geopackage;

import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.attributes.AttributesColumn;
import mil.nga.geopackage.attributes.AttributesCursor;
import mil.nga.geopackage.attributes.AttributesDao;
import mil.nga.geopackage.attributes.AttributesRow;
import mil.nga.geopackage.db.GeoPackageDataType;

/**
 * 线操作类
 */
public class LineGeoOperation extends GeoOperationBase {

    //普通表
    private AttributesDao commonDao;

    public LineGeoOperation(GeoPackage geoPackage) {
        super(geoPackage);
    }

    public void init() {
        commonDao = mGeoPackage.getAttributesDao(GeopackageConstants.LINE_TABLE);
    }

    public AttributesDao getSurveyDao() {
        return commonDao;
    }

    public boolean createTable() {
        //不存在表才创建表
        if (mGeoPackage.isTable(GeopackageConstants.LINE_TABLE)) {
            return true;
        }

        int index = 1;
        List<AttributesColumn> additionalColumns = new ArrayList<>();
        additionalColumns.add(AttributesColumn.createColumn(index++, GeopackageConstants.POINT_NAME, GeoPackageDataType.TEXT, false, ""));
        additionalColumns.add(AttributesColumn.createColumn(index++, GeopackageConstants.CODE, GeoPackageDataType.TEXT, false, ""));
        //根据需要在继续添加,等等
        mGeoPackage.createAttributesTable(GeopackageConstants.LINE_TABLE, GeopackageConstants.FID, additionalColumns);
        return true;
    }

    public boolean deleteTable() {
        mGeoPackage.deleteTable(GeopackageConstants.POINT_TABLE);
        return true;
    }

    public boolean insertData(ContentValues values) {
        try {
            AttributesRow row = commonDao.newRow();
            row.setValue(GeopackageConstants.POINT_NAME, values.getAsString(GeopackageConstants.POINT_NAME));
            row.setValue(GeopackageConstants.CODE, values.getAsString(GeopackageConstants.CODE));
            long fid = commonDao.insert(row);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean deleteData(int ID) {
        String queSQL = String.format(Locale.ENGLISH, GeopackageConstants.FID + "= %d", ID);
        return commonDao.delete(queSQL, null) > 0;
    }

    private List<ContentValues> getAllSurveyPoint() {
        AttributesCursor cursor = commonDao.queryForAll();
        List<ContentValues> arrayslist = new ArrayList<ContentValues>();
        while (cursor.moveToNext()) {
            ContentValues contentValues = cursor.getRow().toContentValues();
            arrayslist.add(0, contentValues);
        }
        return arrayslist;
    }

    public List<AttributesRow> getAllPoint() {
        AttributesCursor cursor = commonDao.queryForAll();
        List<AttributesRow> arrayslist = new ArrayList<AttributesRow>();
        while (cursor.moveToNext()) {
            AttributesRow row = cursor.getRow();
            arrayslist.add(row);
        }
        return arrayslist;
    }

    public void updateWithID(int ID, ContentValues values) {
        String queSQL = String.format(Locale.ENGLISH, GeopackageConstants.FID + "= %d", ID);
        commonDao.update(values, queSQL, null);
    }

    public boolean updateAttributesRow(AttributesRow row) {
        return commonDao.update(row) > 0;
    }
}
