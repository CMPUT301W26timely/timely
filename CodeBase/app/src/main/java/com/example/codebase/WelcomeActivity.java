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
 * Role selection screen displayed on every application launch.
 *
 * <p>Presents two role options to the user:
 * <ul>
 *   <li><b>User (Entrant)</b> — proceeds directly to {@link OrganizerActivity}.</li>
 *   <li><b>Admin</b> — must first pass a PIN challenge before proceeding to
 *       {@link OrganizerActivity}.</li>
 * </ul>
 *
 * <p>On first launch (i.e. the device has not been registered), the user is redirected to
 * {@link SplashActivity} for initial setup. If a profile setup is still pending, the user is
 * sent to {@link ProfileSettingsActivity} before reaching the main screen.
 *
 * <p>The selected role is persisted in {@link SharedPreferences} under {@link #PREFS_NAME}
 * using the key {@link #KEY_ROLE}, and can be retrieved from any context via
 * {@link #getSessionRole(android.content.Context)}.
 */
public class WelcomeActivity extends AppCompatActivity {

    /**
     * Name of the {@link SharedPreferences} file used to store app-wide session state,
     * including the current user role and registration status.
     */
    public static final String PREFS_NAME = "event_lottery_prefs";

    /**
     * {@link SharedPreferences} key for storing the currently selected role.
     * Value will be one of {@link #ROLE_USER} or {@link #ROLE_ADMIN}.
     */
    public static final String KEY_ROLE = "user_role";

    /**
     * {@link SharedPreferences} key indicating whether the user still needs to complete
     * profile setup. When {@code true}, the app routes to {@link ProfileSettingsActivity}
     * before proceeding to the main screen.
     */
    public static final String KEY_PROFILE_PENDING = "profile_setup_pending";

    /**
     * Role constant for a standard entrant / end-user.
     * Stored as the value of {@link #KEY_ROLE} in {@link SharedPreferences}.
     */
    public static final String ROLE_USER = "ENTRANT";

    /**
     * Role constant for an administrator.
     * Stored as the value of {@link #KEY_ROLE} in {@link SharedPreferences}.
     */
    public static final String ROLE_ADMIN = "ADMIN";

    /**
     * Hard-coded PIN required to gain administrator access.
     *
     * <p><b>Note:</b> In production, this should be stored securely (e.g. hashed and
     * held server-side) rather than as a plain-text constant.
     */
    private static final String ADMIN_PIN = "1234";

    /**
     * Whether the current device has already completed first-run registration.
     * Loaded from {@link SharedPreferences} in {@link #onCreate(Bundle)}.
     */
    private boolean isRegistered;

    /**
     * Initialises the activity, reads the registration state from {@link SharedPreferences},
     * inflates the role-selection UI, and attaches click listeners to the role cards.
     *
     * @param savedInstanceState If the activity is being re-created from a previous state,
     *                           this bundle contains the most recent data; otherwise {@code null}.
     */
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

    /**
     * Applies a fade-in and upward-slide entrance animation to the given {@link CardView}.
     *
     * <p>The card starts fully transparent and 40 pixels below its resting position,
     * then animates to fully opaque at its natural position over 400 ms.
     *
     * @param card  The {@link CardView} to animate.
     * @param delay Delay in milliseconds before the animation begins, used to stagger
     *              multiple cards for a sequential entrance effect.
     */
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

    /**
     * Displays a modal PIN entry dialog for administrator authentication.
     *
     * <p>The dialog is inflated from {@code R.layout.dialog_admin_pin} and contains:
     * <ul>
     *   <li>An {@link EditText} for PIN input.</li>
     *   <li>A <i>Confirm</i> button that validates the PIN against {@link #ADMIN_PIN}.</li>
     *   <li>A <i>Cancel</i> button that dismisses the dialog without any role change.</li>
     * </ul>
     *
     * <p>On a successful PIN match the dialog is dismissed and
     * {@link #saveRoleAndProceed(String)} is called with {@link #ROLE_ADMIN}.
     * On failure, a {@link Toast} is shown and the PIN field is cleared.
     */
    private void showAdminPinDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_pin, null);
        EditText pinInput = dialogView.findViewById(R.id.etPin);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel  = dialogView.findViewById(R.id.btnCancel);

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

    /**
     * Persists the selected role and routes the user to the appropriate next screen.
     *
     * <p>Routing logic (evaluated in order):
     * <ol>
     *   <li>If the device has <b>not</b> yet been registered, mark it as registered and
     *       start {@link SplashActivity} for first-run onboarding.</li>
     *   <li>If profile setup is still pending ({@link #KEY_PROFILE_PENDING} is {@code true}),
     *       start {@link ProfileSettingsActivity} with
     *       {@link ProfileSettingsActivity#EXTRA_FIRST_RUN} set to {@code true}.</li>
     *   <li>Otherwise, start {@link OrganizerActivity} as the main application screen
     *       (used for both the {@link #ROLE_USER} and {@link #ROLE_ADMIN} roles).</li>
     * </ol>
     *
     * <p>A fade transition is applied and this activity is finished so it cannot be
     * returned to via the back stack.
     *
     * @param role The role to persist; must be {@link #ROLE_USER} or {@link #ROLE_ADMIN}.
     */
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

    /**
     * Returns the role that was saved for the current session.
     *
     * <p>This is a convenience helper allowing any component to retrieve the active
     * role without holding a direct reference to {@link WelcomeActivity}.
     *
     * @param ctx Any {@link android.content.Context}; used only to access
     *            {@link SharedPreferences}.
     * @return The stored role string ({@link #ROLE_USER} or {@link #ROLE_ADMIN}).
     *         Defaults to {@link #ROLE_USER} if no role has been saved yet.
     */
    public static String getSessionRole(android.content.Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_ROLE, ROLE_USER);
    }
}