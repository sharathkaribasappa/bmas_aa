package com.am.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.am.R;

public class LoginLogoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_logout);

        final String userData = getIntent().getStringExtra("userData");

        Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginLogoutActivity.this, LandingActivity.class);
                intent.putExtra("userData",userData);
                intent.putExtra("isLogin",true);
                startActivity(intent);

                finish();
            }
        });

        Button logout = (Button) findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginLogoutActivity.this, LandingActivity.class);
                intent.putExtra("userData",userData);
                intent.putExtra("isLogin",false);
                startActivity(intent);

                finish();
            }
        });
    }
}
