package com.sina.aidemo.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import com.sina.aidemo.MyApplication;
import com.sina.aidemo.R;
import com.sina.aidemo.blazeface.BlazeFaceManager;
import com.sina.aidemo.blazeface.Face;
import com.sina.aidemo.camera.Camera2;
import com.sina.aidemo.ui.AutoFitTextureView;
import com.sina.aidemo.ui.SkeletonTextureView;
import com.sina.aidemo.utils.AIConstants;
import com.sina.aidemo.utils.AIImageUtils;
import com.sina.aidemo.utils.AIStopWatch;

/**
 * 面部侦测
 * 面部框以及五官
 * 基于BlazeFace模型
 */
public class BlazeFaceActivity extends Activity {
    private AutoFitTextureView mAutoFitTextureView;
    private SkeletonTextureView mSkeletonTextureView;
    private Camera2 mCamera2 = new Camera2();
    private AIStopWatch mStopWatch = new AIStopWatch();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                if (mCamera2.isRunning()) {
                    classifyFrame();
                }
            }
            mCamera2.poseTask(runnable);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!MyApplication.getApplication().isCanRunning()) {
            Log.e(AIConstants.TAG, "当前机型不符合要求");
            finish();
        }
        setContentView(R.layout.blazeface_activity_layout);
        initView();
        initModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!MyApplication.getApplication().isCanRunning()) {
            Log.e(AIConstants.TAG, "当前机型不符合要求");
            finish();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCamera2.onResume(this, true);
        mCamera2.setTextureView(mAutoFitTextureView);
        mCamera2.setSkeletonTextureView(mSkeletonTextureView);
        mCamera2.poseTask(runnable);
    }

    @Override
    protected void onPause() {
        mCamera2.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        BlazeFaceManager.getInstance().close();
        super.onDestroy();
    }

    private void initView() {
        mAutoFitTextureView = (AutoFitTextureView) findViewById(R.id.sfv1);
        mSkeletonTextureView = (SkeletonTextureView) findViewById(R.id.stv1);
        mSkeletonTextureView.setOpaque(false);
    }

    private void initModel() {
        BlazeFaceManager.getInstance().init(this);
    }

    private void classifyFrame() {
        if (!mCamera2.isRunning()) {
            Log.e(AIConstants.TAG, "没有初始化完成或者摄像头没有工作!");
            return;
        }
        if (!mAutoFitTextureView.isAvailable()) {
            Log.e(AIConstants.TAG, "mTextureView不可用!");
            return;
        }
        if (!BlazeFaceManager.getInstance().isInited()) {
            Log.e(AIConstants.TAG, "模型不可用!");
            return;
        }
        Bitmap bitmap = null;
        Bitmap cropBitmap = null;
        Bitmap resizeBitmap = null;
        Face face = null;
        try {
            bitmap = mAutoFitTextureView.getBitmap();
            Size size = new Size(bitmap.getWidth(), bitmap.getHeight());
            cropBitmap = AIImageUtils.cropPoseBitmap(bitmap);
            resizeBitmap = AIImageUtils.cropSkeletonBitmap(cropBitmap, 128, 128);
            face = BlazeFaceManager.getInstance().detector(resizeBitmap, size);
            if (face == null) {
                return;
            }
            Canvas canvas = mSkeletonTextureView.lockCanvas();
            if (canvas != null) {
                AIImageUtils.drawBlazeFacePoint(face, canvas);
                mSkeletonTextureView.unlockCanvasAndPost(canvas);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (cropBitmap != null) {
                cropBitmap.recycle();
            }
            if (resizeBitmap != null) {
                resizeBitmap.recycle();
            }
        }
    }
}
