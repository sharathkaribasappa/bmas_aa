package com.am;

import android.app.Application;

/**
 * Created by skaribasappa on 7/25/17.
 */

public class BmasApplication extends Application {
    private static BmasApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
    }

    public static BmasApplication getInstance() {
        return sInstance;
    }
}
