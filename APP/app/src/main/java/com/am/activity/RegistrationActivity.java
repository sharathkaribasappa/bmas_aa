package com.am.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.am.R;
import com.am.network.ServiceBroker;
import com.am.network.VolleyResponse;
import com.android.volley.VolleyError;

public class RegistrationActivity extends AppCompatActivity implements VolleyResponse{

    final static String TAG = "UserRegistration";

    private String employeeName;
    private String employeeId;
    private String buildingNumber;
    private String department;
    private String phoneNumber;
    private String imei;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

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

        ServiceBroker.getInstance().makeNetworkRequest(getApplicationContext(), url, this, getParsedData());
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

    @Override
    public void noNetworkConnection() {

    }

    @Override
    public void onSuccessResponse(String response) {
        Log.d(TAG,"success response for registration request, response:" + response);
        Intent intent = new Intent(RegistrationActivity.this, LandingActivity.class);
        startActivity(intent);

        finish();
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG,"error response for registration request, error:" + error);
    }
}
