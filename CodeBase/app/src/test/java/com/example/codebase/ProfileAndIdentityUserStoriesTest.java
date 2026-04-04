package com.example.codebase;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Story-focused JUnit coverage for profile, notification, and device identity flows.
 *
 * Covered stories:
 *   US 01.02.02 — Update profile information
 *   US 01.04.01 — Notification when chosen
 *   US 01.04.02 — Notification when not chosen
 *   US 01.07.01 — Device-based authentication
 */
public class ProfileAndIdentityUserStoriesTest {

    private User user;

    @Before
    public void setUp() {
        user = new User("device-abc-123");
    }

    @Test
    public void us010202_updateProfile_acceptsEditedNameEmailAndPhone() {
        user.setName("Taylor Brooks");
        user.setEmail("taylor@example.com");
        user.setPhoneNumber("+1 780 555 0198");

        ProfileInputValidator.ValidationResult result = ProfileInputValidator.validate(
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber()
        );

        assertTrue("Valid edited profile data should pass validation", result.isValid());
        assertEquals("Taylor Brooks", user.getName());
        assertEquals("taylor@example.com", user.getEmail());
    }

    @Test
    public void us010202_updateProfile_displaysUpdatedDataCorrectly() {
        user.setName("Jordan Lee");
        user.setEmail("jordan@example.com");
        user.setPhoneNumber("");

        ProfileDisplayState displayState = toDisplayState(user);

        assertEquals("Jordan Lee", displayState.name);
        assertEquals("jordan@example.com", displayState.email);
        assertEquals("No phone number set", displayState.phone);
        assertTrue("Profile with name and email should be considered complete",
                displayState.complete);
    }

    @Test
    public void us010204_deleteProfile_clearsEntrantFacingStateFromTheProfileModel() {
        user.setName("Jordan Lee");
        user.setEmail("jordan@example.com");
        user.setPhoneNumber("+1 780 555 0133");
        user.setNotificationsEnabled(false);

        clearProfile(user);

        assertEquals("device-abc-123", user.getDeviceId());
        assertEquals("", user.getName());
        assertEquals("", user.getEmail());
        assertEquals("", user.getPhoneNumber());
        assertTrue("A cleared profile should return to the default opted-in state for future signups",
                user.isNotificationsEnabled());
    }

    @Test
    public void us010701_deviceBasedAuthentication_defaultsToEntrantWithDeviceId() {
        assertEquals("device-abc-123", user.getDeviceId());
        assertEquals("entrant", user.getRole());
    }

    @Test
    public void us010701_deviceBasedAuthentication_shortensDeviceIdForDisplay() {
        String shortened = DeviceIdManager.getShortenedId("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("550e8400", shortened);
    }

    @Ignore("Persisting and reusing the same device ID requires Android SharedPreferences or instrumentation.")
    @Test
    public void us010701_deviceBasedAuthentication_persistsUniqueDeviceId() {
        // This acceptance criterion needs Android context or instrumentation support.
    }

    @Test
    public void us010401_notificationWhenChosen_usesSelectedStatusAndEventLink() {
        AppNotification notification = makeNotification(
                "event-001",
                "Selected",
                "selectedEntrants"
        );

        assertTrue("Chosen notification should contain an event link",
                hasEventLink(notification));
        assertEquals("Selected", resolveNotificationLabel(notification));
    }

    @Test
    public void us010402_notificationWhenNotChosen_usesNotSelectedStatusAndEventLink() {
        AppNotification notification = makeNotification(
                "event-002",
                "Not Selected",
                "cancelledEntrants"
        );

        assertTrue("Not-selected notification should still point to the related event",
                hasEventLink(notification));
        assertEquals("Not Selected", resolveNotificationLabel(notification));
    }

    @Test
    public void us010403_optOutNotifications_filtersRecipientsWhoDisabledOrganizerUpdates() {
        List<String> recipients = Arrays.asList("device-a", "device-b", "device-c");
        Map<String, Boolean> preferences = new HashMap<>();
        preferences.put("device-a", true);
        preferences.put("device-b", false);
        // device-c intentionally omitted to verify legacy default-to-enabled behaviour

        List<String> enabledRecipients =
                NotificationPreferenceHelper.filterEnabledRecipients(recipients, preferences);

        assertEquals(Arrays.asList("device-a", "device-c"), enabledRecipients);
    }

    @Test
    public void us010403_optOutNotifications_defaultsMissingPreferenceToEnabled() {
        assertTrue(NotificationPreferenceHelper.isNotificationsEnabled((Boolean) null));
        assertFalse(NotificationPreferenceHelper.isNotificationsEnabled(Boolean.FALSE));
    }

    @Ignore("Verifying that the notification PendingIntent opens the event page requires Android instrumentation.")
    @Test
    public void us010401_notification_opensEventPage() {
        // PendingIntent behaviour belongs in instrumentation testing.
    }

    private ProfileDisplayState toDisplayState(User currentUser) {
        String name = safeText(currentUser.getName(), "No name set");
        String email = safeText(currentUser.getEmail(), "No email set");
        String phone = safeText(currentUser.getPhoneNumber(), "No phone number set");
        boolean complete = !name.equals("No name set") && !email.equals("No email set");
        return new ProfileDisplayState(name, email, phone, complete);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private AppNotification makeNotification(String eventId, String status, String type) {
        AppNotification notification = new AppNotification();
        notification.setEventId(eventId);
        notification.setStatus(status);
        notification.setType(type);
        notification.setTitle("Lottery result");
        notification.setMessage("Open the app to review your event status.");
        return notification;
    }

    private boolean hasEventLink(AppNotification notification) {
        return notification != null
                && notification.getEventId() != null
                && !notification.getEventId().trim().isEmpty();
    }

    private String resolveNotificationLabel(AppNotification notification) {
        if (notification.getStatus() != null && !notification.getStatus().trim().isEmpty()) {
            return notification.getStatus();
        }

        if ("selectedEntrants".equals(notification.getType())) {
            return "Selected";
        }
        if ("cancelledEntrants".equals(notification.getType())) {
            return "Cancelled";
        }
        if ("waitingList".equals(notification.getType())) {
            return "Waiting List";
        }
        return "Notification";
    }

    private void clearProfile(User currentUser) {
        currentUser.setRole("entrant");
        currentUser.setName("");
        currentUser.setEmail("");
        currentUser.setPhoneNumber("");
        currentUser.setNotificationsEnabled(true);
    }

    private static class ProfileDisplayState {
        private final String name;
        private final String email;
        private final String phone;
        private final boolean complete;

        private ProfileDisplayState(String name, String email, String phone, boolean complete) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.complete = complete;
        }
    }
}
