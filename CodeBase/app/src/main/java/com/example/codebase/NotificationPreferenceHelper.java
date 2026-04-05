package com.example.codebase;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared helper for entrant notification opt-in / opt-out logic.
 *
 * <p>The app now stores a user-level notification preference in the Firestore
 * user document. Organizer/admin flows use this helper to respect that setting
 * before writing notification records.</p>
 */
public final class NotificationPreferenceHelper {

    /** Firestore field name storing the entrant's notification preference. */
    public static final String FIELD_NOTIFICATIONS_ENABLED = "notificationsEnabled";

    /**
     * Callback used when the async recipient filtering flow completes.
     */
    public interface EnabledRecipientsCallback {
        /**
         * Called when eligible recipients have been resolved.
         *
         * @param enabledRecipients recipients who still allow notifications
         * @param optedOutCount number of recipients skipped because they opted out
         */
        void onResolved(List<String> enabledRecipients, int optedOutCount);
    }

    private NotificationPreferenceHelper() {
    }

    /**
     * Returns whether notifications should be considered enabled for a stored value.
     *
     * <p>Missing legacy data defaults to {@code true} so older user documents stay
     * opted in until the entrant explicitly changes the setting.</p>
     *
     * @param storedValue Firestore boolean value, or {@code null} if absent
     * @return {@code true} when notifications are enabled
     */
    public static boolean isNotificationsEnabled(Boolean storedValue) {
        return storedValue == null || storedValue;
    }

    /**
     * Returns whether the given user currently allows notifications.
     *
     * @param user the user to inspect
     * @return {@code true} when notifications are enabled
     */
    public static boolean isNotificationsEnabled(User user) {
        return user == null || user.isNotificationsEnabled();
    }

    /**
     * Pure filtering helper used by unit tests and by the async Firestore path.
     *
     * <p>Any recipient missing from {@code preferencesByDeviceId} defaults to
     * enabled, preserving backward compatibility for older user documents.</p>
     *
     * @param recipientIds candidate recipient device IDs
     * @param preferencesByDeviceId preference map keyed by device ID
     * @return only the recipients who still allow notifications
     */
    public static List<String> filterEnabledRecipients(List<String> recipientIds,
                                                       Map<String, Boolean> preferencesByDeviceId) {
        List<String> enabledRecipients = new ArrayList<>();
        if (recipientIds == null) {
            return enabledRecipients;
        }

        for (String recipientId : recipientIds) {
            Boolean preference = preferencesByDeviceId != null
                    ? preferencesByDeviceId.get(recipientId)
                    : null;
            if (isNotificationsEnabled(preference)) {
                enabledRecipients.add(recipientId);
            }
        }
        return enabledRecipients;
    }

    /**
     * Loads each candidate recipient's notification preference from Firestore and
     * returns only those who are still opted in.
     *
     * <p>Read failures fall back to enabled rather than blocking organizer flows.
     * That keeps notification delivery resilient while still respecting explicit
     * opt-out values when they are available.</p>
     *
     * @param recipientIds candidate recipient device IDs
     * @param callback callback receiving the filtered recipient list
     */
    public static void resolveEnabledRecipients(List<String> recipientIds,
                                                EnabledRecipientsCallback callback) {
        List<String> safeRecipients = recipientIds != null ? recipientIds : new ArrayList<>();
        if (safeRecipients.isEmpty()) {
            if (callback != null) {
                callback.onResolved(new ArrayList<>(), 0);
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Boolean> preferencesByDeviceId = new HashMap<>();
        int[] remaining = {safeRecipients.size()};

        for (String recipientId : safeRecipients) {
            db.collection("users")
                    .document(recipientId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        preferencesByDeviceId.put(
                                recipientId,
                                snapshot.getBoolean(FIELD_NOTIFICATIONS_ENABLED));
                        maybeComplete(safeRecipients, preferencesByDeviceId, remaining, callback);
                    })
                    .addOnFailureListener(e -> {
                        // Fall back to enabled when the preference cannot be read.
                        preferencesByDeviceId.put(recipientId, true);
                        maybeComplete(safeRecipients, preferencesByDeviceId, remaining, callback);
                    });
        }
    }

    private static void maybeComplete(List<String> recipientIds,
                                      Map<String, Boolean> preferencesByDeviceId,
                                      int[] remaining,
                                      EnabledRecipientsCallback callback) {
        remaining[0]--;
        if (remaining[0] > 0 || callback == null) {
            return;
        }

        List<String> enabledRecipients =
                filterEnabledRecipients(recipientIds, preferencesByDeviceId);
        int optedOutCount = recipientIds.size() - enabledRecipients.size();
        callback.onResolved(enabledRecipients, optedOutCount);
    }
}
