package com.sina.aidemo.posenet;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Pair;

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
 * https://tfhub.dev/tensorflow/posenet/mobilenet/float/075/1
 * 好处：修建尺寸，只需要5M
 */
public class PoseNetFloat075 {
    protected Interpreter mInterpreter = null;
    protected GpuDelegate mGpuDelegate = null;

    /**
     * 初始化方法，包含：
     * 1、选择GPU还是CPU
     * 2、加载模型
     */
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

    public Float sigmoid(Float x) {
        return Float.valueOf((float) (1.0f / (1.0f + Math.exp(-x))));
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
        //1, 23, 17, 17 float_heatmaps
        int[] heatmapsShape = interpreter.getOutputTensor(0).shape();
        float[][][][] outputMap_0 = new float[heatmapsShape[0]][heatmapsShape[1]][heatmapsShape[2]][heatmapsShape[3]];
        outputMap.put(0, outputMap_0);

        // 1, 23, 17, 34 float_short_offsets
        int[] offsetsShape = interpreter.getOutputTensor(1).shape();
        float[][][][] outputMap_1 = new float[offsetsShape[0]][offsetsShape[1]][offsetsShape[2]][offsetsShape[3]];
        outputMap.put(1, outputMap_1);

        // 1, 23, 17, 64 float_mid_offsets
        int[] displacementsFwdShape = interpreter.getOutputTensor(2).shape();
        float[][][][] outputMap_2 = new float[displacementsFwdShape[0]][displacementsFwdShape[1]][displacementsFwdShape[2]][displacementsFwdShape[3]];
        outputMap.put(2, outputMap_2);

        // 1, 23, 17,  1 float_segments
        int[] displacementsBwdShape = interpreter.getOutputTensor(3).shape();
        float[][][][] outputMap_3 = new float[displacementsBwdShape[0]][displacementsBwdShape[1]][displacementsBwdShape[2]][displacementsBwdShape[3]];
        outputMap.put(3, outputMap_3);

        return outputMap;
    }

    public PosePerson estimateSinglePose(Bitmap bitmap) {
        ByteBuffer[] inputArray = new ByteBuffer[1];
        inputArray[0] = initInputArray(bitmap);
        Map<Integer, Object> outputMap = this.initOutputMap(this.mInterpreter);
        this.mInterpreter.runForMultipleInputsOutputs(inputArray, outputMap);
        float[][][][] heatmaps = (float[][][][]) outputMap.get(0);
        float[][][][] offsets = (float[][][][]) outputMap.get(1);

        int height = heatmaps[0].length;
        int width = heatmaps[0][0].length;
        int numKeypoints = heatmaps[0][0][0].length;

        Pair<Integer, Integer>[] keypointPositions = new Pair[numKeypoints];
        for (int i = 0; i < numKeypoints; i++) {
            keypointPositions[i] = new Pair(0, 0);
        }

        for (int keypoint = 0; keypoint < numKeypoints; keypoint++) {
            float maxVal = Float.valueOf(heatmaps[0][0][0][keypoint]);
            int maxRow = 0;
            int maxCol = 0;
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    heatmaps[0][row][col][keypoint] = heatmaps[0][row][col][keypoint];
                    if (heatmaps[0][row][col][keypoint] > maxVal) {
                        maxVal = heatmaps[0][row][col][keypoint];
                        maxRow = row;
                        maxCol = col;
                    }
                }
            }
            keypointPositions[keypoint] = new Pair(maxRow, maxCol);
        }

        int[] xCoords = new int[numKeypoints];
        int[] yCoords = new int[numKeypoints];
        float[] confidenceScores = new float[numKeypoints];
        if (keypointPositions != null) {
            for (int idx = 0; idx < keypointPositions.length; idx++) {
                Pair<Integer, Integer> position = keypointPositions[idx];
                int positionY = position.first;
                int positionX = position.second;
                yCoords[idx] = (int) (position.first / (float) (height - 1) * bitmap.getHeight() +
                        offsets[0][positionY][positionX][idx]);
                xCoords[idx] = (int) (position.second / (float) (width - 1) * bitmap.getWidth() +
                        offsets[0][positionY]
                                [positionX][idx + numKeypoints]);
                confidenceScores[idx] = sigmoid(heatmaps[0][positionY][positionX][idx]);
            }
        }

        PosePerson person = new PosePerson();
        PoseKeyPoint[] keypointList = new PoseKeyPoint[numKeypoints];
        for (int i = 0; i < numKeypoints; i++) {
            keypointList[i] = new PoseKeyPoint();
        }
        float totalScore = 0.f;
        for (PoseBodyPart part : PoseBodyPart.values()) {
            int idx = part.ordinal();
            keypointList[idx].bodyPart = part;
            keypointList[idx].position.x = xCoords[idx];
            keypointList[idx].position.y = yCoords[idx];
            keypointList[idx].score = confidenceScores[idx];
            totalScore += confidenceScores[idx];
        }

        for (PoseKeyPoint point : keypointList) {
            person.keyPoints.add(point);
        }
        person.score = totalScore / numKeypoints;
        return person;
    }
}
