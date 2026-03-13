package com.example.codebase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Invisible entry point activity, registered as {@code MAIN/LAUNCHER}.
 *
 * <p>Executes on every application launch without rendering any UI
 * (themed with {@code Theme.CodeBase.NoDisplay}). Reads the device's
 * registration state from {@link SharedPreferences} and forwards it to
 * {@link WelcomeActivity} via an {@link Intent} extra, allowing
 * {@link WelcomeActivity} to determine the correct post-role-selection route.
 *
 * <p>Launch flows:
 * <ul>
 *   <li><b>First launch:</b> {@code RoleCheckActivity} → {@link WelcomeActivity}
 *       → {@link SplashActivity} → {@link MainActivity}</li>
 *   <li><b>Subsequent launches:</b> {@code RoleCheckActivity} → {@link WelcomeActivity}
 *       → {@link MainActivity}</li>
 * </ul>
 */
public class RoleCheckActivity extends AppCompatActivity {

    /**
     * Name of the {@link SharedPreferences} file shared across the application
     * for storing session and registration state.
     */
    public static final String PREFS_NAME = "event_lottery_prefs";

    /**
     * {@link SharedPreferences} key whose boolean value indicates whether the device
     * has completed first-run registration.
     * <ul>
     *   <li>{@code false} (default) — first launch; onboarding flow will be triggered.</li>
     *   <li>{@code true} — returning user; routed directly to the main screen.</li>
     * </ul>
     */
    public static final String KEY_REGISTERED = "is_registered";

    /**
     * Reads registration state and immediately delegates to {@link WelcomeActivity}.
     *
     * <p>No content view is set; this activity is intentionally invisible. It finishes
     * itself after starting {@link WelcomeActivity} so it is removed from the back stack.
     *
     * @param savedInstanceState Unused. No UI state is retained by this activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No setContentView — invisible activity (Theme.CodeBase.NoDisplay)

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isRegistered = prefs.getBoolean(KEY_REGISTERED, false);

        // Always go to WelcomeActivity — role is picked every session
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra(KEY_REGISTERED, isRegistered);
        startActivity(intent);
        finish();
    }
}