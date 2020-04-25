package com.sina.aidemo.blazeface;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Size;

import com.sina.aidemo.utils.AIConstants;

import java.util.concurrent.atomic.AtomicBoolean;

public class BlazeFaceManager {
    private AtomicBoolean mIsRunning = new AtomicBoolean(false);

    private static class BlazeFaceManagerINSTANCE {
        private static BlazeFaceManager instance = new BlazeFaceManager();
    }

    public static BlazeFaceManager getInstance() {
        return BlazeFaceManagerINSTANCE.instance;
    }

    private BlazeFace mBlazeFace;

    public void init(Context context) {
        mBlazeFace = new BlazeFace();
        mBlazeFace.init(context, AIConstants.BF_MODEL_PATH, AIConstants.getDevice());
        mIsRunning.set(true);
    }

    public void close() {
        mBlazeFace.close();
        mIsRunning.set(false);
    }

    public boolean isInited() {
        return mIsRunning.get();
    }

    public Face detector(Bitmap bitmap, Size oriSize) {
        if (isInited()) {
            return mBlazeFace.detector(bitmap, oriSize);
        }
        return null;
    }
}
