package com.example.codebase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * WelcomeActivity — Role selection screen shown on every launch.
 *
 * The user picks their role for this session:
 *   • User      → proceeds to OrganizerActivity (Main Event Screen)
 *   • Admin     → PIN dialog first, then proceeds to OrganizerActivity
 */
public class WelcomeActivity extends AppCompatActivity {

    public static final String PREFS_NAME     = "event_lottery_prefs";
    public static final String KEY_ROLE       = "user_role";
    public static final String KEY_PROFILE_PENDING = "profile_setup_pending";
    public static final String ROLE_USER      = "ENTRANT";
    public static final String ROLE_ADMIN     = "ADMIN";

    private static final String ADMIN_PIN = "1234";

    private boolean isRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isRegistered = prefs.getBoolean(RoleCheckActivity.KEY_REGISTERED, false);

        CardView btnUser  = findViewById(R.id.btnEntrant);
        CardView btnAdmin = findViewById(R.id.btnAdmin);

        animateCard(btnUser,  200);
        animateCard(btnAdmin, 400);

        btnUser.setOnClickListener(v  -> saveRoleAndProceed(ROLE_USER));
        btnAdmin.setOnClickListener(v -> showAdminPinDialog());
    }

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

    private void showAdminPinDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_pin, null);
        EditText pinInput = dialogView.findViewById(R.id.etPin);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnConfirm.setOnClickListener(v -> {
            String entered = pinInput.getText().toString().trim();
            if (entered.equals(ADMIN_PIN)) {
                dialog.dismiss();
                saveRoleAndProceed(ROLE_ADMIN);
            } else {
                Toast.makeText(this, "Incorrect PIN. Access denied.", Toast.LENGTH_SHORT).show();
                pinInput.setText("");
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveRoleAndProceed(String role) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_ROLE, role).apply();

        if (!isRegistered) {
            prefs.edit().putBoolean(RoleCheckActivity.KEY_REGISTERED, true).apply();
            startActivity(new Intent(this, SplashActivity.class));
        } else if (prefs.getBoolean(KEY_PROFILE_PENDING, false)) {
            Intent intent = new Intent(this, ProfileSettingsActivity.class);
            intent.putExtra(ProfileSettingsActivity.EXTRA_FIRST_RUN, true);
            startActivity(intent);
        } else {
            // Routing to OrganizerActivity as the primary screen for both roles for now
            startActivity(new Intent(this, OrganizerActivity.class));
        }

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    public static String getSessionRole(android.content.Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_ROLE, ROLE_USER);
    }
}
