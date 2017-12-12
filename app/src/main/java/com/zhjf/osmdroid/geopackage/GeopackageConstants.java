package com.zhjf.osmdroid.geopackage;

public class GeopackageConstants {

    /**
     * GeoPackage file extension
     */
    public static final String GEO_PACKAGE_EXTENSION = "gpkg";

    /**
     * Create elevation tiles database name
     */
    public static final String CREATE_ELEVATION_TILES_DB_NAME = "elevation";

    /**
     * Create elevation tiles database file name
     */
    public static final String CREATE_ELEVATION_TILES_DB_FILE_NAME = CREATE_ELEVATION_TILES_DB_NAME
            + "." + GeopackageConstants.GEO_PACKAGE_EXTENSION;

    public static final String POINT_TABLE = "point_table";

    public static final String LINE_TABLE = "line_table";

    public static final String POLYGON_TABLE = "polygon_table";

    public static final String POINT_NAME = "PointName";
    public static final String CODE = "Code";
    public static final String FID = "id";
}

