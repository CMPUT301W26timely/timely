package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * OrganizerActivity — "My Events" screen for Organizer role.
 */
public class OrganizerActivity extends AppCompatActivity {

    private RecyclerView                 rvEvents;
    private View                         tvNoEvents;
    private ExtendedFloatingActionButton fabCreate;
    private OrganizerEventAdapter        adapter;
    private List<Event>         eventList = new ArrayList<>();
    private String                       deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        rvEvents   = findViewById(R.id.rvEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        fabCreate  = findViewById(R.id.fabCreateEvent);

        adapter = new OrganizerEventAdapter(eventList, event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID,    event.getId());
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
            startActivity(intent);
        });
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        fabCreate.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class))
        );

        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
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
            eventList.add(doc.toObject(Event.class));
        }

        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(eventList.isEmpty()   ? View.GONE    : View.VISIBLE);
    }
}