package com.am;

/**
 * Created by skaribasappa on 6/25/17.
 */

public class GeoFenceDataModel {
    private String mBuildingNumber;
    private Double mLatitude;
    private Double mLongitude;

    public void setBuildingNumber(String buildingNumber) {
        mBuildingNumber = buildingNumber;
    }

    public void setLatitude(String latitude) {
        mLatitude = Double.parseDouble(latitude);
    }

    public void setLongitude(String longitude) {
        mLongitude = Double.parseDouble(longitude);
    }

    public String getBuildingNumber() {
        return mBuildingNumber;
    }

    public Double getLatitude() {
        return mLatitude;
    }

    public Double getLongitude() {
        return mLongitude;
    }
}
