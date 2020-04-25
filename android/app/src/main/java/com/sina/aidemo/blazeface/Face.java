package com.sina.aidemo.blazeface;

import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 面部数据结果
 */
public class Face {
    public List<SNFaceKeyPoint> keyPoints = new ArrayList<>();
    public Rect detectionRect = new Rect();
    public float scores = 0.f;

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("all score is " + scores);
        if (keyPoints != null) {
            for (int i = 0; i < keyPoints.size(); i++) {
                sb.append("i:" + i + "\n");
                sb.append(keyPoints.get(i).toString());
                sb.append("---------------");
            }
        }
        return sb.toString();
    }

    /**
     * 面部关键点，基本就是五官，一共六个点
     */
    public static class SNFaceKeyPoint {
        public SNFacePart facePart = SNFacePart.NOSE;
        public Point position;
        public float score = 0.f;

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("position:" + position.toString() + "\n");
            sb.append("score:" + score + "\n");
            return sb.toString();
        }
    }


    /**
     * 五官枚举
     */
    public enum SNFacePart {
        NOSE,
        MOUTH,
        LEFT_EYE,
        RIGHT_EYE,
        LEFT_EAR,
        RIGHT_EAR,
    }

    /**
     * 辅助数据结构：锚点
     */
    public static class SNAnthor {
        float h = 0.f;
        float w = 0.f;
        float x_center = 0.f;
        float y_center = 0.f;
    }


    /**
     * 辅助数据结构：检测值
     */
    public static class SNDetections {
        float class_id = 0.f;
        float height = 0.f;
        float width = 0.f;
        float score = 0.f;
        float xmin = 0.f;
        float ymin = 0.f;

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class_id:");
            sb.append(class_id);
            sb.append(",height:");
            sb.append(height);
            sb.append(",width:");
            sb.append(width);
            sb.append(",score:");
            sb.append(score);
            sb.append(",xmin:");
            sb.append(xmin);
            sb.append(",ymin:");
            sb.append(ymin);
            sb.append("\n");
            return sb.toString();
        }
    }

}
