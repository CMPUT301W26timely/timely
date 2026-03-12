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
                .whereEqualTo("organizerId", deviceId)
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

        SimpleDateFormat displayFormat =
                new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault());

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Event event = new Event();
            event.setId(doc.getId());

            event.setTitle(doc.getString("name") != null ? doc.getString("name") :
                    (doc.getString("title") != null ? doc.getString("title") : "Untitled Event"));

            event.setLocation(doc.getString("location") != null ? doc.getString("location") : "Location TBD");
            event.setStatus(doc.getString("status") != null ? doc.getString("status") : "draft");

            Timestamp regDeadlineTs = doc.getTimestamp("regClose");
            Timestamp drawDateTs    = doc.getTimestamp("drawDate"); // May not exist
            Timestamp startDateTs   = doc.getTimestamp("eventStart");
            Timestamp endDateTs     = doc.getTimestamp("eventEnd");

            event.setRegistrationDeadline(regDeadlineTs != null ? regDeadlineTs.toDate() : null);
            event.setDrawDate(drawDateTs    != null ? drawDateTs.toDate()     : null);
            event.setStartDate(startDateTs   != null ? startDateTs.toDate()    : null);
            event.setEndDate(endDateTs     != null ? endDateTs.toDate()      : null);

            List<?> waitingList      = (List<?>) doc.get("waitingList");
            List<?> selectedEntrants = (List<?>) doc.get("selectedEntrants");
            List<?> enrolledEntrants = (List<?>) doc.get("enrolledEntrants");

            event.setWaitingList(waitingList != null
                    ? (List<String>) waitingList
                    : new ArrayList<>());
            event.setSelectedEntrants(selectedEntrants != null
                    ? (List<String>) selectedEntrants
                    : new ArrayList<>());
            event.setEnrolledEntrants(enrolledEntrants != null
                    ? (List<String>) enrolledEntrants
                    : new ArrayList<>());

            event.setPoster(new EventPoster(doc.get("posterBase64").toString()));

            eventList.add(event);
        }

        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(eventList.isEmpty()   ? View.GONE    : View.VISIBLE);
    }
}