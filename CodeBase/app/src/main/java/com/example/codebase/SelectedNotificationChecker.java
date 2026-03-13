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
 * Reads unread notifications from Firestore for the current user
 * and shows them as local Android notifications.
 */
public class SelectedNotificationChecker {

    private static final String CHANNEL_ID = "timely_notifications";

    public static void checkAndShow(Context context) {
        String deviceId = DeviceIdManager.getOrCreateDeviceId(context);

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

    private static boolean showNotification(
            Context context,
            AppNotification appNotification,
            String notificationId) {

        if (appNotification == null || appNotification.getEventId() == null) {
            return false;
        }

        Intent intent = new Intent(context, EntrantEventDetailActivity.class);
        intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, appNotification.getEventId());
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
