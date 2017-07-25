package com.am.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.am.R;
import com.am.fingerprint.FingerPrintConfig;
import com.am.geofence.BMAS_GeoFence;
import com.am.geofence.GeoFenceContract;
import com.am.geofence.GeoFenceDataModel;
import com.am.geofence.GeoFenceDbHelper;
import com.am.network.ServiceBroker;
import com.am.network.VolleyResponse;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class LandingActivity extends AppCompatActivity{

    private static String TAG = "LandingActivity";
    private static final int REQUEST_LOCATION = 2;

    private TextView employeeId;
    private TextView employeeName;
    private TextView department;
    private EditText building;
    private TextView Date;
    private TextView Time;

    private GeoFenceDbHelper mGeoFenceDbHelper;
    public static HashMap<Integer,GeoFenceDataModel> GeoFenceDataDbMap = new HashMap();
    private HashMap<Integer,String> mBuildingNumberMap = new HashMap();

    private BMAS_GeoFence mGeoFence;
    private FingerPrintConfig mFingerPrintConfig;

    // Define a projection that specifies which columns from the database
    // you will actually use after this query.
    String[] mProjection = {
            GeoFenceContract.GeoFenceEntry._ID,
            GeoFenceContract.GeoFenceEntry.COLUMN_NAME_BUILDING_NUMBER,
            GeoFenceContract.GeoFenceEntry.COLUMN_NAME_LATITUDE,
            GeoFenceContract.GeoFenceEntry.COLUMN_NAME_LONGITUDE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        requestLocationPermission();

        employeeId = (TextView) findViewById(R.id.emp_id_tv);
        employeeName = (TextView) findViewById(R.id.emp_name_tv);
        department = (TextView) findViewById(R.id.emp_dept_tv);

        building = (EditText) findViewById(R.id.emp_buld_edt);

        Date = (TextView) findViewById(R.id.date_tv);
        Time = (TextView) findViewById(R.id.time_tv);

        String response = getIntent().getStringExtra("userData");
        if(response != null) {
            parseUserData(response);
        } else {
            getUserDataFromServer();
        }

        mGeoFence = new BMAS_GeoFence(this);

        mGeoFenceDbHelper = new GeoFenceDbHelper(this);
        getBuildingNumberFromDb();

        mFingerPrintConfig = new FingerPrintConfig(this);
        mFingerPrintConfig.setupFingerPrint();
    }

    @Override
    protected void onStart() {
        super.onStart();

        //connect to google api client
        mGeoFence.onConnect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mGeoFence.onDisconnect();
    }

    @Override
    protected void onDestroy() {
        mGeoFenceDbHelper.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mGeoFence.onPermissionGranted();
            }
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    private void getUserDataFromServer() {
        //API 3
        String url = "http://ec2-35-154-248-134.ap-south-1.compute.amazonaws.com/losec/emp_dtls.php";

        ServiceBroker.getInstance().makeNetworkRequest(getApplicationContext(), url, new VolleyResponse() {
            @Override
            public void noNetworkConnection() {
                Log.e(TAG,"noNetworkConnection");
            }

            @Override
            public void onSuccessResponse(String response) {
                parseUserData(response);
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG,"error response for userdata request, error:" + error);
            }
        }, getImei());
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

    private void parseUserData(String response) {
        if (response != null && response.contains("null")) {

        } else {
            try {
                JSONObject mainObject = new JSONObject(response);
                JSONArray uniobject = mainObject.getJSONArray("object_name");

                String emp_id = null;
                String emp_name = null, emp_dept = null, emp_bldg = null;

                mBuildingNumberMap.clear();

                for (int i = 0; i < uniobject.length(); i++) {
                    String id = uniobject.getString(i);
                    JSONObject uniobject1 = new JSONObject(id);
                    emp_id = uniobject1.getString("emp_id");
                    emp_name = uniobject1.getString("emp_name");
                    emp_dept = uniobject1.getString("dept");
                    emp_bldg = uniobject1.getString("bldg") + (emp_bldg == null ? "" : ", " + emp_bldg);

                    mBuildingNumberMap.put(i,uniobject1.getString("bldg"));
                }

                employeeId.setText(emp_id);
                employeeName.setText(emp_name);
                department.setText(emp_dept);
                building.setText(emp_bldg);

                Log.d(TAG, "success response for userdata request, parsed data, id:" + emp_id + " name:" + emp_name);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = df.format(c.getTime());

        Date.setText(formattedDate);

        SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm:ss");
        String time = localDateFormat.format(new Date());

        Time.setText(time);
    }

    private void fetchGeoFenceData() {
        //API - 2, get the geoFence data for the building number chosen by the user
        String url = "http://ec2-35-154-248-134.ap-south-1.compute.amazonaws.com/losec/geo_fence.php";

        ServiceBroker.getInstance().makeNetworkRequest(getApplicationContext(), url, new VolleyResponse() {
            @Override
            public void noNetworkConnection() {
                Log.e(TAG,"noNetworkConnection");
            }

            @Override
            public void onSuccessResponse(String response) {
                Log.d(TAG,"fetch Geofence data, success response for userdata request, response:" + response);
                if(response != null) {
                    parseGeoFenceData(response);
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG,"fetch Geofence data, error response for userdata request, error:" + error);
            }
        }, getImei());
    }

    private void parseGeoFenceData(String response) {
        try {
            GeoFenceDataDbMap.clear();
            JSONArray jsonArray = new JSONArray(response);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = (JSONObject)jsonArray.get(i);
                GeoFenceDataModel geoFence = new GeoFenceDataModel();

                geoFence.setBuildingNumber(jsonObject.getString("bldg"));
                geoFence.setLatitude(jsonObject.getString("lat"));
                geoFence.setLongitude(jsonObject.getString("lon"));

                GeoFenceDataDbMap.put(i,geoFence);
            }

            updateGeoFenceToDatabase(GeoFenceDataDbMap);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateGeoFenceToDatabase(HashMap<Integer,GeoFenceDataModel> geoFenceDataMap) {
        // Gets the data repository in write mode
        SQLiteDatabase db = mGeoFenceDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();


        for(int i = 0; i < geoFenceDataMap.size(); i++) {
            GeoFenceDataModel geoFenceData = geoFenceDataMap.get(i);
            values.put(GeoFenceContract.GeoFenceEntry.COLUMN_NAME_BUILDING_NUMBER,geoFenceData.getBuildingNumber());
            values.put(GeoFenceContract.GeoFenceEntry.COLUMN_NAME_LATITUDE,geoFenceData.getLatitude());
            values.put(GeoFenceContract.GeoFenceEntry.COLUMN_NAME_LONGITUDE,geoFenceData.getLongitude());

            // Insert the new row, returning the primary key value of the new row
            long newRowId = db.insert(GeoFenceContract.GeoFenceEntry.TABLE_NAME, null, values);

            Log.d(TAG, "updateGeoFenceToDatabase, newRowId: " + newRowId);
        }
    }

    private void getBuildingNumberFromDb() {
        SQLiteDatabase db = mGeoFenceDbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                GeoFenceContract.GeoFenceEntry.TABLE_NAME,  // The table to query
                mProjection, null, null, null, null, null
        );

        GeoFenceDataDbMap.clear();
        boolean isBuildingNumberPresentInDb = false;
        int i = 0;

        while(cursor.moveToNext()) {
            GeoFenceDataModel geoFenceData = new GeoFenceDataModel();

            geoFenceData.setBuildingNumber(cursor.getString(cursor.getColumnIndexOrThrow(GeoFenceContract.GeoFenceEntry.COLUMN_NAME_BUILDING_NUMBER)));
            geoFenceData.setLatitude(cursor.getString(cursor.getColumnIndexOrThrow(GeoFenceContract.GeoFenceEntry.COLUMN_NAME_LATITUDE)));
            geoFenceData.setLongitude(cursor.getString(cursor.getColumnIndexOrThrow(GeoFenceContract.GeoFenceEntry.COLUMN_NAME_LONGITUDE)));

            GeoFenceDataDbMap.put(i,geoFenceData);
            i++;

            isBuildingNumberPresentInDb = mBuildingNumberMap.containsValue(geoFenceData.getBuildingNumber());
            Log.d(TAG, "getBuildingNumberFromDb, buildingNumber: " + geoFenceData.getBuildingNumber());
        }

        if(!isBuildingNumberPresentInDb) {
            Log.d(TAG, "getBuildingNumberFromDb, buildingNumber not found, fetching from server");
            fetchGeoFenceData();
        }
    }

    public void createFingerPrintKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        mFingerPrintConfig.createKey(keyName, invalidatedByBiometricEnrollment);
    }

    public void onAuthenticateSuccessful() {
        sendUserAttendance(formAttendanceDatatoString());
    }

    private String formAttendanceDatatoString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"" + "emp_id" + "\":");
        sb.append("\"" + employeeId.getText() + "\",");

        sb.append("\"" + "emp_name" + "\":");
        sb.append("\"" + employeeName.getText() + "\",");

        sb.append("\"" + "date_time_raw" + "\":");
        sb.append("\"" + System.currentTimeMillis() + "\",");

        sb.append("\"" + "dept" + "\":");
        sb.append("\"" + department.getText() + "\",");

        sb.append("\"" + "bldg" + "\":");
        sb.append("\"" + building.getText() + "\"");

        sb.append("}");
        return sb.toString();
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
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "sendUserAttendence error:" + error);
            }
        }, userData);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            Toast.makeText(this,
                    "YOU CLICKED ENTER KEY",
                    Toast.LENGTH_LONG).show();
            new Handler().postDelayed(new getBuildingFromUserInput(),500);
        }
        return super.dispatchKeyEvent(e);
    };

    private class getBuildingFromUserInput implements Runnable {

        @Override
        public void run() {
            if(mGeoFence != null) {
                updateBuildNumberMap();
                fetchGeoFenceData();
                mGeoFence.updateGeoFenceData();
            }
        }
    }

    private void updateBuildNumberMap() {
        mBuildingNumberMap.clear();
        String S = building.getText().toString();
        String[] bldnumber =  S.split(",");

        for(int i = 0; i < bldnumber.length; i++) {
            mBuildingNumberMap.put(i,bldnumber[i]);
        }
    }
}