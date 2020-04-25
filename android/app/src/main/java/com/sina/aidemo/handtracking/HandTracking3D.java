package com.sina.aidemo.handtracking;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

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
import java.util.Map;

/**
 * input shape:[  1 256 256   3]
 * output shape:[
 * {
 * 'name': 'ld_21_3d',
 * 'index': 385,
 * 'shape': array([ 1, 63], dtype=int32),
 * 'dtype': <class 'numpy.float32'>,
 * 'quantization': (0.0, 0)
 * }, {
 * 'name': 'output_handflag',
 * 'index': 386,
 * 'shape': array([1, 1],dtype=int32),
 * 'dtype': <class 'numpy.float32'>,
 * 'quantization': (0.0, 0)
 * }]
 */
public class HandTracking3D {
    protected Interpreter mInterpreter = null;
    protected GpuDelegate mGpuDelegate = null;

    public void init(Context context, String modelPath, int device) {
        if (mInterpreter != null) {
            return;
        }

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

    private Map<Integer, Object> initOutputMap(Interpreter interpreter) {
        Map<Integer, Object> outputMap = new HashMap<>();
        //1,63 ld_21_3d
        int[] trackingShape = interpreter.getOutputTensor(0).shape();
        float[][] outputMap_0 = new float[trackingShape[0]][trackingShape[1]];
        outputMap.put(0, outputMap_0);

        return outputMap;
    }

    public Hand3D tracking(Bitmap bitmap) {
        Hand3D hand3D = new Hand3D();
        ByteBuffer[] inputArray = new ByteBuffer[1];
        inputArray[0] = initInputArray(bitmap);
        Map<Integer, Object> outputMap = this.initOutputMap(this.mInterpreter);

        this.mInterpreter.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][] landMarks = (float[][]) outputMap.get(0);

        if (landMarks != null && landMarks.length == 1) {
            for (int i = 0; i < landMarks[0].length; i = i + 3) {
                Hand3D.SNHandPoint3D point = new Hand3D.SNHandPoint3D();
                point.x = (landMarks[0][i]);
                point.y = (landMarks[0][i + 1]);
                point.z = (landMarks[0][i + 2]);
                hand3D.points.add(point);
            }
        }
        Log.e(AIConstants.TAG, "=================");
        Log.e(AIConstants.TAG, hand3D.toString());
        Log.e(AIConstants.TAG, "------------------");

        return hand3D;
    }
}
