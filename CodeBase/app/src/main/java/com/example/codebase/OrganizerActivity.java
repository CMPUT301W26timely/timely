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
    private List<OrganizerEvent>  eventList = new ArrayList<>();
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
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID,    event.id);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.title);
            startActivity(intent);
        });
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        fabCreate.setOnClickListener(v ->
                Toast.makeText(this,
                        getString(R.string.create_event_coming_soon),
                        Toast.LENGTH_SHORT).show()
        );

        loadOrganizerEvents();
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
            event.title = doc.getString("name") != null
                    ? doc.getString("name") : "Untitled Event";
            event.posterBase64 = doc.getString("posterBase64");

            // ── Timestamps → Date ─────────────────────────────────────────────
            Timestamp regOpenTs     = doc.getTimestamp("regOpen");
            Timestamp regDeadlineTs = doc.getTimestamp("regClose");
            Timestamp startDateTs   = doc.getTimestamp("eventStart");
            Timestamp endDateTs     = doc.getTimestamp("eventEnd");

            event.regOpen              = regOpenTs     != null ? regOpenTs.toDate()      : null;
            event.registrationDeadline = regDeadlineTs != null ? regDeadlineTs.toDate()  : null;
            // drawDate = regClose + 3 days (calculated, not stored in Firestore)
            if (event.registrationDeadline != null) {
                event.drawDate = new java.util.Date(event.registrationDeadline.getTime() + 3L * 24 * 60 * 60 * 1000);
            }
            event.startDate = startDateTs != null ? startDateTs.toDate() : null;
            event.endDate   = endDateTs   != null ? endDateTs.toDate()   : null;

            // ── Display date on card ───────────────────────────────────────────
            event.displayDate = event.startDate != null
                    ? displayFormat.format(event.startDate) : "Date not set";

            // ── Arrays ────────────────────────────────────────────────────────
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

    // ─── Event model ──────────────────────────────────────────────────────────

    public static class OrganizerEvent {
        public String id;
        public String title;
        public String displayDate;           // formatted startDate shown on card
        public String posterBase64;          // Base64 encoded poster image

        // Dates for status calculation
        public Date regOpen;             // registration start date
        public Date registrationDeadline; // regClose
        public Date drawDate;             // calculated: regClose + 3 days
        public Date startDate;            // eventStart
        public Date endDate;              // eventEnd

        // Arrays for status calculation
        public List<?> selectedEntrants;
        public List<?> enrolledEntrants;

        // Counts for card stats
        public int waitingCount;
        public int selectedCount;
    }
}