package com.zhjf.osmdroid.geopackage;


import mil.nga.geopackage.GeoPackage;

public abstract class GeoOperationBase {
    protected  GeoPackage mGeoPackage;

    protected GeoOperationBase( GeoPackage geoPackage) {
        if (geoPackage != null) {
            this.mGeoPackage = geoPackage;
        }
    }
}
