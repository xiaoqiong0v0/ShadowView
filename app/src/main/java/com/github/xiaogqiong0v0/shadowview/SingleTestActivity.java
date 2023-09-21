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
        LinearLayout testView = findViewById(R.id.test_view);
        Button testBtn = findViewById(R.id.test_btn);
        testBtn.setOnClickListener(v -> {
            testView.measure(0, 0);
        });
    }
}