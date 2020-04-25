package com.sina.aidemo.handtracking;

import android.support.annotation.NonNull;

import com.sina.aidemo.utils.AIConstants;

import java.util.ArrayList;
import java.util.List;

public class Hand3D {
    public List<SNHandPoint3D> points = new ArrayList<>();

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (points != null && points.size() == AIConstants.HT_MODEL_POINT_NUM) {
            sb.append(points.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public static class SNHandPoint3D {
        public float x;
        public float y;
        public float z;

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("point,x:");
            sb.append(x);
            sb.append("y:");
            sb.append(y);
            sb.append("z:");
            sb.append(z);
            return sb.toString();
        }
    }
}
