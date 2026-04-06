package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Displays the list of lottery invitations for the current device's entrant.
 *
 * <p>Queries Firestore for two invitation sources:
 * <ul>
 *   <li>events whose {@code selectedEntrants} array contains the device ID</li>
 *   <li>pending docs under {@code events/{eventId}/privateInvites/{deviceId}}</li>
 * </ul>
 * The remaining invitations are presented in a {@link RecyclerView} via
 * {@link ExploreEventAdapter}, which exposes accept, decline, and card-tap actions.
 *
 * <p>Invitation visibility rules:
 * <ul>
 *   <li>Shown if the entrant is in {@code selectedEntrants}, even if previously in
 *       {@code cancelledEntrants} (re-selected entrants must be able to respond again).</li>
 *   <li>Hidden if the entrant is already in {@code enrolledEntrants} (already accepted).</li>
 * </ul>
 */
public class InvitationsActivity extends AppCompatActivity {

    /** Scrollable list that displays pending invitation cards. */
    private RecyclerView rvInvitations;

    /** Empty-state label shown when there are no pending invitations. */
    private TextView tvNoInvitations;

    /** Loading indicator displayed during Firestore fetch and update operations. */
    private View progressBar;

    /** Adapter backing {@link #rvInvitations} with accept/decline/tap action callbacks. */
    private ExploreEventAdapter adapter;

    /** Live dataset of {@link Event} objects currently displayed in the list. */
    private List<Event> invitationList = new ArrayList<>();

    /** Unique device identifier used to query and update Firestore entrant arrays. */
    private String deviceId;

    /** Event IDs currently shown as pending private-event invites. */
    private final Set<String> pendingPrivateInviteEventIds = new HashSet<>();

    /** Event IDs currently shown as accepted invitations. */
    private final Set<String> acceptedInvitationEventIds = new HashSet<>();

    /**
     * Initialises the activity, binds views, configures the {@link RecyclerView} and its
     * adapter, and triggers the initial invitation load from Firestore.
     *
     * @param savedInstanceState If the activity is being re-created from a previous state,
     *                           this bundle contains the most recent data; otherwise {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        rvInvitations   = findViewById(R.id.rvInvitationsList);
        tvNoInvitations = findViewById(R.id.tvNoInvitationsList);
        progressBar     = findViewById(R.id.invitationsProgressBar);

        findViewById(R.id.btnBackInvitations).setOnClickListener(v -> finish());

        rvInvitations.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ExploreEventAdapter(
                invitationList,
                pendingPrivateInviteEventIds,
                acceptedInvitationEventIds,
                new ExploreEventAdapter.OnInvitationActionListener() {
            @Override
            public void onAcceptClick(Event event) {
                processInvitationResponse(event, true);
            }

            @Override
            public void onDeclineClick(Event event) {
                processInvitationResponse(event, false);
            }

            @Override
            public void onCardClick(Event event) {
                Intent intent = new Intent(InvitationsActivity.this, EntrantEventDetailActivity.class);
                intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, event.getId());
                startActivity(intent);
            }
        });
        rvInvitations.setAdapter(adapter);

        loadInvitations();
    }

    /**
     * Fetches events from Firestore where {@code selectedEntrants} contains the current
     * device ID along with any pending private-event invites.
     *
     * <p>Shows {@link #progressBar} while the queries are in flight. The screen tolerates
     * one invitation source failing as long as the other still succeeds, so selected
     * lottery invites remain actionable even if private-invite lookup is unavailable.
     */
    private void loadInvitations() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        com.google.android.gms.tasks.Task<QuerySnapshot> pendingSelectedTask = db
                .collection("events")
                .whereArrayContains("selectedEntrants", deviceId)
                .get();
        com.google.android.gms.tasks.Task<QuerySnapshot> acceptedSelectedTask = db
                .collection("events")
                .whereArrayContains("enrolledEntrants", deviceId)
                .get();
        com.google.android.gms.tasks.Task<QuerySnapshot> notificationTask = db
                .collection("notifications")
                .document(deviceId)
                .collection("messages")
                .get();

