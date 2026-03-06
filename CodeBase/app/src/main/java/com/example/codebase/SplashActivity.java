package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        String deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        TextView deviceIdText = findViewById(R.id.deviceIdText);
        // Display a shortened version of the UUID (first 8 characters)
        String displayId = deviceId.length() > 8 ? deviceId.substring(0, 8) : deviceId;
        deviceIdText.setText("Device ID: " + displayId + "...");

        DocumentReference userRef = AppDatabase.getInstance()
                .usersRef
                .document(deviceId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    // First launch — register this device as a new user
                    userRef.set(new User(deviceId))
                            .addOnSuccessListener(v ->
                                    Log.d("Firestore", "New user registered: " + deviceId))
                            .addOnFailureListener(e ->
                                    Log.e("Firestore", "Registration failed", e));
                }
                // Navigate regardless — existing or new user
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                // Firestore unreachable — still let them in, retry later
                Log.e("Firestore", "Failed to check user", task.getException());
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }
}
