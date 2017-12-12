package com.zhjf.osmdroid.overlay;

import android.content.Context;

import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.File;

/**
 * Created by Administrator on 2017/11/11.
 */

public class TileLayer extends TilesOverlay {
    private File tilePath;
    private MapView mapView;

    public TileLayer(MapView mapView, Context aContext, MapTileProviderBase aTileProvider) {
        super(aTileProvider, aContext);
        this.mapView = mapView;
    }

    public File getTilePath() {
        return tilePath;
    }

    public void setTilePath(File tilePath) {
        this.tilePath = tilePath;
    }
}
