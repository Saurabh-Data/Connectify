package com.connectify.com.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.connectify.com.R;
import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            goToNextActivity();
        }

        findViewById(R.id.getStarted).setOnClickListener(view -> goToNextActivity());
    }

    private void goToNextActivity() {
        Intent intent;
        if (auth.getCurrentUser() != null) {
            intent = new Intent(WelcomeActivity.this, MainActivity.class); // Redirect to main activity if user is already logged in
        } else {
            intent = new Intent(WelcomeActivity.this, LoginActivity.class); // Redirect to login activity if user is not logged in
        }
        startActivity(intent);
        finish();
    }
}
