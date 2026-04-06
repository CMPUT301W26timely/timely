package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

    /** Supporting copy under the empty-state headline. */
    private TextView emptyStateSubtitle;

    /** Describes the number of pending invitations inside {@link #invitationCard}. */
    private TextView invitationCountSummary;

    /** Top title, adapted for admin log review mode. */
    private TextView titleView;

    /** Optional subtitle shown in admin log review mode. */
    private TextView subtitleView;

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

    /** Event IDs that currently have an actionable invitation for this entrant. */
    private final Set<String> actionableInvitationEventIds = new HashSet<>();

    /** Device ID of the current user, used as the Firestore notifications document key. */
    private String deviceId;

    /** Whether the current app session is using the administrator role. */
    private boolean isAdminSession;

    /** Formats admin log timestamps for display. */
    private final SimpleDateFormat logDateFormat =
            new SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault());

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

        isAdminSession = WelcomeActivity.ROLE_ADMIN.equals(WelcomeActivity.getSessionRole(this));

        if (isAdminSession) {
            findViewById(R.id.navBarEntrant).setVisibility(View.GONE);
            findViewById(R.id.navBarAdmin).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.navBarEntrant).setVisibility(View.VISIBLE);
            findViewById(R.id.navBarAdmin).setVisibility(View.GONE);
        }

        listViewNotifications = findViewById(R.id.listViewNotifications);
        emptyStateCard        = findViewById(R.id.cardNotificationsEmptyState);
        invitationCard        = findViewById(R.id.invitationSummaryInclude);
        emptyState            = findViewById(R.id.tvNotificationsEmptyState);
        emptyStateSubtitle    = findViewById(R.id.tvNotificationsEmptySubtitle);
        invitationCountSummary = findViewById(R.id.tvInvitationCountSummary);
        titleView             = findViewById(R.id.tvNotificationsTitle);
        subtitleView          = findViewById(R.id.tvNotificationsSubtitle);
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        listViewNotifications.setDivider(null);
        listViewNotifications.setDividerHeight(0);

        invitationCard.setOnClickListener(v ->
                startActivity(new Intent(this, InvitationsActivity.class)));

        if (isAdminSession) {
            titleView.setText(R.string.admin_notification_logs_title);
            subtitleView.setVisibility(View.VISIBLE);
            subtitleView.setText(R.string.admin_notification_logs_subtitle);
            invitationCard.setVisibility(View.GONE);
        }

        setupBottomNavigation();
        if (!isAdminSession) {
            loadInvitationSummary();
        }
        loadNotifications();
    }

    /**
     * Refreshes the invitation summary and notification list each time the activity
     * returns to the foreground, ensuring data stays current after returning from
     * {@link InvitationsActivity} or {@link EntrantEventDetailActivity}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!isAdminSession) {
            loadInvitationSummary();
        }
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
        if (isAdminSession) {
            AppDatabase.getInstance()
                    .notificationLogsRef
                    .orderBy("sentAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots ->
                            populateNotificationLogs(queryDocumentSnapshots.getDocuments()))
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    R.string.admin_notification_logs_load_failed,
                                    Toast.LENGTH_SHORT).show());
            return;
        }

        AppDatabase.getInstance()
                .notificationsRef
                .document(deviceId)
                .collection("messages")
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        populateNotifications(queryDocumentSnapshots.getDocuments()))
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
        PendingInvitationHelper.loadPendingInvitationEventIds(deviceId,
                new PendingInvitationHelper.PendingInvitationCallback() {
                    @Override
                    public void onLoaded(Set<String> eventIds) {
                        actionableInvitationEventIds.clear();
                        actionableInvitationEventIds.addAll(eventIds);
                        updateInvitationCard();
                    }

                    @Override
                    public void onError(Exception e) {
                        actionableInvitationEventIds.clear();
                        updateInvitationCard();
                    }
                });
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
        items.clear();
        eventIds.clear();

        for (DocumentSnapshot doc : documents) {
            AppNotification notification = doc.toObject(AppNotification.class);
            if (notification != null && notification.getEventId() != null) {
                addNotification(
                        getNotificationLabel(notification),
                        notification.getMessage() != null ? notification.getMessage() : "No message",
                        notification.getEventTitle(),
                        notification.getEventId(),
                        notification.getType()
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
            String eventId = eventIds.get(position);
            if (eventId == null || eventId.trim().isEmpty()) {
                return;
            }

            Intent intent = new Intent(this, EntrantEventDetailActivity.class);
            intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, eventId);
            startActivity(intent);
        });
    }

    private void populateNotificationLogs(java.util.List<DocumentSnapshot> documents) {
        items.clear();
        eventIds.clear();

        for (DocumentSnapshot doc : documents) {
            NotificationLogEntry logEntry = doc.toObject(NotificationLogEntry.class);
            if (logEntry == null) {
                continue;
            }

            String targetGroup = logEntry.getTargetGroup() != null
                    ? logEntry.getTargetGroup()
                    : getString(R.string.admin_notification_logs_default_group);
            String message = logEntry.getTitle() != null && !logEntry.getTitle().trim().isEmpty()
                    ? logEntry.getTitle()
                    : getString(R.string.admin_notification_logs_default_title);
            String meta = buildLogMeta(logEntry);

            addNotification(
                    targetGroup,
                    message,
                    meta,
                    logEntry.getEventId(),
                    "adminLog"
            );
        }

        NotificationListAdapter adapter = new NotificationListAdapter(this, items);
        listViewNotifications.setAdapter(adapter);

        boolean hasLogs = !items.isEmpty();
        emptyStateCard.setVisibility(hasLogs ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(hasLogs ? View.GONE : View.VISIBLE);
        emptyStateSubtitle.setVisibility(hasLogs ? View.GONE : View.VISIBLE);
        listViewNotifications.setVisibility(hasLogs ? View.VISIBLE : View.GONE);
        emptyState.setText(R.string.admin_notification_logs_empty_title);
        emptyStateSubtitle.setText(R.string.admin_notification_logs_empty_subtitle);

        listViewNotifications.setOnItemClickListener((parent, view, position, id) -> {
            String eventId = eventIds.get(position);
            if (eventId == null || eventId.trim().isEmpty()) {
                return;
            }

            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
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
                startActivity(new Intent(this, OrganizerActivity.class));
                finish();
            });
            findViewById(R.id.navAdminNotifications).setOnClickListener(v -> {
                // already here
            });
            findViewById(R.id.navAdminProfile).setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
            });
            return;
        }

        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseEventsActivity.class));
            finish();
        });

        // Reuse the same nav slot for entrant history and admin profile browsing.
        findViewById(R.id.navHistory).setOnClickListener(v -> {
            RoleAwareNavHelper.openSecondaryNav(this, true);
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
     * @param type    The notification recipient group type.
     */
    private void addNotification(String status,
                                 String message,
                                 String meta,
                                 String eventId,
                                 String type) {
        HashMap<String, String> row = new HashMap<>();
        row.put("status", status);
        row.put("message", message);
        row.put("meta", meta != null ? meta : "");
        items.add(row);
        eventIds.add(eventId);
    }

    private String buildLogMeta(NotificationLogEntry logEntry) {
        String eventTitle = logEntry.getEventTitle() != null && !logEntry.getEventTitle().trim().isEmpty()
                ? logEntry.getEventTitle().trim()
                : getString(R.string.untitled_event);
        Date sentAt = logEntry.getSentAt();
        String timestamp = sentAt != null
                ? logDateFormat.format(sentAt)
                : getString(R.string.admin_notification_logs_unknown_time);

        return getString(
                R.string.admin_notification_logs_meta,
                eventTitle,
                logEntry.getRecipientCount(),
                timestamp
        );
    }

    private void updateInvitationCard() {
        int invitationCount = actionableInvitationEventIds.size();
        invitationCard.setVisibility(View.VISIBLE);
        if (invitationCount > 0) {
            invitationCountSummary.setText(
                    "You have " + invitationCount + " invitation"
                            + (invitationCount > 1 ? "s" : "")
                            + " awaiting a response.");
        } else {
            invitationCountSummary.setText("No invitations are awaiting a response.");
        }
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
        if ("privateInvite".equals(type))     return "Private Invite";
        return "Notification";
    }
}
