package com.sina.aidemo.blazeface;

import android.util.Log;

import com.sina.aidemo.utils.AIConstants;

import java.util.ArrayList;
import java.util.List;

public class BlazeFaceUtil {
    public static final int[] FEATURE_MAP_WIDTH = {};
    public static final int FEATURE_MAP_WIDTH_SIZE = 0;
    public static final int[] FEATURE_MAP_HEIGHT = {};
    public static final int FEATURE_MAP_HEIGHT_SIZE = 0;
    public static final int[] STRIDES = {8, 16, 16, 16};
    public static final float[] ASPECT_RATIOS = {1.0f};
    public static final boolean REDUCE_BOXES_IN_LOWEST_LAYER = false;
    public static final float INTERPOLATED_SCALE_ASPECT_RATIO = 1.0f;
    public static final boolean FIXED_ANCHOR_SIZE = true;

    public static final int NUM_CLASSES = 1;
    public static final int NUM_BOXES = 896;
    public static final int NUM_COORDS = 16;
    public static final int KEYPOINT_COORD_OFFSET = 4;
    public static final int[] IGNORE_CLASSES = {};
    public static final float SCORE_CLIPPING_THRESH = 100.f;
    public static final float MIN_SCORE_THRESH = 0.75f;
    public static final int NUM_KEYPOINTS = 6;
    public static final int NUM_VALUES_PER_KEYPOINT = 2;
    public static final int BOX_COORD_OFFSET = 0;
    public static final float X_SCALE = 128.f;
    public static final float Y_SCALE = 128.f;
    public static final float W_SCALE = 128.f;
    public static final float H_SCALE = 128.f;
    public static final boolean APPLY_EXPONENTIAL_ON_BOX_SIZE = false;
    public static final boolean REVERSE_OUTPUT_ORDER = true;
    public static final boolean SIGMOID_SCORE = true;
    public static final boolean FLIP_VERTICALLY = false;

    public static float[] rawBoxWithReshape(float[][][] floatArr) {
        int length1 = floatArr[0].length;
        int length2 = floatArr[0][0].length;
        int length = length1 * length2;
        float[] ret = new float[length];
        int key = 0;
        for (int j = 0; j < length1; j++) {
            for (int k = 0; k < length2; k++) {
                key = j * length2 + k;
                ret[key] = floatArr[0][j][k];
            }
        }
        return ret;
    }

