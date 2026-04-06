package com.example.codebase;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared helper for resolving the current entrant's live pending invitation state.
 */
public final class PendingInvitationHelper {

    public static final class PendingInvitationState {
        private final List<Event> publicPendingEvents;
        private final Map<String, PrivateEventInvite> pendingPrivateInvitesByEventId;

        PendingInvitationState(List<Event> publicPendingEvents,
                               Map<String, PrivateEventInvite> pendingPrivateInvitesByEventId) {
            this.publicPendingEvents = publicPendingEvents;
            this.pendingPrivateInvitesByEventId = pendingPrivateInvitesByEventId;
        }

        public List<Event> getPublicPendingEvents() {
            return publicPendingEvents;
        }

        public Map<String, PrivateEventInvite> getPendingPrivateInvitesByEventId() {
            return pendingPrivateInvitesByEventId;
        }

        public Set<String> getAllEventIds() {
            Set<String> eventIds = new HashSet<>();
            for (Event event : publicPendingEvents) {
                if (event != null && event.getId() != null) {
                    eventIds.add(event.getId());
                }
            }
            eventIds.addAll(pendingPrivateInvitesByEventId.keySet());
            return eventIds;
        }
    }

    public interface PendingInvitationCallback {
        void onLoaded(Set<String> eventIds);

        void onError(Exception e);
    }

    public interface PendingInvitationStateCallback {
        void onLoaded(PendingInvitationState state);

        void onError(Exception e);
    }

    private PendingInvitationHelper() {
    }

    public static void loadPendingInvitationEventIds(String deviceId,
                                                     PendingInvitationCallback callback) {
        loadPendingInvitationEventIds(deviceId, true, callback);
    }

    public static void loadPendingInvitationEventIds(String deviceId,
                                                     boolean allowNotificationFallback,
                                                     PendingInvitationCallback callback) {
        loadPendingInvitationState(deviceId, new PendingInvitationStateCallback() {
            @Override
            public void onLoaded(PendingInvitationState state) {
                if (callback != null) {
                    callback.onLoaded(state.getAllEventIds());
                }
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }, allowNotificationFallback);
    }

    public static void loadPendingInvitationState(String deviceId,
                                                  PendingInvitationStateCallback callback) {
        loadPendingInvitationState(deviceId, callback, true);
    }

    public static void loadPendingInvitationState(String deviceId,
                                                  PendingInvitationStateCallback callback,
                                                  boolean allowNotificationFallback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        com.google.android.gms.tasks.Task<QuerySnapshot> selectedTask = db
                .collection("events")
                .whereArrayContains("selectedEntrants", deviceId)
                .get();
        com.google.android.gms.tasks.Task<QuerySnapshot> privateInviteTask = db
                .collectionGroup(PrivateEventInvite.SUBCOLLECTION)
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("status", PrivateEventInvite.STATUS_PENDING)
                .get();
        com.google.android.gms.tasks.Task<QuerySnapshot> notificationTask = db
                .collection("notifications")
                .document(deviceId)
                .collection("messages")
                .get();

        Tasks.whenAllComplete(selectedTask, privateInviteTask, notificationTask)
                .addOnSuccessListener(tasks -> {
                    List<Event> publicPendingEvents = new ArrayList<>();
                    Map<String, PrivateEventInvite> pendingPrivateInvitesByEventId = new HashMap<>();
                    Map<String, AppNotification> selectedNotificationFallbacks = new HashMap<>();
                    Map<String, AppNotification> privateInviteNotificationFallbacks = new HashMap<>();

                    if (notificationTask.isSuccessful() && notificationTask.getResult() != null) {
                        for (DocumentSnapshot doc : notificationTask.getResult().getDocuments()) {
                            AppNotification notification = doc.toObject(AppNotification.class);
                            if (notification == null || notification.getEventId() == null) {
                                continue;
                            }

                            if ("selectedEntrants".equals(notification.getType())) {
                                selectedNotificationFallbacks.put(notification.getEventId(), notification);
                            } else if ("privateInvite".equals(notification.getType())) {
                                privateInviteNotificationFallbacks.put(notification.getEventId(), notification);
                            }
                        }
                    }

                    if (selectedTask.isSuccessful() && selectedTask.getResult() != null) {
                        for (DocumentSnapshot doc : selectedTask.getResult().getDocuments()) {
                            boolean isEnrolled = EventSchema.toDeviceIdList(doc.get("enrolledEntrants"))
                                    .contains(deviceId);
                            if (isEnrolled) {
                                continue;
                            }

                            Event event;
                            try {
                                event = EventSchema.normalizeLoadedEvent(doc);
                            } catch (Exception ignored) {
                                event = null;
                            }

                            if (event == null) {
                                event = buildFallbackEvent(doc);
                            }

                            if (event != null && event.getId() != null) {
                                publicPendingEvents.add(event);
                            }
                        }
                    } else if (allowNotificationFallback) {
                        for (AppNotification notification : selectedNotificationFallbacks.values()) {
                            Event fallbackEvent = buildFallbackEvent(
                                    notification.getEventId(),
                                    notification.getEventTitle()
                            );
                            if (fallbackEvent != null) {
                                publicPendingEvents.add(fallbackEvent);
                            }
                        }
                    }

                    if (privateInviteTask.isSuccessful() && privateInviteTask.getResult() != null) {
                        for (DocumentSnapshot inviteDoc : privateInviteTask.getResult().getDocuments()) {
                            PrivateEventInvite invite = inviteDoc.toObject(PrivateEventInvite.class);
                            if (inviteDoc.getReference().getParent() == null
                                    || inviteDoc.getReference().getParent().getParent() == null) {
                                continue;
                            }
                            String eventId = invite != null && invite.getEventId() != null
                                    ? invite.getEventId()
                                    : inviteDoc.getReference().getParent().getParent().getId();
                            if (invite == null) {
                                invite = new PrivateEventInvite();
                                invite.setEventId(eventId);
                            }
                            pendingPrivateInvitesByEventId.put(eventId, invite);
                        }
                    } else if (allowNotificationFallback) {
                        for (AppNotification notification : privateInviteNotificationFallbacks.values()) {
                            PrivateEventInvite invite = new PrivateEventInvite();
                            invite.setEventId(notification.getEventId());
                            invite.setEventTitle(notification.getEventTitle());
                            invite.setStatus(PrivateEventInvite.STATUS_PENDING);
                            pendingPrivateInvitesByEventId.put(notification.getEventId(), invite);
                        }
                    }

                    if (callback != null) {
                        callback.onLoaded(new PendingInvitationState(
                                publicPendingEvents,
                                pendingPrivateInvitesByEventId
                        ));
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    private static Event buildFallbackEvent(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }

        return buildFallbackEvent(doc.getId(), doc.getString("title"));
    }

    private static Event buildFallbackEvent(String eventId, String title) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return null;
        }
        Event event = new Event();
        event.setId(eventId);
        event.setTitle(title != null && !title.trim().isEmpty() ? title : "Untitled Event");
        return event;
    }
}
