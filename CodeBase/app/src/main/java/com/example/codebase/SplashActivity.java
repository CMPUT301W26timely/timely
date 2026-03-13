package com.example.codebase;

import android.content.Intent;
import android.content.SharedPreferences;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        String deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        TextView deviceIdText = findViewById(R.id.deviceIdText);
        String displayId = DeviceIdManager.getShortenedId(deviceId);
        deviceIdText.setText("Device ID: " + displayId + "...");

        DocumentReference userRef = AppDatabase.getInstance()
                .usersRef
                .document(deviceId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    userRef.set(new User(deviceId))
                            .addOnSuccessListener(v -> {
                                Log.d("Firestore", "New user registered: " + deviceId);
                                launchProfileCreation();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Registration failed", e);
                                navigateToMain();
                            });
                } else {
                    navigateToMain();
                }
            } else {
                Log.e("Firestore", "Failed to check user", task.getException());
                navigateToMain();
            }
        });
    }

    private void navigateToMain() {
        SelectedNotificationChecker.checkAndShow(this);
        startActivity(new Intent(this, OrganizerActivity.class));
        finish();
    }

    private void launchProfileCreation() {
        SharedPreferences prefs = getSharedPreferences(WelcomeActivity.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(WelcomeActivity.KEY_PROFILE_PENDING, true).apply();

        Intent intent = new Intent(this, ProfileSettingsActivity.class);
        intent.putExtra(ProfileSettingsActivity.EXTRA_FIRST_RUN, true);
        startActivity(intent);
        finish();
    }
}
