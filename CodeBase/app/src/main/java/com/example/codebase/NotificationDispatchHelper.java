package com.example.codebase;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes system-generated event notifications while respecting recipient opt-out preferences.
 */
public final class NotificationDispatchHelper {

    /**
     * Callback used by notification dispatch operations that need delivery counts.
     */
    public interface DispatchCallback {
        /**
         * Called after every recipient write succeeds.
         *
         * @param deliveredCount number of notification documents written
         * @param optedOutCount recipients skipped because they opted out
         */
        void onComplete(int deliveredCount, int optedOutCount);

        /**
         * Called when one or more notification writes fail.
         *
         * @param e the failure that interrupted dispatch
         */
        void onFailure(Exception e);
    }

    private NotificationDispatchHelper() {
    }

    public static void sendEventNotifications(List<String> recipients,
                                              String eventId,
                                              String eventTitle,
                                              String title,
                                              String message,
                                              String status,
                                              String type) {
        sendEventNotifications(recipients, eventId, eventTitle, title, message, status, type,
                null, null, null);
    }

    /**
     * Writes notifications for the given recipients and optionally emits one admin audit log.
     *
     * <p>This path resolves user opt-out preferences before dispatching individual
     * notification records.
     *
     * @param recipients recipient device IDs before opt-out filtering
     * @param eventId related event ID
     * @param eventTitle related event title
     * @param title notification title
     * @param message notification body
     * @param status human-readable status label
     * @param type notification type key
     * @param senderDeviceId organizer or system sender to record in admin audit logs, or {@code null}
     * @param auditTargetGroup audit label for the recipient group, or {@code null}
     * @param callback optional result callback
     */
    public static void sendEventNotifications(List<String> recipients,
                                              String eventId,
                                              String eventTitle,
                                              String title,
                                              String message,
                                              String status,
                                              String type,
                                              @Nullable String senderDeviceId,
                                              @Nullable String auditTargetGroup,
                                              @Nullable DispatchCallback callback) {
        NotificationPreferenceHelper.resolveEnabledRecipients(recipients,
                (enabledRecipients, optedOutCount) -> {
                    sendPreFilteredNotifications(enabledRecipients,
                            eventId,
                            eventTitle,
                            title,
                            message,
                            status,
                            type,
                            senderDeviceId,
                            auditTargetGroup,
                            optedOutCount,
                            callback);
                });
    }

    /**
     * Writes notifications for recipients that have already passed preference filtering.
     *
     * @param recipients recipient device IDs that should receive notification documents
     * @param eventId related event ID
     * @param eventTitle related event title
     * @param title notification title
     * @param message notification body
     * @param status human-readable status label
     * @param type notification type key
     * @param senderDeviceId organizer or system sender to record in admin audit logs, or {@code null}
     * @param auditTargetGroup audit label for the recipient group, or {@code null}
     * @param optedOutCount recipients previously skipped because of preferences
     * @param callback optional result callback
     */
    public static void sendPreFilteredNotifications(List<String> recipients,
                                                    String eventId,
                                                    String eventTitle,
                                                    String title,
                                                    String message,
                                                    String status,
                                                    String type,
                                                    @Nullable String senderDeviceId,
                                                    @Nullable String auditTargetGroup,
                                                    int optedOutCount,
                                                    @Nullable DispatchCallback callback) {
        if (recipients == null || recipients.isEmpty()) {
            if (callback != null) {
                callback.onComplete(0, optedOutCount);
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        int total = recipients.size();
        int[] sent = {0};
        boolean[] failed = {false};

        for (String recipientId : recipients) {
            Map<String, Object> notif = new HashMap<>();
            notif.put("userId", recipientId);
            notif.put("eventId", eventId);
            notif.put("eventTitle", eventTitle);
            notif.put("title", title);
            notif.put("message", message);
            notif.put("status", status);
            notif.put("type", type);
            notif.put("sentAt", new Timestamp(new Date()));
            notif.put("read", false);

            db.collection("notifications")
                    .document(recipientId)
                    .collection("messages")
                    .add(notif)
                    .addOnSuccessListener(unused -> {
                        if (failed[0]) {
                            return;
                        }

                        sent[0]++;
                        if (sent[0] == total) {
                            if (senderDeviceId != null
                                    && !senderDeviceId.trim().isEmpty()
                                    && auditTargetGroup != null
                                    && !auditTargetGroup.trim().isEmpty()) {
                                NotificationLogHelper.logOrganizerSend(
                                        senderDeviceId,
                                        eventId,
                                        eventTitle,
                                        auditTargetGroup,
                                        title,
                                        message,
                                        total
                                );
                            }
                            if (callback != null) {
                                callback.onComplete(total, optedOutCount);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (failed[0]) {
                            return;
                        }
                        failed[0] = true;
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                    );
        }
    }
}
