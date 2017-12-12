package com.zhjf.osmdroid.tile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.tom_roush.pdfbox.cos.COSStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import mil.nga.wkb.geom.GeometryEnvelope;

/**
 * Created by Administrator on 2017/11/23.
 */

public class TileHelper {
    static int TILE_SIZE = 256;
    static String TILE_FILENAME = "tile_%d_%d_%d.png";
    static String OUTPUT_DIR = "./output";

    public void createBitmapTile(int level, COSStream inputStream, GeometryEnvelope envelope, String outputPath) {
        if (level < 1 || level > 15 || inputStream == null) {
            System.err.println("usage: \"TilesGenerator [1-15] [filename]\"");
            return;
        }
        try {
            OUTPUT_DIR = outputPath;
            if (!new File(OUTPUT_DIR).exists()) {
                new File(OUTPUT_DIR).mkdir();
            }
            SplitTilesRecursive(BitmapFactory.decodeStream(inputStream.getFilteredStream()), level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void SplitTilesRecursive(Bitmap original, int level) {

        int mapWidth = GetMapWidth(level);
        int tilesOnSide = mapWidth / TILE_SIZE;
        Bitmap resized = ResizeImage(original, mapWidth * 2, mapWidth);

        for (int x = 0; x < tilesOnSide * 2; x++)
            for (int y = 0; y < tilesOnSide; y++)
                CropAndSaveTile(resized, x, y, level);
        if (level > 0)
            SplitTilesRecursive(original, level - 1);
    }

    private int GetMapWidth(int level) {
        return TILE_SIZE * (int) Math.pow(2, level);
    }

    private void CropAndSaveTile(Bitmap image, int x, int y, int level) {
        Rect cropArea = new Rect(x * TILE_SIZE, y * TILE_SIZE, x * TILE_SIZE + TILE_SIZE, y * TILE_SIZE + TILE_SIZE);
        Bitmap clone = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight());
        Bitmap cropped = cropImage(clone, cropArea);
        String filename = String.format(TILE_FILENAME, level, x, y);
        try {
            FileOutputStream out = new FileOutputStream(new File(OUTPUT_DIR + "/" + filename));
            cropped.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private Bitmap cropImage(Bitmap bitmap, Rect rect) {
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), null, false);
    }

    /**
     * @param toResize
     * @param width    mapWidth * 2
     * @param height   mapWidth
     * @return
     */
    private Bitmap ResizeImage(Bitmap toResize, int width, int height) {
        Bitmap resizedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(resizedImage);
        cv.drawBitmap(toResize, 0, 0, null);//在 0，0坐标开始画入bg
        cv.save(Canvas.ALL_SAVE_FLAG);//保存
        return resizedImage;
    }
}
