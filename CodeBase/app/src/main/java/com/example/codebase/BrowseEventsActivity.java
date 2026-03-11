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

public class BrowseEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewEvents;
    private TextView textViewEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_events);

        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        textViewEmptyState = findViewById(R.id.textViewEmptyState);

        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));

        // Show cached data immediately if available
        if (AppCache.getInstance().hasCachedEvents()) {
            showEvents(AppCache.getInstance().getCachedEvents());
            refreshEventsInBackground();
        } else {
            loadEventsFromFirestore();
        }
    }

    private void loadEventsFromFirestore() {
        EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                Log.d("BrowseEvents", "Loaded events count = " + events.size());
                showEvents(events);
            }

            @Override
            public void onError(Exception e) {
                Log.e("BrowseEvents", "Failed to load events", e);
                Toast.makeText(BrowseEventsActivity.this,
                        "Failed to load events: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void refreshEventsInBackground() {
        EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                showEvents(events);
            }

            @Override
            public void onError(Exception e) {
                Log.e("BrowseEvents", "Background refresh failed", e);
            }
        });
    }

    private void showEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            textViewEmptyState.setVisibility(View.VISIBLE);
            recyclerViewEvents.setVisibility(View.GONE);
        } else {
            textViewEmptyState.setVisibility(View.GONE);
            recyclerViewEvents.setVisibility(View.VISIBLE);

            EventAdapter adapter = new EventAdapter(events, event -> {
                Intent intent = new Intent(BrowseEventsActivity.this, EventDetailsActivity.class);
                intent.putExtra("eventId", event.getEventId());
                startActivity(intent);
            });

            recyclerViewEvents.setAdapter(adapter);
        }
    }
}