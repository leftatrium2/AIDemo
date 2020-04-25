package com.sina.aidemo;

import android.app.Application;
import android.content.Context;

import com.sina.aidemo.utils.DeviceFilter;

public class MyApplication extends Application {
    private boolean canRunning = false;
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        this.canRunning = DeviceFilter.canRunning(this);
        mContext = this;
    }

    public static MyApplication getApplication() {
        return (MyApplication) mContext;
    }

    public boolean isCanRunning() {
        return canRunning;
    }
}
