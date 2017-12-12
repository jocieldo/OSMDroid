package com.zhjf.osmdroid.tile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.osmdroid.tileprovider.util.StreamUtils;

import java.io.InputStream;

/**
 * Created by Administrator on 2017/11/23.
 */

public class CustomTileSource extends CustomBitmapTileSourceBase {

    public CustomTileSource(final String aName, final int aZoomMinLevel, final int aZoomMaxLevel, final int aTileSizePixels,
                            final String aImageFilenameEnding) {
        super(aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding);
    }

    @Override
    public Drawable getDrawable(final String aFilePath) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(aFilePath);
            if (bitmap != null) {
                return new BitmapDrawable(bitmap);
            }
        } catch (final Throwable e) {
            // Tile does not exist in assets folder.
            // Ignore silently
        }
        return null;
    }

    @Override
    public String getCopyrightNotice() {
        return "JerFer";
    }
}