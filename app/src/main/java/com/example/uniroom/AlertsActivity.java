package com.example.uniroom;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AlertsActivity extends AppCompatActivity {

    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alerts);

        backButton =
                findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}