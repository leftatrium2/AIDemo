package com.sina.aidemo.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;

import com.sina.aidemo.MyApplication;
import com.sina.aidemo.R;
import com.sina.aidemo.camera.Camera2;
import com.sina.aidemo.posenet.PoseNetManager;
import com.sina.aidemo.posenet.PosePerson;
import com.sina.aidemo.ui.AutoFitTextureView;
import com.sina.aidemo.ui.SkeletonTextureView;
import com.sina.aidemo.utils.AIConstants;
import com.sina.aidemo.utils.AIImageUtils;
import com.sina.aidemo.utils.AIStopWatch;

/**
 * 火炬活动activity
 */
public class PoseActivity extends Activity {
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
        setContentView(R.layout.torchbearers_activity_layout);
        initView();
        initModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!MyApplication.getApplication().isCanRunning()) {
            Log.e(AIConstants.TAG, "当前机型不符合要求");
            finish();
        }
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
        PoseNetManager.getInstance().close();
        super.onDestroy();
    }

    private void initView() {
        mAutoFitTextureView = (AutoFitTextureView) findViewById(R.id.sfv1);
        mSkeletonTextureView = (SkeletonTextureView) findViewById(R.id.stv1);
        mSkeletonTextureView.setOpaque(false);
    }

    private void initModel() {
        PoseNetManager.getInstance().initAsync(this);
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
        if (!PoseNetManager.getInstance().isInited()) {
            Log.e(AIConstants.TAG, "模型未初始化完成");
            return;
        }
        mStopWatch.start();
        Bitmap origionalBitmap = mAutoFitTextureView.getBitmap();
        mStopWatch.lap("放缩、裁剪耗时1(ms):");
        //先裁剪
        Bitmap cropBitmap = AIImageUtils.cropPoseBitmap(origionalBitmap);
        mStopWatch.lap("放缩、裁剪耗时2(ms):");
        //再放缩
        Bitmap resizeBitmap = ThumbnailUtils.extractThumbnail(cropBitmap, AIConstants.MODEL2_WIDTH, AIConstants.MODEL2_HEIGHT);
        mStopWatch.lap("放缩、裁剪耗时3(ms):");
        PosePerson person = PoseNetManager.getInstance().estimate(resizeBitmap);
        mStopWatch.lap("posenet模型耗时(ms):");
        //渲染到制定的textureview
        Canvas canvas = mSkeletonTextureView.lockCanvas();
        if (canvas != null) {
            AIImageUtils.drawPoseNetSkeleton(person, canvas);
            mSkeletonTextureView.unlockCanvasAndPost(canvas);
        }
        mStopWatch.stop("骨架图绘制耗时(ms):");
    }
}
