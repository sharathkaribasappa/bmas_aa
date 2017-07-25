package com.am.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

/**
 * Created by skaribasappa on 7/16/17.
 */

public class GeoFenceIntentServiceListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 1000 milliseconds
        v.vibrate(1000);

        //show fingerprint dialog for authentication
        //mFingerPrintConfig.showFingerPrintDialog();
    }
}