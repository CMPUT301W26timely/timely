package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for searching events by keyword.
 */
public class SearchEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewSearchResults;
    private TextView textViewNoResults;
    private TextInputEditText editTextSearch;
    private EventAdapter adapter;
    private List<Event> allActiveEvents = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_events);

        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults);
        textViewNoResults = findViewById(R.id.textViewNoResults);
        editTextSearch = findViewById(R.id.editTextSearch);
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(filteredEvents, event -> {
            Intent intent;
            if (deviceId.equals(event.getOrganizerDeviceId())) {
                intent = new Intent(SearchEventsActivity.this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
            } else {
                intent = new Intent(SearchEventsActivity.this, EntrantEventDetailActivity.class);
                intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
            }
            startActivity(intent);
        });
        recyclerViewSearchResults.setAdapter(adapter);

        setupBottomNavigation();
        loadAllEvents();

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseEventsActivity.class));
            finish();
        });

        findViewById(R.id.navSearch).setOnClickListener(v -> {
            // Already here
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

    private void loadAllEvents() {
        EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                allActiveEvents.clear();
                allActiveEvents.addAll(events);
                filterEvents(editTextSearch.getText().toString());
            }

            @Override
            public void onError(Exception e) {
                Log.e("SearchEventsActivity", "Failed to load events", e);
                Toast.makeText(SearchEventsActivity.this, "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterEvents(String query) {
        filteredEvents.clear();
        if (query.trim().isEmpty()) {
            // Show nothing if empty
        } else {
            // Split by comma and trim each keyword
            String[] keywords = query.split(",");
            List<String> validKeywords = new ArrayList<>();
            for (String kw : keywords) {
                String trimmed = kw.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    validKeywords.add(trimmed);
                }
            }

            if (!validKeywords.isEmpty()) {
                for (Event event : allActiveEvents) {
                    String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
                    String description = event.getDescription() != null ? event.getDescription().toLowerCase() : "";

                    boolean allMatch = true;
                    for (String keyword : validKeywords) {
                        // Check if the current keyword is present in either title or description
                        if (!title.contains(keyword) && !description.contains(keyword)) {
                            allMatch = false;
                            break;
                        }
                    }

                    if (allMatch) {
                        filteredEvents.add(event);
                    }
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        
        if (filteredEvents.isEmpty() && !query.trim().isEmpty()) {
            textViewNoResults.setVisibility(View.VISIBLE);
            recyclerViewSearchResults.setVisibility(View.GONE);
        } else {
            textViewNoResults.setVisibility(View.GONE);
            recyclerViewSearchResults.setVisibility(View.VISIBLE);
        }
    }
}
