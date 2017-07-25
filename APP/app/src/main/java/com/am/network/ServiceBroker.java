package com.am.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.io.UnsupportedEncodingException;

/**
 * Created by skaribasappa on 7/25/17.
 */

public class ServiceBroker {
    private static String TAG = "ServiceBroker";

    private static ServiceBroker mServiceBroker;

    private ServiceBroker() {

    }

    public static ServiceBroker getInstance() {
        synchronized (ServiceBroker.class) {
            if (mServiceBroker == null) {
                mServiceBroker = new ServiceBroker();
            }
        }

        return mServiceBroker;
    }

    public void makeNetworkRequest(Context context, String url, final VolleyResponse volleyResponse, final String data) {
        // Formulate the request and handle the response.
        if(isConnected(context)) {
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "success response for userdata request, response:" + response);
                            volleyResponse.onSuccessResponse(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "error response for userdata request, error:" + error);
                            volleyResponse.onErrorResponse(error);
                        }
                    }) {
                @Override
                public byte[] getBody() throws AuthFailureError {
                    byte[] body = new byte[0];
                    try {
                        body = data.getBytes("UTF-8");
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
            VolleySingleton.getInstance().getRequestQueue().add(stringRequest);
        } else {
            volleyResponse.noNetworkConnection();
        }
    }

    private static boolean isConnected(Context context) {
        if (context != null) {
            ConnectivityManager cm = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                return (networkInfo != null && networkInfo.isConnected());
            }
        }

        return false;
    }
}
