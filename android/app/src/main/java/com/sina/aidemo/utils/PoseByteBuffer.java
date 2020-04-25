package com.sina.aidemo.utils;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PoseByteBuffer {
    private ByteBuffer mByteBuffer;
    int[] mIntValues;
    private int mBatchSize = 0;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mNumBytes = 0;
    private int mPixelSize = 0;

    public void init(int batchSize, int width, int height, int numBytes, int pixelSize) {
        mByteBuffer = ByteBuffer.allocateDirect(batchSize * width * height * pixelSize * numBytes);
        mIntValues = new int[width * height];
        mBatchSize = batchSize;
        mWidth = width;
        mHeight = height;
        mNumBytes = numBytes;
        mPixelSize = pixelSize;
        mByteBuffer.order(ByteOrder.nativeOrder());
    }

    public void convertBitmapToByteBuffer(Bitmap bitmap) {
        mByteBuffer.rewind();
        bitmap.getPixels(mIntValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < AIConstants.DIM_PIXEL_WIDTH; ++i) {
            for (int j = 0; j < AIConstants.DIM_PIXEL_HEIGHT; ++j) {
                final int val = mIntValues[pixel++];
                //red
                mByteBuffer.putFloat((((val >> 16) & 0xFF) / AIConstants.IMAGE_STD) - AIConstants.IMAGE_MEAN);
                //green
                mByteBuffer.putFloat((((val >> 8) & 0xFF) / AIConstants.IMAGE_STD) - AIConstants.IMAGE_MEAN);
                //blue
                mByteBuffer.putFloat(((val & 0xFF) / AIConstants.IMAGE_STD) - AIConstants.IMAGE_MEAN);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(AIConstants.TAG, "转换bitmap(ms): " + Long.toString(endTime - startTime));
    }

    public ByteBuffer getByteBuffer() {
        return mByteBuffer;
    }
}
