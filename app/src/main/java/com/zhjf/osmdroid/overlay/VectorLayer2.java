package com.zhjf.osmdroid.overlay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.widget.Toast;

import com.zhjf.osmdroid.R;
import com.zhjf.osmdroid.entity.style.Style;
import com.zhjf.osmdroid.geopackage.CustomGeoPackageManager;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.gpkg.overlay.OsmMapShapeConverter;
import org.osmdroid.gpkg.overlay.features.MarkerOptions;
import org.osmdroid.gpkg.overlay.features.PolygonOptions;
import org.osmdroid.gpkg.overlay.features.PolylineOptions;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions;
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme;

import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.projection.Projection;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryEnvelope;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.geom.LineString;
import mil.nga.wkb.geom.Point;
import mil.nga.wkb.geom.Polygon;
import mil.nga.wkb.util.GeometryEnvelopeBuilder;

/**
 * Created by Administrator on 2017/11/12.
 */

public class VectorLayer2 extends FolderOverlay {
    private GeometryType geometryType;
    private boolean isEdit = false;
    private Style style;
    private FeatureColumn labeledColumn;
    private List<FeatureColumn> columns;
    private BoundingBox boundingBox;
    private MapView mapView;
    private Context context;
    private GeoPackage geoPackage;
    private MarkerOptions markerOptions;
    private PolygonOptions polygonOptions;
    private PolylineOptions polylineOptions;
    private OsmMapShapeConverter converter;
    private Projection projection = ProjectionFactory.getProjection(4326);


    public VectorLayer2(MapView mapView, Context context, String name) {
        super();
        this.mapView = mapView;
        this.context = context;
        this.mName = name;
    }