        com.google.android.gms.tasks.Tasks.whenAllComplete(
                        pendingSelectedTask,
                        acceptedSelectedTask,
                        notificationTask
                )
                .addOnSuccessListener(tasks -> {
                    QuerySnapshot pendingSelectedSnapshot =
                            pendingSelectedTask.isSuccessful() ? pendingSelectedTask.getResult() : null;
                    QuerySnapshot acceptedSelectedSnapshot =
                            acceptedSelectedTask.isSuccessful() ? acceptedSelectedTask.getResult() : null;
                    QuerySnapshot notificationSnapshot =
                            notificationTask.isSuccessful() ? notificationTask.getResult() : null;

                    loadPrivateInvitesAndPopulate(
                            pendingSelectedSnapshot,
                            acceptedSelectedSnapshot,
                            notificationSnapshot
                    );
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading invitations", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPrivateInvitesAndPopulate(QuerySnapshot pendingSelectedSnapshot,
                                               QuerySnapshot acceptedSelectedSnapshot,
                                               QuerySnapshot notificationSnapshot) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, AppNotification> privateInviteNotificationsByEventId = new HashMap<>();
        List<com.google.android.gms.tasks.Task<DocumentSnapshot>> privateInviteTasks = new ArrayList<>();

        if (notificationSnapshot != null) {
            for (DocumentSnapshot doc : notificationSnapshot.getDocuments()) {
                AppNotification notification = doc.toObject(AppNotification.class);
                if (notification == null
                        || notification.getEventId() == null
                        || notification.getEventId().trim().isEmpty()) {
                    continue;
                }

                if ("privateInvite".equals(notification.getType())) {
                    privateInviteNotificationsByEventId.put(notification.getEventId(), notification);
                    privateInviteTasks.add(
                            db.collection("events")
                                    .document(notification.getEventId())
                                    .collection(PrivateEventInvite.SUBCOLLECTION)
                                    .document(deviceId)
                                    .get()
                    );
                }
            }
        }

        if (privateInviteTasks.isEmpty()) {
            populateList(
                    pendingSelectedSnapshot,
                    acceptedSelectedSnapshot,
                    new HashMap<>(),
                    privateInviteNotificationsByEventId
            );
            return;
        }

        com.google.android.gms.tasks.Tasks.whenAllComplete(privateInviteTasks)
                .addOnSuccessListener(results -> {
                    Map<String, PrivateEventInvite> privateInvitesByEventId = new HashMap<>();
                    for (com.google.android.gms.tasks.Task<?> task : results) {
                        if (!task.isSuccessful() || !(task.getResult() instanceof DocumentSnapshot)) {
                            continue;
                        }

                        DocumentSnapshot inviteSnapshot = (DocumentSnapshot) task.getResult();
                        PrivateEventInvite invite = inviteSnapshot.toObject(PrivateEventInvite.class);
                        if (invite == null || invite.getEventId() == null || invite.getEventId().trim().isEmpty()) {
                            continue;
                        }
                        privateInvitesByEventId.put(invite.getEventId(), invite);
                    }

                    populateList(
                            pendingSelectedSnapshot,
                            acceptedSelectedSnapshot,
                            privateInvitesByEventId,
                            privateInviteNotificationsByEventId
                    );
                })
                .addOnFailureListener(e -> populateList(
                        pendingSelectedSnapshot,
                        acceptedSelectedSnapshot,
                        new HashMap<>(),
                        privateInviteNotificationsByEventId
                ));
    }

