package com.example.codebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * Utility class that reads unread notifications from Firestore for the current device
 * and posts them as local Android notifications.
 *
 * <p>Notifications are stored in Firestore under:
 * {@code notifications/{deviceId}/messages/{notificationId}}.
 * Each document is expected to deserialise into an {@link AppNotification}. After a
 * notification is successfully posted it is marked as read ({@code "read": true}) in
 * Firestore so it is not shown again on subsequent checks.
 *
 * <p>All methods are static; this class is not intended to be instantiated.
 */
public class SelectedNotificationChecker {

    /**
     * The notification channel ID used for all lottery-result notifications on
     * Android 8.0 (Oreo, API 26) and above.
     */
    private static final String CHANNEL_ID = "timely_notifications";

    /**
     * Queries Firestore for unread notifications belonging to the current device and
     * posts each one as a local Android notification.
     *
     * <p>For each unread {@link AppNotification} document:
     * <ol>
     *   <li>{@link #showNotification(Context, AppNotification, String)} is called to
     *       build and post the system notification.</li>
     *   <li>If posting succeeds, the Firestore document's {@code "read"} field is set
     *       to {@code true} to prevent duplicate notifications.</li>
     * </ol>
     *
     * <p>The notification channel is created (if not already present) before any
     * notifications are posted.
     *
     * @param context The {@link Context} used to access the device ID, notification
     *                system services, and Firestore.
     */
    public static void checkAndShow(Context context) {
        String deviceId = DeviceIdManager.getOrCreateDeviceId(context);

        AppDatabase.getInstance()
                .usersRef
                .document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Boolean storedPreference = snapshot.getBoolean(
                            NotificationPreferenceHelper.FIELD_NOTIFICATIONS_ENABLED);
                    if (!NotificationPreferenceHelper.isNotificationsEnabled(storedPreference)) {
                        return;
                    }

                    loadUnreadNotifications(context, deviceId);
                })
                .addOnFailureListener(e ->
                        // Preserve legacy behaviour if the preference lookup fails.
                        loadUnreadNotifications(context, deviceId));
    }

    /**
     * Queries Firestore for unread notifications only after the entrant's preference
     * has been checked and confirmed as enabled.
     *
     * @param context  app context used for Android notifications
     * @param deviceId current device/user ID
     */
    private static void loadUnreadNotifications(Context context, String deviceId) {
        createNotificationChannel(context);

        AppDatabase.getInstance()
                .notificationsRef
                .document(deviceId)
                .collection("messages")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AppNotification appNotification = doc.toObject(AppNotification.class);
                        if (showNotification(context, appNotification, doc.getId())) {
                            doc.getReference().update("read", true);
                        }
                    }
                });
    }

    /**
     * Builds and posts a single local notification for the given {@link AppNotification}.
     *
     * <p>Tapping the notification opens {@link EntrantEventDetailActivity} with the
     * associated event ID. The notification is auto-cancelled when tapped.
     *
     * <p>Returns {@code false} without posting if:
     * <ul>
     *   <li>{@code appNotification} is {@code null} or its event ID is {@code null}.</li>
     *   <li>The device is running Android 13 (Tiramisu, API 33) or above and the
     *       {@link Manifest.permission#POST_NOTIFICATIONS} permission has not been
     *       granted.</li>
     * </ul>
     *
     * <p>Fallback strings are used if {@link AppNotification#getTitle()} or
     * {@link AppNotification#getMessage()} return {@code null}.
     *
     * @param context          The {@link Context} used to build the notification and
     *                         resolve the pending intent.
     * @param appNotification  The {@link AppNotification} data to display.
     * @param notificationId   The Firestore document ID, used as a stable numeric
     *                         notification ID (via {@link String#hashCode()}).
     * @return {@code true} if the notification was posted successfully;
     *         {@code false} otherwise.
     */
    private static boolean showNotification(
            Context context,
            AppNotification appNotification,
            String notificationId) {

        if (appNotification == null || appNotification.getEventId() == null) {
            return false;
        }

        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(appNotification.getTitle() != null
                        ? appNotification.getTitle() : "Event update")
                .setContentText(appNotification.getMessage() != null
                        ? appNotification.getMessage() : "Open the app to view the update.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        manager.notify(notificationId.hashCode(), builder.build());
        return true;
    }

    /**
     * Creates the {@link NotificationChannel} required for posting notifications on
     * Android 8.0 (Oreo, API 26) and above.
     *
     * <p>The channel is created with {@link NotificationManager#IMPORTANCE_HIGH} and
     * the ID {@link #CHANNEL_ID}. This call is a no-op on earlier API levels or if the
     * channel already exists.
     *
     * @param context The {@link Context} used to obtain the {@link NotificationManager}.
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Timely Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Lottery result notifications");

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
