package com.zhjf.osmdroid.tile;

import java.io.File;
import java.io.InputStream;
import java.util.Random;

import org.osmdroid.tileprovider.ExpirableBitmapDrawable;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.ITileSource;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Created by Administrator on 2017/11/23.
 */

public abstract class CustomBitmapTileSourceBase extends OpenStreetMapTileProviderConstants implements ITileSource {
    private final String TAG = "CustomBitmapTileSourceBase";
    private static int globalOrdinal = 0;
    private final int mMinimumZoomLevel;
    private final int mMaximumZoomLevel;
    private final int mOrdinal;
    protected final String mName;
    protected final String mImageFilenameEnding;
    protected final Random random = new Random();

    private final int mTileSizePixels;


    public CustomBitmapTileSourceBase(final String aName, final int aZoomMinLevel, final int aZoomMaxLevel, final int aTileSizePixels,
                                      final String aImageFilenameEnding) {
        mOrdinal = globalOrdinal++;
        mName = aName;
        mMinimumZoomLevel = aZoomMinLevel;
        mMaximumZoomLevel = aZoomMaxLevel;
        mTileSizePixels = aTileSizePixels;
        mImageFilenameEnding = aImageFilenameEnding;
    }

    @Override
    public int ordinal() {
        return mOrdinal;
    }

    @Override
    public String name() {
        return mName;
    }

    public String pathBase() {
        return mName;
    }

    public String imageFilenameEnding() {
        return mImageFilenameEnding;
    }

    @Override
    public int getMinimumZoomLevel() {
        return mMinimumZoomLevel;
    }

    @Override
    public int getMaximumZoomLevel() {
        return mMaximumZoomLevel;
    }

    @Override
    public int getTileSizePixels() {
        return mTileSizePixels;
    }


//    @Override
//    public String localizedName(final ResourceProxy proxy) {
//        return proxy.getString(mResourceId);
//    }

    @SuppressLint("LongLogTag")
    @Override
    public Drawable getDrawable(final String aFilePath) {
        try {
            // default implementation will load the file as a bitmap and create
            // a BitmapDrawable from it
            final Bitmap bitmap = BitmapFactory.decodeFile(aFilePath);
            if (bitmap != null) {
                return new ExpirableBitmapDrawable(bitmap);
            } else {
                // if we couldn't load it then it's invalid - delete it
                try {
                    new File(aFilePath).delete();
                } catch (final Throwable e) {
                    Log.e(TAG, "Error deleting invalid file: " + aFilePath, e);
                }
            }
        } catch (final OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError loading bitmap: " + aFilePath);
            System.gc();
        }
        return null;
    }

    @Override
    public String getTileRelativeFilenameString(final MapTile tile) {
        final StringBuilder sb = new StringBuilder();
        sb.append(pathBase());
        sb.append('/');
        sb.append(tile.getZoomLevel());
        sb.append('/');
        sb.append(tile.getX());
        sb.append('_');
        sb.append(tile.getY());
        sb.append('_');
        sb.append(tile.getZoomLevel());
        sb.append(imageFilenameEnding());
        return sb.toString();
    }


    @SuppressLint("LongLogTag")
    @Override
    public Drawable getDrawable(final InputStream aFileInputStream) {
        try {
            // default implementation will load the file as a bitmap and create
            // a BitmapDrawable from it
            final Bitmap bitmap = BitmapFactory.decodeStream(aFileInputStream);
            if (bitmap != null) {
                return new ExpirableBitmapDrawable(bitmap);
            }
            System.gc();
        } catch (final OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError loading bitmap");
            System.gc();
            //throw new LowMemoryException(e);
        }
        return null;
    }

    public final class LowMemoryException extends Exception {
        private static final long serialVersionUID = 146526524087765134L;

        public LowMemoryException(final String pDetailMessage) {
            super(pDetailMessage);
        }

        public LowMemoryException(final Throwable pThrowable) {
            super(pThrowable);
        }
    }
}