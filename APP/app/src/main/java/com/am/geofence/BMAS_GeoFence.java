package com.am.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.am.activity.LandingActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by skaribasappa on 6/25/17.
 */

public class BMAS_GeoFence implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static String TAG = "BMAS_GeoFence";

    private final int MINIMUM_RECOMENDED_RADIUS=200;
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;

    public BMAS_GeoFence(Context context) {
        mContext = context;
        setupGoogleApiClient();
    }

    public void onConnect() {
        mGoogleApiClient.connect();
    }

    public void onDisconnect(){
        mGoogleApiClient.disconnect();
    }

    public void onPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    createGeoFencingRequest(),
                    getGeoFencePendingIntent()
            ).setResultCallback(mResultCallback);
        }
    }

    private synchronized void setupGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    ResultCallback mResultCallback = new ResultCallback() {
        @Override
        public void onResult(Result result) {
            Log.i("onResult()", "result: " +
                    result.getStatus().toString());
        }
    };

    private PendingIntent getGeoFencePendingIntent() {
        Intent intent = new Intent(mContext,
                GeoFenceIntentService.class);
        return PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private List createGeoFenceList(HashMap<Integer,GeoFenceDataModel> geoFenceDataMap) {
        List<Geofence> geoFenceList = new ArrayList<>();

        for(int i = 0; i < geoFenceDataMap.size(); i++) {
            geoFenceList.add(new Geofence.Builder()
                    .setRequestId("ID_" + i)
                    .setCircularRegion(geoFenceDataMap.get(i).getLatitude(), geoFenceDataMap.get(i).getLongitude(), MINIMUM_RECOMENDED_RADIUS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build());
        }

        return geoFenceList;
    }

    private GeofencingRequest createGeoFencingRequest() {
        GeofencingRequest.Builder builder = new
                GeofencingRequest.Builder();
        builder.setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(createGeoFenceList(LandingActivity.GeoFenceDataDbMap));
        return builder.build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("onConnected", "google client is connected");
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    createGeoFencingRequest(),
                    getGeoFencePendingIntent()
            ).setResultCallback(mResultCallback);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("onConnectionFailed()", "connectionResult: "
                +connectionResult.toString());
    }

    public void updateGeoFenceData() {
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    createGeoFencingRequest(),
                    getGeoFencePendingIntent()
            ).setResultCallback(mResultCallback);
        }
    }
}
