package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Administrator profile browser for US 03.05.01.
 *
 * <p>This screen lists every saved user profile in the system so an administrator can
 * review contact information and profile completeness without impersonating that user.
 * It also keeps the shared bottom navigation visible so admins can move between
 * profile browsing and the rest of the app without falling out of the flow.</p>
 */
public class AdminBrowseProfilesActivity extends AppCompatActivity {

    /** Displays the system-wide profile list. */
    private RecyclerView recyclerViewProfiles;

    /** Empty-state label shown when no saved profiles exist. */
    private TextView textViewEmptyState;

    /** Subtitle refreshed with the current profile count. */
    private TextView textViewSubtitle;

    /** Returns to the administrator event browser. */
    private ImageButton buttonBack;

    /** Adapter bound to {@link #profileList}. */
    private AdminProfileAdapter adapter;

    /** Mutable backing list for the administrator profile browser. */
    private final List<User> profileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!WelcomeActivity.ROLE_ADMIN.equals(WelcomeActivity.getSessionRole(this))) {
            Toast.makeText(this, R.string.admin_profiles_no_access, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_browse_profiles);

        recyclerViewProfiles = findViewById(R.id.recyclerViewAdminProfiles);
        textViewEmptyState = findViewById(R.id.textViewAdminProfilesEmptyState);
        textViewSubtitle = findViewById(R.id.textViewAdminProfilesSubtitle);
        buttonBack = findViewById(R.id.buttonAdminProfilesBack);

        recyclerViewProfiles.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize adapter with delete functionality for administrator
        adapter = new AdminProfileAdapter(profileList, new AdminProfileAdapter.OnProfileActionListener() {
            @Override
            public void onProfileDelete(User user) {
                showDeleteProfileConfirmation(user);
            }
        });
        recyclerViewProfiles.setAdapter(adapter);

        // Route both explicit back taps and system back gestures to the admin home screen.
        buttonBack.setOnClickListener(v -> navigateToAdminHome());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToAdminHome();
            }
        });

        setupBottomNavigation();
        loadProfiles();
    }

    /**
     * Shows a confirmation dialog before deleting a user profile.
     *
     * @param user The profile to be deleted.
     */
    private void showDeleteProfileConfirmation(User user) {
        String displayName = user.getName() != null ? user.getName() : "this user";
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to permanently delete the profile for \"" + displayName + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfileFromDatabase(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Removes the specified user profile from Firestore.
     *
     * @param user The profile document to delete.
     */
    private void deleteProfileFromDatabase(User user) {
        AppDatabase.getInstance().usersRef.document(user.getDeviceId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                    loadProfiles(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete profile", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfiles();
    }

    /**
     * Wires the shared bottom navigation for the administrator profile screen.
     */
    private void setupBottomNavigation() {
        findViewById(R.id.navImage).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminBrowseImagesActivity.class));
            finish();
        });

        findViewById(R.id.navHistory).setOnClickListener(v -> {
            // Already on the admin profile browser.
        });

        findViewById(R.id.navMyEvents).setOnClickListener(v -> navigateToAdminHome());

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    /**
     * Returns to the shared administrator event browser.
     *
     * <p>This screen is often opened from a navigation action that already finished the
     * previous activity, so we start the admin home explicitly instead of relying on
     * {@link #finish()} to reveal an existing screen.</p>
     */
    private void navigateToAdminHome() {
        Intent intent = new Intent(this, OrganizerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /** Loads every saved profile so the administrator can browse the full directory. */
    private void loadProfiles() {
        UserRepository.loadAllProfiles(new UserRepository.ProfilesCallback() {
            @Override
            public void onProfilesLoaded(List<User> users) {
                showProfiles(AdminBrowseHelper.sortProfilesForAdmin(users));
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AdminBrowseProfilesActivity.this,
                        R.string.admin_profiles_load_failed,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Replaces the visible profile list and toggles the empty state.
     *
     * @param users profiles ready for display
     */
    private void showProfiles(List<User> users) {
        profileList.clear();
        if (users != null) {
            profileList.addAll(users);
        }

        adapter.notifyDataSetChanged();
        textViewSubtitle.setText(getString(R.string.admin_profiles_subtitle_count, profileList.size()));

        boolean hasProfiles = !profileList.isEmpty();
        recyclerViewProfiles.setVisibility(hasProfiles ? View.VISIBLE : View.GONE);
        textViewEmptyState.setVisibility(hasProfiles ? View.GONE : View.VISIBLE);
    }
}