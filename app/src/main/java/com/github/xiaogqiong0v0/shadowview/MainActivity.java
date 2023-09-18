package com.github.xiaogqiong0v0.shadowview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnFirstTest = findViewById(R.id.btn_first_test);
        btnFirstTest.setOnClickListener(v -> {
            SingleTestActivity.start(MainActivity.this);
        });
    }
}