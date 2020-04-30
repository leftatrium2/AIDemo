#import "BlazeFaceUtil.h"
#import "AIConstants.h"
#import "Face.h"

#define SCORE_CLIPPING_THRESH 100.f
#define BOX_COORD_OFFSET 0
#define APPLY_EXPONENTIAL_ON_BOX_SIZE NO
#define FLIP_VERTICALLY NO
#define REDUCE_BOXES_IN_LOWEST_LAYER NO
#define FIXED_ANCHOR_SIZE YES
#define MIN_SCORE_THRESH 0.75f
#define X_SCALE 128.f
#define Y_SCALE 128.f
#define W_SCALE 128.f
#define H_SCALE 128.f
#define INTERPOLATED_SCALE_ASPECT_RATIO 1.f
#define FEATURE_MAP_HEIGHT_SIZE 0

@implementation BlazeFaceUtil

+ (void) decodeBoxes:(float *) rawBoxes anchors:(NSArray *) anchors boxes:(float *) boxes {
    for (int i = 0; i < BF_NUM_BOXES; i++) {
        int box_offset = i * BF_NUM_COORDS + BOX_COORD_OFFSET;
        float x_center = rawBoxes[box_offset];
        float y_center = rawBoxes[box_offset + 1];
        float w = rawBoxes[box_offset + 2];
        float h = rawBoxes[box_offset + 3];
        
        Anchor *anthor = (Anchor *)[anchors objectAtIndex:i];
        x_center = x_center / X_SCALE * anthor.w + anthor.x_center;
        y_center = y_center / Y_SCALE * anthor.h + anthor.y_center;
        h = h / H_SCALE * anthor.h;
        w = w / W_SCALE * anthor.w;
        
        float ymin = y_center - h / 2.0f;
        float xmin = x_center - w / 2.0f;
        float ymax = y_center + h / 2.0f;
        float xmax = x_center + w / 2.0f;
        boxes[i * BF_NUM_COORDS + 0] = ymin;
        boxes[i * BF_NUM_COORDS + 1] = xmin;
        boxes[i * BF_NUM_COORDS + 2] = ymax;
        boxes[i * BF_NUM_COORDS + 3] = xmax;
        for (int k = 0; k < BF_NUM_KEYPOINTS; k++) {
            int offset = i * BF_NUM_COORDS + BF_KEYPOINT_COORD_OFFSET + k * BF_NUM_VALUES_PER_KEYPOINT;
            float keypoint_x = rawBoxes[offset];
            float keypoint_y = rawBoxes[offset + 1];
            boxes[offset] = keypoint_x / X_SCALE * anthor.w + anthor.x_center;
            boxes[offset + 1] = keypoint_y / Y_SCALE * anthor.h + anthor.y_center;
        }
    }
}

+ (NSArray *) convertToDetections:(float *) detection_boxes detection_scores:(float *) detection_scores detection_classes:(float *) detection_classes {
    NSMutableArray *output_detections = [NSMutableArray array];
    for (int i = 0; i < BF_NUM_BOXES; i++) {
        if (detection_scores[i] < MIN_SCORE_THRESH) {
            continue;
        }
        int box_offset = i * BF_NUM_COORDS;
        Detections *detection = [[Detections alloc] init];
        detection.score = detection_scores[i];
        detection.class_id = detection_classes[i];
        detection.xmin = detection_boxes[box_offset + 1];
        if (FLIP_VERTICALLY) {
            detection.ymin = 1 - detection_boxes[box_offset + 2];
        } else {
            detection.ymin = detection_boxes[box_offset + 0];
        }
        detection.width = detection_boxes[box_offset + 3] - detection_boxes[box_offset + 1];
        detection.height = detection_boxes[box_offset + 2] - detection_boxes[box_offset + 0];
        [output_detections addObject:detection];
    }
    return output_detections;
}

