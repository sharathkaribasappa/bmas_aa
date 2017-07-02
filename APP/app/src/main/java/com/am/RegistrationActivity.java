package com.am;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;

public class RegistrationActivity extends AppCompatActivity {

    final static String TAG = "UserRegistration";

    private String employeeName;
    private String employeeId;
    private String buildingNumber;
    private String department;
    private String phoneNumber;
    private String imei;

    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mRequestQueue = Volley.newRequestQueue(this);

        Button button = (Button) findViewById(R.id.register_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fillUserData();
                sendUserDataToServer();
            }
        });
    }

    private void fillUserData() {
        EditText ed1 = (EditText) findViewById(R.id.emp_id_edt);
        employeeId = ed1.getText().toString();

        EditText ed2 = (EditText) findViewById(R.id.emp_name_edt);
        employeeName = ed2.getText().toString();

        EditText ed3 = (EditText) findViewById(R.id.emp_dept_edt);
        department = ed3.getText().toString();

        EditText ed4 = (EditText) findViewById(R.id.emp_buld_edt);
        buildingNumber = ed4.getText().toString();

        EditText ed5 = (EditText) findViewById(R.id.emp_phnnum_edt);
        phoneNumber = ed5.getText().toString();

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        imei = telephonyManager.getDeviceId();
    }

    private void sendUserDataToServer() {
        //API 1 - user registration
        String url = "http://ec2-35-154-248-134.ap-south-1.compute.amazonaws.com/losec/reg_user.php";

        // Formulate the request and handle the response.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG,"success response for registration request, response:" + response);
                    Intent intent = new Intent(RegistrationActivity.this, LandingActivity.class);
                    startActivity(intent);

                    finish();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG,"error response for registration request, error:" + error);
                }
            }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.e(TAG, "Registration data:" + getParsedData());
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
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"" + "emp_id" + "\":");
        sb.append("\"" + employeeId + "\",");
        sb.append("\"" + "emp_name" + "\":");
        sb.append("\"" + employeeName + "\",");
        sb.append("\"" + "bldg" + "\":");
        sb.append("\"" + buildingNumber + "\",");
        sb.append("\"" + "dept" + "\":");
        sb.append("\"" + department + "\",");
        sb.append("\"" + "IMEI" + "\":");
        sb.append("\"" + imei + "\"");
        sb.append("}");

        return sb.toString();
    }
}
