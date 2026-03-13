package com.example.codebase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Unit tests for {@link AppNotification}.
 */
public class AppNotificationTest {

    /**
     * Verifies that the notification model stores the fields required by the
     * organizer messaging flow.
     */
    @Test
    public void gettersAndSetters_roundTripNotificationFields() {
        AppNotification notification = new AppNotification();

        notification.setNotificationId("notif-1");
        notification.setUserId("device-77");
        notification.setEventId("event-33");
        notification.setEventTitle("Spring Dance Class");
        notification.setTitle("You were selected");
        notification.setMessage("Please confirm your spot in the app.");
        notification.setStatus("Selected");
        notification.setType("selectedEntrants");
        notification.setRead(false);

        assertEquals("notif-1", notification.getNotificationId());
        assertEquals("device-77", notification.getUserId());
        assertEquals("event-33", notification.getEventId());
        assertEquals("Spring Dance Class", notification.getEventTitle());
        assertEquals("You were selected", notification.getTitle());
        assertEquals("Please confirm your spot in the app.", notification.getMessage());
        assertEquals("Selected", notification.getStatus());
        assertEquals("selectedEntrants", notification.getType());
        assertFalse(notification.isRead());
    }
}
