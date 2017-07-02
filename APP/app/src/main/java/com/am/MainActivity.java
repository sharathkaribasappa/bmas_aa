package com.am;

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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    private final static int MY_PERMISSIONS_READ_PHONE_STATE = 1;
    private final static int MY_PERMISSIONS_WRITE_STORAGE_PHONE_STATE = 2;

    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(checkForWriteStoragePermissions()) {
            startLogger();
        }

        mRequestQueue = Volley.newRequestQueue(this);

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

        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG,"success response for userdata request, response:" + response);
                    if(response != null && response.contains("null")) {
                        Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(MainActivity.this, LandingActivity.class);
                        intent.putExtra("userData",response);
                        startActivity(intent);
                    }
                    finish();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG,"error response for userdata request, error:" + error);
                }
            }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.e(TAG, "user imei:" + getImei());
                byte[] body = new byte[0];
                try {
                    body = getImei().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e("UserProfileEditFragment", "Unable to get bytes from JSON", e.fillInStackTrace());
                }
                return body;
            }
        };

        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
    }

    private String getImei() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String imei = telephonyManager.getDeviceId();

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
}
