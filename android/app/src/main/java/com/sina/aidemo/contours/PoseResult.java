package com.sina.aidemo.contours;

import android.support.annotation.NonNull;

public class PoseResult {
    public int labelId = 0;
    public String label = "";
    public float accuracy = 0.f;

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("label:");
        sb.append(label);
        sb.append("\n精确度：");
        sb.append(accuracy);
        return sb.toString();
    }
}
