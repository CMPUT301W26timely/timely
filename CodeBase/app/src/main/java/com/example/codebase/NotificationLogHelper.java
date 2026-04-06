package com.example.codebase;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Writes admin-visible audit entries for organizer notification sends.
 */
public final class NotificationLogHelper {

    private NotificationLogHelper() {
    }

    public static void logOrganizerSend(String senderDeviceId,
                                        String eventId,
                                        String eventTitle,
                                        String targetGroup,
                                        String title,
                                        String message,
                                        int recipientCount) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("senderDeviceId", senderDeviceId);
        logEntry.put("eventId", eventId);
        logEntry.put("eventTitle", eventTitle);
        logEntry.put("targetGroup", targetGroup);
        logEntry.put("title", title);
        logEntry.put("message", message);
        logEntry.put("recipientCount", recipientCount);
        logEntry.put("sentAt", new Timestamp(new Date()));

        AppDatabase.getInstance()
                .notificationLogsRef
                .add(logEntry);
    }
}
