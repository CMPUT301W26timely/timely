package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ProfileActivity — Shows user profile, device ID, and allows switching roles.
 */
public class ProfileActivity extends AppCompatActivity {

    private boolean isDeviceIdVisible = false;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);
        String currentRole = WelcomeActivity.getSessionRole(this);

        TextView tvRole = findViewById(R.id.tvUserRole);
        TextView tvDeviceId = findViewById(R.id.tvDeviceId);
        ImageButton btnToggleDeviceId = findViewById(R.id.btnToggleDeviceId);
        Button btnSwitch = findViewById(R.id.btnSwitchRole);

        if (WelcomeActivity.ROLE_ADMIN.equals(currentRole)) {
            tvRole.setText(getString(R.string.user_role_label, "Admin"));
            btnSwitch.setText(R.string.switch_to_user);
        } else {
            tvRole.setText(getString(R.string.user_role_label, "User"));
            btnSwitch.setText(R.string.switch_to_admin);
        }

        updateDeviceIdDisplay(tvDeviceId, btnToggleDeviceId);

        btnToggleDeviceId.setOnClickListener(v -> {
            isDeviceIdVisible = !isDeviceIdVisible;
            updateDeviceIdDisplay(tvDeviceId, btnToggleDeviceId);
        });

        btnSwitch.setOnClickListener(v -> {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        setupBottomNavigation();
    }

    private void updateDeviceIdDisplay(TextView tvDeviceId, ImageButton btnToggle) {
        if (isDeviceIdVisible) {
            tvDeviceId.setText(getString(R.string.device_id_label, deviceId));
            btnToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
            tvDeviceId.setText(getString(R.string.device_id_masked));
            btnToggle.setImageResource(android.R.drawable.ic_menu_view);
        }
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            // Already here
        });

        findViewById(R.id.navExplore).setOnClickListener(v -> {
            Toast.makeText(this, "Explore coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.navSearch).setOnClickListener(v -> {
            Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show();
        });
    }
}