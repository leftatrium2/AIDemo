package com.sina.aidemo.handtracking;

import android.graphics.PointF;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Hand {
    public List<PointF> points = new ArrayList<>();

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (points != null && points.size() == 21) {
            for (PointF point : points) {
                sb.append("poisition:");
                sb.append(point.toString());
            }
        }
        return sb.toString();
    }
}
