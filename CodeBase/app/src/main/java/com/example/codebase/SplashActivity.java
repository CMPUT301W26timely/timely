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
 *
 * <p>Handles device identification and Firestore registration/sync before
 * navigating to the appropriate next screen. The flow is as follows:
 * <ol>
 *   <li>A device ID is retrieved or created via {@link DeviceIdManager#getOrCreateDeviceId(android.content.Context)}
 *       and a shortened version is displayed on screen.</li>
 *   <li>The corresponding Firestore user document is checked for existence.</li>
 *   <li>If the document does <em>not</em> exist, a new {@link User} is registered and
 *       the user is directed to {@link ProfileSettingsActivity} for first-run profile
 *       creation via {@link #launchProfileCreation()}.</li>
 *   <li>If the document already exists (or a Firestore error occurs), the app proceeds
 *       directly to {@link OrganizerActivity} via {@link #navigateToMain()}.</li>
 * </ol>
 *
 * <p>This activity always calls {@link #finish()} after navigation so it is removed
 * from the back stack.
 *
 * @see DeviceIdManager
 * @see AppDatabase
 * @see SelectedNotificationChecker
 */
public class SplashActivity extends AppCompatActivity {

    /**
     * Initialises the splash screen, resolves the device ID, and performs the
     * Firestore new-user check.
     *
     * <p>Steps performed:
     * <ol>
     *   <li>Inflates {@code activity_splash} and displays a shortened device ID in
     *       {@code deviceIdText}.</li>
     *   <li>Obtains a {@link DocumentReference} to the user document keyed by
     *       {@code deviceId} from {@link AppDatabase#usersRef}.</li>
     *   <li>Fetches the document asynchronously:
     *     <ul>
     *       <li>Document absent → calls {@link #launchProfileCreation()} on write
     *           success, or {@link #navigateToMain()} on write failure.</li>
     *       <li>Document present → calls {@link #navigateToMain()}.</li>
     *       <li>Fetch failure → logs the error and calls {@link #navigateToMain()}.</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
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

    /**
     * Runs any pending selected-entrant notification checks, then starts
     * {@link OrganizerActivity} and finishes this activity.
     *
     * <p>Called when a returning user's Firestore document already exists, or
     * as a fallback when Firestore operations fail.
     */
    private void navigateToMain() {
        SelectedNotificationChecker.checkAndShow(this);
        startActivity(new Intent(this, OrganizerActivity.class));
        finish();
    }

    /**
     * Marks a first-run profile setup as pending in {@link SharedPreferences} and
     * launches {@link ProfileSettingsActivity} in first-run mode, then finishes
     * this activity.
     *
     * <p>Sets {@link WelcomeActivity#KEY_PROFILE_PENDING} to {@code true} in the
     * {@link WelcomeActivity#PREFS_NAME} preferences file so that other parts of the
     * app can detect an incomplete profile. Passes
     * {@link ProfileSettingsActivity#EXTRA_FIRST_RUN} as {@code true} in the intent
     * to allow {@link ProfileSettingsActivity} to tailor its UI for new users.
     *
     * <p>Called only after a new user document has been successfully written to
     * Firestore.
     */
    private void launchProfileCreation() {
        SharedPreferences prefs = getSharedPreferences(WelcomeActivity.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(WelcomeActivity.KEY_PROFILE_PENDING, true).apply();

        Intent intent = new Intent(this, ProfileSettingsActivity.class);
        intent.putExtra(ProfileSettingsActivity.EXTRA_FIRST_RUN, true);
        startActivity(intent);
        finish();
    }
}