package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;

/**
 * The initial activity shown to the user on application launch.
 * Handles device identification and Firestore registration/sync before
 * navigating to the main activity.
 */
public class SplashActivity extends AppCompatActivity {
    /**
     * Initializes the splash screen, handles device ID logic, and manages navigation.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        String deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        TextView deviceIdText = findViewById(R.id.deviceIdText);
        // Display a shortened version of the UUID (first 8 characters)
        String displayId = DeviceIdManager.getShortenedId(deviceId);
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
                navigateToMain();
            } else {
                // Firestore unreachable — still let them in, retry later
                Log.e("Firestore", "Failed to check user", task.getException());
                navigateToMain();
            }
        });
    }

    /**
     * Navigates to the MainActivity and finishes the splash activity.
     */
    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
