package com.zhjf.osmdroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import com.rey.material.widget.EditText;
import com.rey.material.widget.LinearLayout;
import com.zhjf.osmdroid.common.SerializableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.features.user.FeatureColumn;

/**
 * Created by Administrator on 2017/9/17.
 */

public class AttributeEditActivity extends AppCompatActivity {
    private LinearLayout inputInfoContainer;
    private List<EditText> attrControlList;
    private Map<String, Object> attrResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attr_edit);
        inputInfoContainer = (LinearLayout) findViewById(R.id.input_info_container);
        attrControlList = new ArrayList<>();
        for (FeatureColumn column : MainActivity.currentLayer.getColumns()) {
            if (column.getDataType() != null && !column.isPrimaryKey()) {
                if (column.getDataType().getClassType() == Boolean.class) {
                    EditText editText = (EditText) LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null);
                    editText.setHint(column.getName());
                    editText.setTag(column);
                    editText.setInputType(InputType.TYPE_CLASS_TEXT);
                    inputInfoContainer.addView(editText);
                    attrControlList.add(editText);
                } else if (column.getDataType().getClassType() == Byte.class) {
                    EditText editText = (EditText) LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null);
                    editText.setHint(column.getName());
                    editText.setTag(column);
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputInfoContainer.addView(editText);
                    attrControlList.add(editText);
                } else if (column.getDataType().getClassType() == Short.class) {
                    EditText editText = (EditText) LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null);
                    editText.setHint(column.getName());
                    editText.setTag(column);
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputInfoContainer.addView(editText);
                    attrControlList.add(editText);
                } else if (column.getDataType().getClassType() == Integer.class) {
                    EditText editText = (EditText) LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null);
                    editText.setHint(column.getName());
                    editText.setTag(column);
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputInfoContainer.addView(editText);
                    attrControlList.add(editText);
                } else if (column.getDataType().getClassType() == Long.class) {
                    EditText editText = (EditText) LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null);
                    editText.setHint(column.getName());
                    editText.setTag(column);
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputInfoContainer.addView(editText);
                    attrControlList.add(editText);
                } else if (column.getDataType().getClassType() == Float.class) {
                    EditText editText = (EditText) LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null);
                    editText.setHint(column.getName());
                    editText.setTag(column);
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputInfoContainer.addView(editText);
                    attrControlList.add(editText);
                } else if (column.getDataType().getClassType() == Double.class) {
                    EditText editText = (EditText) LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null);
                    editText.setHint(column.getName());
                    editText.setTag(column);
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputInfoContainer.addView(editText);
                    attrControlList.add(editText);
                } else if (column.getDataType().getClassType() == String.class) {
                    EditText editText = (EditText) LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null);
                    editText.setHint(column.getName());
                    editText.setTag(column);
                    editText.setInputType(InputType.TYPE_CLASS_TEXT);
                    inputInfoContainer.addView(editText);
                    attrControlList.add(editText);
                } else if (column.getDataType().getClassType() == byte[].class) {
                    EditText editText = (EditText) LayoutInflater.from(this).inflate(R.layout.edit_text_layout, null);
                    editText.setHint(column.getName());
                    editText.setTag(column);
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputInfoContainer.addView(editText);
                    attrControlList.add(editText);
                }
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.child_toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();

        //使能app bar的导航功能
        ab.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_attr_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                actionAdd();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void actionAdd() {
        attrResult = new HashMap<>();
        for (EditText editText : attrControlList) {
            FeatureColumn featureColumn = (FeatureColumn) editText.getTag();
            Object objVal = null;
            if (featureColumn.getDataType() == GeoPackageDataType.TEXT) {
                objVal = String.valueOf(editText.getText());
            } else if (featureColumn.getDataType() == GeoPackageDataType.FLOAT) {
                objVal = Float.parseFloat(editText.getText().toString());
            } else if (featureColumn.getDataType() == GeoPackageDataType.INTEGER) {
                objVal = Integer.parseInt(editText.getText().toString());
            }
            attrResult.put(featureColumn.getName(), objVal);
        }
        Intent intent = new Intent();
        SerializableMap tmpmap = new SerializableMap();
        tmpmap.setMap(attrResult);
        Bundle bundle = new Bundle();
        bundle.putSerializable("map", tmpmap);
        intent.putExtras(bundle);
        setResult(Activity.RESULT_OK, intent);
        this.finish();
    }
}
