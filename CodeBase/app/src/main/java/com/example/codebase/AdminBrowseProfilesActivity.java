package com.example.codebase;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Administrator profile browser for US 03.05.01.
 *
 * <p>This screen lists every saved user profile in the system so an administrator can
 * review contact information and profile completeness without impersonating that user.</p>
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
        adapter = new AdminProfileAdapter(profileList);
        recyclerViewProfiles.setAdapter(adapter);

        buttonBack.setOnClickListener(v -> finish());
        loadProfiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfiles();
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
