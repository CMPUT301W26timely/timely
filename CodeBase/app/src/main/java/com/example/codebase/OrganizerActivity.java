package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared home screen for organizer and administrator sessions.
 *
 * <p>Organizer sessions keep the original "My Events" behaviour and only show events
 * created by the current device. Administrator sessions reuse this screen as a system
 * browser entry point, so the second bottom-nav slot becomes "Profiles" and opens
 * {@link AdminBrowseProfilesActivity}. Tapping an event row always opens
 * {@link EventDetailActivity}. The floating action button opens
 * {@link CreateEventActivity} for organizers only.
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

    /** Title at the top of the shared organizer/admin home screen. */
    private TextView tvTitle;

    /** Admin-only subtitle that explains the broader browse scope. */
    private TextView tvSubtitle;

    /** Shared second-tab label, shown as History for entrants and Profiles for admins. */
    private TextView navHistoryLabel;

    /** Shared second-tab icon, swapped for admin profile browsing. */
    private ImageView navHistoryIcon;

    /** Shared third-tab label, renamed from "My Events" to "Events" for admins. */
    private TextView navMyEventsLabel;

    /** Adapter binding {@link #eventList} to {@link #rvEvents}. */
    private OrganizerEventAdapter adapter;

    /** Live list of events owned by the current device; mutated in place on reload. */
    private final List<Event> eventList = new ArrayList<>();

    /** Device ID used to filter events by {@code organizerDeviceId} in Firestore. */
    private String deviceId;

    /** Whether the current app session is using the administrator role. */
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
        navHistoryLabel = findViewById(R.id.navHistoryLabel);
        navHistoryIcon = findViewById(R.id.navHistoryIcon);
        navMyEventsLabel = findViewById(R.id.navMyEventsLabel);

        adapter = new OrganizerEventAdapter(eventList, isAdminSession, new OrganizerEventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                Intent intent = new Intent(OrganizerActivity.this, EventDetailActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getId());
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_TITLE, event.getTitle());
                startActivity(intent);
            }

            @Override
            public void onEventDelete(Event event) {
                if (isAdminSession) {
                    showDeleteConfirmationDialog(event);
                }
            }
        });

        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);

        fabCreate.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        configureRoleSpecificUi();
        setupBottomNavigation();
        loadHomeEvents();
    }

    /**
     * Shows a confirmation dialog before deleting an event as an administrator.
     *
     * @param event The event to be deleted.
     */
    private void showDeleteConfirmationDialog(Event event) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to permanently delete this event: \"" + event.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteEventFromDatabase(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the specified event from Firestore.
     *
     * @param event The event to delete.
     */
    private void deleteEventFromDatabase(Event event) {
        AppDatabase.getInstance().eventsRef.document(event.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                    loadHomeEvents(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
                });
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
     * Applies the small UI differences between organizer and administrator sessions.
     *
     * <p>This keeps the original organizer screen intact while making the shared admin
     * entry points obvious: admins browse events here and open the profile directory
     * from the second navigation slot.</p>
     */
    private void configureRoleSpecificUi() {
        if (isAdminSession) {
            findViewById(R.id.navBarOrganizer).setVisibility(View.GONE);
            findViewById(R.id.navBarAdmin).setVisibility(View.VISIBLE);

            tvTitle.setText(R.string.browse_events_title);
            tvSubtitle.setVisibility(View.VISIBLE);
            tvNoEvents.setText(R.string.admin_browse_events_empty);
            navMyEventsLabel.setText(R.string.admin_nav_events);
            fabCreate.setVisibility(View.GONE);
            return;
        }

        findViewById(R.id.navBarOrganizer).setVisibility(View.VISIBLE);
        findViewById(R.id.navBarAdmin).setVisibility(View.GONE);

        tvTitle.setText(R.string.my_events_title);
        tvSubtitle.setVisibility(View.GONE);
        tvNoEvents.setText(R.string.no_events_yet);
        fabCreate.setVisibility(View.VISIBLE);
    }

    /**
     * Wires the bottom navigation items to their respective destinations.
     *
     * <ul>
     *   <li>My Events → no-op (already on this screen)</li>
     *   <li>Profile → {@link ProfileActivity}</li>
     *   <li>Explore → {@link BrowseEventsActivity}</li>
     *   <li>History → {@link HistoryActivity}</li>
     *   <li>Notifications → {@link NotificationsActivity}</li>
     *   <li>History/Profiles → {@link HistoryActivity} for organizers,
     *       {@link AdminBrowseProfilesActivity} for administrators</li>
     * </ul>
     */
    private void setupBottomNavigation() {
        if (isAdminSession) {
            findViewById(R.id.navImage).setOnClickListener(v -> {
                startActivity(new Intent(this, AdminBrowseImagesActivity.class));
                finish();
            });
            findViewById(R.id.navAdminProfiles).setOnClickListener(v -> {
                startActivity(new Intent(this, AdminBrowseProfilesActivity.class));
                finish();
            });
            findViewById(R.id.navAdminMyEvents).setOnClickListener(v -> {
                // already here
            });
            findViewById(R.id.navAdminNotifications).setOnClickListener(v -> {
                startActivity(new Intent(this, NotificationsActivity.class));
                finish();
            });
            findViewById(R.id.navAdminProfile).setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
            });
            return;
        }

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

        // Reuse the shared second nav slot so admins can open the profile browser
        // without changing the organizer flow for other roles.
        findViewById(R.id.navHistory).setOnClickListener(v -> {
            Intent intent = new Intent(
                    this,
                    isAdminSession ? AdminBrowseProfilesActivity.class : HistoryActivity.class
            );
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            finish();
        });
    }

    /**
     * Loads the event list that matches the current role.
     *
     * <p>Organizers still see only their own events, while administrators browse
     * the full event catalogue before drilling into profile management.</p>
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
     * Replaces the visible event list and updates the shared empty state.
     *
     * @param events normalized events ready for display
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
