package com.sina.aidemo.utils;

public class FilterUtils {
    public static float range(final int percentage, final float start, final float end) {
        return (end - start) * percentage / 100.0f + start;
    }

    public static int progress(final float factor, final float start, final float end) {
        return (int) ((factor - start) * 100.f / (end - start));
    }
}
