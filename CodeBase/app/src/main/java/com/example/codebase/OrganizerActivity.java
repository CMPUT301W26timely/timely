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
 * "My Events" screen for the organizer role.
 *
 * <p>Displays a {@link RecyclerView} of all events whose {@code organizerDeviceId}
 * field matches the current device, loaded from Firestore. Tapping an event row
 * opens {@link EventDetailActivity}. The floating action button opens
 * {@link CreateEventActivity} to create a new event.
 *
 * <p>An empty-state label is shown when no events exist for the device.
 * The event list is reloaded on every {@link #onResume()} so it stays current
 * after returning from the create or detail screens.
 *
 * @see OrganizerEventAdapter
 * @see EventSchema
 * @see DeviceIdManager
 */
public class OrganizerActivity extends AppCompatActivity {

    /** Displays the list of events owned by the current organizer. */
    private RecyclerView rvEvents;

    /** Empty-state label shown when the organizer has no events. */
    private TextView tvNoEvents;

    /** Opens {@link CreateEventActivity} when tapped. */
    private FloatingActionButton fabCreate;

    /** Adapter binding {@link #eventList} to {@link #rvEvents}. */
    private OrganizerEventAdapter adapter;

    /** Live list of events owned by the current device; mutated in place on reload. */
    private final List<Event> eventList = new ArrayList<>();

    /** Device ID used to filter events by {@code organizerDeviceId} in Firestore. */
    private String deviceId;

    /**
     * Initialises the activity, resolves the device ID, binds views, sets up the
     * {@link RecyclerView} with {@link OrganizerEventAdapter}, wires the FAB and
     * bottom navigation, and triggers the initial event load.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        rvEvents  = findViewById(R.id.rvEvents);
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
                startActivity(new Intent(this, CreateEventActivity.class)));

        setupBottomNavigation();
        loadOrganizerEvents();
    }

    /**
     * Reloads the organizer's event list from Firestore each time the activity
     * resumes, ensuring the list reflects any changes made in
     * {@link CreateEventActivity} or {@link EventDetailActivity}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadOrganizerEvents();
    }

    /**
     * Wires the five bottom navigation items to their respective destinations.
     *
     * <ul>
     *   <li>My Events → no-op (already on this screen)</li>
     *   <li>Profile → {@link ProfileActivity}</li>
     *   <li>Explore → {@link BrowseEventsActivity}</li>
     *   <li>History → {@link HistoryActivity}</li>
     *   <li>Notifications → {@link NotificationsActivity}</li>
     * </ul>
     */
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

        // The layout keeps the legacy ID, but the second slot now consistently opens History.
        findViewById(R.id.navSearch).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            finish();
        });
    }

    /**
     * Queries Firestore for all events where {@code organizerDeviceId} equals
     * {@link #deviceId} and delegates rendering to {@link #populateList(QuerySnapshot)}.
     *
     * <p>Shows a localised error toast on failure.
     */
    private void loadOrganizerEvents() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("organizerDeviceId", deviceId)
                .get()
                .addOnSuccessListener(this::populateList)
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                getString(R.string.error_loading_events),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Clears and repopulates {@link #eventList} from the Firestore query snapshot,
     * normalising each document via {@link EventSchema#normalizeLoadedEvent(DocumentSnapshot)}.
     *
     * <p>Documents that cannot be normalised (return {@code null}) are silently
     * skipped. After updating the list, notifies the adapter and toggles the
     * empty-state label and the {@link RecyclerView} visibility.
     *
     * @param snapshot The {@link QuerySnapshot} returned by the Firestore events query.
     */
    private void populateList(QuerySnapshot snapshot) {
        eventList.clear();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Event event = EventSchema.normalizeLoadedEvent(doc);
            if (event != null) {
                eventList.add(event);
            }
        }

        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(eventList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
