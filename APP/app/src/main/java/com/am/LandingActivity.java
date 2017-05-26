package com.am;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

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
import java.util.HashMap;
import java.util.Map;

public class LandingActivity extends AppCompatActivity {

    private static String TAG = "LandingActivity";

    private TextView employeeId;
    private TextView employeeName;
    private TextView department;
    private EditText building;
    private TextView Date;
    private TextView Time;

    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        mRequestQueue = Volley.newRequestQueue(this);

        employeeId = (TextView) findViewById(R.id.emp_id_tv);
        employeeName = (TextView) findViewById(R.id.emp_name_tv);
        department = (TextView) findViewById(R.id.emp_dept_tv);
        building = (EditText) findViewById(R.id.emp_buld_edt);
        Date = (TextView) findViewById(R.id.date_tv);
        Time = (TextView) findViewById(R.id.time_tv);

        getUserDataFromServer();
    }

    private void getUserDataFromServer() {
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
                Log.e(TAG, "user imei:" + getParsedData());
                byte[] body = new byte[0];
                try {
                    body = getParsedData().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e("UserProfileEditFragment", "Unable to gets bytes from JSON", e.fillInStackTrace());
                }
                return body;
            }
        };

        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
    }

    private String getParsedData() {
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
        if(response != null) {
            try {
                JSONObject mainObject = new JSONObject(response);
                JSONArray uniobject = mainObject.getJSONArray("object_name");

                String emp_id = null;
                String emp_name = null, emp_dept = null, emp_bldg = null;

                for (int i = 0; i < uniobject.length(); i++) {
                    String id = uniobject.getString(i);
                    JSONObject uniobject1 = new JSONObject(id);
                    emp_id = uniobject1.getString("emp_id");
                    emp_name = uniobject1.getString("emp_name");
                    emp_dept = uniobject1.getString("dept");
                    emp_bldg = uniobject1.getString("bldg") + ", " + emp_bldg;
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

        String sDate = c.get(Calendar.YEAR) + "-"
                + c.get(Calendar.MONTH)
                + "-" + c.get(Calendar.DAY_OF_MONTH);

        Date.setText(sDate);

        String time = c.get(Calendar.HOUR_OF_DAY)
                + ":" + c.get(Calendar.MINUTE);

        Time.setText(time);
    }
}