    public void buildOverlays() {
        FeatureDao featureDao;
        FeatureCursor featureCursor = null;
        try {
            geoPackage = CustomGeoPackageManager.getInstance(context).getGeoPackage();
            featureDao = geoPackage.getFeatureDao(getName());
            if (featureDao.getGeometryType() == GeometryType.POINT) {
                if (markerOptions == null) {
                    initMarkerOption();
                }
                converter = new OsmMapShapeConverter(projection, markerOptions, null, null);
            } else if (featureDao.getGeometryType() == GeometryType.LINESTRING) {
                if (markerOptions == null) {
                    initPolylineOption();
                }
                converter = new OsmMapShapeConverter(projection, markerOptions, null, null);
            } else if (featureDao.getGeometryType() == GeometryType.POLYGON) {
                if (markerOptions == null) {
                    initPolygonOption();
                }
                converter = new OsmMapShapeConverter(projection, markerOptions, null, null);
            } else {
                if (markerOptions == null) {
                    initMarkerOption();
                }
                converter = new OsmMapShapeConverter(projection, markerOptions, null, null);
            }
            featureCursor = featureDao.queryForAll();
            while (featureCursor.moveToNext()) {
                FeatureRow featureRow = featureCursor.getRow();
                Geometry geometry = featureRow.getGeometry().getGeometry();
                if (geometry instanceof Point) {
                    Marker marker = new Marker(mapView);
                    marker.setPosition(converter.toLatLng((Point) geometry));
                    add(marker);
                } else if (geometry instanceof LineString) {
                    add(converter.toPolyline((LineString) geometry));
                } else if (geometry instanceof Polygon) {
                    add(converter.toPolygon((Polygon) geometry));
                } else {
                    Marker marker = new Marker(mapView);
                    marker.setPosition(converter.toLatLng((Point) geometry));
                    add(marker);
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

    private void initPolylineOption() {
        polylineOptions = new PolylineOptions();
        polylineOptions.setColor(Color.RED);
        polylineOptions.setWidth(2);
    }

    private void initMarkerOption() {
        markerOptions = new MarkerOptions();
        markerOptions.setIcon(context.getResources().getDrawable(R.drawable.marker_default));
        markerOptions.setAlpha((float) 1.0);
    }

    private void initPolygonOption() {
        polygonOptions = new PolygonOptions();
        polygonOptions.setFillColor(Color.YELLOW);
        polygonOptions.setStrokeColor(Color.RED);
        polygonOptions.setStrokeWidth(2);
    }

    private void drawLine(FeatureDao featureDao) {
        FeatureCursor featureCursor = featureDao.queryForAll();
        try {
            List<IGeoPoint> labelPoint = new ArrayList<>();
            while (featureCursor.moveToNext()) {
                FeatureRow featureRow = featureCursor.getRow();
                GeoPackageGeometryData geometryData = featureRow.getGeometry();
                Geometry geometry = geometryData.getGeometry();
                LineString lineString = (LineString) geometry;
                Polyline polyline = new Polyline();
                polyline.setWidth(2);
                polyline.setColor(Color.BLUE);
                List<Point> points = lineString.getPoints();
                List<GeoPoint> geoPoints = new ArrayList<>();
                for (Point p : points) {
                    geoPoints.add(new GeoPoint(p.getY(), p.getX()));
                }
                polyline.setPoints(geoPoints);
                mapView.getOverlays().add(polyline);
                GeometryEnvelope envelope = GeometryEnvelopeBuilder.buildEnvelope(geometry);
                IGeoPoint point = new LabelledGeoPoint(envelope.getMinY() + (envelope.getMaxY() - envelope.getMinY()) / 2,
                        envelope.getMinX() + (envelope.getMaxX() - envelope.getMinX()) / 2,
                        featureRow.getValue("name").toString());
                labelPoint.add(point);
            }
            addLabel(labelPoint);
        } finally {
            featureCursor.close();
        }
    }

    private void drawPolygon(FeatureDao featureDao) {
        FeatureCursor featureCursor = featureDao.queryForAll();
        try {
            org.osmdroid.views.overlay.Polygon polygonOverlay = null;
            List<IGeoPoint> labelPoint = new ArrayList<>();
            while (featureCursor.moveToNext()) {
                FeatureRow featureRow = featureCursor.getRow();
                GeoPackageGeometryData geometryData = featureRow.getGeometry();
                Geometry geometry = geometryData.getGeometry();
                Polygon polygon = (Polygon) geometry;
                polygon.getRings().get(0).getPoints();
                polygonOverlay = new org.osmdroid.views.overlay.Polygon();
                polygonOverlay.setStrokeWidth(2);
                polygonOverlay.setFillColor(0x8032B5EB);
                polygonOverlay.setStrokeColor(Color.BLUE);
                List<Point> points = polygon.getRings().get(0).getPoints();
                List<GeoPoint> geoPoints = new ArrayList<>();
                for (Point p : points) {
                    geoPoints.add(new GeoPoint(p.getY(), p.getX()));
                }
                polygonOverlay.setPoints(geoPoints);
                mapView.getOverlays().add(polygonOverlay);
                GeometryEnvelope envelope = GeometryEnvelopeBuilder.buildEnvelope(geometry);
                IGeoPoint point = new LabelledGeoPoint(envelope.getMinY() + (envelope.getMaxY() - envelope.getMinY()) / 2,
                        envelope.getMinX() + (envelope.getMaxX() - envelope.getMinX()) / 2,
                        featureRow.getValue("name").toString());
                labelPoint.add(point);
            }
            addLabel(labelPoint);
        } finally {
            featureCursor.close();
        }
    }

    private void addLabel(List<IGeoPoint> geoPoints) {
        // wrap them in a theme
        SimplePointTheme pt = new SimplePointTheme(geoPoints, true);

        // create label style
        Paint textStyle = new Paint();
        textStyle.setStyle(Paint.Style.FILL);
        textStyle.setColor(Color.parseColor("#0000ff"));
        textStyle.setTextAlign(Paint.Align.CENTER);
        textStyle.setTextSize(24);

        // set some visual options for the overlay
        // we use here MAXIMUM_OPTIMIZATION algorithm, which works well with >100k points
        SimpleFastPointOverlayOptions opt = SimpleFastPointOverlayOptions.getDefaultStyle();
        opt.setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION);
        opt.setRadius(7).setIsClickable(true).setCellSize(15).setTextStyle(textStyle);

        // create the overlay with the theme
        final SimpleFastPointOverlay sfpo = new SimpleFastPointOverlay(pt, opt);

        // onClick callback
        sfpo.setOnClickListener(new SimpleFastPointOverlay.OnClickListener() {
            @Override
            public void onClick(SimpleFastPointOverlay.PointAdapter points, Integer point) {
                Toast.makeText(mapView.getContext(), "You clicked " + ((LabelledGeoPoint) points.get(point)).getLabel(), Toast.LENGTH_SHORT).show();
            }
        });

        // add overlay
        mapView.getOverlays().add(sfpo);
    }

    private void drawPoint(FeatureDao featureDao) {
        List<IGeoPoint> points = new ArrayList<>();
        FeatureCursor featureCursor = featureDao.queryForAll();
        try {
            while (featureCursor.moveToNext()) {
                FeatureRow featureRow = featureCursor.getRow();
                GeoPackageGeometryData geometryData = featureRow.getGeometry();
                Object obj = featureRow.getValue("name");
                Geometry geometry = geometryData.getGeometry();
                Point geoPoint = (Point) geometry;
                points.add(new LabelledGeoPoint(geoPoint.getY(), geoPoint.getX(), obj.toString()));
            }
            SimplePointTheme pt = new SimplePointTheme(points, true);

            Paint textStyle = new Paint();
            textStyle.setStyle(Paint.Style.FILL);
            textStyle.setColor(Color.parseColor("#0000ff"));
            textStyle.setTextAlign(Paint.Align.CENTER);
            textStyle.setTextSize(24);

            SimpleFastPointOverlayOptions opt = SimpleFastPointOverlayOptions.getDefaultStyle();
            opt.setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION);
            opt.setRadius(7).setIsClickable(true).setCellSize(15).setTextStyle(textStyle);

            final SimpleFastPointOverlay sfpo = new SimpleFastPointOverlay(pt, opt);

            sfpo.setOnClickListener(new SimpleFastPointOverlay.OnClickListener() {
                @Override
                public void onClick(SimpleFastPointOverlay.PointAdapter points, Integer point) {
                    Toast.makeText(mapView.getContext(), "You clicked " + ((LabelledGeoPoint) points.get(point)).getLabel(), Toast.LENGTH_SHORT).show();
                }
            });
            mapView.getOverlays().add(sfpo);
            BoundingBox bindingBox = sfpo.getBoundingBox();
            mapView.zoomToBoundingBox(bindingBox, true);
        } finally {
            featureCursor.close();
        }
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
    public List<Overlay> hitTestLine(GeoPoint point, MapView mapView) {
        List<Overlay> result = new ArrayList<>();
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
    public List<Overlay> hitTest(MotionEvent event, MapView mapView) {
        List<Overlay> result = new ArrayList<>();
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
     * @param overlay
     * @return
     */
    public long restore(Overlay overlay) {
        FeatureDao featureDao = CustomGeoPackageManager.getInstance(context).getGeoPackage().getFeatureDao(this.getName());
        GeoPackageGeometryData geomData = new GeoPackageGeometryData(featureDao.getGeometryColumns().getSrsId());
        if (overlay instanceof Marker) {
            Marker marker = (Marker) overlay;
            geomData.setGeometry(new Point(marker.getPosition().getLongitude(), marker.getPosition().getLatitude()));
        } else if (overlay instanceof Polyline) {
            Polyline polyline = (Polyline) overlay;
            LineString lineString = new LineString(false, false);
            List<GeoPoint> geoPoints = polyline.getPoints();
            List<Point> points = new ArrayList<>();
            for (GeoPoint geoPoint : geoPoints) {
                points.add(new Point(geoPoint.getLongitude(), geoPoint.getLatitude()));
            }
            lineString.setPoints(points);
            geomData.setGeometry(lineString);
        } else if (overlay instanceof org.osmdroid.views.overlay.Polygon) {
            org.osmdroid.views.overlay.Polygon polygon = (org.osmdroid.views.overlay.Polygon) overlay;
            Polygon polygon1 = new Polygon(false, false);
            List<GeoPoint> geoPoints = polygon.getPoints();
            List<Point> points = new ArrayList<>();
            for (GeoPoint geoPoint : geoPoints) {
                points.add(new Point(geoPoint.getLongitude(), geoPoint.getLatitude()));
            }
            LineString lineString = new LineString(false, false);
            lineString.setPoints(points);
            polygon1.addRing(lineString);
            geomData.setGeometry(polygon1);
        }

        FeatureRow row = featureDao.newRow();
        row.setGeometry(geomData);
        return featureDao.insert(row);
    }
}
