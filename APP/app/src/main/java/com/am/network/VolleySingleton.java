package com.am.network;

import com.am.BmasApplication;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by skaribasappa on 7/16/17.
 */

public class VolleySingleton {
    private static final String TAG = "VolleySingleton";
    private RequestQueue mRequestQueue;

    private static final VolleySingleton mVolleySingleton = new VolleySingleton();

    public static VolleySingleton getInstance() {
        return mVolleySingleton;
    }

    private VolleySingleton() {
    }

    /**
     * @return The Volley Request queue, the queue will be created if it is null
     */
    public RequestQueue getRequestQueue() {
        // lazy initialize the request queue, the queue instance will be
        // created when it is accessed for the first time
        return mRequestQueue = (mRequestQueue == null) ? Volley.newRequestQueue(BmasApplication.getInstance()) : mRequestQueue;
    }
}
