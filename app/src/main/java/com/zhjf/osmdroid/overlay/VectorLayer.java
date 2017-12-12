package com.zhjf.osmdroid.overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.widget.Toast;

import com.zhjf.osmdroid.R;
import com.zhjf.osmdroid.common.DensityUtil;
import com.zhjf.osmdroid.entity.style.Style;
import com.zhjf.osmdroid.geopackage.CustomGeoPackageManager;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.gpkg.overlay.OsmMapShapeConverter;
import org.osmdroid.gpkg.overlay.features.MarkerOptions;
import org.osmdroid.gpkg.overlay.features.OsmDroidMapShape;
import org.osmdroid.gpkg.overlay.features.PolygonOptions;
import org.osmdroid.gpkg.overlay.features.PolylineOptions;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions;
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryEnvelope;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;
import mil.nga.wkb.util.GeometryEnvelopeBuilder;

import static mil.nga.tiff.FieldTagType.DateTime;

/**
 * Created by Administrator on 2017/11/12.
 */

public class VectorLayer extends FolderOverlay {
    private GeometryType geometryType;
    private boolean isEdit = false;
    private Style style;
    private FeatureColumn labeledColumn;
    private List<FeatureColumn> columns;
    private MapView mapView;
    private Context context;
    private GeoPackage geoPackage;
    private OsmMapShapeConverter converter;
    private MarkerOptions markerOptions;
    private PolylineOptions polylineOptions;
    private PolygonOptions polygonOptions;

    public VectorLayer(MapView mapView, Context context, String name) {
        super();
        this.mapView = mapView;
        this.context = context;
        this.mName = name;
    }

    public void setConverter(OsmMapShapeConverter converter) {
        this.converter = converter;
    }

    public void buildOverlays() {
        if (converter == null) buildDrawOptions();
        FeatureDao featureDao;
        FeatureCursor featureCursor = null;
        try {
            geoPackage = CustomGeoPackageManager.getInstance(context).getGeoPackage();
            featureDao = geoPackage.getFeatureDao(getName());
            featureCursor = featureDao.queryForAll();
            while (featureCursor.moveToNext()) {
                FeatureRow featureRow = featureCursor.getRow();
                Geometry geometry = featureRow.getGeometry().getGeometry();
                if (geometry instanceof Point) {
                    Point geoPoint = (Point) featureRow.getGeometry().getGeometry();
                    Marker marker = new Marker(mapView);
                    marker.setPosition(new GeoPoint(geoPoint.getY(), geoPoint.getX()));
                    add(marker);
                } else if (geometry instanceof LineString) {
                    LineString lineString = (LineString) geometry;
                    add(converter.toPolyline(lineString));
                } else if (geometry instanceof Polygon) {
                    add(converter.toPolygon((Polygon) geometry));
                } else {
                }
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (featureCursor != null) {
                featureCursor.close();
            }
        }
    }

    private void buildDrawOptions() {
        markerOptions = new MarkerOptions();
        markerOptions.setIcon(context.getResources().getDrawable(R.drawable.ic_default_marker));
        polylineOptions = new PolylineOptions();
        polylineOptions.setWidth(4);
        polylineOptions.setColor(Color.YELLOW);
        polygonOptions = new PolygonOptions();
        polygonOptions.setStrokeWidth(2);
        polygonOptions.setStrokeColor(Color.BLACK);
        polygonOptions.setFillColor(0x8032B5EB);
        converter = new OsmMapShapeConverter(ProjectionFactory.getProjection(4326), markerOptions, polylineOptions, polygonOptions);
    }

    public OverlayWithIW buildOverlay(Geometry geometry) {
        if (geometry instanceof Point) {
            Marker marker = new Marker(mapView, context);
            marker.setIcon(markerOptions.getIcon());
            return marker;
        } else if (geometry instanceof LineString) {
            return converter.toPolyline((LineString) geometry);
        } else if (geometry instanceof Polygon) {
            return converter.toPolygon((Polygon) geometry);
        }
        return null;
    }

    public GeometryType getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryType geometryType) {
        this.geometryType = geometryType;
    }

    public boolean isEdit() {
        return isEdit;
    }

