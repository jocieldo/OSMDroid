package com.zhjf.osmdroid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zhjf.osmdroid.adapter.AttributeListAdapter;
import com.zhjf.osmdroid.common.DensityUtil;
import com.zhjf.osmdroid.entity.AttributeEntity;
import com.zhjf.osmdroid.entity.style.Style;
import com.zhjf.osmdroid.geopackage.CustomGeoPackageManager;
import com.zhjf.osmdroid.overlay.VectorLayer;
import com.zhjf.osmdroid.view.ColorBar;

import org.osmdroid.gpkg.overlay.OsmMapShapeConverter;
import org.osmdroid.gpkg.overlay.features.MarkerOptions;
import org.osmdroid.gpkg.overlay.features.PolygonOptions;
import org.osmdroid.gpkg.overlay.features.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.wkb.geom.GeometryType;

/**
 * Created by Administrator on 2017/9/19.
 */

public class FeatureClassCreateActivity extends AppCompatActivity {
    public static final String CREATED_TABLE_ATTRS = "CREATED_TABLE_ATTRS";
    private EditText featureClassName;
    private Spinner featureType;
    private ColorBar featureColor;
    private SeekBar featureSize;
    private SeekBar featureWidth;
    private ColorBar featureFillColor;
    private SeekBar featureOutlineWidth;
    private ColorBar featureOutlineColor;
    private ImageView addAttrs;
    private ListView attrsList;
    private EditText addAttrName;
    private Spinner addAttrDataType;
    private List<AttributeEntity> attributeEntityList;
    private AttributeListAdapter attributeListAdapter;

