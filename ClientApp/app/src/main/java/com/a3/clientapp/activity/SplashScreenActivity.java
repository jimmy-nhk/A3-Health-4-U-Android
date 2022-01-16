package com.a3.clientapp.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.a3.clientapp.R;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Intent intent = new Intent(SplashScreenActivity.this, LogInActivity.class);
                startActivity(intent);
                finish();
            }
        });
        thread.start();
    }
}