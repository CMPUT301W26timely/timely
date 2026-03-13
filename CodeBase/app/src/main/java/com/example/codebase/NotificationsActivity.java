package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * NotificationsActivity shows entrant notifications for:
 * - Selected
 * - Not Selected
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
        FirebaseFirestore.getInstance()
                .collection("events")
                .get()
                .addOnSuccessListener(this::populateNotifications)
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load notifications",
                                Toast.LENGTH_SHORT).show());
    }

    private void populateNotifications(QuerySnapshot snapshot) {
        items.clear();
        eventIds.clear();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String eventId = doc.getId();
            String title = doc.getString("title");
            if (title == null || title.isEmpty()) {
                title = "Untitled Event";
            }

            Date drawDate = doc.getDate("drawDate");

            List<?> selectedEntrants = (List<?>) doc.get("selectedEntrants");
            List<?> enrolledEntrants = (List<?>) doc.get("enrolledEntrants");
            List<?> waitingList = (List<?>) doc.get("waitingList");

            boolean isSelected =
                    listContainsDeviceId(selectedEntrants, deviceId) ||
                            listContainsDeviceId(enrolledEntrants, deviceId);

            boolean wasOnWaitlist = listContainsDeviceId(waitingList, deviceId);

            if (isSelected) {
                addNotification("Selected",
                        "You were selected for " + title,
                        eventId);
            } else if (drawDate != null && new Date().after(drawDate) && wasOnWaitlist) {
                addNotification("Not Selected",
                        "You were not selected for " + title,
                        eventId);
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

    /**
     * Checks whether a Firestore list contains the current device ID.
     * Supports:
     * - list of plain strings
     * - list of maps with field "deviceId"
     */
    private boolean listContainsDeviceId(List<?> list, String deviceId) {
        if (list == null) return false;

        for (Object item : list) {
            if (item instanceof String) {
                if (deviceId.equals(item)) {
                    return true;
                }
            } else if (item instanceof java.util.Map) {
                Object mapDeviceId = ((java.util.Map<?, ?>) item).get("deviceId");
                if (deviceId.equals(mapDeviceId)) {
                    return true;
                }
            }
        }
        return false;
    }
}