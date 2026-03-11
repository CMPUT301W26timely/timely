package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.codebase.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserRepository.syncRole(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        Button buttonBrowseEvents = findViewById(R.id.buttonBrowseEvents);
        Button buttonOpenProfile = findViewById(R.id.buttonOpenProfile);

        preloadAppData();

        buttonBrowseEvents.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, BrowseEventsActivity.class)));

        buttonOpenProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        binding.fab.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        // Leave commented until notifications are implemented
        // SelectedNotificationChecker.checkAndShow(this);
    }

    private void preloadAppData() {
        // Preload profile
        if (!AppCache.getInstance().hasCachedUser()) {
            UserRepository.loadUserProfile(this, new UserRepository.UserCallback() {
                @Override
                public void onUserLoaded(User user) {
                    // cached automatically in repository
                }

                @Override
                public void onError(Exception e) {
                    // Optional: keep silent or show a toast for debugging
                }
            });
        }

        // Preload events
        if (!AppCache.getInstance().hasCachedEvents()) {
            EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {
                @Override
                public void onEventsLoaded(java.util.List<Event> events) {
                    // cached automatically in repository
                }

                @Override
                public void onError(Exception e) {
                    // Optional: keep silent or show a toast for debugging
                    Toast.makeText(MainActivity.this,
                            "Could not preload events",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}