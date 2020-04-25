package com.sina.aidemo.blazeface;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Size;

import com.sina.aidemo.utils.AIConstants;
import com.sina.aidemo.utils.AIDevice;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BlazeFace模型，模型很小，可以用在边缘计算领域
 * 模型信息：
 * [  1 128 128   3]
 * [{
 * 'name': 'regressors',
 * 'index': 175,
 * 'shape': array([  1, 896,  16], dtype=int32),
 * 'dtype': <class 'numpy.float32'>, 'quantization': (0.0, 0)
 * }, {
 * 'name': 'classificators',
 * 'index': 174,
 * 'shape': array([  1, 896,   1], dtype=int32),
 * 'dtype': <class 'numpy.float32'>,'quantization': (0.0, 0)
 * }]
 */
public class BlazeFace {
    private Interpreter mInterpreter = null;
    private GpuDelegate mGpuDelegate = null;
    private List<Face.SNAnthor> mAnthors = null;
    private boolean mIsInited = false;

    /**
     * 初始化
     *
     * @param context   context
     * @param modelPath 模型路径
     * @param device    device，使用的设备，是否用GPU
     */
    public void init(Context context, String modelPath, int device) {
        if (mInterpreter != null) {
            return;
        }
        mAnthors = BlazeFaceUtil.genAnthor();

        Interpreter.Options options = new Interpreter.Options();
        if (AIDevice.isGPU(device)) {
            mGpuDelegate = new GpuDelegate();
            options.addDelegate(mGpuDelegate);
        }
        if (AIDevice.isNNAPI(device)) {
            options.setUseNNAPI(true);
        }
        try {
            this.mInterpreter = new Interpreter(loadModelFile(modelPath, context), options);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        mIsInited = true;
    }

    /**
     * 侦测
     * 1*128*128*3 RGB input
     * output:
     * regressors: [1,896,16]
     * classificators：[1,896,1]
     *
     * @param bitmap bitmap
     * @return SNFace
     */
    public Face detector(Bitmap bitmap, Size oriSize) {
        Face face = null;
        if (!mIsInited) {
            return face;
        }
        ByteBuffer[] inputArray = new ByteBuffer[1];
        inputArray[0] = initInputArray(bitmap);
        Map<Integer, Object> outputMap = this.initOutputMap(this.mInterpreter);
        this.mInterpreter.runForMultipleInputsOutputs(inputArray, outputMap);
        //896*16 [128/16=8] 8个像素作为一个识别点进行的识别操作，16*16 铺满 128*128的位图，用准确度来进行判断
        float[][][] regressors = (float[][][]) outputMap.get(0);
        float[][][] classificators = (float[][][]) outputMap.get(1);
        if (regressors == null || classificators == null) {
            return null;
        }

        int img_height = oriSize.getHeight();
        int img_width = oriSize.getWidth();

        float[] rawBoxes = BlazeFaceUtil.rawBoxWithReshape(regressors);
        float[] rawScores = BlazeFaceUtil.rawBoxWithReshape(classificators);

        List<Face.SNDetections> detections = BlazeFaceUtil.processCPU(rawBoxes, rawScores, mAnthors);
        if (detections.size() == 0) {
            return null;
        }
        int num = 0;
        for (Face.SNDetections detection : detections) {
            Log.e(AIConstants.TAG, "num:" + num);
            Log.e(AIConstants.TAG, detection.toString());
            Log.e(AIConstants.TAG, "");
            num++;
        }
        Face.SNDetections newDetections = BlazeFaceUtil.origNms(detections, 0.3f);

        //取得识别框的交叉坐标
        face = new Face();
        face.detectionRect.left = (int) (img_width * newDetections.xmin);
        face.detectionRect.right = (int) (img_width * (newDetections.xmin + newDetections.width));
        face.detectionRect.top = (int) (img_height * newDetections.ymin);
        face.detectionRect.bottom = (int) (img_height * (newDetections.ymin + newDetections.height));

        return face;
    }

    private MappedByteBuffer loadModelFile(String path, Context context) throws IOException {
        if (context == null) {
            return null;
        }
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(path);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * 输入部分处理
     *
     * @param bitmap bitmap
     * @return ByteBuffer
     */
    private ByteBuffer initInputArray(Bitmap bitmap) {
        int bytesPerChannel = 4;
        int inputChannels = 3;
        int batchSize = 1;
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(batchSize * bytesPerChannel * bitmap.getHeight() * bitmap.getWidth() * inputChannels);
        inputBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.rewind();

        float mean = 128.0f;
        float std = 128.0f;
        for (int row = 0; row < bitmap.getHeight(); row++) {
            for (int col = 0; col < bitmap.getWidth(); col++) {
                int pixelValue = bitmap.getPixel(col, row);
                inputBuffer.putFloat(((pixelValue >> 16 & 0xFF) - mean) / std);
                inputBuffer.putFloat(((pixelValue >> 8 & 0xFF) - mean) / std);
                inputBuffer.putFloat(((pixelValue & 0xFF) - mean) / std);
            }
        }
        return inputBuffer;
    }

    /**
     * 输出部分初始化
     *
     * @param interpreter interpreter
     * @return Map<Integer, Object>
     */
    private Map<Integer, Object> initOutputMap(Interpreter interpreter) {
        Map<Integer, Object> outputMap = new HashMap<>();
        int[] regressorsShape = interpreter.getOutputTensor(0).shape();
        float[][][] outputMap_0 = new float[regressorsShape[0]][regressorsShape[1]][regressorsShape[2]];
        outputMap.put(0, outputMap_0);

        int[] classificatorsShape = interpreter.getOutputTensor(1).shape();
        float[][][] outputMap_1 = new float[classificatorsShape[0]][classificatorsShape[1]][classificatorsShape[2]];
        outputMap.put(1, outputMap_1);

        return outputMap;
    }

    /**
     * 清理
     */
    public void close() {
        if (this.mInterpreter != null) {
            this.mInterpreter.close();
        }
        this.mInterpreter = null;
        if (this.mGpuDelegate != null) {
            this.mGpuDelegate.close();
        }
        this.mGpuDelegate = null;
    }
}
