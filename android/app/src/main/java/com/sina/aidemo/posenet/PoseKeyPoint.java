package com.sina.aidemo.posenet;


import android.graphics.Point;
import android.support.annotation.NonNull;

public class PoseKeyPoint {
    public PoseBodyPart bodyPart = PoseBodyPart.NOSE;
    public Point position = new Point();
    public float score = 0.f;


    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BodyPart:\n");
        sb.append(bodyPart.toString());
        sb.append("\nPosition:\n");
        sb.append(position);
        sb.append("\n");
        return sb.toString();
    }
}
