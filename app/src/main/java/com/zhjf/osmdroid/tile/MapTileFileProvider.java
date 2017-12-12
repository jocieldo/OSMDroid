package com.zhjf.osmdroid.tile;

import android.graphics.drawable.Drawable;

import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;

import static org.osmdroid.views.util.constants.MapViewConstants.MAXIMUM_ZOOMLEVEL;
import static org.osmdroid.views.util.constants.MapViewConstants.MINIMUM_ZOOMLEVEL;

/**
 * Created by Administrator on 2017/11/23.
 */

public class MapTileFileProvider extends MapTileModuleProviderBase {

    protected ITileSource mTileSource;

    public MapTileFileProvider(final ITileSource pTileSource) {
        super(2, 2);
        mTileSource = pTileSource;
    }

    @Override
    public boolean getUsesDataConnection() {
        return false;
    }

    @Override
    protected String getName() {
        return "Assets Folder Provider";
    }

    @Override
    protected String getThreadGroupName() {
        return "assetsfolder";
    }

    @Override
    public MapTileModuleProviderBase.TileLoader getTileLoader() {
        return new TileLoader();
    }

    @Override
    public int getMinimumZoomLevel() {
        return mTileSource != null ? mTileSource.getMinimumZoomLevel() : MAXIMUM_ZOOMLEVEL;
    }

    @Override
    public int getMaximumZoomLevel() {
        return mTileSource != null ? mTileSource.getMaximumZoomLevel() : MINIMUM_ZOOMLEVEL;
    }

    @Override
    public void setTileSource(final ITileSource pTileSource) {
        mTileSource = pTileSource;
    }

    private class TileLoader extends MapTileModuleProviderBase.TileLoader {

        @Override
        public Drawable loadTile(MapTile mapTile) throws CantContinueException {
            if (mTileSource == null) {
                return null;
            }
            String path = mTileSource.getTileRelativeFilenameString(mapTile);

            Drawable drawable;
            try {
                drawable = mTileSource.getDrawable(path);
            } catch (final BitmapTileSourceBase.LowMemoryException e) {
                // low memory so empty the queue
                throw new CantContinueException(e);
            }

            return drawable;
        }
    }
}
