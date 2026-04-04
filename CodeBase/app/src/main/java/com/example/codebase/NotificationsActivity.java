package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Displays the current device's notifications and pending event invitations.
 *
 * <p>Notifications are loaded from the Firestore path
 * {@code notifications/<deviceId>/messages}, ordered by {@code sentAt} descending,
 * and rendered in a {@link ListView} via {@link NotificationListAdapter}. Tapping a
 * row navigates to {@link EntrantEventDetailActivity} for the associated event.
 *
 * <p>An invitation summary card is shown above the list when the device is present
 * in any event's {@code selectedEntrants} list but has not yet enrolled. Tapping the
 * card opens {@link InvitationsActivity}. The card is hidden when there are no
 * pending invitations or if the Firestore query fails.
 *
 * <p>Both the notification list and the invitation summary are refreshed every time
 * the activity resumes via {@link #onResume()}.
 *
 * @see NotificationListAdapter
 * @see AppNotification
 * @see AppDatabase
 */
public class NotificationsActivity extends AppCompatActivity {

    /** Displays the list of notifications. */
    private ListView listViewNotifications;

    /** Empty-state card shown when there are no notifications. */
    private View emptyStateCard;

    /** Card shown at the top when there are pending event invitations. */
    private View invitationCard;

    /** Empty-state label inside {@link #emptyStateCard}. */
    private TextView emptyState;

    /** Supporting copy shown under {@link #emptyState}. */
    private TextView emptyStateSubtitle;

    /** Describes the number of pending invitations inside {@link #invitationCard}. */
    private TextView invitationCountSummary;

    /**
     * Tracks whether organizer/admin notifications are currently enabled for the
     * signed-in device. Async Firestore results check this flag before updating UI
     * so a late-arriving query cannot repopulate the screen after the entrant opts out.
     */
    private boolean notificationsEnabled = true;

    /**
     * Row data for {@link NotificationListAdapter}. Each map contains
     * {@code "status"} and {@code "message"} keys.
     */
    private final ArrayList<HashMap<String, String>> items = new ArrayList<>();

    /**
     * Parallel list of event IDs corresponding to each entry in {@link #items},
     * used to launch {@link EntrantEventDetailActivity} on item tap.
     */
    private final ArrayList<String> eventIds = new ArrayList<>();

    /** Device ID of the current user, used as the Firestore notifications document key. */
    private String deviceId;

    /**
     * Initialises the activity, binds views, sets up the invitation card click listener,
     * configures bottom navigation, and triggers the initial data loads.
     *
     * @param savedInstanceState Previously saved instance state, or {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        listViewNotifications = findViewById(R.id.listViewNotifications);
        emptyStateCard        = findViewById(R.id.cardNotificationsEmptyState);
        invitationCard        = findViewById(R.id.invitationSummaryInclude);
        emptyState            = findViewById(R.id.tvNotificationsEmptyState);
        emptyStateSubtitle    = findViewById(R.id.tvNotificationsEmptySubtitle);
        invitationCountSummary = findViewById(R.id.tvInvitationCountSummary);
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        listViewNotifications.setDivider(null);
        listViewNotifications.setDividerHeight(0);

        invitationCard.setOnClickListener(v ->
                startActivity(new Intent(this, InvitationsActivity.class)));

        setupBottomNavigation();
        refreshNotificationPreferencesAndContent();
    }

    /**
     * Refreshes the invitation summary and notification list each time the activity
     * returns to the foreground, ensuring data stays current after returning from
     * {@link InvitationsActivity} or {@link EntrantEventDetailActivity}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        refreshNotificationPreferencesAndContent();
    }

    /**
     * Loads the entrant's latest notification preference before refreshing any
     * organizer/admin notification content on screen.
     *
     * <p>If cached profile data already exists, the UI is updated immediately so the
     * screen feels responsive while the background refresh confirms the latest value.
     */
    private void refreshNotificationPreferencesAndContent() {
        if (AppCache.getInstance().hasCachedUser()) {
            applyNotificationPreference(AppCache.getInstance().getCachedUser().isNotificationsEnabled());
        } else {
            applyNotificationPreference(true);
        }

        UserRepository.loadUserProfile(this, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                applyNotificationPreference(NotificationPreferenceHelper.isNotificationsEnabled(user));
            }

            @Override
            public void onError(Exception e) {
                // Fall back to the legacy enabled behaviour if the profile cannot be read.
                applyNotificationPreference(true);
            }
        });
    }

    /**
     * Applies the current notification preference to the screen and either refreshes
     * notification content or replaces it with an opted-out empty state.
     *
     * @param enabled {@code true} when organizer/admin notifications are enabled
     */
    private void applyNotificationPreference(boolean enabled) {
        notificationsEnabled = enabled;

        if (!enabled) {
            showOptedOutState();
            return;
        }

        showDefaultEmptyState();
        loadInvitationSummary();
        loadNotifications();
    }

    /**
     * Fetches the device's notification messages from Firestore, ordered by
     * {@code sentAt} descending, and delegates rendering to
     * {@link #populateNotifications(java.util.List)}.
     *
     * <p>Shows a toast on failure.
     */
    private void loadNotifications() {
        if (!notificationsEnabled) {
            showOptedOutState();
            return;
        }

        AppDatabase.getInstance()
                .notificationsRef
                .document(deviceId)
                .collection("messages")
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!notificationsEnabled) {
                        return;
                    }
                    populateNotifications(queryDocumentSnapshots.getDocuments());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load notifications",
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Queries Firestore for events where {@code selectedEntrants} contains the
     * current {@link #deviceId}, counts those where the device is not yet enrolled,
     * and shows or hides {@link #invitationCard} accordingly.
     *
     * <p>An event counts as a pending invitation when {@link #deviceId} is in
     * {@code selectedEntrants} but <em>not</em> in {@code enrolledEntrants}.
     * The summary text is pluralised based on the count.
     *
     * <p>Hides the card on query failure.
     */
    private void loadInvitationSummary() {
        if (!notificationsEnabled) {
            invitationCard.setVisibility(View.GONE);
            return;
        }

        AppDatabase.getInstance()
                .eventsRef
                .whereArrayContains("selectedEntrants", deviceId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!notificationsEnabled) {
                        invitationCard.setVisibility(View.GONE);
                        return;
                    }

                    int invitationCount = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Event event = EventSchema.normalizeLoadedEvent(doc);
                        if (event == null) continue;

                        boolean isEnrolled = event.getEnrolledEntrants() != null
                                && event.getEnrolledEntrants().contains(deviceId);
                        if (!isEnrolled) {
                            invitationCount++;
                        }
                    }

                    if (invitationCount > 0) {
                        invitationCard.setVisibility(View.VISIBLE);
                        invitationCountSummary.setText(
                                "You have " + invitationCount + " invitation"
                                        + (invitationCount > 1 ? "s" : "")
                                        + " awaiting a response.");
                    } else {
                        invitationCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> invitationCard.setVisibility(View.GONE));
    }

    /**
     * Builds the adapter data from the given Firestore documents, binds the
     * {@link NotificationListAdapter} to the list, toggles the empty-state views,
     * and sets an item-click listener that opens {@link EntrantEventDetailActivity}.
     *
     * <p>Documents that cannot be mapped to an {@link AppNotification} or that have
     * a {@code null} event ID are silently skipped.
     *
     * @param documents The list of Firestore {@link DocumentSnapshot} objects from the
     *                  {@code messages} sub-collection.
     */
    private void populateNotifications(java.util.List<DocumentSnapshot> documents) {
        if (!notificationsEnabled) {
            showOptedOutState();
            return;
        }

        items.clear();
        eventIds.clear();

        for (DocumentSnapshot doc : documents) {
            AppNotification notification = doc.toObject(AppNotification.class);
            if (notification != null && notification.getEventId() != null) {
                addNotification(
                        getNotificationLabel(notification),
                        notification.getMessage() != null ? notification.getMessage() : "No message",
                        notification.getEventId()
                );
            }
        }

        NotificationListAdapter adapter = new NotificationListAdapter(this, items);
        listViewNotifications.setAdapter(adapter);

        boolean hasNotifications = !items.isEmpty();
        emptyStateCard.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        emptyStateSubtitle.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        listViewNotifications.setVisibility(hasNotifications ? View.VISIBLE : View.GONE);

        listViewNotifications.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, EntrantEventDetailActivity.class);
            intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, eventIds.get(position));
            startActivity(intent);
        });
    }

    /**
     * Wires the five bottom navigation items to their respective destinations.
     *
     * <ul>
     *   <li>Explore → {@link BrowseEventsActivity}</li>
     *   <li>History → {@link HistoryActivity}</li>
     *   <li>My Events → {@link OrganizerActivity}</li>
     *   <li>Notifications → no-op (already on this screen)</li>
     *   <li>Profile → {@link ProfileActivity}</li>
     * </ul>
     */
    private void setupBottomNavigation() {
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseEventsActivity.class));
            finish();
        });

        findViewById(R.id.navHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });

        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerActivity.class));
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            // already here
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    /**
     * Appends a notification row to {@link #items} and its associated event ID to
     * {@link #eventIds}.
     *
     * @param status  The status label to display (e.g. "Selected", "Cancelled").
     * @param message The notification message body.
     * @param eventId The Firestore event document ID associated with this notification.
     */
    private void addNotification(String status, String message, String eventId) {
        HashMap<String, String> row = new HashMap<>();
        row.put("status", status);
        row.put("message", message);
        items.add(row);
        eventIds.add(eventId);
    }

    /**
     * Derives a human-readable label for a notification row.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>If {@link AppNotification#getStatus()} is non-null and non-blank, it is
     *       returned as-is.</li>
     *   <li>Otherwise the {@link AppNotification#getType()} field is mapped:
     *     <ul>
     *       <li>{@code "selectedEntrants"} → {@code "Selected"}</li>
     *       <li>{@code "cancelledEntrants"} → {@code "Cancelled"}</li>
     *       <li>{@code "waitingList"} → {@code "Waiting List"}</li>
     *       <li>Any other value → {@code "Notification"}</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param notification The {@link AppNotification} to derive a label for.
     * @return A non-null display label string.
     */
    private String getNotificationLabel(AppNotification notification) {
        if (notification.getStatus() != null && !notification.getStatus().trim().isEmpty()) {
            return notification.getStatus();
        }

        String type = notification.getType();
        if ("selectedEntrants".equals(type))  return "Selected";
        if ("cancelledEntrants".equals(type)) return "Cancelled";
        if ("waitingList".equals(type))       return "Waiting List";
        return "Notification";
    }

    /** Restores the default empty-state copy used when notifications are enabled. */
    private void showDefaultEmptyState() {
        emptyState.setText(R.string.notifications_empty_title);
        emptyStateSubtitle.setText(R.string.notifications_empty_subtitle);
    }

    /**
     * Replaces the list with an opted-out message so entrants can clearly see that
     * organizer/admin notifications are disabled for their profile.
     */
    private void showOptedOutState() {
        invitationCard.setVisibility(View.GONE);
        clearNotifications();
        emptyStateCard.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.VISIBLE);
        emptyStateSubtitle.setVisibility(View.VISIBLE);
        emptyState.setText(R.string.notifications_opted_out_title);
        emptyStateSubtitle.setText(R.string.notifications_opted_out_subtitle);
        listViewNotifications.setVisibility(View.GONE);
    }

    /** Clears any currently rendered notification rows from the list view. */
    private void clearNotifications() {
        items.clear();
        eventIds.clear();
        listViewNotifications.setAdapter(new NotificationListAdapter(this, items));
    }
}
