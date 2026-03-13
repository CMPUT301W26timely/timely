package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * OrganizerActivity — "My Events" screen for Organizer role.
 */
public class OrganizerActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private TextView tvNoEvents;
    private FloatingActionButton fabCreate;
    private OrganizerEventAdapter adapter;
    private final List<Event> eventList = new ArrayList<>();
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        // Back button → WelcomeActivity

        rvEvents = findViewById(R.id.rvEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        fabCreate = findViewById(R.id.fabCreateEvent);

        adapter = new OrganizerEventAdapter(eventList, event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
            startActivity(intent);
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        fabCreate.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class))
        );

        setupBottomNavigation();
        loadOrganizerEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            // already here
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseEventsActivity.class));
            finish();
        });

        findViewById(R.id.navSearch).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseEventsActivity.class));
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            finish();
        });
    }

    private void loadOrganizerEvents() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("organizerDeviceId", deviceId)
                .get()
                .addOnSuccessListener(this::populateList)
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.error_loading_events),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void populateList(QuerySnapshot snapshot) {
        eventList.clear();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Event event = doc.toObject(Event.class);
            if (event != null) {
                if (event.getId() == null || event.getId().isEmpty()) {
                    event.setId(doc.getId());
                }
                if (event.getEventId() == null || event.getEventId().isEmpty()) {
                    event.setEventId(doc.getId());
                }
                eventList.add(event);
            }
        }

        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(eventList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}