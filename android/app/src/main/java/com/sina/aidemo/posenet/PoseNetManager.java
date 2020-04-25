package com.sina.aidemo.posenet;

import android.content.Context;
import android.graphics.Bitmap;

import com.sina.aidemo.utils.AIConstants;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PoseNet的管理器类
 */
public class PoseNetManager {
    private AtomicBoolean mIsRunning = new AtomicBoolean(false);

    private static class PoseNetManagerINSTANCE {
        private static PoseNetManager instance = new PoseNetManager();
    }

    public static PoseNetManager getInstance() {
        return PoseNetManagerINSTANCE.instance;
    }

    private PoseNetFloat075 mPoseNet = new PoseNetFloat075();

    public void initAsync(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mIsRunning.get()) {
                    mPoseNet.init(context, AIConstants.MODEL_PATH2, AIConstants.getDevice());
                    PoseNetManager.this.mIsRunning.set(true);
                }
            }
        }, "SNPoseNetManager").start();
    }

    /**
     * 异步初始化函数，需要有个标识位标志是否初始化完毕
     *
     * @return
     */
    public boolean isInited() {
        return this.mIsRunning.get();
    }

    public void init(Context context) {
        if (!mIsRunning.get()) {
            mPoseNet.init(context, AIConstants.MODEL_PATH2, AIConstants.getDevice());
            mIsRunning.set(true);
        }
    }

    public PosePerson estimate(Bitmap bitmap) {
        return mPoseNet.estimateSinglePose(bitmap);
    }

    public void close() {
        mPoseNet.close();
        mIsRunning.set(false);
    }
}
