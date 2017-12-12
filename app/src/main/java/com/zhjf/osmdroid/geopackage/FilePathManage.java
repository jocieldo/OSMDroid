package com.zhjf.osmdroid.geopackage;

import com.zhjf.osmdroid.common.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilePathManage {
    private volatile static FilePathManage instance = null;
    private String rootPath = "";
    private List<String> tileMapList;
    private List<String> vectorMapList;

    public static FilePathManage getInstance() {
        if (instance == null) {
            synchronized (FilePathManage.class) {
                if (instance == null) {
                    instance = new FilePathManage();
                    if (!instance.rootPath.isEmpty()) {
                        instance.init();
                    }
                }
            }
        }
        return instance;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
        init();
    }

    private void init() {
        File projFile = new File(instance.rootPath);
        if (!projFile.exists()) {
            projFile.mkdir();
        }
        projFile = new File(instance.rootPath + "/media");
        if (!projFile.exists()) {
            projFile.mkdir();
        }
        projFile = new File(instance.rootPath + "/cache");
        if (!projFile.exists()) {
            projFile.mkdir();
        }
        tileMapList = new ArrayList<>();
        vectorMapList = new ArrayList<>();
    }

    public String getMediaDirectory() {
        return instance.rootPath + "/media";
    }

    public String getCacheDirectory() {
        return instance.rootPath + "/cache";
    }

    public String getMap() {
        FileUtil.getFiles(tileMapList, instance.rootPath, ".mbtiles", true);
        FileUtil.getFiles(tileMapList, instance.rootPath, ".pdf", true);

        if (tileMapList.size() > 0) return tileMapList.get(0);
        else return "";
    }

    public String getVector() {
        FileUtil.getFiles(vectorMapList, instance.rootPath, ".gpkg", true);
        if (vectorMapList.size() > 0) return vectorMapList.get(0);
        else return "";
    }


    public String getRootDir() {
        return rootPath;
    }
}
