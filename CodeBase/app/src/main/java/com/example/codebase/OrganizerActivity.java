package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
 * Organizer/admin event browser screen.
 *
 * <p>Role-specific behaviour:
 * <ul>
 *   <li><b>Entrant session</b> — behaves as the organizer's "My Events" screen and
 *       only shows events created by the current device.</li>
 *   <li><b>Admin session</b> — becomes the administrator browse-events screen and
 *       shows every event in the system, including closed ones.</li>
 * </ul>
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

    /** Title text that changes between organizer and administrator modes. */
    private TextView tvScreenTitle;

    /** Supporting subtitle shown only for administrator mode. */
    private TextView tvScreenSubtitle;

    /** Opens {@link CreateEventActivity} when tapped. */
    private FloatingActionButton fabCreate;

    /** Search-nav icon repurposed as the profile browser shortcut for admins. */
    private ImageView navSearchIcon;

    /** Search-nav label repurposed as the profile browser shortcut for admins. */
    private TextView navSearchLabel;

    /** Active events label, renamed to "Events" when an admin is browsing. */
    private TextView navMyEventsLabel;

    /** Adapter binding {@link #eventList} to {@link #rvEvents}. */
    private OrganizerEventAdapter adapter;

    /** Live list of events owned by the current device; mutated in place on reload. */
    private final List<Event> eventList = new ArrayList<>();

    /** Device ID used to filter events by {@code organizerDeviceId} in Firestore. */
    private String deviceId;

    /** Whether the current session is the administrator browse mode. */
    private boolean isAdminMode;

    /**
     * Initialises the activity, resolves the device ID, binds views, sets up the
     * {@link RecyclerView} with {@link OrganizerEventAdapter}, wires the FAB and
     * bottom navigation, and triggers the initial event load.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);
        isAdminMode = WelcomeActivity.ROLE_ADMIN.equals(WelcomeActivity.getSessionRole(this));

        rvEvents  = findViewById(R.id.rvEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        tvScreenTitle = findViewById(R.id.tvOrganizerTitle);
        tvScreenSubtitle = findViewById(R.id.tvOrganizerSubtitle);
        fabCreate = findViewById(R.id.fabCreateEvent);
        navSearchIcon = findViewById(R.id.navSearchIcon);
        navSearchLabel = findViewById(R.id.navSearchLabel);
        navMyEventsLabel = findViewById(R.id.navMyEventsLabel);

        adapter = new OrganizerEventAdapter(eventList, event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
            startActivity(intent);
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        applyRoleMode();
        fabCreate.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        setupBottomNavigation();
        loadVisibleEvents();
    }

    /**
     * Reloads the organizer's event list from Firestore each time the activity
     * resumes, ensuring the list reflects any changes made in
     * {@link CreateEventActivity} or {@link EventDetailActivity}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadVisibleEvents();
    }

    /**
     * Applies the role-specific screen copy and navigation affordances.
     */
    private void applyRoleMode() {
        if (isAdminMode) {
            tvScreenTitle.setText(R.string.browse_events_title);
            tvScreenSubtitle.setVisibility(View.VISIBLE);
            tvScreenSubtitle.setText(R.string.admin_browse_events_subtitle);
            tvNoEvents.setText(R.string.admin_browse_events_empty);
            fabCreate.setVisibility(View.GONE);

            // Reuse the unused search slot as the admin's profile-browser shortcut.
            findViewById(R.id.navExplore).setVisibility(View.GONE);
            navSearchIcon.setImageResource(R.drawable.ic_nav_profile);
            navSearchIcon.setContentDescription(getString(R.string.admin_nav_profiles));
            navSearchLabel.setText(R.string.admin_nav_profiles);
            navMyEventsLabel.setText(R.string.admin_nav_events);
        } else {
            tvScreenTitle.setText(R.string.my_events_title);
            tvScreenSubtitle.setVisibility(View.GONE);
            tvNoEvents.setText(R.string.no_events_yet);
            fabCreate.setVisibility(View.VISIBLE);
            findViewById(R.id.navExplore).setVisibility(View.VISIBLE);
            navSearchIcon.setImageResource(R.drawable.ic_nav_search);
            navSearchIcon.setContentDescription(getString(R.string.nav_search));
            navSearchLabel.setText(R.string.nav_search);
            navMyEventsLabel.setText(R.string.nav_my_events);
        }
    }

    /**
     * Wires the five bottom navigation items to their respective destinations.
     *
     * <ul>
     *   <li>My Events / Events → no-op (already on this screen)</li>
     *   <li>Profile → {@link ProfileActivity}</li>
     *   <li>Explore → {@link BrowseEventsActivity} for entrant mode only</li>
     *   <li>Search / Profiles → {@link AdminBrowseProfilesActivity} for admin mode</li>
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

        findViewById(R.id.navSearch).setOnClickListener(v -> {
            if (isAdminMode) {
                startActivity(new Intent(this, AdminBrowseProfilesActivity.class));
                return;
            }

            Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
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
    private void loadVisibleEvents() {
        if (isAdminMode) {
            loadAdminEvents();
            return;
        }

        loadOrganizerEvents();
    }

    /** Loads all system events for the administrator browse-events story. */
    private void loadAdminEvents() {
        EventRepository.loadAllEvents(new EventRepository.EventsCallback() {
            @Override
            public void onEventsLoaded(List<Event> events) {
                showEvents(AdminBrowseHelper.sortEventsForAdmin(events));
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(OrganizerActivity.this,
                        getString(R.string.error_loading_events),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Loads only the current device's events for organizer mode. */
    private void loadOrganizerEvents() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("organizerDeviceId", deviceId)
                .get()
                .addOnSuccessListener(this::populateOrganizerEvents)
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
    private void populateOrganizerEvents(QuerySnapshot snapshot) {
        List<Event> loadedEvents = new ArrayList<>();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Event event = EventSchema.normalizeLoadedEvent(doc);
            if (event != null) {
                loadedEvents.add(event);
            }
        }

        showEvents(loadedEvents);
    }

    /**
     * Replaces the current adapter data and toggles the empty state.
     *
     * @param loadedEvents events ready for display
     */
    private void showEvents(List<Event> loadedEvents) {
        eventList.clear();
        if (loadedEvents != null) {
            eventList.addAll(loadedEvents);
        }
        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(eventList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
