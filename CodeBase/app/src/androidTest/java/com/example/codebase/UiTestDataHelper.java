package com.example.codebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Shared helper methods for instrumentation tests.
 *
 * <p>The UI tests in this package rely on seeded Firestore documents so the real
 * activities can render realistic data without mocking the production repository layer.
 */
public final class UiTestDataHelper {

    /** SharedPreferences file used by {@link DeviceIdManager}. */
    public static final String APP_PREFS = "app_prefs";

    /** SharedPreferences key used by {@link DeviceIdManager}. */
    public static final String DEVICE_ID_KEY = "device_id";

    /** Stable device ID used by most instrumentation tests. */
    public static final String TEST_DEVICE_ID = "ui-test-device-001";

    /** Tiny valid PNG used for poster/image instrumentation tests. */
    public static final String TEST_POSTER_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wn4nYQAAAAASUVORK5CYII=";

    private static final long FIRESTORE_TIMEOUT_SECONDS = 10L;

    private UiTestDataHelper() {
    }

    /**
     * Returns the target application context.
     *
     * @return the target app context
     */
    public static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    /**
     * Stores a deterministic device ID so screens load the expected user data.
     *
     * @param deviceId the device ID to persist for the current test
     */
    public static void setDeviceId(String deviceId) {
        SharedPreferences preferences = context().getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        preferences.edit().putString(DEVICE_ID_KEY, deviceId).commit();
    }

    /**
     * Stores the active session role used by screens that branch on entrant vs admin UI.
     *
     * @param role one of the {@link WelcomeActivity} role constants
     */
    public static void setSessionRole(String role) {
        SharedPreferences preferences =
                context().getSharedPreferences(WelcomeActivity.PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(WelcomeActivity.KEY_ROLE, role).commit();
    }

    /**
     * Returns a unique ID suitable for test documents.
     *
     * @param prefix a readable prefix for the generated ID
     * @return a unique document identifier
     */
    public static String uniqueId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    /**
     * Seeds a user profile document in Firestore.
     *
     * @param deviceId the user/device ID
     * @param name the display name
     * @param email the email address
     * @param phone the phone number
     * @throws Exception if Firestore write fails
     */
    public static void seedUser(String deviceId, String name, String email, String phone) throws Exception {
        seedUser(deviceId, name, email, phone, true);
    }

    /**
     * Seeds a user profile document in Firestore with an explicit notification preference.
     *
     * @param deviceId the user/device ID
     * @param name the display name
     * @param email the email address
     * @param phone the phone number
     * @param notificationsEnabled whether organizer/admin notifications are enabled
     * @throws Exception if Firestore write fails
     */
    public static void seedUser(String deviceId,
                                String name,
                                String email,
                                String phone,
                                boolean notificationsEnabled) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("role", "entrant");
        data.put("name", name);
        data.put("email", email);
        data.put("phoneNumber", phone);
        data.put("notificationsEnabled", notificationsEnabled);
        await(FirebaseFirestore.getInstance().collection("users").document(deviceId).set(data));
    }

    /**
     * Seeds an event document with the canonical schema used by the app.
     *
     * @param eventId the Firestore document ID to write
     * @param title the event title
     * @param waitingList waiting-list device IDs
     * @param selectedEntrants selected entrant device IDs
     * @param enrolledEntrants enrolled entrant device IDs
     * @param cancelledEntrants cancelled entrant device IDs
     * @param registrationOpen registration-open date
     * @param registrationDeadline registration-deadline date
     * @param drawDate lottery draw date
     * @throws Exception if Firestore write fails
     */
    public static void seedEvent(String eventId,
                                 String title,
                                 ArrayList<String> waitingList,
                                 ArrayList<String> selectedEntrants,
                                 ArrayList<String> enrolledEntrants,
                                 ArrayList<String> cancelledEntrants,
                                 Date registrationOpen,
                                 Date registrationDeadline,
                                 Date drawDate) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        data.put("eventId", eventId);
        data.put("title", title);
        data.put("description", "Realistic test data for instrumentation coverage.");
        data.put("location", "Edmonton Community Hall");
        data.put("organizerDeviceId", "organizer-ui-test");
        data.put("registrationOpen", registrationOpen);
        data.put("registrationDeadline", registrationDeadline);
        data.put("drawDate", drawDate);
        data.put("startDate", daysFromToday(7));
        data.put("endDate", daysFromToday(8));
        data.put("maxCapacity", 60L);
        data.put("winnersCount", 60L);
        data.put("waitlistCap", 120);
        data.put("geoEnabled", true);
        data.put("price", 25.0);
        data.put("status", "published");
        data.put("waitingList", waitingList);
        data.put("selectedEntrants", selectedEntrants);
        data.put("enrolledEntrants", enrolledEntrants);
        data.put("cancelledEntrants", cancelledEntrants);
        data.put("invitedEntrants", new ArrayList<>(selectedEntrants));
        data.put("declinedEntrants", new ArrayList<String>());
        data.put("coOrganizers", new ArrayList<String>());
        data.put("isPrivate", false);

