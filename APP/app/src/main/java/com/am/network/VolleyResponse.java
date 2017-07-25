package com.am.network;

import com.android.volley.VolleyError;

/**
 * Created by skaribasappa on 7/16/17.
 */

public interface VolleyResponse {
    void noNetworkConnection();

    void onSuccessResponse(String response);

    void onErrorResponse(VolleyError error);
}
