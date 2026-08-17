package com.example.exampleapp;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("안드로이드 예시 앱");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        TextView counter = new TextView(this);
        counter.setText("클릭 횟수: 0");
        counter.setTextSize(20);
        counter.setGravity(Gravity.CENTER);

        Button button = new Button(this);
        button.setText("클릭하기");
        button.setOnClickListener(v -> {
            count++;
            counter.setText("클릭 횟수: " + count);
        });

        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        root.addView(counter, new LinearLayout.LayoutParams(-1, -2));
        root.addView(button, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }
}