+ (NSArray *)processCPU:(float *)rawBoxes raw_scores:(float *)raw_scores anchors_:(NSArray *)anchors_ {
    float *boxes = malloc(BF_NUM_BOXES * BF_NUM_COORDS * sizeof(float));
    NSAssert(boxes, @"boxes is null!!");
    memset(boxes, 0, sizeof(float) * BF_NUM_BOXES * BF_NUM_COORDS);
    [BlazeFaceUtil decodeBoxes:rawBoxes anchors:anchors_ boxes:boxes];
    float detection_scores[BF_NUM_BOXES];
    float detection_classes[BF_NUM_BOXES];
    for(int i=0;i<BF_NUM_BOXES;i++){
        int class_id = -1;
        float max_score = FLT_MIN;
        for(int score_idx=0;score_idx<BF_NUM_CLASSES;score_idx++){
            float score = raw_scores[i*BF_NUM_CLASSES+score_idx];
            if(score < -SCORE_CLIPPING_THRESH){
                score = -SCORE_CLIPPING_THRESH;
            }else if (score > SCORE_CLIPPING_THRESH) {
                score = SCORE_CLIPPING_THRESH;
            }
            //sigmoid (0,1)
            score = (float) (1.0 / (1.0 + expf(-score)));
            if (max_score < score) {
                max_score = score;
                class_id = score_idx;
            }
        }
        detection_scores[i] = max_score;
        detection_classes[i] = class_id;
    }
    NSArray *ret = [BlazeFaceUtil convertToDetections:boxes detection_scores:detection_scores detection_classes:detection_classes];
    if(boxes!=NULL){
        free(boxes);
    }
    return ret;
}

+ (Detections *)origNms:(NSArray *)detections threshold:(float)threshold {
    if(detections==nil || detections.count == 0){
        return nil;
    }
    uint8_t length = detections.count;
    float x1[length];
    float x2[length];
    float y1[length];
    float y2[length];
    float s[length];
    for (int i = 0; i < length; i++) {
        Detections *detection = [detections objectAtIndex:i];
        x1[i] = detection.xmin;
        x2[i] = detection.xmin + detection.width;
        y1[i] = detection.ymin;
        y2[i] = detection.ymin + detection.height;
        s[i] = detection.score;
    }
    Detections *temp = [[Detections alloc] init];
    float max = FLT_MIN;
    for (int i = 0; i < length; i++) {
        if (max < s[i]) {
            temp.class_id = i;
            temp.xmin = x1[i];
            temp.ymin = y1[i];
            temp.height = y2[i] - y1[i];
            temp.width = x2[i] - x1[i];
            temp.score = s[i];
            max = s[i];
        }
    }
    return temp;
}

+ (BOOL) rawBoxWithReshape:(float *)floatArr ret:(float *) ret {
    float (*floatArrPoint)[1][BF_NUM_BOXES][BF_NUM_COORDS];
    floatArrPoint = (float (*)[1][BF_NUM_BOXES][BF_NUM_COORDS]) floatArr;
    
    memset(ret, 0, sizeof(float) * BF_NUM_BOXES * BF_NUM_COORDS);
    int key = 0;
    for (int j = 0; j < BF_NUM_BOXES; j++) {
        for (int k = 0; k < BF_NUM_COORDS; k++) {
            key = j * BF_NUM_COORDS + k;
            ret[key] = (*floatArrPoint)[0][j][k];
        }
    }
    return YES;
}

+ (BOOL)rawScoresWithReshape:(float *)floatArr ret:(float *)ret {
    float (*floatArrPoint)[1][BF_NUM_BOXES][1];
    floatArrPoint = (float (*)[1][BF_NUM_BOXES][1]) floatArr;
    
    memset(ret, 0, sizeof(float) * BF_NUM_BOXES * 1);
    int key = 0;
    for (int j = 0; j < BF_NUM_BOXES; j++) {
        key = j;
        ret[key] = (*floatArrPoint)[0][j][0];
    }
    return YES;
    return YES;
}

