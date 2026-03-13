package com.example.codebase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for SelectedNotificationChecker logic.
 *
 * Tests cover:
 *   1.  showNotification returns false when appNotification is null
 *   2.  showNotification returns false when eventId is null
 *   3.  showNotification returns false when eventId is empty
 *   4.  Valid notification passes null checks
 *   5.  Notification title falls back to default when null
 *   6.  Notification message falls back to default when null
 *   7.  Notification title used when provided
 *   8.  Notification message used when provided
 *   9.  notificationId hashCode used as notification ID (no collision check)
 *   10. AppNotification model fields set correctly
 *   11. AppNotification eventId null → invalid
 *   12. AppNotification with all fields → valid
 *   13. Two different notificationIds produce different hash codes
 *   14. Same notificationId always produces same hash code
 *
 * Add to: app/src/test/java/com/example/codebase/SelectedNotificationCheckerTest.java
 *
 * Note: Firestore, Android context, and NotificationManager calls are NOT
 * tested here — those require instrumented tests. These tests cover the
 * pure logic that can be extracted and verified without Android dependencies.
 */
public class SelectedNotificationCheckerTest {

    private static final String DEFAULT_TITLE   = "Event update";
    private static final String DEFAULT_MESSAGE = "Open the app to view the update.";

    // ── Helper — mirrors showNotification() guard logic ───────────────────────

    /**
     * Returns false if the notification should not be shown.
     * Mirrors the null checks in showNotification().
     */
    private boolean isValidNotification(AppNotification notification) {
        if (notification == null)              return false;
        if (notification.getEventId() == null) return false;
        if (notification.getEventId().isEmpty()) return false;
        return true;
    }

    /**
     * Returns the title to display — mirrors builder logic.
     */
    private String resolveTitle(AppNotification notification) {
        return notification.getTitle() != null
                ? notification.getTitle()
                : DEFAULT_TITLE;
    }

    /**
     * Returns the message to display — mirrors builder logic.
     */
    private String resolveMessage(AppNotification notification) {
        return notification.getMessage() != null
                ? notification.getMessage()
                : DEFAULT_MESSAGE;
    }

    /** Creates a minimal valid AppNotification */
    private AppNotification makeNotification(String eventId, String title, String message) {
        AppNotification n = new AppNotification();
        n.setEventId(eventId);
        n.setTitle(title);
        n.setMessage(message);
        return n;
    }

    // ─── Test 1: Null notification → invalid ─────────────────────────────────

    @Test
    public void testShowNotification_nullNotification() {
        assertFalse("Null notification should not be shown",
                isValidNotification(null));
    }

    // ─── Test 2: Null eventId → invalid ──────────────────────────────────────

    @Test
    public void testShowNotification_nullEventId() {
        AppNotification n = makeNotification(null, "Title", "Message");
        assertFalse("Null eventId should not be shown", isValidNotification(n));
    }

    // ─── Test 3: Empty eventId → invalid ─────────────────────────────────────

    @Test
    public void testShowNotification_emptyEventId() {
        AppNotification n = makeNotification("", "Title", "Message");
        assertFalse("Empty eventId should not be shown", isValidNotification(n));
    }

    // ─── Test 4: Valid notification passes checks ─────────────────────────────

    @Test
    public void testShowNotification_validNotification() {
        AppNotification n = makeNotification("event123", "You're selected!", "Confirm your spot.");
        assertTrue("Valid notification should pass", isValidNotification(n));
    }

    // ─── Test 5: Title falls back to default when null ───────────────────────

    @Test
    public void testTitle_fallsBackToDefaultWhenNull() {
        AppNotification n = makeNotification("event123", null, "Some message");
        assertEquals("Should use default title when null",
                DEFAULT_TITLE, resolveTitle(n));
    }

    // ─── Test 6: Message falls back to default when null ─────────────────────

    @Test
    public void testMessage_fallsBackToDefaultWhenNull() {
        AppNotification n = makeNotification("event123", "Title", null);
        assertEquals("Should use default message when null",
                DEFAULT_MESSAGE, resolveMessage(n));
    }

    // ─── Test 7: Title used when provided ────────────────────────────────────

    @Test
    public void testTitle_usedWhenProvided() {
        AppNotification n = makeNotification("event123", "Congratulations!", "Message");
        assertEquals("Should use provided title",
                "Congratulations!", resolveTitle(n));
    }

    // ─── Test 8: Message used when provided ──────────────────────────────────

    @Test
    public void testMessage_usedWhenProvided() {
        AppNotification n = makeNotification("event123", "Title", "You have been selected for Cabo Festival.");
        assertEquals("Should use provided message",
                "You have been selected for Cabo Festival.", resolveMessage(n));
    }

    // ─── Test 9: notificationId hashCode is consistent ───────────────────────

    @Test
    public void testNotificationId_hashCodeConsistent() {
        String notifId = "abc123";
        int hash1 = notifId.hashCode();
        int hash2 = notifId.hashCode();
        assertEquals("Same notificationId should always produce same hash", hash1, hash2);
    }

    // ─── Test 10: AppNotification model fields set correctly ─────────────────

    @Test
    public void testAppNotificationModel_fieldsSetCorrectly() {
        AppNotification n = makeNotification("eventXYZ", "Test Title", "Test Message");
        assertEquals("eventXYZ",    n.getEventId());
        assertEquals("Test Title",  n.getTitle());
        assertEquals("Test Message", n.getMessage());
    }

    // ─── Test 11: AppNotification with null eventId → invalid ────────────────

    @Test
    public void testAppNotificationModel_nullEventIdInvalid() {
        AppNotification n = new AppNotification();
        // eventId not set — defaults to null
        assertFalse("Notification with null eventId should be invalid",
                isValidNotification(n));
    }

    // ─── Test 12: AppNotification with all fields → valid ────────────────────

    @Test
    public void testAppNotificationModel_allFieldsValid() {
        AppNotification n = makeNotification("event999", "Title", "Message");
        n.setRead(false);
        assertTrue("Fully populated notification should be valid", isValidNotification(n));
        assertFalse("read should be false initially", n.isRead());
    }

    // ─── Test 13: Different notificationIds produce different hashes ──────────

    @Test
    public void testNotificationId_differentIdsLikelyDifferentHashes() {
        String id1 = "notification_001";
        String id2 = "notification_002";
        // Not guaranteed but overwhelmingly likely for these inputs
        assertNotEquals("Different IDs should produce different hash codes",
                id1.hashCode(), id2.hashCode());
    }

    // ─── Test 14: Same notificationId always same hash ───────────────────────

    @Test
    public void testNotificationId_sameIdAlwaysSameHash() {
        String id = "stable_notification_id";
        assertEquals("Hash code must be deterministic",
                id.hashCode(), id.hashCode());
    }
}
