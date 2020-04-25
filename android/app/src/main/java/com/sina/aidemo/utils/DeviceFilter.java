package com.sina.aidemo.utils;

import android.content.Context;

import com.facebook.device.yearclass.YearClass;
import com.sina.aidemo.BuildConfig;

public class DeviceFilter {
    public static boolean canRunning(Context context) {
        if (BuildConfig.DEBUG) {
            return true;
        }
        int year = YearClass.get(context);
        if (year >= YearClass.CLASS_2016) {
            return true;
        }
        return false;
    }
}
