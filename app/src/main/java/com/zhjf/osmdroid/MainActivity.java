package com.zhjf.osmdroid;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;
import com.zhjf.osmdroid.adapter.LayerAdapter;
import com.zhjf.osmdroid.common.RequestConstant;
import com.zhjf.osmdroid.common.SerializableMap;
import com.zhjf.osmdroid.fileexplorer.FileSelectActivity;
import com.zhjf.osmdroid.fileexplorer.FileSelectConstant;
import com.zhjf.osmdroid.geopackage.CustomGeoPackageManager;
import com.zhjf.osmdroid.geopackage.FilePathManage;
import com.zhjf.osmdroid.overlay.VectorLayer;
import com.zhjf.osmdroid.permission.PermissionManager;
import com.zhjf.osmdroid.tile.CustomTileSource;
import com.zhjf.osmdroid.tile.MapTileFileProvider;
import com.zhjf.osmdroid.tile.pdf.PDFCacheUtils;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.gpkg.overlay.OsmMapShapeConverter;
import org.osmdroid.gpkg.overlay.features.MarkerOptions;
import org.osmdroid.gpkg.overlay.features.OsmDroidMapShape;
import org.osmdroid.gpkg.overlay.features.PolygonOptions;
import org.osmdroid.gpkg.overlay.features.PolylineOptions;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.Point;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_SELECTOR_REQUEST_CODE = 2016;
    private static final int CREATE_FEATURECLASS_CODE = 2017;
    public static final int SELECTOR_FEATURE_REQUEST_CODE = 2018;
    private static final int ATTR_EDIT_CODE = 2019;

    public static MapView mapView;
    //比例尺
    private ScaleBarOverlay mScaleBarOverlay;
    //指南针方向
    private CompassOverlay mCompassOverlay = null;
    //设置导航图标的位置
    private MyLocationNewOverlay mLocationOverlay;
    private Toolbar toolbar;
    private MenuItem createdFeatureItem;
    public static VectorLayer currentLayer;
    public static OverlayWithIW currentFeature;
    private EditMode editMode = EditMode.SELECT;
    private LayerAdapter layerAdapter;
    private ListView layerListView;
    private List<Overlay> layerOverlays;
    private LinearLayout layerContainer;
    private MyMapEventsOverlay myMapEventsOverlay;
    private MyMapEventsReceiver myMapEventsReceiver;

    private OsmMapShapeConverter converter;
    private MarkerOptions markerOptions;
    private PolylineOptions polylineOptions;
    private PolygonOptions polygonOptions;
    private OsmDroidMapShape mapShape;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);

        PDFBoxResourceLoader.init(getApplicationContext());

        initPermission();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setDrawingCacheEnabled(true);
        mapView.setMaxZoomLevel(20.0);
        mapView.setMinZoomLevel(6.0);
        mapView.getController().setZoom(12);

        mapView.setUseDataConnection(true);
        mapView.setMultiTouchControls(true);// 触控放大缩小
        //是否显示地图数据源
        mapView.getOverlayManager().getTilesOverlay().setEnabled(true);

        //比例尺配置
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        mScaleBarOverlay = new ScaleBarOverlay(mapView);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setAlignBottom(true); //底部显示
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 5, 80);
        mapView.getOverlays().add(this.mScaleBarOverlay);

        //指南针方向
        mCompassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        mCompassOverlay.enableCompass();
        mapView.getOverlays().add(this.mCompassOverlay);

        //设置导航图标
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        mLocationOverlay.enableMyLocation();  //设置可视
        mapView.getOverlays().add(this.mLocationOverlay);
        //设置版权信息
        mapView.getOverlays().add(new CopyrightOverlay(this));

        myMapEventsReceiver = new MyMapEventsReceiver(null);
        myMapEventsOverlay = new MyMapEventsOverlay(MainActivity.this, myMapEventsReceiver);
        mapView.getOverlays().add(myMapEventsOverlay);

        buildTemplateDrawOption();

        /////////////////////////////
        findViewById(R.id.tool_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editMode = EditMode.DELETE;
            }
        });

        findViewById(R.id.tool_select).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editMode = EditMode.SELECT;
            }
        });
    }

    private void buildTemplateDrawOption() {
        markerOptions = new MarkerOptions();
        markerOptions.setIcon(this.getResources().getDrawable(R.drawable.ic_default_marker));
        polylineOptions = new PolylineOptions();
        polylineOptions.setWidth(4);
        polylineOptions.setColor(Color.YELLOW);
        polygonOptions = new PolygonOptions();
        polygonOptions.setStrokeWidth(2);
        polygonOptions.setStrokeColor(Color.BLACK);
        polygonOptions.setFillColor(0x8032B5EB);
        converter = new OsmMapShapeConverter(ProjectionFactory.getProjection(4326), markerOptions, polylineOptions, polygonOptions);
    }


    private void initPermission() {
        //同时申请多个权限
        PermissionManager.getInstance(getApplicationContext()).execute(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_WIFI_STATE,
                android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.getInstance(getApplicationContext()).onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        createdFeatureItem = menu.findItem(R.id.action_add);
        createdFeatureItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.action_search:
                break;
            case R.id.action_add:
                if (currentLayer != null && mapShape != null) {
                    if (points != null) points.clear();
                    intent = new Intent(MainActivity.this, AttributeEditActivity.class);
                    startActivityForResult(intent, ATTR_EDIT_CODE);
                    createdFeatureItem.setVisible(false);
                }
                break;
            case R.id.action_layers:
                try {
                    initAndShowLayerView();
                } catch (Exception e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                break;
            case R.id.action_select_workspace:
                intent = new Intent();
                intent.setClass(getApplicationContext(), FileSelectActivity.class);
                intent.putExtra(FileSelectConstant.SELECTOR_REQUEST_CODE_KEY, FileSelectConstant.SELECTOR_MODE_FOLDER);
                startActivityForResult(intent, FILE_SELECTOR_REQUEST_CODE);
                break;
            case R.id.action_create_featureclass:
                intent = new Intent(this, FeatureClassCreateActivity.class);
                startActivityForResult(intent, CREATE_FEATURECLASS_CODE);
                break;
            case R.id.action_save_geopackage:
                try {
                    CustomGeoPackageManager.getInstance(this).exportGeopackage();
                    Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                break;
            case R.id.action_save_photo:
//                intent = new Intent();
//                intent.setClass(getApplicationContext(), com.zhjf.osmdroid.takephoto.MainActivity.class);
//                startActivityForResult(intent, FILE_SELECTOR_REQUEST_CODE);
                break;
            case R.id.action_create_tile:
                try {
                    String mapName = FilePathManage.getInstance().getMap();
                    File targetCacheFile = new File(FilePathManage.getInstance().getCacheDirectory(), new File(mapName).getName());
                    if (!targetCacheFile.exists()) {
                        PDFCacheUtils utils = new PDFCacheUtils(MainActivity.this);
                        PDFCacheUtils.PDFInfo pdfInfo = utils.getPDFInfo(new File(FilePathManage.getInstance().getMap()));
//                        utils.createCache3(new File(mapName).getName(), pdfInfo.envelope, 15, 16, pdfInfo.bitmap);
                        utils.createCache4(new File(mapName).getName(), pdfInfo.envelope, 15, 16, pdfInfo.bitmap);
                    }
                    CustomTileSource source = new CustomTileSource(targetCacheFile.getAbsolutePath(), 15, 16, 256, ".png");
                    MapTileModuleProviderBase moduleProvider = new MapTileFileProvider(source);
                    SimpleRegisterReceiver simpleReceiver = new SimpleRegisterReceiver(getApplicationContext());
                    MapTileProviderArray tileProviderArray = new MapTileProviderArray(source, simpleReceiver, new MapTileModuleProviderBase[]{moduleProvider});
                    TilesOverlay tilesOverlay = new TilesOverlay(tileProviderArray, getApplicationContext());

                    mapView.getOverlays().add(tilesOverlay);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            case R.id.action_open_shape:
                try {
                    readShp(FilePathManage.getInstance().getRootDir() + "//shp//poly.shp");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.action_read_pdf:
                try {
                    readPdf(FilePathManage.getInstance().getRootDir() + "//home.pdf");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void readPdf(String pdfPath) throws Exception {
        gdal.AllRegister();
        Dataset dataset = gdal.Open(pdfPath);
        if (dataset == null) {
            throw new Exception(gdal.GetLastErrorNo() + ":" + gdal.GetLastErrorMsg());
        }

        int rasterCount = dataset.getRasterCount();
        System.out.println(rasterCount);
    }

    // 读取shp
    private void readShp(String shpPath) throws UnsupportedEncodingException {
        // 注册所有的驱动
        ogr.RegisterAll();
//        String encoding = gdal.GetConfigOption("SHAPE_ENCODING", null);
        // 为了支持中文路径，请添加下面这句代码
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "NO");
        // 为了使属性表字段支持中文，请添加下面这句
        gdal.SetConfigOption("SHAPE_ENCODING", "UTF-8");

        //打开文件
        DataSource ds = ogr.Open(shpPath, 0);
        if (ds == null) {
            System.out.println("打开文件失败！");
            return;
        }
        System.out.println("打开文件成功！");

        // 获取该数据源中的图层个数，一般shp数据图层只有一个，如果是mdb、dxf等图层就会有多个
//        int iLayerCount = ds.GetLayerCount();
        // 获取第一个图层
        Layer oLayer = ds.GetLayerByIndex(0);
        if (oLayer == null) {
            System.out.println("获取第0个图层失败！\n");
            return;
        }

        // 对图层进行初始化，如果对图层进行了过滤操作，执行这句后，之前的过滤全部清空
        oLayer.ResetReading();
        // 通过属性表的SQL语句对图层中的要素进行筛选，这部分详细参考SQL查询章节内容
        //oLayer.SetAttributeFilter("\"NAME99\"LIKE \"北京市市辖区\"");
        // 通过指定的几何对象对图层中的要素进行筛选
        //oLayer.SetSpatialFilter();
        // 通过指定的四至范围对图层中的要素进行筛选
        //oLayer.SetSpatialFilterRect();

        // 获取图层中的属性表表头并输出
        FeatureDefn oDefn = oLayer.GetLayerDefn();
        int iFieldCount = oDefn.GetFieldCount();
        for (int iAttr = 0; iAttr < iFieldCount; iAttr++) {
            FieldDefn oField = oDefn.GetFieldDefn(iAttr);

            String content = oField.GetNameRef() + ": " + oField.GetFieldTypeName(oField.GetFieldType()) + "(" + oField.GetWidth() + "." + oField.GetPrecision() + ")";
            System.out.println(content);
        }

        // 输出图层中的要素个数
        System.out.println("要素个数 = " + oLayer.GetFeatureCount(0));

        Feature oFeature = null;
        // 下面开始遍历图层中的要素
        while ((oFeature = oLayer.GetNextFeature()) != null) {
            System.out.println("当前处理第" + oFeature.GetFID() + "个:\n属性值：");
            // 获取要素中的属性表内容
            for (int iField = 0; iField < iFieldCount; iField++) {
                FieldDefn oFieldDefn = oDefn.GetFieldDefn(iField);
                int type = oFieldDefn.GetFieldType();

                switch (type) { // 只支持下面四种
                    case ogr.OFTString:
                        System.out.println(oFeature.GetFieldAsString(iField) + "\t");
                        break;
                    case ogr.OFTReal:
                        System.out.println(oFeature.GetFieldAsDouble(iField) + "\t");
                        break;
                    case ogr.OFTInteger:
                        System.out.println(oFeature.GetFieldAsInteger(iField) + "\t");
                        break;
                    case ogr.OFTDate:
//                        oFeature.GetFieldAsDateTime();
                        break;
                    default:
                        System.out.println(oFeature.GetFieldAsString(iField) + "\t");
                        break;
                }
            }

            // 获取要素中的几何体
            Geometry oGeometry = oFeature.GetGeometryRef();
            System.out.println(oGeometry.ExportToJson());
        }

        System.out.println("数据集关闭！");

    }

    private void initAndShowLayerView() throws Exception {
        layerContainer = (LinearLayout) findViewById(R.id.layer_list_container);
        findViewById(R.id.layer_list_container_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layerContainer.setVisibility(View.GONE);
            }
        });
        layerContainer.setVisibility(View.VISIBLE);
        layerOverlays = CustomGeoPackageManager.getInstance(this).getLayers();
        layerListView = (ListView) findViewById(R.id.layers_vector_list);
        layerAdapter = new LayerAdapter(layerOverlays, this, new LayerAdapter.VisibleChangeCallback() {
            @Override
            public void setVisible(Overlay overlay, Boolean isVisible) {
                overlay.setEnabled(isVisible);
                mapView.invalidate();
            }

            @Override
            public void setEditable(Overlay overlay, Boolean isEditable) {
                if (overlay instanceof VectorLayer) {
                    VectorLayer vectorLayer = (VectorLayer) overlay;
                    vectorLayer.setEdit(isEditable);
                    if (isEditable) {
                        editMode = EditMode.DRAW;
                        findViewById(R.id.tool_container).setVisibility(View.VISIBLE);
                        currentLayer = vectorLayer;
                    } else {
                        findViewById(R.id.tool_container).setVisibility(View.GONE);
                        editMode = EditMode.NULL;
                    }
                }
                layerContainer.setVisibility(View.GONE);
            }
        });
        layerListView.setAdapter(layerAdapter);
    }

    private mil.nga.wkb.geom.Polygon polygon;
    private LineString lineString;
    private List<Point> points;

    private class MyMapEventsOverlay extends MapEventsOverlay {

        private MapEventsReceiver receiver;

        public MyMapEventsOverlay(Context ctx, MapEventsReceiver receiver) {
            super(ctx, receiver);
            this.receiver = receiver;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
            Projection proj = mapView.getProjection();
            GeoPoint p = (GeoPoint) proj.fromPixels((int) e.getX(), (int) e.getY());
            if (currentLayer != null && (currentLayer.getGeometryType() == GeometryType.POINT || currentLayer.getGeometryType() == GeometryType.POLYGON)) {
                if (editMode == EditMode.DELETE) {
                    List<OverlayWithIW> deletedList = currentLayer.hitTest(e, mapView);
                    currentLayer.getItems().removeAll(deletedList);
                    return true;
                } else if (editMode == EditMode.SELECT) {
                    List<OverlayWithIW> selectList = currentLayer.hitTest(e, mapView);
                    if (selectList.size() > 0) {
                        editAttrs(selectList.get(0));
                    }
                }
            }
            return receiver.singleTapConfirmedHelper(p);
        }
    }

    private class MyMapEventsReceiver implements MapEventsReceiver {

        public MyMapEventsReceiver(VectorLayer vectorLayer) {
            currentLayer = vectorLayer;
        }

        @Override
        public boolean singleTapConfirmedHelper(GeoPoint p) {
            if (currentLayer != null) {
                if (points == null) points = new ArrayList<>();
                if (currentLayer.getGeometryType() == GeometryType.POINT) {
                    if (editMode == EditMode.DRAW) {
                        if (mapShape != null) mapView.getOverlays().remove(mapShape.getShape());
                        mapShape = converter.addToMap(mapView, new Point(false, false, p.getLongitude(), p.getLatitude()));
                        mapView.invalidate();
                        createdFeatureItem.setVisible(true);
                    }
                    mapView.invalidate();
                } else if (currentLayer.getGeometryType() == GeometryType.LINESTRING) {
                    if (editMode == EditMode.DELETE) {
                        List<OverlayWithIW> deletedList = currentLayer.hitTestLine(p, mapView);
                        currentLayer.getItems().removeAll(deletedList);
                    } else if (editMode == EditMode.DRAW) {
                        if (mapShape != null) mapView.getOverlays().remove(mapShape.getShape());
                        if (lineString == null) {
                            lineString = new LineString(false, false);
                        }
                        points.add(new Point(p.getLongitude(), p.getLatitude()));
                        lineString.setPoints(points);
                        mapShape = converter.addToMap(mapView, lineString);
                        if (points.size() > 1)
                            createdFeatureItem.setVisible(true);
                    } else if (editMode == EditMode.SELECT) {
                        List<OverlayWithIW> selectedOverlays = currentLayer.hitTestLine(p, mapView);
                        if (selectedOverlays.size() > 0) {
                            editAttrs(selectedOverlays.get(0));
                        }
                    }
                    mapView.invalidate();
                } else if (currentLayer.getGeometryType() == GeometryType.POLYGON) {
                    if (editMode == EditMode.DRAW) {
                        if (mapShape != null) mapView.getOverlays().remove(mapShape.getShape());
                        if (polygon == null) {
                            polygon = new mil.nga.wkb.geom.Polygon(false, false);
                        }
                        if (lineString == null) {
                            lineString = new LineString(false, false);
                        }
                        points.add(new Point(p.getLongitude(), p.getLatitude()));
                        lineString.setPoints(points);
                        polygon.addRing(lineString);
                        if (mapShape != null) mapShape.getShape();
                        mapShape = converter.addToMap(mapView, polygon);
                        if (points.size() > 2)
                            createdFeatureItem.setVisible(true);
                    }
                    mapView.invalidate();
                }
            }
            return true;
        }

        @Override
        public boolean longPressHelper(GeoPoint p) {
            return true;
        }
    }

    /**
     * 打开编辑属性窗口
     *
     * @param overlay
     */
    private void editAttrs(OverlayWithIW overlay) {
        currentFeature = overlay;
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), AttributeEditActivity.class);
        if (overlay instanceof Marker) {
            intent.putExtra(RequestConstant.SELECTOR_FEATURE_ID, ((Marker) overlay).getId());
        } else if (overlay instanceof Polyline) {
            intent.putExtra(RequestConstant.SELECTOR_FEATURE_ID, ((Polyline) overlay).getId());
        } else if (overlay instanceof org.osmdroid.views.overlay.Polygon) {
            intent.putExtra(RequestConstant.SELECTOR_FEATURE_ID, ((org.osmdroid.views.overlay.Polygon) overlay).getId());
        }
        startActivityForResult(intent, SELECTOR_FEATURE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle bundle;
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case FILE_SELECTOR_REQUEST_CODE:
                    List<String> result = data.getStringArrayListExtra(FileSelectConstant.SELECTOR_BUNDLE_PATHS);
                    if (result.size() > 0) {
                        String wp = result.get(0);
                        FilePathManage.getInstance().setRootPath(wp);
                        try {
                            CustomGeoPackageManager.getInstance(this).setMapView(mapView);
                            CustomGeoPackageManager.getInstance(this).initMap();
                            findViewById(R.id.bg_view).setVisibility(View.GONE);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        mapView.setMaxZoomLevel(18.0);
                        mapView.setMinZoomLevel(0.0);
                        mapView.getController().setZoom(12);
                        mapView.getController().setCenter(new GeoPoint(24.14525, 98.70748));
                    }
                    break;
                case CREATE_FEATURECLASS_CODE:
                    bundle = data.getExtras();
                    String name = (String) bundle.get(FeatureClassCreateActivity.CREATED_TABLE_NAME);
                    Toast.makeText(this, "要素类：" + name + "创建成功", Toast.LENGTH_SHORT).show();
                    break;
                case ATTR_EDIT_CODE:
                    bundle = data.getExtras();
                    SerializableMap serializableMap = (SerializableMap) bundle.get("map");
                    Map<String, Object> map = serializableMap.getMap();
                    if (currentLayer != null && mapShape != null) {
                        currentLayer.restore(mapShape, map);
                    }
                    break;
                default:
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private enum EditMode {
        DRAW(0), DELETE(1), SELECT(2), NULL(3);
        private int value;

        EditMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}