package com.am;

import android.provider.BaseColumns;

/**
 * Created by skaribasappa on 6/24/17.
 */

public class GeoFenceContract {
    private GeoFenceContract(){

    }

    public static class GeoFenceEntry implements BaseColumns {
        public static final String TABLE_NAME = "geofence";
        public static final String COLUMN_NAME_BUILDING_NUMBER = "bldgnumber";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
    }
}