    public static String CREATED_TABLE_NAME = "CREATED_TABLE_NAME";
    private VectorLayer layer;
    private MarkerOptions markerOptions;
    private PolylineOptions polylineOptions;
    private PolygonOptions polygonOptions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fclass_create);

        Toolbar toolbar = (Toolbar) findViewById(R.id.child_toolbar);
        setSupportActionBar(toolbar);
        layer = new VectorLayer(MainActivity.mapView, this, null);

        featureClassName = (EditText) findViewById(R.id.fclass_name);
        featureType = (Spinner) findViewById(R.id.fclass_type);
        featureColor = (ColorBar) findViewById(R.id.fclass_color);
        featureSize = (SeekBar) findViewById(R.id.fclass_size);
        featureWidth = (SeekBar) findViewById(R.id.fclass_width);
        featureFillColor = (ColorBar) findViewById(R.id.fclass_fillcolor);
        featureOutlineWidth = (SeekBar) findViewById(R.id.fclass_outlinewidth);
        featureOutlineColor = (ColorBar) findViewById(R.id.fclass_outlinecolor);
        addAttrs = (ImageView) findViewById(R.id.create_fclass_add);
        attrsList = (ListView) findViewById(R.id.create_fclass_attr_list);
        addAttrName = (EditText) findViewById(R.id.create_fclass_add_name);
        addAttrDataType = (Spinner) findViewById(R.id.create_fclass_add_type);
        attributeEntityList = new ArrayList<>();
        attributeListAdapter = new AttributeListAdapter(this, attributeEntityList);
        attrsList.setAdapter(attributeListAdapter);
        addAttrs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!addAttrName.getText().toString().isEmpty() && addAttrDataType.getSelectedItem() != null) {
                    AttributeEntity entity = new AttributeEntity();
                    entity.setName(String.valueOf(addAttrName.getText()));
                    String type = addAttrDataType.getSelectedItem().toString();
                    if ("TEXT".equals(type)) {
                        entity.setType(String.class);
                    } else if ("INTEGER".equals(type)) {
                        entity.setType(Integer.class);
                    } else if ("REAL".equals(type)) {
                        entity.setType(Float.class);
                    } else if ("TEXT".equals(type)) {
                        entity.setType(String.class);
                    } else if ("PHOTO".equals(type) || "AUDIO".equals(type) || "VIDEO".equals(type)) {
                        entity.setType(String.class);
                    }
                    attributeEntityList.add(entity);
                    attributeListAdapter.notifyDataSetChanged();
                    addAttrName.setText("");
                }
            }
        });

        featureType.setOnItemSelectedListener(new SpinnerFClassTypeSelectedListener());

        featureSize.setOnSeekBarChangeListener(new MySeckBarChangeLinstener());
        featureWidth.setOnSeekBarChangeListener(new MySeckBarChangeLinstener());
        featureOutlineWidth.setOnSeekBarChangeListener(new MySeckBarChangeLinstener());

        featureColor.setOnColorChangerListener(new MyColorBarChangeListener(featureColor));
        featureFillColor.setOnColorChangerListener(new MyColorBarChangeListener(featureFillColor));
        featureOutlineColor.setOnColorChangerListener(new MyColorBarChangeListener(featureOutlineColor));
    }

    private class MySeckBarChangeLinstener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switch (seekBar.getId()) {
                case R.id.fclass_size:
                    ((TextView) findViewById(R.id.fclass_size_val)).setText(String.valueOf(progress));
                    break;
                case R.id.fclass_width:
                    ((TextView) findViewById(R.id.fclass_width_val)).setText(String.valueOf(progress));
                    break;
                case R.id.fclass_outlinewidth:
                    ((TextView) findViewById(R.id.fclass_outlinewidth_val)).setText(String.valueOf(progress));
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (layer.getStyle() == null) {
                Style style = new Style();
                layer.setStyle(style);
            }
            switch (seekBar.getId()) {
                case R.id.fclass_size:
                    layer.getStyle().setSize((float) seekBar.getProgress());
                    break;
                case R.id.fclass_width:
                    layer.getStyle().setWidth((float) seekBar.getProgress());
                    break;
                case R.id.fclass_outlinewidth:
                    layer.getStyle().setOutlineWidth((float) seekBar.getProgress());
                    break;
                default:
                    break;
            }
            drawStyle();
        }
    }

    private class MyColorBarChangeListener implements ColorBar.ColorChangeListener {
        private ColorBar colorBar;

        public MyColorBarChangeListener(ColorBar colorBar) {
            this.colorBar = colorBar;
        }

        @Override
        public void colorChange(int color) {
            if (layer.getStyle() == null) {
                Style style = new Style();
                style.setFillColor(color);
                layer.setStyle(style);
            }
            switch (colorBar.getId()) {
                case R.id.fclass_color:
                    layer.getStyle().setColor(color);
                    break;
                case R.id.fclass_fillcolor:
                    layer.getStyle().setFillColor(color);
                    break;
                case R.id.fclass_outlinecolor:
                    layer.getStyle().setOutlineColor(color);
                    break;
                default:
                    break;
            }

            drawStyle();
        }
    }

    private class SpinnerFClassTypeSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String[] types = getResources().getStringArray(R.array.create_fclass_type_array);
            if ("点".equals(types[position])) {
                layer.setGeometryType(GeometryType.POINT);
                findViewById(R.id.fclass_color_container).setVisibility(View.VISIBLE);
                findViewById(R.id.fclass_size_container).setVisibility(View.VISIBLE);
                findViewById(R.id.fclass_width_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_fillcolor_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_outlinewidth_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_outlinecolor_container).setVisibility(View.GONE);
                markerOptions = new MarkerOptions();
            } else if ("线".equals(types[position])) {
                layer.setGeometryType(GeometryType.LINESTRING);
                findViewById(R.id.fclass_color_container).setVisibility(View.VISIBLE);
                findViewById(R.id.fclass_size_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_width_container).setVisibility(View.VISIBLE);
                findViewById(R.id.fclass_fillcolor_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_outlinewidth_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_outlinecolor_container).setVisibility(View.GONE);
                polylineOptions = new PolylineOptions();
            } else if ("面".equals(types[position])) {
                layer.setGeometryType(GeometryType.POLYGON);
                findViewById(R.id.fclass_color_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_size_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_width_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_fillcolor_container).setVisibility(View.VISIBLE);
                findViewById(R.id.fclass_outlinewidth_container).setVisibility(View.VISIBLE);
                findViewById(R.id.fclass_outlinecolor_container).setVisibility(View.VISIBLE);
                polygonOptions = new PolygonOptions();
            } else {
                layer.setGeometryType(GeometryType.POINT);
                findViewById(R.id.fclass_color_container).setVisibility(View.VISIBLE);
                findViewById(R.id.fclass_size_container).setVisibility(View.VISIBLE);
                findViewById(R.id.fclass_width_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_fillcolor_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_outlinewidth_container).setVisibility(View.GONE);
                findViewById(R.id.fclass_outlinecolor_container).setVisibility(View.GONE);
                markerOptions = new MarkerOptions();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private void drawStyle() {
        int size = DensityUtil.dip2px(this, 80);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (layer.getGeometryType() == GeometryType.POINT) {
            canvas.drawColor(Color.WHITE);
            Paint paint = new Paint();
            //去锯齿
            paint.setAntiAlias(true);
            paint.setColor(layer.getStyle().getColor());
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            //绘制圆形
            canvas.drawCircle(size / 2, size / 2, layer.getStyle().getSize(), paint);
            ((ImageView) findViewById(R.id.style_view)).setImageBitmap(bitmap);
        } else if (layer.getGeometryType() == GeometryType.LINESTRING) {

        } else if (layer.getGeometryType() == GeometryType.POLYGON) {

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fclass_create, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create:
                try {
                    if (!featureClassName.getText().toString().isEmpty()) {
                        layer.setName(featureClassName.getText().toString());
                        layer.setColumns(new ArrayList<FeatureColumn>());
                        if (markerOptions != null) {
                            markerOptions.setIcon(FeatureClassCreateActivity.this.getResources().getDrawable(R.drawable.ic_default_marker));
                        }
                        if (polylineOptions != null) {
                            polylineOptions.setColor(layer.getStyle().getColor());
                            polylineOptions.setWidth(layer.getStyle().getWidth());
                        }
                        if (polygonOptions != null) {
                            polygonOptions.setFillColor(layer.getStyle().getFillColor());
                            polygonOptions.setStrokeColor(layer.getStyle().getOutlineColor());
                            polygonOptions.setStrokeWidth(layer.getStyle().getOutlineWidth());
                        }
                        OsmMapShapeConverter converter = new OsmMapShapeConverter(ProjectionFactory.getProjection(4326), markerOptions, polylineOptions, polygonOptions);
                        layer.setConverter(converter);
                        if (CustomGeoPackageManager.getInstance(this).isHasDb()) {
                            if (CustomGeoPackageManager.getInstance(this).createFeatureClass(layer, attributeEntityList)) {
                                MainActivity.mapView.getOverlayManager().add(layer);
                                Intent intent = new Intent();
                                Bundle bundle = new Bundle();
                                bundle.putSerializable(CREATED_TABLE_NAME, layer.getName());
                                intent.putExtras(bundle);
                                setResult(Activity.RESULT_OK, intent);
                                Toast.makeText(this, "创建成功", Toast.LENGTH_SHORT).show();
                                this.finish();
                            }
                        } else {
//                            CustomGeoPackageManager.getInstance(this).createEmptDatabase(new File(""));
//                            CustomGeoPackageManager.getInstance(this).createFeatureClass(layer);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