    /**
     * Populates {@link #invitationList} from both lottery-selected events and pending
     * private-event invites, then refreshes the UI.
     *
     * <p>Each {@link DocumentSnapshot} is converted to an {@link Event}. Events where the
     * device ID appears in {@code enrolledEntrants} are excluded (the entrant has already
     * accepted). Events where the device ID appears in {@code cancelledEntrants} are
     * intentionally included, allowing re-selected entrants to respond again.
     *
     * <p>After the list is rebuilt, the adapter is notified and the empty-state view and
     * {@link RecyclerView} visibility are toggled accordingly.
     *
     */
    private void populateList(QuerySnapshot pendingSelectedSnapshot,
                              QuerySnapshot acceptedSelectedSnapshot,
                              Map<String, PrivateEventInvite> privateInvitesByEventId,
                              Map<String, AppNotification> privateInviteNotificationsByEventId) {
        invitationList.clear();
        pendingPrivateInviteEventIds.clear();
        acceptedInvitationEventIds.clear();
        Set<String> seenEventIds = new HashSet<>();

        if (pendingSelectedSnapshot != null) {
            for (DocumentSnapshot doc : pendingSelectedSnapshot.getDocuments()) {
                Event event = EventSchema.normalizeLoadedEvent(doc);
                if (event == null || event.getId() == null || seenEventIds.contains(event.getId())) {
                    continue;
                }
                invitationList.add(event);
                seenEventIds.add(event.getId());
            }
        }

        if (acceptedSelectedSnapshot != null) {
            for (DocumentSnapshot doc : acceptedSelectedSnapshot.getDocuments()) {
                Event event = EventSchema.normalizeLoadedEvent(doc);
                if (event == null || event.getId() == null || seenEventIds.contains(event.getId())) {
                    continue;
                }
                invitationList.add(event);
                acceptedInvitationEventIds.add(event.getId());
                seenEventIds.add(event.getId());
            }
        }

        if (privateInvitesByEventId != null) {
            for (Map.Entry<String, PrivateEventInvite> entry : privateInvitesByEventId.entrySet()) {
                PrivateEventInvite invite = entry.getValue();
                if (invite == null || invite.getEventId() == null || seenEventIds.contains(invite.getEventId())) {
                    continue;
                }

                String status = invite.getStatus();
                boolean isPending = PrivateEventInvite.STATUS_PENDING.equals(status);
                boolean isAccepted = PrivateEventInvite.STATUS_ACCEPTED.equals(status);
                if (!isPending && !isAccepted) {
                    continue;
                }

                String eventId = invite.getEventId();
                invitationList.add(buildFallbackPrivateInviteEvent(eventId, invite));
                if (isPending) {
                    pendingPrivateInviteEventIds.add(eventId);
                } else if (isAccepted) {
                    acceptedInvitationEventIds.add(eventId);
                }
                seenEventIds.add(eventId);
            }
        }

        for (Map.Entry<String, AppNotification> entry : privateInviteNotificationsByEventId.entrySet()) {
            String eventId = entry.getKey();
            if (seenEventIds.contains(eventId)) {
                continue;
            }

            AppNotification notification = entry.getValue();
            PrivateEventInvite fallbackInvite = new PrivateEventInvite();
            fallbackInvite.setEventId(eventId);
            fallbackInvite.setEventTitle(notification.getEventTitle());
            fallbackInvite.setStatus(PrivateEventInvite.STATUS_PENDING);

            invitationList.add(buildFallbackPrivateInviteEvent(eventId, fallbackInvite));
            pendingPrivateInviteEventIds.add(eventId);
            seenEventIds.add(eventId);
        }

        renderInvitations();
    }

    private Event buildFallbackPrivateInviteEvent(String eventId, PrivateEventInvite invite) {
        Event event = new Event();
        event.setId(eventId);
        event.setPrivate(true);
        if (invite != null && invite.getEventTitle() != null && !invite.getEventTitle().trim().isEmpty()) {
            event.setTitle(invite.getEventTitle());
        } else {
            event.setTitle(getString(R.string.invitation_private_event_fallback_title));
        }
        return event;
    }

