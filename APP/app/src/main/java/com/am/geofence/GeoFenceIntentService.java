package com.am.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by skaribasappa on 6/25/17.
 */

public class GeoFenceIntentService extends IntentService {

    private static String TAG = "GeoFenceIntentService";

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION =
            "com.am.broadcast_geofenceupdate";

    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS =
            "com.am.broadcast_geofencestatus";

    private Handler mHandler;

    public GeoFenceIntentService() {
        super("GeoFenceIntentService");
        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Toast.makeText(getApplicationContext(), "Geofence " +
                    "error code= " + geofencingEvent.getErrorCode(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        //geofencingEvent receieved, check if the location is mocked
        boolean isMock = geofencingEvent.getTriggeringLocation().isFromMockProvider();
        Log.e(TAG,"ismocked:" + isMock);
        if(isMock) {
            return;
        }

        int geoFenceTransition =
                geofencingEvent.getGeofenceTransition();
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geoFenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            mHandler.post(new GeoFenceEvent());
        }
    }

    class GeoFenceEvent implements Runnable {
        @Override
        public void run() {
            /*
             * Creates a new Intent containing a Uri object
             * BROADCAST_ACTION is a custom Intent action
             */
            Intent localIntent = new Intent(BROADCAST_ACTION);
            // Puts the status into the Intent
            localIntent.putExtra(EXTENDED_DATA_STATUS, "location_true");
            // Broadcasts the Intent to receiver in this app.
            sendBroadcast(localIntent);
        }
    }
}