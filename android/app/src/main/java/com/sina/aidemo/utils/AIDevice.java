package com.sina.aidemo.utils;

/**
 * 8421的bitmap编码处理的占位设备选择
 */
public class AIDevice {
    public final static int GPU = 1;
    public final static int NNAPI = 2;

    public static boolean isGPU(int device) {
        return (device & AIDevice.GPU) == AIDevice.GPU;
    }

    public static boolean isNNAPI(int device) {
        return (device & AIDevice.NNAPI) == AIDevice.NNAPI;
    }

}
