//
//  姿势识别，宏定义
//

#ifndef SNAIConstants_h
#define SNAIConstants_h

#pragma mark 常量部分
#define POSE_THREAD_COUNT 2
#define POSE_FRAMERATE 30
//通道数
#define POSE_RGBPIXELCHANNELS 3
#define POSENET_MODEL_NAME @"posenet_model"
#define SKELETON_MODEL_NAME @"skeleton"

#pragma mark input
#define INPUT_BATCH_SIZE 1
#define INPUT_WIDTH 257
#define INPUT_HEIGHT 353
#define INPUT_CHANNEL_SIZE 3
#define INPUT_CAPTURE_CHANNEL_NUM 4
#define INPUT_POSENET_CHANNEL_NUM 3

#pragma mark output
#define OUTPUT_BATCH_SIZE 1
#define OUTPUT_WIDTH 17
#define OUTPUT_HEIGHT 23
#define OUTPUT_KEYPONINT_SIZE 17
#define OUTPUT_OFFSET_SIZE 34

#pragma mark skeleton
#define SK_INPUT_WIDTH 96
#define SK_INPUT_HEIGHT 96
#define SK_NUM_BYTES_PER_CHANNEL 4
#define SK_IMAGE_MEAN 1.f
#define SK_IMAGE_STD 127.0f
#define SK_DIM_BATCH_SIZE 1
#define SK_DIM_PIXEL_SIZE 3
#define SK_FILTER_STAGES 3
#define SK_FILTER_FACTOR 0.4f
#define SK_MIN_CONFIDENCE 0.5f
#define SK_CIRCLE_RADIUS 8.0f
#define SK_OUTPUT_MODEL_NUM 3

#define SK_SHOW_IN_SCREEN YES

#define PAIR_JOINTS_NUM 12


#pragma mark 骨架图关键点
#define NOSE 0
#define LEFT_EYE 1
#define RIGHT_EYE 2
#define LEFT_EAR 3
#define RIGHT_EAR 4
#define LEFT_SHOULDER 5
#define RIGHT_SHOULDER 6
#define LEFT_ELBOW 7
#define RIGHT_ELBOW 8
#define LEFT_WRIST 9
#define RIGHT_WRIST 10
#define LEFT_HIP 11
#define RIGHT_HIP 12
#define LEFT_KNEE 13
#define RIGHT_KNEE 14
#define LEFT_ANKLE 15
#define RIGHT_ANKLE 16

typedef unsigned int SNBODYPART;

static NSString *getBodyPartName(SNBODYPART part) {
    switch (part) {
        case NOSE:
            return @"鼻子";
            break;
            
        case LEFT_EYE:
            return @"左眼";
            break;
        
        case RIGHT_EYE:
            return @"右眼";
            break;
            
        case LEFT_EAR:
            return @"左耳";
            break;
        
        case RIGHT_EAR:
            return @"右耳";
            break;
            
        case LEFT_SHOULDER:
            return @"左肩膀";
            break;
            
        case RIGHT_SHOULDER:
            return @"右肩膀";
            break;
            
        case LEFT_ELBOW:
            return @"左手肘";
            break;
            
        case RIGHT_ELBOW:
            return @"右手肘";
            break;
            
        case LEFT_WRIST:
            return @"左手腕";
            break;
            
        case RIGHT_WRIST:
            return @"左手腕";
            break;
            
        case LEFT_HIP:
            return @"左臀";
            break;
            
        case RIGHT_HIP:
            return @"右臀";
            break;
            
        case LEFT_KNEE:
            return @"左膝盖";
            break;
            
        case RIGHT_KNEE:
            return @"右膝盖";
            break;
            
        case LEFT_ANKLE:
            return @"左脚踝";
            break;
            
        case RIGHT_ANKLE:
            return @"右脚踝";
            break;
        default:
            return @"UNKNOWN";
            break;
    }
}

#pragma mark 几个辅助数据结构
typedef struct _PosePair {
    int row;
    int col;
} PosePair; //用来存储M*N行列中最大可能性的行列数

typedef struct _PairJoints {
    int from;
    int to;
} PairJoints; //用来存储关节方向的行列数

#pragma mark 手势侦测
#define HT_MODEL_NAME @"hand_landmark"
#define HT_MODEL_3D_NAME @"hand_landmark_3d"
#define HT_INPUT_WIDTH 256
#define HT_INPUT_HEIGHT 256
#define HT_INPUT_CHANNEL_NUM 3
#define HT_MODEL_POINT_NUM 21

#pragma mark 面部侦测
#define BF_MODEL_NAME @"face_detection_front"
#define BF_INPUT_WIDTH 128
#define BF_INPUT_HEIGHT 128
#define BF_INPUT_CHANNEL_NUM 3
#define BF_NUM_CLASSES 1
#define BF_NUM_BOXES 896
#define BF_NUM_COORDS 16
#define BF_NUM_KEYPOINTS 6
#define BF_KEYPOINT_COORD_OFFSET 4
#define BF_NUM_VALUES_PER_KEYPOINT 2
#define BF_MAX_SCALE 0.75f
#define BF_MIN_SCALE 0.1484375f
#define BF_ANCHOR_OFFSET_X 0.5f
#define BF_ANCHOR_OFFSET_Y 0.5f

#endif /* SNAIConstants_h */
