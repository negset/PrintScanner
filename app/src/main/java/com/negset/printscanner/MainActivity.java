package com.negset.printscanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonOrientation = findViewById(R.id.button_start);
        buttonOrientation.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
            startActivity(intent);
        });
    }
}