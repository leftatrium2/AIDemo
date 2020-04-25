package com.sina.aidemo.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import com.sina.aidemo.MyApplication;
import com.sina.aidemo.R;
import com.sina.aidemo.camera.Camera2;
import com.sina.aidemo.contours.Pose;
import com.sina.aidemo.posenet.PoseNetManager;
import com.sina.aidemo.posenet.PosePerson;
import com.sina.aidemo.skeleton.PoseSkeletonManager;
import com.sina.aidemo.ui.AutoFitTextureView;
import com.sina.aidemo.ui.SkeletonView;
import com.sina.aidemo.utils.AIConstants;
import com.sina.aidemo.utils.AIImageUtils;

public class PoseDemoActivity extends Activity {
    private AutoFitTextureView mTextureView;
    private SkeletonView mSkeletonView;
    private TextView mResultTextView;
    private Camera2 mCamera2 = new Camera2();
    /**
     * 循环发送识别任务到后台的handler中
     * 备注：
     * 因为android机型各异，不能直接在预览中同步进行姿势识别，会将当前预览部分的画面拖慢
     */
    private Runnable periodicClassify =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        if (mCamera2.isRunning()) {
                            classifyFrame();
                        }
                    }
                    mCamera2.poseTask(periodicClassify);
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!MyApplication.getApplication().isCanRunning()) {
            Log.e(AIConstants.TAG, "当前机型不符合要求");
            finish();
        }
        setContentView(R.layout.gesture_activity_layout);
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
        mCamera2.setTextureView(mTextureView);
        mCamera2.onResume(this, true);
        mCamera2.poseTask(periodicClassify);
    }

    @Override
    protected void onPause() {
        mCamera2.onPause();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        PoseNetManager.getInstance().close();
        PoseSkeletonManager.getInstance().close();
        super.onDestroy();
    }

    private void initView() {
        mTextureView = findViewById(R.id.sfv1);
        mSkeletonView = findViewById(R.id.skeleton);
        mResultTextView = findViewById(R.id.tv1);
    }

    private void initModel() {
        PoseNetManager.getInstance().init(this);
        PoseSkeletonManager.getInstance().init(this);
    }

    private void classifyFrame() {
        if (!mCamera2.isRunning()) {
            Log.e(AIConstants.TAG, "没有初始化完成或者摄像头没有工作!");
            return;
        }
        if (!mTextureView.isAvailable()) {
            Log.e(AIConstants.TAG, "mTextureView不可用!");
            return;
        }
        if (!PoseNetManager.getInstance().isInited()) {
            Log.e(AIConstants.TAG, "模型未初始化完成");
            return;
        }
        if (!PoseSkeletonManager.getInstance().isInited()) {
            Log.e(AIConstants.TAG, "模型2未初始化完成");
            return;
        }
        Bitmap origionalBitmap = mTextureView.getBitmap();
        //先裁剪
        Bitmap cropBitmap = AIImageUtils.cropPoseBitmap(origionalBitmap);
        //再放缩
        Bitmap resizeBitmap = ThumbnailUtils.extractThumbnail(cropBitmap, AIConstants.MODEL2_WIDTH, AIConstants.MODEL2_HEIGHT);
        PosePerson person = PoseNetManager.getInstance().estimate(resizeBitmap);
        //生成骨架图的bitmap
        Bitmap bitmap = Bitmap.createBitmap(AIConstants.DIM_PIXEL_WIDTH, AIConstants.DIM_PIXEL_HEIGHT, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        AIImageUtils.drawPoseNetSkeleton(person, canvas);
        if (AIConstants.IS_SAVE_BITMAP) {
            AIImageUtils.saveBitmap(this, bitmap);
        }

        //识别姿势
        Pose pose = PoseSkeletonManager.getInstance().classifyFrame(bitmap);
        if (pose != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mResultTextView.setText(pose.toString());
                }
            });
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (cropBitmap != null) {
                cropBitmap.recycle();
            }
            if (resizeBitmap != null) {
                resizeBitmap.recycle();
            }
            mSkeletonView.drawSkeleton(person);
        }
    }
}
