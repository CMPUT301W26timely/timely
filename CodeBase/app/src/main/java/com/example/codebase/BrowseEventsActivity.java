package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Entrant browse events screen.
 */
public class BrowseEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewEvents;
    private TextView textViewEmptyState;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_events);

        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        textViewEmptyState = findViewById(R.id.textViewEmptyState);
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));

        setupBottomNavigation();
        loadEvents();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            // already here
        });

        findViewById(R.id.navSearch).setOnClickListener(v -> {
            startActivity(new Intent(this, SearchEventsActivity.class));
            finish();
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

    private void loadEvents() {
        EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                showEvents(events);
            }

            @Override
            public void onError(Exception e) {
                Log.e("BrowseEventsActivity", "Failed to load events", e);
                Toast.makeText(BrowseEventsActivity.this,
                        "Failed to load events: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();

                textViewEmptyState.setVisibility(View.VISIBLE);
                recyclerViewEvents.setVisibility(View.GONE);
            }
        });
    }

    private void showEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            textViewEmptyState.setVisibility(View.VISIBLE);
            recyclerViewEvents.setVisibility(View.GONE);
            return;
        }

        textViewEmptyState.setVisibility(View.GONE);
        recyclerViewEvents.setVisibility(View.VISIBLE);

        EventAdapter adapter = new EventAdapter(events, event -> {
            Intent intent;
            if (deviceId.equals(event.getOrganizerDeviceId())) {
                intent = new Intent(BrowseEventsActivity.this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
            } else {
                intent = new Intent(BrowseEventsActivity.this, EntrantEventDetailActivity.class);
                intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
            }
            startActivity(intent);
        });

        recyclerViewEvents.setAdapter(adapter);
    }
}
