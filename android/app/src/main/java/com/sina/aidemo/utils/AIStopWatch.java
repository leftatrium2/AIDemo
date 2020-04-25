package com.sina.aidemo.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AIStopWatch {
    private List<String> tagList = new ArrayList<>();
    private List<Long> timeList = new ArrayList<>();

    public void start() {
        if (AIConstants.isDebug) {
            tagList.add(AIConstants.TAG);
            timeList.add(System.currentTimeMillis());
        }
    }

    public void lap(String TAG) {
        if (AIConstants.isDebug) {
            tagList.add(TAG);
            timeList.add(System.currentTimeMillis());
        }
    }

    public void stop(String TAG) {
        if (!AIConstants.isDebug) {
            return;
        }
        tagList.add(TAG);
        timeList.add(System.currentTimeMillis());
        Log.e(AIConstants.TAG, "====================");
        if (timeList.size() > 1) {
            long startTS = 0L;
            for (int i = 0; i < timeList.size(); i++) {
                String key = tagList.get(i);
                long value = timeList.get(i);
                if (i == 0) {
                    startTS = value;
                } else {
                    Log.e(AIConstants.TAG, "TAG:" + key + ",time(ms):" + (value - startTS));
                    startTS = value;
                }
            }
        }
        Log.e(AIConstants.TAG, "------------------------");
        tagList.clear();
        timeList.clear();
    }
}
