package com.sina.aidemo.handtracking;

import android.content.Context;
import android.graphics.Bitmap;

import com.sina.aidemo.utils.AIConstants;

import java.util.concurrent.atomic.AtomicBoolean;

public class HandTrackingManager {
    private AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private HandTracking3D handTracking3D = new HandTracking3D();
    private HandTracking handTracking = new HandTracking();
    private boolean mIs2D = true;

    private static class SNHandTrackingManagerINSTANCE {
        private static HandTrackingManager instance = new HandTrackingManager();
    }

    public static HandTrackingManager getInstance() {
        return SNHandTrackingManagerINSTANCE.instance;
    }

    public void init(Context context, boolean is2D) {
        mIs2D = is2D;
        if (mIs2D) {
            handTracking.init(context, AIConstants.HT_MODEL_PATH, AIConstants.getDevice());
        } else {
            handTracking3D.init(context, AIConstants.HT_3D_MODEL_PATH, AIConstants.getDevice());
        }
        mIsRunning.set(true);
    }

    public void close() {
        if (mIs2D) {
            handTracking.close();
        } else {
            handTracking3D.close();
        }
        mIsRunning.set(false);
    }

    public Hand tracking(Bitmap bitmap) {
        if (mIs2D) {
            return handTracking.tracking(bitmap);
        }
        return null;
    }

    public Hand3D tracking3D(Bitmap bitmap) {
        if (!mIs2D) {
            return handTracking3D.tracking(bitmap);
        }
        return null;
    }

    public boolean isInited() {
        return mIsRunning.get();
    }

}