        await(FirebaseFirestore.getInstance().collection("events").document(eventId).set(data));
    }

    /**
     * Seeds a private event document used by invite-only organizer and entrant tests.
     *
     * @param eventId the Firestore document ID to write
     * @param title the event title
     * @throws Exception if Firestore write fails
     */
    public static void seedPrivateEvent(String eventId, String title) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        data.put("eventId", eventId);
        data.put("title", title);
        data.put("description", "Realistic private-event test data.");
        data.put("location", "Downtown Training Room");
        data.put("organizerDeviceId", "organizer-ui-test");
        data.put("registrationOpen", daysFromToday(-1));
        data.put("registrationDeadline", daysFromToday(2));
        data.put("drawDate", daysFromToday(5));
        data.put("startDate", daysFromToday(7));
        data.put("endDate", daysFromToday(8));
        data.put("maxCapacity", 20L);
        data.put("winnersCount", 20L);
        data.put("waitlistCap", 40);
        data.put("geoEnabled", false);
        data.put("price", 0.0);
        data.put("status", "published");
        data.put("waitingList", new ArrayList<String>());
        data.put("selectedEntrants", new ArrayList<String>());
        data.put("enrolledEntrants", new ArrayList<String>());
        data.put("cancelledEntrants", new ArrayList<String>());
        data.put("invitedEntrants", new ArrayList<String>());
        data.put("declinedEntrants", new ArrayList<String>());
        data.put("coOrganizers", new ArrayList<String>());
        data.put("isPrivate", true);

        await(FirebaseFirestore.getInstance().collection("events").document(eventId).set(data));
    }

    /**
     * Seeds a public event that includes a poster image for admin image-management tests.
     *
     * @param eventId the Firestore document ID to write
     * @param title the event title
     * @throws Exception if Firestore write fails
     */
    public static void seedPosterEvent(String eventId, String title) throws Exception {
        Map<String, Object> poster = new HashMap<>();
        poster.put("posterImageBase64", TEST_POSTER_BASE64);

        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        data.put("eventId", eventId);
        data.put("title", title);
        data.put("description", "Poster-backed test event.");
        data.put("location", "Edmonton Arts Hall");
        data.put("organizerDeviceId", "organizer-ui-test");
        data.put("registrationOpen", daysFromToday(-1));
        data.put("registrationDeadline", daysFromToday(2));
        data.put("drawDate", daysFromToday(5));
        data.put("startDate", daysFromToday(7));
        data.put("endDate", daysFromToday(8));
        data.put("maxCapacity", 20L);
        data.put("winnersCount", 20L);
        data.put("waitlistCap", 40);
        data.put("geoEnabled", false);
        data.put("price", 0.0);
        data.put("status", "published");
        data.put("waitingList", new ArrayList<String>());
        data.put("selectedEntrants", new ArrayList<String>());
        data.put("enrolledEntrants", new ArrayList<String>());
        data.put("cancelledEntrants", new ArrayList<String>());
        data.put("invitedEntrants", new ArrayList<String>());
        data.put("declinedEntrants", new ArrayList<String>());
        data.put("coOrganizers", new ArrayList<String>());
        data.put("isPrivate", false);
        data.put("poster", poster);

        await(FirebaseFirestore.getInstance().collection("events").document(eventId).set(data));
    }

    /**
     * Seeds a private-invite document beneath an event.
     *
     * @param eventId target event ID
     * @param deviceId invited device ID
     * @param eventTitle target event title
     * @param status invite status
     * @throws Exception if Firestore write fails
     */
    public static void seedPrivateInvite(String eventId,
                                         String deviceId,
                                         String eventTitle,
                                         String status) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("eventId", eventId);
        data.put("eventTitle", eventTitle);
        data.put("status", status);
        data.put("invitedAt", new Date());
        await(FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection(PrivateEventInvite.SUBCOLLECTION)
                .document(deviceId)
                .set(data));
    }

    /**
     * Deletes a previously seeded private-invite document.
     *
     * @param eventId target event ID
     * @param deviceId invited device ID
     * @throws Exception if Firestore delete fails
     */
    public static void deletePrivateInvite(String eventId, String deviceId) throws Exception {
        await(FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection(PrivateEventInvite.SUBCOLLECTION)
                .document(deviceId)
                .delete());
    }

    /**
     * Seeds an admin-visible notification log entry.
     *
     * @param eventId related event ID
     * @param eventTitle event title
     * @param targetGroup target group label
     * @param title log title
     * @param message log message
     * @return generated log document ID
     * @throws Exception if Firestore write fails
     */
    public static String seedNotificationLog(String eventId,
                                             String eventTitle,
                                             String targetGroup,
                                             String title,
                                             String message) throws Exception {
        String logId = uniqueId("notification-log");
        Map<String, Object> data = new HashMap<>();
        data.put("senderDeviceId", "organizer-ui-test");
        data.put("eventId", eventId);
        data.put("eventTitle", eventTitle);
        data.put("targetGroup", targetGroup);
        data.put("title", title);
        data.put("message", message);
        data.put("recipientCount", 3);
        data.put("sentAt", new Date());
        await(FirebaseFirestore.getInstance()
                .collection("notificationLogs")
                .document(logId)
                .set(data));
        return logId;
    }

    /**
     * Deletes an admin notification log document.
     *
     * @param logId log document ID
     * @throws Exception if Firestore delete fails
     */
    public static void deleteNotificationLog(String logId) throws Exception {
        await(FirebaseFirestore.getInstance()
                .collection("notificationLogs")
                .document(logId)
                .delete());
    }

    /**
     * Seeds a comment beneath an event.
     *
     * @param eventId related event ID
     * @param authorDeviceId author device ID
     * @param authorName author display name
     * @param text comment body
     * @param isOrganizer whether the comment is from an organizer
     * @return generated comment ID
     * @throws Exception if Firestore write fails
     */
    public static String seedComment(String eventId,
                                     String authorDeviceId,
                                     String authorName,
                                     String text,
                                     boolean isOrganizer) throws Exception {
        String commentId = uniqueId("comment");
        Map<String, Object> data = new HashMap<>();
        data.put("authorDeviceId", authorDeviceId);
        data.put("authorName", authorName);
        data.put("text", text);
        data.put("isOrganizer", isOrganizer);
        data.put("timestamp", new Date());
        await(FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .document(commentId)
                .set(data));
        return commentId;
    }

    /**
     * Seeds an entrant-location document beneath an event.
     *
     * @param eventId related event ID
     * @param deviceId entrant device ID
     * @param entrantName entrant display name
     * @param latitude latitude coordinate
     * @param longitude longitude coordinate
     * @throws Exception if Firestore write fails
     */
    public static void seedEntrantLocation(String eventId,
                                           String deviceId,
                                           String entrantName,
                                           double latitude,
                                           double longitude) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("entrantName", entrantName);
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("capturedAt", new Date());
        await(FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("entrantLocations")
                .document(deviceId)
                .set(data));
    }

    /**
     * Deletes a seeded entrant-location document.
     *
     * @param eventId related event ID
     * @param deviceId entrant device ID
     * @throws Exception if Firestore delete fails
     */
    public static void deleteEntrantLocation(String eventId, String deviceId) throws Exception {
        await(FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("entrantLocations")
                .document(deviceId)
                .delete());
    }

    /**
     * Deletes a seeded comment document.
     *
     * @param eventId related event ID
     * @param commentId comment document ID
     * @throws Exception if Firestore delete fails
     */
    public static void deleteComment(String eventId, String commentId) throws Exception {
        await(FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("comments")
                .document(commentId)
                .delete());
    }

    /**
     * Seeds a notification document for the current user.
     *
     * @param deviceId recipient device ID
     * @param eventId related event ID
     * @param title notification title
     * @param message notification message
     * @param status notification status label
     * @param type recipient-group type
     * @return the generated notification document ID
     * @throws Exception if Firestore write fails
     */
    public static String seedNotification(String deviceId,
                                          String eventId,
                                          String title,
                                          String message,
                                          String status,
                                          String type) throws Exception {
        String notificationId = uniqueId("notification");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", deviceId);
        data.put("eventId", eventId);
        data.put("eventTitle", "UI Test Event");
        data.put("title", title);
        data.put("message", message);
        data.put("status", status);
        data.put("type", type);
        data.put("read", false);
        data.put("sentAt", new Date());

        await(FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(deviceId)
                .collection("messages")
                .document(notificationId)
                .set(data));
        return notificationId;
    }

    /**
     * Deletes a seeded event document.
     *
     * @param eventId the event ID to delete
     * @throws Exception if Firestore delete fails
     */
    public static void deleteEvent(String eventId) throws Exception {
        await(FirebaseFirestore.getInstance().collection("events").document(eventId).delete());
    }

    /**
     * Deletes a seeded notification document.
     *
     * @param deviceId recipient device ID
     * @param notificationId the notification document ID
     * @throws Exception if Firestore delete fails
     */
    public static void deleteNotification(String deviceId, String notificationId) throws Exception {
        await(FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(deviceId)
                .collection("messages")
                .document(notificationId)
                .delete());
    }

    /**
     * Deletes a seeded user document.
     *
     * @param deviceId the user/device ID to delete
     * @throws Exception if Firestore delete fails
     */
    public static void deleteUser(String deviceId) throws Exception {
        await(FirebaseFirestore.getInstance().collection("users").document(deviceId).delete());
    }

    /**
     * Waits briefly for Firestore listeners and UI updates to complete.
     */
    public static void waitForUi() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(1500L);
    }

    /**
     * Returns a date offset from today.
     *
     * @param offsetDays number of days relative to today
     * @return the calculated date
     */
    public static Date daysFromToday(int offsetDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays);
        return calendar.getTime();
    }

    private static void await(com.google.android.gms.tasks.Task<?> task) throws Exception {
        Tasks.await(task, FIRESTORE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
