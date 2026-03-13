package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Displays a summary of the current user's profile and provides navigation to
 * profile editing and role switching.
 *
 * <p>Profile data is loaded in two passes:
 * <ol>
 *   <li>Synchronously from {@link AppCache} if a cached {@link User} is available.</li>
 *   <li>Asynchronously from {@link UserRepository} (Firestore), which overwrites the
 *       cached values once loaded.</li>
 * </ol>
 *
 * <p>The screen also exposes:
 * <ul>
 *   <li>A toggleable device ID display (masked by default).</li>
 *   <li>A role-switch button that returns the user to {@link WelcomeActivity} for
 *       re-selection.</li>
 *   <li>An edit button that launches {@link ProfileSettingsActivity}.</li>
 *   <li>A bottom navigation bar shared across the main sections of the app.</li>
 * </ul>
 *
 * <p>Profile data is refreshed on every {@link #onResume()} to reflect edits made in
 * {@link ProfileSettingsActivity}.
 */
public class ProfileActivity extends AppCompatActivity {

    /**
     * Whether the full device ID is currently visible in {@link #tvDeviceId}.
     * Toggled by {@link #btnToggleDeviceId}; defaults to {@code false} (masked).
     */
    private boolean isDeviceIdVisible = false;

    /**
     * The unique device identifier obtained from {@link DeviceIdManager}, used for
     * display purposes in {@link #tvDeviceId}.
     */
    private String deviceId;

    /** Displays the current session role (e.g. "Admin" or "User"). */
    private TextView tvRole;

    /** Displays the device ID, either masked or in full depending on {@link #isDeviceIdVisible}. */
    private TextView tvDeviceId;

    /** Displays the user's name, or a placeholder if not set. */
    private TextView tvName;

    /** Displays the user's email address, or a placeholder if not set. */
    private TextView tvEmail;

    /** Displays the user's phone number, or a placeholder if not set. */
    private TextView tvPhone;
    private TextView tvProfileStatus;
    private TextView tvProfileSummary;
    private TextView tvAvatarInitials;
    private ImageButton btnToggleDeviceId;

    /**
     * Navigates back to {@link WelcomeActivity} to allow the user to switch roles.
     * Label reflects the opposite of the current role.
     */
    private Button btnSwitch;

    /** Launches {@link ProfileSettingsActivity} in edit mode. */
    private Button btnEditProfile;

    /**
     * Initialises the activity, resolves the current session role, binds all views,
     * configures button listeners, sets up bottom navigation, and triggers the initial
     * profile load.
     *
     * @param savedInstanceState If the activity is being re-created from a previous state,
     *                           this bundle contains the most recent data; otherwise {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);
        String currentRole = WelcomeActivity.getSessionRole(this);

        tvRole = findViewById(R.id.tvUserRole);
        tvDeviceId = findViewById(R.id.tvDeviceId);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvProfileStatus = findViewById(R.id.tvProfileStatus);
        tvProfileSummary = findViewById(R.id.tvProfileSummary);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        btnToggleDeviceId = findViewById(R.id.btnToggleDeviceId);
        btnSwitch        = findViewById(R.id.btnSwitchRole);
        btnEditProfile   = findViewById(R.id.btnEditProfile);

        if (WelcomeActivity.ROLE_ADMIN.equals(currentRole)) {
            tvRole.setText(getString(R.string.user_role_label, getString(R.string.role_admin)));
            btnSwitch.setText(R.string.switch_to_user);
        } else {
            tvRole.setText(getString(R.string.user_role_label, getString(R.string.role_entrant)));
            btnSwitch.setText(R.string.switch_to_admin);
        }

        updateDeviceIdDisplay();

        btnToggleDeviceId.setOnClickListener(v -> {
            isDeviceIdVisible = !isDeviceIdVisible;
            updateDeviceIdDisplay();
        });

        btnSwitch.setOnClickListener(v -> {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileSettingsActivity.class)));

        setupBottomNavigation();
        loadProfile();
    }

    /**
     * Reloads the user's profile each time the activity returns to the foreground,
     * ensuring edits made in {@link ProfileSettingsActivity} are reflected immediately.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    /**
     * Updates {@link #tvDeviceId} and the toggle button icon to reflect the current
     * value of {@link #isDeviceIdVisible}.
     *
     * <p>When visible, the full device ID is shown and the icon changes to a close/clear
     * symbol. When masked, a placeholder string is shown and the icon reverts to an eye symbol.
     */
    private void updateDeviceIdDisplay() {
        if (isDeviceIdVisible) {
            tvDeviceId.setText(getString(R.string.device_id_label, deviceId));
            btnToggleDeviceId.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            tvDeviceId.setText(getString(R.string.device_id_masked));
            btnToggleDeviceId.setImageResource(android.R.drawable.ic_menu_view);
        }
    }

    /**
     * Loads the user's profile data from {@link AppCache} (synchronous) and then from
     * {@link UserRepository} (asynchronous Firestore fetch).
     *
     * <p>If a cached {@link User} exists it is displayed immediately via
     * {@link #showUser(User)}. The Firestore result overwrites the cached display once
     * available. Shows a {@link Toast} error if the Firestore load fails.
     */
    private void loadProfile() {
        if (AppCache.getInstance().hasCachedUser()) {
            showUser(AppCache.getInstance().getCachedUser());
        }

        UserRepository.loadUserProfile(this, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                showUser(user);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ProfileActivity.this,
                        R.string.profile_load_failed,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Populates the name, email, and phone {@link TextView}s from the given {@link User}.
     *
     * <p>If any field is empty or {@code null}, a human-readable placeholder is shown
     * (e.g. "No name set") rather than a blank field.
     *
     * @param user The {@link User} whose data should be displayed.
     */
    private void showUser(User user) {
        boolean hasRequiredProfileData =
                !TextUtils.isEmpty(user.getName()) && !TextUtils.isEmpty(user.getEmail());

        tvName.setText(
                TextUtils.isEmpty(user.getName())
                        ? getString(R.string.profile_placeholder_name)
                        : user.getName()
        );
        tvEmail.setText(
                TextUtils.isEmpty(user.getEmail())
                        ? getString(R.string.profile_placeholder_email)
                        : user.getEmail()
        );
        tvPhone.setText(
                TextUtils.isEmpty(user.getPhoneNumber())
                        ? getString(R.string.profile_placeholder_phone)
                        : user.getPhoneNumber()
        );
        tvAvatarInitials.setText(resolveInitials(user.getName()));
        tvProfileStatus.setText(
                hasRequiredProfileData
                        ? R.string.profile_status_complete
                        : R.string.profile_status_incomplete
        );
        tvProfileStatus.setBackgroundResource(
                hasRequiredProfileData ? R.drawable.bg_pill_green : R.drawable.bg_pill_amber
        );
        tvProfileStatus.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        tvProfileSummary.setText(
                hasRequiredProfileData
                        ? R.string.profile_status_complete_message
                        : R.string.profile_status_incomplete_message
        );
    }

    private String resolveInitials(String name) {
        if (TextUtils.isEmpty(name)) {
            return "U";
        }

        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.length() == 0 ? "U" : initials.toString();
    }

    /**
     * Wires click listeners for the bottom navigation bar items.
     *
     * <ul>
     *   <li><b>My Events</b> — starts {@link OrganizerActivity} and finishes this activity.</li>
     *   <li><b>Profile</b> — no-op; the user is already on this screen.</li>
     *   <li><b>Explore</b> — starts {@link BrowseEventsActivity}.</li>
     *   <li><b>Search</b> — shows a "Not implemented yet" {@link Toast}.</li>
     *   <li><b>Notifications</b> — starts {@link NotificationsActivity}.</li>
     * </ul>
     */
    private void setupBottomNavigation() {
        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            // Already on this screen — no action required.
        });

        findViewById(R.id.navExplore).setOnClickListener(v ->
                startActivity(new Intent(this, BrowseEventsActivity.class)));

        findViewById(R.id.navSearch).setOnClickListener(v ->
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
    }
}