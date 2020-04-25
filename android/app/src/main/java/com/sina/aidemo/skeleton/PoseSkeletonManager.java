package com.sina.aidemo.skeleton;

import android.content.Context;
import android.graphics.Bitmap;

import com.sina.aidemo.contours.Pose;
import com.sina.aidemo.utils.AIConstants;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 骨架图识别的管理器（针对MobilenetCNN识别方式）
 */
public class PoseSkeletonManager {
    private AtomicBoolean mIsRunning = new AtomicBoolean(false);

    private static class SkeletonManagerINSTANCE {
        private static PoseSkeletonManager instance = new PoseSkeletonManager();
    }

    public static PoseSkeletonManager getInstance() {
        return SkeletonManagerINSTANCE.instance;
    }

    private PoseSkeleton skeleton = new PoseSkeleton();

    public void initAsync(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!mIsRunning.get()) {
                        skeleton.init(context, AIConstants.MN_MODEL_PATH, AIConstants.MN_LABEL_PATH, AIConstants.getDevice());
                        mIsRunning.set(true);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }, "SNPoseSkeletonManager").start();
    }

    /**
     * 初始化函数
     * // TODO: 2020-03-02 记得加上异步操作
     *
     * @param context application context
     */
    public void init(Context context) {
        try {
            if (!mIsRunning.get()) {
                skeleton.init(context, AIConstants.MN_MODEL_PATH, AIConstants.MN_LABEL_PATH, AIConstants.getDevice());
                mIsRunning.set(true);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isInited() {
        return mIsRunning.get();
    }

    /**
     * 关闭、清理资源
     */
    public void close() {
        skeleton.close();
        mIsRunning.set(false);
    }

    /**
     * 识别一帧静态图
     *
     * @param bitmap 静态图像
     * @return 准确值概率数组(label size个)
     */
    public Pose classifyFrame(Bitmap bitmap) {
        return skeleton.classifyFrame(bitmap);
    }


}
