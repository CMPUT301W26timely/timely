package com.example.codebase;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.List;

/**
 * MainActivity is the app's main host screen.
 * It contains the bottom navigation and swaps fragments.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Sync the user's role to Firestore when app opens
        UserRepository.syncRole(this);

        // Preload data once so tabs feel faster
        preloadAppData();

        // Get the bottom navigation view from XML
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation =
                findViewById(R.id.bottomNavigation);

        // Load Home tab by default only first time activity is created
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Handle bottom nav tab switching
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (item.getItemId() == R.id.nav_events) {
                loadFragment(new EventsFragment());
                return true;
            } else if (item.getItemId() == R.id.nav_history) {
                loadFragment(new HistoryFragment());
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    /**
     * Replace the current fragment in the container.
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /**
     * Preload user profile and active events into memory cache.
     * This helps Browse Events and Profile open faster.
     */
    private void preloadAppData() {
        // Load user profile into cache if not already present
        if (!AppCache.getInstance().hasCachedUser()) {
            UserRepository.loadUserProfile(this, new UserRepository.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    // User gets cached in repository
                }

                @Override
                public void onError(Exception e) {
                    // Silent fail for now
                }
            });
        }

        // Load active events into cache if not already present
        if (!AppCache.getInstance().hasCachedEvents()) {
            EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {
                @Override
                public void onEventsLoaded(List<Event> events) {
                    // Events get cached in repository
                }

                @Override
                public void onError(Exception e) {
                    // Silent fail for now
                }
            });
        }
    }
}