+ (NSArray *)genAnchor {
    int FEATURE_MAP_HEIGHT[] = {};
    int FEATURE_MAP_WIDTH[] = {};
    int STRIDES[] = {8, 16, 16, 16};
    int STRIDES_SIZE = 4;
    float ASPECT_RATIOS[] = {1.0f};
    int ASPECT_RATIOS_SIZE = 1;
    NSMutableArray *anchors = [NSMutableArray array];
    int layer_id = 0;
    while (layer_id < STRIDES_SIZE) {
        NSMutableArray *anchor_height = [NSMutableArray array];
        NSMutableArray *anchor_width = [NSMutableArray array];
        NSMutableArray *aspect_ratios = [NSMutableArray array];
        NSMutableArray *scales = [NSMutableArray array];
        
        int last_same_stride_layer = layer_id;
        while (last_same_stride_layer < STRIDES_SIZE && STRIDES[last_same_stride_layer] == STRIDES[layer_id]) {
            float scale = BF_MIN_SCALE + (BF_MAX_SCALE - BF_MIN_SCALE) * 1.f * last_same_stride_layer / (STRIDES_SIZE - 1.f);
            if (last_same_stride_layer == 0 && REDUCE_BOXES_IN_LOWEST_LAYER) {
                [aspect_ratios addObject:[NSNumber numberWithFloat:1.f]];
                [aspect_ratios addObject:[NSNumber numberWithFloat:2.f]];
                [aspect_ratios addObject:[NSNumber numberWithFloat:0.5f]];
                [scales addObject:[NSNumber numberWithFloat:0.1f]];
                [scales addObject:[NSNumber numberWithFloat:scale]];
                [scales addObject:[NSNumber numberWithFloat:scale]];
            }else{
                for (int aspect_ratio_id = 0; aspect_ratio_id < ASPECT_RATIOS_SIZE; aspect_ratio_id++) {
                    [aspect_ratios addObject:[NSNumber numberWithFloat:ASPECT_RATIOS[aspect_ratio_id]]];
                    [scales addObject:[NSNumber numberWithFloat:scale]];
                }
                if (INTERPOLATED_SCALE_ASPECT_RATIO > 0.f) {
                    float scale_next = 1.f;
                    if (last_same_stride_layer != (STRIDES_SIZE - 1)) {
                        scale_next = BF_MIN_SCALE + (BF_MAX_SCALE - BF_MIN_SCALE) * 1.f * (last_same_stride_layer + 1) / (STRIDES_SIZE - 1.f);
                    }
                    [scales addObject:[NSNumber numberWithFloat:sqrtf(scale * scale_next)]];
                    [aspect_ratios addObject:[NSNumber numberWithFloat:INTERPOLATED_SCALE_ASPECT_RATIO]];
                }
            }
            last_same_stride_layer += 1;
        }
        for (int i = 0; i < aspect_ratios.count; i++) {
            float ratio_sqrts = sqrtf([[aspect_ratios objectAtIndex:i] floatValue]);
            [anchor_height addObject:[NSNumber numberWithInt:((int) ([[scales objectAtIndex:i] floatValue] / ratio_sqrts))]];
            [anchor_width addObject:[NSNumber numberWithInt:((int) ([[scales objectAtIndex:i] floatValue] * ratio_sqrts))]];
        }
        int feature_map_height = 0;
        int feature_map_width = 0;
        if (FEATURE_MAP_HEIGHT_SIZE > 0) {
            feature_map_height = FEATURE_MAP_HEIGHT[layer_id];
            feature_map_width = FEATURE_MAP_WIDTH[layer_id];
        } else {
            int stride = STRIDES[layer_id];
            feature_map_height = (int) ceil(1.f * BF_INPUT_HEIGHT / stride);
            feature_map_width = (int) ceil(1.f * BF_INPUT_WIDTH / stride);
        }
        for (int y = 0; y < feature_map_height; y++) {
            for (int x = 0; x < feature_map_width; x++) {
                for (int anchor_id = 0; anchor_id < anchor_height.count; anchor_id++) {
                    float x_center = (x + BF_ANCHOR_OFFSET_X) * 1.f / feature_map_width;
                    float y_center = (y + BF_ANCHOR_OFFSET_Y) * 1.f / feature_map_height;
                    float w = 0.f;
                    float h = 0.f;
                    if (FIXED_ANCHOR_SIZE) {
                        w = 1.f;
                        h = 1.f;
                    } else {
                        w = [[anchor_width objectAtIndex:anchor_id] floatValue];
                        h = [[anchor_height objectAtIndex:anchor_id] floatValue];
                    }
                    Anchor *anchor = [[Anchor alloc] init];
                    anchor.x_center = x_center;
                    anchor.y_center = y_center;
                    anchor.w = w;
                    anchor.h = h;
                    [anchors addObject:anchor];
                }
            }
        }
        layer_id = last_same_stride_layer;
    }
    return anchors;
}


@end
