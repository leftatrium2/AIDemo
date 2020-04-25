package com.sina.aidemo.utils;

import android.util.Pair;

import com.sina.aidemo.posenet.PoseBodyPart;

import java.util.ArrayList;
import java.util.List;

public class AIConstants {
    public static final String TAG = "SNPoseEstimation";
    public static final boolean isDebug = true;

    public static int getDevice() {
        return AIDevice.GPU;
    }

    //-------------- POSENET 部分常量 -----------------------//
    //是否存骨架图到SDCARD
    public static final boolean IS_SAVE_BITMAP = false;
    //PoseNet模型，bitmap的宽、高
    public static final int MODEL2_WIDTH = 257;
    public static final int MODEL2_HEIGHT = 353;
    //posenet075
    public final static String MODEL_PATH2 = "posenet_model.tflite";
    //骨架连接数组，确定关节间的连接关系
    public static final List<Pair<PoseBodyPart, PoseBodyPart>> BODY_JOINTS = new ArrayList<Pair<PoseBodyPart, PoseBodyPart>>() {
        {
            add(new Pair(PoseBodyPart.LEFT_WRIST, PoseBodyPart.LEFT_ELBOW));
            add(new Pair(PoseBodyPart.LEFT_ELBOW, PoseBodyPart.LEFT_SHOULDER));
            add(new Pair(PoseBodyPart.LEFT_SHOULDER, PoseBodyPart.RIGHT_SHOULDER));
            add(new Pair(PoseBodyPart.RIGHT_SHOULDER, PoseBodyPart.RIGHT_ELBOW));
            add(new Pair(PoseBodyPart.RIGHT_ELBOW, PoseBodyPart.RIGHT_WRIST));
            add(new Pair(PoseBodyPart.LEFT_SHOULDER, PoseBodyPart.LEFT_HIP));
            add(new Pair(PoseBodyPart.LEFT_HIP, PoseBodyPart.RIGHT_HIP));
            add(new Pair(PoseBodyPart.RIGHT_HIP, PoseBodyPart.RIGHT_SHOULDER));
            add(new Pair(PoseBodyPart.LEFT_HIP, PoseBodyPart.LEFT_KNEE));
            add(new Pair(PoseBodyPart.LEFT_KNEE, PoseBodyPart.LEFT_ANKLE));
            add(new Pair(PoseBodyPart.RIGHT_HIP, PoseBodyPart.RIGHT_KNEE));
            add(new Pair(PoseBodyPart.RIGHT_KNEE, PoseBodyPart.RIGHT_ANKLE));
        }
    };
    //只是姿势识别，去掉无用的识别点（左右眼、左右耳、鼻子）
    public static final List<PoseBodyPart> UNUSED_BODYPART = new ArrayList<PoseBodyPart>() {
        {
            add(PoseBodyPart.LEFT_EYE);
            add(PoseBodyPart.RIGHT_EYE);
            add(PoseBodyPart.LEFT_EAR);
            add(PoseBodyPart.RIGHT_EAR);
        }
    };
    //-------------- 骨架图识别 部分常量 -----------------------//
    //骨架图的Mobilenet识别方式的模型路径
    public final static String MN_MODEL_PATH = "skeleton.tflite";
    public final static String MN_LABEL_PATH = "labels.txt";
    //骨架图mobilenet模型使用的bitmap的宽、高
    public final static int DIM_PIXEL_HEIGHT = 96;
    public final static int DIM_PIXEL_WIDTH = 96;


    public final static int NUM_BYTES_PER_CHANNEL = 4;
    public static final float IMAGE_MEAN = 1.0f;
    public static final float IMAGE_STD = 127.0f;
    public static final int DIM_BATCH_SIZE = 1;
    public static final int DIM_PIXEL_SIZE = 3;
    public static final int FILTER_STAGES = 3;
    public static final float FILTER_FACTOR = 0.4f;

    public static final float MIN_CONFIDENCE = 0.5f;
    public static final float CIRCLE_RADIUS = 8.0f;

    //-------------- 人脸识别 部分常量 -----------------------//
    public final static String BF_MODEL_PATH = "face_detection_front.tflite";
    public final static int BF_MODEL_WIDTH = 128;
    public final static int BF_MODEL_HEIGHT = 128;
    public final static float BF_MIN_SCALE = 0.1484375f; //一个经验值
    public final static float BF_MAX_SCALE = 0.75f;
    public final static float BF_ANCHOR_OFFSET_X = 0.5f;
    public final static float BF_ANCHOR_OFFSET_Y = 0.5f;
    public final static int BF_NUM_LAYERS = 4;

    //-------------- 手势跟踪 部分常量 -----------------------//
    public final static String HT_MODEL_PATH = "hand_landmark.tflite";
    public final static String HT_3D_MODEL_PATH = "hand_landmark_3d.tflite";
    public static final int HT_MODEL_WIDTH = 256;
    public static final int HT_MODEL_HEIGHT = 256;
    public static final int HT_MODEL_POINT_NUM = 21;
}
