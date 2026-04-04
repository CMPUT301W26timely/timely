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
 *
 * Displays a scrollable list of all active events loaded from Firestore
 * via {@link EventRepository}. Tapping an event navigates to either
 * {@link EventDetailActivity} (if the current user is the organizer) or
 * {@link EntrantEventDetailActivity} (if the current user is an entrant).
 *
 * <p>Part of the bottom navigation bar — accessible via the Explore tab.</p>
 *
 * Add to AndroidManifest.xml:
 * <pre>
 *   &lt;activity android:name=".BrowseEventsActivity" android:exported="false" /&gt;
 * </pre>
 */
public class BrowseEventsActivity extends AppCompatActivity {

    /** RecyclerView that displays the list of active events. */
    private RecyclerView recyclerViewEvents;

    /** Empty state label shown when no events are available or loading fails. */
    private TextView textViewEmptyState;

    /** Device ID of the current user, used to determine organizer vs entrant role. */
    private String deviceId;

    /**
     * Called when the activity is created.
     * Initialises views, retrieves the device ID, sets up bottom navigation,
     * and triggers an event load from Firestore.
     *
     * @param savedInstanceState saved state bundle from a previous instance, or {@code null}
     */
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

    /**
     * Wires up the bottom navigation bar click listeners.
     *
     * <ul>
     *   <li>Explore — no-op (already on this screen)</li>
     *   <li>Search — shows "Not implemented yet" toast</li>
     *   <li>My Events — navigates to {@link OrganizerActivity}</li>
     *   <li>Notifications — navigates to {@link NotificationsActivity}</li>
     *   <li>Profile — navigates to {@link ProfileActivity}</li>
     * </ul>
     */
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

        findViewById(R.id.fabCamera).setOnClickListener(v -> {
            startActivity(new Intent(this, QRScannerActivity.class));
        });
    }

    /**
     * Loads all active events from Firestore via {@link EventRepository}.
     *
     * On success, passes the result to {@link #showEvents(List)}.
     * On failure, logs the error, shows a toast, and displays the empty state view.
     */
    private void loadEvents() {
        EventRepository.loadActiveEvents(new EventRepository.EventsCallback() {

            /**
             * Called when events are successfully loaded from Firestore.
             *
             * @param events the list of active {@link Event} objects
             */
            @Override
            public void onEventsLoaded(List<Event> events) {
                showEvents(events);
            }

            /**
             * Called when loading events fails.
             * Logs the exception and shows an error toast.
             *
             * @param e the exception that caused the failure
             */
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

    /**
     * Displays the loaded events in the RecyclerView.
     *
     * If the list is null or empty, the empty state view is shown and the
     * RecyclerView is hidden. Otherwise, an {@link EventAdapter} is created
     * and attached to the RecyclerView.
     *
     * <p>On event tap, navigates to:</p>
     * <ul>
     *   <li>{@link EventDetailActivity} if the current user is the event organizer</li>
     *   <li>{@link EntrantEventDetailActivity} if the current user is an entrant</li>
     * </ul>
     *
     * @param events the list of {@link Event} objects to display
     */
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