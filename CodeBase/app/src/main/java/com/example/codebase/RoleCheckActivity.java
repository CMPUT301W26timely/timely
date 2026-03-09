package com.example.codebase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * RoleCheckActivity — Invisible entry point. Registered as MAIN/LAUNCHER.
 *
 * Runs on every launch. Always shows WelcomeActivity for role selection.
 * Passes a flag (is_registered) to WelcomeActivity so it knows whether
 * to route through SplashActivity (first launch) or directly to
 * MainActivity (subsequent launches).
 *
 * Flow:
 *   1st launch:  RoleCheckActivity → WelcomeActivity → SplashActivity → MainActivity
 *   2nd+ launch: RoleCheckActivity → WelcomeActivity → MainActivity
 */
public class RoleCheckActivity extends AppCompatActivity {

    public static final String PREFS_NAME     = "event_lottery_prefs";
    public static final String KEY_REGISTERED = "is_registered";

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