package com.sina.aidemo.contours;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Pose {
    public List<PoseResult> results = new ArrayList<>();
    public String label = "";
    public int labelId = 0;

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("识别列表：\n");
        if (results != null) {
            for (PoseResult result : results) {
                sb.append(result.toString());
                sb.append("\n");
            }
        }
        sb.append("最终结果：" + label);
        return sb.toString();
    }
}
