package com.sina.aidemo.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.sina.aidemo.ui.AutoFitTextureView;
import com.sina.aidemo.ui.SkeletonTextureView;
import com.sina.aidemo.utils.AIConstants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 摄像头设置工具类
 */
public class Camera2 {
    private static final String HANDLE_THREAD_NAME = "CameraBackground";
    private static final int PERMISSIONS_REQUEST_CODE = 1;
//    private static final int MAX_PREVIEW_WIDTH = 640;
//    private static final int MAX_PREVIEW_HEIGHT = 480;

    private WeakReference<Activity> mWKContext;
    private CameraDevice mCameraDevice;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private boolean mCheckedPermissions = false;
    private ImageReader mImageReader;
    private Size mPreviewSize;
    private String mCameraId;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CaptureRequest mPreviewRequest;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCaptureSession;

    private AutoFitTextureView mTextureView;
    private SkeletonTextureView mSkeletonTextureView;
    private Size textureSize = new Size(0, 0);

    private boolean mIsUseFront = true;

    private CameraDevice.StateCallback mStateCallBack = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            stateCallbackOnOpened(camera);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            stateCallbackOnDisconnected(camera);
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            stateCallbackOnError(camera, error);
        }
    };

    //============= 外部接口部分 =============//

    /**
     * activity onresume调用
     *
     * @param activity activity,wk方式，放心传
     */
    public void onResume(Activity activity, boolean isFront) {
        this.mIsUseFront = isFront;
        init(activity);
    }

    /**
     * activity onPause调用
     */
    public void onPause() {
        close();
    }


    /**
     * 检查摄像头是否正常运行
     *
     * @return true/false
     */
    public boolean isRunning() {
        return mIsRunning.get();
    }

    /**
     * 设置外部的textureview
     *
     * @param view view
     */
    public void setTextureView(AutoFitTextureView view) {
        if (view != null) {
            mTextureView = view;
            mTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    /**
     * 设置骨架图显示的textureview
     *
     * @param view view
     */
    public void setSkeletonTextureView(SkeletonTextureView view) {
        if (view != null) {
            mSkeletonTextureView = view;
        }
    }

    /**
     * 外部申请权限回调
     *
     * @param requestCode  requestCode
     * @param permissions  permissions
     * @param grantResults grantResults
     * @return true:成功&false:失败
     */
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mCheckedPermissions = true;
                openCamera(textureSize.getWidth(), textureSize.getHeight());
                return true;
            } else {
                // TODO: 2020/3/19 不允许摄像头权限，那么不能运行
                Log.e(AIConstants.TAG, "不允许摄像头权限，那么不能运行");
            }
        }
        return false;
    }

    /**
     * 发送一个runnable任务过来
     *
     * @param runnable runnable
     */
    public void poseTask(Runnable runnable) {
        if (mHandlerThread.isAlive()) {
            mHandler.post(runnable);
        }
    }

    //============= 内部接口部分 =============//

    private void stateCallbackOnOpened(CameraDevice currentCameraDevice) {
        mCameraOpenCloseLock.release();
        mCameraDevice = currentCameraDevice;
        createCameraPreviewSession();
    }

    private void stateCallbackOnDisconnected(CameraDevice currentCameraDevice) {
        mCameraOpenCloseLock.release();
        currentCameraDevice.close();
        mCameraDevice = null;
    }

    private void stateCallbackOnError(CameraDevice currentCameraDevice, int error) {
        mCameraOpenCloseLock.release();
        currentCameraDevice.close();
        mCameraDevice = null;
        Log.e(AIConstants.TAG, "stateCallbackOnError");
    }

    private String[] getRequiredPermissions() {
        return new String[]{Manifest.permission.CAMERA};
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            textureSize = new Size(width, height);
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            textureSize = new Size(width, height);
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private boolean allPermissionsGranted() {
        Activity activity = mWKContext.get();
        if (activity == null) {
            return false;
        }
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 选择前置还是后置摄像头
     *
     * @param manager manager
     * @param isFront true:前置，false:后置
     * @return
     * @throws CameraAccessException
     */
    private String chooseCamera(CameraManager manager, boolean isFront) throws CameraAccessException {
        String frontCameraId = null;
        String backCameraId = null;
        if (manager != null && manager.getCameraIdList().length > 0) {
            for (String camId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(camId);
                StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && map != null) {
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        frontCameraId = camId;
                        break;
                    } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        backCameraId = camId;
                    }
                }
            }

            if (isFront && frontCameraId != null) {
                return frontCameraId;
            } else if (!isFront && backCameraId != null) {
                return backCameraId;
            }
        }
        return null;
    }

    /**
     * 设置摄像头部分
     *
     * @param width  textureview获取的宽度
     * @param height textureview获取的高度
     * @throws IllegalStateException 当camid获取出现问题的时候，推出此异常
     */
    private void setUpCameraOutputs(int width, int height, boolean isUseFront) {
        Activity activity = mWKContext.get();
        if (activity == null) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            String camId = chooseCamera(manager, isUseFront);
            // 从camid确认是后置还是前置相机，一般就两个
            if (camId == null) {
                throw new IllegalStateException("Camera Not Found");
            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(camId);

            StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // 拿到当前相机支持的最大size
            Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/ 2);

            // 显示方向
            int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            // 感应器的角度
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (sensorOrientation == 0 || sensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(AIConstants.TAG, "Display rotation is invalid: " + displayRotation);
            }

            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

//            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
//                maxPreviewWidth = MAX_PREVIEW_WIDTH;
//            }
//
//            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
//                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
//            }

            mPreviewSize =
                    chooseOptimalSize(
                            map.getOutputSizes(SurfaceTexture.class),
                            rotatedPreviewWidth,
                            rotatedPreviewHeight,
                            maxPreviewWidth,
                            maxPreviewHeight,
                            largest);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = activity.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                if (mSkeletonTextureView != null) {
                    mSkeletonTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                }
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                if (mSkeletonTextureView != null) {
                    mSkeletonTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
            }

            this.mCameraId = camId;
        } catch (CameraAccessException e) {
            Log.e(AIConstants.TAG, "Failed to access Camera", e);
        } catch (NullPointerException e) {
            Log.e(AIConstants.TAG, e.getMessage());
        }
    }

    /**
     * 从三组宽高中选择合适的一个
     *
     * @param choices           SurfaceTexture支持的尺寸列表
     * @param textureViewWidth  textureView传入的宽度
     * @param textureViewHeight textureview传入的高度
     * @param maxWidth          屏幕最大宽度
     * @param maxHeight         屏幕最大高度
     * @param aspectRatio       当前相机JPEG类型中支持的最大尺寸
     * @return size
     */
    private static Size chooseOptimalSize(
            Size[] choices,
            int textureViewWidth,
            int textureViewHeight,
            int maxWidth,
            int maxHeight,
            Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth
                    && option.getHeight() <= maxHeight
                    && option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(AIConstants.TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(
                    Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(
                                        mPreviewRequest, mCaptureCallback, mHandler);
                            } catch (CameraAccessException e) {
                                Log.e(AIConstants.TAG, "Failed to set up config to capture Camera", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e(AIConstants.TAG, "onConfigureFailed");
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            Log.e(AIConstants.TAG, "Failed to preview Camera", e);
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureProgressed(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        CaptureResult partialResult) {
                }

                @Override
                public void onCaptureCompleted(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        TotalCaptureResult result) {
                }
            };

    /**
     * 开启摄像头
     *
     * @param width  width
     * @param height height
     * @throws IllegalStateException camid为null的时候，推出此Exception
     */
    private void openCamera(int width, int height) {
        Activity activity = mWKContext.get();
        if (activity == null) {
            return;
        }
        if (!mCheckedPermissions && !allPermissionsGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE);
            }
            return;
        } else {
            mCheckedPermissions = true;
        }
        setUpCameraOutputs(width, height, mIsUseFront);
        if (mCameraId == null) {
            throw new IllegalStateException("No front camera available.");
        }
        configureTransform(width, height);
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            manager.openCamera(mCameraId, mStateCallBack, mHandler);
        } catch (CameraAccessException e) {
            Log.e(AIConstants.TAG, "Failed to open Camera", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = mWKContext.get();
        if (activity != null) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale =
                    Math.max(
                            (float) viewHeight / mPreviewSize.getHeight(),
                            (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void init(Activity activity) {
        mWKContext = new WeakReference<>(activity);
        mHandlerThread = new HandlerThread(HANDLE_THREAD_NAME);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        if (mTextureView != null && mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        }
        mIsRunning.set(true);
    }

    private void close() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
        mHandlerThread.quitSafely();
        try {
            mHandler.removeCallbacksAndMessages(null);
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        mIsRunning.set(false);
    }

    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
