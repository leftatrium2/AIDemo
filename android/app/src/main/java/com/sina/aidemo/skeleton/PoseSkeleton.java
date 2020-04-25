package com.sina.aidemo.skeleton;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import com.sina.aidemo.contours.Pose;
import com.sina.aidemo.contours.PoseResult;
import com.sina.aidemo.utils.PoseByteBuffer;
import com.sina.aidemo.utils.AIConstants;
import com.sina.aidemo.utils.AIDevice;
import com.sina.aidemo.utils.AIStopWatch;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * MobileNet方式识别骨架图的操作类
 * 不要直接调用，使用SkeletonManager
 */
class PoseSkeleton {
    private Interpreter mInterpreter;
    private GpuDelegate mGpuDelegate = null;
    private boolean mIsInited = false;
    private float[][] mFilterLabelProbArray = null;
    private List<String> mLabelList = new ArrayList<>();
    private Pose mPose = new Pose();
    private PoseByteBuffer mByteBuffer;
    private float[][] mLabelProbArray = null;
    private AIStopWatch mStopWatch = new AIStopWatch();

    /**
     * 初始化并加载模型
     *
     * @param context   context
     * @param modelPath modelPath
     */
    public void init(Context context, String modelPath, String labelPath, int device) throws IOException {
        //加载 label
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelPath)));
        String line;
        line = reader.readLine();
        reader.close();
        StringTokenizer tokenizer = new StringTokenizer(line, ",");
        int labelId = 0;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            mLabelList.add(token);
            PoseResult result = new PoseResult();
            result.label = token;
            result.labelId = labelId;
            result.accuracy = 0.f;
            mPose.results.add(result);
            labelId++;
        }
        mLabelProbArray = new float[1][mLabelList.size()];
        //加载 model
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        Interpreter.Options options = new Interpreter.Options();
        if (AIDevice.isGPU(device)) {
            mGpuDelegate = new GpuDelegate();
            options.addDelegate(mGpuDelegate);
        }
        if (AIDevice.isNNAPI(device)) {
            options.setUseNNAPI(true);
        }
        mInterpreter = new Interpreter(buffer, options);
        //init imgdata array
        mByteBuffer = new PoseByteBuffer();
        mByteBuffer.init(AIConstants.DIM_BATCH_SIZE, AIConstants.DIM_PIXEL_WIDTH, AIConstants.DIM_PIXEL_HEIGHT, AIConstants.DIM_PIXEL_SIZE, AIConstants.NUM_BYTES_PER_CHANNEL);
        mFilterLabelProbArray = new float[AIConstants.FILTER_STAGES][mLabelList.size()];
        mIsInited = true;
    }

    public void close() {
        if (mInterpreter != null) {
            mInterpreter.close();
        }
        mInterpreter = null;
        if (mGpuDelegate != null) {
            mGpuDelegate.close();
        }
    }

    /**
     * 开始骨架图识别
     *
     * @param bitmap bitmap
     * @return 识别概率数组
     */
    public Pose classifyFrame(Bitmap bitmap) {
        if (!mIsInited) {
            throw new RuntimeException("没有初始化成功!");
        }
        if (mInterpreter == null) {
            throw new RuntimeException("没有初始化成功!");
        }
        mStopWatch.start();
        mByteBuffer.convertBitmapToByteBuffer(bitmap);
        mStopWatch.lap("convertBitmapToByteBuffer消耗");
        ByteBuffer imgData = mByteBuffer.getByteBuffer();
        mInterpreter.run(imgData, mLabelProbArray);
        mStopWatch.lap("骨架图模型识别耗费");
        applyFilter();
        mStopWatch.lap("applyFilter耗费");
        float accuracy = Float.MIN_VALUE;
        for (int i = 0; i < mLabelProbArray[0].length; i++) {
            mPose.results.get(i).accuracy = mLabelProbArray[0][i];
            mPose.results.get(i).labelId = i;
            mPose.results.get(i).label = mLabelList.get(i);
            if (accuracy < mLabelProbArray[0][i]) {
                accuracy = mLabelProbArray[0][i];
                mPose.labelId = i;
                mPose.label = mLabelList.get(i);
            }
        }
        mStopWatch.stop("SNPose结构构件消耗");
        return mPose;
    }

    /**
     * Smooth the results across frames.
     */
    void applyFilter() {
        int numLabels = mLabelList.size();

        // Low pass filter `labelProbArray` into the first stage of the filter.
        for (int j = 0; j < numLabels; ++j) {
            mFilterLabelProbArray[0][j] +=
                    AIConstants.FILTER_FACTOR * (mLabelProbArray[0][j] - mFilterLabelProbArray[0][j]);
        }
        // Low pass filter each stage into the next.
        for (int i = 1; i < AIConstants.FILTER_STAGES; ++i) {
            for (int j = 0; j < numLabels; ++j) {
                mFilterLabelProbArray[i][j] +=
                        AIConstants.FILTER_FACTOR * (mFilterLabelProbArray[i - 1][j] - mFilterLabelProbArray[i][j]);
            }
        }

        // Copy the last stage filter output back to `labelProbArray`.
        for (int j = 0; j < numLabels; ++j) {
            setProbability(j, mFilterLabelProbArray[AIConstants.FILTER_STAGES - 1][j]);
        }
    }

    void setProbability(int labelIndex, Number value) {
        mLabelProbArray[0][labelIndex] = value.floatValue();
    }
}
