package com.am;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Vibrator;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class LandingActivity extends AppCompatActivity {

    private static String TAG = "LandingActivity";
    private static final int REQUEST_LOCATION = 2;

    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    private static final String KEY_NAME_NOT_INVALIDATED = "key_not_invalidated";
    static final String DEFAULT_KEY_NAME = "default_key";

    private TextView employeeId;
    private TextView employeeName;
    private TextView department;
    private EditText building;
    private TextView Date;
    private TextView Time;

    private RequestQueue mRequestQueue;

    private GeoFenceDbHelper mGeoFenceDbHelper;
    public static HashMap<Integer,GeoFenceDataModel> GeoFenceDataDbMap = new HashMap();
    private HashMap<Integer,String> mBuildingNumberMap = new HashMap();

    private BMAS_GeoFence mGeoFence;

    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private Cipher mCipher;
    private String mKeyName;
    private SharedPreferences mSharedPreferences;

    // Define a projection that specifies which columns from the database
    // you will actually use after this query.
    String[] mProjection = {
            GeoFenceContract.GeoFenceEntry._ID,
            GeoFenceContract.GeoFenceEntry.COLUMN_NAME_BUILDING_NUMBER,
            GeoFenceContract.GeoFenceEntry.COLUMN_NAME_LATITUDE,
            GeoFenceContract.GeoFenceEntry.COLUMN_NAME_LONGITUDE
    };

    private GeoFenceIntentServiceListener mGeoFenceIntentServiceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        requestLocationPermission();
        mRequestQueue = Volley.newRequestQueue(this);

        employeeId = (TextView) findViewById(R.id.emp_id_tv);
        employeeName = (TextView) findViewById(R.id.emp_name_tv);
        department = (TextView) findViewById(R.id.emp_dept_tv);

        building = (EditText) findViewById(R.id.emp_buld_edt);

        Date = (TextView) findViewById(R.id.date_tv);
        Time = (TextView) findViewById(R.id.time_tv);

        String response = getIntent().getStringExtra("userData");
        if(response != null) {
            parseData(response);
        } else {
            getUserDataFromServer();
        }

        mGeoFence = new BMAS_GeoFence(this);

        mGeoFenceDbHelper = new GeoFenceDbHelper(this);
        getBuildingNumberFromDb();

        setupFingerPrint();
    }

    @Override
    protected void onStart() {
        super.onStart();

        //connect to google api client
        mGeoFence.onConnect();

        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(GeoFenceIntentService.BROADCAST_ACTION);
        // Instantiates a new DownloadStateReceiver
        mGeoFenceIntentServiceListener = new GeoFenceIntentServiceListener();

        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mGeoFenceIntentServiceListener,
                statusIntentFilter);

        //showFingerPrintDialog();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mGeoFence.onDisconnect();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGeoFenceIntentServiceListener);
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

        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG,"success response for userdata request, response:" + response);
                    parseData(response);
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
                    Log.e("UserProfileEditFragment", "Unable to gets bytes from JSON", e.fillInStackTrace());
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

    private void parseData(String response) {
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

        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG,"fetch Geofence data, success response for userdata request, response:" + response);
                        if(response != null) {
                            parseGeoFenceData(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG,"fetch Geofence data, error response for userdata request, error:" + error);
                    }
                }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.e(TAG, "user imei:" + getImei());
                byte[] body = new byte[0];
                try {
                    body = getImei().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e("UserProfileEditFragment", "Unable to gets bytes from JSON", e.fillInStackTrace());
                }
                return body;
            }
        };

        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
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

    private class GeoFenceIntentServiceListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 1000 milliseconds
            v.vibrate(1000);
            showFingerPrintDialog();
        }
    }

    public void onAuthenticateSuccessful() {
        sendUserData(getUserData());
    }

    private String getUserData() {
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

    private void sendUserData(final String userData) {
        //API - 4, insert attendance data to table
        String url ="http://ec2-35-154-248-134.ap-south-1.compute.amazonaws.com/losec/log_atndnc.php";

        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "sendUserData successful");
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "sendUserData error:" + error);
                }
            }){
            @Override
            public byte[] getBody() throws AuthFailureError {
                byte[] body = new byte[0];
                try {
                    body = userData.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e("UserProfileEditFragment", "Unable to gets bytes from JSON", e.fillInStackTrace());
                }
                return body;
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
    }

    private void setupFingerPrint() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
        try {
            mKeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }
        Cipher defaultCipher;
        Cipher cipherNotInvalidated;
        try {
            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipherNotInvalidated = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);

        if (!keyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            Toast.makeText(this,
                    "Secure lock screen hasn't set up.\n"
                            + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Now the protection level of USE_FINGERPRINT permission is normal instead of dangerous.
        // See http://developer.android.com/reference/android/Manifest.permission.html#USE_FINGERPRINT
        // The line below prevents the false positive inspection from Android Studio
        // noinspection ResourceType
        if (!fingerprintManager.hasEnrolledFingerprints()) {
            // This happens when no fingerprints are registered.
            Toast.makeText(this,
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }
        createKey(DEFAULT_KEY_NAME, true);
        createKey(KEY_NAME_NOT_INVALIDATED, false);

        mCipher = defaultCipher;
        mKeyName = DEFAULT_KEY_NAME;
    }

    /**
     * Initialize the {@link Cipher} instance with the created key in the
     * {@link #createKey(String, boolean)} method.
     *
     * @param keyName the key name to init the cipher
     * @return {@code true} if initialization is successful, {@code false} if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    private boolean initCipher(Cipher cipher, String keyName) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     *
     * @param keyName the name of the key to be created
     * @param invalidatedByBiometricEnrollment if {@code false} is passed, the created key will not
     *                                         be invalidated even if a new fingerprint is enrolled.
     *                                         The default value is {@code true}, so passing
     *                                         {@code true} doesn't change the behavior
     *                                         (the key will be invalidated if a new fingerprint is
     *                                         enrolled.). Note that this parameter is only valid if
     *                                         the app works on Android N developer preview.
     *
     */
    public void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showFingerPrintDialog() {
        // Set up the crypto object for later. The object will be authenticated by use
        // of the fingerprint.
        if (initCipher(mCipher, mKeyName)) {

            // Show the fingerprint dialog. The user has the option to use the fingerprint with
            // crypto, or you can fall back to using a server-side verified password.
            FingerprintAuthenticationDialogFragment fragment
                    = new FingerprintAuthenticationDialogFragment();
            fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
            boolean useFingerprintPreference = mSharedPreferences
                    .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                            true);
            if (useFingerprintPreference) {
                fragment.setStage(
                        FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
            } else {
                fragment.setStage(
                        FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
            }
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            // This happens if the lock screen has been disabled or or a fingerprint got
            // enrolled. Thus show the dialog to authenticate with their password first
            // and ask the user if they want to authenticate with fingerprints in the
            // future
            FingerprintAuthenticationDialogFragment fragment
                    = new FingerprintAuthenticationDialogFragment();
            fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
            fragment.setStage(
                    FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        }
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