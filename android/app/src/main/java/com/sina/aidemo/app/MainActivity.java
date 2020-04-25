package com.sina.aidemo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.sina.aidemo.MyApplication;
import com.sina.aidemo.R;

public class MainActivity extends Activity implements View.OnClickListener {
    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;
    private Button mBtn4;
    private Button mBtn5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication application = (MyApplication) MyApplication.getApplication();
        if (!application.isCanRunning()) {
            new AlertDialog.Builder(this).setMessage("当前设备太陈旧，无法使用此功能").setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).create().show();
        }
        setContentView(R.layout.activity_main);

        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setOnClickListener(this);
        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setOnClickListener(this);
        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setOnClickListener(this);
        mBtn5 = (Button) findViewById(R.id.btn5);
        mBtn5.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mBtn1) {
            Intent intent = new Intent(MainActivity.this, PoseDemoActivity.class);
            startActivity(intent);
        } else if (v == mBtn2) {
            Intent intent = new Intent(MainActivity.this, PoseActivity.class);
            startActivity(intent);
        } else if (v == mBtn3) {
            Intent intent = new Intent(MainActivity.this, BlazeFaceActivity.class);
            startActivity(intent);
        }else if (v == mBtn5) {
            Intent intent = new Intent(MainActivity.this, HandTrackingActivity.class);
            startActivity(intent);
        }
    }
}
