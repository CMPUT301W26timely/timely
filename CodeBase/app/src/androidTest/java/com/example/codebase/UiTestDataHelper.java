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

        await(FirebaseFirestore.getInstance().collection("events").document(eventId).set(data));
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
