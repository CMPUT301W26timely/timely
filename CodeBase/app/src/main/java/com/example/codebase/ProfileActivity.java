package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ProfileActivity — shows user profile summary and navigation.
 * This supports viewing updated profile data after save.
 */
public class ProfileActivity extends AppCompatActivity {

    private boolean isDeviceIdVisible = false;
    private String deviceId;

    private TextView tvRole;
    private TextView tvDeviceId;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvPhone;
    private ImageButton btnToggleDeviceId;
    private Button btnSwitch;
    private Button btnEditProfile;

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
        btnToggleDeviceId = findViewById(R.id.btnToggleDeviceId);
        btnSwitch = findViewById(R.id.btnSwitchRole);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        if (WelcomeActivity.ROLE_ADMIN.equals(currentRole)) {
            tvRole.setText(getString(R.string.user_role_label, "Admin"));
            btnSwitch.setText(R.string.switch_to_user);
        } else {
            tvRole.setText(getString(R.string.user_role_label, "User"));
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

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void updateDeviceIdDisplay() {
        if (isDeviceIdVisible) {
            tvDeviceId.setText(getString(R.string.device_id_label, deviceId));
            btnToggleDeviceId.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            tvDeviceId.setText(getString(R.string.device_id_masked));
            btnToggleDeviceId.setImageResource(android.R.drawable.ic_menu_view);
        }
    }

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
                        "Failed to load profile",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUser(User user) {
        tvName.setText(TextUtils.isEmpty(user.getName()) ? "No name set" : user.getName());
        tvEmail.setText(TextUtils.isEmpty(user.getEmail()) ? "No email set" : user.getEmail());
        tvPhone.setText(TextUtils.isEmpty(user.getPhoneNumber()) ? "No phone number set" : user.getPhoneNumber());
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            // already here
        });

        findViewById(R.id.navExplore).setOnClickListener(v ->
                startActivity(new Intent(this, BrowseEventsActivity.class)));

        findViewById(R.id.navSearch).setOnClickListener(v ->
                startActivity(new Intent(this, BrowseEventsActivity.class)));

        findViewById(R.id.navNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
    }
}