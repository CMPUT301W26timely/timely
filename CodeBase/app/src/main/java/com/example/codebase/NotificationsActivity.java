package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    /** Scrollable list that displays notification rows. */
    private RecyclerView listViewNotifications;

    /** Adapter backing the RecyclerView. */
    private NotificationListAdapter notifAdapter;

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

    /** Notification rows currently displayed in the list. */
    private final List<NotificationListAdapter.NotificationItem> notifItems = new ArrayList<>();

    /** Device ID of the current user, used as the Firestore notifications document key. */
    private String deviceId;

    /** Whether the current app session is using the administrator role. */
    private boolean isAdminSession;

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
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        listViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        notifAdapter = new NotificationListAdapter(
                notifItems,
                item -> {
                    Intent intent = new Intent(this, EntrantEventDetailActivity.class);
                    intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, item.eventId);
                    startActivity(intent);
                },
                new NotificationListAdapter.OnPrivateInviteActionListener() {
                    @Override
                    public void onAccept(NotificationListAdapter.NotificationItem item) {
                        respondToPrivateInvite(item, true);
                    }
                    @Override
                    public void onDecline(NotificationListAdapter.NotificationItem item) {
                        respondToPrivateInvite(item, false);
                    }
                });
        listViewNotifications.setAdapter(notifAdapter);

        invitationCard.setOnClickListener(v ->
                startActivity(new Intent(this, InvitationsActivity.class)));

        setupBottomNavigation();
        loadInvitationSummary();
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
        AppDatabase.getInstance()
                .eventsRef
                .whereArrayContains("selectedEntrants", deviceId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
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
     * Builds the adapter data from Firestore documents and refreshes the RecyclerView.
     * For each notification, also triggers auto-expiry of pending invites past their
     * registration deadline (US 01.05.07 Option B).
     */
    private void populateNotifications(java.util.List<DocumentSnapshot> documents) {
        notifItems.clear();

        for (DocumentSnapshot doc : documents) {
            AppNotification notification = doc.toObject(AppNotification.class);
            if (notification != null && notification.getEventId() != null) {
                notifItems.add(new NotificationListAdapter.NotificationItem(
                        getNotificationLabel(notification),
                        notification.getMessage() != null ? notification.getMessage() : "No message",
                        notification.getEventId(),
                        notification.getType()
                ));

                // US 01.05.07 Option B: auto-expire pending invites past registration deadline
                if ("privateInvite".equals(notification.getType())) {
                    autoExpirePendingInviteIfNeeded(notification.getEventId());
                }
            }
        }

        notifAdapter.updateItems(notifItems);

        boolean hasNotifications = !notifItems.isEmpty();
        emptyStateCard.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        emptyStateSubtitle.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        listViewNotifications.setVisibility(hasNotifications ? View.VISIBLE : View.GONE);
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

    // US 01.05.07: Private waiting-list invite response

    /**
     * Shows a confirmation dialog then handles Accept or Decline of a private
     * waiting-list invitation (US 01.05.07 Option B).
     * Accept: pendingInvites -> waitingList.
     * Decline: pendingInvites -> cancelledEntrants.
     */
    private void respondToPrivateInvite(
            NotificationListAdapter.NotificationItem item, boolean isAccepted) {
        // First verify the invitation is still valid server-side.
        // The entrant may have been assigned co-organizer after the notification was sent,
        // which clears pendingInvites. Without this check, Accept would incorrectly
        // add a co-organizer back to the waitingList (US 01.05.07 guard).
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(item.eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    Event latestEvent = EventSchema.normalizeLoadedEvent(doc);
                    if (latestEvent == null) {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean stillPending = latestEvent.getPendingInvites() != null
                            && latestEvent.getPendingInvites().contains(deviceId);

                    if (!stillPending) {
                        // Invitation was revoked (e.g. entrant became co-organizer,
                        // or deadline passed). Refresh to hide stale Accept/Decline buttons.
                        Toast.makeText(this,
                                "This invitation is no longer valid.",
                                Toast.LENGTH_LONG).show();
                        loadNotifications();
                        return;
                    }

                    // Invitation still valid - show confirmation dialog
                    new AlertDialog.Builder(this)
                            .setTitle(isAccepted ? "Accept Invitation" : "Decline Invitation")
                            .setMessage(isAccepted
                                    ? "Accept the invitation to join the waiting list for this event?"
                                    : "Are you sure you want to decline this waiting list invitation?")
                            .setPositiveButton("Confirm", (dialog, which) -> {
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                if (isAccepted) {
                                    db.collection("events").document(item.eventId)
                                            .update(
                                                    "pendingInvites",    FieldValue.arrayRemove(deviceId),
                                                    "waitingList",        FieldValue.arrayUnion(deviceId),
                                                    "registeredEntrants", FieldValue.arrayUnion(deviceId)
                                            )
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this,
                                                        "You have joined the waiting list!",
                                                        Toast.LENGTH_SHORT).show();
                                                loadNotifications();
                                                loadInvitationSummary();
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this,
                                                    "Failed to accept. Please try again.",
                                                    Toast.LENGTH_SHORT).show());
                                } else {
                                    db.collection("events").document(item.eventId)
                                            .update(
                                                    "pendingInvites",    FieldValue.arrayRemove(deviceId),
                                                    "cancelledEntrants", FieldValue.arrayUnion(deviceId)
                                            )
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this,
                                                        "Invitation declined.",
                                                        Toast.LENGTH_SHORT).show();
                                                loadNotifications();
                                                loadInvitationSummary();
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this,
                                                    "Failed to decline. Please try again.",
                                                    Toast.LENGTH_SHORT).show());
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to verify invitation. Please try again.",
                        Toast.LENGTH_SHORT).show());
    }

    /**
     * Checks if a pending private invite has passed the registration deadline.
     * If so, auto-moves the entrant from pendingInvites to cancelledEntrants.
     * US 01.05.07 Option B deadline enforcement.
     */
    private void autoExpirePendingInviteIfNeeded(String eventId) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    Event event = EventSchema.normalizeLoadedEvent(doc);
                    if (event == null) return;
                    if (event.getPendingInvites() == null
                            || !event.getPendingInvites().contains(deviceId)) return;
                    Date deadline = event.getRegistrationDeadline();
                    if (deadline == null) return;
                    if (new Date().after(deadline)) {
                        FirebaseFirestore.getInstance()
                                .collection("events")
                                .document(eventId)
                                .update(
                                        "pendingInvites",    FieldValue.arrayRemove(deviceId),
                                        "cancelledEntrants", FieldValue.arrayUnion(deviceId)
                                )
                                .addOnSuccessListener(aVoid -> loadNotifications());
                    }
                });
    }

}