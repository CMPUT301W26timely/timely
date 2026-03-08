package com.example.codebase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * WelcomeActivity — Role selection screen shown on every launch.
 *
 * The user picks their role for this session:
 *   • Entrant   → proceeds directly
 *   • Organizer → proceeds directly
 *   • Admin     → PIN dialog first
 *
 * After role is picked:
 *   1. Role saved to SharedPreferences
 *   2. Routing:
 *      1st launch  → SplashActivity (registers device in Firestore) → MainActivity
 *      2nd+ launch → MainActivity directly
 *
 * Role is synced to Firestore inside MainActivity via UserRepository.syncRole()
 * This ensures the correct role is always stored regardless of launch number.
 */
public class WelcomeActivity extends AppCompatActivity {

    public static final String PREFS_NAME     = "event_lottery_prefs";
    public static final String KEY_ROLE       = "user_role";
    public static final String ROLE_ENTRANT   = "ENTRANT";
    public static final String ROLE_ORGANIZER = "ORGANIZER";
    public static final String ROLE_ADMIN     = "ADMIN";

    // Hardcoded PIN for now — replace with Firestore check in a later part
    private static final String ADMIN_PIN = "1234";

    private boolean isRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Received from RoleCheckActivity
        isRegistered = getIntent().getBooleanExtra(RoleCheckActivity.KEY_REGISTERED, false);

        CardView btnEntrant   = findViewById(R.id.btnEntrant);
        CardView btnOrganizer = findViewById(R.id.btnOrganizer);
        CardView btnAdmin     = findViewById(R.id.btnAdmin);

        // Staggered entrance animations
        animateCard(btnEntrant,   200);
        animateCard(btnOrganizer, 350);
        animateCard(btnAdmin,     500);

        btnEntrant.setOnClickListener(v   -> saveRoleAndProceed(ROLE_ENTRANT));
        btnOrganizer.setOnClickListener(v -> saveRoleAndProceed(ROLE_ORGANIZER));
        btnAdmin.setOnClickListener(v     -> showAdminPinDialog());
    }

    // ─── Animations ──────────────────────────────────────────────────────────

    private void animateCard(CardView card, long delay) {
        card.setAlpha(0f);
        card.setTranslationY(40f);
        card.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(400)
                .start();
    }

    // ─── Admin PIN Dialog ─────────────────────────────────────────────────────

    private void showAdminPinDialog() {
        EditText pinInput = new EditText(this);
        pinInput.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );
        pinInput.setHint("Enter Admin PIN");

        new AlertDialog.Builder(this)
                .setTitle("Admin Access")
                .setMessage("Enter the administrator PIN to continue.")
                .setView(pinInput)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String entered = pinInput.getText().toString().trim();
                    if (entered.equals(ADMIN_PIN)) {
                        saveRoleAndProceed(ROLE_ADMIN);
                    } else {
                        Toast.makeText(this,
                                "Incorrect PIN. Access denied.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    /**
     * Saves role to SharedPreferences and routes to correct next screen.
     * Firestore sync happens in MainActivity via UserRepository.syncRole().
     */
    private void saveRoleAndProceed(String role) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_ROLE, role).apply();

        if (!isRegistered) {
            // First launch — run SplashActivity to register device in Firestore
            prefs.edit().putBoolean(RoleCheckActivity.KEY_REGISTERED, true).apply();
            startActivity(new Intent(this, SplashActivity.class));
        } else {
            // Already registered — go straight to MainActivity
            startActivity(new Intent(this, MainActivity.class));
        }

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ─── Public Utility ──────────────────────────────────────────────────────

    /**
     * Read the current session role from anywhere in the app.
     *
     * Usage:
     *   String role = WelcomeActivity.getSessionRole(context);
     *   if (role.equals(WelcomeActivity.ROLE_ORGANIZER)) { ... }
     */
    public static String getSessionRole(android.content.Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_ROLE, ROLE_ENTRANT);
    }
}