    private void renderInvitations() {
        progressBar.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        tvNoInvitations.setVisibility(invitationList.isEmpty() ? View.VISIBLE : View.GONE);
        rvInvitations.setVisibility(invitationList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Applies the entrant's accept or decline response to the given {@link Event} and
     * persists the result using Firestore atomic updates.
     *
     * <p>On success, a confirmation {@link Toast} is shown and {@link #loadInvitations()}
     * is called to refresh the list. On failure, a {@link Toast} error is shown and
     * {@link #progressBar} is hidden.
     *
     * <p>Lottery invitations update the event arrays directly. Pending private-event
     * invites update the corresponding invite document status instead.</p>
     *
     * @param event      The {@link Event} the entrant is responding to.
     * @param isAccepted {@code true} if the entrant accepted the invitation;
     *                   {@code false} if they declined.
     */
    private void processInvitationResponse(Event event, boolean isAccepted) {
        progressBar.setVisibility(View.VISIBLE);

        if (pendingPrivateInviteEventIds.contains(event.getId())) {
            processPrivateInviteResponse(event, isAccepted);
            return;
        }

        FirebaseFirestore.getInstance().collection("events").document(event.getId())
                .update(
                        "selectedEntrants", FieldValue.arrayRemove(deviceId),
                        "invitedEntrants", FieldValue.arrayUnion(deviceId),
                        "enrolledEntrants", isAccepted ? FieldValue.arrayUnion(deviceId) : FieldValue.arrayRemove(deviceId),
                        "cancelledEntrants", isAccepted ? FieldValue.arrayRemove(deviceId) : FieldValue.arrayUnion(deviceId),
                        "declinedEntrants", isAccepted ? FieldValue.arrayRemove(deviceId) : FieldValue.arrayUnion(deviceId)
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isAccepted ? "Invitation Accepted!" : "Invitation Declined.", Toast.LENGTH_SHORT).show();
                    loadInvitations();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to update response. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void processPrivateInviteResponse(Event event, boolean isAccepted) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference eventRef = db.collection("events").document(event.getId());
        DocumentReference inviteRef = eventRef
                .collection(PrivateEventInvite.SUBCOLLECTION)
                .document(deviceId);

        if (!isAccepted) {
            inviteRef.update(
                            "status", PrivateEventInvite.STATUS_DECLINED,
                            "respondedAt", new Date()
                    )
                    .addOnSuccessListener(unused -> {
                        pendingPrivateInviteEventIds.remove(event.getId());
                        Toast.makeText(this, "Private invitation declined.", Toast.LENGTH_LONG).show();
                        loadInvitations();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to update response. Please try again.", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        eventRef.get()
                .addOnSuccessListener(snapshot -> {
                    Event currentEvent = EventSchema.normalizeLoadedEvent(snapshot);
                    if (currentEvent == null) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Event not found for this invitation.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentEvent.getCoOrganizers() != null
                            && currentEvent.getCoOrganizers().contains(deviceId)) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Co-organizers cannot join the waiting list.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if ((currentEvent.getSelectedEntrants() != null && currentEvent.getSelectedEntrants().contains(deviceId))
                            || (currentEvent.getEnrolledEntrants() != null && currentEvent.getEnrolledEntrants().contains(deviceId))
                            || (currentEvent.getWaitingList() != null && currentEvent.getWaitingList().contains(deviceId))) {
                        inviteRef.update(
                                        "status", PrivateEventInvite.STATUS_ACCEPTED,
                                        "respondedAt", new Date()
                                )
                                .addOnSuccessListener(unused -> {
                                    pendingPrivateInviteEventIds.remove(event.getId());
                                    Toast.makeText(this, "This invitation is already active on your account.", Toast.LENGTH_LONG).show();
                                    loadInvitations();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(this, "Failed to update response. Please try again.", Toast.LENGTH_SHORT).show();
                                });
                        return;
                    }

                    if (!isRegistrationOpenNow(currentEvent)) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Registration is not currently open for this event.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!hasWaitlistCapacity(currentEvent)) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "This waiting list is already full.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    batch.update(inviteRef,
                            "status", PrivateEventInvite.STATUS_ACCEPTED,
                            "respondedAt", new Date());
                    batch.update(eventRef,
                            "waitingList", FieldValue.arrayUnion(deviceId),
                            "registeredEntrants", FieldValue.arrayUnion(deviceId));
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                pendingPrivateInviteEventIds.remove(event.getId());
                                Toast.makeText(
                                        this,
                                        "Private invitation accepted. You have been added to the waiting list.",
                                        Toast.LENGTH_LONG
                                ).show();
                                loadInvitations();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed to update response. Please try again.", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to verify the event for this invitation.", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isRegistrationOpenNow(Event currentEvent) {
        Date now = new Date();
        Date open = currentEvent.getRegistrationOpen();
        Date deadline = currentEvent.getRegistrationDeadline();

        boolean afterOpen = open == null || !now.before(open);
        boolean beforeDeadline = deadline == null || !now.after(deadline);
        return afterOpen && beforeDeadline;
    }

    private boolean hasWaitlistCapacity(Event currentEvent) {
        int waitlistCap = currentEvent.getWaitlistCap();
        return waitlistCap <= 0
                || currentEvent.getWaitingList() == null
                || currentEvent.getWaitingList().size() < waitlistCap;
    }
}
