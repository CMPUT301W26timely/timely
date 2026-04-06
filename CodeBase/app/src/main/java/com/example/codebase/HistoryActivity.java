package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity host for the entrant registration-history screen.
 *
 * <p>This mirrors the existing entrant activity navigation pattern and embeds
 * {@link HistoryFragment} as the content area.</p>
 */
public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Administrators share this nav slot with the profile browser instead of entrant history.
        if (RoleAwareNavHelper.isAdminSession(this)) {
            RoleAwareNavHelper.openSecondaryNav(this, true);
            return;
        }

        setContentView(R.layout.activity_history);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.historyFragmentContainer, new HistoryFragment())
                    .commit();
        }

        setupBottomNavigation();
    }

    /**
     * Wires the entrant bottom navigation for the history screen.
     */
    private void setupBottomNavigation() {
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseEventsActivity.class));
            finish();
        });

        findViewById(R.id.navHistory).setOnClickListener(v -> {
            // Already on the history screen.
        });

        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerActivity.class));
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }
}
