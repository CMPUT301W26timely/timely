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
 * Home screen for organizer and administrator sessions.
 *
 * <p>Organizers see only the events they own, while administrators see the full
 * system event catalogue and can jump to the profile browser from the second
 * bottom-navigation tab. Tapping an event row opens {@link EventDetailActivity}.
 * The floating action button remains available only for organizer sessions and
 * opens {@link CreateEventActivity} to create a new event.
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

    /** Role-aware title shown at the top of the screen. */
    private TextView tvTitle;

    /** Optional admin subtitle explaining the browse scope. */
    private TextView tvSubtitle;

    /** Bottom-nav label reused as "Search" for entrants and "Profiles" for admins. */
    private TextView navSearchLabel;

    /** Bottom-nav icon reused as search for entrants and profile browsing for admins. */
    private ImageView navSearchIcon;

    /** Bottom-nav label shown as "My Events" for organizers and "Events" for admins. */
    private TextView navMyEventsLabel;

    /** Adapter binding {@link #eventList} to {@link #rvEvents}. */
    private OrganizerEventAdapter adapter;

    /** Live list of events owned by the current device; mutated in place on reload. */
    private final List<Event> eventList = new ArrayList<>();

    /** Device ID used to filter events by {@code organizerDeviceId} in Firestore. */
    private String deviceId;

    /** Whether the active session belongs to an administrator. */
    private boolean isAdminSession;

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
        isAdminSession = WelcomeActivity.ROLE_ADMIN.equals(WelcomeActivity.getSessionRole(this));

        rvEvents  = findViewById(R.id.rvEvents);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        fabCreate = findViewById(R.id.fabCreateEvent);
        tvTitle = findViewById(R.id.tvOrganizerTitle);
        tvSubtitle = findViewById(R.id.tvOrganizerSubtitle);
        navSearchLabel = findViewById(R.id.navSearchLabel);
        navSearchIcon = findViewById(R.id.navSearchIcon);
        navMyEventsLabel = findViewById(R.id.navMyEventsLabel);

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

        applyRoleConfiguration();
        setupBottomNavigation();
        loadHomeEvents();
    }

    /**
     * Reloads the organizer's event list from Firestore each time the activity
     * resumes, ensuring the list reflects any changes made in
     * {@link CreateEventActivity} or {@link EventDetailActivity}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadHomeEvents();
    }

    /**
     * Applies the small set of UI changes that distinguish organizer and admin sessions.
     *
     * <p>This keeps the original organizer screen intact while reusing the same layout
     * for administrators, who need event browsing plus a direct path to the profile
     * directory.</p>
     */
    private void applyRoleConfiguration() {
        if (isAdminSession) {
            tvTitle.setText(R.string.browse_events_title);
            tvSubtitle.setVisibility(View.VISIBLE);
            tvNoEvents.setText(R.string.admin_browse_events_empty);

            navMyEventsLabel.setText(R.string.admin_nav_events);
            navSearchLabel.setText(R.string.admin_nav_profiles);
            navSearchIcon.setImageResource(R.drawable.ic_nav_profile);
            navSearchIcon.setContentDescription(getString(R.string.admin_nav_profiles));

            // Admins browse data only here, so the organizer-only create affordance is hidden.
            fabCreate.setVisibility(View.GONE);
            return;
        }

        tvTitle.setText(R.string.my_events_title);
        tvSubtitle.setVisibility(View.GONE);
        tvNoEvents.setText(R.string.no_events_yet);

        navMyEventsLabel.setText(R.string.nav_my_events);
        navSearchLabel.setText(R.string.nav_search);
        navSearchIcon.setImageResource(R.drawable.ic_nav_search);
        navSearchIcon.setContentDescription(getString(R.string.nav_search));
        fabCreate.setVisibility(View.VISIBLE);
    }

    /**
     * Wires the five bottom navigation items to their respective destinations.
     *
     * <ul>
     *   <li>My Events → no-op (already on this screen)</li>
     *   <li>Profile → {@link ProfileActivity}</li>
     *   <li>Explore → {@link BrowseEventsActivity}</li>
     *   <li>Search/Profile tab → {@link SearchEventsActivity} for entrants,
     *       {@link AdminBrowseProfilesActivity} for admins</li>
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
            if (isAdminSession) {
                startActivity(new Intent(this, AdminBrowseProfilesActivity.class));
                return;
            }

            startActivity(new Intent(this, SearchEventsActivity.class));
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            finish();
        });
    }

    /**
     * Loads the event list appropriate for the current role.
     *
     * <p>Organizers only see events they own, while administrators see every event in
     * the system. Both paths share the same rendering code so empty states and row
     * handling stay consistent.</p>
     */
    private void loadHomeEvents() {
        if (isAdminSession) {
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
            return;
        }

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
        List<Event> normalizedEvents = new ArrayList<>();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Event event = EventSchema.normalizeLoadedEvent(doc);
            if (event != null) {
                normalizedEvents.add(event);
            }
        }

        showEvents(normalizedEvents);
    }

    /**
     * Replaces the visible event list and updates the empty state.
     *
     * @param events events ready to display in the shared home list
     */
    private void showEvents(List<Event> events) {
        eventList.clear();
        if (events != null) {
            eventList.addAll(events);
        }

        adapter.notifyDataSetChanged();
        tvNoEvents.setVisibility(eventList.isEmpty() ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(eventList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
