package com.sina.aidemo.posenet;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class PosePerson {
    public List<PoseKeyPoint> keyPoints = new ArrayList<>();
    public float score = 0.f;

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=================\n");
        sb.append("keypoint:\n");
        if (keyPoints == null || keyPoints.size() == 0) {
            sb.append("none!!!");
        } else {
            for (PoseKeyPoint points : keyPoints) {
                sb.append("point:\n");
                sb.append(points.toString());
                sb.append("\n");
            }
        }
        sb.append("score:\n");
        sb.append(score);
        sb.append("\n");
        sb.append("=================\n");
        return sb.toString();
    }
}
