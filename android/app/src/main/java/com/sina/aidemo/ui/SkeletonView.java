package com.sina.aidemo.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.sina.aidemo.posenet.PosePerson;
import com.sina.aidemo.utils.AIImageUtils;


public class SkeletonView extends View {
    private PosePerson mPerson;

    public SkeletonView(Context context) {
        super(context);
    }

    public SkeletonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SkeletonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void drawSkeleton(PosePerson person) {
        this.mPerson = person;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        AIImageUtils.drawPoseNetSkeleton(mPerson, canvas);
    }
}
