package com.sina.aidemo.posenet;


import android.support.annotation.NonNull;

public class PosePosition {
    public int x = 0;
    public int y = 0;

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("x:");
        sb.append(x);
        sb.append(" ||| y:");
        sb.append(y);
        return sb.toString();
    }
}