    public void setEdit(boolean edit) {
        isEdit = edit;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public List<FeatureColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<FeatureColumn> columns) {
        this.columns = columns;
    }

    public FeatureColumn getLabeledColumn() {
        return labeledColumn;
    }

    public void setLabeledColumn(FeatureColumn labeledColumn) {
        this.labeledColumn = labeledColumn;
    }

    /**
     * 线选择
     *
     * @param point
     * @param mapView
     * @return
     */
    public List<OverlayWithIW> hitTestLine(GeoPoint point, MapView mapView) {
        List<OverlayWithIW> result = new ArrayList<>();
        for (Overlay overlay : getItems()) {
            if (geometryType == GeometryType.LINESTRING) {
                Polyline polyline = (Polyline) overlay;
                if (polyline.isCloseTo(point, 4, mapView)) {
                    result.add(polyline);
                }
            }
        }
        return result;
    }

    /**
     * 点、面选择
     *
     * @param event
     * @param mapView
     * @return
     */
    public List<OverlayWithIW> hitTest(MotionEvent event, MapView mapView) {
        List<OverlayWithIW> result = new ArrayList<>();
        for (Overlay overlay : getItems()) {
            if (geometryType == GeometryType.POINT) {
                Marker marker = (Marker) overlay;
                if (marker.hitTest(event, mapView)) {
                    result.add(marker);
                }
            } else if (geometryType == GeometryType.POLYGON) {
                org.osmdroid.views.overlay.Polygon polygon = (org.osmdroid.views.overlay.Polygon) overlay;
                if (polygon.contains(event)) {
                    result.add(polygon);
                }
            }
        }
        return result;
    }

    /**
     * 保存编辑的点
     *
     * @param attrs
     * @return
     */
    public long restore(OsmDroidMapShape osmDroidMapShape, Map<String, Object> attrs) {
        FeatureDao featureDao = CustomGeoPackageManager.getInstance(context).getGeoPackage().getFeatureDao(this.getName());
        GeoPackageGeometryData geomData = new GeoPackageGeometryData(featureDao.getGeometryColumns().getSrsId());
        Geometry geometry = null;
        if (osmDroidMapShape.getGeometryType() == GeometryType.POINT) {
            Marker marker = (Marker) osmDroidMapShape.getShape();
            geometry = new Point(false, false, marker.getPosition().getLongitude(), marker.getPosition().getLatitude());
        } else if (osmDroidMapShape.getGeometryType() == GeometryType.LINESTRING) {
            Polyline polyline = (Polyline) osmDroidMapShape.getShape();
            LineString lineString = new LineString(false, false);
            lineString.setPoints(toGeomPoints(polyline.getPoints()));
            geometry = lineString;
        } else if (osmDroidMapShape.getGeometryType() == GeometryType.POLYGON) {
            org.osmdroid.views.overlay.Polygon polygon = (org.osmdroid.views.overlay.Polygon) osmDroidMapShape.getShape();
            Polygon polygon1 = new Polygon(false, false);
            LineString lineString = new LineString(false, false);
            lineString.setPoints(toGeomPoints(polygon.getPoints()));
            polygon1.addRing(lineString);
            geometry = polygon1;
        }
        geomData.setGeometry(geometry);
        FeatureRow row = featureDao.newRow();
        row.setGeometry(geomData);
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            row.setValue(entry.getKey(), entry.getValue());
        }
        return featureDao.insert(row);
    }

    public long update(Geometry geometry, Map<String, Object> attrs) {
        FeatureDao featureDao = CustomGeoPackageManager.getInstance(context).getGeoPackage().getFeatureDao(this.getName());
        FeatureRow row = featureDao.newRow();
        if (geometry != null) {
            GeoPackageGeometryData geomData = new GeoPackageGeometryData(featureDao.getGeometryColumns().getSrsId());
            geomData.setGeometry(geometry);
            row.setGeometry(geomData);
        }
        if (attrs != null)
            for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                row.setValue(entry.getKey(), entry.getValue());
            }
        return featureDao.update(row);
    }

    private List<Point> toGeomPoints(List<GeoPoint> points) {
        List<Point> result = new ArrayList<>();
        for (GeoPoint p : points) {
            result.add(new Point(false, false, p.getLongitude(), p.getLatitude()));
        }
        return result;
    }
}
