package com.github.xiaogqiong0v0.shadowview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class SingleTestActivity extends AppCompatActivity {

    public static void start(AppCompatActivity activity) {
        activity.startActivity(new android.content.Intent(activity, SingleTestActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_test);
    }
}