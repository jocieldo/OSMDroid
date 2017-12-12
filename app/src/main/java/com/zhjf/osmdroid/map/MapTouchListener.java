package com.zhjf.osmdroid.map;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Administrator on 2017/11/7.
 */

class MyTouchListener implements View.OnTouchListener {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }
//    private MapView mapView;
//    private Context context;
//    private Point pointFrom;
//    private boolean isSelected = true;
//    private static final int ENVELOPE = 0, POINT = 1, POLYLINE = 2, POLYGON = 3;
//    private int editingMode = ENVELOPE;
//    private GeometryEnvelope envelope;
//    private boolean isRedraw = false;
//    private Polygon polygon;
//    public LineString polyline;
//
//    public MyTouchListener(Context context, MapView view) {
//        this.mapView = view;
//        this.context = context;
//    }
//
//    @Override
//    public boolean onTouch(View v, MotionEvent event) {
//        IProjection projection = mapView.getProjection();
//        IGeoPoint p = projection.fromPixels((int) event.getX(), (int) event.getY());
//        Point point = new Point(p.getLongitude(), p.getLatitude());
//        if (pointFrom == null) {
//            pointFrom = point;
//        }
//        if (isSelected && editingMode == ENVELOPE && event.getAction() == MotionEvent.ACTION_DOWN) {
//            envelope = new GeometryEnvelope();
//            envelope.setMinX(pointFrom.getX());
//            envelope.setMinY(pointFrom.getY());
//            envelope.setMaxX(pointFrom.getX());
//            envelope.setMaxY(pointFrom.getY());
//        } else {
//            if ((selectedLayer != null) && (editingMode == POLYLINE || editingMode == POLYGON) && event.getAction() == MotionEvent.ACTION_DOWN) {
//                try {
//                    if (isRedraw) {
//                        switch (editingMode) {
//                            case POLYGON:
//                                polygon = new Polygon();
//                                polygon.startPath(pointFrom);
//                                break;
//                            case POLYLINE:
//                                polyline = new LineString();
//                                polyline.startPath(pointFrom);
//                                break;
//                        }
//                    } else {
//                        GeodatabaseFeature geodatabaseFeature = currentInfo.getTable().createFeatureWithTemplate(featureTemplate, null);
//                        if (geodatabaseFeature == null) {
//                            Toast.makeText(context, "创建失败", Toast.LENGTH_LONG).show();
//                            return false;
//                        }
//                        currentInfo.addFeature(geodatabaseFeature);
//
//                        if (geodatabaseFeature != null) {
//                            switch (editingMode) {
//                                case POLYGON:
//                                    polygon = new Polygon();
//                                    polygon.startPath(pointFrom);
//                                    break;
//                                case POLYLINE:
//                                    polyline = new LineString();
//                                    polyline.addPoint(pointFrom);
//                                    break;
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public boolean onDragPointerMove(MotionEvent from, final MotionEvent to) {
//        Point pointTo = mapView.toMapPoint(to.getX(), to.getY());
//        if (isSelected && editingMode == ENVELOPE) {
//            envelope.setXMin(pointFrom.getX() > pointTo.getX() ? pointTo.getX() : pointFrom.getX());
//            envelope.setYMin(pointFrom.getY() > pointTo.getY() ? pointTo.getY() : pointFrom.getY());
//            envelope.setXMax(pointFrom.getX() < pointTo.getX() ? pointTo.getX() : pointFrom.getX());
//            envelope.setYMax(pointFrom.getY() < pointTo.getY() ? pointTo.getY() : pointFrom.getY());
//            geodatabaseHelper.getDrawLayer().removeAll();
//            geodatabaseHelper.getDrawLayer().addGraphic(new Graphic(envelope, simpleFillSymbol));
//            return true;
//        } else {
//            if ((selectedLayer != null) && (editingMode == POLYLINE || editingMode == POLYGON)) {
//                try {
//                    switch (editingMode) {
//                        case POLYGON:
//                            polygon.lineTo(pointTo);
//                            if (currentInfo.hasFeature()) {
//                                currentInfo.updateFeature(polygon);
//
//                            }
//                            break;
//                        case POLYLINE:
//                            polyline.lineTo(pointTo);
//                            if (currentInfo.hasFeature()) {
//                                currentInfo.updateFeature(polyline);
//                            }
//                            break;
//                    }
//                    return true;
//                } catch (Exception e) {
//                    return super.onDragPointerMove(from, to);
//                }
//            }
//        }
//        return super.onDragPointerMove(from, to);
//    }
//
//    @Override
//    public boolean onDragPointerUp(MotionEvent from, final MotionEvent to) {
//        Point pointTo = mapView.toMapPoint(to.getX(), to.getY());
//        if (isSelected && editingMode == ENVELOPE) {
//            envelope.setXMin(pointFrom.getX() > pointTo.getX() ? pointTo.getX() : pointFrom.getX());
//            envelope.setYMin(pointFrom.getY() > pointTo.getY() ? pointTo.getY() : pointFrom.getY());
//            envelope.setXMax(pointFrom.getX() < pointTo.getX() ? pointTo.getX() : pointFrom.getX());
//            envelope.setYMax(pointFrom.getY() < pointTo.getY() ? pointTo.getY() : pointFrom.getY());
//            geodatabaseHelper.getDrawLayer().removeAll();
//            pointFrom = null;
//            geodatabaseHelper.getDrawLayer().addGraphic(new Graphic(envelope, simpleFillSymbol));
//            if (ansyTry != null && ansyTry.getStatus() == android.os.AsyncTask.Status.RUNNING) {
//                ansyTry.cancel(true);
//            }
//            ansyTry = new AnsyTry();
//            ansyTry.execute("");
//            geodatabaseHelper.getDrawLayer().removeAll();
//            isSelected = false;
//            redrawTextView.setSelected(false);
//            return true;
//        } else {
//            if ((selectedLayer != null) && (editingMode == POLYLINE || editingMode == POLYGON)) {
//                try {
//                    switch (editingMode) {
//                        case POLYGON:
//                            polygon.lineTo(pointTo);
//                            if (currentInfo.hasFeature()) {
//                                currentInfo.updateFeature(polygon);
//                                geodatabaseHelper.queryRegion(polygon);
//                                menuItemNew.setVisible(true);
//                            }
//                            break;
//                        case POLYLINE:
//                            if (isRedraw) {
//                                polyline.lineTo(pointTo);
//                                if (currentInfo.hasFeature()) {
//                                    currentInfo.updateFeature(polyline);
//                                    updateLocationInfo(polyline, selectEntity);
//                                }
//                                isRedraw = false;
//                                editingMode = ENVELOPE;
//                            } else {
//                                polyline.lineTo(pointTo);
//                                if (currentInfo.hasFeature()) {
//                                    currentInfo.updateFeature(polyline);
//                                    geodatabaseHelper.queryRegion(polyline);
//                                    menuItemNew.setVisible(true);
//                                }
//                            }
//                            break;
//                    }
//                } catch (Exception e) {
//                    return super.onDragPointerUp(from, to);
//                }
//            }
//        }
//        pointFrom = null;
//        return super.onDragPointerUp(from, to);
//    }
//
//    /**
//     * In this method we check if the point clicked on the map denotes a new
//     * point or means an existing vertex must be moved.
//     */
//    @Override
//    public boolean onSingleTap(final MotionEvent e) {
//        if ((selectedLayer != null) && (editingMode == POINT)) {
//            try {
//                Point point = mapView.toMapPoint(e.getX(), e.getY());
//                if (!isRedraw && editingMode == POINT) {
//                    if (currentInfo.hasFeature()) {
//                        currentInfo.deleteFature();
//                    }
//                    GeodatabaseFeature f = currentInfo.getTable().createFeatureWithTemplate(featureTemplate, null);
//                    if (f == null) {
//                        Toast.makeText(activity, "创建失败", Toast.LENGTH_LONG).show();
//                        return false;
//                    }
//                    currentInfo.addFeature(f);
//                    currentInfo.updateFeature(point);
//                    currentInfo.putAttr("状态", String.valueOf(XMLHelper.getStatusValue(added)));
//                    try {
//                        geodatabaseHelper.queryRegion(point);
//                    } catch (InterruptedException e1) {
//                        e1.printStackTrace();
//                    } catch (ExecutionException e1) {
//                        e1.printStackTrace();
//                    }
//                    menuItemNew.setVisible(true);
//                } else if (isRedraw && editingMode == POINT) {
//                    currentInfo.updateFeature(point);
//                    updateLocationInfo(point, selectEntity);
//                    isRedraw = false;
//                    editingMode = ENVELOPE;
//                }
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//        pointFrom = null;
//        return true;
//    }

}