    public static List<Face.SNAnthor> genAnthor() {
        int STRIDES_SIZE = STRIDES.length;
        int ASPECT_RATIOS_SIZE = ASPECT_RATIOS.length;
        List<Face.SNAnthor> anchors = new ArrayList<>();
        if (STRIDES.length != AIConstants.BF_NUM_LAYERS) {
            Log.e(AIConstants.TAG, "strides_size and num_layers must be equal.");
            return null;
        }
        int layer_id = 0;
        while (layer_id < STRIDES_SIZE) {
            List<Integer> anchor_height = new ArrayList<>();
            List<Integer> anchor_width = new ArrayList<>();
            List<Float> aspect_ratios = new ArrayList<>();
            List<Float> scales = new ArrayList<>();

            int last_same_stride_layer = layer_id;
            while (last_same_stride_layer < STRIDES_SIZE && STRIDES[last_same_stride_layer] == STRIDES[layer_id]) {
                float scale = AIConstants.BF_MIN_SCALE + (AIConstants.BF_MAX_SCALE - AIConstants.BF_MIN_SCALE) * 1.f * last_same_stride_layer / (STRIDES_SIZE - 1.f);
                if (last_same_stride_layer == 0 && REDUCE_BOXES_IN_LOWEST_LAYER) {
                    aspect_ratios.add(1.f);
                    aspect_ratios.add(2.f);
                    aspect_ratios.add(0.5f);
                    scales.add(0.1f);
                    scales.add(scale);
                    scales.add(scale);
                } else {
                    for (int aspect_ratio_id = 0; aspect_ratio_id < ASPECT_RATIOS_SIZE; aspect_ratio_id++) {
                        aspect_ratios.add(ASPECT_RATIOS[aspect_ratio_id]);
                        scales.add(scale);
                    }
                    if (INTERPOLATED_SCALE_ASPECT_RATIO > 0.f) {
                        float scale_next = 1.f;
                        if (last_same_stride_layer != (STRIDES_SIZE - 1)) {
                            scale_next = AIConstants.BF_MIN_SCALE + (AIConstants.BF_MAX_SCALE - AIConstants.BF_MIN_SCALE) * 1.f * (last_same_stride_layer + 1) / (STRIDES_SIZE - 1.f);
                        }
                        scales.add((float) Math.sqrt(scale * scale_next));
                        aspect_ratios.add(INTERPOLATED_SCALE_ASPECT_RATIO);
                    }
                }
                last_same_stride_layer += 1;
            }
            for (int i = 0; i < aspect_ratios.size(); i++) {
                double ratio_sqrts = Math.sqrt(aspect_ratios.get(i).floatValue());
                anchor_height.add((int) (scales.get(i) / ratio_sqrts));
                anchor_width.add((int) (scales.get(i) * ratio_sqrts));
            }
            int feature_map_height = 0;
            int feature_map_width = 0;
            if (FEATURE_MAP_HEIGHT_SIZE > 0) {
                feature_map_height = FEATURE_MAP_HEIGHT[layer_id];
                feature_map_width = FEATURE_MAP_WIDTH[layer_id];
            } else {
                int stride = STRIDES[layer_id];
                feature_map_height = (int) Math.ceil(1.f * AIConstants.BF_MODEL_HEIGHT / stride);
                feature_map_width = (int) Math.ceil(1.f * AIConstants.BF_MODEL_WIDTH / stride);
            }
            for (int y = 0; y < feature_map_height; y++) {
                for (int x = 0; x < feature_map_width; x++) {
                    for (int anchor_id = 0; anchor_id < anchor_height.size(); anchor_id++) {
                        float x_center = (x + AIConstants.BF_ANCHOR_OFFSET_X) * 1.f / feature_map_width;
                        float y_center = (y + AIConstants.BF_ANCHOR_OFFSET_Y) * 1.f / feature_map_height;
                        float w = 0.f;
                        float h = 0.f;
                        if (FIXED_ANCHOR_SIZE) {
                            w = 1.f;
                            h = 1.f;
                        } else {
                            w = anchor_width.get(anchor_id);
                            h = anchor_height.get(anchor_id);
                        }
                        Face.SNAnthor anthor = new Face.SNAnthor();
                        anthor.x_center = x_center;
                        anthor.y_center = y_center;
                        anthor.w = w;
                        anthor.h = h;
                        anchors.add(anthor);
                    }
                }
            }
            layer_id = last_same_stride_layer;
        }
        return anchors;
    }

    public static float[] decodeBoxes(float[] rawBoxes, List<Face.SNAnthor> anchors) {
        float[] boxes = new float[NUM_BOXES * NUM_COORDS];
        for (int i = 0; i < NUM_BOXES; i++) {
            int box_offset = i * NUM_COORDS + BOX_COORD_OFFSET;
            float y_center = rawBoxes[box_offset];
            float x_center = rawBoxes[box_offset + 1];
            float h = rawBoxes[box_offset + 2];
            float w = rawBoxes[box_offset + 3];
            if (REVERSE_OUTPUT_ORDER) {
                x_center = rawBoxes[box_offset];
                y_center = rawBoxes[box_offset + 1];
                w = rawBoxes[box_offset + 2];
                h = rawBoxes[box_offset + 3];
            }
            x_center = x_center / X_SCALE * anchors.get(i).w + anchors.get(i).x_center;
            y_center = y_center / Y_SCALE * anchors.get(i).h + anchors.get(i).y_center;
            if (APPLY_EXPONENTIAL_ON_BOX_SIZE) {
                h = (float) (Math.exp(h / H_SCALE) * anchors.get(i).h);
                w = (float) (Math.exp(w / W_SCALE) * anchors.get(i).w);
            } else {
                h = h / H_SCALE * anchors.get(i).h;
                w = w / W_SCALE * anchors.get(i).w;
            }
            float ymin = y_center - h / 2.0f;
            float xmin = x_center - w / 2.0f;
            float ymax = y_center + h / 2.0f;
            float xmax = x_center + w / 2.0f;
            boxes[i * NUM_COORDS + 0] = ymin;
            boxes[i * NUM_COORDS + 1] = xmin;
            boxes[i * NUM_COORDS + 2] = ymax;
            boxes[i * NUM_COORDS + 3] = xmax;
            for (int k = 0; k < NUM_KEYPOINTS; k++) {
                int offset = i * NUM_COORDS + KEYPOINT_COORD_OFFSET + k * NUM_VALUES_PER_KEYPOINT;
                float keypoint_y = rawBoxes[offset];
                float keypoint_x = rawBoxes[offset + 1];
                if (REVERSE_OUTPUT_ORDER) {
                    keypoint_x = rawBoxes[offset];
                    keypoint_y = rawBoxes[offset + 1];
                }
                boxes[offset] = keypoint_x / X_SCALE * anchors.get(i).w + anchors.get(i).x_center;
                boxes[offset + 1] = keypoint_y / Y_SCALE * anchors.get(i).h + anchors.get(i).y_center;
            }
        }
        return boxes;
    }

