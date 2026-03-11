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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OrganizerActivity — "My Events" screen for Organizer role.
 *
 * Add to AndroidManifest.xml:
 *   <activity android:name=".OrganizerActivity" android:exported="false" />
 *
 * Route here from MainActivity when role = ORGANIZER.
 */
public class OrganizerActivity extends AppCompatActivity {

    private RecyclerView          rvEvents;
    private TextView              tvNoEvents;
    private FloatingActionButton  fabCreate;
    private OrganizerEventAdapter adapter;
    private List<Event>  eventList = new ArrayList<>();
    private String                deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        // Back button → WelcomeActivity
        findViewById(R.id.btnBackOrganizer).setOnClickListener(v -> {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

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

        loadOrganizerEvents();
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

        SimpleDateFormat displayFormat =
                new SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault());

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Event event = new Event();
            event.setId(doc.getId());
            event.setTitle(doc.getString("title") != null
                    ? doc.getString("title") : "Untitled Event");

            // ── Timestamps → Date ─────────────────────────────────────────────
            Timestamp regDeadlineTs = doc.getTimestamp("registrationDeadline");
            Timestamp drawDateTs    = doc.getTimestamp("drawDate");
            Timestamp startDateTs   = doc.getTimestamp("startDate");
            Timestamp endDateTs     = doc.getTimestamp("endDate");

            event.setRegistrationDeadline(regDeadlineTs != null ? regDeadlineTs.toDate() : null);
            event.setDrawDate(drawDateTs    != null ? drawDateTs.toDate()     : null);
            event.setStartDate(startDateTs   != null ? startDateTs.toDate()    : null);
            event.setEndDate(endDateTs     != null ? endDateTs.toDate()      : null);

//            // ── Display date on card ───────────────────────────────────────────
//            event.displayDate = event.startDate != null
//                    ? displayFormat.format(event.startDate) : "Date not set";

            // ── Arrays ────────────────────────────────────────────────────────
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

            Object posterObj = doc.get("poster");

            if (posterObj instanceof Map) {
                Map<String, Object> posterMap = (Map<String, Object>) posterObj;
                String base64 = (String) posterMap.get("posterImageBase64");

                if (base64 != null) {
                    event.setPoster(new EventPoster(base64));
                }
            }

            eventList.add(event);
        }

        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(eventList.isEmpty()   ? View.GONE    : View.VISIBLE);
    }

    // ─── Event model ──────────────────────────────────────────────────────────

    public static class OrganizerEvent {
        public String id;
        public String title;
        public String displayDate;           // formatted startDate shown on card

        // Dates for status calculation
        public Date registrationDeadline;
        public Date drawDate;
        public Date startDate;
        public Date endDate;

        // Arrays for status calculation
        public List<?> selectedEntrants;
        public List<?> enrolledEntrants;

        // Counts for card stats
        public int waitingCount;
        public int selectedCount;
    }
}