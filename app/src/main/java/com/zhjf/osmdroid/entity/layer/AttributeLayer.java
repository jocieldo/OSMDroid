package com.zhjf.osmdroid.entity.layer;

import android.content.Context;

import java.util.List;

import mil.nga.geopackage.attributes.AttributesColumn;

/**
 * Created by Administrator on 2017/11/11.
 */

public class AttributeLayer {
    private List<AttributesColumn> columns;
    private Context context;

    public AttributeLayer(Context context) {
        this.context = context;
    }

    public List<AttributesColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<AttributesColumn> columns) {
        this.columns = columns;
    }
}
