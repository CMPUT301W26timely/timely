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
    private List<OrganizerEvent>         eventList = new ArrayList<>();
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
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID,    event.id);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.title);
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
            OrganizerEvent event = new OrganizerEvent();
            event.id    = doc.getId();
            
            event.title = doc.getString("name") != null ? doc.getString("name") :
                          (doc.getString("title") != null ? doc.getString("title") : "Untitled Event");
                          
            event.location = doc.getString("location") != null ? doc.getString("location") : "Location TBD";
            event.status = doc.getString("status") != null ? doc.getString("status") : "draft";

            Timestamp regDeadlineTs = doc.getTimestamp("regClose");
            Timestamp drawDateTs    = doc.getTimestamp("drawDate"); // May not exist
            Timestamp startDateTs   = doc.getTimestamp("eventStart");
            Timestamp endDateTs     = doc.getTimestamp("eventEnd");

            event.registrationDeadline = regDeadlineTs != null ? regDeadlineTs.toDate() : null;
            event.drawDate             = drawDateTs    != null ? drawDateTs.toDate()     : null;
            event.startDate            = startDateTs   != null ? startDateTs.toDate()    : null;
            event.endDate              = endDateTs     != null ? endDateTs.toDate()      : null;

            event.displayDate = event.startDate != null
                    ? displayFormat.format(event.startDate) : "Date not set";

            List<?> waitingList      = (List<?>) doc.get("waitingList");
            List<?> selectedEntrants = (List<?>) doc.get("selectedEntrants");
            List<?> enrolledEntrants = (List<?>) doc.get("enrolledEntrants");

            event.waitingCount      = waitingList      != null ? waitingList.size()      : 0;
            event.selectedCount     = selectedEntrants != null ? selectedEntrants.size() : 0;
            event.selectedEntrants  = selectedEntrants;
            event.enrolledEntrants  = enrolledEntrants;

            eventList.add(event);
        }

        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(eventList.isEmpty()   ? View.GONE    : View.VISIBLE);
    }

    public static class OrganizerEvent {
        public String id;
        public String title;
        public String location;
        public String status; // "published" or "draft"
        public String displayDate;
        public Date registrationDeadline;
        public Date drawDate;
        public Date startDate;
        public Date endDate;
        public List<?> selectedEntrants;
        public List<?> enrolledEntrants;
        public int waitingCount;
        public int selectedCount;
    }
}