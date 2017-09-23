package com.am.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.am.LogReader;
import com.am.R;
import com.am.network.ServiceBroker;
import com.am.network.VolleyResponse;
import com.android.volley.VolleyError;

public class MainActivity extends AppCompatActivity implements VolleyResponse{

    private static String TAG = "MainActivity";

    private final static int MY_PERMISSIONS_READ_PHONE_STATE = 1;
    private final static int MY_PERMISSIONS_WRITE_STORAGE_PHONE_STATE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*if(checkForWriteStoragePermissions()) {
            startLogger();
        }*/

        if(checkForPhoneStatePermissions()) {
            getUserDataFromServer();
        }
    }

    private boolean checkForPhoneStatePermissions() {
        final int version = Build.VERSION.SDK_INT;
        if(version >= 21) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSIONS_READ_PHONE_STATE);
                return false;
            } else {
                return true;
            }
        } else
            return true;
    }

    private boolean checkForWriteStoragePermissions() {
        final int version = Build.VERSION.SDK_INT;
        if(version >= 21) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_STORAGE_PHONE_STATE);
                return false;
            } else {
                return true;
            }
        } else
            return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_READ_PHONE_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getUserDataFromServer();
                }
                break;

            case MY_PERMISSIONS_WRITE_STORAGE_PHONE_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkForPhoneStatePermissions();
                    startLogger();
                }
                break;
        }
    }

    private void getUserDataFromServer() {
        //API 3 , get registered user data
        String url = "http://ec2-35-154-248-134.ap-south-1.compute.amazonaws.com/losec/emp_dtls.php";

        ServiceBroker.getInstance().makeNetworkRequest(getApplicationContext(), url, this, getImei());
    }

    private String getImei() {
        String imei = null;
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            imei = telephonyManager.getDeviceId();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"" + "IMEI" + "\":");
        sb.append("\"" + imei + "\"");
        sb.append("}");

        return sb.toString();
    }

    private void startLogger() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LogReader.writeLog();
            }
        });
        thread.start();
    }

    @Override
    public void noNetworkConnection() {
        Log.e(TAG,"MainActivity, noNetworkConnection");

        Toast.makeText(this, "No network connection, please connect to wifi or data network and try again ",
                Toast.LENGTH_LONG).show();

        finish();
    }

    @Override
    public void onSuccessResponse(String response) {
        Log.d(TAG,"success response for userdata request, response:" + response);
        if(response != null && response.contains("null")) {
            Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(MainActivity.this, LoginLogoutActivity.class);
            intent.putExtra("userData",response);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG,"error response for userdata request, error:" + error);
    }
}
