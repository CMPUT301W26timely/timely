package com.example.codebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * NotificationsActivity shows notifications stored in the notifications collection.
 *
 * Tapping a notification opens the related event details page.
 */
public class NotificationsActivity extends AppCompatActivity {

    private ListView listViewNotifications;
    private View emptyStateCard;
    private View invitationCard;
    private TextView emptyState;
    private TextView invitationCountSummary;

    private final ArrayList<HashMap<String, String>> items = new ArrayList<>();
    private final ArrayList<String> eventIds = new ArrayList<>();

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        listViewNotifications = findViewById(R.id.listViewNotifications);
        emptyStateCard = findViewById(R.id.cardNotificationsEmptyState);
        invitationCard = findViewById(R.id.invitationSummaryInclude);
        emptyState = findViewById(R.id.tvNotificationsEmptyState);
        invitationCountSummary = findViewById(R.id.tvInvitationCountSummary);
        deviceId = DeviceIdManager.getOrCreateDeviceId(this);

        listViewNotifications.setDivider(null);
        listViewNotifications.setDividerHeight(0);

        invitationCard.setOnClickListener(v ->
                startActivity(new Intent(this, InvitationsActivity.class)));

        setupBottomNavigation();
        loadInvitationSummary();
        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInvitationSummary();
        loadNotifications();
    }

    private void loadNotifications() {
        AppDatabase.getInstance()
                .notificationsRef
                .document(deviceId)
                .collection("messages")
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> populateNotifications(queryDocumentSnapshots.getDocuments()))
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load notifications",
                                Toast.LENGTH_SHORT).show());
    }

    private void loadInvitationSummary() {
        AppDatabase.getInstance()
                .eventsRef
                .whereArrayContains("selectedEntrants", deviceId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int invitationCount = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Event event = EventSchema.normalizeLoadedEvent(doc);
                        if (event == null) {
                            continue;
                        }

                        boolean isEnrolled = event.getEnrolledEntrants() != null
                                && event.getEnrolledEntrants().contains(deviceId);
                        if (!isEnrolled) {
                            invitationCount++;
                        }
                    }

                    if (invitationCount > 0) {
                        invitationCard.setVisibility(View.VISIBLE);
                        invitationCountSummary.setText(
                                "You have " + invitationCount + " invitation"
                                        + (invitationCount > 1 ? "s" : "")
                                        + " awaiting a response.");
                    } else {
                        invitationCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> invitationCard.setVisibility(View.GONE));
    }

    private void populateNotifications(java.util.List<DocumentSnapshot> documents) {
        items.clear();
        eventIds.clear();

        for (DocumentSnapshot doc : documents) {
            AppNotification notification = doc.toObject(AppNotification.class);
            if (notification != null && notification.getEventId() != null) {
                addNotification(
                        getNotificationLabel(notification),
                        notification.getMessage() != null ? notification.getMessage() : "No message",
                        notification.getEventId()
                );
            }
        }

        NotificationListAdapter adapter = new NotificationListAdapter(this, items);
        listViewNotifications.setAdapter(adapter);
        boolean hasNotifications = !items.isEmpty();
        emptyStateCard.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(hasNotifications ? View.GONE : View.VISIBLE);
        listViewNotifications.setVisibility(hasNotifications ? View.VISIBLE : View.GONE);

        listViewNotifications.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, EntrantEventDetailActivity.class);
            intent.putExtra(EntrantEventDetailActivity.EXTRA_EVENT_ID, eventIds.get(position));
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navExplore).setOnClickListener(v -> {
            startActivity(new Intent(this, BrowseEventsActivity.class));
            finish();
        });

        findViewById(R.id.navSearch).setOnClickListener(v ->
                Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navMyEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, OrganizerActivity.class));
            finish();
        });

        findViewById(R.id.navNotifications).setOnClickListener(v -> {
            // already here
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    private void addNotification(String status, String message, String eventId) {
        HashMap<String, String> row = new HashMap<>();
        row.put("status", status);
        row.put("message", message);

        items.add(row);
        eventIds.add(eventId);
    }

    private String getNotificationLabel(AppNotification notification) {
        if (notification.getStatus() != null && !notification.getStatus().trim().isEmpty()) {
            return notification.getStatus();
        }

        String type = notification.getType();
        if ("selectedEntrants".equals(type)) {
            return "Selected";
        }
        if ("cancelledEntrants".equals(type)) {
            return "Cancelled";
        }
        if ("waitingList".equals(type)) {
            return "Waiting List";
        }
        return "Notification";
    }

}
