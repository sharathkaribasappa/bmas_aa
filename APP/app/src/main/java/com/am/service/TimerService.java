package com.am.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.am.R;
import com.am.activity.LandingActivity;
import com.am.network.ServiceBroker;
import com.am.network.VolleyResponse;
import com.android.volley.VolleyError;

import java.util.Timer;
import java.util.TimerTask;

public class TimerService extends Service {
    private static String TAG = "TimerService";

    private Timer mTimer;
    private static int mElapsedTime = 0;
    private Context mContext;

    private static boolean isLoggedIn = false;

    public TimerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        mContext = this;
        mTimer = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        isLoggedIn = intent.getBooleanExtra("isLogin",false);

        //start the timer
        mTimer.schedule(new ElapsedTimer(),500,500);

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    class ElapsedTimer extends TimerTask {
        @Override
        public void run() {
            mElapsedTime++;

            Log.e(TAG, "ElapsedTimer, time:" + mElapsedTime);
            if(haveNetworkConnection()) {
                sendUserAttendance(LandingActivity.getUserAttendanceData().toString());
            }
        }
    }

    private boolean haveNetworkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if(netInfo != null && netInfo.isAvailable() && netInfo.isConnected()) {
            return true;
        }

        return false;
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
    }

    private void sendUserAttendance(final String userData) {
        //API - 4, insert attendance data to table
        String url ="http://ec2-35-154-248-134.ap-south-1.compute.amazonaws.com/losec/log_atndnc.php";

        ServiceBroker.getInstance().makeNetworkRequest(getApplicationContext(), url, new VolleyResponse() {
            @Override
            public void noNetworkConnection() {
                Log.e(TAG, "noNetworkConnection, attendance data not sent");
            }

            @Override
            public void onSuccessResponse(String response) {
                Log.d(TAG, "sendUserAttendence successful");

                SharedPreferences sharedPref = getSharedPreferences(
                        getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.preference_file_key), isLoggedIn);
                editor.commit();

                Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 1000 milliseconds
                v.vibrate(500);

                Toast.makeText(mContext, "Attendance data sent successfully",
                        Toast.LENGTH_LONG).show();

                //stop the service
                stopSelf();
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "sendUserAttendence error:" + error + "trying again to send the data");

                sendUserAttendance(userData);
            }
        }, userData);
    }
}
