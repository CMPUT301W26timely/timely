package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * NotificationsActivity shows notifications stored in the notifications collection.
 *
 * Tapping a notification opens the related event details page.
 */
public class NotificationsActivity extends AppCompatActivity {

    private ListView listViewNotifications;

    private final ArrayList<HashMap<String, String>> items = new ArrayList<>();
    private final ArrayList<String> eventIds = new ArrayList<>();

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        listViewNotifications = findViewById(R.id.listViewNotifications);
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        loadNotifications();
    }

    private void loadNotifications() {
        AppDatabase.getInstance()
                .notificationsRef
                .whereEqualTo("userId", deviceId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> populateNotifications(queryDocumentSnapshots.getDocuments()))
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load notifications",
                                Toast.LENGTH_SHORT).show());
    }

    private void populateNotifications(java.util.List<DocumentSnapshot> documents) {
        items.clear();
        eventIds.clear();

        for (DocumentSnapshot doc : documents) {
            AppNotification notification = doc.toObject(AppNotification.class);
            if (notification != null && notification.getEventId() != null) {
                addNotification(
                        notification.getStatus() != null ? notification.getStatus() : "Notification",
                        notification.getMessage() != null ? notification.getMessage() : "No message",
                        notification.getEventId()
                );
            }
        }

        NotificationListAdapter adapter = new NotificationListAdapter(this, items);
        listViewNotifications.setAdapter(adapter);

        listViewNotifications.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("eventId", eventIds.get(position));
            startActivity(intent);
        });
    }

    private void addNotification(String status, String message, String eventId) {
        HashMap<String, String> row = new HashMap<>();
        row.put("status", status);
        row.put("message", message);

        items.add(row);
        eventIds.add(eventId);
    }

}