    public static List<Face.SNDetections> processCPU(float[] rawBoxes, float[] raw_scores, List<Face.SNAnthor> anchors_) {
        float[] boxes = BlazeFaceUtil.decodeBoxes(rawBoxes, anchors_);
        float[] detection_scores = new float[NUM_BOXES];
        float[] detection_classes = new float[NUM_BOXES];
        for (int i = 0; i < NUM_BOXES; i++) {
            int class_id = -1;
            float max_score = Float.MIN_VALUE;
            for (int score_idx = 0; score_idx < NUM_CLASSES; score_idx++) {
                float score = raw_scores[i * NUM_CLASSES + score_idx];
                if (SIGMOID_SCORE) {
                    if (SCORE_CLIPPING_THRESH > 0) {
                        if (score < -SCORE_CLIPPING_THRESH) {
                            score = -SCORE_CLIPPING_THRESH;
                        } else if (score > SCORE_CLIPPING_THRESH) {
                            score = SCORE_CLIPPING_THRESH;
                        }
                    }
                    score = (float) (1.0 / (1.0 + Math.exp(-score)));
                    if (max_score < score) {
                        max_score = score;
                        class_id = score_idx;
                    }
                }
            }
            detection_scores[i] = max_score;
            detection_classes[i] = class_id;
        }

        return convertToDetections(boxes, detection_scores, detection_classes);
    }

    public static List<Face.SNDetections> convertToDetections(float[] detection_boxes, float[] detection_scores, float[] detection_classes) {
        List<Face.SNDetections> output_detections = new ArrayList<>();
        for (int i = 0; i < NUM_BOXES; i++) {
            if (detection_scores[i] < MIN_SCORE_THRESH) {
                continue;
            }
            /**
             * detection = ConvertToDetection(
             *             detection_boxes[box_offset + 0], detection_boxes[box_offset + 1],
             *             detection_boxes[box_offset + 2], detection_boxes[box_offset + 3],
             *             detection_scores[i], detection_classes[i], options.flip_vertically)
             */
            int box_offset = i * NUM_COORDS;
            Face.SNDetections detection = new Face.SNDetections();
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
            output_detections.add(detection);
        }
        return output_detections;
    }

    public static Face.SNDetections origNms(List<Face.SNDetections> detections, float threshold) {
        if (detections == null || detections.size() == 0) {
            return null;
        }
        int length = detections.size();
        float[] x1 = new float[length];
        float[] x2 = new float[length];
        float[] y1 = new float[length];
        float[] y2 = new float[length];
        float[] s = new float[length];
        for (int i = 0; i < length; i++) {
            x1[i] = detections.get(i).xmin;
            x2[i] = detections.get(i).xmin + detections.get(i).width;
            y1[i] = detections.get(i).ymin;
            y2[i] = detections.get(i).ymin + detections.get(i).height;
            s[i] = detections.get(i).score;
        }

        Face.SNDetections temp = new Face.SNDetections();
        float max = Float.MIN_VALUE;
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

    public static float[] multiply(float[] x1, float[] x2, float[] y1, float[] y2) {
        float[] ret = new float[x1.length];
        for (int i = 0; i < x1.length; i++) {
            float temp_x = x2[i] - x1[i] + 1;
            float temp_y = y2[i] - y1[i] + 1;
            ret[i] = temp_x * temp_y;
        }
        return ret;
    }
}
